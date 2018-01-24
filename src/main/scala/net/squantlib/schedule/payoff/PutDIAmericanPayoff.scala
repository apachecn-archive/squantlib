package net.squantlib.schedule.payoff

import scala.collection.JavaConversions._
import org.codehaus.jackson.map.ObjectMapper
import net.squantlib.util.DisplayUtils._
import net.squantlib.util.JsonUtils._
import java.util.{Map => JavaMap}
import net.squantlib.database.DB
import net.squantlib.util.Date
import net.squantlib.schedule.CalculationPeriod
import net.squantlib.util.FixingInformation
import scala.reflect.ClassTag
import net.squantlib.model.market.Market

/**
 * Interprets JSON formula specification for sum of linear formulas with discrete range.
 * JSON format:
 *  {type:"putdiamerican", variable:[String], trigger:[Double], strike:[Double], refstart:Date, refend:Date, description:String}, 
 * No strike is considered as no low boundary
 */
case class PutDIAmericanPayoff(
    putVariables:List[String], 
    trigger:List[Double], 
    strike:List[Double],
    finalTrigger:List[Double],
    refstart:Date, 
    refend:Date, 
    var knockedIn:Boolean,
    override val physical:Boolean,
    forward:Boolean,
    closeOnly:Boolean,
    amount:Double = 1.0, 
    description:String = null,
    inputString:String = null)(implicit val fixingInfo:FixingInformation) extends Payoff {
  
  override val variables = putVariables.toSet
  
  nominal = amount
  
  val strikeMap:Map[String, Double] = (putVariables zip strike) (collection.breakOut)
   
  val triggerMap:Map[String, Double] = (putVariables zip trigger) (collection.breakOut)

  val finalTriggerMap:Map[String, Double] = (putVariables zip finalTrigger) (collection.breakOut)

  val strikeOrFinalTriggerMap:Map[String, Double] = (strikeMap.keySet ++ finalTriggerMap.keySet).map(v => (strikeMap.get(v), finalTriggerMap.get(v)) match {
    case (Some(s), Some(t)) => (v, Math.min(s, t))
    case (None, Some(t)) => (v, t)
    case (Some(s), None) => (v, s)
    case _ => (v, Double.NaN)
  }).toMap

  override val isPriceable:Boolean = 
    !trigger.exists(v => v.isNaN || v.isInfinity) && 
    !strike.exists(v => v.isNaN || v.isInfinity) && 
    refstart != null && 
    refend != null &&
    (refstart le refend)
  
  var mcPeriod6m = 30
  var mcPeriod1y = 90
  var mcPeriodbefore = 180
  
  override def eventDates(period:CalculationPeriod):List[Date] = {
    if (!isPriceable) {return List(period.endDate)}
    val basemod = refend.serialNumber % mcPeriod6m
    val start = refstart.serialNumber
    val end = refend.serialNumber
    val dates:List[Date] = (for (i <- (start to end) 
        if (i >= end - 180 && i % mcPeriod6m == basemod)
        || (i >= end - 360 && i % mcPeriod1y == basemod)
        || (i % mcPeriodbefore == basemod)) yield Date(i)) (collection.breakOut)
          
    if (physical) {
      if (dates.head == refstart) dates :+ period.paymentDate else (refstart :: dates) :+ period.paymentDate
    } else {
      if (dates.head == refstart) dates else refstart :: dates
    }
  }
  
  trait FixingInterpreter[T] {
    def isKnockIn(fixings:T):Boolean // Method to be implemented
    def minBelowStrike(fixings:T):Boolean // Method to be implemented
    def price(fixings:T, isKnockedIn:Boolean):Double // Method to be implemented
    
    def isKnockIn(fixings:List[T]):Boolean = {
      if (fixings.isEmpty) knockedIn
      else (knockedIn || fixings.exists(isKnockIn(_))) && (forward || minBelowStrike(fixings.last))
    }

    def price(fixings:T):Double = {
      if (physical) {
        if (isFixed) price(fixings, knockedIn)
        else Double.NaN
      }
      else price(fixings, isKnockIn(fixings))
    }

    def price(fixings:List[T]):Double = {
      fixings.lastOption match {
        case Some(lastFixing) => 
          if (physical) {
            val fixingSize = fixings.length
            if (isFixed) price(lastFixing, knockedIn)
            else if (fixingSize >= 2) price(lastFixing, isKnockIn(fixings.dropRight(1)))
            else Double.NaN
          }
          else price(lastFixing, isKnockIn(fixings))
        case None => Double.NaN
      }
    }
    
    
//    def price(fixings:List[T]):Double = {
//      if (fixings.isEmpty) Double.NaN
//      else if (physical) price(fixings.last, isKnockIn(fixings.dropRight(1)))
//      else price(fixings.last, isKnockIn(fixings))
//    }
  }
  
  implicit object MapInterpreter extends FixingInterpreter[Map[String, Double]] {
    
    override def isKnockIn(fixings:Map[String, Double]):Boolean = {
      knockedIn || variables.exists(p => fixings.get(p) match { 
        case Some(v) if triggerMap.contains(p) => v <= triggerMap(p) 
        case _ => false
      })
    }

    override def minBelowStrike(fixings:Map[String, Double]):Boolean = {
      variables.exists(p => fixings.get(p) match { 
        case Some(v) if strikeOrFinalTriggerMap.contains(p) => v <= strikeOrFinalTriggerMap(p)
        case _ => false
      })
    }

    override def price(fixings:Map[String, Double], isKnockedIn:Boolean):Double = {
      if ((variables subsetOf fixings.keySet) && variables.forall(v => !fixings(v).isNaN && !fixings(v).isInfinity) && isPriceable) {
        if (isKnockedIn) variables.map(v => fixings(v) / strikeMap(v)).min
        else 1.0
      } else Double.NaN
    }

  }
  
  implicit object DoubleInterpreter extends FixingInterpreter[Double] {

    override def isKnockIn(fixing:Double):Boolean = knockedIn || fixing <= trigger.head

    override def minBelowStrike(fixing:Double):Boolean = strikeOrFinalTriggerMap.values.headOption.collect{case s => fixing <= s}.getOrElse(true)

    override def price(fixing:Double, isKnockedIn:Boolean):Double = 
      if (fixing.isNaN || fixing.isInfinity || variables.size != 1 || !isPriceable) Double.NaN
      else if (physical) {
        if (isKnockedIn) strikeMap.values.headOption.collect{case s => fixing / s}.getOrElse(Double.NaN)
        else 1.0
      }
      else {
        if (isKnockedIn && (forward || minBelowStrike(fixing))) strikeMap.values.headOption.collect{case s => fixing / s}.getOrElse(Double.NaN)
        else 1.0
      }
    }
  
  def priceSingle[A:FixingInterpreter](fixings:A):Double = implicitly[FixingInterpreter[A]] price fixings
  
  def priceList[A:FixingInterpreter](fixings:List[A]):Double = implicitly[FixingInterpreter[A]] price fixings

  override def priceImpl(fixings:List[Map[String, Double]]):Double = priceList(fixings)

  override def priceImpl(fixings:Map[String, Double]):Double = priceSingle(fixings)
  
  override def priceImpl[T:ClassTag](fixings:List[Double]):Double = priceList(fixings)
  
  override def priceImpl(fixing:Double):Double = priceSingle(fixing)

  override def priceImpl = Double.NaN

  override def priceImpl(market:Market):Double = price(List.fill(2)(market.getFixings(variables)))

  override def toString =
    nominal.asPercent + " [" + trigger.map(_.asDouble).mkString(",") + "](Amer) " + nominal.asPercent + " x Min([" + variables.mkString(",") + "] / [" + strike.map(_.asDouble).mkString(",") + "])" 
  
  override def jsonMapImpl = Map(
    "type" -> "putdiamerican", 
    "variable" -> putVariables.toArray, 
    "trigger" -> trigger.toArray, 
    "strike" -> strike.toArray,
    "refstart" -> (if (refstart == null) null else refstart.toString),
    "refend" -> (if (refend == null) null else refend.toString),
    "description" -> description)
    

  override def clearFixings = {
    super.clearFixings
    knockedIn = false
  }
    
  override def assignFixings(f:Map[String, Double]):Unit = {
    super.assignFixings(f)
    checkKnockIn
  }
    
  def checkKnockIn:Unit = {
    knockedIn = 
      if (refstart == null || refend == null) false
      else (putVariables zip trigger).exists{case (v, trig) => 
        
        val historicalPrices = if (closeOnly) DB.getHistorical(v, refstart, refend) else DB.getHistorical(v, refstart, refend) ++ DB.getHistoricalLow(v, refstart, refend)
        
        historicalPrices match {
          case hs if hs.isEmpty => false
          case hs if hs.get(refend).isDefined => implicitly[FixingInterpreter[Double]] isKnockIn(hs.values.toList)
          case hs => hs.exists{case (_, x) => x <= trig}
        }
      }
  }
  
}

