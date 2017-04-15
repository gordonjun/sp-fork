package spgui.widgets

import java.time._ //ARTO: Använder wrappern https://github.com/scala-js/scala-js-java-time
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
import sp.messages.Pickles._
import upickle._

import org.singlespaced.d3js.d3
import org.singlespaced.d3js.Ops._

import scala.concurrent.duration._
import scala.scalajs.js
import scala.util.{ Try, Success }
import scala.util.Random.nextInt

import org.scalajs.dom
import org.scalajs.dom.raw
import org.scalajs.dom.{svg => *}
import org.singlespaced.d3js.d3
import org.singlespaced.d3js.Ops._

import scalacss.ScalaCssReact._
import scalacss.Defaults._

import spgui.widgets.{API_PatientEvent => api}

object PatientReminderServiceWidget {

  case class Patient(careContactId: String, patientData: Map[String, String])

  private class Backend($: BackendScope[Unit, Map[String, Patient]]) {
    spgui.widgets.css.WidgetStyles.addToDocument()

    val messObs = BackendCommunication.getMessageObserver(
      mess => {
        mess.getBodyAs[api.PatientProperty] map {
          case x => println(s"THIS WAS NOT EXPECTED IN PatientReminderServiceWidget: $x")
        }
      }, "patient-reminder-widget-topic"
    )

    def triageColor(p: String) = {
      p match {
        case "Grön" => "green"//"#289500" //"prio4"
        case "Gul" => "yellow"//"#EAC706" //"prio3"
        case "Orange" => "orange"//"#F08100" //"prio2"
        case "Röd" => "red"//"#950000" //"prio1"
        case _ =>  "grey"//"#D5D5D5" //"prioNA"
      }
    }

