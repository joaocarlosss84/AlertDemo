#include "DebugFunctions.h"

// Using ArduinoJson 5.11
#include <ArduinoJson.h>

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

void handleAPPassword() {
  
}

void handleSetTime() {
  String ReqJson = server.arg("plain");
  String message = "POST received:\n";
         message += ReqJson;
         message += "\n";
  
  DEBUG_PRINTLN(message);
  
  DynamicJsonBuffer jsonBuffer;
  JsonObject& _root = jsonBuffer.parseObject(ReqJson);
  String sTime = _root["_time"].as<char*>();
  //in HH:MM -> translated to minutes
  int iTime = parseTime( sTime );

  //Copying the weekdays int[]
  // Mon Tue Wed Thu Fri Sat Sun
  // 2   3   4   5   6   7   1        
  short iWeekday = _root["weekday"]; //weekday translate to seconds, Sunday 00:00 == 0sec
  int iCurrentTimeRecv = _root["timestamp"];
    
  //Error Treatment
  if (iTime < 0) {
    DEBUG_PRINTLN("SET TIME FORMAT ERROR");
    server.send ( 404, "application/json", "{\"Status\":\"-1\", \"Message\":\"Time Format Error: HH:MM\"}" );    
    return;
  } else if (iWeekday < 0 || iWeekday > 7) {
    DEBUG_PRINTLN("SET TIME WEEKDAY ERROR");
    server.send ( 404, "application/json", "{\"Status\":\"-1\", \"Message\":\"Weekday Error: Sun-1, Mon-2, Tue-3, Wed-4, Thu-5, Fri-6, Sat-7\"}" );    
    return;
  }

  if (iCurrentTimeRecv) {
    iCurrentTime = iCurrentTimeRecv;
  } else {
    iCurrentTime = (iWeekday-1)*24*60*60 + iTime*60;
  }  
  message = "Current Time\n";
  message += iCurrentTime;
  message += "\n";
  DEBUG_PRINTLN(message);

  //rtc.setDOW(TUESDAY);      //Define o dia da semana
  //rtc.setTime(13, 19, 0);     //Define o horario
  //rtc.setDate(7, 8, 2018);   //Define o dia, mes e ano
  rtc.setDOWAndroid(iWeekday);
  
  iTime = (iCurrentTime - ((iWeekday-1)*24*60*60));//time in seconds
  short iHour = iTime/3600; 
  short iMinutes = (iTime%3600) * 60;
  short iSeconds = iTime % 60;
  rtc.setTime(iHour, iMinutes, iSeconds);     //Define o horario

  server.send ( 200, "application/json", "{\"Status\":\"1\"}" );
      
}

void handleGetTime() {
  /*
  DynamicJsonBuffer jsonBuffer;
  JsonObject& root = jsonBuffer.createObject();
  root["Status"] = 1;
  root["timestamp"] = iCurrentTime;
  short weekday = (iCurrentTime/(24*60*60))+1;
  root["weekday"] = weekday;
  int iTime = (iCurrentTime - (weekday-1)*24*60*60)/60;
  root["time"] = convertTime(iTime); //in minutes
  
  String JSON;
  root.printTo(JSON);
  */  
  server.send ( 200, "application/json", getTime() );
}

void getTime(JsonObject& root) {
  root["Status"] = 1;
  root["timestamp"] = iCurrentTime;
  short weekday = (iCurrentTime/(24*60*60))+1;
  root["weekday"] = weekday;
  int iTime = (iCurrentTime - (weekday-1)*24*60*60)/60;
  root["time"] = convertTime(iTime); //in minutes  
}

String getTime() {  
  
  DynamicJsonBuffer jsonBuffer;
  JsonObject& root = jsonBuffer.createObject();
  getTime(root);
  
  String JSON;
  root.printTo(JSON);
  
  return JSON;
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

    DEBUG_PRINTLN(message);
 
  if (server.hasArg("plain")== false && server.hasHeader("plain") == false) {
    //Check if body received
    server.send(500, "application/json", "{\"Status\":\"-1\", \"Message\":\"Missing Fields\"}");      
    return; 
  }

  String ReqJson = (server.hasArg("plain") ? server.arg("plain") : server.header("plain") );   
    
  
  message = "DELETE received:\n";
         message += ReqJson;
         message += "\n";
  
  DEBUG_PRINTLN(message);
  
  DynamicJsonBuffer jsonBuffer;
  JsonObject& _root = jsonBuffer.parseObject(ReqJson);
  JsonArray& alerts = _root["alerts"];     
  std::list<WeekAlert>::iterator wki;
  Alerts oAlert;
  
  //alert == {"_duration":"00:15","_id":1,"_time":"06:30","Weekdays":[2,3,4,5,6]}
  for(JsonArray::iterator jsonIt=alerts.begin(); jsonIt!=alerts.end(); ++jsonIt) {
        
    JsonObject& alert = *jsonIt;    
    DEBUG_PRINTLN("SEARCHING ID: " + String(alert["_id"].as<char*>()) );
    
    auto it = AlertsMap.find( alert["_id"].as<int>() );
    
    if (it != AlertsMap.end()) {
      oAlert = (*it).second;
      DEBUG_PRINTLN("DELETING ID: " + String(oAlert.iId) );
    
      //search for Alert Id inside weekdaysList to remove it
      for (i = 0; i < 8; i++) {
        if (CHECK_BIT(oAlert.bWeekdays, i)) {
          //delete weekday
          wki = WeekdaysList[i].begin();
                              
          //iterate in all values inside the week
          DEBUG_PRINT("DELETING WEEKDAYs: ");
          while(wki != WeekdaysList[i].end()) {
            if ((*wki).iId == oAlert.iId ) {
              //get out of the loop
              DEBUG_PRINT(String(i) + " ");
              wki = WeekdaysList[i].erase(wki);
              break;
            }
            ++wki;
          }
          DEBUG_PRINTLN(); 
        }            
      }

      //Now delete the alert from DB
      AlertsMap.erase( it );      
      server.send ( 200, "application/json", "{\"Status\":\"1\"}" );
      
    } else {
      DEBUG_PRINTLN("NOT FOUND ID:" + String(alert["_id"].as<char*>()));
      server.send ( 404, "application/json", "{\"Status\":\"-1\", \"Message\":\"Not Found\"}" );
    }    
  }
}

