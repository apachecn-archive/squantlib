package squantlib.parameter.yieldparameter

import org.jquantlib.time.{ Date => JDate }
import squantlib.chart.LineChart

/**
 * Basic Framework for Long-Double Interpolation
 * Points are interpolated between max and min range, and extrapolated outside.
 */
trait AbstractYieldParameter{
	var valuedate : JDate
	val mindays : Long
	val maxdays : Long

	def lowextrapolation(v : Long) : Double
    def highextrapolation(v : Long) : Double
    def interpolation(v : Long) : Double
  
    def value(v : Long) : Double = {
      v match {
        case vv if vv <= mindays => lowextrapolation(vv)
        case vv if vv >= maxdays => highextrapolation(vv)
        case _ => interpolation(v)
          }
    }

	
  /**
   * Renders a line chart to a PNG file.
   *
   * @param png Path for output file.
   * @param data Data to plot. In Seq[Pair[X, Y]] format.
   * @param x Label for X-axis
   * @param y Label for Y-axis
   * @param title Title for the plot.
   * @return Path to the output file (= png)
   *  
   */	
	def render(png:String, xgrid:Int = 30, xlabel:String = "X-axis", ylabel:String = "Y-axis", title:String = "Untitled"):String = {
		val data = (0 to (maxdays.toInt + 365) by xgrid) map { x => (new java.lang.Double(x / 365.0), new java.lang.Double(value(x)))}
		LineChart.render(png, data, xlabel, ylabel, title)
	}
}