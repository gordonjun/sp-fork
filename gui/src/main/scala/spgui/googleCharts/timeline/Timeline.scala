/****************************************
  *      HELP CLASS TO GOOGLE CHARTS     *
  ****************************************/

package spgui.googleCharts.timeline

import scala.scalajs.js

// Configuration Options for Timeline inner
// trait
trait TimelineTrait {
  val barLabelStyle:     js.Object
  val colorByRowLabel:   Boolean
  val groupByRowLabel:   Boolean
  val rowLabelStyle:     js.Object
  val showBarLabels:     Boolean
  val showRowLabels:     Boolean
  val singleColor:       String
}

// null, false, true, null, true, true, null
class Timeline (
                        override val barLabelStyle: js.Object = null,
                        override val colorByRowLabel: Boolean = false,
                        override val groupByRowLabel: Boolean = true,
                        override val rowLabelStyle: js.Object = null,
                        override val showBarLabels: Boolean = true,
                        override val showRowLabels: Boolean = true,
                        override val singleColor: String = null
                      ) extends TimelineTrait {

  def toArray(): js.Array[js.Any] =
    js.Array(
      this.barLabelStyle,
      this.colorByRowLabel,
      this.groupByRowLabel,
      this.rowLabelStyle,
      this.showBarLabels,
      this.showRowLabels,
      this.singleColor
    )


  override def toString = s"Timeline($barLabelStyle, " +
    s"$colorByRowLabel, $groupByRowLabel, " +
    s"$rowLabelStyle, $showBarLabels, " +
    s"$showRowLabels, $singleColor)"
}