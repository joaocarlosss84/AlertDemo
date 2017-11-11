
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


String convertTime(int iTime) {
  int iHour, iMin, i;
  iHour = iTime/60;
  iMin = iTime%60;
  return String(iHour) + ":" + String(iMin);
}

void dumpAlertsMap() {
  //std::map<int, Alerts>::iterator i
  int iHour, iMin, i;
  
  for (auto it = AlertsMap.begin(); it != AlertsMap.end(); it++) {
    Alerts alert = (*it).second;
    
    Serial.println("ID: " + String( (*it).first ));    
    //iHour = alert.iTime/60;
    //iMin = alert.iTime%60;
    Serial.println("TIME: " + String( alert.iTime ) + " = " + convertTime(alert.iTime));

    //iHour = alert.iDuration/60;
    //iMin = alert.iDuration%60;
        
    //Serial.println("DURATION: " + String( alert.iDuration ) + " = " + String(iHour) +":"+String(iMin));
    Serial.println("DURATION: " + String( alert.iDuration ) + " = " + convertTime(alert.iDuration));
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

void dumpWeekdaysList() {
  int i, iTime, iId;
  std::list<WeekAlert> oList;

  Serial.println("Weekdays List");
  for (i=0; i<8; i++){
    oList = WeekdaysList[i];
    Serial.print(String(i) + ": ");
    for (auto it = oList.begin(); it != oList.end(); it++) {
      iId = (*it).iId;
      iTime = (AlertsMap[ iId ]).iTime;
      Serial.print(String( iId ) + "-" + convertTime(iTime) + " ");
    }
    Serial.println();
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
  
  oAlert = (Alerts) {.iId = id, .iTime = iTime, .iDuration = iDuration, .bWeekdays = bWeekdays};
  //oAlert = Alerts(iTime, iDuration, bWeekdays);
  
  return oAlert;
}

void WeekdaysListInsert(std::list<WeekAlert> &oList, WeekAlert oWeekAlert) {
  std::list<WeekAlert>::iterator wki;

  wki = oList.begin();
  while (wki != oList.end()) {
    if ( (*wki).iId == oWeekAlert.iId) {
      //update iTime and get out of here
      (*wki).iTime = oWeekAlert.iTime;
      return;
    }
    ++wki;
  }
  //not found, add new item
  oList.push_back(oWeekAlert);
}

