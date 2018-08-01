/*
 * EEPROM Organization
 * 
 * Each EEPROM position holds 8 bits only at ESP8266, so some treatment needs to be done
 * ESP8266 has a total of 4096 bytes at NodeMCU v3
 * 
 * First positions will store WiFi Networks and Passwords
 * Each Networks has a maximum of 32 characters and each password has 32 chars too. 
 * 
 * Alerts will have 
 * id = byte - 8 bits
 * time = int - 11 bits
 * duration = int - 11 bits
 * weekdays = byte - 8 bits
 * Total: 38 bits 
 * 
 * 1 Alert occupies 5 bytes = 40 bits
 * 64 bytes for Network     = 64 bytes
 * 128 alerts occpy 128*5bytes = 640 bytes
 * 
 */


#define NETWORK_SIZE 2*(32+1)
#define MAX_ALERTS 64 //1 byte //must reduce the amount of ALERTS due to ESP01
#define ALERT_SIZE 5 // 5 bytes

#define ALERTS_LENGTH_POS NETWORK_SIZE
#define ALERTS_INIT  ALERTS_LENGTH_POS+1

#define EEPROM_SIZE (MAX_ALERTS*ALERT_SIZE + 1 + NETWORK_SIZE)

Alerts wAlert, rAlert; //for testing purposes
int wAddr, rAddr;

void dumpAlert(Alerts *oAlert) {
  if (oAlert != 0) {
    sprintln("ID: " + String( oAlert->iId ));    
    sprintln("TIME: " + String( oAlert->iTime ) + " = " + convertTime(oAlert->iTime));
    sprintln("DURATION: " + String( oAlert->iDuration ) + " = " + convertTime(oAlert->iDuration));
    Serial.print("WEEKDAYS: ");
    Serial.print(oAlert->bWeekdays, HEX);
    sprintln();
  }
}

void EEPROMLoadAlerts() {
  Alerts oAlert;

  rAddr = ALERTS_INIT;

  AlertsMap.clear();

  wAddr = EEPROM.read(ALERTS_LENGTH_POS); 

  sprintln("--> EEPROM : Saved Alerts = " + String(wAddr) );

  wAddr = ALERTS_INIT + wAddr*ALERT_SIZE;

  sprintln("rAddr: " + String(rAddr));
  sprintln("wAddr: " + String(wAddr));
  sprintln("EEPROM_SIZE: " + String(EEPROM_SIZE));
  
  while (rAddr < EEPROM_SIZE && rAddr < wAddr) {

    EEPROMReadNextAlert(&oAlert);
    
    //it may have found garbage and not an alert
    if (oAlert.iId >= 0) {
      AlertsMap[ oAlert.iId ] = oAlert;    
  
      sprintln("--> EEPROM : LOADED Alert " + String(oAlert.iId) );
  
      //check if the weekday is set and add it to WeekdaysList
      for (int i = 0; i < 8; i++) {
        if (CHECK_BIT(oAlert.bWeekdays, i)) {
          //Check if it exists and update if necessary, add it otherwise
          WeekdaysListInsert(WeekdaysList[i], WeekAlert(oAlert.iId, oAlert.iTime));
        } else {
          //Check if it exists and remove it if necessary
          WeekdaysListRemove(WeekdaysList[i], WeekAlert(oAlert.iId, oAlert.iTime));
        }
      }
    }
    
  }
  
}

void EEPROMInit() {
  EEPROM.begin(EEPROM_SIZE);

  //ID TIME DURA W   _SMTWTFS  
  //00 0000 0000 0
  //00 0000 00000

  //wAlert = (Alerts) {.iId = 0, .iTime = 360, .iDuration = 10, .bWeekdays = 0x55};

  //sprintln("DUMP Write Alert");
  //dumpAlert(&wAlert);
  
  wAddr = ALERTS_INIT;
  rAddr = ALERTS_INIT;
  
  //EEPROMWriteAlert(wAddr, &wAlert);
  //EEPROM.commit();
  //delay(100);
    
  //sprintln("DUMP Read Alert " + String(wAddr));
  //EEPROMReadAlert(wAddr, &rAlert);
  //dumpAlert(&rAlert);
}

