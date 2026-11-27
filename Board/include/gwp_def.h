#pragma once
#include <stdint.h>



//This header file defines the structures that will be used for the communication between our layers

struct ScanSettings
{
    uint8_t SweepMode;
    uint8_t RadarCoverage;
    uint8_t RadarSafetyTimeout;
};

struct InitData
{
    ScanSettings settings;
    uint8_t RadarMinDist;
    uint8_t RadarMaxDist;
};

struct ScanResultData
{
    uint8_t angle;
    uint8_t distance;
    uint8_t flags;
};