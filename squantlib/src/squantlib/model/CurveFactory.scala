package squantlib.model

import scala.collection.mutable.HashMap
import scala.collection.JavaConversions
import squantlib.model.yieldparameter.{YieldParameter, FlatVector}
import squantlib.model.rates._
import squantlib.model.fx._
import org.jquantlib.currencies.Currency
import org.jquantlib.time.{Date => qlDate, Period => qlPeriod, TimeUnit, Calendar}
import org.jquantlib.instruments.{Bond => qlBond}
import org.jquantlib.pricingengines.bond.DiscountingBondEngine
import org.jquantlib.termstructures.YieldTermStructure

/** 
 * Stores rate curve information and initialize discount curves as requested.
 * Require all discount curves to have same value date.
 * 
 * @param Map CurrencyId => DiscountCurve
 */
class CurveFactory(val curves:Map[String, DiscountableCurve], val cdscurves:Map[String, CDSCurve] = null, val fxparams:Map[String, FXparameter], val paramset:String = null) {

	var valuedate:qlDate = curves.head._2.valuedate
	require(curves.forall(_._2.valuedate == valuedate))
	
	val FXbaseSpread = 0.0
	val FXbaseCurrency = "USD"
	
	/** 
	 * USD
	 */ 
	val pivotCurrency:String = BasisSwapCurve.pivotcurrency.code
	
	/** 
	 * Currencies
	 */
	val currencies:Set[Currency] = curves.collect { case (k, v) => v.currency }.toSet
	val curveList = curves.keySet
	def contains(ccy:String) = curves.contains(ccy)

	/** 
	 * Issuers
	 */
	val cdsNames:Set[String] = if (cdscurves == null) null else cdscurves.keySet
	def containsCDS(issuer:String) = cdscurves.contains(issuer)
	
	/** 
	 * Discounting Curves
	 */
	val discountingCurves:Map[String, RateCurve] = curves.collect{ case (cur:String, curve:RateCurve) => (cur, curve)}
	  
	/**
	 * Stores already calculated discount curves.
	 * Assumption: for each key, value contains discount curve for both discount and pivot currency.
	 */
	var repository:Map[String, scala.collection.mutable.Map[String, DiscountCurve]] = Map.empty
	
	/**
	 * Returns FX spot ccy1 / ccy2
	 * @param currency code, 
	 */
	def fx(ccy1:String, ccy2:String):Double = 
	  try {curves(ccy2).fx / curves(ccy1).fx } catch { case _ => Double.NaN}

	/**
	 * Returns discount curve with "base" spread & currency.
	 * @param currency code, spread
	 */
	def getBaseDiscountCurve(ccy:String):Option[DiscountCurve] = getDiscountCurve(ccy, FXbaseCurrency, FXbaseSpread)
	
	def getBaseDiscountCurve(ccy:Currency):Option[DiscountCurve] = getBaseDiscountCurve(ccy.code)
	  
	/**
	 * Returns discount curve. Discount currency is flat and same currency with given spread.
	 * @param currency code, spread
	 */
	def getDiscountCurve(ccy:String, spread:Double):Option[DiscountCurve] = getDiscountCurve(ccy, ccy, spread)
	
	def getDiscountCurve(ccy:Currency, spread:Double):Option[DiscountCurve] = getDiscountCurve(ccy.code, spread)
	
	/**
	 * Returns discount curve, flat spread, using specific currency.
	 * @param currency code, discounting currency name, spread
	 */
	def getDiscountCurve(ccy:String, discountccy:String, spread:Double) : Option[DiscountCurve] = getDiscountCurve(ccy, discountccy, new FlatVector(valuedate, spread), null)
	  
	def getDiscountCurve(ccy:Currency, discountccy:String, spread:Double) : Option[DiscountCurve] = getDiscountCurve(ccy.code, discountccy, spread)

