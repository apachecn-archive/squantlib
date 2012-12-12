package squantlib.setting.rateconventions

import squantlib.setting.RateConvention
import org.jquantlib.time.{Period, Frequency, TimeUnit}
import org.jquantlib.daycounters._

class SekRateConvention extends RateConvention{
  import org.jquantlib.indexes.ibor.STIBOR
  import org.jquantlib.currencies.Europe.SEKCurrency
  
  	val currency = new SEKCurrency
  
	val useRateDiscount = true
	def iborindex(p:Period) = new STIBOR(p)
	val swapFloatIndex = new STIBOR(new Period(3, TimeUnit.Months))
	val swapFixDaycount = new Thirty360
	val swapFixPeriod = Frequency.Annual

	val useFXdiscount = true
	val swapPointMultiplier = 10000.0
}
