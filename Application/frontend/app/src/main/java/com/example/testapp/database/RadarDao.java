package com.example.testapp.database;

import com.example.testapp.models.RadarEntity;

import java.util.List;

public interface RadarDao {
    List<RadarEntity> getAll();
    RadarEntity getById(String radarId);
    void insert(RadarEntity radar);
    void delete(String radarId);
}