	/**
	 * Returns discount curve using spread of given cds curve.
	 * @param currency code, cds id
	 */
	def getDiscountCurve(ccy:String, cdsid:String) : Option[DiscountCurve] = 
	  if (cdscurves.isEmpty || cdscurves.contains(cdsid)) getDiscountCurve(ccy, cdscurves(cdsid).currency.code, cdscurves(cdsid).rate, cdsid)
	  else None
	  
	def getDiscountCurve(ccy:Currency, cdsid:String) : Option[DiscountCurve] = getDiscountCurve(ccy.code, cdsid)

	/**
	 * Returns discount curve from given CDS curve.
	 * @param currency code, CDS curve
	 */
	def getDiscountCurve(ccy:String, spread:CDSCurve) : Option[DiscountCurve] = getDiscountCurve(ccy, spread.currency.code, spread.rate, null)
	
	def getDiscountCurve(ccy:Currency, spread:CDSCurve) : Option[DiscountCurve] = getDiscountCurve(ccy.code, spread)
	  
	/**
	 * Returns discount curve from full given parameter.
	 */
	private def getDiscountCurve(ccy:String, discountccy:String, spread:YieldParameter, cdsid:String) : Option[DiscountCurve] = 
	  if (!curves.contains(ccy)) {println("curve " + ccy + " not found"); None}
	  else if (contains(ccy, cdsid)) Some(repository(cdsid)(ccy))
	  else {
	    val newcurve = ccy match {
		    case `discountccy` => { curves(ccy).getZC(spread) }
		    					
		    case `pivotCurrency` => { 
			    val zccurve = getDiscountCurve(discountccy, discountccy, spread, cdsid).get
			    curves(ccy).getZC(ratecurve(discountccy), zccurve)
			    }
		      
		    case _ => { 
			    val pivotZC = getDiscountCurve(pivotCurrency, discountccy, spread, cdsid).get
			    curves(ccy).getZC(ratecurve(pivotCurrency), pivotZC)
			    }
    	}
	    
	    if (cdsid != null) {
		    if (!repository.contains(cdsid)) repository += (cdsid -> scala.collection.mutable.Map(ccy -> newcurve))
		    else repository(cdsid) += (ccy -> newcurve)}
	    
	    Some(newcurve)
	  }
	
	private def ratecurve(c:String):RateCurve = 
	  if (discountingCurves.contains(c)) discountingCurves(c) 
	  else throw new ClassCastException
	
	/**
	 * Returns zero volatility FX object representing the FX exchange rate between given currencies.
	 * @param currency code
	 */
	def getFX(ccyFor:String, ccyDom:String) : Option[FX] = {
	    val curveDom = getBaseDiscountCurve(ccyDom)
	    val curveFor = getBaseDiscountCurve(ccyFor)
	    if ((curveDom isDefined) && (curveFor isDefined)) {
	      if (fxparams.contains(ccyFor + ccyDom)) fxparams(ccyFor + ccyDom).getModel(curveDom.get, curveFor.get)
	      else Some(FXzeroVol(curveDom.get, curveFor.get))
	    }
	    else None
	  }
	
	def getFX(fxpair:String):Option[FX] = {
	  if (fxpair == null || fxpair.size != 6) None
	  else getFX(fxpair.substring(0, 3), fxpair.substring(3, 6))
	}
	
	/**
	 * Returns flat volatility FX object representing the FX exchange rate between given currencies.
	 * @param currency code
	 * @param volatility (flat over timeline & strike)
	 */
	def getFX(ccyFor:String, ccyDom:String, vol:Double) : Option[FX] = {
	    val curveDom = getBaseDiscountCurve(ccyDom)
	    val curveFor = getBaseDiscountCurve(ccyFor)
	    if ((curveDom isDefined) && (curveFor isDefined)) FXflatVol(curveDom.get, curveFor.get, vol) else None
	}
	
	/**
	 * Returns non-smiled volatility FX object representing the FX exchange rate between given currencies.
	 * @param currency code
	 * @param volatility as function of time t
	 */
	def getFX(ccyFor:String, ccyDom:String, vol:Double => Double) : Option[FX] = {
	    val curveDom = getBaseDiscountCurve(ccyDom)
	    val curveFor = getBaseDiscountCurve(ccyFor)
	    if ((curveDom isDefined) && (curveFor isDefined)) FXnoSmile(curveDom.get, curveFor.get, vol) else None
	}

