#include "radar.h"

    //constructor
    Radar::Radar(){}

    void Radar::Init()
    {
        pinMode(m_trig, OUTPUT);
        pinMode(m_echo, INPUT);
    }
    void Radar::Scan(){}