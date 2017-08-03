package sp.example

import akka.actor._

import scala.util.{Failure, Random, Success, Try}
import sp.domain._
import sp.domain.Logic._



// The messages that this service can send and receive is defined as case classes
// Messages you can send to me
/**
  * Adds a new pie to the memory with an id
  * @param id an UUID identifying the pie
  */
case class APIExampleService_StartTheTicker(id: java.util.UUID) extends APIExampleService.Request

/**
  * removes the pie with the id
  * @param id an UUID identifying the pie
  */
case class APIExampleService_StopTheTicker(id: java.util.UUID) extends APIExampleService.Request

/**
  * Changes the pie to the given map
  * @param id  an UUID identifying the pie
  * @param map A map representing a pie
  */
case class APIExampleService_SetTheTicker(id: java.util.UUID, map: Map[String, Int]) extends APIExampleService.Request
case class APIExampleService_GetTheTickers() extends APIExampleService.Request
case class APIExampleService_ResetAllTickers() extends APIExampleService.Request


// Messages that I will send as answer
case class APIExampleService_TickerEvent(map: Map[String, Int], id: java.util.UUID) extends APIExampleService.Response
case class APIExampleService_TheTickers(ids: List[java.util.UUID]) extends APIExampleService.Response


// this object is used for converting to and from json.
object APIExampleService {
  sealed trait Request
  sealed trait Response

  implicit lazy val readReq = deriveReadISA[Request]
  implicit lazy val writeReq = deriveWriteISA[Request]
  implicit lazy val readResp = deriveReadISA[Response]
  implicit lazy val writeResp = deriveWriteISA[Response]

  val service = "exampleService"
  val attributes  = SPAttributes(
    "service" -> service,
    "version" -> 1,
    "api" -> "to be fixed by macros"
  )
}






/**
  *  This is the actor (the service) that listens for messages on the bus
  *  It keeps track of a set of Pie diagrams that is updated every second
  */
class ExampleService extends Actor with ActorLogging with ExampleServiceLogic {

  // connecting to the pubsub bus using the mediator actor
  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Put, Send, Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("services", self)
  mediator ! Subscribe("spevents", self)


  // The method that receive messages. Add service logic in a trait so you can test it. Here the focus in on parsing
  // and on the messages on the bus
  def receive = {
    case mess @ _ if {log.debug(s"ExampleService MESSAGE: $mess from $sender"); false} => Unit

    case "tick" =>
      val upd = tick  // Updated the pies on a tick
      tick.foreach{ e =>
        val header = SPAttributes(
          "from" -> APIExampleService.service,
          "reqID" -> e.id
        ).addTimeStamp
        val mess = SPMessage.makeJson(header, e)
        mediator ! Publish("answers", mess)  // sends out the updated pies
      }

    case x: String =>
      // SPMessage uses the APIParser to parse the json string

      val message = SPMessage.fromJson(x)

      // extract the body if it is an case class from my api as well as the header.to has my name
      // act on the messages from the API. Always add the logic in a trait to enable testing
      // we need to keep the old header, that is why we need to make the message using m here and
      // use the m.make(...) below
      val bodyAPI = for {
        m <- message
        h <- m.getHeaderAs[SPHeader] if h.to == APIExampleService.service  // only extract body if it is to me
        b <- m.getBodyAs[APIExampleService.Request]
      } yield {
        val toSend = commands(b) // doing the logic
        val spHeader = h.copy(from = APIExampleService.service) // upd header put keep most info

        // We must do a pattern match here to enable the json conversion (SPMessage.make. Or the command can return pickled bodies
        toSend.foreach{
          case x: APIExampleService.Request =>
              mediator ! Publish("answers", m.makeJson(spHeader, x))
          case x: APISP =>
              mediator ! Publish("answers", m.makeJson(spHeader, x))
        }
      }

      // Extract the body if it is a StatusRequest
      val bodySP = for {m <- message; b <- m.getBodyAs[APISP.StatusRequest]} yield b





      // reply to a statusresponse
      for {
        m <- message
        h <- m.getHeaderAs[SPHeader]
        b <- m.getBodyAs[APISP.StatusRequest]
      } yield {
        val spHeader = h.copy(from = APIExampleService.service) // upd header put keep most info
        mediator ! Publish("spevents", m.makeJson(spHeader, statusResponse))
      }


  }




  val statusResponse = APISP.StatusResponse(
    APIExampleService.attributes
  )

  // Sends a status response when the actor is started so service handlers finds it
  override def preStart() = {
    val mess = SPMessage.makeJson(SPHeader(from = APIExampleService.service, to = "serviceHandler"), statusResponse)
    mediator ! Publish("spevents", mess)
  }

  // A "ticker" that sends a "tick" string to self every 2 second
  import scala.concurrent.duration._
  import context.dispatcher
  val ticker = context.system.scheduler.schedule(2 seconds, 2 seconds, self, "tick")

}

object ExampleService {
  def props = Props(classOf[ExampleService])
}





/*
 * Using a trait to make the logic testable
 */
trait ExampleServiceLogic {

  // This variable stores the pies that are used by the different widgets
  // Initially, it is empty
  var thePies: Map[java.util.UUID, Map[String, Int]] = Map()

  // Matching and doing the stuff based on the message
  // This method returns multiple messages that will be sent out on the bus
  // Services should start and end with an SPACK and SPDONE if there is a
  // a clear start and end of the message stream (so listeners can unregister)
  def commands(body: APIExampleService.Request) = {
    body match {
      case APIExampleService_StartTheTicker(id) =>
        thePies += id -> Map("first"->10, "second"-> 30, "third" -> 60)
        List(APISP.SPACK(), getTheTickers)
      case APIExampleService_StopTheTicker(id) =>
        thePies -= id
        List(APISP.SPDone(), getTheTickers)
      case APIExampleService_SetTheTicker(id, map) =>
        thePies += id -> map
        List(APISP.SPACK(), getTheTickers)
      case APIExampleService_ResetAllTickers() =>
        thePies = Map()
        List(getTheTickers)
      case x => List(APISP.SPError(s"ExampleService can not understand: $x"))
    }


  }

  def tick = {
    thePies = thePies.map(kv => kv._1 -> updPie(kv._2))
    thePies.map{case (id, p) =>
      APIExampleService_TickerEvent(p, id)
    }.toList
  }

  def getTheTickers = APIExampleService_TheTickers(thePies.keys.toList)


  // Just some logic to make the pies change
  val r = Random
  def updPie(pie: Map[String, Int]) = {
    val no = r.nextInt(20)
    val part = r.nextInt(pie.size)
    val key = pie.keys.toList(part)
    val newPie = pie + (key -> (pie(key) + no))
    norm(newPie)
  }

  def norm(pie: Map[String, Int]) = {
    val sum = pie.foldLeft(1){(a, b) => a + b._2}
    pie.map{case (key, v) => key -> ((v.toDouble / sum)*100).toInt}
  }

}