package com.example.lieu;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import java.util.ArrayList;

public class APFilter {
    public static ArrayList<ScanResult> FilterScanResults(ArrayList<ScanResult> unfiltered)
    {
        ArrayList<ScanResult> toRemove = new ArrayList<ScanResult>();

        //SSIDs, if any part matches remove it
        for (ScanResult s : unfiltered)
        {
            System.out.println(s.SSID);


            if(WifiManager.calculateSignalLevel(s.level, 10) < 6)
            {
                toRemove.add(s);
                break;
            }

            for(int i = 0; i < SSID_Identifiers.length; i++)
            {
                System.out.println(SSID_Identifiers[i][1]);
                if(s.SSID.toLowerCase().contains(SSID_Identifiers[i][1].toLowerCase()))
                {
                    //if match
                    toRemove.add(s);
                }


            }

        }

        if(toRemove.size() != 0)
        {
            for (ScanResult r : toRemove)
            {
                unfiltered.remove(r);
            }
            toRemove.clear();
        }

        //BSSIDs, only match the first half for filtering
        for (ScanResult s : unfiltered)
        {
            System.out.println(s.BSSID.substring(0, 7));
            for(int i = 0; i < BSSID_Identifiers.length; i++)
            {
                if(s.BSSID.substring(0, 7) == BSSID_Identifiers[i][1])
                {
                    System.out.println(s.BSSID.substring(0, 7));
                    //if match
                    toRemove.add(s);
                }
            }
        }

        if(toRemove.size() != 0)
        {
            for (ScanResult r : toRemove)
            {
                unfiltered.remove(r);
            }
        }

        //now filtered
        return unfiltered;
    }

