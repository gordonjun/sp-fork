package sp.devicehandler

import java.util.UUID

import akka.actor._
import sp.domain._
import sp.domain.Logic._

import scala.concurrent.Future
import akka.util._
import akka.pattern.ask
import akka.persistence._

import scala.concurrent._
import scala.concurrent.duration._
import scala.util.Properties
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Put, Subscribe}
import java.util.concurrent.TimeUnit

import org.joda.time.DateTime
import sp.messages._
import Pickles._

import scala.util.{Failure, Success, Try}




// to be able to use opcua runtime api
package APIVirtualDevice {
  sealed trait Requests
  // requests setup
  case class SetUpDeviceDriver(driver: Driver) extends Requests
  case class SetUpResource(resource: Resource) extends Requests

  // Add new transformers here as needed.
  sealed trait DriverStateMapper
  case class OneToOneMapper(driver: String, key: String) extends DriverStateMapper
  case class OneToOneNestedMapper(driver: String, key: List[String]) extends DriverStateMapper
  case class OneToOneNewKeyMapper(driver: String, key: String, newKey: String) extends DriverStateMapper


  // requests command (gets a SPACK and when applied, SPDone (and indirectly a StateEvent))
  case class ResourceCommand(resource: String, stateRequest: Map[String, SPValue], timeout: Int = 0) extends Requests

  // requests from driver
  case class DriverStateChange(name: String, id: UUID, state: Map[String, SPValue], diff: Boolean = false)  extends Requests


  // answers
  sealed trait Replies
  case class StateEvent(resource: String, id: UUID, state: Map[String, SPValue], diff: Boolean = false) extends Replies

  case class Resources(xs: List[Resource]) extends Replies
  case class Drivers(xs: List[Driver]) extends Replies
  case class NewResource(x: Resource) extends Replies
  case class RemovedResource(x: Resource) extends Replies
  case class NewDriver(x: Driver) extends Replies
  case class RemovedDriver(x: Driver) extends Replies


  case class Resource(name: String, id: UUID, stateMap: List[DriverStateMapper], setup: SPAttributes, sendOnlyDiffs: Boolean = false)
  case class Driver(name: String, id: UUID, driverType: String, setup: SPAttributes)


  object attributes {
    val service = "virtualDevice"
  }
}
import sp.devicehandler.{APIVirtualDevice => api}


object VirtualDevice {
  def props(name: String, id: UUID) = Props(classOf[VirtualDevice], name, id)
}



class VirtualDevice(name: String, id: UUID) extends PersistentActor with ActorLogging {
  override def persistenceId = id.toString

  import context.dispatcher
  val mediator = DistributedPubSub(context.system).mediator

  mediator ! Subscribe("services", self)
  mediator ! Subscribe("spevents", self)


  override def receiveCommand = {
    case x: String =>


  }

  def receiveRecover = {
    case x: String =>

    case RecoveryCompleted =>

  }

  val statusResponse = SPAttributes(
    "service" -> name,
    "instanceID" -> id,
    "groups" -> List("devices"),
    "attributes" -> api.attributes
  )

}


trait VirtualDeviceLogic {
  val name: String
  val id: UUID

  type StateMap = Map[String, SPValue]
  case class StateMapper(f: (Map[String, StateMap], Map[String, StateMap]) =>  Map[String, StateMap])
  val defMapper = StateMapper { (in, out) =>
    val current = out(name)
    val upd = in.foldLeft(current){(a, b) =>
      val prefixState = b._2.map(kv => s"${b._1}.${kv._1}"->kv._2)

    }

  }
  val defResource = api.Resource(name, id, List(), SPAttributes, false)


  var drivers: Map[String, api.Driver] = Map()
  var driverState: Map[String, StateMap] = Map()
  var resources: Map[String, api.Resource] = Map(defResource.name -> defResource)
  var resourceState: Map[String, StateMap] = Map()

  var driverToResMapper: List[StateMapper] = List()
  var resToDriverMapper: List[StateMapper] = List()



  def newDriver(d: api.Driver) = {
    drivers += d.name -> d
  }

  def newResource(resource: api.Resource) = {

  }

  def driverEvent(e: api.DriverStateChange) = {

  }



}

trait VDDriverComm {
  val context: ActorContext
  import context.dispatcher
  val mediator: ActorRef


}

trait VDMappers {
  type StateMap = Map[String, SPValue]
  def flatMapState(thingState: Map[String, State], prefix: Boolean) = {
    thingState.foldLeft(Map[String, SPValue]()){(a, b) =>
      val pref = if (prefix) b._1+"." else ""
      val prefixState = b._2.map(kv => s"${pref}${kv._1}"->kv._2)
      a ++ prefixState
  }

}