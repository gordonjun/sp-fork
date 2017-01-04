package spgui.dashboard

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import spgui.circuit.{SPGUICircuit, CloseWidget}

object DashboardItem {
  case class Props(element: ReactElement, index: Int)
  private val component = ReactComponentB[Props]("Widget")
    .render_P(props =>
    <.div(
      ^.className := DashboardCSS.widgetPanel.htmlClass,
      ^.className := "panel panel-default",
      <.div(
        ^.className := DashboardCSS.widgetPanelHeader.htmlClass,
        <.button(
          "close me",
          ^.onClick --> Callback(SPGUICircuit.dispatch(CloseWidget(props.index)))
        )
      ),
      <.div(
        ^.className := DashboardCSS.widgetPanelBody.htmlClass,
        <.div(
          ^.className := "panel-body",
          ^.className := DashboardCSS.widgetPanelContent.htmlClass,
           props.element
        )
      )
    )
  )
    .build
  def apply(element: ReactElement, index: Int) = component(Props(element, index))
}
