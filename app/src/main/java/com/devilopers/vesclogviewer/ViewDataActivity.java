package com.devilopers.vesclogviewer;

import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.text.Format;
import java.text.SimpleDateFormat;

public class ViewDataActivity extends AppCompatActivity {

    TextView tvSpeed, tvDistance, tvBattery, tvElasped;
    VESCData data;
    Switch switchRefresh;
    Uri uri;

    int sampling;
    int lastIndex = 0;
    float maxSpeed = 0;
    float maxSpeedMiles = 0;
    static final int MAX_DATA_POINTS = 20000;

    GraphView graphSpeed, graphDistance, graphTemp, graphVolt;
    LineGraphSeries<com.jjoe64.graphview.series.DataPoint> seriesSpeed = new LineGraphSeries<>();
    LineGraphSeries<com.jjoe64.graphview.series.DataPoint> seriesSpeedAvg = new LineGraphSeries<>();
    LineGraphSeries<com.jjoe64.graphview.series.DataPoint> seriesDistance = new LineGraphSeries<>();
    LineGraphSeries<com.jjoe64.graphview.series.DataPoint> seriesTemp = new LineGraphSeries<>();
    LineGraphSeries<com.jjoe64.graphview.series.DataPoint> seriesTempEsc = new LineGraphSeries<>();
    LineGraphSeries<com.jjoe64.graphview.series.DataPoint> seriesVoltage = new LineGraphSeries<>();
    LineGraphSeries<com.jjoe64.graphview.series.DataPoint> seriesBattery = new LineGraphSeries<>();

    // Column indexes
    public static final int TIME_MS = 0;
    public static final int INPUT_V = 1;
    public static final int MOTOR_TEMP = 6;
    public static final int ESC_TEMP = 2;
    public static final int DISTANCE = 34;
    public static final int SPEED_M = 33;
    public static final int BATTERY = 29;

    public static final int REFRESH_DELAY_MS = 2000;

    private boolean refreshing = false;
    private Handler handler = new Handler();

