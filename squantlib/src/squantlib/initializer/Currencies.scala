package squantlib.initializer

import org.jquantlib.currencies.Africa._
import org.jquantlib.currencies.Asia._
import org.jquantlib.currencies.America._
import org.jquantlib.currencies.Europe._
import org.jquantlib.currencies.Oceania._
import org.jquantlib.time.calendars._
import org.jquantlib.currencies.{Currency => qlCurrency}

/**
 * Default conversion table for ID => Currency => Calendar.
 */
object Currencies {
  
	def getcurrency(id:String) = id_currency(id)
	def getcalendar(id:String) = ccyid_calendar(id)
	def getcalendar(currency:qlCurrency) = ccyid_calendar(currency.code)
  
	private val id_currency = Map(
			("ARS" -> new ARSCurrency),
			("ATS" -> new ATSCurrency),
			("AUD" -> new AUDCurrency),
			("BDT" -> new BDTCurrency),
			("BEF" -> new BEFCurrency),
			("BGL" -> new BGLCurrency),
			("BRL" -> new BRLCurrency),
			("BYR" -> new BYRCurrency),
			("CAD" -> new CADCurrency),
			("CHF" -> new CHFCurrency),
			("CLP" -> new CLPCurrency),
			("CNY" -> new CNYCurrency),
			("COP" -> new COPCurrency),
			("CYP" -> new CYPCurrency),
			("CZK" -> new CZKCurrency),
			("DEM" -> new DEMCurrency),
			("DKK" -> new DKKCurrency),
			("EEK" -> new EEKCurrency),
			("ESP" -> new ESPCurrency),
			("EUR" -> new EURCurrency),
			("FIM" -> new FIMCurrency),
			("FRF" -> new FRFCurrency),
			("GBP" -> new GBPCurrency),
			("GRD" -> new GRDCurrency),
			("HKD" -> new HKDCurrency),
			("HUF" -> new HUFCurrency),
			("IDR" -> new IDRCurrency),
			("IEP" -> new IEPCurrency),
			("ILS" -> new ILSCurrency),
			("INR" -> new INRCurrency),
			("IQD" -> new IQDCurrency),
			("IRR" -> new IRRCurrency),
			("ISK" -> new ISKCurrency),
			("ITL" -> new ITLCurrency),
			("JPY" -> new JPYCurrency),
			("KRW" -> new KRWCurrency),
			("KWD" -> new KWDCurrency),
			("LTL" -> new LTLCurrency),
			("LUF" -> new LUFCurrency),
			("LVL" -> new LVLCurrency),
			("MTL" -> new MTLCurrency),
			("MXN" -> new MXNCurrency),
			("NLG" -> new NLGCurrency),
			("NOK" -> new NOKCurrency),
			("NPR" -> new NPRCurrency),
			("NZD" -> new NZDCurrency),
			("PEH" -> new PEHCurrency),
			("PEI" -> new PEICurrency),
			("PEN" -> new PENCurrency),
			("PKR" -> new PKRCurrency),
			("PLN" -> new PLNCurrency),
			("PTE" -> new PTECurrency),
			("ROL" -> new ROLCurrency),
			("RON" -> new RONCurrency),
			("RUB" -> new RUBCurrency),
			("SAR" -> new SARCurrency),
			("SEK" -> new SEKCurrency),
			("SGD" -> new SGDCurrency),
			("SIT" -> new SITCurrency),
			("SKK" -> new SKKCurrency),
			("THB" -> new THBCurrency),
			("TRL" -> new TRLCurrency),
			("TRY" -> new TRYCurrency),
			("TTD" -> new TTDCurrency),
			("TWD" -> new TWDCurrency),
			("USD" -> new USDCurrency),
			("VEB" -> new VEBCurrency),
			("ZAR" -> new ZARCurrency))
	
		private val ccyid_calendar = Map(
			("ARS" -> new Argentina),
			("AUD" -> new Australia),
			("BRL" -> new Brazil),
			("CAD" -> new Canada),
			("CNY" -> new China),
			("CZK" -> new CzechRepublic),
			("DKK" -> new Denmark),
			("HKD" -> new HongKong),
			("HUF" -> new Hungary),
			("ISK" -> new Iceland),
			("INR" -> new India),
			("IDR" -> new Indonesia),
			("JPY" -> new Japan),
			("MXN" -> new Mexico),
			("NZD" -> new NewZealand), 
			("NOK" -> new Norway),
			("PLN" -> new Poland),
			("RON" -> new Romania),
			("RUB" -> new Russia),
			("SAR" -> new SaudiArabia),
			("SGD" -> new Singapore),
			("ZAR" -> new SouthAfrica),
			("KRW" -> new SouthKorea),
			("SEK" -> new Sweden),
			("CHF" -> new Switzerland),
			("TWD" -> new Taiwan),
			("EUR" -> new Target),
			("TRY" -> new Turkey),
			("GBP" -> new UnitedKingdom),
			("USD" -> new UnitedStates))

}
