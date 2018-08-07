/*
 * Copyright (c) 2017, JCSS Technologies
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * 
 * * Redistributions in binary form must reproduce the above copyright notice, this
 *   list of conditions and the following disclaimer in the documentation and/or
 *   other materials provided with the distribution.
 * 
 * * Neither the name of Majenko Technologies nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * NodeMCU V1.0 Lolin(V3)
 * https://i2.wp.com/www.esploradores.com/wp-content/uploads/2016/08/PINOUT-NodeMCU_1.0-V2-y-V3.png?fit=1024%2C701
 * 
 * https://techtutorialsx.com/2016/10/22/esp8266-webserver-getting-query-parameters/
 * https://github.com/esp8266/Arduino/blob/master/libraries/ESP8266WebServer/examples/SDWebServer/SDWebServer.ino
 * https://github.com/esp8266/Arduino/blob/master/libraries/ESP8266WebServer/examples/FSBrowser/FSBrowser.ino
 * 
 * DHCP Problems - Fixing IP
 * https://github.com/esp8266/Arduino/issues/1959
 * 
 * Get List of Connected Devices
 * http://www.esp8266.com/viewtopic.php?p=30091
 * 
 * Cancel STA reconnection
 * http://bbs.espressif.com/viewtopic.php?t=324
 */
 
/* Create a WiFi access point and provide a web server on it. */

#include <ESP8266WiFi.h>
#include <WiFiClient.h> 
#include <ESP8266WebServer.h>
#include <ArduinoJson.h>
#include <map>
#include <list>
#include "EEPROM.h"
#include "DebugFunctions.h"
#include "DS1307.h"

extern "C" { 
  #include<user_interface.h>
}

#define D1PIN 05 // 
#define D2PIN 04 // 
#define D4PIN 02 // 
#define D5PIN 14 // 
#define D6PIN 12 // Hardware Reset

#define FANPIN D1PIN
#define LEDPIN D2PIN //
#define SDAPIN D4PIN
#define SCLPIN D5PIN

#define CHECK_BIT(var,pos) ((var) & (1<<(pos)))
#define INTERVAL_US 1000000

//using namespace std;


byte relON[] = {0xA0, 0x01, 0x01, 0xA2};  //Hex command to send to serial for open relay
byte relOFF[] = {0xA0, 0x01, 0x00, 0xA1}; //Hex command to send to serial for close relay


struct WifiConfig {
  const char *ssid;
  const char *password;  
  IPAddress ip;
  //IPAddress gateway;
  //IPAddress subnet; 
};


/* Set these to your desired credentials. */
const char *ssidAP = "Atmosphera";
const char *pwdAP = "atmosphera2018";
/* You can remove the password parameter if you want the AP to be open. */
IPAddress ipAP(192, 168, 1, 2); //default value
IPAddress gatewayAP(192, 168, 1, 1); //default value

//String ssidSTA = "UNITEC_VISITANTES";
//String pwdSTA = "Bem-vindo!"; 
IPAddress ipSTA(10, 23, 5, 201);
IPAddress gatewaySTA(10, 23, 1, 1);
IPAddress subnetSTA(255, 255, 255, 0);

//String ssidSTA = "UNITEC_USUARIOS";
//String pwdSTA = "#4tva82015"; 

String ssidSTA = "GVT-7B39";
String pwdSTA = "0071749692"; 

//String ssidSTA = "JCSSAP";
//String pwdSTA = "jcss8469"; 

//Modulo RTC DS1307 
DS1307 rtc(SDAPIN, SCLPIN); //SDA, SCL


struct Alerts {
  Alerts() : iId(0), iTime(0), iDuration(0), bWeekdays(0) {}
  Alerts(int newId, int newTime, int newDur, byte newWeek)
         : iId(newId), iTime(newTime), iDuration(newDur), bWeekdays(newWeek) {}
  
  int iId;         
  int iTime; //in minutes
  int iDuration; //timestamp in minutes = 00h00 23h59 = 0 - 1439
  byte bWeekdays;
};

struct WeekAlert {
  WeekAlert() : iId(0), iTime(0) {}
  WeekAlert(int newId, int newTime)
         : iId(newId), iTime(newTime) {}

  int iTime;
  int iId;  
};

std::map<int, Alerts> AlertsMap;
std::list<WifiConfig> wifiSTAList; //list of all STA configured
std::list<WeekAlert> WeekdaysList[8];

