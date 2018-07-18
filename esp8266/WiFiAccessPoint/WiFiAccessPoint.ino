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
 * 
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

extern "C" { 
  #include<user_interface.h>
}


#define D6PIN 12 // Hardware Reset

#define CHECK_BIT(var,pos) ((var) & (1<<(pos)))
#define INTERVAL_US 1000000

//using namespace std;

struct WifiConfig {
  const char *ssid;
  const char *password;  
  IPAddress ip;
  //IPAddress gateway;
  //IPAddress subnet; 
};


/* Set these to your desired credentials. */
const char *ssidAP = "UnitecDemoServer";
const char *pwdAP = "unitec2017";
/* You can remove the password parameter if you want the AP to be open. */
IPAddress ipAP(192, 168, 1, 2); //default value
IPAddress gatewayAP(192, 168, 1, 1); //default value



String ssidSTA = "UNITEC_VISITANTES";
String pwdSTA = "Bem-vindo!"; 
IPAddress ipSTA(10, 23, 5, 201);
IPAddress gatewaySTA(10, 23, 1, 1);
IPAddress subnetSTA(255, 255, 255, 0);

//const char *ssidSTA = "UNITEC_USUARIOS";
//const char *pwdSTA = "#4tva82015"; 

//const char *ssidSTA = "GVT-7B39";
//const char *pwdSTA = "0071749692"; 

//const char *ssidSTA = "JCSSAP";
//const char *pwdSTA = "jcss8469"; 

const int led = 2;

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
  
  Serial.println("RTC Weekday:" + String(iWeekday) + " Time: " + convertTime(iNowTime));
  
  //Get the Alerts for this weekday
  std::list<WeekAlert>::iterator wki = WeekdaysList[iWeekday].begin();
  
  //Loop the Alerts for today until the end
  while(wki != WeekdaysList[iWeekday].end()) {
    //Serial.println("FOUND WEEKDAY");             
    //Get the Alert info from AlertsMap;
    auto it = AlertsMap.find( (*wki).iId );
    if (it != AlertsMap.end()) {      
      oAlert = (*it).second;       
      //Serial.println("FOUND ALERT:");             
      //dumpAlert(oAlert);
      
      short startTime = (*wki).iTime;
      short endTime = startTime + oAlert.iDuration;
      Serial.println("\tId:"+ String(oAlert.iId) + " Start: " + convertTime(startTime) + " End: " + convertTime(endTime));             
      if (iNowTime >= startTime && iNowTime < endTime) {
        iON++;      
      }
    }
    ++wki;     
  }

  if (iON > 0) {
    if (_bLEDState == false) {
      Serial.println("LED ON");             
      _bLEDState = true;
    }
  } else {
    if (_bLEDState == true) {
      Serial.println("LED OFF");             
      _bLEDState = false;
    }
  }

}

void inline handler (void){
  timer0_write(ESP.getCycleCount() + INTERVAL_US * 80); // 160 when running at 160mhz

  //Check Every Minute
  if (iCurrentTime % 60 == 0) {    
    checkAlerts();
  }   

  iCurrentTime++;
  
}

//Nunca execute nada na interrupcao, apenas setar flags!
void tCallback(void *tCall){
    _timeout = true;
}

void handleHardReset() {
  Serial.println("---->>>> HARDWARE RESET <<<------");

  EEPROMErase();
  
}

void usrInit(void){
    os_timer_setfn(&mTimer, tCallback, NULL);
    //The milliseconds parameter is the duration of the timer measured in milliseconds. The repeat parameter is whether or not the timer will restart once it has reached zero.
    os_timer_arm(&mTimer, 10000, true);    
}

void dumpClients() {
  Serial.print(" Clients:\r\n");
  struct station_info *stat_info = wifi_softap_get_station_info();
  IPAddress address;
  struct ip_addr *pIPaddress;
  while (stat_info != NULL) {
    pIPaddress = &stat_info->ip;
    address = pIPaddress->addr;
    Serial.print("\t");
    Serial.print(address);
    Serial.print("\r\n");
    stat_info = STAILQ_NEXT(stat_info, next);
  } 
}

bool enableWiFiSTA() {
  String hostname = WiFi.softAPmacAddress();
  hostname.replace(":","");
  hostname = "ESP" + hostname.substring(hostname.length()-4, hostname.length());
    
  Serial.println("Configuring WiFi Point: "  + ssidSTA + " Password: " + pwdSTA + " hostname: " + hostname);
    
  //start timer
  usrInit();
 
  WiFi.disconnect();
  
  WiFi.hostname(hostname);

  IPAddress emptyIp(0,0,0,0);
  if (ipSTA != emptyIp) {    
    WiFi.config(ipSTA, gatewaySTA, subnetSTA);
    Serial.print("Configuring WiFi IP: "); 
    Serial.print(ipSTA);
    Serial.print(" Gateway: "); 
    Serial.print(gatewaySTA);
    Serial.print(" Subnet: "); 
    Serial.println(subnetSTA);    
  }
    
  if (WiFi.status() != WL_CONNECTED) 
    WiFi.begin(ssidSTA.c_str(), pwdSTA.c_str());
  
  Serial.println("Connecting STA WiFi");
  int timeout = 20;
  while (WiFi.status() != WL_CONNECTED && !_timeout) {
    delay(500);
    Serial.print(".");
    //timeout--;
  }
  Serial.println();
  if (WiFi.status() == WL_CONNECTED) {
    //mySTA_IP = WiFi.localIP();
    Serial.print("Connected, IP address: ");
    Serial.println(WiFi.localIP());    
    bSTA_Running = true;
  } else {
    Serial.println("Error connecting to WiFi");
    bSTA_Running = false;
  }

  return bSTA_Running;
}