	/**
	 * Returns smiled volatility FX object representing the FX exchange rate between given currencies.
	 * @param currency code
	 * @param volatility as function of time t and strike k
	 */
	def getFX(ccyFor:String, ccyDom:String, vol:(Double, Double) => Double) : Option[FX] = {
	    val curveDom = getBaseDiscountCurve(ccyDom)
	    val curveFor = getBaseDiscountCurve(ccyFor)
	    if ((curveDom isDefined) && (curveFor isDefined)) FXsmiled(curveDom.get, curveFor.get, vol) else None
	}
	
	
	def getYieldTermStructure(bond:qlBond):Option[YieldTermStructure] = 
	  	try { Some(getDiscountCurve(bond.currency.code, bond.creditSpreadID).get.toZCImpliedYieldTermStructure) } 
		catch { case _ => None}
										   
	def getYieldTermStructure(bond:qlBond, calendar:Calendar):Option[YieldTermStructure] = 
		try { Some(getDiscountCurve(bond.currency.code, bond.creditSpreadID).get.toZCImpliedYieldTermStructure(calendar))} 
		catch { case _ => None}
	
	def getCustomYieldTermStructure(bond:qlBond, calendar:Calendar, newvaluedate:qlDate):Option[YieldTermStructure] = 
		try { 
		  val newcurve = getDiscountCurve(bond.currency.code, bond.creditSpreadID).get
		  newcurve.valuedate = newvaluedate
		  Some(newcurve.toZCImpliedYieldTermStructure(calendar))
		  } 
		catch { case _ => None}
	
	def getDiscountBondEngine(bond:qlBond):Option[DiscountingBondEngine] = 
	  	try { Some(getDiscountCurve(bond.currency.code, bond.creditSpreadID).get.toDiscountBondEngine) } 
		catch { case _ => None}
		 
	def getDiscountBondEngine(bond:qlBond, calendar:Calendar):Option[DiscountingBondEngine] = 
		try { Some(getDiscountCurve(bond.currency.code, bond.creditSpreadID).get.toDiscountBondEngine(calendar)) } 
		catch { case _ => None}
	
	def getCustomDiscountBondEngine(bond:qlBond, calendar:Calendar, newvaluedate:qlDate):Option[DiscountingBondEngine] = 
		try { 
		  val newcurve = getDiscountCurve(bond.currency.code, bond.creditSpreadID).get
		  newcurve.valuedate = newvaluedate
		  Some(newcurve.toDiscountBondEngine(calendar)) } 
		catch { case _ => None}
	
	/**
	 * Checks whether the given curve is already calculated and stored in the repository.
	 */
	def contains(ccy:String, cdsid:String) = {
		repository.contains(cdsid) && repository(cdsid).contains(ccy)
	 }
	
	def describe = {
		val eol = sys.props("line.separator")
		val sortedcurves = scala.collection.immutable.TreeMap(curves.toArray:_*)	    
		val sortedcdscurves = scala.collection.immutable.TreeMap(cdscurves.toArray:_*)	    
		"Curves:" + eol + sortedcurves.map(c => c._2.describe + (if (discountingCurves.contains(c._1)) "(*)" else "") + eol).mkString("") + 
		"(*) Discounting curves" + eol + eol +
		"Credit Spreads:" + eol + sortedcdscurves.map(c => c._1 + "\t" + c._2.rate.valuedate.shortDate + "\t" + c._2.rate.maxdate.shortDate + eol).mkString("")
	}
	
    override def toString():String = "DiscountCurveFactory{" + curves.map(c => c._2).mkString(", ") + "}"
	
//    def this(curves:Map[String, DiscountableCurve]) = this(curves, null, null)
//    def this(curves:Map[String, DiscountableCurve], cdscurves:Map[String, CDSCurve]) = this(curves, cdscurves, null)
}
