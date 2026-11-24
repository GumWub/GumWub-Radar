#pragma once

#include <Arduino.h>


class ESP 
{
    void ReceiveData();
    void SendData();

public:
    
    void Init();

    //Constructor
    ESP();
};