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

std::map<int, Alerts> AlertsMap;
//std::vector<int> 

ESP8266WebServer server(80);

/* Just a little test message.  Go to http://192.168.1.1 in a web browser
 * connected to this access point to see it.
 */
void handleRoot() {
	server.send(200, "text/html", "<h1>You are connected</h1>");
}

/*
 * Handle HTTP Request and defines its header
void handleJson(WiFiClient& client, JsonObject& json) {
  client.println("HTTP/1.1 200 OK");
  client.println("Content-Type: application/json");
  client.println("Access-Control-Allow-Origin: *");
  client.println("Connection: close");
  client.println();

  json.prettyPrintTo(client);
}
*/

void dumpJsonObject(JsonObject& obj) {
  for(JsonObject::iterator at=obj.begin(); at!=obj.end(); ++at) 
  {
    // *at contains the key/value pair
    const char* key = at->key;

    // at->value contains the JsonVariant which can be casted as usual
    const char* value = at->value;

    Serial.println( String(key) + " - " + String(value) );            
  }        
}

void dumpAlertsMap() {
  //std::map<int, Alerts>::iterator i
  int iHour, iMin;
  
  for (auto it = AlertsMap.begin(); it != AlertsMap.end(); it++) {
    Alerts alert = (*it).second;
    
    Serial.println("ID: " + String( (*it).first ));    
    iHour = alert.iTime/60;
    iMin = alert.iTime%60;
    Serial.println("TIME: " + String( alert.iTime ) + " = " + String(iHour) +":"+String(iMin));

    iHour = alert.iDuration/60;
    iMin = alert.iDuration%60;
    Serial.println("DURATION: " + String( alert.iDuration ) + " = " + String(iHour) +":"+String(iMin));
    Serial.print("WEEKDAYS: ");
    Serial.print(alert.bWeekdays, HEX);
    Serial.print(" = ");
    Serial.print(alert.bWeekdays, BIN);
    Serial.println();
    Serial.println();
    
    //Serial << *i << endl;
    //Serial << "Took: " << (int) (finish - start) << " milliseconds." << endl;
  }
}

Alerts parseAlertJson(JsonObject& alert) {
  int iTime = 0;
  int iDuration = 0;
  int aWeekdays[7];
  byte bWeekdays = 0;
  int iHour = 0;
  int iMin = 0;
  Alerts oAlert;
  String sTime = "";
  int i = 0;
  int weekdaysCount = 0;
  int id = alert["_id"];
  
  //Parsing string time HH:MM to integer
  sTime = alert["_time"].as<char*>();
  iHour = sTime.substring(0,2).toInt();
  iMin = sTime.substring(3).toInt();
  iTime = iHour*60 + iMin;        
  
  sTime = alert["_duration"].as<char*>();
  iHour = sTime.substring(0,2).toInt();
  iMin = sTime.substring(3).toInt();
  iDuration = iHour*60 + iMin;        
          
  //Copying the weekdays int[]
  // Mon Tue Wed Thu Fri Sat Sun
  // 2   3   4   5   6   7   1        
  weekdaysCount = alert["Weekdays"].as<JsonArray>().copyTo(aWeekdays);
  //Serial.println("\tWeekdays: " + String(weekdaysCount));
  for (i = 0; i < weekdaysCount; i++) {
    //Serial.println("\t ["+ String(i) +"] = " + String(aWeekdays[i]));
    bWeekdays |= 1 << aWeekdays[i];
  }

  //alert.prettyPrintTo(Serial);

  /*
  Serial.println("id =" + String(id));  
  alert["_duration"].prettyPrintTo(Serial);
  Serial.println(" Duration = " + String(iDuration));  
  alert["_time"].prettyPrintTo(Serial);
  Serial.println(" Time = " + String(iTime));  
  Serial.println(bWeekdays, HEX);
  Serial.println();
  */
  
  oAlert = (Alerts) {.iId = id, .iTime = iTime, .iDuration = iDuration, .bWeekdays = bWeekdays};
  //oAlert = Alerts(iTime, iDuration, bWeekdays);
  
  return oAlert;
}

void handleAlerts() {
    String ReqJson = server.arg("plain");
    String message = "Body received:\n";
           message += ReqJson;
           message += "\n";

    Serial.println(message);

    DynamicJsonBuffer jsonBuffer;
    JsonObject& _root = jsonBuffer.parseObject(ReqJson);
    JsonArray& alerts = _root["alerts"];     
    Alerts oAlert;
    
    //alert == {"_duration":"00:15","_id":1,"_time":"06:30","Weekdays":[2,3,4,5,6]}
    for(JsonArray::iterator it=alerts.begin(); it!=alerts.end(); ++it) 
    {
        JsonObject& alert = *it;
        oAlert = parseAlertJson(alert);
        AlertsMap[ alert["_id"] ] = oAlert;        
    }

    dumpAlertsMap();
 
    //server.send(200, "text/plain", message);
    server.send ( 200, "application/json", "{\"Status\":\"Ok\"}" );
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
  server.on("/alerts", handleAlerts);
	server.begin();
	Serial.println("HTTP server started");
}

void loop() {
	server.handleClient();
}
