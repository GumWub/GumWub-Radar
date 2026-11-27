package com.example.testapp.database;

import com.example.testapp.models.RadarSettingsEntity;

public interface RadarSettingsDao {
    RadarSettingsEntity get();
    void save(RadarSettingsEntity settings);
}
