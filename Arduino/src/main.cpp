#include <Arduino.h>
#include "board.h"

Board arduino = Board();

void setup() {
  arduino.Init();
}

void loop() {
  arduino.HandleRoutine();
}