void enableWiFiAP() {
  //IPAddress NMask(255, 255, 255, 0);
    
  Serial.println("Configuring Access Point: "  + String(ssidAP) + " Password: " + String(pwdAP));
  
  //WiFi.softAPConfig(apIP, mySTA_IP, NMask);
  
  if (!WiFi.softAP(ssidAP, pwdAP)) {
    Serial.println("Problems to create AP");    
    bAP_Running = false;
  } else {
    bAP_Running = true;
    ipAP = WiFi.softAPIP();
    Serial.print("AP IP address: ");
    Serial.println(ipAP);  
    Serial.printf("MAC address = %s\n", WiFi.softAPmacAddress().c_str());    
  }  
}


void scanWiFi() {
  Serial.println("scan start");

  // WiFi.scanNetworks will return the number of networks found
  int n = WiFi.scanNetworks();
  char* ssidAux;
  
  Serial.println("scan done");
  if (n == 0)
    Serial.println("no networks found");
  else
  {
    Serial.print(n);
    Serial.println(" networks found");
    for (int i = 0; i < n; ++i)
    {
      // Print SSID and RSSI for each network found
      Serial.print(i + 1);
      Serial.print(": ");
      Serial.print(WiFi.SSID(i));
      Serial.print(": ");
      Serial.print(WiFi.BSSIDstr(i));
      Serial.printf(" Channel: %d ", WiFi.channel(i) );            
      Serial.print(" (");
      Serial.print(WiFi.RSSI(i));
      Serial.print(")");
      Serial.println((WiFi.encryptionType(i) == ENC_TYPE_NONE)?" ":"*");
      delay(10);
     
    }
  }
  Serial.println("");  
}

void setup() {
	delay(10);
	Serial.begin(115200);

  Serial.setDebugOutput(true);

  WiFi.mode(WIFI_AP_STA);

  //pinMode ( led, OUTPUT );
  //digitalWrite ( led, 0 );

  delay(100);

  EEPROMInit();
  
  EEPROMWriteNetwork(ssidSTA, pwdSTA);

  EEPROMLoadAlerts();

  pinMode(D6PIN, INPUT_PULLUP);
  attachInterrupt(digitalPinToInterrupt(D6PIN), handleHardReset, FALLING);

  
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
      Serial.println(message);
  });
  
  server.onNotFound ( handleNotFound );
	server.on("/", handleRoot);
  server.on("/scan", HTTP_GET, handleScan);
  server.on("/scan", HTTP_POST, handleConnect);
  server.on("/password", HTTP_POST, handleAPPassword);
  server.on("/time", HTTP_POST, handleSetTime);
  server.on("/time", HTTP_GET, handleGetTime);
  server.on("/alerts", HTTP_GET, handleDumpAlerts);
  server.on("/alerts", HTTP_POST, handleAlerts);
  server.on("/alerts", HTTP_DELETE, handleDeleteAlerts);

  //here the list of headers to be recorded
  const char * headerkeys[] = {"User-Agent","Cookie","plain"} ;
  size_t headerkeyssize = sizeof(headerkeys)/sizeof(char*);
  //ask server to track these headers
  server.collectHeaders(headerkeys, headerkeyssize );  
    
	server.begin();
	Serial.println("HTTP server started");

  noInterrupts();
  timer0_isr_init();
  timer0_attachInterrupt(handler);
  timer0_write(ESP.getCycleCount() + INTERVAL_US * 80); // 160 when running at 160mhz
  interrupts();

  WiFi.printDiag(Serial);
  
}

void loop() {
	server.handleClient();

  int iTemp = WiFi.softAPgetStationNum();
  if (iTemp != iConnectedAP) {
    Serial.printf("Stations connected = %d\n", iTemp);
    iConnectedAP = iTemp;
  }


  if (_timeout){
      Serial.println("TIME IS UP!");
      _timeout = false;
      if (WiFi.status() != WL_CONNECTED) { 
          Serial.println("WIFI STA DISCONNECTING!");
          WiFi.setAutoConnect(false);
          WiFi.disconnect();
          os_timer_disarm(&mTimer);
          //wifi_station_set_reconnect_policy(false); // if the ESP8266 station connected to the router, and then the connection broke, ESP8266 will not try to reconnect to the router.
          //wifi_station_set_auto_connect(false); //the ESP8266 station will not try to connect to the router automatically when power on until wifi_station_connect is called.
          //wifi_station_disconnect(); // ESP8266 station disconnects to the router, or ESP8266 station stops trying to connect to the target router.
      }
  }
  
  yield();
  
}