ESP8266WebServer server(80);
boolean bAP_Running = false;
boolean bSTA_Running = false;
boolean bCheckedAlerts = false;
int iConnectedAP = -1;
int iChannel = 11;

uint32_t iCurrentTime = 0;
os_timer_t mTimer;

bool _timeout = false;
bool _bLEDState = false;

void checkAlerts() {
  short iWeekday = getWeekday(iCurrentTime);
  short iNowTime = getTimeInMinutes(iCurrentTime);
  Alerts oAlert;
  short iON = 0;
  
  DEBUG_PRINTLN("RTC Weekday:" + String(iWeekday) + " Time: " + convertTime(iNowTime));
  
  //Get the Alerts for this weekday
  std::list<WeekAlert>::iterator wki = WeekdaysList[iWeekday].begin();
  
  //Loop the Alerts for today until the end
  while(wki != WeekdaysList[iWeekday].end()) {
    //DEBUG_PRINTLN("FOUND WEEKDAY");             
    //Get the Alert info from AlertsMap;
    auto it = AlertsMap.find( (*wki).iId );
    if (it != AlertsMap.end()) {      
      oAlert = (*it).second;       
      //DEBUG_PRINTLN("FOUND ALERT:");             
      //dumpAlert(oAlert);
      
      short startTime = (*wki).iTime;
      short endTime = startTime + oAlert.iDuration;
      DEBUG_PRINTLN("\tId:"+ String(oAlert.iId) + " Start: " + convertTime(startTime) + " End: " + convertTime(endTime));             
      if (iNowTime >= startTime && iNowTime < endTime) {
        iON++;      
      }
    }
    ++wki;     
  }

  if (iON > 0) {
    if (_bLEDState == false) {
      DEBUG_PRINTLN("FAN ON");             
      _bLEDState = true;
      digitalWrite(FANPIN, _bLEDState);
      Serial.write(relON, sizeof(relON));     // turns the relay ON
      delay(5000);        
    }
  } else {
    if (_bLEDState == true) {
      DEBUG_PRINTLN("FAN OFF");                   
      _bLEDState = false;
      digitalWrite(FANPIN, _bLEDState);
      Serial.write(relOFF, sizeof(relOFF));     // turns the relay relOFF
      delay(5000);
    }
  }

}

void inline handler (void){
  timer0_write(ESP.getCycleCount() + INTERVAL_US * 80); // 160 when running at 160mhz

  //Check Every Minute
  if (iCurrentTime % 60 == 0) {    
    //checkAlerts();
    bCheckedAlerts = false;
  }   

  iCurrentTime++;
  
}

//Nunca execute nada na interrupcao, apenas setar flags!
void tCallback(void *tCall){
    _timeout = true;
}

void handleHardReset() {
  DEBUG_PRINTLN("---->>>> HARDWARE RESET <<<------");

  EEPROMErase();
  
}

void usrInit(void){
    os_timer_setfn(&mTimer, tCallback, NULL);
    //The milliseconds parameter is the duration of the timer measured in milliseconds. The repeat parameter is whether or not the timer will restart once it has reached zero.
    os_timer_arm(&mTimer, 10000, true);    
}

void dumpClients() {
  DEBUG_PRINT(" Clients:\r\n");
  struct station_info *stat_info = wifi_softap_get_station_info();
  IPAddress address;
  struct ip_addr *pIPaddress;
  while (stat_info != NULL) {
    pIPaddress = &stat_info->ip;
    address = pIPaddress->addr;
    DEBUG_PRINT("\t");
    DEBUG_PRINT(address);
    DEBUG_PRINT("\r\n");
    stat_info = STAILQ_NEXT(stat_info, next);
  } 
}

