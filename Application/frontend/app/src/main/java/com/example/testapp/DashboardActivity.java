package com.example.testapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.app.Dialog;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardActivity extends AppCompatActivity {
    private static final String PREFERENCES_NAME = "dashboard_preferences";
    private static final String KEY_BACKEND_URL = "backend_url";
    private static final String DEFAULT_BACKEND_URL = "http://192.168.1.10:8080";

    private RadarViewModel viewModel;
    private HttpRadarRepository repository;
    private LinearLayout adminPanel;
    private LinearLayout radarCardsContainer;
    private TextView connectionStatus;
    private TextView scanStatus;
    private TextView linkMessage;
    private TextView roleMenuButton;
    private LinearLayout readingsContainer;
    private Button connectButton;
    private Button scanButton;
    private Button addRadarButton;
    private Button sendSettingsButton;
    private TextView activeSettingsValue;
    private TextView coverageValue;
    private TextView motorSweepValue;
    private TextView sensitivityValue;
    private android.widget.SeekBar coverageSeek;
    private android.widget.SeekBar motorSweepSeek;
    private android.widget.SeekBar sensitivitySeek;
    private boolean connected;
    private boolean scanning;
    private boolean adminMode;
    private final Map<String, RadarCardViews> radarCards = new HashMap<>();
    private final Map<String, String> radarNames = new HashMap<>();
    private final Map<String, List<RadarReading>> readingsByRadar = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        adminPanel = findViewById(R.id.adminPanel);
        radarCardsContainer = findViewById(R.id.radarCardsContainer);
        connectionStatus = findViewById(R.id.connectionStatus);
        scanStatus = findViewById(R.id.scanStatus);
        linkMessage = findViewById(R.id.linkMessage);
        roleMenuButton = findViewById(R.id.roleMenuButton);
        readingsContainer = findViewById(R.id.readingsContainer);
        connectButton = findViewById(R.id.connectButton);
        scanButton = findViewById(R.id.scanButton);
        addRadarButton = findViewById(R.id.addRadarButton);
        sendSettingsButton = findViewById(R.id.sendSettingsButton);
        activeSettingsValue = findViewById(R.id.activeSettingsValue);
        coverageValue = findViewById(R.id.coverageValue);
        motorSweepValue = findViewById(R.id.motorSweepValue);
        sensitivityValue = findViewById(R.id.sensitivityValue);
        coverageSeek = findViewById(R.id.coverageSeek);
        motorSweepSeek = findViewById(R.id.motorSweepSeek);
        sensitivitySeek = findViewById(R.id.sensitivitySeek);

        repository = new HttpRadarRepository();
        viewModel = new RadarViewModel(repository);
        viewModel.setListener(new RadarViewModel.Listener() {
            @Override
            public void onConnectionChanged(boolean connected, boolean scanning) {
                renderState(connected, scanning);
            }

            @Override
            public void onRadarsChanged(List<RadarDevice> radars) {
                renderRadars(radars);
                addRadarButton.setEnabled(true);
            }

            @Override
            public void onStateChanged(String radarId, RadarReading reading, int count,
                                       List<RadarReading> readings) {
                renderRadarReading(radarId, reading, count, readings);
            }

            @Override
            public void onSettingsReceived(RadarSettings settings) {
                activeSettingsValue.setText(settings.toDisplayLabel());
                activeSettingsValue.setTextColor(
                        getResources().getColor(R.color.success, getTheme()));
                coverageSeek.setProgress(settings.getRobotCoverage());
                motorSweepSeek.setProgress(settings.getMotorSweepDegrees());
                sensitivitySeek.setProgress(settings.getSensitivityCm() - 2);
                linkMessage.setText("Backend settings loaded");
                linkMessage.setTextColor(
                        getResources().getColor(R.color.success, getTheme()));
            }

            @Override
            public void onError(String message) {
                linkMessage.setText(message);
                linkMessage.setTextColor(
                        getResources().getColor(R.color.warning, getTheme()));
                if (!connected) {
                    connectButton.setEnabled(true);
                    connectionStatus.setText("CONNECTION ERROR");
                    connectionStatus.setTextColor(
                            getResources().getColor(R.color.warning, getTheme()));
                }
                addRadarButton.setEnabled(true);
            }
        });
        renderRadars(java.util.Collections.emptyList());

        connectButton.setOnClickListener(v -> {
            if (connected) {
                viewModel.disconnect();
            } else {
                showConnectionPopup();
            }
        });

        roleMenuButton.setOnClickListener(v -> showRoleMenu(v));
        addRadarButton.setOnClickListener(v -> {
            if (adminMode && connected) {
                addRadarButton.setEnabled(false);
                viewModel.addRadar();
            } else if (!connected) {
                showMessage("Connect to the backend before adding a radar.");
            }
        });
        sendSettingsButton.setOnClickListener(v -> {
            if (!adminMode || !connected) {
                return;
            }
            RadarSettings settings = new RadarSettings(
                    coverageSeek.getProgress(),
                    motorSweepSeek.getProgress(),
                    sensitivitySeek.getProgress() + 2);
            viewModel.sendSettings(settings);
            linkMessage.setText("Saving settings…");
            linkMessage.setTextColor(getResources().getColor(R.color.muted, getTheme()));
        });

        coverageSeek.setOnSeekBarChangeListener(new ValueListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress,
                                          boolean fromUser) {
                coverageValue.setText(progress + "%");
            }
        });
        motorSweepSeek.setOnSeekBarChangeListener(new ValueListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress,
                                          boolean fromUser) {
                motorSweepValue.setText(progress + "°");
            }
        });
        sensitivitySeek.setOnSeekBarChangeListener(new ValueListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress,
                                          boolean fromUser) {
                sensitivityValue.setText((progress + 2) + " cm");
            }
        });

        scanButton.setOnClickListener(v -> {
            if (!connected) {
                return;
            }
            if (scanning) {
                viewModel.stopScanning();
            } else {
                viewModel.startScanning();
            }
        });
    }

    private void showRoleMenu(android.view.View anchor) {
        android.widget.PopupMenu menu = new android.widget.PopupMenu(this, anchor);
        if (adminMode) {
            menu.getMenu().add("Admin logout");
        } else {
            menu.getMenu().add("Admin login");
        }
        menu.setOnMenuItemClickListener(item -> {
            if (adminMode) {
                setAdminMode(false);
            } else {
                requestAdminAccess();
            }
            return true;
        });
        menu.show();
    }

    private void requestAdminAccess() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_admin_login);
        dialog.setCanceledOnTouchOutside(true);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }

        EditText usernameInput = dialog.findViewById(R.id.adminUsernameInput);
        EditText passwordInput = dialog.findViewById(R.id.adminPasswordInput);
        TextView errorMessage = dialog.findViewById(R.id.adminLoginError);
        Button loginButton = dialog.findViewById(R.id.adminLoginSubmit);
        Button cancelButton = dialog.findViewById(R.id.adminLoginCancel);
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        loginButton.setOnClickListener(v -> {
            String username = usernameInput.getText() == null
                    ? "" : usernameInput.getText().toString().trim();
            String password = passwordInput.getText() == null
                    ? "" : passwordInput.getText().toString();
            if (username.isEmpty()) {
                usernameInput.setError("Enter your username");
                return;
            }
            if (password.isEmpty()) {
                passwordInput.setError("Enter your password");
                return;
            }

            SharedPreferences preferences = getSharedPreferences(
                    PREFERENCES_NAME, MODE_PRIVATE);
            String backendUrl = preferences.getString(KEY_BACKEND_URL, DEFAULT_BACKEND_URL);
            loginButton.setEnabled(false);
            loginButton.setText("Signing in...");
            errorMessage.setVisibility(android.view.View.GONE);
            repository.authenticate(backendUrl, username, password,
                    new HttpRadarRepository.AuthenticationCallback() {
                        @Override
                        public void onSuccess() {
                            if (isFinishing() || !dialog.isShowing()) {
                                return;
                            }
                            setAdminMode(true);
                            dialog.dismiss();
                            if (!connected) {
                                connectToBackend(backendUrl);
                            }
                        }

                        @Override
                        public void onFailure(String message) {
                            if (isFinishing() || !dialog.isShowing()) {
                                return;
                            }
                            errorMessage.setText(message);
                            errorMessage.setVisibility(android.view.View.VISIBLE);
                            loginButton.setEnabled(true);
                            loginButton.setText("Sign in");
                        }
                    });
        });
        dialog.setOnShowListener(ignored -> {
            Window shownWindow = dialog.getWindow();
            if (shownWindow != null) {
                int screenWidth = getResources().getDisplayMetrics().widthPixels;
                int dialogWidth = Math.min(screenWidth - dp(32), dp(420));
                shownWindow.setLayout(dialogWidth, WindowManager.LayoutParams.WRAP_CONTENT);
            }
        });
        dialog.show();
    }

    private void setAdminMode(boolean enabled) {
        adminMode = enabled;
        adminPanel.setVisibility(enabled ? android.view.View.VISIBLE : android.view.View.GONE);
        addRadarButton.setVisibility(enabled ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private void showMessage(String message) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showConnectionPopup() {
        SharedPreferences preferences = getSharedPreferences(
                PREFERENCES_NAME, Context.MODE_PRIVATE);
        EditText urlInput = new EditText(this);
        urlInput.setSingleLine(true);
        urlInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        urlInput.setHint("http://192.168.1.10:8080");
        urlInput.setText(preferences.getString(KEY_BACKEND_URL, DEFAULT_BACKEND_URL));
        int horizontalPadding = (int) (20 * getResources().getDisplayMetrics().density);
        urlInput.setPadding(horizontalPadding, 0, horizontalPadding, 0);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(horizontalPadding, 0, horizontalPadding, 0);
        TextView description = new TextView(this);
        description.setText("Enter your Java backend address");
        description.setTextColor(getResources().getColor(R.color.muted, getTheme()));
        content.addView(description);
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        inputParams.topMargin = (int) (8 * getResources().getDisplayMetrics().density);
        content.addView(urlInput, inputParams);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Connect backend")
                .setView(content)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Connect", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String baseUrl = urlInput.getText().toString().trim();
                    Uri uri = Uri.parse(baseUrl);
                    String scheme = uri.getScheme();
                    if (baseUrl.isEmpty() || uri.getHost() == null
                            || !("http".equalsIgnoreCase(scheme)
                            || "https".equalsIgnoreCase(scheme))) {
                        urlInput.setError("Enter a valid http:// or https:// URL");
                        return;
                    }
                    preferences.edit().putString(KEY_BACKEND_URL, baseUrl).apply();
                    dialog.dismiss();
                    connectToBackend(baseUrl);
                }));
        dialog.show();
    }

    private void connectToBackend(String baseUrl) {
        connectionStatus.setText("CONNECTING");
        connectionStatus.setTextColor(
                getResources().getColor(R.color.accent, getTheme()));
        linkMessage.setText("Connecting to " + baseUrl);
        linkMessage.setTextColor(getResources().getColor(R.color.muted, getTheme()));
        connectButton.setEnabled(false);
        viewModel.connect(baseUrl);
    }

    private void renderState(boolean isConnected, boolean isScanning) {
        connected = isConnected;
        scanning = isScanning;

        connectionStatus.setText(isConnected ? "CONNECTED" : "DISCONNECTED");
        connectionStatus.setTextColor(isConnected
                ? getResources().getColor(R.color.success, getTheme())
                : getResources().getColor(R.color.warning, getTheme()));
        scanStatus.setText(isScanning ? "SCANNING" : "STANDBY");
        scanStatus.setTextColor(isScanning
                ? getResources().getColor(R.color.success, getTheme())
                : getResources().getColor(R.color.muted, getTheme()));
        connectButton.setText(isConnected ? "Disconnect" : "Connect");
        connectButton.setEnabled(true);
        scanButton.setText(isScanning ? "Stop scan" : "Start scan");
        scanButton.setEnabled(isConnected);
        sendSettingsButton.setEnabled(isConnected && adminMode);
        addRadarButton.setEnabled(isConnected);
        for (RadarCardViews card : radarCards.values()) {
            card.radarView.setSweepEnabled(isScanning);
        }

        if (isConnected) {
            linkMessage.setText("Connected to Java backend");
            linkMessage.setTextColor(getResources().getColor(R.color.success, getTheme()));
        } else {
            linkMessage.setText(adminMode
                    ? "Ready to connect to your Java backend"
                    : "Admin access is required to connect the backend");
            linkMessage.setTextColor(getResources().getColor(R.color.muted, getTheme()));
        }
    }

    private void renderRadars(List<RadarDevice> radars) {
        radarCardsContainer.removeAllViews();
        radarCards.clear();
        radarNames.clear();
        for (RadarDevice radar : radars) {
            radarNames.put(radar.getId(), radar.getName());
            RadarCardViews card = createRadarCard(radar);
            radarCards.put(radar.getId(), card);
            radarCardsContainer.addView(card.container);
        }
        if (radars.isEmpty()) {
            RadarDevice defaultRadar = new RadarDevice("default-radar", "Radar");
            radarNames.put(defaultRadar.getId(), defaultRadar.getName());
            RadarCardViews card = createRadarCard(defaultRadar);
            radarCards.put(defaultRadar.getId(), card);
            radarCardsContainer.addView(card.container);
        }
    }

    private RadarCardViews createRadarCard(RadarDevice radar) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(12));
        card.setBackgroundResource(R.drawable.card_bg);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(10);
        card.setLayoutParams(cardParams);

        TextView title = new TextView(this);
        title.setText(radarCards.isEmpty()
                ? "LIVE RADAR"
                : "LIVE RADAR  ·  " + radar.getName());
        title.setTextColor(getResources().getColor(R.color.accent, getTheme()));
        title.setTextSize(11);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setLetterSpacing(0.08f);
        title.setPadding(dp(2), dp(2), dp(2), dp(6));
        card.addView(title);

        RadarView radarView = new RadarView(this, null);
        card.addView(radarView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(250)));

        LinearLayout values = new LinearLayout(this);
        values.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams valuesParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        valuesParams.topMargin = dp(8);
        TextView distance = createMetric(values, "DISTANCE", "-- cm");
        TextView angle = createMetric(values, "ANGLE", "-- deg");
        TextView count = createMetric(values, "SCANS", "0");
        card.addView(values, valuesParams);
        return new RadarCardViews(card, radarView, distance, angle, count);
    }

    private TextView createMetric(LinearLayout parent, String label, String initialValue) {
        LinearLayout metric = new LinearLayout(this);
        metric.setOrientation(LinearLayout.VERTICAL);
        metric.setPadding(dp(10), dp(9), dp(6), dp(9));
        LinearLayout.LayoutParams metricParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        metricParams.setMarginEnd(dp(6));
        parent.addView(metric, metricParams);
        metric.setBackgroundResource(R.drawable.card_bg);

        TextView caption = new TextView(this);
        caption.setText(label);
        caption.setTextColor(getResources().getColor(R.color.muted, getTheme()));
        caption.setTextSize(9);
        metric.addView(caption);

        TextView value = new TextView(this);
        value.setText(initialValue);
        value.setTextColor(getResources().getColor(R.color.accent, getTheme()));
        value.setTextSize(15);
        value.setTypeface(null, android.graphics.Typeface.BOLD);
        metric.addView(value);
        return value;
    }

    private void renderRadarReading(String radarId, RadarReading reading, int count,
                                    List<RadarReading> readings) {
        RadarCardViews card = radarCards.get(radarId);
        if (card == null) {
            return;
        }
        card.distanceValue.setText(reading.getDistanceLabel());
        card.angleValue.setText(reading.getAngleLabel());
        card.readingCount.setText(String.valueOf(count));
        card.radarView.setReading(reading.getDistanceCm(), reading.getAngleDegrees(), scanning);
        readingsByRadar.put(radarId, readings);
        renderReadings();
    }

    private void renderReadings() {
        readingsContainer.removeAllViews();
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss", Locale.US);
        for (Map.Entry<String, List<RadarReading>> entry : readingsByRadar.entrySet()) {
            String radarName = radarNames.containsKey(entry.getKey())
                    ? radarNames.get(entry.getKey()) : entry.getKey();
            for (RadarReading reading : entry.getValue()) {
                TextView row = new TextView(this);
                row.setText(String.format(Locale.US, "%s  ·  %s  ·  %s  ·  %s",
                        radarName,
                        format.format(new Date(reading.getTimestampMillis())),
                        reading.getDistanceLabel(),
                        reading.getAngleLabel()));
                row.setTextColor(getResources().getColor(R.color.text_primary, getTheme()));
                row.setTextSize(12);
                row.setPadding(0, 8, 0, 8);
                readingsContainer.addView(row);
            }
        }
        if (readingsContainer.getChildCount() == 0) {
            TextView emptyState = new TextView(this);
            emptyState.setText("No readings yet");
            emptyState.setTextColor(getResources().getColor(R.color.muted, getTheme()));
            emptyState.setTextSize(13);
            emptyState.setPadding(0, dp(8), 0, dp(8));
            readingsContainer.addView(emptyState);
        }
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static class RadarCardViews {
        final LinearLayout container;
        final RadarView radarView;
        final TextView distanceValue;
        final TextView angleValue;
        final TextView readingCount;

        RadarCardViews(LinearLayout container, RadarView radarView, TextView distanceValue,
                       TextView angleValue, TextView readingCount) {
            this.container = container;
            this.radarView = radarView;
            this.distanceValue = distanceValue;
            this.angleValue = angleValue;
            this.readingCount = readingCount;
        }
    }

    private abstract static class ValueListener
            implements android.widget.SeekBar.OnSeekBarChangeListener {
        @Override
        public void onStartTrackingTouch(android.widget.SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(android.widget.SeekBar seekBar) {
        }
    }

    @Override
    protected void onDestroy() {
        if (viewModel != null) {
            viewModel.stopScanning();
            viewModel.release();
        }
        super.onDestroy();
    }
}
