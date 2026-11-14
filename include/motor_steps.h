#pragma once

#include <stddef.h>
#include <stdint.h>


enum class ScanMode : uint8_t
{
    accurate,//8 steps sweep
    fast//4 steps sweep
};


struct Step{
    size_t steps_index;
    size_t next_index;
};

const Step  fast_mode_steps_index[6] = {{0,1},{1,2},{2,3},{3,4},{2,5},{1,0}};
const Step  accurate_mode_steps_index[14] = {{0,1},{1,2},{2,3},{3,4},{4,5},{5,6},{6,7},{7,8},{6,9},{5,10},{4,11},{3,12},{2,13},{1,0}};

const uint8_t fast_mode_steps[4] = {0b0001, 0b0010, 0b0100, 0b1000};
const uint8_t accurate_mode_steps[8] = {0b0001, 0b0011, 0b0010, 0b0110, 0b0100, 0b1100, 0b1000, 0b1001};