bool enableWiFiSTA() {
  String sHostname = WiFi.softAPmacAddress();
  sHostname.replace(":","");
  sHostname = "ESP" + sHostname.substring(sHostname.length()-4, sHostname.length());
    
  DEBUG_PRINTLN("Configuring WiFi Point: "  + ssidSTA + " Password: " + pwdSTA + " hostname: " + sHostname);
    
  //start timer
  usrInit();
 
  WiFi.disconnect();
  
  WiFi.hostname(sHostname);

  IPAddress emptyIp(0,0,0,0);
  if (ipSTA != emptyIp) {    
    WiFi.config(ipSTA, gatewaySTA, subnetSTA);
    DEBUG_PRINT("Configuring WiFi IP: "); 
    DEBUG_PRINT(ipSTA);
    DEBUG_PRINT(" Gateway: "); 
    DEBUG_PRINT(gatewaySTA);
    DEBUG_PRINT(" Subnet: "); 
    DEBUG_PRINTLN(subnetSTA);    
  }
    
  if (WiFi.status() != WL_CONNECTED) 
    WiFi.begin(ssidSTA.c_str(), pwdSTA.c_str());
  
  DEBUG_PRINTLN("Connecting STA WiFi");
  int timeout = 20;
  while (WiFi.status() != WL_CONNECTED && !_timeout) {
    delay(500);
    DEBUG_PRINT(".");
    //timeout--;
  }
  DEBUG_PRINTLN();
  if (WiFi.status() == WL_CONNECTED) {
    //mySTA_IP = WiFi.localIP();
    DEBUG_PRINT("Connected, IP address: ");
    DEBUG_PRINTLN(WiFi.localIP());    
    bSTA_Running = true;
  } else {
    DEBUG_PRINTLN("Error connecting to WiFi");
    bSTA_Running = false;
  }

  return bSTA_Running;
}

void enableWiFiAP() {
  //IPAddress NMask(255, 255, 255, 0);
  String sMAC = WiFi.macAddress();
  sMAC.replace(":","");  
  String sAP = ssidAP + sMAC.substring(sMAC.length()-4, sMAC.length());
    
  DEBUG_PRINTLN("Configuring Access Point: "  + String(sAP) + " Password: " + String(pwdAP));
  
  //WiFi.softAPConfig(apIP, mySTA_IP, NMask);
  
  //if (!WiFi.softAP(ssidAP, pwdAP)) {
  if (!WiFi.softAP(sAP.c_str(), pwdAP)) {
    DEBUG_PRINTLN("Problems to create AP");    
    bAP_Running = false;
  } else {
    bAP_Running = true;
    ipAP = WiFi.softAPIP();
    DEBUG_PRINT("AP IP address: ");
    DEBUG_PRINTLN(ipAP);  
    DEBUG_PRINT("MAC address = ");
    DEBUG_PRINTLN(WiFi.softAPmacAddress().c_str());    
  }  
}


void scanWiFi() {
  DEBUG_PRINTLN("scan start");

  // WiFi.scanNetworks will return the number of networks found
  int n = WiFi.scanNetworks();
  char* ssidAux;
  
  DEBUG_PRINTLN("scan done");
  if (n == 0) {
    DEBUG_PRINTLN("no networks found");
  } else {
    DEBUG_PRINT(n);
    DEBUG_PRINTLN(" networks found");
    for (int i = 0; i < n; ++i)
    {
      // Print SSID and RSSI for each network found
      DEBUG_PRINT(i + 1);
      DEBUG_PRINT(": ");
      DEBUG_PRINT(WiFi.SSID(i));
      DEBUG_PRINT(": ");
      DEBUG_PRINT(WiFi.BSSIDstr(i));
      DEBUG_PRINT(" Channel: ");
      DEBUG_PRINT(WiFi.channel(i));
      DEBUG_PRINT(" (");
      DEBUG_PRINT(WiFi.RSSI(i));
      DEBUG_PRINT(")");
      DEBUG_PRINTLN((WiFi.encryptionType(i) == ENC_TYPE_NONE)?" ":"*");
      delay(10);
     
    }
  }
  DEBUG_PRINTLN();  
}

