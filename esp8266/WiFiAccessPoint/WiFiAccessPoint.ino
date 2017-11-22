/*
 * Copyright (c) 2015, Majenko Technologies
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
 * Get List of Connected Devices
 * http://www.esp8266.com/viewtopic.php?p=30091
 */
 
/* Create a WiFi access point and provide a web server on it. */

#include <ESP8266WiFi.h>
#include <WiFiClient.h> 
#include <ESP8266WebServer.h>
#include <ArduinoJson.h>
#include <map>
#include <list>

extern "C" { 
  #include<user_interface.h>
}


#define CHECK_BIT(var,pos) ((var) & (1<<(pos)))

//using namespace std;

/* Set these to your desired credentials. */
const char *ssidAP = "UnitecDemoServer";
const char *pwdAP = "unitec2017";

const char *ssidSTA = "UNITEC_VISITANTES";
const char *pwdSTA = "Bem-vindo!"; 

//const char *ssidSTA = "UNITEC_USUARIOS";
//const char *pwdSTA = "#4tva82015"; 

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
std::list<WeekAlert> WeekdaysList[8];

ESP8266WebServer server(80);
boolean bAP_Running = false;
boolean bSAT_Running = false;
/* You can remove the password parameter if you want the AP to be open. */
IPAddress mySTA_IP(192, 168, 1, 1); //default value


bool compFirst(const WeekAlert & a, const WeekAlert & b) {
  return a.iTime < b.iTime;
}

/* Just a little test message.  Go to http://192.168.1.1 in a web browser
 * connected to this access point to see it.
 */
void handleRoot() {
	server.send(200, "text/html", "<h1>You are connected</h1>");
}

void returnFail(String msg) {
  server.send(500, "text/plain", msg + "\r\n");
}


void handleDeleteAlerts() {

    String message = "";
    int i;

    for (i = 0; i < server.args(); i++) {
      message += "Arg num:" + String(i) + " -> ";
      message += server.argName(i) + ": ";
      message += server.arg(i) + "\n";      
    } 

    for (i = 0; i < server.headers(); i++) {
      message += "Header num:" + String(i) + " -> ";
      message += server.headerName(i) + ": ";
      message += server.header(i) + "\n";      
    } 

    Serial.println(message);
 
  if (server.hasArg("plain")== false && server.hasHeader("plain") == false) {
    //Check if body received
    server.send(500, "application/json", "{\"Status\":\"-1\", \"Message\":\"Missing Fields\"}");      
    return; 
  }

  String ReqJson = (server.hasArg("plain") ? server.arg("plain") : server.header("plain") );   
    
  
  message = "DELETE received:\n";
         message += ReqJson;
         message += "\n";
  
  Serial.println(message);
  
  DynamicJsonBuffer jsonBuffer;
  JsonObject& _root = jsonBuffer.parseObject(ReqJson);
  JsonArray& alerts = _root["alerts"];     
  std::list<WeekAlert>::iterator wki;
  Alerts oAlert;
  
  //alert == {"_duration":"00:15","_id":1,"_time":"06:30","Weekdays":[2,3,4,5,6]}
  for(JsonArray::iterator jsonIt=alerts.begin(); jsonIt!=alerts.end(); ++jsonIt) {
        
    JsonObject& alert = *jsonIt;    
    Serial.println("SEARCHING ID: " + String(alert["_id"].as<char*>()) );
    
    auto it = AlertsMap.find( alert["_id"].as<int>() );
    
    if (it != AlertsMap.end()) {
      oAlert = (*it).second;
      Serial.println("DELETING ID: " + String(oAlert.iId) );
    
      //search for Alert Id inside weekdaysList to remove it
      for (i = 0; i < 8; i++) {
        if (CHECK_BIT(oAlert.bWeekdays, i)) {
          //delete weekday
          wki = WeekdaysList[i].begin();
                              
          //iterate in all values inside the week
          Serial.print("DELETING WEEKDAYs: ");
          while(wki != WeekdaysList[i].end()) {
            if ((*wki).iId == oAlert.iId ) {
              //get out of the loop
              Serial.print(String(i) + " ");
              wki = WeekdaysList[i].erase(wki);
              break;
            }
            ++wki;
          }
          Serial.println(); 
        }            
      }

      //Now delete the alert from DB
      AlertsMap.erase( it );      
      server.send ( 200, "application/json", "{\"Status\":\"1\"}" );
      
    } else {
      Serial.println("NOT FOUND ID:" + String(alert["_id"].as<char*>()));
      server.send ( 404, "application/json", "{\"Status\":\"-1\", \"Message\":\"Not Found\"}" );
    }    
  }
}

/**
 * Main Function
 */
