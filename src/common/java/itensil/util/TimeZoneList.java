/*
 * Copyright 2004-2007 by Itensil, Inc.,
 * All rights reserved.
 * 
 * This software is the confidential and proprietary information
 * of Itensil, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Itensil.
 */
package itensil.util;

import java.util.*;


public class TimeZoneList {

    static class Backwards implements Comparator<String> {

        public int compare(String s1, String s2) {
           return -s1.compareTo(s2);
        }

    }

    // These values taken from a Windows XP SP1 registry entries from the
    // HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Time Zones
    // keys. Java timezones taken from SUN JDK 1.3.1_03 TimeZone.getAvailableIDs();
    // Geographic best guesses courtesy of the New Zealand education system
    public static TreeMap<String,String> zoneNames = new TreeMap<String,String>(new Backwards());
    public static HashMap<String,String> zones = new HashMap<String,String>(75);

    static {
         zoneNames.put("(GMT+13:00) Nuku'alofa", "Pacific/Tongatapu");
         zoneNames.put("(GMT+12:00) Fiji, Kamchatka, Marshall Is.", "Pacific/Fiji");
         zoneNames.put("(GMT+12:00) Auckland, Wellington", "Pacific/Auckland");
         zoneNames.put("(GMT+11:00) Magadan, Solomon Is., New Caledonia", "Asia/Magadan");
         zoneNames.put("(GMT+10:00) Vladivostok", "Asia/Vladivostok");
         zoneNames.put("(GMT+10:00) Hobart", "Australia/Hobart");
         zoneNames.put("(GMT+10:00) Guam, Port Moresby", "Pacific/Guam");
         zoneNames.put("(GMT+10:00) Canberra, Melbourne, Sydney", "Australia/Sydney");
         zoneNames.put("(GMT+10:00) Brisbane", "Australia/Brisbane");
         zoneNames.put("(GMT+09:30) Adelaide", "Australia/Adelaide");
         zoneNames.put("(GMT+09:00) Yakutsk", "Asia/Yakutsk");
         zoneNames.put("(GMT+09:00) Seoul", "Asia/Seoul");
         zoneNames.put("(GMT+09:00) Osaka, Sapporo, Tokyo", "Asia/Tokyo");
         zoneNames.put("(GMT+08:00) Taipei", "Asia/Taipei");
         zoneNames.put("(GMT+08:00) Perth", "Australia/Perth");
         zoneNames.put("(GMT+08:00) Kuala Lumpur, Singapore", "Asia/Kuala_Lumpur");
         zoneNames.put("(GMT+08:00) Irkutsk, Ulaan Bataar", "Asia/Irkutsk");
         zoneNames.put("(GMT+08:00) Beijing, Chongqing, Hong Kong, Urumqi", "Asia/Hong_Kong");
         zoneNames.put("(GMT+07:00) Krasnoyarsk", "Asia/Krasnoyarsk");
         zoneNames.put("(GMT+07:00) Bangkok, Hanoi, Jakarta", "Asia/Bangkok");
         zoneNames.put("(GMT+06:30) Rangoon", "Asia/Rangoon");
         zoneNames.put("(GMT+06:00) Sri Jayawardenepura", "Asia/Colombo");
         zoneNames.put("(GMT+06:00) Astana, Dhaka", "Asia/Dhaka");
         zoneNames.put("(GMT+06:00) Almaty, Novosibirsk", "Asia/Almaty");
         zoneNames.put("(GMT+05:45) Kathmandu", "Asia/Katmandu");
         zoneNames.put("(GMT+05:30) Chennai, Kolkata, Mumbai, New Delhi", "Asia/Calcutta");
         zoneNames.put("(GMT+05:00) Islamabad, Karachi, Tashkent", "Asia/Karachi");
         zoneNames.put("(GMT+05:00) Ekaterinburg", "Asia/Yekaterinburg");
         zoneNames.put("(GMT+04:30) Kabul", "Asia/Kabul");
         zoneNames.put("(GMT+04:00) Baku, Tbilisi, Yerevan", "Asia/Baku");
         zoneNames.put("(GMT+04:00) Abu Dhabi, Muscat", "Asia/Dubai");
         zoneNames.put("(GMT+03:30) Tehran", "Asia/Tehran");
         zoneNames.put("(GMT+03:00) Nairobi", "Africa/Nairobi");
         zoneNames.put("(GMT+03:00) Moscow, St. Petersburg, Volgograd", "Europe/Moscow");
         zoneNames.put("(GMT+03:00) Kuwait, Riyadh", "Asia/Kuwait");
         zoneNames.put("(GMT+03:00) Baghdad", "Asia/Baghdad");
         zoneNames.put("(GMT+02:00) Jerusalem", "Asia/Jerusalem");
         zoneNames.put("(GMT+02:00) Helsinki, Kyiv, Riga, Sofia, Tallinn, Vilnius", "Europe/Helsinki");
         zoneNames.put("(GMT+02:00) Harare, Pretoria", "Africa/Harare");
         zoneNames.put("(GMT+02:00) Cairo", "Africa/Cairo");
         zoneNames.put("(GMT+02:00) Bucharest", "Europe/Bucharest");
         zoneNames.put("(GMT+02:00) Athens, Istanbul, Minsk", "Europe/Athens");
         zoneNames.put("(GMT+01:00) West Central Africa", "Africa/Lagos");
         zoneNames.put("(GMT+01:00) Sarajevo, Skopje, Warsaw, Zagreb", "Europe/Warsaw");
         zoneNames.put("(GMT+01:00) Brussels, Copenhagen, Madrid, Paris", "Europe/Brussels");
         zoneNames.put("(GMT+01:00) Belgrade, Bratislava, Budapest, Ljubljana, Prague", "Europe/Belgrade");
         zoneNames.put("(GMT+01:00) Amsterdam, Berlin, Bern, Rome, Stockholm, Vienna", "Europe/Amsterdam");
         zoneNames.put("(GMT) Casablanca, Monrovia", "Africa/Casablanca");
         zoneNames.put("(GMT) Greenwich Mean Time : Dublin, Edinburgh, Lisbon, London", "Europe/Dublin");
         zoneNames.put("(GMT-01:00) Azores", "Atlantic/Azores");
         zoneNames.put("(GMT-01:00) Cape Verde Is.", "Atlantic/Cape_Verde");

         zoneNames.put("(GMT-02:00) Mid-Atlantic", "Atlantic/South_Georgia");
         zoneNames.put("(GMT-03:00) Brasilia", "America/Sao_Paulo");
         zoneNames.put("(GMT-03:00) Buenos Aires, Georgetown", "America/Buenos_Aires");
         zoneNames.put("(GMT-03:00) Greenland", "America/Thule");
         zoneNames.put("(GMT-03:30) Newfoundland", "America/St_Johns");
         zoneNames.put("(GMT-04:00) Atlantic Time (Canada)", "America/Montreal");
         zoneNames.put("(GMT-04:00) Caracas, La Paz", "America/Caracas");
         zoneNames.put("(GMT-04:00) Santiago", "America/Santiago");
         zoneNames.put("(GMT-05:00) Bogota, Lima, Quito", "America/Bogota");
         zoneNames.put("(GMT-05:00) Eastern Time (US & Canada)", "America/New_York");
         zoneNames.put("(GMT-05:00) Indiana (East)", "America/Indianapolis");
         zoneNames.put("(GMT-06:00) Central America", "America/Costa_Rica");
         zoneNames.put("(GMT-06:00) Central Time (US & Canada)", "America/Chicago");
         zoneNames.put("(GMT-06:00) Guadalajara, Mexico City, Monterrey", "America/Mexico_City");
         zoneNames.put("(GMT-06:00) Saskatchewan", "America/Winnipeg");
         zoneNames.put("(GMT-07:00) Arizona", "America/Phoenix");
         zoneNames.put("(GMT-07:00) Chihuahua, La Paz, Mazatlan", "America/Tegucigalpa");
         zoneNames.put("(GMT-07:00) Mountain Time (US & Canada)", "America/Denver");
         zoneNames.put("(GMT-08:00) Pacific Time (US & Canada); Tijuana", "America/Los_Angeles");
         zoneNames.put("(GMT-09:00) Alaska", "America/Anchorage");
         zoneNames.put("(GMT-10:00) Hawaii", "Pacific/Honolulu");
         zoneNames.put("(GMT-11:00) Midway Island, Samoa", "Pacific/Apia");
         zoneNames.put("(GMT-12:00) International Date Line West", "MIT");

        
        for (Map.Entry<String, String> ent : zoneNames.entrySet()) {            
            zones.put(ent.getValue(), ent.getKey());
        }
    }



}