    //based on the work by Radiobeacon folk and
    //https://stackoverflow.com/a/23916044
    //+ own intuitions
    private static final String[][] SSID_Identifiers = {
            {"default", "ASUS"},
            {"default", "Android Barnacle Wifi Tether"},
            {"default", "AndroidAP"},
            {"default", "AndroidTether"},
            {"default", "blackberry mobile hotspot"},
            {"default", "Clear Spot"},
            {"default", "ClearSpot"},
            {"default", "docomo"},
            {"Maintenance network on German ICE trains", "dr_I)p"},
            {"default", "Galaxy Note"},
            {"default", "Galaxy S"},
            {"default", "Galaxy Tab"},
            {"default", "HelloMoto"},
            {"default", "HTC "},
            {"default", "iDockUSA"},
            {"default", "iHub_"},
            {"default", "iPad"},
            {"default", "ipad"},
            {"default", "iPhone"},
            {"default", "LG VS910 4G"},
            {"default", "MIFI"},
            {"default", "MiFi"},
            {"default", "mifi"},
            {"default", "MOBILE"},
            {"default", "Mobile"},
            {"default", "mobile"},
            {"default", "myLGNet"},
            {"default", "myTouch 4G Hotspot"},
            {"default", "PhoneAP"},
            {"default", "SAMSUNG"},
            {"default", "Samsung"},
            {"default", "Sprint"},
            {"Long haul buses", "megabus-wifi"},
            {"German long haul buses", "DeinBus"},
            {"German long haul buses", "MeinFernbus"},
            {"German long haul buses", "adac_postbus"},
            {"German long haul buses", "flixbus"},
            {"Long haul buses", "eurolines"},
            {"Long haul buses", "ecolines"},
            {"Hurtigen lines", "guest@MS"},
            {"Hurtigen lines", "admin@MS"},
            {"German fast trains", "Telekom_ICE"},
            {"European fast trains", "thalysnet"},
            {"default", "Trimble "},
            {"default", "Verizon"},
            {"default", "VirginMobile"},
            {"default", "VTA Free Wi-Fi"},
            {"default", "webOS Network"},
            {"GoPro cams", "goprohero3"},
            {"Swiss Post Auto Wifi", "PostAuto"},
            {"Swiss Post Auto Wifi French", "CarPostal"},
            {"Swiss Post Auto Wifi Italian", "AutoPostale"},
            {"Huawei Smartphones", "Huawei"},
            {"Huawei Smartphones", "huawei"},
            {"Xiaomi Smartphones", "紅米手機"},

            // mobile hotspots
            {"German 1und1 mobile hotspots", "1und1 mobile"},
            {"xperia tablet", "xperia tablet"},
            {"Sony devices", "XPERIA"},
            {"xperia tablet", "androidhotspot"},
            {"HP laptops", "HP envy"},


            // some ssids from our friends at https://github.com/dougt/MozStumbler
            {"default", "ac_transit_wifi_bus"},
            {"Nazareen express transportation services (Israel)", "Afifi"},
            {"Oslo airport express train on-train WiFi (Norway)","AirportExpressZone"},
            {"default", "AmtrakConnect"},
            {"default", "amtrak_"},
            {"Arriva Nederland on-train Wifi (Netherlands)", "arriva"},
            {"Arcticbus on-bus WiFi (Sweden)","Arcticbus Wifi"},
            {"Swiss municipial busses on-bus WiFi (Italian speaking part)","AutoPostale"},
            {"Barcelona tourisitic buses http://barcelonabusturistic.cat","Barcelona Bus Turistic "},
            {"Tromso on-boat (and probably bus) WiFi (Norway)"	,"Boreal_Kundenett"},
            {"Bus4You on-bus WiFi (Norway)","Bus4You-"},
            {"Capital Bus on-bus WiFi (Taiwan)", "CapitalBus"},
            {"Swiss municipial busses on-bus WiFi (French speaking part)" ,"CarPostal"},
            {"Ceske drahy (Czech railways)", "CDWiFi"},
            {"Copenhagen S-Tog on-train WiFi: http://www.dsb.dk/s-tog/kampagner/fri-internet-i-s-tog" ,"CommuteNet"},
            {"CSAD Plzen","csadplzen_bus"},
            {"Egged transportation services (Israel)", "egged.co.il"},
            {"Empresa municipal de transportes de Madrid","EMT-Madrid"},
            {"First Bus wifi (United Kingdom)","first-wifi"},
            {"Oslo airport transportation on-bus WiFi (Norway)" ,"Flybussekspressen"},
            {"Airport transportation on-bus WiFi all over Norway (Norway)" ,"Flybussen"},
            {"Flygbussarna.se on-bus WiFi (Sweden)"	,"Flygbussarna Free WiFi"},
            {"GB Tours transportation services (Israel)", "gb-tours.com"},
            {"default", "GBUS"},
            {"default", "GBusWifi"},
            {"Gogo in-flight WiFi", "gogoinflight"},
            {"Koleje Slaskie transportation services (Poland)" ,"Hot-Spot-KS"},
            {"ISRAEL-RAILWAYS","ISRAEL-RAILWAYS"},
            {"Stavanger public transport on-boat WiFi (Norway)"	,"Kolumbus"},
            {"Kystbussen on-bus WiFi (Norway)" ,"Kystbussen_Kundennett"},
            {"Hungarian State Railways onboard hotspot on InterCity trains (Hungary)", "MAVSTART-WiFi"},
            {"Nateev Express transportation services (Israel)"	,"Nateev-WiFi"},
            {"National Express on-bus WiFi (United Kingdom)" ,"NationalExpress"},
            {"Norgesbuss on-bus WiFi (Norway)"	,"Norgesbuss"},
            {"Norwegian in-flight WiFi (Norway)" ,"Norwegian Internet Access"},
            {"NSB on-train WiFi (Norway)"	,"NSB_INTERAKTIV"},
            {"Omnibus transportation services (Israel)", "Omni-WiFi"},
            {"OnniBus.com Oy on-bus WiFi (Finland)"	,"onnibus.com"},
            {"Oxford Tube on-bus WiFi (United Kindom)" ,"Oxford Tube"},
            {"Swiss municipial busses on-bus WiFi (German speaking part)" ,"PostAuto"},
            {"Qbuzz on-bus WiFi (Netherlands)", "QbuzzWIFI"},
            {"default", "SF Shuttle Wireless"},
            {"default", "ShuttleWiFi"},

            {"Southwest Airlines in-flight WiFi",  "Southwest WiFi"},
            {"default", "SST-PR-1"}, // Sears Home Service van hotspot?!
            {"Stagecoach on-bus WiFi (United Kingdom)" ,"stagecoach-wifi"},

            {"Taipei City on-bus WiFi (Taiwan)", "TPE-Free Bus"},
            {"Taipei City on-bus WiFi (Taiwan)", "NewTaipeiBusWiFi"},
            {"Taiwan transport on-bus WiFi (Taiwan)", "Y5Bus_4G"},
            {"Taiwan transport on-bus WiFi (Taiwan)", "Y5Bus_LTE"},
            {"(Taiwan) Taoyuan MRT", "TyMetro"},
            {"Taiwan High Speed Rail on-train WiFi", "THSR-VeeTIME"},
            {"Triangle Transit on-bus WiFi"	,"TriangleTransitWiFi_"},
            {"Nederlandse Spoorwegen on-train WiFi by T-Mobile (Netherlands)", "tmobile"},
            {"Triangle Transit on-bus WiFi","TriangleTransitWiFi_"},
            {"VR on-train WiFi (Finland)", "VR-junaverkko"},
            {"Boreal on-bus WiFi (Norway)" ,"wifi@boreal.no"},
            {"Nettbuss on-bus WiFi (Norway)", "wifi@nettbuss.no"},
            {"BART", "wifi_rail"},

            //device identifiers
            {"default", "MacBook"},
            {"default", "MacBook Pro"},
            {"default", "MiFi"},
            {"default", "MyWi"},
            {"default", "Tether"},
            {"default", "iPad"},
            {"default", "iPhone"},
            {"default", "ipad"},
            {"default", "iphone"},
            {"default", "tether"},
            {"default", "adhoc"},
            {"Google's SSID opt-out", "_nomap"}
    };