void handleAlerts() {
    String ReqJson = server.arg("plain");
    String message = "POST received:\n";
           message += ReqJson;
           message += "\n";

    Serial.println(message);

    DynamicJsonBuffer jsonBuffer;
    JsonObject& _root = jsonBuffer.parseObject(ReqJson);
    JsonArray& alerts = _root["alerts"];     
    Alerts oAlert;
    int i;    
    
    //alert == {"_duration":"00:15","_id":1,"_time":"06:30","Weekdays":[2,3,4,5,6]}
    for(JsonArray::iterator it=alerts.begin(); it!=alerts.end(); ++it) 
    {
        JsonObject& alert = *it;
        oAlert = parseAlertJson(alert);
        AlertsMap[ alert["_id"].as<int>() ] = oAlert;        
        
        //check if the weekday is set and add it to WeekdaysList
        for (i = 0; i < 8; i++) {
          if (CHECK_BIT(oAlert.bWeekdays, i)) {
            //Check if it exists and update if necessary, add it otherwise
            WeekdaysListInsert(WeekdaysList[i], WeekAlert(oAlert.iId, oAlert.iTime));
          } else {
            //Check if it exists and remove it if necessary
            WeekdaysListRemove(WeekdaysList[i], WeekAlert(oAlert.iId, oAlert.iTime));
          }
        }
    }

    dumpAlertsMap();
    dumpWeekdaysList();

    //Sort all weekdays time
    for (i = 0; i < 8; i++) {
      WeekdaysList[i].sort(compFirst);          
    }

    Serial.println("SORTED");
    
    dumpWeekdaysList();
   
    server.send ( 200, "application/json", "{\"Status\":\"1\"}" );
}

//Return all alerts at the database
void handleDumpAlerts() {
  dumpAlertsMap();
  dumpWeekdaysList();

  int i;
  DynamicJsonBuffer jsonBuffer;
  JsonObject& root = jsonBuffer.createObject();
  root["Status"] = 1;
  JsonArray& alerts = root.createNestedArray("alerts");

  //iterates all DB and create the JSON Array for each alert as JSON Object
  for (auto it = AlertsMap.begin(); it != AlertsMap.end(); it++) {
    Alerts oAlert = (*it).second;
    JsonObject& JsonAlert = jsonBuffer.createObject();
    JsonAlert["_id"] = oAlert.iId;
    JsonAlert["_time"] = oAlert.iTime;
    JsonAlert["_duration"] = oAlert.iDuration;
    JsonArray& JsonWeekdays = JsonAlert.createNestedArray("Weekdays");

    //Parse the binary represetation of weekdays into an Array[interger]
    for (i = 0; i < 8; i++) {
      if (CHECK_BIT(oAlert.bWeekdays, i)) {        
        JsonWeekdays.add(i);
      }
    }
    
    alerts.add(JsonAlert);
  }  

  String JSON;
  root.printTo(JSON);
  
  server.send ( 200, "application/json", JSON );
}

void handleNotFound() {
  //digitalWrite ( led, 1 );
  
  String message = "File Not Found\n\n";
  message += "URI: ";
  message += server.uri();
  message += "\nMethod: ";
  message += ( server.method() == HTTP_GET ) ? "GET" : "POST";
  message += "\nArguments: ";
  message += server.args();
  message += "\n";

  for ( uint8_t i = 0; i < server.args(); i++ ) {
    message += " " + server.argName ( i ) + ": " + server.arg ( i ) + "\n";
  }

  server.send ( 404, "text/plain", message );
  
  //digitalWrite ( led, 0 );
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

void enableWiFiSTA() {
  
  Serial.println("Configuring WiFi Point: "  + String(ssidSTA) + " Password: " + String(pwdSTA));
  WiFi.begin(ssidSTA, pwdSTA);
  
  Serial.print("Connecting");
  int timeout = 10;
  while (WiFi.status() != WL_CONNECTED && timeout > 0) {
    delay(500);
    Serial.print(".");
    timeout--;
  }
  Serial.println();
  if (timeout > 0) {
    mySTA_IP = WiFi.localIP();
    Serial.print("Connected, IP address: ");
    Serial.println(mySTA_IP);
    bSAT_Running = true;
  } else {
    Serial.println("Error connecting to WiFi");
    bSAT_Running = false;
  }  
}

void enableWiFiAP() {
  IPAddress NMask(255, 255, 255, 0);
  
  Serial.println("Configuring Access Point: "  + String(ssidAP) + " Password: " + String(pwdAP));
  
  WiFi.softAPConfig(mySTA_IP, mySTA_IP, NMask);
  
  if (!WiFi.softAP(ssidAP, pwdAP)) {
    Serial.println("Problems to create AP");    
    bAP_Running = false;
  } else {
    bAP_Running = true;
    IPAddress myIP = WiFi.softAPIP();
    Serial.print("AP IP address: ");
    Serial.println(myIP);  
  }  
}

void setup() {
	delay(1000);
	Serial.begin(115200);

  Serial.setDebugOutput(true);

  WiFi.mode(WIFI_AP_STA);

  //pinMode ( led, OUTPUT );
  //digitalWrite ( led, 0 );
  
  enableWiFiSTA();
 
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

  WiFi.printDiag(Serial);
  
}

void loop() {
	server.handleClient();
}
