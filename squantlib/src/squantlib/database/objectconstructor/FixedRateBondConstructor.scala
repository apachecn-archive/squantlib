package squantlib.database.objectconstructor

import squantlib.database.schemadefinitions.Bond
import squantlib.database.schemadefinitions.DbKeywords
import squantlib.model.currencies.CurrencyConversion
import org.jquantlib.instruments.bonds.FixedRateBond
import org.jquantlib.time.{Date => JDate, Period => JPeriod, TimeUnit, Schedule, DateGeneration}

object FixedRateBondConstructor {
  
	val productid = "SB"
	
	def ratetoarray(formula:String, size:Int) = {
		val numarray = formula.split(";").map(x => (try{x.trim.toDouble / 100.0} catch { case _ => Double.NaN}))
		(0 to (size-1)).map(i => { val m = size - numarray.size; if(i < m) numarray(0) else numarray(i - m)}).toArray
	}
	
	def getbonds(bonds:Set[Bond]):Map[String, FixedRateBond] = {
	  bonds.map(b => (b.id, getbond(b))).filter(b => b._2 != null).toMap
	}
	
	def getbond(bond:Bond):FixedRateBond = {
	  val isvalid = bond.productid == productid && !bond.coupon.isEmpty && !bond.coupon_freq.isEmpty && !bond.redemprice.isEmpty
	  if (!isvalid) null
	  else {
		  		val issuedate = new JDate(bond.issuedate)
				val maturity = new JDate(bond.maturity)
				val schedule = {
				  val tenor = new JPeriod(bond.coupon_freq.get, TimeUnit.Months)
				  val calendar = CurrencyConversion.getcalendar(bond.currencyid)
				  val convention = DbKeywords.daycount_adj(bond.daycount_adj)
				  val maturityconvention = DbKeywords.daycount_adj(bond.daycount_adj)
				  val rule = DateGeneration.Rule.Backward
				  val endofmonth = false
				  new Schedule(issuedate, maturity, tenor, calendar, convention, maturityconvention, rule, endofmonth)
				}
				
				val currency = CurrencyConversion.getcurrency(bond.currencyid)
				val settlementdays = 0
				val faceamount = 100.0
				val coupons:Array[Double] = ratetoarray(bond.coupon, schedule.size)
				val accrualdaycounter = DbKeywords.daycount(bond.daycount)
				val paymentconvention = DbKeywords.daycount_adj(bond.payment_adj)
				val redemption = try{bond.redemprice.trim.toDouble} catch { case _ => Double.NaN}
				new FixedRateBond(settlementdays, faceamount, schedule, coupons, accrualdaycounter, paymentconvention, redemption, issuedate)
		  	}
	}	
}