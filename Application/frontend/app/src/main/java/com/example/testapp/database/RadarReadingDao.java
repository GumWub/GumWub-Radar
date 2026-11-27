package com.example.testapp.database;

import com.example.testapp.models.RadarReadingEntity;

import java.util.List;

public interface RadarReadingDao {
    List<RadarReadingEntity> getForRadar(String radarId);
    void insert(RadarReadingEntity reading);
    void deleteForRadar(String radarId);
}
