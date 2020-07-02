package io.inprice.api.utils;

import java.util.HashMap;
import java.util.Map;

public class CurrencyFormats {

  private static final Map<String, String> map;

  static {
    map = new HashMap<>(155);
    map.put("AED", "د.إ.‏ #,##0.00");
    map.put("AFN", "؋#,##0.00");
    map.put("ALL", "#.##0,00Lek");
    map.put("AMD", "#,##0.00 ֏");
    map.put("ANG", "ƒ#,##0.00");
    map.put("AOA", "Kz#,##0.00");
    map.put("ARS", "$ #.##0,00");
    map.put("AUD", "$#,##0.00");
    map.put("AWG", "ƒ#,##0.00");
    map.put("AZN", "#.##0,00 ₼");
    map.put("BAM", "#.##0,00 КМ");
    map.put("BBD", "$#,##0.00");
    map.put("BDT", "৳ #,##");
    map.put("BGN", "#.##0,00 лв.");
    map.put("BHD", "د.ب.‏ #,##0.000");
    map.put("BIF", "#,##FBu");
    map.put("BMD", "$#,##0.00");
    map.put("BND", "$#.##0");
    map.put("BOB", "Bs #.##0,00");
    map.put("BRL", "R$ #.##0,00");
    map.put("BSD", "$#,##0.00");
    map.put("BTC", "#,##0.Ƀ");
    map.put("BTN", "Nu. #,##.0");
    map.put("BWP", "P#,##0.00");
    map.put("BYR", "#.##0,00 р.");
    map.put("BZD", "BZ$#,##0.00");
    map.put("CAD", "$#,##0.00");
    map.put("CDF", "#,##0.00FC");
    map.put("CHF", "CHF # ##0.00");
    map.put("CLP", "$ #.##0,00");
    map.put("CNY", "¥#,##0.00");
    map.put("COP", "$ #.##0,00");
    map.put("CRC", "₡#.##0,00");
    map.put("CUC", "CUC#,##0.00");
    map.put("CUP", "$MN#,##0.00");
    map.put("CVE", "$#,##0.00");
    map.put("CZK", "#.##0,00 Kč");
    map.put("DJF", "#,##0Fdj");
    map.put("DKK", "#.##0,00 kr.");
    map.put("DOP", "RD$#,##0.00");
    map.put("DZD", "د.ج.‏ #,##0.00");
    map.put("EGP", "ج.م.‏ #,##0.00");
    map.put("ERN", "#,##0.00Nfk");
    map.put("ETB", "ETB#,##0.00");
    map.put("EUR", "#.##0,00 €");
    map.put("FJD", "$#,##0.00");
    map.put("FKP", "£#,##0.00");
    map.put("GBP", "£#,##0.00");
    map.put("GEL", "#.##0,00 Lari");
    map.put("GHS", "₵#,##0.00");
    map.put("GIP", "£#,##0.00");
    map.put("GMD", "#,##0.00D");
    map.put("GNF", "#,##0FG");
    map.put("GTQ", "Q#,##0.00");
    map.put("GYD", "$#,##0.00");
    map.put("HKD", "HK$#,##0.00");
    map.put("HNL", "L. #,##0.00");
    map.put("HRK", "#.##0,00 kn");
    map.put("HTG", "G#,##0.00");
    map.put("HUF", "#.##0,00 Ft");
    map.put("IDR", "Rp#.##0");
    map.put("ILS", "₪ #,##0.00");
    map.put("INR", "₹#,##0.00");
    map.put("IQD", "د.ع.‏ #,##0.00");
    map.put("IRR", "﷼ #,##/00");
    map.put("ISK", "#.##0 kr.");
    map.put("JMD", "J$#,##0.00");
    map.put("JOD", "د.ا.‏ #,##0.000");
    map.put("JPY", "¥#,##0");
    map.put("KES", "KSh#,##0.00");
    map.put("KGS", "#.##-00 сом");
    map.put("KHR", "#,##0៛");
    map.put("KMF", "#,##0.00CF");
    map.put("KPW", "₩#,##");
    map.put("KRW", "₩#,##");
    map.put("KWD", "د.ك.‏ #,##0.000");
    map.put("KYD", "$#,##0.00");
    map.put("KZT", "₸#.##-00");
    map.put("LAK", "#,##₭");
    map.put("LBP", "ل.ل.‏ #,##0.00");
    map.put("LKR", "₨ #,##");
    map.put("LRD", "$#,##0.00");
    map.put("LSL", "#,##0.00M");
    map.put("LYD", "د.ل.‏#,##0.000");
    map.put("MAD", "د.م.‏ #,##0.00");
    map.put("MDL", "#,##0.00 lei");
    map.put("MGA", "Ar#,##");
    map.put("MKD", "#.##0,00 ден.");
    map.put("MMK", "K#,##0.00");
    map.put("MNT", "₮#.##0,00");
    map.put("MOP", "MOP$#,##0.00");
    map.put("MRO", "#,##0.00UM");
    map.put("MUR", "₨#,##0.00");
    map.put("MVR", "#,##.0 MVR");
    map.put("MWK", "MK#,##0.00");
    map.put("MXN", "$#,##0.00");
    map.put("MYR", "RM#,##0.00");
    map.put("MZN", "MT#,##");
    map.put("NAD", "$#,##0.00");
    map.put("NGN", "₦#,##0.00");
    map.put("NIO", "C$ #,##0.00");
    map.put("NOK", "kr #.##0,00");
    map.put("NPR", "₨#,##0.00");
    map.put("NZD", "$#,##0.00");
    map.put("OMR", "﷼ #,##0.00");
    map.put("PAB", "B/. #,##0.00");
    map.put("PEN", "S/. #,##0.00");
    map.put("PGK", "K#,##0.00");
    map.put("PHP", "₱#,##0.00");
    map.put("PKR", "₨#,##0.00");
    map.put("PLN", "#.##0,00 zł");
    map.put("PYG", "₲ #.##0,00");
    map.put("QAR", "﷼ #,##0.00");
    map.put("RON", "#.##0,00 lei");
    map.put("RSD", "#.##0,00 Дин.");
    map.put("RUB", "#.##0,00 ₽");
    map.put("RWF", "RWF #.##0,00");
    map.put("SAR", "﷼ #,##0.00");
    map.put("SBD", "$#,##0.00");
    map.put("SCR", "₨#,##0.00");
    map.put("SDG", "£‏#,##0.00");
    map.put("SEK", "#.##0,00 kr");
    map.put("SGD", "$#,##0.00");
    map.put("SHP", "£#,##0.00");
    map.put("SLL", "Le#,##0.00");
    map.put("SOS", "S#,##0.00");
    map.put("SRD", "$#,##0.00");
    map.put("STD", "Db#,##0.00");
    map.put("SVC", "₡#,##0.00");
    map.put("SYP", "£ #,##0.00");
    map.put("SZL", "E#,##0.00");
    map.put("THB", "฿#,##0.00");
    map.put("TJS", "#.##");
    map.put("TMT", "#.##m");
    map.put("TND", "د.ت.‏ #,##.000");
    map.put("TOP", "T$#,##0.00");
    map.put("TRY", "#,##0.00 TL");
    map.put("TTD", "TT$#,##0.00");
    map.put("TWD", "NT$#,##0.00");
    map.put("TZS", "TSh#,##0.00");
    map.put("UAH", "#.##0,00₴");
    map.put("UGX", "USh#,##0.00");
    map.put("USD", "$#,##0.00");
    map.put("UYU", "$U #.##0,00");
    map.put("UZS", "#.##0,00 сўм");
    map.put("VEF", "Bs. F. #.##0,00");
    map.put("VND", "#.## ₫");
    map.put("VUV", "#,##VT");
    map.put("WST", "WS$#,##0.00");
    map.put("XAF", "#,##0.00F");
    map.put("XCD", "$#,##0.00");
    map.put("XOF", "#.##0,00F");
    map.put("XPF", "#,##0.00F");
    map.put("YER", "﷼ #,##0.00");
    map.put("ZAR", "R#.##0,00");
    map.put("ZMW", "ZK#,##0.00");
  }

  /**
   * Returns currency format
   * 
   * @param code
   * @return format
   */
  public static String get(String code) {
    return map.get(code);
  }
  
}