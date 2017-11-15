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
 */

/* Create a WiFi access point and provide a web server on it. */

#include <ESP8266WiFi.h>
#include <WiFiClient.h> 
#include <ESP8266WebServer.h>
#include <ArduinoJson.h>
#include <map>
#include <list>

#define CHECK_BIT(var,pos) ((var) & (1<<(pos)))

//using namespace std;

/* Set these to your desired credentials. */
const char *ssid = "UnitecDemoServer";
const char *password = "unitec2017";
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

bool compFirst(const WeekAlert & a, const WeekAlert & b) {
  return a.iTime < b.iTime;
}

/* Just a little test message.  Go to http://192.168.1.1 in a web browser
 * connected to this access point to see it.
 */
void handleRoot() {
	server.send(200, "text/html", "<h1>You are connected</h1>");
}

void handleDeleteAlerts() {
  String ReqJson = server.arg("plain");
  String message = "DELETE received:\n";
         message += ReqJson;
         message += "\n";
  
  Serial.println(message);
  
  DynamicJsonBuffer jsonBuffer;
  JsonObject& _root = jsonBuffer.parseObject(ReqJson);
  JsonArray& alerts = _root["alerts"];     
  int i;
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
      server.send ( 200, "application/json", "{\"Status\":\"Ok\"}" );
      
    } else {
      Serial.print("NOT FOUND ID:" + String(alert["_id"].as<char*>()));
      server.send ( 404, "application/json", "{\"Status\":\"Not Found\"}" );
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
   
    server.send ( 200, "application/json", "{\"Status\":\"Ok\"}" );
}

void handleDumpAlerts() {
  dumpAlertsMap();
  dumpWeekdaysList();

  int i;
  DynamicJsonBuffer jsonBuffer;
  JsonObject& root = jsonBuffer.createObject();
  //root["sensor"] = "gps";
  JsonArray& alerts = root.createNestedArray("alerts");
  //alerts.add(48.756080);
  
  for (auto it = AlertsMap.begin(); it != AlertsMap.end(); it++) {
    Alerts oAlert = (*it).second;
    JsonObject& JsonAlert = jsonBuffer.createObject();
    JsonAlert["_id"] = oAlert.iId;
    JsonAlert["_time"] = oAlert.iTime;
    JsonAlert["_duration"] = oAlert.iDuration;
    JsonArray& JsonWeekdays = JsonAlert.createNestedArray("Weekdays");

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

void setup() {
	delay(1000);
	Serial.begin(115200);

  //pinMode ( led, OUTPUT );
  //digitalWrite ( led, 0 );
 
	Serial.println();
	Serial.print("Configuring access point:"  + String(ssid));
	/* You can remove the password parameter if you want the AP to be open. */
  IPAddress Ip(192, 168, 1, 1);
  IPAddress NMask(255, 255, 255, 0);
  
  WiFi.softAPConfig(Ip, Ip, NMask);
  
	if (!WiFi.softAP(ssid, password)) {
    Serial.println("Problems to create AP");
    return;
	}

	IPAddress myIP = WiFi.softAPIP();
	Serial.print("AP IP address: ");
	Serial.println(myIP);
    
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
	server.begin();
	Serial.println("HTTP server started");
}

void loop() {
	server.handleClient();
}
