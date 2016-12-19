package sp.psl

import akka.actor._
import sp.system._
import sp.system.messages._
import sp.domain._
import sp.domain.Logic._

/**
  * Created by kristofer on 2016-05-08.
  */

object OrderHandler extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group" -> "hide" // to organize in gui. maybe use "hide" to hide service in gui
    ),
    "order" -> SPAttributes(
      "id"->KeyDefinition("ID", List(), Some(ID.newID)),
      "name"->KeyDefinition("String", List(), Some("noNameOrder")),
      "stations"->KeyDefinition("Map[String, ID]", List(), None)
    )
  )
  val transformTuple = (
    TransformValue("order", _.getAs[OrderDefinition]("order"))
    )

  val transformation: List[TransformValue[_]] = List(transformTuple)
  def props(serviceHandler: ActorRef, eventHandler: ActorRef) = Props(classOf[OrderHandler], serviceHandler, eventHandler)
}

// An order for a set of stations, where a station is a name and an ID for a SOP
case class OrderDefinition(id: ID, name: String, stations: Map[String, ID])
case class SPOrder(id: ID, name: String, stations: Map[String, ID], idMap: Map[ID, IDAble])

class OrderHandler(sh: ActorRef, ev: ActorRef) extends Actor with ServiceSupport with OrderHandlerLogic {
  var completedSops: Set[ID] = Set()
  ev ! SubscribeToSSE(self)

  def receive = {
    case r @ Request(_, attr, ids, id) =>
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)

      val newOrder = transform(OrderHandler.transformTuple)
      val order = SPOrder(newOrder.id, newOrder.name, newOrder.stations, ids.map(x => x.id -> x).toMap)

      println(s"new order: $newOrder")
      ev ! Progress(SPAttributes("status"->"new", "order"->newOrder), "OrderHandler", id)

      addNewOrder(order)


      replyTo ! Response(List(), SPAttributes("status"-> "order received", "silent"->true), r.service, r.reqID)

    case Progress(attr, "RunnerService", id) => println(s"order handler got a progress: $attr")

    case Response(ids, attr, "RunnerService", id) =>
      println("====================================")
      println("=== ORDER HANDLER ==================")
      println("====================================")

      println(s"Order handler got a response: $attr")
      val station = attr.getAs[String]("station")
      val sop = attr.getAs[ID]("sop").get
      if(completedSops.contains(sop)) {
        println(s"Order handler response already processed...")
      } else {
        completedSops += sop
        println(s"Order handler completing sop...")
        val complStationOrder = station.flatMap(orderInStationCompleted)
        complStationOrder.map(order =>
          ev ! Progress(SPAttributes("status"->"completed", "station"->station, "order"->OrderDefinition(order.id, order.name, order.stations)), "OrderHandler", ID.newID)
        )
      }
    case r @ Response(ids, attr, service, _) if service == "OperationControl" => {
      // high level force reset...
      if(attr.getAs[Boolean]("reset").getOrElse(false)) {
        println("OrderHandler: High level force reset! Resetting.")
        completedSops = Set()
        resetOrders
        ev ! Progress(SPAttributes("status"->"reset"), "OrderHandler", ID.newID)
      }
    }
    case r @ Response(ids, attr, service, id) => // println(s"order handler got a response, but no match: $r")
  }

  def startStationOrder(order: ActiveOrder) = {
    val req = Request("RunnerService", SPAttributes("SOP"->order.sop.id,"station"->order.station), order.order.idMap.values.toList)
    sh ! req
    ev ! Progress(SPAttributes("status"->"stationOrder", "station"->order.station, "sop"->order.sop, "order"->OrderDefinition(order.order.id, order.order.name, order.order.stations)), "OrderHandler", ID.newID)
  }
}

case class ActiveOrder(station: String, order: SPOrder, sop: SOPSpec, step: Option[ID])
case class CompletedOrders(completedStations: List[String], completed: Boolean)
sealed trait OrderHandlerLogic {

  def startStationOrder(order: ActiveOrder)

  var orders = List[SPOrder]()
  var orderCompleted = Map[SPOrder, CompletedOrders]()
  var activeStations = Map[String, ActiveOrder]()

  def resetOrders = {
    orders = List()
    orderCompleted = Map()
    activeStations = Map()
  }

  def addNewOrder(order: SPOrder) = {
    orders = orders :+ order
    order.stations.keys.foreach(s => {
      activeStations.get(s) match {
        case None => nextStationOrder(s)
        case _ => ()
        }
      }
    )
  }

  def orderInStationCompleted(station: String) = {
    val updOrder = for {
      ao <- activeStations.get(station)
      or = ao.order
    } yield {
      val compl = orderCompleted.get(or).getOrElse(CompletedOrders(List(), false))
      val complStations = station :: compl.completedStations
      val allStations = or.stations.keySet
      val allDone = complStations.toSet == allStations
      or -> compl.copy(complStations, allDone)
    }
    updOrder.foreach(co => orderCompleted = orderCompleted + co)
    if (updOrder.isEmpty) println(s"The station $station is not active")
    nextStationOrder(station)
    updOrder.map(_._1)
  }

  def nextStationOrder(station: String) = {
    val nextOrder = orders.find(o =>
      o.stations.keySet.contains(station) && (!orderCompleted.contains(o) || !orderCompleted(o).completedStations.contains(station))
    )
    val ao = nextOrder.flatMap(o => activateOrder(station, o))
    if (ao.isEmpty) {
      println(s"No more orders for station: $station")
      activeStations -= station
    }

    ao.foreach(startStationOrder)
  }

  def activateOrder(station: String, order: SPOrder) = {
    val sop = for {
      id <- order.stations.get(station)
      sopSpec <- order.idMap.get(id) if sopSpec.isInstanceOf[SOPSpec]
    } yield sopSpec.asInstanceOf[SOPSpec]
    val ao = sop.map(s => ActiveOrder(station, order, s, None))
    ao.foreach(a => activeStations = activeStations + (station -> a))

    if (ao.isEmpty) println(s"Wrong in activeOrder: $station, $order")
    ao
  }

  def updateActiveOrder(station: String, step: ID) = {
    val ao = activeStations.get(station).map(_.copy(step = Some(step)))
    ao.foreach(a => activeStations = activeStations + (station -> a))
    if (ao.isEmpty) println(s"Wrong in update Order: $station, $activeStations")
    ao
  }

}
