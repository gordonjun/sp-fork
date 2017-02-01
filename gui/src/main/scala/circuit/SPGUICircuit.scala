package spgui.circuit

import diode._
import diode.react.ReactConnector

import upickle.default._
import org.scalajs.dom.ext.SessionStorage
import scala.util.{Success, Try}

object SPGUICircuit extends Circuit[SPGUIModel] with ReactConnector[SPGUIModel] {
  def initialModel = BrowserStorage.load.getOrElse(InitialState())
  val actionHandler = composeHandlers(
    new DashboardHandler(zoomRW(_.openWidgets)((m,v) => m.copy(openWidgets = v))),
    new LayoutHandler(zoomRW(_.layout)((m,v) => m.copy(layout = v)))
  )
  // store state upon any model change
  subscribe(zoomRW(myM => myM)((m,v) => v))(m => BrowserStorage.store(m.value))
}

class DashboardHandler[M](modelRW: ModelRW[M, OpenWidgets]) extends ActionHandler(modelRW) {
  def handle = {
    case AddWidget(widgetType) => updated(OpenWidgets(value.count + 1, value.list :+ OpenWidget(value.count + 1, widgetType)))
    case CloseWidget(id) => updated(OpenWidgets(value.count, value.list.filter(_.id != id)))
    case SetWidgetData(id, stringifiedWidgetData) =>
      updated(OpenWidgets(value.count, value.list.map(ow => if(ow.id == id) ow.copy(stringifiedWidgetData = stringifiedWidgetData) else ow)))
  }
}

class LayoutHandler[M](modelRW: ModelRW[M, Int]) extends ActionHandler(modelRW) {
  def handle = {
    case LayoutUpdated(count: Int) =>  {
      updated(value+1) // this should contain the actual layout obviously
    }
  }
}

object BrowserStorage {
  val namespace = "SPGUIState"
  def store(spGUIState: SPGUIModel) = SessionStorage(namespace) = write(spGUIState)
  def load: Option[SPGUIModel] =
    Try(SessionStorage(namespace) map read[SPGUIModel]) match {
      case Success(Some(state)) => Some(state)
      case _ => None
    }
}
