package sp.patientcardsservice

import sp.domain._
import sp.domain.Logic._
import sp.messages._
import sp.messages.Pickles._

import akka.actor._
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import scala.util.{ Try, Success }
import scala.util.Random.nextInt

import sp.patientcardsservice.{API_PatientEvent => api}

class PatientCardsDevice extends Actor with ActorLogging {
  // *****************************************************************************
  // conneting to the pubsub bus using the mediator actor
  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("services", self)
  mediator ! Subscribe("spevents", self)
  mediator ! Subscribe("patient-event-topic", self) // "self" is an actor, goes to actors "recieve()"

  def receive = {
    case mess @ _ if {log.debug(s"ExampleService MESSAGE: $mess from $sender"); false} => Unit

    case x: String => handleRequests(x)

  }

  def handleRequests(x: String): Unit = {
    val mess = SPMessage.fromJson(x)
    matchRequests(mess)
  }

  def matchRequests(mess: Try[SPMessage]) = {
    extractPatientEvent(mess) map { case (h, b) =>
      b match {
        case api.NewPatient(careContactId, patientData, events) => println("new patient: " + careContactId + " -- patient data: " + patientData + " --- events: " + events)
        case api.DiffPatient(careContactId, patientData, newEvents, removedEvents) => println("updated patient: " + careContactId + " -- patient data: " + patientData + " --- newEvents: " + newEvents + " ---- removedEvents: " + removedEvents)
        case api.RemovedPatient(careContactId) => println("removed patient: " + careContactId)
      }
    }
  }

  def extractPatientEvent(mess: Try[SPMessage]) = for {
    m <- mess
    h <- m.getHeaderAs[SPHeader]
    b <- m.getBodyAs[api.PatientEvent]
  } yield (h, b)

  val statusResponse = SPAttributes(
    "service" -> api.attributes.service,
    "api" -> "to be added with macros later",
    "groups" -> List("examples"),
    "attributes" -> api.attributes
  )

  // Sends a status response when the actor is started so service handlers finds it
  override def preStart() = {
    val mess = SPMessage.makeJson(SPHeader(api.attributes.service, "serviceHandler"), statusResponse)
    mess.map(m => mediator ! Publish("spevents", m))
  }
  // *****************************************************************************


  var serviceOn: Boolean = false


  def parseCommand(x: String): Try[api.PatientEvent] =
    SPMessage fromJson(x) flatMap (_.getBodyAs[api.PatientEvent])

    // def handleCommand: api.PatientEvent => Unit = {
    //   println("handleCommand")
    //   // case api.Start() => serviceOn = true
    //   // case api.Stop() => serviceOn = false
    // }

    // def dataMsg() = SPMessage.make(
    //   SPAttributes("from" -> api.attributes.service).addTimeStamp
    //   // api.D3Data(List.fill(7)(nextInt(50)))
    // ).get.toJson

    // def receive = {
    //   case "tick" => if(serviceOn) mediator ! Publish("d3ExampleAnswers", dataMsg())
    //   case x: String => parseCommand(x) foreach handleCommand
    //   case z => println(s"PatientCardsDevice didn't recognize $z")
    // }



  }

  object PatientCardsDevice {
    def props = Props(classOf[PatientCardsDevice])
    // val service = "patientCardsService"
  }