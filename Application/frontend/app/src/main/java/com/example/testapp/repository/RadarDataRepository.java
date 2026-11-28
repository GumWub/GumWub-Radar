package com.example.testapp.repository;

import com.example.testapp.models.RadarEntity;
import com.example.testapp.models.RadarReadingEntity;
import com.example.testapp.models.RadarSettingsEntity;

import java.util.List;

public interface RadarDataRepository {
    List<RadarEntity> getRadars();
    RadarEntity getRadar(String radarId);
    void saveRadar(RadarEntity radar);
    void deleteRadar(String radarId);

    List<RadarReadingEntity> getReadings(String radarId);
    void saveReading(RadarReadingEntity reading);

    RadarSettingsEntity getSettings();
    void saveSettings(RadarSettingsEntity settings);
}