object PutDIAmericanPayoff {
  
  def apply(inputString:String)(implicit fixingInfo:FixingInformation):PutDIAmericanPayoff = {
    val formula = Payoff.updateReplacements(inputString)
    val fixed = fixingInfo.update(formula)
    val variable:List[String] = formula.parseJsonStringList("variable").map(_.orNull)
    val trigger:List[Double] = fixed.parseJsonDoubleList("trigger").map(_.getOrElse(Double.NaN))
    val strike:List[Double] = fixed.parseJsonDoubleList("strike").map(_.getOrElse(Double.NaN))

    val finalTrigger:List[Double] = {
      val inputValues:List[Option[Double]] = fixed.parseJsonDoubleList("final_strike")
      if (inputValues.size == trigger.size) (inputValues, trigger).zipped.map{case (i, t) => (i.getOrElse(t))}
      else trigger
    }

    val amount:Double = fixed.parseJsonDouble("amount").getOrElse(1.0)
    val refstart:Date = formula.parseJsonDate("refstart").orNull
    val refend:Date = formula.parseJsonDate("refend").orNull
    val physical:Boolean = formula.parseJsonString("physical").getOrElse("0") == "1"
    val forward:Boolean = formula.parseJsonString("forward").getOrElse("0") == "1"
    val description:String = formula.parseJsonString("description").orNull
    val closeOnly:Boolean = formula.parseJsonString("reftype").getOrElse("closing") != "continuous"
    
    val knockedIn:Boolean = false
    
    PutDIAmericanPayoff(variable, trigger, strike, finalTrigger, refstart, refend, knockedIn, physical, forward, closeOnly, amount, description, inputString)
  }
  
}

