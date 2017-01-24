package spgui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._


object WidgetList {
  def apply() =
    Map[String, ReactElement](
      ("Grid Test", spgui.dashboard.Grid.component()),
      ("Widget Injection", widgets.injection.WidgetInjectionTest()),
      ("Item Editor", widgets.itemeditor.ItemEditor()),
      ("DragDrop Example", widgets.examples.DragAndDrop()),
      ("Widget with json", widgets.examples.WidgetWithJSON(0)),
      ("PlcHldrC", PlaceholderComp()),
      ("CommTest", widgets.WidgetCommTest()),
      ("ChartTest", widgets.charts.ChartTest())
    )
}

object PlaceholderComp {
  val component = ReactComponentB[Unit]("PlaceholderComp")
    .render(_ => <.h2("placeholder"))
    .build

  def apply() = component()
}
