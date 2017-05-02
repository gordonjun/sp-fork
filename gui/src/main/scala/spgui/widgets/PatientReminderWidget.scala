package spgui.widgets

import java.time._
import java.time.OffsetDateTime

import spgui.circuit.SPGUICircuit
// import java.time.temporal._
import java.time.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.UUID

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.ReactDOM

import spgui.SPWidget
import spgui.widgets.css.{WidgetStyles => Styles}
import spgui.communication._

import sp.domain._
import sp.messages._
import Pickles._

import scala.concurrent.duration._
import scala.scalajs.js
import scala.util.{ Try, Success }
import scala.util.Random.nextInt
import scala.collection.mutable.ListBuffer

import org.scalajs.dom
import org.scalajs.dom.raw
import org.scalajs.dom.{svg => *}
import org.singlespaced.d3js.d3
import org.singlespaced.d3js.Ops._

import scalacss.ScalaCssReact._
import scalacss.Defaults._

import spgui.widgets.{API_PatientEvent => api}
import spgui.widgets.{API_Patient => apiPatient}

object PatientReminderWidget {

  private class Backend($: BackendScope[Unit, Map[String, apiPatient.Patient]]) {

    val messObs = spgui.widgets.akuten.PatientModel.getPatientObserver(
      patients => {
        $.modState{s =>
          patients
        }.runNow()
      }
    )


    send(api.GetState())


    def send(mess: api.StateEvent) {
      val json = ToAndFrom.make(SPHeader(from = "PatientCardsWidget", to = "PatientCardsService"), mess)
      BackendCommunication.publish(json, "patient-cards-service-topic")
    }

    /**
    * Checks if the patient belongs to this team.
    */
    def belongsToThisTeam(patient: apiPatient.Patient, filter: String): Boolean = {
      patient.team.team.contains(filter)
    }
    /**
    * Returns the correct hex color for each priority.
    */
    def decodeTriageColor(p: apiPatient.Priority): String = {
      p.color match {
        case "NotTriaged" => "#D5D5D5"
        case "Blue" => "#538AF4"
        case "Green" => "#009550" //"prio4"
        case "Yellow" => "#EAC706" //"prio3"
        case "Orange" => "#F08100" //"prio2"
        case "Red" => "#950000" //"prio1"
        case _ =>  {
          println("TriageColor: "+ p.color +" not expected in PatientReminderWidget")
          return "#D5D5D5" //"prioNA"
        }
      }
    }
    /*
    Takes an AttendedEvent and returns a tuple containing a bool declaring wether
    icon should be filled or not as we as a string containing text to be shown
    **/
    def decodeAttended(a: apiPatient.Attended): (Boolean, String) = {
      if (a.attended)
      (true, a.doctorId)
      else
      (false, "Ej Påtittad")
    }
    /**
    Decodes the initial background and icon colors of the progress bar. Tuple values as follows:
    (initial background, initial symbol)
    */
    def progressBarInitialColoring(p: apiPatient.Priority): (String, String) = {
      p.color match {
        case "NotTriaged" => ("#E0E0E0", "#AFAFAF")
        case "Blue" => ("#DDE8FF", "#538AF4")
        case "Green" => ("#F5FAF8", "#DCE2DF")
        case "Yellow" => ("#FFED8D", "#EAC706")
        case "Orange" => ("#FCC381", "#F08100")
        case "Red" => ("#D99898", "#950000")
        case _ =>  {
          println("TriageColor: "+ p.color +" not expected in PatientReminderWidget")
          return ("#E0E0E0", "#AFAFAF") //"NotTriaged"
        }
      }
    }
    /**
    Decodes the background and icon colors of the progress bar when stages are completed.
    Returns List as follows:
    List((attended background color, attended symbol color, patient is attended),
    (plan background color, plan symbol color, plan does exist),
    (finished background color, finished symbol color, patient is finished))
    */
    def progressBarColoring(p: apiPatient.Patient): List[(String, String, Boolean)] = {
      val coloring = ListBuffer[(String, String, Boolean)]()
      val initColoring = progressBarInitialColoring(p.priority)

      if (decodeAttended(p.attended)._1) coloring += Tuple3("#8D47AA", "#FFFFFF", true)
      else coloring += Tuple3(initColoring._1, initColoring._2, false)

      if (false) coloring += Tuple3("#E9B7FF", "#FFFFFF", true) // To be implemented: Plan exists
      else coloring += Tuple3(initColoring._1, initColoring._2, false)

      if (p.finished.finishedStillPresent) coloring += Tuple3("#47AA62", "#FFEDFF", true)
      else coloring += Tuple3(initColoring._1, initColoring._2, false)

      coloring.toList
    }

