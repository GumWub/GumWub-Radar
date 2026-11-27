package com.example.testapp.database;

public interface AppDatabase extends AutoCloseable {
    RadarDao radarDao();
    RadarReadingDao radarReadingDao();
    RadarSettingsDao radarSettingsDao();

    @Override
    void close();
}
