package spgui.widgets.itemeditor

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import spgui.SPWidget

object ItemEditor {

  private val component = ReactComponentB[Unit]("ItemEditor")
    .render(_ =>
    <.div(
      ^.className := ItemEditorCSS.editor.htmlClass,
      JSONEditorTest()
    )
  )
    .build

  def apply() = SPWidget(spwb => component())
}