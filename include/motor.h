#pragma once

#include <stdint.h>
#include <Arduino.h>

#include "motor_steps.h"

//ULN2003AN (Stepper Motor Controller)
class Motor
{

private:
    //Modify the pins the step motor controller is plugged into here
    uint8_t m_in1 = 8;
    uint8_t m_in2 = 4;
    uint8_t m_in3 = 7;
    uint8_t m_in4 = 2;

    //attributes
    ScanMode m_mode;
    size_t m_current_step;

    const Step *m_steps_index;
    const uint8_t *m_steps;

    void Rotate();
    void ResetPosition();
    uint8_t BitMaskCheck(uint8_t pin_number);

public:

    void Init();
    void SetScanMode(ScanMode mode);
    void NextStep();

    //Constructor;
    Motor();
};