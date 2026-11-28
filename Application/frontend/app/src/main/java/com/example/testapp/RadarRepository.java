package com.example.testapp;

public interface RadarRepository {
    interface Listener {
        void onConnectionChanged(boolean connected);
        void onRadarsChanged(java.util.List<RadarDevice> radars);
        void onReading(String radarId, RadarReading reading);
        void onSettingsReceived(RadarSettings settings);
        void onError(String message);
    }

    void setListener(Listener listener);
    void connect(String baseUrl);
    void disconnect();
    void startScanning();
    void stopScanning();
    void addRadar();
    void sendSettings(RadarSettings settings);
    void release();
}
