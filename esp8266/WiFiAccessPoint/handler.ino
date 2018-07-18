
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
  
  Serial.println(message);
  
  DynamicJsonBuffer jsonBuffer;
  JsonObject& _root = jsonBuffer.parseObject(ReqJson);

  //in HH:MM -> translated to minutes
  int iTime = parseTime(_root.as<char*>());

  //Copying the weekdays int[]
  // Mon Tue Wed Thu Fri Sat Sun
  // 2   3   4   5   6   7   1        
  short iWeekday = _root["weekday"]; //weekday translate to seconds, Sunday 00:00 == 0sec

  //Error Treatment
  if (iTime < 0) {
    Serial.println("SET TIME FORMAT ERROR");
    server.send ( 404, "application/json", "{\"Status\":\"-1\", \"Message\":\"Time Format Error: HH:MM\"}" );    
    return;
  } else if (iWeekday < 0 || iWeekday > 7) {
    Serial.println("SET TIME WEEKDAY ERROR");
    server.send ( 404, "application/json", "{\"Status\":\"-1\", \"Message\":\"Weekday Error: Sun-1, Mon-2, Tue-3, Wed-4, Thu-5, Fri-6, Sat-7\"}" );    
    return;
  }

  iCurrentTime = (iWeekday-1)*24*60*60 + iTime*60;
  message = "Current Time\n";
  message += iCurrentTime;
  message += "\n";
  Serial.println(message);

  server.send ( 200, "application/json", "{\"Status\":\"1\"}" );
      
}

void handleGetTime() {
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
  
  server.send ( 200, "application/json", JSON );
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

    Serial.println("SORTED");
    
    dumpWeekdaysList();

    EEPROMCommit();
   
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

//Scan WiFi Networks
void handleScan() {
  Serial.println("WiFi Scan start");

  // WiFi.scanNetworks will return the number of networks found
  int n = WiFi.scanNetworks();
  
  DynamicJsonBuffer jsonBuffer;
  JsonObject& wifiJSON = jsonBuffer.createObject();
  //wifiJSON = jsonBuffer.createObject();
  JsonArray& networks = wifiJSON.createNestedArray("networks");

  Serial.println("WiFi Scan done");
  if (n == 0) {
    wifiJSON["Status"] = 0;
    Serial.println("No networks found");
  } else {
    wifiJSON["Status"] = 1;
    Serial.printf("%d networks found\n", n);
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
  Serial.println("WiFi Connection start");

  String ReqJson = server.arg("plain");
  String message = "POST received:\n";
         message += ReqJson;
         message += "\n";

  Serial.println(message);
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
