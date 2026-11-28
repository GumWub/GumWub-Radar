package com.example.testapp;

import android.os.Handler;
import android.os.Looper;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class HttpRadarRepository implements RadarRepository {
    public interface AuthenticationCallback {
        void onSuccess();
        void onFailure(String message);
    }

    private static final int TIMEOUT_MS = 4000;
    private static final long POLL_INTERVAL_MS = 250;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor();
    private Listener listener;
    private ScheduledFuture<?> pollTask;
    private String baseUrl;
    private List<RadarDevice> radars = Collections.emptyList();
    private final Set<String> radarErrorsReported = new HashSet<>();
    private int generation;
    private volatile boolean connected;
    private volatile boolean scanning;

    @Override
    public synchronized void setListener(Listener listener) {
        this.listener = listener;
    }

    public void authenticate(String backendUrl, String username, String password,
                             AuthenticationCallback callback) {
        String normalizedUrl = trimTrailingSlash(backendUrl);
        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                JSONObject body = new JSONObject();
                body.put("username", username);
                body.put("password", password);
                connection = (HttpURLConnection) new URL(
                        normalizedUrl + "/api/auth/login").openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(TIMEOUT_MS);
                connection.setReadTimeout(TIMEOUT_MS);
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty(
                        "Content-Type", "application/json; charset=utf-8");
                connection.setDoOutput(true);
                byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(payload);
                }

                int statusCode = connection.getResponseCode();
                InputStream responseStream = statusCode >= 200 && statusCode < 300
                        ? connection.getInputStream() : connection.getErrorStream();
                readResponse(responseStream);
                if (statusCode >= 200 && statusCode < 300) {
                    mainHandler.post(callback::onSuccess);
                } else if (statusCode == HttpURLConnection.HTTP_UNAUTHORIZED
                        || statusCode == HttpURLConnection.HTTP_FORBIDDEN) {
                    mainHandler.post(() -> callback.onFailure(
                            "Username or password is incorrect."));
                } else {
                    mainHandler.post(() -> callback.onFailure(
                            "Login failed (HTTP " + statusCode + ")."));
                }
            } catch (IOException | JSONException exception) {
                String message = messageFor(exception, "Could not reach the login server.");
                mainHandler.post(() -> callback.onFailure(message));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    @Override
    public void connect(String baseUrl) {
        final int requestGeneration;
        final String normalizedUrl = trimTrailingSlash(baseUrl);
        synchronized (this) {
            generation++;
            requestGeneration = generation;
            stopPollingLocked();
            connected = false;
            scanning = false;
            radarErrorsReported.clear();
            radars = Collections.emptyList();
            this.baseUrl = normalizedUrl;
        }

        executor.execute(() -> {
            try {
                requestJson("GET", normalizedUrl + "/api/status", null);
                synchronized (HttpRadarRepository.this) {
                    if (requestGeneration != generation) {
                        return;
                    }
                    connected = true;
                }
                postConnectionChanged(requestGeneration, true);
                try {
                    List<RadarDevice> loadedRadars = parseRadars(
                            requestJson("GET", normalizedUrl + "/api/radars", null));
                    synchronized (HttpRadarRepository.this) {
                        if (requestGeneration != generation) {
                            return;
                        }
                        radars = loadedRadars;
                    }
                    postRadarsChanged(requestGeneration, loadedRadars);
                } catch (IOException | JSONException exception) {
                    postError(requestGeneration,
                            messageFor(exception, "Could not load radar devices"));
                }
                try {
                    RadarSettings settings = parseSettings(
                            requestJson("GET", normalizedUrl + "/api/settings", null));
                    postSettings(requestGeneration, settings);
                } catch (IOException | JSONException exception) {
                    postError(requestGeneration,
                            messageFor(exception, "Could not load backend settings"));
                }
            } catch (IOException | JSONException exception) {
                postError(requestGeneration, messageFor(exception, "Could not connect to backend"));
            }
        });
    }

    @Override
    public synchronized void disconnect() {
        generation++;
        scanning = false;
        connected = false;
        stopPollingLocked();
        postConnectionChanged(generation, false);
    }

    @Override
    public synchronized void startScanning() {
        if (!connected || scanning) {
            return;
        }
        scanning = true;
        int pollGeneration = generation;
        pollTask = executor.scheduleWithFixedDelay(
                () -> pollRadars(pollGeneration), 0, POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void pollRadars(int pollGeneration) {
        List<RadarDevice> currentRadars;
        synchronized (this) {
            if (!scanning || pollGeneration != generation) {
                return;
            }
            currentRadars = new ArrayList<>(radars);
        }
        for (RadarDevice radar : currentRadars) {
            if (!isCurrentScan(pollGeneration)) {
                return;
            }
            pollRadar(pollGeneration, radar);
        }
    }

    private void pollRadar(int pollGeneration, RadarDevice radar) {
        String url = baseUrl + "/api/radars/" + Uri.encode(radar.getId()) + "/reading";
        try {
            RadarReading reading = parseReading(requestJson("GET", url, null));
            synchronized (this) {
                radarErrorsReported.remove(radar.getId());
            }
            postReading(pollGeneration, radar.getId(), reading);
        } catch (IOException | JSONException exception) {
            boolean report;
            synchronized (this) {
                report = radarErrorsReported.add(radar.getId());
            }
            if (report) {
                postError(pollGeneration, radar.getName() + ": "
                        + messageFor(exception, "Could not read radar data"));
            }
        }
    }

    private synchronized boolean isCurrentScan(int pollGeneration) {
        return scanning && pollGeneration == generation;
    }

    @Override
    public synchronized void stopScanning() {
        scanning = false;
        stopPollingLocked();
    }

    @Override
    public void addRadar() {
        final int requestGeneration;
        final String radarsUrl;
        final int nextNumber;
        synchronized (this) {
            if (!connected || baseUrl == null) {
                postError(generation, "Connect to the backend before adding a radar");
                return;
            }
            requestGeneration = generation;
            radarsUrl = baseUrl + "/api/radars";
            nextNumber = radars.size() + 1;
        }

        executor.execute(() -> {
            JSONObject body = new JSONObject();
            try {
                body.put("name", "Radar " + nextNumber);
                RadarDevice created = parseRadar(requestJson("POST", radarsUrl, body));
                List<RadarDevice> updated;
                synchronized (HttpRadarRepository.this) {
                    if (requestGeneration != generation) {
                        return;
                    }
                    updated = new ArrayList<>(radars);
                    for (RadarDevice existing : updated) {
                        if (existing.getId().equals(created.getId())) {
                            throw new IOException("Backend returned a duplicate radar ID");
                        }
                    }
                    updated.add(created);
                    radars = updated;
                }
                postRadarsChanged(requestGeneration, updated);
            } catch (IOException | JSONException exception) {
                postError(requestGeneration, messageFor(exception, "Could not add radar"));
            }
        });
    }

    @Override
    public void sendSettings(RadarSettings settings) {
        final int requestGeneration;
        final String settingsUrl;
        synchronized (this) {
            if (!connected || baseUrl == null) {
                postError(generation, "Connect to the backend before saving settings");
                return;
            }
            requestGeneration = generation;
            settingsUrl = baseUrl + "/api/settings";
        }

        executor.execute(() -> {
            JSONObject body = new JSONObject();
            try {
                body.put("robotCoverage", settings.getRobotCoverage());
                body.put("motorSweepDegrees", settings.getMotorSweepDegrees());
                body.put("sensitivityCm", settings.getSensitivityCm());
                requestJson("PUT", settingsUrl, body);
                postSettings(requestGeneration, settings);
            } catch (IOException | JSONException exception) {
                postError(requestGeneration,
                        messageFor(exception, "Could not save backend settings"));
            }
        });
    }

    private JSONObject requestJson(String method, String endpoint, JSONObject body)
            throws IOException, JSONException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(endpoint).openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestProperty("Accept", "application/json");
            if (body != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(payload);
                }
            }

            int statusCode = connection.getResponseCode();
            InputStream responseStream = statusCode >= 200 && statusCode < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            String response = readResponse(responseStream);
            if (statusCode < 200 || statusCode >= 300) {
                throw new IOException("Backend returned HTTP " + statusCode
                        + (response.isEmpty() ? "" : ": " + response));
            }
            if (response.isEmpty()) {
                return new JSONObject();
            }
            return new JSONObject(response);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String readResponse(InputStream input) throws IOException {
        if (input == null) {
            return "";
        }
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }

    private List<RadarDevice> parseRadars(JSONObject response) throws JSONException, IOException {
        JSONArray items = response.optJSONArray("data");
        if (items == null) {
            items = response.optJSONArray("radars");
        }
        if (items == null) {
            JSONObject data = response.optJSONObject("data");
            if (data != null) {
                items = data.optJSONArray("radars");
            }
        }
        if (items == null) {
            throw new IOException("Backend response must include a radars array");
        }
        List<RadarDevice> result = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (int index = 0; index < items.length(); index++) {
            RadarDevice radar = parseRadar(items.getJSONObject(index));
            if (!ids.add(radar.getId())) {
                throw new IOException("Backend returned duplicate radar IDs");
            }
            result.add(radar);
        }
        return result;
    }

    private RadarDevice parseRadar(JSONObject response) throws JSONException, IOException {
        JSONObject data = response.optJSONObject("data");
        JSONObject radar = data == null ? response : data;
        String id = radar.getString("id").trim();
        String name = radar.optString("name", "").trim();
        if (id.isEmpty()) {
            throw new IOException("Backend returned a radar without an ID");
        }
        return new RadarDevice(id, name.isEmpty() ? id : name);
    }

    private RadarSettings parseSettings(JSONObject response) throws JSONException, IOException {
        JSONObject data = response.optJSONObject("data");
        JSONObject settings = data == null ? response : data;
        int coverage = settings.getInt("robotCoverage");
        int sweep = settings.getInt("motorSweepDegrees");
        int sensitivity = settings.getInt("sensitivityCm");
        if (coverage < 0 || coverage > 100 || sweep < 0 || sweep > 180
                || sensitivity < 2 || sensitivity > 400) {
            throw new IOException("Backend returned invalid settings");
        }
        return new RadarSettings(coverage, sweep, sensitivity);
    }

    private RadarReading parseReading(JSONObject response) throws JSONException, IOException {
        JSONObject data = response.optJSONObject("data");
        JSONObject reading = data == null ? response : data;
        double distance = reading.has("distanceCm")
                ? reading.getDouble("distanceCm") : reading.getDouble("distance");
        double angle = reading.has("angleDegrees")
                ? reading.getDouble("angleDegrees") : reading.getDouble("angle");
        if (Double.isNaN(distance) || Double.isInfinite(distance) || distance < 0
                || Double.isNaN(angle) || Double.isInfinite(angle) || angle < 0 || angle > 180) {
            throw new IOException("Backend returned invalid radar values");
        }
        return new RadarReading((float) distance, (float) angle, System.currentTimeMillis());
    }

    private String trimTrailingSlash(String value) {
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String messageFor(Exception exception, String fallback) {
        String message = exception.getMessage();
        return message == null || message.trim().isEmpty() ? fallback : message;
    }

    private void stopPollingLocked() {
        if (pollTask != null) {
            pollTask.cancel(false);
            pollTask = null;
        }
    }

    private void postConnectionChanged(int callbackGeneration, boolean isConnected) {
        mainHandler.post(() -> {
            Listener currentListener;
            synchronized (HttpRadarRepository.this) {
                if (callbackGeneration != generation) {
                    return;
                }
                currentListener = listener;
            }
            if (currentListener != null) {
                currentListener.onConnectionChanged(isConnected);
            }
        });
    }

    private void postReading(int callbackGeneration, String radarId, RadarReading reading) {
        mainHandler.post(() -> {
            Listener currentListener;
            synchronized (HttpRadarRepository.this) {
                if (callbackGeneration != generation || !scanning) {
                    return;
                }
                currentListener = listener;
            }
            if (currentListener != null) {
                currentListener.onReading(radarId, reading);
            }
        });
    }

    private void postRadarsChanged(int callbackGeneration, List<RadarDevice> updatedRadars) {
        List<RadarDevice> snapshot = Collections.unmodifiableList(new ArrayList<>(updatedRadars));
        mainHandler.post(() -> {
            Listener currentListener;
            synchronized (HttpRadarRepository.this) {
                if (callbackGeneration != generation) {
                    return;
                }
                currentListener = listener;
            }
            if (currentListener != null) {
                currentListener.onRadarsChanged(snapshot);
            }
        });
    }

    private void postSettings(int callbackGeneration, RadarSettings settings) {
        mainHandler.post(() -> {
            Listener currentListener;
            synchronized (HttpRadarRepository.this) {
                if (callbackGeneration != generation) {
                    return;
                }
                currentListener = listener;
            }
            if (currentListener != null) {
                currentListener.onSettingsReceived(settings);
            }
        });
    }

    private void postError(int callbackGeneration, String message) {
        mainHandler.post(() -> {
            Listener currentListener;
            synchronized (HttpRadarRepository.this) {
                if (callbackGeneration != generation) {
                    return;
                }
                currentListener = listener;
            }
            if (currentListener != null) {
                currentListener.onError(message);
            }
        });
    }

    @Override
    public void release() {
        disconnect();
        setListener(null);
        executor.shutdownNow();
    }
}
