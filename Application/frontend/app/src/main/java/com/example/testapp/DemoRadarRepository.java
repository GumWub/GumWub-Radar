package com.example.testapp;

import android.os.Handler;
import android.os.Looper;

import java.util.Random;
import java.util.Collections;

public class DemoRadarRepository implements RadarRepository {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private Listener listener;
    private boolean connected;
    private boolean scanning;
    private float angle;
    private static final String DEMO_RADAR_ID = "demo-radar";

    private final Runnable scanRunnable = new Runnable() {
        @Override
        public void run() {
            if (!connected || !scanning || listener == null) {
                return;
            }

            angle += 4.5f;
            if (angle > 180f) {
                angle -= 180f;
            }

            float baseDistance = 42f + (float) (14f * Math.sin(Math.toRadians(angle * 2f)));
            float distance = baseDistance + random.nextFloat() * 18f - 9f;
            listener.onReading(DEMO_RADAR_ID,
                    new RadarReading(distance, angle, System.currentTimeMillis()));
            handler.postDelayed(this, 120);
        }
    };

    @Override
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void connect(String baseUrl) {
        connected = true;
        if (listener != null) {
            listener.onConnectionChanged(true);
            listener.onRadarsChanged(Collections.singletonList(
                    new RadarDevice(DEMO_RADAR_ID, "Demo Radar")));
        }
    }

    @Override
    public void disconnect() {
        stopScanning();
        connected = false;
        if (listener != null) {
            listener.onConnectionChanged(false);
        }
    }

    @Override
    public void startScanning() {
        if (!connected) {
            return;
        }
        if (!scanning) {
            scanning = true;
            handler.post(scanRunnable);
        }
    }

    @Override
    public void stopScanning() {
        scanning = false;
        handler.removeCallbacks(scanRunnable);
    }

    @Override
    public void addRadar() {
        if (listener != null) {
            listener.onError("Demo mode does not support adding radars");
        }
    }

    @Override
    public void sendSettings(RadarSettings settings) {
        if (listener != null) {
            listener.onError("Demo mode does not support saving settings");
        }
    }

    @Override
    public void release() {
        stopScanning();
        listener = null;
        connected = false;
    }
}