    private Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            readData();
            if (refreshing) {
                handler.postDelayed(refreshRunnable, REFRESH_DELAY_MS);
            }
        }
    };

    @Override
    public void onResume(){
        super.onResume();
        handler.postDelayed(refreshRunnable, 50);
    }

    @Override
    public void onPause(){
        super.onPause();
        handler.removeCallbacks(refreshRunnable);
    }



    private void readData() {
        readFile(uri);

        if (data.rows.isEmpty()) {
            tvSpeed.setText("Waiting for data");
            return;
        }

        DataPoint firstRow = data.rows.get(0);
        DataPoint lastRow = data.rows.get(data.rows.size() -1);

        for (int i = lastIndex; i < data.rows.size(); ++i) {
            DataPoint row = data.rows.get(i);
            row.computeAverageSpeed(firstRow.time);
            if (row.speed > maxSpeed) {
                maxSpeed = row.speed;
                maxSpeedMiles = row.speedMiles;
            }
        }

        float batteryStart = firstRow.batteryLevel;
        float batteryEnd = lastRow.batteryLevel;
        long elapsedSeconds = (lastRow.time - firstRow.time) / 1000;

        int hours = 0, minutes = 0, seconds = 0;

        if (hours >= 3600) {
            hours = (int) elapsedSeconds / 3600;
            elapsedSeconds = (int) elapsedSeconds % 3600;
        }
        if (elapsedSeconds >= 60) {
            minutes = (int) elapsedSeconds / 60;
            elapsedSeconds = (int) elapsedSeconds % 60;
        }
        seconds = (int) elapsedSeconds;

        tvSpeed.setText(maxSpeed + " km/h (" + maxSpeedMiles + " miles/h)");
        tvDistance.setText(lastRow.distance + " km (" + lastRow.distanceMiles + " miles)");
        tvElasped.setText((hours > 0 ? hours + "h " : "") + (minutes > 0 ? minutes + "mn " : "") + (seconds > 0 ? seconds + "s" : ""));
        tvBattery.setText("From " + batteryStart + "% to " + batteryEnd + "%");

        refreshGraphData();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_data);

        try {
            this.getSupportActionBar().hide();
        }
        catch (NullPointerException e) {}

        Bundle bundle = this.getIntent().getExtras();
        uri = Uri.parse((String) bundle.getSerializable("uri"));
        sampling = (int) bundle.getSerializable("sampling");

        tvSpeed = (TextView) findViewById(R.id.tvMaxSpeed);
        tvDistance = (TextView) findViewById(R.id.tvDistance);
        tvBattery = (TextView) findViewById(R.id.tvBatteryLoss);
        tvElasped = (TextView) findViewById(R.id.tvTime);
        switchRefresh = (Switch) findViewById(R.id.switchRefresh);

        graphSpeed = (GraphView) findViewById(R.id.graphSpeed);
        graphDistance = (GraphView) findViewById(R.id.graphDistance);
        graphTemp = (GraphView) findViewById(R.id.graphTemp);
        graphVolt = (GraphView) findViewById(R.id.graphVoltage);

        data = new VESCData();

        setupGraphs();

        switchRefresh.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    refreshing = true;
                    handler.postDelayed(refreshRunnable, REFRESH_DELAY_MS);
                } else {
                    refreshing = false;
                    handler.removeCallbacks(refreshRunnable);
                }
            }
        });

        readData();
    }

    private void refreshGraphData() {
        int size = data.rows.size();
        long nextTimestamp = data.rows.get(lastIndex).time;
        for (int i = lastIndex; i < data.rows.size(); ++i) {
            DataPoint row = data.rows.get(i);
            if (row.time >= nextTimestamp) {
                nextTimestamp = row.time + sampling;
                seriesSpeed.appendData(new com.jjoe64.graphview.series.DataPoint(row.time, row.speed), true, MAX_DATA_POINTS);
                seriesSpeedAvg.appendData(new com.jjoe64.graphview.series.DataPoint(row.time, row.averageSpeed), true, MAX_DATA_POINTS);
                seriesDistance.appendData(new com.jjoe64.graphview.series.DataPoint(row.time, row.distance), true, MAX_DATA_POINTS);
                seriesTemp.appendData(new com.jjoe64.graphview.series.DataPoint(row.time, row.motorTemp), true, MAX_DATA_POINTS);
                seriesTempEsc.appendData(new com.jjoe64.graphview.series.DataPoint(row.time, row.escTemp), true, MAX_DATA_POINTS);
                seriesVoltage.appendData(new com.jjoe64.graphview.series.DataPoint(row.time, row.inputVoltage), true, MAX_DATA_POINTS);
                seriesBattery.appendData(new com.jjoe64.graphview.series.DataPoint(row.time, row.batteryLevel), true, MAX_DATA_POINTS);
            }
        }
        lastIndex = size -1;


        graphSpeed.removeAllSeries();
        graphDistance.removeAllSeries();
        graphTemp.removeAllSeries();
        graphVolt.removeAllSeries();
        graphSpeed.addSeries(seriesSpeed);
        graphSpeed.addSeries(seriesSpeedAvg);
        graphDistance.addSeries(seriesDistance);
        graphTemp.addSeries(seriesTemp);
        graphTemp.addSeries(seriesTempEsc);
        graphVolt.addSeries(seriesVoltage);
        graphVolt.addSeries(seriesBattery);

        graphSpeed.getViewport().setMinX(data.rows.get(0).time);
        graphSpeed.getViewport().setMaxX(data.rows.get(size -1).time);
        graphDistance.getViewport().setMinX(data.rows.get(0).time);
        graphDistance.getViewport().setMaxX(data.rows.get(size -1).time);
        graphTemp.getViewport().setMinX(data.rows.get(0).time);
        graphTemp.getViewport().setMaxX(data.rows.get(size -1).time);
        graphVolt.getViewport().setMinX(data.rows.get(0).time);
        graphVolt.getViewport().setMaxX(data.rows.get(size -1).time);
    }

    private void setupGraphs() {
        // Create the time formatter for labels
        DefaultLabelFormatter timeSeriesFormatter = new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) {
                    Format formatter = new SimpleDateFormat("HH:mm:ss");
                    return formatter.format(value);
                }
                return super.formatLabel(value, isValueX);
            }
        };

        // Display time correctly
        fixLabels(graphSpeed, timeSeriesFormatter);
        fixLabels(graphDistance, timeSeriesFormatter);
        fixLabels(graphTemp, timeSeriesFormatter);
        fixLabels(graphVolt, timeSeriesFormatter);

        // Graph titles
        graphSpeed.setTitle("Speed (km/h)");
        graphDistance.setTitle("Distance (km)");
        graphTemp.setTitle("Temperatures");
        graphVolt.setTitle("Battery health");

        // Legend
        graphSpeed.getLegendRenderer().setVisible(true);
        graphSpeed.getLegendRenderer().setBackgroundColor(getResources().getColor(R.color.legend_bg));
        graphTemp.getLegendRenderer().setVisible(true);
        graphTemp.getLegendRenderer().setBackgroundColor(getResources().getColor(R.color.legend_bg));
        graphVolt.getLegendRenderer().setVisible(true);
        graphVolt.getLegendRenderer().setBackgroundColor(getResources().getColor(R.color.legend_bg));

        // Series names & color
        // - Temp
        seriesTempEsc.setColor(getResources().getColor(R.color.deep_orange));
        seriesTempEsc.setTitle("MOS");
        seriesTemp.setColor(getResources().getColor(R.color.orange));
        seriesTemp.setTitle("Motor");
        // - Battery
        seriesVoltage.setColor(getResources().getColor(R.color.orange));
        seriesVoltage.setTitle("Input voltage (V)");
        seriesBattery.setColor(getResources().getColor(R.color.green));
        seriesBattery.setTitle("Battery level (%)");
        // - Speed
        seriesSpeedAvg.setColor(getResources().getColor(R.color.orange));
        seriesSpeedAvg.setTitle("Average");
        seriesSpeed.setTitle("Instant");

        graphSpeed.addSeries(seriesSpeed);
        graphSpeed.addSeries(seriesSpeedAvg);
        graphDistance.addSeries(seriesDistance);
        graphTemp.addSeries(seriesTemp);
        graphTemp.addSeries(seriesTempEsc);
        graphVolt.addSeries(seriesVoltage);
        graphVolt.addSeries(seriesBattery);

        graphSpeed.getViewport().setScrollable(true);
        graphSpeed.getViewport().setScalable(true);
        graphDistance.getViewport().setScrollable(true);
        graphDistance.getViewport().setScalable(true);
        graphTemp.getViewport().setScrollable(true);
        graphTemp.getViewport().setScalable(true);
        graphVolt.getViewport().setScrollable(true);
        graphVolt.getViewport().setScalable(true);

    }

    private void fixLabels(GraphView gv, DefaultLabelFormatter formatter) {
        gv.getGridLabelRenderer().setLabelFormatter(formatter);
        gv.getGridLabelRenderer().setHorizontalLabelsAngle(90);
    }

    public void readFile(Uri uri) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri)));
            String line = null;
            int lineIndex = 0;
            while ((line = br.readLine()) != null) {
                if (lineIndex++ <= lastIndex) {
                    continue;
                }
                String[] tokens = line.split(";");

//                // Skip the header
//                if(!tokens[TIME_MS].matches("\\d+")) {
//                    continue;
//                }

                data.addRow(new DataPoint(Long.parseLong(tokens[TIME_MS]), Float.parseFloat(tokens[BATTERY]),
                        Float.parseFloat(tokens[MOTOR_TEMP]), Float.parseFloat(tokens[DISTANCE]),
                        Float.parseFloat(tokens[SPEED_M]), Float.parseFloat(tokens[INPUT_V]),
                        Float.parseFloat(tokens[ESC_TEMP])));
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error reading this file.", Toast.LENGTH_SHORT).show();
        }
    }
}