#pragma once

class Radar
{
    //attributes
    float m_coverage;

    //Helpers
    void Init();
public:
    
    void Scan();

    //constructor
    Radar();
};