#pragma once

#include <Arduino.h>

#include "esp.h"
#include "motor.h"
#include "radar.h"

/**Our Board*/
class Board
{
private:
    //attributes
    ESP m_esp;
    Radar m_radar;
    Motor m_motor;

    uint8_t coverage = 50;
    uint8_t timeout = 3;

    void SendScanReport();
    void SendData();
    void ReceiveData();
    
public:
    
    void Init();
    void HandleRoutine();
    

    //Constructor
    Board();
};