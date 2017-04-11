package sp.patientcardsservice

import sp.domain._
import sp.domain.Logic._
import sp.messages._
import sp.messages.Pickles._

import akka.actor._
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import scala.util.{ Try, Success, Failure }

import scala.collection.mutable.ListBuffer

import java.util.Date
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.chrono.ChronoLocalDate

import sp.patientcardsservice.{API_PatientEvent => api}

class PatientCardsDevice extends Actor with ActorLogging {
  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("services", self)
  mediator ! Subscribe("spevents", self)
  mediator ! Subscribe("patient-event-topic", self)

  /**
  Receives incoming messages on the AKKA-bus
  */
  def receive = {
    case mess @ _ if {log.debug(s"PatientCardsService MESSAGE: $mess from $sender"); false} => Unit
    case x: String => handleRequests(x)
  }

  /**
  Handles the received message and sends it further
  */
  def handleRequests(x: String): Unit = {
    val mess = SPMessage.fromJson(x)
    matchRequests(mess)
  }

  /**
  Identifies the body of the SP-message and acts correspondingly
  */
  def matchRequests(mess: Try[SPMessage]) = {
    val header = SPHeader(from = "patientCardsService")
    PatientCardsComm.extractPatientEvent(mess) map { case (h, b) =>
      b match {
        case api.NewPatient(careContactId, patientData, events) => {
          for (patientProperty <- extractNewPatientProperties(api.NewPatient(careContactId, patientData, events))) {
            publishOnAkka(header, patientProperty)
          }
        }
        case api.DiffPatient(careContactId, patientDataDiff, newEvents, removedEvents) => {
          for (patientProperty <- extractDiffPatientProperties(api.DiffPatient(careContactId, patientDataDiff, newEvents, removedEvents))) {
            publishOnAkka(header, patientProperty)
          }
        }
        case api.RemovedPatient(careContactId, timestamp) => {
          println("REMOVED PATIENT: PatientProps to send: ")
          val toSend = api.Finished(careContactId, timestamp)
          println(toSend)
          println()
          println()
          publishOnAkka(header, toSend)
        }
      }
    }
  }

  /**
  Takes a NewPatient and returns PatientProperties based on patient data and events.
  */
  def extractNewPatientProperties(patient: api.NewPatient): List[api.PatientProperty] = {
    val filteredPatientProps = filterNewPatientProperties(patient, getNewPatientProperties(patient))
    println("NEW PATIENT: PatientProps to send: ")
    println(filteredPatientProps)
    println()
    println()
    return filteredPatientProps
  }

  /**
  Takes a DiffPatient and returns PatientProperties based on updates and new events.
  */
  def extractDiffPatientProperties(patient: api.DiffPatient): List[api.PatientProperty] = {
    val filteredPatientProps = filterDiffPatientProperties(patient, getDiffPatientProperties(patient))
    println("DIFF PATIENT: PatientProps to send: ")
    println(filteredPatientProps)
    println()
    println()
    return filteredPatientProps
  }

  /**
  Takes a NewPatient and extracts PatientProperties based on patient data and events.
  */
  def getNewPatientProperties(patient: api.NewPatient): List[api.PatientProperty] = {
    var patientPropertyBuffer = new ListBuffer[api.PatientProperty]()
    patient.patientData.foreach{ p =>
      p._1 match {
        case "Location" => if (!fieldEmpty(p._2)) patientPropertyBuffer += updateLocation(patient.careContactId, patient.patientData("timestamp"), p._2)
        case "Team" => if (!fieldEmpty(p._2)) patientPropertyBuffer += updateTeam(patient.careContactId, patient.patientData("timestamp"), p._2, patient.patientData("ReasonForVisit"), patient.patientData("Location"))
        case "Priority" => if (!fieldEmpty(p._2)) patientPropertyBuffer += updatePriority(patient.careContactId, patient.patientData("timestamp"), p._2)
        case "VisitRegistrationTime" => updateArrivalTime(patient.careContactId, p._2)
        case _ => patientPropertyBuffer += api.Undefined(patient.careContactId, "0000-00-00T00:00:00.000Z")
      }
    }
    patientPropertyBuffer += updateAttended(patient.careContactId, patient.events)
    patientPropertyBuffer += updateLatestEvent(patient.careContactId, patient.events)
    return patientPropertyBuffer.toList
  }

