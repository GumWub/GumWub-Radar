#include "radar.h"

    //I can't use some C++ functions for some reason

    //constructor
    Radar::Radar():
    m_time(defaultTime),//time in s
    m_timeout(defaultSafetyTimeOut)//3ms timeout default
    {
    }

    void Radar::CoverageToTime(uint8_t &coverage)
    {
        Clamp100(coverage);
        float distance = ((coverage*(max_distance-min_distance))/100) + min_distance;
        m_time = (distance*2)/SpeedOfSound;
    }


    //Evil Branchless Clamp hack
    void Radar::Clamp100(uint8_t &value)
    {
        uint8_t cmp = (value<100);
        value = value*(cmp) + 100*(1-(cmp));
    }

    void Radar::Init(uint8_t &coverage, uint8_t &timeout)
    {
        pinMode(m_trig, OUTPUT);
        pinMode(m_echo, INPUT);

        digitalWrite(m_trig, LOW);
        SetCoverage(coverage);
        SetTimeOut(timeout);
    }

    void Radar::SetTimeOut(uint8_t &timeout)
    {
        if(timeout == 0 || timeout > 60)
            return;

        m_timeout = (timeout)*1e3;
    }

    void Radar::TriggerScan()
    {
        digitalWrite(m_trig, LOW);
        delayMicroseconds(PreTriggerLowSafetyDuration);


        digitalWrite(m_trig, HIGH);
        delayMicroseconds(TiggerDuration);

        digitalWrite(m_trig, LOW);
    }

    bool Radar::IsHardwareFaulty()
    {
        unsigned long start = micros();
        
        while ((digitalRead(m_echo) == HIGH) && (micros()-start) < defaultEchoTimeout*1e6){}

        return (digitalRead(m_echo) == HIGH);
    }

    bool Radar::HasScanFailed()
    {
        unsigned long start = micros();

        while ((digitalRead(m_echo) == LOW) && (micros() - start) < m_timeout*1e6){}

        return (digitalRead(m_echo) == LOW);
    }

    float Radar::ProcessScan()
    {
        unsigned long start = micros();

        while(digitalRead(m_echo) == 1)
        {
            if((micros() - start)<m_time*1e6)
                return -1;
        }

        return (((micros() - start)/2) * SpeedOfSound);
    }

    float Radar::Scan(){
        
        if(IsHardwareFaulty())
            return -3;

        TriggerScan();

        if(HasScanFailed())
            return -2;

        return ProcessScan();
    }

    void Radar::SetCoverage(uint8_t &coverage){  
        Clamp100(coverage);
        CoverageToTime(coverage);
    }