package com.example.ml_model;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.Arrays;

public class resultactivity extends AppCompatActivity {

    private TextView outputResult;
    private LineChart chart;
    private Button btnViewGraph, btnBack;
    private ProgressBar progressBar;
    private float[] chartData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resultactivity);

        // Initialize UI components
        outputResult = findViewById(R.id.outputResult);
        chart = findViewById(R.id.chart);
        btnViewGraph = findViewById(R.id.btnViewGraph);
        btnBack = findViewById(R.id.btnBack);
        progressBar = findViewById(R.id.progressBar);

        // Get data from intent
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String result = extras.getString("result");
            chartData = extras.getFloatArray("chartData");

            // Display the result
            outputResult.setText(result);

            // Log data for debugging
            Log.d("resultactivity", "Result: " + result);
            Log.d("resultactivity", "Chart Data: " + Arrays.toString(chartData));

            // Display the snackbar message with the result
            Snackbar.make(findViewById(android.R.id.content), "Prediction completed: " + result, Snackbar.LENGTH_LONG).show();

            // Display the chart if there's data
            if (chartData != null && chartData.length > 0) {
                btnViewGraph.setOnClickListener(v -> displayChart());
            } else {
                btnViewGraph.setEnabled(false);
            }
        } else {
            Snackbar.make(findViewById(android.R.id.content), "No data available.", Snackbar.LENGTH_LONG).show();
            btnViewGraph.setEnabled(false);
        }

        btnBack.setOnClickListener(v -> onBackPressed());
    }


    private void displayChart() {
        progressBar.setVisibility(View.VISIBLE);  // Show progress bar

        String resultText = outputResult.getText().toString();
        int percentageValue;
        try {
            String[] parts = resultText.split("\\s+");
            String percentagePart = parts[parts.length - 1].replace("%", "");
            percentageValue = Math.round(Float.parseFloat(percentagePart)); // Convert to integer
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            percentageValue = 0;
        }

        // Create data points for the value
        ArrayList<Entry> entries = new ArrayList<>();
        entries.add(new Entry(0, 0));
        entries.add(new Entry(1, percentageValue));

        LineDataSet dataSet = new LineDataSet(entries, "Class Probability");
        dataSet.setColor(ContextCompat.getColor(this, android.R.color.holo_blue_light));
        dataSet.setValueTextColor(ContextCompat.getColor(this, R.color.black));
        dataSet.setDrawCircles(true);
        dataSet.setDrawFilled(true);
        dataSet.setDrawValues(true);
        dataSet.setValueTextColor(ContextCompat.getColor(this, R.color.black));
        dataSet.setValueTextSize(10f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getPointLabel(Entry entry) {
                return String.valueOf((int) entry.getY());
            }
        });

        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(dataSet);
        LineData lineData = new LineData(dataSets);
        chart.setData(lineData);

        // Configure X and Y axis
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(2, true);
        xAxis.setLabelRotationAngle(45f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{"Start", "Result"}));

        YAxis yAxisLeft = chart.getAxisLeft();
        yAxisLeft.setAxisMinimum(0f);
        yAxisLeft.setAxisMaximum(100f);
        yAxisLeft.setGranularity(10f);

        YAxis yAxisRight = chart.getAxisRight();
        yAxisRight.setEnabled(false);

        chart.invalidate();

        progressBar.setVisibility(View.GONE);
        chart.setVisibility(View.VISIBLE);
    }


    private String getClassLabel(int index) {
        String[] allLabels = new String[]{
                "standing", "sitting", "walking-left", "sleeping", "elevator-up", "sitting",
                "run", "sleeping", "walking-right", "walking-downstairs", "walking-left",
                "jump", "sitting", "walking-left", "sleeping", "jump", "sleeping",
                "walking-forward", "walking-upstairs", "walking-downstairs", "walking-left",
                "walking-upstairs", "running", "standing"
        };

        return allLabels[index];
    }
}