  /**
  Takes a DiffPatient and extracts PatientProperties based on updates and events.
  */
  def getDiffPatientProperties(patient: api.DiffPatient): List[api.PatientProperty] = {
    var patientPropertyBuffer = new ListBuffer[api.PatientProperty]()
    patient.patientData.foreach{ p =>
      p._1 match {
        case "Location" => if (!fieldEmpty(p._2)) patientPropertyBuffer += updateLocation(patient.careContactId, patient.patientData("timestamp"), p._2)
        case "Team" => if (!fieldEmpty(p._2)) patientPropertyBuffer += updateTeam(patient.careContactId, patient.patientData("timestamp"), p._2, patient.patientData("ReasonForVisit"), patient.patientData("Location"))
        case _ => patientPropertyBuffer += api.Undefined(patient.careContactId, "0000-00-00T00:00:00.000Z")
      }
    }
    patient.newEvents.foreach{ e =>
      if (e("Title") == "Läkare") {
        patientPropertyBuffer += api.Attended(patient.careContactId, e("Start"), true, e("Value"))
      }
    }
    patientPropertyBuffer += getLatestPrioEvent(patient.careContactId, patient.newEvents)
    patientPropertyBuffer += updateLatestEvent(patient.careContactId, patient.newEvents)
    return patientPropertyBuffer.toList
  }

  /**
  Filters out unwanted patient properties.
  */
  def filterNewPatientProperties(patient: api.NewPatient, patientProperties: List[api.PatientProperty]): List[api.PatientProperty] = {
    patientProperties
      .filter(_ != (api.Undefined(patient.careContactId, "0000-00-00T00:00:00.000Z")))
      .filter(_ != api.LatestEvent(patient.careContactId, "-1", "NA"))
      .filter(_ != api.Attended(patient.careContactId, "-1", false, "NA"))
  }

  /**
  Filters out unwanted patient properties.
  */
  def filterDiffPatientProperties(patient: api.DiffPatient, patientProperties: List[api.PatientProperty]): List[api.PatientProperty] = {
    patientProperties
      .filter(_ != (api.Undefined(patient.careContactId, "0000-00-00T00:00:00.000Z")))
      .filter(_ != api.LatestEvent(patient.careContactId, "-1", "NA"))
      .filter(_ != api.Attended(patient.careContactId, "-1", false, "NA"))
  }

  /**
  Identifies the latest event considering triage and returns corresponding PriorityEvent.
  OBS: revidera
  */
  def getLatestPrioEvent(careContactId: String, events: List[Map[String, String]]): api.PatientProperty = {
    var prio = "NA"
    var latestEventString = "NA"
    var formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
    val tmp = new SimpleDateFormat("yyyy-MM-dd'T'hh:MM:ss'Z'").format(new Date())
    var startOfLatestEvent = new SimpleDateFormat("yyyy-MM-dd'T'hh:MM:ss'Z'").parse(tmp)
    var latestEventSet: Boolean = false
    var prioEventBuffer = new ListBuffer[Map[String, String]]()
    events.foreach{ e =>
      if (isValidTriageColor(e("Title"))) {
        val startOfEvent = formatter.parse(e("Start").replaceAll("Z$", "+0000"))
        if (latestEventSet) {
          if (startOfEvent.after(startOfLatestEvent)) {
            startOfLatestEvent = startOfEvent
            latestEventString = e("Start")
            prio = e("Title")
          }
        } else {
          startOfLatestEvent = startOfEvent
          latestEventString = e("Start")
          prio = e("Title")
          latestEventSet = true
        }
      }
    }
    if (prio != "NA" && latestEventString != "NA") {
      return updatePriority(careContactId, latestEventString, prio)
    }
    return api.Undefined(careContactId, "0000-00-00T00:00:00.000Z")
  }

  /**
  Discerns team and klinik, returns a Team-type.
  */
  def updateTeam(careContactId: String, timestamp: String, clinic: String, reasonForVisit: String, location: String): api.Team = {
    api.Team(careContactId, timestamp, decodeTeam(reasonForVisit, location), decodeClinic(clinic))
  }

  /**
  Returns an ArrivalTime-type.
  */
  def updateArrivalTime(careContactId: String, timestamp: String): api.ArrivalTime = {
    api.ArrivalTime(careContactId, timestamp)
  }



  /**
  Cleans up Location-value and returns a RoomNr-type.
  */
  def updateLocation(careContactId: String, timestamp: String, location: String): api.RoomNr = {
    api.RoomNr(careContactId, timestamp, decodeLocation(location))
  }

