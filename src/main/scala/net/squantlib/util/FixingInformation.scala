package net.squantlib.util

import scala.annotation.tailrec
import DisplayUtils._

case class FixingInformation(
    var tbd:Option[Double], 
    var minRange:Option[Double], 
    var maxRange:Option[Double], 
    var initialFixing:Map[String, Double],
    fixingPageInformation: List[Map[String, String]]) {
  
  def initialFixingFull:Map[String, Double] = {
    val inv:Map[String, Double] = initialFixing.withFilter{case (k, v) => k.size == 6}.map{case (k, v) => (((k takeRight 3) + (k take 3)), (if (v == 0.0) 0.0 else 1.0 / v))}.toMap
    if (inv.isEmpty) initialFixing else initialFixing ++ inv
  }
    
  def all:Map[String, Double] = tbd match {
    case Some(c) => initialFixingFull.updated("tbd", c)
    case None => initialFixingFull
  }
    
  def update(p:String):String = multipleReplace(p, all.map{case (k, v) => ("@" + k, v)})
  
  def updateInitial(p:String):String = multipleReplace(p, initialFixing.map{case (k, v) => ("@" + k, v)})
  
  @tailrec private def multipleReplace(s:String, replacements:Map[String, Double]):String = 
    if (s == null) null
    else replacements.headOption match {
      case None => s
      case Some((k, v)) => multipleReplace(s.replace(k, v.toString), replacements - k)
    }
  
  def updateCompute(p:String):Option[Double] = FormulaParser.calculate(update(p))
  
  /*
   * Fixing Information Accessor
   */
    
  def currentPercent(decimal:Int):String = tbd.collect{case v => v.asPercent(decimal)}.getOrElse("未定")
    
  def currentDouble(decimal:Int):String = tbd.collect{case v => v.asDouble(decimal)}.getOrElse("未定")
  
  def minRangePercent(decimal:Int):String = minRange.collect{case v => v.asPercent(decimal)}.getOrElse("未定")
    
  def minRangeDouble(decimal:Int):String = minRange.collect{case v => v.asDouble(decimal)}.getOrElse("未定")
    
  def maxRangePercent(decimal:Int):String = maxRange.collect{case v => v.asPercent(decimal)}.getOrElse("未定")
    
  def maxRangeDouble(decimal:Int):String = maxRange.collect{case v => v.asDouble(decimal)}.getOrElse("未定")
    
  def rangePercent(decimal:Int):String = (minRange, maxRange) match {
    case (min, max) if min.isDefined || max.isDefined => s"[${min.collect{case v => v.asPercent(decimal)}.getOrElse("")}～${max.collect{case v => v.asPercent(decimal)}.getOrElse("")}]"
    case _ => ""
  }
    
  def rangeDouble(decimal:Int):String = (minRange, maxRange) match {
    case (min, max) if min.isDefined || max.isDefined => s"[${min.collect{case v => v.asDouble(decimal)}.getOrElse("")}～${max.collect{case v => v.asDouble(decimal)}.getOrElse("")}]"
    case _ => ""
  }

  val underlyingAssetIds:Map[String, String] = fixingPageInformation.map(pageInfo => {
    val bidOffer = pageInfo.get("bidoffer") match {
      case b if b == "bid" || b == "offer" => Some(b)
      case _ => None
    }

    (pageInfo.getOrElse("underlying", "unknown"), List(Some(pageInfo.getOrElse("underlying", "unknown")), pageInfo.get("page"), pageInfo.get("time"), pageInfo.get("country"), bidOffer).flatMap(s => s).mkString("/"))
  }).toMap
    
}
  
object FixingInformation {
  
  def empty = FixingInformation(None, None, None, Map.empty, List.empty)
  
}
