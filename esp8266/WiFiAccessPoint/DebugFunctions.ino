
void dumpJsonObject(JsonObject& obj) {
  for(JsonObject::iterator at=obj.begin(); at!=obj.end(); ++at) 
  {
    // *at contains the key/value pair
    const char* key = at->key;

    // at->value contains the JsonVariant which can be casted as usual
    const char* value = at->value;

    sprintln( String(key) + " - " + String(value) );            
  }        
}


String convertTime(int iTime) {
  char cTime[6];  //buffer used to format a line (+1 is for trailing 0)
  sprintf(cTime,"%02d:%02d",iTime/60, iTime%60);   
  return String(cTime);
}

void dumpAlertsMap() {
  //std::map<int, Alerts>::iterator i
  
  for (auto it = AlertsMap.begin(); it != AlertsMap.end(); it++) {
    Alerts oAlert = (*it).second;

    dumpAlert(oAlert);
/*    
    sprintln("ID: " + String( (*it).first ));    
    //iHour = alert.iTime/60;
    //iMin = alert.iTime%60;
    sprintln("TIME: " + String( alert.iTime ) + " = " + convertTime(alert.iTime));

    //iHour = alert.iDuration/60;
    //iMin = alert.iDuration%60;
        
    //sprintln("DURATION: " + String( alert.iDuration ) + " = " + String(iHour) +":"+String(iMin));
    sprintln("DURATION: " + String( alert.iDuration ) + " = " + convertTime(alert.iDuration));
    sprint("WEEKDAYS: ");
    sprint(alert.bWeekdays, HEX);
    sprint(" = ");
    sprint(alert.bWeekdays, BIN);
    sprintln();
    sprintln();
*/
  }
}

void dumpAlert(Alerts oAlert) {
#if DEBUG
    sprintln("ID: " + String( oAlert.iId ));    
    sprintln("TIME: " + String( oAlert.iTime ) + " = " + convertTime(oAlert.iTime));
    sprintln("DURATION: " + String( oAlert.iDuration ) + " = " + convertTime(oAlert.iDuration));
    sprint("WEEKDAYS: ");
    Serial.print(oAlert.bWeekdays, HEX);
    sprint(" = ");
    Serial.print(oAlert.bWeekdays, BIN);
    sprintln();  
#endif    
}

void dumpWeekdaysList() {
  int i, iTime, iId;
  std::list<WeekAlert> oList;

  sprintln("Weekdays List");
  for (i=0; i<8; i++){
    oList = WeekdaysList[i];
    sprint(String(i) + ": ");
    for (auto it = oList.begin(); it != oList.end(); it++) {
      iId = (*it).iId;
      iTime = (AlertsMap[ iId ]).iTime;
      sprint(String( iId ) + "-" + convertTime(iTime) + " ");
    }
    sprintln();
  }
}

void parseBytes(const char* str, char sep, byte* bytes, int maxBytes, int base) {
    for (int i = 0; i < maxBytes; i++) {
        bytes[i] = strtoul(str, NULL, base);  // Convert byte
        str = strchr(str, sep);               // Find next separator
        if (str == NULL || *str == '\0') {
            break;                            // No more separators, exit
        }
        str++;                                // Point to next character after separator
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
  iHour = sTime.substring(0,sTime.indexOf(":")).toInt();
  iMin = sTime.substring(sTime.indexOf(":")+1).toInt();
  iTime = iHour*60 + iMin;        
  
  sTime = alert["_duration"].as<char*>();
  iHour = sTime.substring(0,sTime.indexOf(":")).toInt();
  iMin = sTime.substring(sTime.indexOf(":")+1).toInt();
  iDuration = iHour*60 + iMin;        
          
  //Copying the weekdays int[]
  // Mon Tue Wed Thu Fri Sat Sun
  // 2   3   4   5   6   7   1        
  weekdaysCount = alert["Weekdays"].as<JsonArray>().copyTo(aWeekdays);
  //sprintln("\tWeekdays: " + String(weekdaysCount));
  for (i = 0; i < weekdaysCount; i++) {
    //sprintln("\t ["+ String(i) +"] = " + String(aWeekdays[i]));
    bWeekdays |= 1 << aWeekdays[i];
  }

  //alert.prettyPrintTo(Serial);
  
  oAlert = (Alerts) {.iId = id, .iTime = iTime, .iDuration = iDuration, .bWeekdays = bWeekdays};
  //oAlert = Alerts(iTime, iDuration, bWeekdays);
  
  return oAlert;
}

int parseTime(String sTime) {
  //Parsing string time HH:MM to integer
  if (sTime.length() < 3 || sTime.length() > 5)
    return -1;
    
  //String sTime = alert["_time"].as<char*>();
  short iHour = sTime.substring(0,sTime.indexOf(":")).toInt();
  short iMin = sTime.substring(sTime.indexOf(":")+1).toInt();
  short iTime = iHour*60 + iMin;        
  return iTime;
}

Alerts parseTimestamp(int iTimestamp) {
  //Parsing Timestamp in seconds to Alerts Object
  Alerts oAlert;
  short iWeekday = (iTimestamp/(24*60*60))+1;  
  int iTime = (iCurrentTime - (iWeekday-1)*24*60*60)/60; //HH:MM in minutes
  byte bWeekdays = 1 << iWeekday;
  oAlert = (Alerts) {.iId = 0, .iTime = iTime, .iDuration = 0, .bWeekdays = bWeekdays};
  return oAlert;
}

short getWeekday(int iTimestamp) {
  return (iTimestamp/(24*60*60))+1;
}

short getTimeInMinutes(int iTimestamp) {
  short weekday = getWeekday(iTimestamp);
  return (iCurrentTime - (weekday-1)*24*60*60)/60;
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

void WeekdaysListRemove(std::list<WeekAlert> &oList, WeekAlert oWeekAlert) {
  std::list<WeekAlert>::iterator wki;

  wki = oList.begin();
  while (wki != oList.end()) {
    if ( (*wki).iId == oWeekAlert.iId) {
      //remove it and get out of here
      wki = oList.erase(wki);      
      return;
    }
    ++wki;
  }  
}