void EEPROMErase() {
  wAddr = 0;
  while (wAddr < EEPROM_SIZE) {
     EEPROM.write(wAddr, 0);
     wAddr++;
  }
  wAddr = ALERTS_INIT;
  EEPROM.commit();
  EEPROM.end();
}

void EEPROMWriteNextAlert(Alerts *oAlert) {
  if (wAddr < EEPROM_SIZE) {
      sprintln(">>> EEPROMWriteNextAlert[" + String(wAddr) + "] = " + String(oAlert->iId) );
      EEPROMWriteAlert(wAddr, oAlert);
      wAddr += ALERT_SIZE;      
  }
}

void EEPROMReadNextAlert(Alerts *oAlert) {
  if (rAddr < EEPROM_SIZE) {      
      EEPROMReadAlert(rAddr, oAlert);
      if (oAlert->iId >= 0) {
        sprintln(">>> EEPROMReadNextAlert[" + String(rAddr) + "] = " + String(oAlert->iId) );
      }
      rAddr += ALERT_SIZE;      
  }
}


void EEPROMCommit() {
  //Store Alerts Size
  auto it = AlertsMap.begin();
  int AlertsSize = 0;
  while (it != AlertsMap.end()) {
    it++;    
    AlertsSize++;
  }

  sprintln(">>> EEPROMCommit Alerts = " + String(AlertsSize) );
  
  EEPROM.write(ALERTS_LENGTH_POS, AlertsSize );  
  EEPROM.commit();
  EEPROM.end();
}

bool EEPROMWriteNetwork(String network, String password) {
  if (network.length() > 32 || password.length() > 32) {    
    sprintln("Maximum Network/Password size exceeded");
    return false;  
  } else {
    int address = 0;
    char aNet[33], aPwd[33];
    
    memcpy(&aNet, network.c_str(), 32);    
    aNet[32] = '\0';

    memcpy(&aPwd, password.c_str(), 32);    
    aPwd[32] = '\0';
    
    sprintln("Network: '" + network + "' == '" + aNet + "'");
    sprintln("Password: '" + password + "' == '" + aPwd + "'");    
        
    //EEPROM.write(address, four);
    return true;
  } 
}

void EEPROMWriteAlert(int address,  Alerts *oAlert)
{
  //Decomposition from a long to 4 bytes by using bitshift.
  //Zero = Most significant -> Four = Least significant byte
  long value = ((oAlert->iTime & 0x7FF) << (11+8)) + ((oAlert->iDuration & 0x7FF) << 8) + (oAlert->bWeekdays & 0xFF);
   
  byte four  = (value & 0xFF);
  byte three = ((value >> 8) & 0xFF);
  byte two   = ((value >> 16) & 0xFF);
  byte one   = ((value >> 24) & 0xFF);
  byte zero  = oAlert->iId;
  
  //Write the 4 bytes into the eeprom memory.
  EEPROM.write(address, four);
  EEPROM.write(address + 1, three);
  EEPROM.write(address + 2, two);
  EEPROM.write(address + 3, one);
  EEPROM.write(address + 4, zero);
}

void EEPROMReadAlert(long address, Alerts *oAlert)
{
  //Read the 4 bytes from the eeprom memory.  
  long four = EEPROM.read(address);
  long three = EEPROM.read(address + 1);
  long two = EEPROM.read(address + 2);
  long one = EEPROM.read(address + 3);

  long value = ((four << 0) & 0xFF) + ((three << 8) & 0xFFFF) + ((two << 16) & 0xFFFFFF) + ((one << 24) & 0xFFFFFFFF);
  
  oAlert->iId = EEPROM.read(address + 4);
  if (oAlert->iId < MAX_ALERTS) {  
    oAlert->iTime = (value >> (11+8)) & 0x7FF;
    oAlert->iDuration = (value >> 8) & 0x7FF;
    oAlert->bWeekdays =  (value >> 0) & 0xFF;
  } else {
    oAlert->iId = -1;
  }
}