void setup() {
	delay(10);
	Serial.begin(9600);

#if DEBUG
  Serial.setDebugOutput(true);
#endif

  WiFi.mode(WIFI_AP_STA);

  delay(100);

  EEPROMInit();
  
  EEPROMWriteNetwork(ssidSTA, pwdSTA);

  EEPROMLoadAlerts();

  pinMode(D6PIN, INPUT_PULLUP);
  attachInterrupt(digitalPinToInterrupt(D6PIN), handleHardReset, FALLING);

  pinMode(FANPIN, OUTPUT);
  digitalWrite(FANPIN, 0);
  Serial.write(relOFF, sizeof(relOFF));     // turns the relay relOFF

  pinMode(LEDPIN, OUTPUT);
  digitalWrite(LEDPIN, 0);

  digitalWrite(LEDPIN, 0);
  
  //loadCredentials();
   
  //enableWiFiSTA();
 
  enableWiFiAP();
     
  server.on ( "/json", []() {
    String ReqJson = server.arg("plain");
    String message = "Body received:\n";
           message += ReqJson;
           message += "\n";

      //server.send(200, "text/plain", message);
      server.send ( 200, "application/json", "{\"Status\":\"Ok\"}" );
      DEBUG_PRINTLN(message);
  });
  
  server.onNotFound ( handleNotFound );
	server.on("/", handleRoot);
  server.on("/scan", HTTP_GET, handleScan);
  server.on("/scan", HTTP_POST, handleConnect);
  server.on("/password", HTTP_POST, handleAPPassword);
  server.on("/time", HTTP_POST, handleSetTime);
  server.on("/time", HTTP_GET, handleGetTime);
  server.on("/alerts", HTTP_GET, handleGetAlerts);
  server.on("/alerts", HTTP_POST, handleUpdateAlerts);
  server.on("/alerts", HTTP_DELETE, handleDeleteAlerts);

  //here the list of headers to be recorded
  const char * headerkeys[] = {"User-Agent","Cookie","plain"} ;
  size_t headerkeyssize = sizeof(headerkeys)/sizeof(char*);
  //ask server to track these headers
  server.collectHeaders(headerkeys, headerkeyssize );  
    
	server.begin();
	DEBUG_PRINTLN("HTTP server started");

  noInterrupts();
  timer0_isr_init();
  timer0_attachInterrupt(handler);
  timer0_write(ESP.getCycleCount() + INTERVAL_US * 80); // 160 when running at 160mhz
  interrupts();

#if DEBUG
  WiFi.printDiag(Serial);
#endif

  //Turn on RTC
  rtc.halt(false);
  
  //As linhas abaixo setam a data e hora do modulo
  //e podem ser comentada apos a primeira utilizacao
  //rtc.setDOW(TUESDAY);      //Define o dia da semana
  //rtc.setTime(13, 19, 0);     //Define o horario
  //rtc.setDate(7, 8, 2018);   //Define o dia, mes e ano
  
  //Definicoes do pino SQW/Out
  rtc.setSQWRate(SQW_RATE_1);
  rtc.enableSQW(true);

  //Mostra as informações no Serial Monitor
  Serial.print("Hora : ");
  Serial.print(rtc.getTimeStr());
  Serial.print(" ");
  Serial.print("Data : ");
  Serial.print(rtc.getDateStr(FORMAT_SHORT));
  Serial.print(" ");
  Serial.println(rtc.getDOWStr(FORMAT_SHORT));

  //iCurrentTime = (iWeekday-1)*24*60*60 + iTime*60;
  iCurrentTime = (rtc.getDOWInt() - 1)*24*60*60 + rtc.getTimeInSeconds();

  //DEBUGING WAKEUP
  for(int i=0; i<5; i++) {
    digitalWrite(LEDPIN, (i%2 == 0));
    if (i%2 == 0) {
      Serial.write(relOFF, sizeof(relOFF));     // turns the relay relOFF
    } else {
      Serial.write(relON, sizeof(relON));     // turns the relay relOFF
    }
    delay(6000);
  }
  Serial.write(relOFF, sizeof(relOFF));     // turns the relay relOFF
  digitalWrite(LEDPIN, 0);
    
}

void loop() {
	server.handleClient();

  int iTemp = WiFi.softAPgetStationNum();
  if (iTemp != iConnectedAP) {
    DEBUG_PRINT("Stations connected = ");
    DEBUG_PRINTLN(iTemp);
    iConnectedAP = iTemp;
  }


  if (_timeout){
      DEBUG_PRINTLN("TIME IS UP!");
      _timeout = false;
      if (WiFi.status() != WL_CONNECTED) { 
          DEBUG_PRINTLN("WIFI STA DISCONNECTING!");
          WiFi.setAutoConnect(false);
          WiFi.disconnect();
          os_timer_disarm(&mTimer);
          //wifi_station_set_reconnect_policy(false); // if the ESP8266 station connected to the router, and then the connection broke, ESP8266 will not try to reconnect to the router.
          //wifi_station_set_auto_connect(false); //the ESP8266 station will not try to connect to the router automatically when power on until wifi_station_connect is called.
          //wifi_station_disconnect(); // ESP8266 station disconnects to the router, or ESP8266 station stops trying to connect to the target router.
      }
  }

  if (!bCheckedAlerts) {    
    checkAlerts();
    bCheckedAlerts = true;
  }
  
  yield();
  
}