/**
 * Main Function
 */
void handleUpdateAlerts() {
    String ReqJson = server.arg("plain");
    String message = "POST received:\n";
           message += ReqJson;
           message += "\n";

    DEBUG_PRINTLN(message);

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

        EEPROMWriteNextAlert(&oAlert);
        
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

    DEBUG_PRINTLN("SORTED");
    
    dumpWeekdaysList();

    EEPROMCommit();
   
    server.send ( 200, "application/json", "{\"Status\":\"1\"}" );
}

//Return all alerts at the database
void handleGetAlerts() {
  dumpAlertsMap();
  dumpWeekdaysList();

  int i;
  DynamicJsonBuffer jsonBuffer;
  JsonObject& root = jsonBuffer.createObject();
  root["Status"] = 1;
  JsonArray& alerts = root.createNestedArray("alerts");

  getTime(root);

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

//Scan WiFi Networks
void handleScan() {
  DEBUG_PRINTLN("WiFi Scan start");

  // WiFi.scanNetworks will return the number of networks found
  int n = WiFi.scanNetworks();
  
  DynamicJsonBuffer jsonBuffer;
  JsonObject& wifiJSON = jsonBuffer.createObject();
  //wifiJSON = jsonBuffer.createObject();
  JsonArray& networks = wifiJSON.createNestedArray("networks");

  DEBUG_PRINTLN("WiFi Scan done");
  if (n == 0) {
    wifiJSON["Status"] = 0;
    DEBUG_PRINTLN("No networks found");
  } else {
    wifiJSON["Status"] = 1;
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
      DEBUG_PRINT(" Channel: ")
      DEBUG_PRINT(WiFi.channel(i));            
      DEBUG_PRINT(" (");
      DEBUG_PRINT(WiFi.RSSI(i));
      DEBUG_PRINT(")");
      DEBUG_PRINTLN((WiFi.encryptionType(i) == ENC_TYPE_NONE)?" ":"*");
      delay(10);
      
      JsonObject& JsonNetwork = jsonBuffer.createObject();
      JsonNetwork["ssid"] = WiFi.SSID(i);
      JsonNetwork["bssid"] = WiFi.BSSIDstr(i);
      JsonNetwork["rssi"] = WiFi.RSSI(i);
      JsonNetwork["channel"] = WiFi.channel(i);
      JsonNetwork["enc"] = WiFi.encryptionType(i);
      
      networks.add(JsonNetwork);     
    }
  }

  String JSON;
  wifiJSON.printTo(JSON);
  
  server.send ( 200, "application/json", JSON );
}

//Connect to a previous Wifi
void handleConnect() {
  DEBUG_PRINTLN("WiFi Connection start");

  String ReqJson = server.arg("plain");
  String message = "POST received:\n";
         message += ReqJson;
         message += "\n";

  DEBUG_PRINTLN(message);
  //Parse JSON Packet
  DynamicJsonBuffer jsonBuffer;
  JsonObject& _root = jsonBuffer.parseObject(ReqJson);
  ssidSTA = _root["ssid"].as<char*>();
  pwdSTA = _root["password"].as<char*>();
  if (_root.containsKey("ip")) {
    byte ip[4];
    parseBytes(_root["ip"], '.', ip, 4, 10);
    ipSTA = IPAddress(ip);

    if(_root.containsKey("gateway")) {
      parseBytes(_root["gateway"], '.', ip, 4, 10);
      gatewaySTA = IPAddress(ip);  
    } else {
      //bad parameter, if it sets the Station IP, it must set the gateway
      returnFail("Missing gateway parameter");
      return;
    }    
  }
  
  //Create JSON Reply
  JsonObject& wifiJSON = jsonBuffer.createObject();
  
  if(enableWiFiSTA()) {
    wifiJSON["Status"] = 1;
    wifiJSON["ip"] = WiFi.localIP().toString();
  } else {
    wifiJSON["Status"] = 0;
  }

  String JSON;
  wifiJSON.printTo(JSON);
  
  server.send ( 200, "application/json", JSON );  
}