    def patientReminder(p: Patient) = {
      val cardHeight = 186
      val cardWidth = cardHeight * 1.7
      val triageFieldWidth = (cardWidth / 3)
      val textLeftAlignment = (triageFieldWidth + (triageFieldWidth / 11))
      val fontSize = (cardHeight / 12)
      val roomNrFontSize = (cardHeight / 3)

      val secondsDiff = ((p.patientData("LatestEventTimeDiff").toLong / (1000)) % 60)
      val minutesDiff = ((p.patientData("LatestEventTimeDiff").toLong / (1000*60)) % 60)
      val hoursDiff = ((p.patientData("LatestEventTimeDiff").toLong / (1000*60*60)) % 24)
      val daysDiff = p.patientData("LatestEventTimeDiff").toLong / (1000*60*60*24)


      <.svg.svg( //ARTO: Skapar en <svg>-tagg att fylla med obekt
        ^.`class` := "patientReminder",
        ^.svg.id := p.careContactId,
        ^.svg.width := cardWidth.toString,
        ^.svg.height := cardHeight.toString,
        <.svg.rect(
          ^.`class` := "bg",
          ^.svg.x := "0",
          ^.svg.y := "0",
          ^.svg.width := cardWidth.toString,
          ^.svg.height := cardHeight.toString,
          ^.svg.fill := "white"
        ),
        <.svg.rect(
          ^.`class` := "triageField",
          ^.svg.x := "0",
          ^.svg.y := "0",
          ^.svg.width := triageFieldWidth.toString,
          ^.svg.height := cardHeight.toString
          //^.svg.fill := triageColor(p.patientData("Priority"))
        ),
        <.svg.rect(
          ^.`class` := "delimiter",
          ^.svg.x := textLeftAlignment,
          ^.svg.y := (cardHeight / 1.24).toString,
          ^.svg.width := (cardWidth - triageFieldWidth - ((textLeftAlignment - triageFieldWidth) * 2)).toString,
          ^.svg.height := "1"
          //^.svg.fill := triageColor(p.patientData("Priority"))
        ),
        <.svg.path(
          ^.`class` := "klinikfield",
          // ^.svg.d := s"M 0,$cardHeight, "+(cardWidth / 4.6).toString+s",$cardHeight, -"+(cardWidth / 4.6).toString+s",0 Z",
          ^.svg.d := s"M 0,$cardHeight, 0,"+(cardHeight - (cardHeight / 2.6)).toString+s", "+(cardHeight / 2.6).toString+s",$cardHeight Z",
          ^.svg.fill := "lightblue"
        ),
        <.svg.path(
          ^.`class` := "teamfield",
          // ^.svg.d := s"M 0,$cardHeight, "+(cardWidth / 4.6).toString+s",$cardHeight, -"+(cardWidth / 4.6).toString+s",0 Z",
          ^.svg.d := s"M 0,$cardHeight, 0,"+(cardHeight - (cardHeight / 3)).toString+s", "+(cardHeight / 3).toString+s",$cardHeight Z",
          ^.svg.fill := "darkseagreen"
        ),
        <.svg.text(
          ^.`class` := "roomNr",
          ^.svg.x := (triageFieldWidth / 2).toString,
          ^.svg.y := (triageFieldWidth - (triageFieldWidth / 2.3)).toString,
          ^.svg.width := triageFieldWidth.toString,
          //^.svg.align := "center",
          ^.svg.textAnchor := "middle",
          ^.svg.fontSize := roomNrFontSize.toString  + "px",
          ^.svg.fill := "white",
          p.patientData("Location")
        ),
        // <.svg.text(
        //   ^.`class` := "careContactId",
        //   ^.svg.x := "100",
        //   ^.svg.y := "40",
        //   ^.svg.fontSize := "47",
        //   ^.svg.fill := "black",
        //   p.careContactId
        // ),
        <.svg.text(
          ^.`class` := "textLatestEvent",
          ^.svg.x := textLeftAlignment.toString,
          ^.svg.y := ((cardHeight / 3) / 3).toString,
          ^.svg.textAnchor := "top",
          ^.svg.fontSize := fontSize.toString  + "px",
          "Senaste händelse"
        ),
        <.svg.text(
          ^.`class` := "latestEvent",
          ^.svg.x := textLeftAlignment.toString,
          ^.svg.y := (cardHeight / 2.8).toString,
          ^.svg.textAnchor := "bottom",
          ^.svg.fontSize := (roomNrFontSize * 0.8).toString  + "px",
          p.patientData("LatestEventTitle")
        ),
        <.svg.svg(
          ^.`class` := "timerSymbol",
          ^.svg.x := textLeftAlignment.toString,
          ^.svg.y := (cardHeight / 2.3).toString,
          //^.svg.viewBox := "0 50 400 400",
          // ^.svg.viewBox := textLeftAlignment.toString+" "+(cardHeight / 2.3).toString+" 40 40",
          <.svg.path(
            ^.svg.d := "m 1.9316099,0.03941873 -0.8388126,0 0,0.27960417 0.8388126,0 0,-0.27960417 z M 1.3724015,1.856846 l 0.2796042,0 0,-0.8388125 -0.2796042,0 0,0.8388125 z M 2.4950124,0.93275411 2.6935313,0.73423513 C 2.6334165,0.66293603 2.5677095,0.59583103 2.4964104,0.53711413 L 2.2978914,0.73563319 C 2.0811982,0.56227853 1.8085841,0.45882498 1.5122036,0.45882498 c -0.69481669,0 -1.25821911,0.56340252 -1.25821911,1.25821892 0,0.6948164 0.56200436,1.2582189 1.25821911,1.2582189 0.6962145,0 1.2582189,-0.5634025 1.2582189,-1.2582189 0,-0.2963805 -0.1034535,-0.5689945 -0.2754101,-0.78428979 z M 1.5122036,2.6956586 c -0.5410343,0 -0.97861487,-0.4375806 -0.97861487,-0.9786147 0,-0.5410341 0.43758057,-0.97861475 0.97861487,-0.97861475 0.5410342,0 0.9786147,0.43758065 0.9786147,0.97861475 0,0.5410341 -0.4375805,0.9786147 -0.9786147,0.9786147 z",
            ^.svg.fill := "black",
            ^.svg.transform := "scale("+(cardHeight / 11).toString+")"

            // <.svg.animateTransform(
            //   ^.svg.`type` := "scale",
            //   ^.svg.attributeType := "XML",
            //   ^.svg.attributeName := "transform",
            //   ^.svg.from := "1 1",
            //   ^.svg.to := "2 2",
            //   ^.svg.dur := "10s"
            // )
          )),
          <.svg.text(
            ^.`class` := "timeSinceLatestEvent",
            ^.svg.x := (textLeftAlignment * 1.48).toString,
            ^.svg.y := (cardHeight / 1.5).toString,
            ^.svg.fontSize := (fontSize * 1).toString  + "px",
            daysDiff + " d " + hoursDiff + " h " + minutesDiff + " m " + secondsDiff + " s "
          ),
          // <.svg.svg(
          //   ^.`class` := "latestActor",
          //   ^.svg.x := (cardWidth / 1.8).toString,
          //   ^.svg.y := (cardHeight / 1.7).toString,
          //   <.svg.path(
          //     ^.`class` := "actorSymbol",
          //     ^.svg.d := "M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z",
          //     ^.svg.fill := "black"
          //   ),
          //   <.svg.text(
          //     ^.svg.x := (textLeftAlignment * 0.2).toString,
          //     ^.svg.y := 20.toString,
          //     ^.svg.fontSize := (cardHeight / 9).toString,
          //     Styles.patientCardText,
          //     p.patientData("CareContactRegistrationTime")
          //   )
          // ),
          <.svg.svg(
            ^.`class` := "arrivalTime",
            ^.svg.x := textLeftAlignment.toString,
            ^.svg.y := (cardHeight / 1.2).toString,
            <.svg.path(
              ^.`class` := "clockSymbol",
              //^.svg.width := cardWidth.toString,
              ^.svg.d := "m 12.510169,6.8983051 -1.530508,0 0,6.1220339 5.35678,3.214068 0.765254,-1.255017 -4.591526,-2.724305 z M 11.989797,1.7966102 C 6.3575254,1.7966102 1.7966102,6.3677288 1.7966102,12 c 0,5.632271 4.5609152,10.20339 10.1931868,10.20339 5.642474,0 10.213593,-4.571119 10.213593,-10.20339 0,-5.6322712 -4.571119,-10.2033898 -10.213593,-10.2033898 z M 12,20.162712 C 7.4901017,20.162712 3.8372881,16.509898 3.8372881,12 3.8372881,7.4901017 7.4901017,3.8372881 12,3.8372881 c 4.509898,0 8.162712,3.6528136 8.162712,8.1627119 0,4.509898 -3.652814,8.162712 -8.162712,8.162712 z",
              ^.svg.fill := "black"
            ),
            <.svg.text(
              ^.svg.x := (textLeftAlignment * 0.3).toString,
              ^.svg.y := 20.toString,
              ^.svg.fontSize := fontSize.toString  + "px",
              p.patientData("LatestEventTitle")
            )
          ),
          <.svg.svg(
            ^.`class` := "doctorStatus",
            ^.svg.x := (textLeftAlignment + ((cardWidth - triageFieldWidth) / 2)).toString,
            ^.svg.y := (cardHeight / 1.2).toString,
            <.svg.path(
              ^.`class` := "doctorSymbol",
              //^.svg.width := cardWidth.toString,
              ^.svg.d := "m 19.632768,2.7559322 -4.557853,0 C 14.616949,1.5605424 13.417514,0.69491525 12,0.69491525 c -1.417514,0 -2.6169492,0.86562715 -3.0749153,2.06101695 l -4.5578531,0 c -1.199435,0 -2.1807909,0.9274576 -2.1807909,2.0610169 l 0,14.4271189 c 0,1.133559 0.9813559,2.061017 2.1807909,2.061017 l 15.2655364,0 c 1.199435,0 2.180791,-0.927458 2.180791,-2.061017 l 0,-14.4271189 c 0,-1.1335593 -0.981356,-2.0610169 -2.180791,-2.0610169 z m -7.632768,0 c 0.599718,0 1.090395,0.4637288 1.090395,1.0305085 0,0.5667796 -0.490677,1.0305084 -1.090395,1.0305084 -0.599718,0 -1.090395,-0.4637288 -1.090395,-1.0305084 0,-0.5667797 0.490677,-1.0305085 1.090395,-1.0305085 z M 9.819209,17.183051 5.4576271,13.061017 6.9950847,11.608 9.819209,14.266712 17.004915,7.475661 18.542373,8.938983 9.819209,17.183051 Z",
              ^.svg.fill := "black"
            ),
            <.svg.text(
              ^.svg.x := (textLeftAlignment * 0.3).toString,
              ^.svg.y := 20.toString,
              ^.svg.fontSize := fontSize.toString  + "px",
              p.careContactId
            )
          )

        )
      }

