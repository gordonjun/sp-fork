package spgui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import spgui.widgets.examples._


object WidgetList {
  val list =
    List[(String, SPWidgetBase => ReactElement, Int, Int)](
      ("Klocka", widgets.ClockWidget(), 3, 2),

      // Triage diagram widgets -->
      ("Triagediagram (process)", widgets.ProcessTriageWidget(), 3, 3),
      ("Triagediagram (stream)", widgets.StreamTriageWidget(), 3, 3),
      ("Triagediagram (medicin blå)", widgets.MedicineBlueTriageWidget(), 3, 3),
      ("Triagediagram (medicin gul)", widgets.MedicineYellowTriageWidget(), 3, 3),
      ("Triagediagram (kirurgi)", widgets.SurgeryTriageWidget(), 3, 3),
      ("Triagediagram (ortopedi)", widgets.OrthopedyTriageWidget(), 3, 3),
      ("Triagediagram (jour)", widgets.JourTriageWidget(), 3, 3),
      // <--

      // Status diagram widgets -->
      ("Statusdiagram (process)", widgets.ProcessStatusWidget(), 4, 1),
      ("Statusdiagram (stream)", widgets.StreamStatusWidget(), 4, 1),
      ("Statusdiagram (medicin blå)", widgets.MedicineBlueStatusWidget(), 4, 1),
      ("Statusdiagram (medicin gul)", widgets.MedicineYellowStatusWidget(), 4, 1),
      ("Statusdiagram (kirurgi)", widgets.SurgeryStatusWidget(), 4, 1),
      ("Statusdiagram (ortopedi)", widgets.OrthopedyStatusWidget(), 4, 1),
      ("Statusdiagram (jour)", widgets.JourStatusWidget(), 4, 1),
      // <--

      // Place diagram widgets -->
      ("Platsdiagram (process)", widgets.ProcessPlaceWidget(), 4, 1),
      ("Platsdiagram (stream)", widgets.StreamPlaceWidget(), 4, 1),
      ("Platsdiagram (medicin blå)", widgets.MedicineBluePlaceWidget(), 4, 1),
      ("Platsdiagram (medicin gul)", widgets.MedicineYellowPlaceWidget(), 4, 1),
      ("Platsdiagram (kirurgi)", widgets.SurgeryPlaceWidget(), 4, 1),
      ("Platsdiagram (ortopedi)", widgets.OrthopedyPlaceWidget(), 4, 1),
      ("Platsdiagram (jour)", widgets.JourPlaceWidget(), 4, 1),
      // <--

      // Patient cards widgets -->
      ("Patientkort (process)", widgets.ProcessPatientCardsWidget(), 8, 8),
      ("Patientkort (stream)", widgets.StreamPatientCardsWidget(), 8, 8),
      ("Patientkort (medicin blå)", widgets.MedicineBluePatientCardsWidget(), 8, 8),
      ("Patientkort (medicin gul)", widgets.MedicineYellowPatientCardsWidget(), 8, 8),
      ("Patientkort (kirurgi)", widgets.SurgeryPatientCardsWidget(), 8, 8),
      ("Patientkort (ortopedi)", widgets.OrthopedyPatientCardsWidget(), 8, 8),
      ("Patientkort (jour)", widgets.JourPatientCardsWidget(), 8, 8),
      // <--

      // Coordinator widgets -->
      ("Rumskarta (koordinator)", widgets.RoomOverviewServiceWidget(), 3, 3),
      ("Triage- och statusdiagram (koordinator)", widgets.CoordinatorDiagramServiceWidget(), 3, 3),
      ("Väntrumsdiagram (koordinator)", widgets.WaitingRoomServiceWidget(), 3, 3)
      // <--

      // Undone widgets -->
      ("PatientReminderWidget", widgets.PatientReminderServiceWidget(), 3, 3)
      // <--
    )

  val map = list.map(t => t._1 -> (t._2, t._3, t._4)).toMap
}

object PlaceholderComp {
  val component = ReactComponentB[Unit]("PlaceholderComp")
    .render(_ => <.h2("placeholder"))
    .build

  def apply() = SPWidget(spwb => component())
}
