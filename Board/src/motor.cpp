   #include "motor.h"

    //Constructor;
    Motor::Motor(ScanMode mode):
    m_mode(mode),
    m_current_step(0),
    m_steps_index(nullptr),
    m_steps(nullptr)
    {
    }

    void Motor::Init()
    {
        pinMode(m_in1, OUTPUT);
        pinMode(m_in2, OUTPUT);
        pinMode(m_in3, OUTPUT);
        pinMode(m_in4, OUTPUT);

        InitScanMode();
        ResetPosition();
    }

    void Motor::ResetPosition()
    {
        Rotate();//In case our motor isn't powered on yet

        //Keep Rotating until we reach our desired position(Cool Transition animation if our Radar's position wasn't in the correct position)
        while (m_current_step != 0)
            NextStep();
        
    }

    void Motor::InitScanMode(){
        //Lightning Fast Switch Case
        switch (m_mode)
        {
        case (ScanMode::accurate):
            m_steps = &accurate_mode_steps[0];
            m_steps_index = &accurate_mode_steps_index[0];
            return;
        
        default:
            m_steps = &fast_mode_steps[0];
            m_steps_index = &fast_mode_steps_index[0];
            return;
        }
    }

    void Motor::Rotate()
    {
        digitalWrite(m_in1, BitMaskCheck(1));
        digitalWrite(m_in2, BitMaskCheck(2));
        digitalWrite(m_in3, BitMaskCheck(3));
        digitalWrite(m_in4, BitMaskCheck(4));
    }

    void Motor::SetScanMode(ScanMode mode)
    {
        //Avoid Unnecessary Operations
        if(m_mode == mode)
            return;

        //Reset Position
        while(m_current_step != 0)
            NextStep();
        
        //Set new Scan Mode
        m_mode = mode;

        
        //Lightning Fast Switch Case
        switch (m_mode)
        {
        case (ScanMode::accurate):
            m_steps = &accurate_mode_steps[0];
            m_steps_index = &accurate_mode_steps_index[0];
            return;
        
        default:
            m_steps = &fast_mode_steps[0];
            m_steps_index = &fast_mode_steps_index[0];
            return;
        }
        
    }

    
    void Motor::NextStep(){
        m_current_step = m_steps_index[m_current_step].next_index;
        Rotate();
    }

    uint8_t Motor::BitMaskCheck(uint8_t pin_number){
        return (((m_steps[m_steps_index[m_current_step].steps_index]) >> (pin_number-1)) & 1);
    }