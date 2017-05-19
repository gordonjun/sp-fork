package spgui.widgets.Kandidat

  import java.util.UUID

  import japgolly.scalajs.react._
  import japgolly.scalajs.react.vdom.prefix_<^._
  import sp.messages.Pickles._
  import spgui.SPWidget
  import org.scalajs.dom.raw
  import org.singlespaced.d3js.d3
  import org.singlespaced.d3js.Ops._
  import sp.domain.{ID, SPValue}
  import spgui.components.SPButton
  import sp.labkit.operations.{APIAbilityHandler => abapi}
  import sp.labkit.operations.{API_OperationRunner => opapi}
  import sp.labkit.operations.{API_LOS => los}
  import spgui.communication.BackendCommunication
  import spgui.widgets.abilityhandler.{APIAbilityHandler, APIVirtualDevice}
package API_LOS{
  sealed trait API_LOS
  case class sendThings(things: List[String], things2: List[String]) extends API_LOS
  object attributes {
    val service = "LOS"
  }
}
object ErrorHandler{
  case class State(things: List[String], things2: List[String])
  private class Backend($: BackendScope[Unit, State]) {

    val messObs = BackendCommunication.getMessageObserver(
      mess => {
        //println(s"The widget example got: $mess" +s"parsing: ${mess.getBodyAs[api.API_ExampleService]}")
        mess.getBodyAs[los.API_LOS].map {
          case los.sendThings(m, id) =>

          case x =>
            println(s"THIS WAS NOT EXPECTED IN EXAMPLEWIDGET: $x")
        }

      },
      "answers" // the topic you want to listen to. Soon we will also add some kind of backend filter,  but for now you get all answers
    )

      }
    }
  def apply() = SPWidget{spwb =>
    component()
  }
  val colorMap = List(
    "#00ff00",
    "#00ff00",
    "#00ff00"
  )
  val fault1 = List(
    "#00ff00",
    "#ff0000",
    "#00ff00"
  )
  val fault2 = List(
    "#ff0000",
    "#00ff00"
  )
  private val component = ReactComponentB[Unit]("ErrorHandler")
    .initialState(colorMap)
    .render{dcb =>
      <.div(
        SPButton("mod state", Seq(^.onClick --> dcb.modState(_ => colorMap))),
        SPButton("mod state", Seq(^.onClick --> dcb.modState(_ => fault1))),
        D3BarsComponent(dcb.state)
      )
    }
    .build
  }/*
  def sendToAB(mess: abapi.Request): Callback = {
    val h = SPHeader(from = "AbilityHandlerWidget", to = abapi.attributes.service,
    reply = SPValue("AbilityHandlerWidget"), reqID = java.util.UUID.randomUUID())
    val json = SPMessage(*(h), *(mess))
    BackendCommunication.publish(json, "services")
    Callback.empty
  }*/

object D3BarsComponent {
  def apply(data: List[String]) = component(data)

  private val component = ReactComponentB[List[String]]("d3DivComponent")
    .render(_ => <.div())
    .componentDidUpdate(dcb => Callback(addTheD3(ReactDOM.findDOMNode(dcb.component), dcb.currentProps)))
    .build

  private def addTheD3(element: raw.Element, list: List[String]): Unit = {
    val graphHeight = 440
    val barWidth = 45
    val frameHeight = 100
    val horizontalBarDistance = barWidth + 100
    val barHeightMultiplier = graphHeight / frameHeight

    // clear all before rendering new data
    // plain-js d3-examples online don't have this line, adding it is a way
    // to let react take care of the rerendering, rather than d3 itself
    d3.select(element).selectAll("*").remove()
    val svg = d3.select(element).append("svg")
      .attr("width", horizontalBarDistance)
      .attr("height", graphHeight)
    val g = svg.append("g")

    g.append("rect")
      .attr("x", 5)
      .attr("y", 5)
      .attr("width", barWidth)
      .attr("height", barWidth*1.5)
      .attr("fill", list(0))

    svg.append("text")
      .attr("x", 5)
      .attr("y",5)
      .attr("font-size",10)
      .text("P1")


    g.append("rect")
      .attr("x", 5)
      .attr("y", 75)
      .attr("width", barWidth)
      .attr("height", barWidth*1.5)
      .attr("fill", list(1))

    g.append("rect")
      .attr("x", 5)
      .attr("y", 145)
      .attr("width", barWidth)
      .attr("height", barWidth*1.5)
      .attr("fill", list(2))
  }
}
