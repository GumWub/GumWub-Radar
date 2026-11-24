#pragma once

#include <Arduino.h>
#include "radar_coverage.h"

//I'll create a bunch of functions I need instead of using the functions provided by C++ because PIO is limiting the functions that I can use 

//HC-SR04 (Ultra Sound radar)
class Radar
{

private:

    //Modify the pins the radar is plugged into here
    uint8_t m_trig = 12;
    uint8_t m_echo = 13;

    //attributes
    float m_time;
    float m_timeout;

    //Helpers
    
    //clamps my % between 0 and 100 (the variable is unsigned so it can't be a negative number)
    void Clamp100(uint8_t &value);
    void CoverageToTime(uint8_t &coverage);
    void TriggerScan();
    bool IsHardwareFaulty();
    bool HasScanFailed();
    float ProcessScan();

public:

    void Init(uint8_t &coverage, uint8_t &timeout);
    void SetCoverage(uint8_t &coverage);
    void SetTimeOut(uint8_t &timeout);
    float Scan();

    //constructor
    Radar();
};