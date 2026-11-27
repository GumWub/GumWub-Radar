package com.example.testapp.services;

import com.example.testapp.models.RadarEntity;
import com.example.testapp.models.RadarReadingEntity;
import com.example.testapp.models.RadarSettingsEntity;

import java.util.List;

public interface RadarService {
    List<RadarEntity> getRadars();
    RadarEntity getRadar(String radarId);
    List<RadarReadingEntity> getReadings(String radarId);
    RadarSettingsEntity getSettings();
}