    /**
    * Converts milliseconds to hours and minutes, visualized in string.
    */
    def getTimeDiffReadable(milliseconds: Long): (String, String) = {
      val minutes = ((milliseconds / (1000*60)) % 60)
      val hours = ((milliseconds / (1000*60*60)) )// % 24)

      val timeString = (hours, minutes) match {
        case (0,_) => {if (minutes == 0) "" else (minutes + " min").toString}
        case _ => (hours + " h " + minutes + " m").toString
      }
      val days = (milliseconds / (1000*60*60*24))
      val dayString = days match {
        case 0 => ""
        case (n: Long) => "(+ " + n + " dygn)"
      }
      (timeString, dayString)
    }

    /*
    * Returns true if a patient has waited longer than is recommended according
    * to triage priority.
    * Prio red: immediately, Prio orange: 20 min, Prio yellow or green: 60 min.
    **/
    def hasWaitedTooLong(p: apiPatient.Patient) = {
      p.priority.color match {
        case "Green" | "Yellow" => if (p.latestEvent.timeDiff > 1200000) true else false
        case "Orange" => if (p.latestEvent.timeDiff > 3600000) true else false
        case "Red" => true
        case _ => false
      }
    }

    /*
    * Specifies a patientCard in SVG for scalajs-react based on a Patient.
    **/
    def patientCard(p: apiPatient.Patient) = {
      val cardScaler = 0.99 // Use this to scale cards

      val cardWidth = 307 // change only with new graphics
      val cardHeight = 72 // change only with new graphics

      val fontSizeSmall = 10
      val fontSizeMedium = 20
      val fontSizeLarge = 58

      val cardBackgroundColor = "#ffffff"
      val contentColorDark = "#000000"
      val contentColorLight = "#ffffff"
      val contentColorAttention = "#E60000"
      val delimiterColor = "#95989a"
      val shadowColor = "lightgrey"

      <.svg.svg( //ARTO: Skapar en <svg></svg>-tagg att fylla med objekt
        ^.key := p.careContactId,
        ^.`class` := "patient-card-canvas",
        ^.svg.width := (cardScaler * cardWidth * 1.04).toString,
        ^.svg.height := (cardScaler * cardHeight * 1.04).toString,
        ^.svg.viewBox := "0 0 "+ (cardWidth + 4).toString +" "+ (cardHeight + 4).toString,
        // ^.svg.transform := "scale(" + cardScaler + ")",
        ^.svg.id := p.careContactId,
        <.svg.g(
          ^.`class` := "patient-card-graphics",
          //^.svg.transform := "translate(0,0)",
          <.svg.rect(
            ^.`class` := "shadow",
            ^.svg.y := "2",
            ^.svg.x := "2",
            ^.svg.height := cardHeight.toString,
            ^.svg.width := cardWidth.toString,
            ^.svg.fill := shadowColor
          ),
          <.svg.rect(
            ^.`class` := "bg-field",
            ^.svg.y := 0,
            ^.svg.x := 0,
            ^.svg.height := cardHeight,
            ^.svg.width := cardWidth,
            ^.svg.fill := cardBackgroundColor
          ),
          <.svg.path(
            ^.`class` := "triage-field",
            ^.svg.d := "m -8.3529763e-4,0.04154422 108.15776529763,0 0,72.66000378 -108.15776529763,0 z",
            ^.svg.fill := decodeTriageColor(p.priority)
          ),
          <.svg.path(
            ^.`class` := "clock-symbol",
            ^.svg.d := "m 236.88792,15.039552 -1.366,0 0,5.465 4.782,2.869 0.683,-1.12 -4.1,-2.432 z m -0.463,-4.554 a 9.108,9.108 0 1 0 9.117,9.108 9.1,9.1 0 0 0 -9.117,-9.108 z m 0.01,16.394 a 7.286,7.286 0 1 1 7.286,-7.286 7.284,7.284 0 0 1 -7.287,7.286 z",
            ^.svg.fill := contentColorDark
          ),
          <.svg.path(
            ^.`class` := "timeline-attended-bg",
            ^.svg.d := "m 35.252165,72.701548 -35.2530003,0 0,-7 35.2520003,0 0,0.028 0,0 3.413,3.486 -3.412,3.486 z",
            ^.svg.fill := progressBarColoring(p).head._1
          ),
          <.svg.path(
            ^.`class` := "timeline-plan-bg",
            ^.svg.d := "m 72.180165,72.701548 -36,0 3.485,-3.486 -3.485,-3.485 36,-0.028 3.487,3.514 -3.487,3.485 z",
            ^.svg.fill := progressBarColoring(p).tail.head._1
          ),
          <.svg.path(
            ^.`class` := "timeline-finished-bg",
            ^.svg.d := "m 108.18116,72.701548 -34.999995,0 3.484,-3.484 -3.484,-3.516 34.999995,0 0,7 z",
            ^.svg.fill := progressBarColoring(p).tail.tail.head._1
          )
        ),
        <.svg.g(
          ^.`class` := "patient-card-text",

          <.svg.text(
            ^.`class` := "room-nr",
            ^.svg.y := "53.462563",
            ^.svg.x := "54.5",
            ^.svg.textAnchor := "middle",
            ^.svg.fontSize :=  fontSizeLarge + "px",
            ^.svg.fill := contentColorLight,
            p.location.roomNr
          ),
          <.svg.text(
            ^.`class` := "header-latest-event",
            ^.svg.y := "23.813511",
            ^.svg.x := "119.74184",
            ^.svg.textAnchor := "start",
            ^.svg.fontSize :=  fontSizeSmall + "px",
            ^.svg.fill := contentColorDark,
            if (p.latestEvent.latestEvent != "") "Senaste händelse"
            else "Ingen senaste händelse"
          ),
          <.svg.text(
            ^.`class` := "latest-event",
            ^.svg.y := "51",
            ^.svg.x := "118.19789",
            ^.svg.textAnchor := "start",
            ^.svg.fontSize := fontSizeMedium  + "px",
            Styles.freeSansBold,
            ^.svg.fill := contentColorDark,
            p.latestEvent.latestEvent.toUpperCase
          ),
          <.svg.text(
            ^.`class` := "time-since-latest-event",
            ^.svg.y := "23.813511",
            ^.svg.textAnchor := "start",
            ^.svg.fontSize :=  fontSizeSmall + "px",
            ^.svg.fill := { if (hasWaitedTooLong(p)) contentColorAttention else contentColorDark },
            <.svg.tspan(^.svg.x := "251")(getTimeDiffReadable(p.latestEvent.timeDiff)._1)
            //<.svg.tspan(^.svg.x := "252", ^.svg.dy := "15 px")(getTimeDiffReadable(p.latestEvent.timeDiff)._2)
          )
        )
      )
    }

