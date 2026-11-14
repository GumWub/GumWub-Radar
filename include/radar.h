#pragma once

#include <Arduino.h>


//HC-SR04 (Ultra Sound radar)
class Radar
{
    //Modify the pins the radar is plugged into here
    uint8_t m_trig = 12;
    uint8_t m_echo = 13;

    //attributes
    float m_coverage;

    //Helpers
    
public:

    void Init();
    void Scan();

    //constructor
    Radar();
};