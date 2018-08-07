#ifndef DEBUGFUNCTIONS_H
#define DEBUGFUNCTIONS_H

#define DEBUG 1

#ifdef DEBUG
  #define DEBUG_PRINT(...) Serial.print(__VA_ARGS__);
  #define DEBUG_PRINTLN(...) Serial.println(__VA_ARGS__);
#else
  #define DEBUG_PRINT(...)
  #define DEBUG_PRINTLN(...)
#endif

#endif