  /**
  Checks if patient has been attended by doctor, returns an Attended-type.
  */
  def updateAttended(careContactId: String, events: List[Map[String, String]]): api.Attended = {
    events.foreach{ e =>
      if (e("Title") == "Läkare" || e("Title") == "läkare") {
        return api.Attended(careContactId, e("Start"), true, e("Value"))
      }
    }
    return api.Attended(careContactId, "-1", false, "NA")
  }

  /**
  Discerns priority and returns corresponding PriorityEvent-type
  */
  def updatePriority(careContactId: String, timestamp: String, priority: String): api.PriorityEvent = {
    priority match {
      case "Grön" => api.Green(careContactId, timestamp)
      case "Gul" =>  api.Yellow(careContactId, timestamp)
      case "Orange" => api.Orange(careContactId, timestamp)
      case "Röd" => api.Red(careContactId, timestamp)
      case _ => api.NotTriaged(careContactId, timestamp)
    }
  }

  /**
  Identifies the latest event if there is any in the events list, returns LatestEvent-type
  */
  def updateLatestEvent(careContactId: String, events: List[Map[String, String]]): api.LatestEvent = {
    var eventFound: Boolean = false
    var timeDiff: Long = Long.MaxValue
    var title = ""
    var timestampString = ""
    // Get the latest event title and its time diff
    events.foreach{ e =>
      val startOfEventString = e("Start")
      var formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
      val startOfEvent = formatter.parse(startOfEventString.replaceAll("Z$", "+0000"))
      val nowString = new SimpleDateFormat("yyyy-MM-dd'T'hh:MM:ss'Z'").format(new Date())
      val now = new SimpleDateFormat("yyyy-MM-dd'T'hh:MM:ss'Z'").parse(nowString)
      val diff: Long = now.getTime() - startOfEvent.getTime() // returns diff in millisec
      if (diff < timeDiff) {
        timeDiff = diff
        timestampString = startOfEventString
        eventFound = true
        if (e("Title") == "" || e("Title") == " ") {
          title = "> ej angett <"
        } else {
          if (e("Title") == "Grön" || e("Title") == "Gul" || e("Title") == "Orange" || e("Title") == "Röd") {
            title = "Triage"
          } else {
            title = e("Title")
          }
        }
      }
    }
    if (eventFound) {
      return api.LatestEvent(careContactId, timestampString, title)
    }
    return api.LatestEvent(careContactId, "-1", "NA")
  }

  /**
  Filters out a room nr or "ivr" from a location
  */
  def decodeLocation(location: String): String = {
    if (location contains "ivr") {
      return "ivr"
    }
    return location.replaceAll("[^0-9]","")
  }

  /**
  Discerns clinic from Team-field.
  Used by updateTeam().
  */
  def decodeClinic(clinic: String): String = {
    clinic match {
      case "NAKKI" => "kirurgi"
      case "NAKME" => "medicin"
      case "NAKOR" => "ortopedi"
      case "NAKBA" | "NAKGY" | "NAKÖN" => "bgö"
    }
  }

  /**
  Discerns team from ReasonForVisit and Location-fields.
  Used by updateTeam().
  */
  def decodeTeam(reasonForVisit: String, location: String): String = {
    (reasonForVisit, location.charAt(0)) match {
      case ("APK", location) => "Streamteam"
      case (reasonForVisit, 'B') => "Blå"
      case (reasonForVisit, 'G') => "Gul"
      case (reasonForVisit, 'P') => "Process"
      case _ => "Röd"
    }
  }

  /**
  Checks if string is valid triage color.
  */
  def isValidTriageColor(string: String): Boolean = {
    if (string == "Grön" || string == "Gul" || string == "Orange" || string == "Röd") {
      return true
    }
    return false
  }

  /**
  Checks if given field is empty or not.
  */
  def fieldEmpty(field: String): Boolean = {
    if (field == "") {
      return true
    }
    return false
  }

  /**
  Publishes a SPMessage on bus with header and body
  */
  def publishOnAkka(header: SPHeader, body: api.PatientProperty) {
    val toSend = PatientCardsComm.makeMess(header, body)
    toSend match {
      case Success(v) =>
        mediator ! Publish("patient-cards-widget-topic", v) // Publishes on bus for widget to receive
      case Failure(e) =>
        println("Failed")
    }
  }

  }

  object PatientCardsDevice {
    def props = Props(classOf[PatientCardsDevice])
  }
