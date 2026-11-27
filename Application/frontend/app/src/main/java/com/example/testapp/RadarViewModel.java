package com.example.testapp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RadarViewModel implements RadarRepository.Listener {
    public interface Listener {
        void onConnectionChanged(boolean connected, boolean scanning);
        void onRadarsChanged(List<RadarDevice> radars);
        void onStateChanged(String radarId, RadarReading reading,
                            int scanCount, List<RadarReading> readings);
        void onSettingsReceived(RadarSettings settings);
        void onError(String message);
    }

    private final RadarRepository repository;
    private Listener listener;
    private boolean connected;
    private boolean scanning;
    private final Map<String, Integer> scanCounts = new HashMap<>();
    private final Map<String, ArrayList<RadarReading>> readingsByRadar = new HashMap<>();

    public RadarViewModel(RadarRepository repository) {
        this.repository = repository;
        this.repository.setListener(this);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void connect(String baseUrl) {
        repository.connect(baseUrl);
    }

    public void disconnect() {
        repository.disconnect();
    }

    public void startScanning() {
        repository.startScanning();
        scanning = true;
        notifyConnection();
    }

    public void stopScanning() {
        repository.stopScanning();
        scanning = false;
        notifyConnection();
    }

    public void addRadar() {
        repository.addRadar();
    }

    public void sendSettings(RadarSettings settings) {
        repository.sendSettings(settings);
    }

    public void release() {
        listener = null;
        repository.release();
    }

    @Override
    public void onConnectionChanged(boolean connected) {
        this.connected = connected;
        if (!connected) {
            scanning = false;
        }
        notifyConnection();
    }

    @Override
    public void onRadarsChanged(List<RadarDevice> radars) {
        if (listener != null) {
            listener.onRadarsChanged(radars);
        }
    }

    @Override
    public void onReading(String radarId, RadarReading reading) {
        scanning = true;
        int scanCount = scanCounts.containsKey(radarId) ? scanCounts.get(radarId) + 1 : 1;
        scanCounts.put(radarId, scanCount);
        ArrayList<RadarReading> readings = readingsByRadar.get(radarId);
        if (readings == null) {
            readings = new ArrayList<>();
            readingsByRadar.put(radarId, readings);
        }
        readings.add(0, reading);
        while (readings.size() > 8) {
            readings.remove(readings.size() - 1);
        }
        if (listener != null) {
            listener.onStateChanged(
                    radarId,
                    reading,
                    scanCount,
                    Collections.unmodifiableList(new ArrayList<>(readings)));
        }
    }

    @Override
    public void onError(String message) {
        if (listener != null) {
            listener.onError(message);
        }
    }

    @Override
    public void onSettingsReceived(RadarSettings settings) {
        if (listener != null) {
            listener.onSettingsReceived(settings);
        }
    }

    private void notifyConnection() {
        if (listener != null) {
            listener.onConnectionChanged(connected, scanning);
        }
    }
}