    /*
    Sorts a Map[String, Patient] by time since latest event and returns a list of sorted ccids.
    Patients missing latest event are placed last and sorted by careContactId.
    **/
    def sortPatientsByLatestEvent(pmap: Map[String, apiPatient.Patient]): List[String] = {
      val currentCcids = pmap.map(p => p._1)
      val ccidsSortedByLatestEvent = ListBuffer[(String, Long)]()
      val ccidsMissingLatestEvent = ListBuffer[(String, String)]()

      currentCcids.foreach{ ccid =>
        if (pmap(ccid).latestEvent.timeDiff == 0) ccidsMissingLatestEvent += Tuple2(ccid, ccid)
        else ccidsSortedByLatestEvent += Tuple2(ccid, pmap(ccid).latestEvent.timeDiff)
      }
      (ccidsSortedByLatestEvent.sortBy(_._2) ++ ccidsMissingLatestEvent.sortBy(_._2)).map(p => p._1).toList.reverse
    }


    val globalState = SPGUICircuit.connect(x => (x.openWidgets.xs, x.globalState))

    def render(pmap: Map[String, apiPatient.Patient]) = {

      globalState{x =>
        val filter = x()._2.attributes.get("team").map(x => x.str).getOrElse("medicin")
        val pats = (pmap - "-1").filter(p => belongsToThisTeam(p._2, filter))
        var numberToDraw = 4

        <.div(^.`class` := "card-holder-root", Styles.helveticaZ)(
          <.div(^.`class` := "widget-header", Styles.widgetHeader)(
            <.svg.text(
              "LÅNG TID SEDAN HÄNDELSE"
            )
          ),
          sortPatientsByLatestEvent(pats).take(numberToDraw) map { ccid =>
            patientCard(pats(ccid))
          },

          <.div(^.`class` := "number-not-shown", Styles.widgetText)(
            <.svg.text(
              ^.fontWeight.bold,
              if ((pats.size - numberToDraw) >= 0) (pats.size - numberToDraw)
              else pats.size
            ),
            <.svg.text(
              " till i kö"
            )
          )
        )
      }

    }

    def onUnmount() = {
      println("Unmounting")
      messObs.kill()
      Callback.empty
    }
  }

  private val cardHolderComponent = ReactComponentB[Unit]("cardHolderComponent")
  .initialState(Map("-1" ->
    apiPatient.Patient(
      "4502085",
      apiPatient.Priority("NotTriaged", "2017-02-01T15:49:19Z"),
      apiPatient.Attended(true, "sarli29", "2017-02-01T15:58:33Z"),
      apiPatient.Location("52", "2017-02-01T15:58:33Z"),
      apiPatient.Team("GUL", "NAKME", "2017-02-01T15:58:33Z"),
      apiPatient.Examination(false, "2017-02-01T15:58:33Z"),
      apiPatient.LatestEvent("OmsKoord", -1, false, "2017-02-01T15:58:33Z"),
      apiPatient.ArrivalTime("", "2017-02-01T10:01:38Z"),
      apiPatient.Finished(false, false, "2017-02-01T10:01:38Z")
    )))
    .renderBackend[Backend]
    .componentWillUnmount(_.backend.onUnmount())
    .build

    def apply() = spgui.SPWidget(spwb => {
      cardHolderComponent()
    })
  }
