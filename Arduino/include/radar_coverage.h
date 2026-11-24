#pragma once

#include <stdint.h>

const float min_distance = 0.02f;//Radar's min distance in m
const float max_distance = 4.00f;//Radar's max distance in m

const float defaultEchoTimeout = 0.038;//Default time echo goes back to 0 in case nothing is around it (in s)
const float SpeedOfSound = 343;//speed of sound in m/s

const float defaultSafetyTimeOut = 0.003;
const float defaultTime = 0.0002;

const uint8_t PreTriggerLowSafetyDuration = 2;//in Microseconds
const uint8_t TiggerDuration = 10;//in microsecs