    //BSSID's from https://github.com/openbmap/radiocells-scanner-android/blob/bf361b8b62a07580fe2393ff83955bc81cbac262/android/src/org/openbmap/services/wireless/blacklists/BssidBlackListBootstraper.java
    //this is old data lmao
    private  static final String[][] BSSID_Identifiers = {
            // automotive manufacturers
            {"Harman/Becker Automotive Systems, used by Audi", "00:1C:D7"},
            {"Harman/Becker Automotive Systems GmbH", "9C:DF:03"},
            {"Continental Automotive Systems", "00:1E:AE"},
            {"Continental Automotive Systems", "00:54:AF"},
            {"Bosch Automotive Aftermarket", "70:C6:AC"},
            {"Continental Automotive Czech Republic s.r.o.", "9C:28:BF"},
            {"Robert Bosch LLC Automotive Electronics", "D0:B4:98"},
            {"Panasonic Automotive Systems Company of America", "E0:EE:1B"},
            {"QCOM Technology Inc (Reporting as InCar Hotspot, probably Daimler)", "00:0D:F0"},
            {"LessWire AG","00:06:C6"},
            {"Wistron Neweb Corp.","00:0B:6B"}, // device reporting as ssid Moovbox
            {"Ford Motor Company", "00:26:B4"},

            // Mobile devices
            {"Apple", "00:26:B0"},
            {"Apple", "00:26:BB"},
            {"Apple Computer Inc.", "00:19:E3"},
            {"Apple", "00:25:00"},
            {"Apple", "00:26:4A"},
            {"Apple", "00:C6:10"},

            // LG
            {"LG Electronics ", "00:AA:70"},
            {"LG Electronics, Nexus 4",	"10:68:3F"},
            {"LG Electronics", "C4:43:8F"},

            // Murata Manufactoring, used in LG and some other devices
            {"Murata Manufacturing Co.", "00:21:E8"},
            {"Murata Manufacturing Co.", "00:26:E8"},
            {"Murata Manufacturing Co.", "00:37:6D"},
            {"Murata Manufacturing Co., Ltd.", "04:46:65"},
            {"Murata Manufacturing Co., Ltd.", "44:A7:CF"},
            {"Murata Manufacturing Co., Ltd.", "40:F3:08"},
            {"Murata Manufactuaring Co.,Ltd.", "60:21:C0"},

            // Sony Mobile
            {"Sony Ericsson Mobile Communications AB", "00:0A:D9",},
            {"Sony Ericsson Mobile Communications AB", "00:0E:07"},
            {"Sony Ericsson Mobile Communications AB", "00:0F:DE"},
            {"Sony Ericsson Mobile Communications AB", "00:12:EE"},
            {"Sony Ericsson Mobile Communications AB", "00:16:20"},
            {"Sony Ericsson Mobile Communications AB", "00:16:B8"},
            {"Sony Ericsson Mobile Communications AB", "00:18:13"},
            {"Sony Ericsson Mobile Communications AB", "00:19:63"},
            {"Sony Ericsson Mobile Communications AB", "00:1A:75"},
            {"Sony Ericsson Mobile Communications AB", "00:1B:59"},
            {"Sony Ericsson Mobile Communications AB", "00:1C:A4"},
            {"Sony Ericsson Mobile Communications AB", "00:1D:28"},
            {"Sony Ericsson Mobile Communications AB", "00:1E:45"},
            {"Sony Ericsson Mobile Communications AB", "00:1F:E4"},
            {"Sony Ericsson Mobile Communications AB", "00:21:9E"},
            {"Sony Ericsson Mobile Communications AB", "00:22:98"},
            {"Sony Ericsson Mobile Communications AB", "00:23:45"},
            {"Sony Ericsson Mobile Communications AB", "00:23:F1"},
            {"Sony Ericsson Mobile Communications AB", "00:24:EF"},
            {"Sony Ericsson Mobile Communications AB", "00:25:E7"},
            {"Sony Mobile Communications AB", "00:EB:2D"},
            {"Sony Mobile Communications AB", "18:00:2D"},
            {"Sony Mobile Communications AB", "1C:7B:21"},
            {"Sony Mobile Communications AB", "20:54:76"},
            {"Sony Ericsson Mobile Communications AB", "24:21:AB",},
            {"Sony Ericsson Mobile Communications AB", "30:17:C8"},
            {"Sony Ericsson Mobile Communications AB", "30:39:26"},
            {"Sony Ericsson Mobile Communications AB", "40:2B:A1"},
            {"Sony Mobile Communications AB", "4C:21:D0"},
            {"Sony Ericsson Mobile Communications AB", "58:17:0C"},
            {"Sony Ericsson Mobile Communications AB", "5C:B5:24"},
            {"Sony Mobile Communications AB", "68:76:4F"},
            {"Sony Ericsson Mobile Communications AB", "6C:0E:0D"},
            {"Sony Ericsson Mobile Communications AB", "6C:23:B9"},
            {"Sony Ericsson Mobile Communications AB", "84:00:D2"},
            {"Sony Ericsson Mobile Communications AB", "8C:64:22"},
            {"Sony Ericsson Mobile Communications AB", "90:C1:15"},
            {"Sony Mobile Communications AB", "94:CE:2C"},
            {"Sony Mobile Communications AB", "B4:52:7D"},
            {"Sony Mobile Communications AB", "B4:52:7E"},
            {"Sony Ericsson Mobile Communications AB", "B8:F9:34"},
            {"Sony Mobile Communications AB", "D0:51:62"},
            {"Sony Mobile Communications AB", "E0:63:E5"},

            // HTC
            {"HTC Corporation", "00:23:76"},
            {"HTC Corporation", "38:E7:D8"},
            {"HTC Corporation", "64:A7:69"},
            {"HTC Corporation", "90:21:55"},
            {"HTC Corporation", "A8:26:D9"},
            {"HTC Corporation", "F8:DB:7F"},

            // ZTE
            {"zte corporation", "00:22:93"},
            {"zte corporation", "68:1A:B2"},
            {"zte corporation", "6C:8B:2F"},
            {"zte corporation", "84:74:2A"},
            {"zte corporation", "8C:E0:81"},
            {"zte corporation", "98:F5:37"},
            {"zte corporation", "9C:D2:4B"},

            // Samsung
            {"Samsung Electronics Co.", "00:07:AB"},
            {"Samsung Electronics Co.", "00:12:47"},
            {"Samsung Electronics Co.", "00:1D:F6"},
            {"Samsung Electro-Mechanics", "00:21:19"},
            {"Samsung Electro-Mechanics", "00:26:37"}, // e.g. XSBoxGo
            {"Samsung Electro Mechanics Co.,Ltd.","50:CC:F8"},
            {"Samsung Electronics Co.,Ltd","50:F5:20"},
            {"SAMSUNG ELECTRO-MECHANICS CO., LTD.", "5C:A3:9D"},
            {"Samsung Electronics Co.,Ltd","60:A1:0A"},
            {"Samsung Electro Mechanics Co.,Ltd.","88:32:9B"},
            {"Samsung Electronics Co.", "8C:77:12"},
            {"Samsung Electronics Co.", "9C:E6:E7"},
            {"SamsungE", "A0:21:B7"},
            {"SAMSUNG ELECTRO MECHANICS CO., LTD.", "CC:3A:61"},
            {"Samsung Electronics Co.", "D0:66:7B"},
            {"Samsung Electronics Co.", "D0:C1:B1"},
            {"Samsung Electronics Co.,Ltd", "F0:E7:7E"},

            // Huawei
            {"HUAWEI TECHNOLOGIES CO.", "00:E0:FC"},
            {"Huawei Technologies Co., Ltd","0C:37:DC"},
            {"Huawei Technologies Co., Ltd","20:F3:A3"},
            {"Huawei Technologies Co., Ltd","34:6B:D3"},
            {"Huawei Technologies Co., Ltd","80:B6:86"},
            {"Huawei Technologies Co., Ltd","88:53:D4"},
            {"Huawei Technologies Co., Ltd","AC:E8:7B"},
            {"Huawei Technologies Co., Ltd","C8:D1:5E"},
            {"Huawei Technologies Co., Ltd","E8:CD:2D"},
            {"Huawei Technologies Co., Ltd","F4:55:9C"},

            // GoPro
            {"GoPro", "D8:96:85"},

            // Longcheer - mobile 3G boxes, e.g. XSBoxGo
            {"Longcheer Technology (Singapore) Pte Ltd", "00:23:B1"},

            // mostly devices
            // 	- reporting ssid AndroidAP
            //  - catched during 'Autobahn hunting', i.e. driving around on highways and find
            //	  wifis where no building are around
            {"Shenzhen Huawei Communication Technologies Co., Ltd", "20:2B:C1"},
            {"TCT Mobile Limited", "4C:0B:3A"},
            {"EQUIP'TRANS", "00:01:00"},
            {"CyberTAN Technology", "00:01:36"},
            {"PORTech Communications", "00:03:7E"},
            {"Atheros Communications", "00:03:7F"},
            {"Adax", "00:07:10"},
            {"Qisda Corporation", "00:17:CA"},
            {"UNIGRAND LTD", "00:18:00"},
            {"SIM Technology Group Shanghai Simcom Ltd.", "00:18:60"},
            {"Cameo Communications", "00:18:E7"},
            {"Intelliverese - DBA Voicecom", "00:19:00"},
            {"YuHua TelTech (ShangHai) Co.", "00:19:65"},
            {"Hon Hai Precision Ind. Co.", "00:19:7D"},
            {"Panasonic Mobile Communications Co.", "00:19:87"},
            {"VIZIO", "00:19:9D"},
            {"Boundary Devices", "00:19:B8"},
            {"MICRO-STAR INTERNATIONAL CO.", "00:19:DB"},
            {"FusionDynamic Ltd.", "00:1A:91"},
            {"ASUSTek COMPUTER INC.", "00:1A:92"},
            {"Hisense Mobile Communications Technoligy Co.", "00:1A:95"},
            {"ECLER S.A.", "00:1A:96"},
            {"Asotel Communication Limited Taiwan Branch", "00:1A:98"},
            {"Smarty (HZ) Information Electronics Co.", "00:1A:99"},
            {"ShenZhen Kang Hui Technology Co.", "00:1B:10"},
            {"Nintendo Co.", "00:1B:EA"},
            {"Hon Hai Precision Ind. Co.", "00:1C:26"},
            {"AirTies Wireless Networks", "00:1C:A8"},
            {"Shenzhen Sang Fei Consumer Communications Co.", "00:1D:07"},
            {"ARRIS Group", "00:1D:D0"},
            {"Palm", "00:1D:FE"},
            {"ShenZhen Huawei Communication Technologies Co.", "00:1E:10"},
            {"Hon Hai Precision Ind.Co.", "00:1E:4C"},
            {"Wingtech Group Limited", "00:1E:AD"},
            {"Edimax Technology Co. Ltd.", "00:1f:1f"},
            {"Hon Hai Precision Ind. Co.", "00:1F:E1"},
            {"LEXMARK INTERNATIONAL", "00:20:00"},
            {"JEOL SYSTEM TECHNOLOGY CO. LTD", "00:20:10"},
            {"Motorola Mobility", "00:21:36"},
            {"SMT C Co.", "00:22:31"},
            {"Digicable Network India Pvt. Ltd.", "00:22:5D"},
            {"Liteon Technology Corporation", "00:22:5F"},
            {"Hewlett-Packard Company", "00:22:64"},
            {"Kyocera Corporation", "00:22:94"},
            {"Nintendo Co.", "00:22:D7"},
            {"AMPAK Technology", "00:22:F4"},
            //{"Arcadyan Technology Corporation, has mobile as well as fixed devices", "00:23:08"},
            {"LNC Technology Co.", "00:23:10"},
            {"Hon Hai Precision Ind. Co.", "00:23:4D"},
            {"AzureWave Technologies (Shanghai) Inc.", "00:24:23"},
            {"Onda Communication spa", "00:24:6F"},
            {"Universal Global Scientific Industrial Co.", "00:24:7E"},
            {"Sony Computer Entertainment Inc.", "00:24:8D"},
            {"ASRock Incorporation", "00:25:22"},
            {"Hon Hai Precision Ind. Co.", "00:25:56"},
            {"Microsoft Corporation", "00:25:AE"},
            {"TEAC Australia Pty Ltd.", "00:26:00"},
            {"Trapeze Networks", "00:26:3E"},
            {"Hon Hai Precision Ind. Co.", "00:26:5C"},
            {"Universal Global Scientific Industrial Co.", "00:27:13"},
            {"Rebound Telecom. Co.", "00:27:15"},
            {"EFM Networks", "00:08:9F"},
            {"TwinHan Technology Co.", "00:08:CA"},
            {"Simple Access Inc.", "00:09:10"},
            {"Shenzhen Tp-Link Technology Co Ltd.", "00:0A:EB"},
            {"Airgo Networks", "00:0A:F5"},
            {"AXIS COMMUNICATIONS AB", "00:40:8C"},
            {"MARVELL SEMICONDUCTOR", "00:50:43"},
            {"AIWA CO.", "00:50:71"},
            {"CORVIS CORPORATION", "00:50:72"},
            {"IEEE REGISTRATION AUTHORITY", "00:50:C2"},
            {"EPIGRAM", "00:90:4C"},
            {"CYBERTAN TECHNOLOGY", "00:90:A2"},
            {"K.J. LAW ENGINEERS", "00:90:C0"},
            {"THE APPCON GROUP", "00:A0:6F"},
            {"MITSUMI ELECTRIC CO.", "00:A0:96"},
            {"ALFA", "00:C0:CA"},
            {"KYOCERA CORPORATION", "00:C0:EE"},
            {"AMIGO TECHNOLOGY CO.", "00:D0:41"},
            {"REALTEK SEMICONDUCTOR CORP.", "00:E0:4C"},
            {"KODAI HITEC CO.", "00:E0:54"},
            {"MATSUSHITA KOTOBUKI ELECTRONICS INDUSTRIES", "00:E0:5C"},
            {"Traverse Technologies Australia", "00:0A:FA"},
            {"Lumenera Corporation", "00:0B:E2"},
            {"Ralink Technology", "00:0C:43"},
            {"Cooper Industries Inc.", "00:0C:C1"},
            {"Arima Communication Corporation", "00:0D:92"},
            {"Advantech AMT Inc.", "00:0E:02"},
            {"CTS electronics", "00:0E:72"},
            {"zioncom", "00:0E:E8"},
            {"CIMSYS Inc", "00:11:22"},
            {"Chi Mei Communication Systems", "00:11:94"},
            {"TiVo", "00:11:D9"},
            {"ASKEY COMPUTER CORP.", "00:11:F5"},
            {"Camille Bauer", "00:12:34"},
            {"ConSentry Networks", "00:12:36"},
            {"Lenovo Mobile Communication Technology Ltd.", "00:12:FE"},
            {"GuangZhou Post Telecom Equipment ltd", "00:13:13"},
            {"AMOD Technology Co.", "00:13:F1"},
            {"Motorola Mobility", "00:14:9A"},
            {"Gemtek Technology Co.", "00:14:A5"},
            {"Intel Corporate", "00:15:00"},
            {"Actiontec Electronics", "00:15:05"},
            {"LibreStream Technologies Inc.", "00:16:13"},
            {"Sunhillo Corporation", "00:16:43"},
            {"TPS", "00:16:6A"},
            {"Yulong Computer Telecommunication Scientific Co.", "00:16:6D"},
            {"Dovado FZ-LLC", "00:16:A6"},
            {"Compal Communications", "00:16:D4"},
            {"ARCHOS", "00:16:DC"},
            {"Methode Electronics", "00:17:05"},
            {"YOSIN ELECTRONICS CO.", "00:17:A6"},
            {"SK Telesys", "00:17:B2"},
            {"KTF Technologies Inc.", "00:17:C3"},
    };


}