      def render(pmap: Map[String, Patient]) = {
        spgui.widgets.css.WidgetStyles.addToDocument()
        val longestWaiting = getLongestWaitingPatients(pmap)

        <.div(^.`class` := "card-holder-root")( // This div is really not necessary
          longestWaiting.values map ( p =>
            patientReminder(p)
          )
        )
      }

      def getLongestWaitingPatients(patients: Map[String, Patient]): Map[String, Patient] = {
        var longestWaiting: Map[String, Patient] = Map()
        var timeDiffMap: Map[String, Long] = Map()
        patients.foreach{ p =>
          timeDiffMap += p._2.careContactId -> p._2.patientData("LatestEventTimeDiff").toLong
        }
        if (timeDiffMap.size > 4) {
          for (i <- 1 to 4) {
            val id = timeDiffMap.maxBy(_._2)._1
            longestWaiting += id -> patients(id)
            timeDiffMap -= id
          }
        } else {
          for (i <- 1 to timeDiffMap.size) {
            val id = timeDiffMap.maxBy(_._2)._1
            longestWaiting += id -> patients(id)
            timeDiffMap -= id
          }
        }
        return longestWaiting
      }
  }

  private val patientReminderComponent = ReactComponentB[Unit]("patientReminderComponent")
  .initialState(Map("4502085" -> Patient("4502085", Map("careContactId" -> "4502085", "Location" -> "13", "CareContactRegistrationTime"->"2017-02-01T15:49:19Z", "LatestEventTitle" -> "testtitel", "LatestEventTimeDiff" -> "7"))))
  .renderBackend[Backend]
  .build

  def apply() = spgui.SPWidget(spwb => {
    patientReminderComponent()
  })
}