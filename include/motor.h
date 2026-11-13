#pragma once

#include <stdint.h>

enum class ScanMode : uint8_t
{
    accurate,//8 steps sweep
    fast//4 steps sweep
};

class Motor
{
private:
    //attributes
    ScanMode m_mode;

    void Init();
    void Rotate();

public:
    void ScanMode();

    //Constructor;
    Motor();
};