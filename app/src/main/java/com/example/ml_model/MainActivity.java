package com.example.ml_model;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.speech.RecognizerIntent;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatActivity;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private Interpreter tflite;
    private ProgressBar progressBar;
    private float[][][] inputData;
    private Button btnPredict;
    private Switch btnToggleHighContrast;
    private Switch switchLargerText;
    private boolean isHighContrastEnabled = false;
    private Button btnVoiceCommand;
    private Button btnSelectFile;
    private TextView resultTextView;

    private static final int REQ_CODE_SPEECH_INPUT = 100;
    private static final int REQ_CODE_PICK_CSV = 101;

    private String[] classLabels = {
            " standing",
            "sitting",
            "walking-left",
            "sleeping",
            "elevator-up",
            "sitting",
            "run",
            "sleeping",
            "walking-right",
            "walking-downstairs",
            "walking-left",
            "jump",
            "sitting",
            "walking-left",
            "sleeping",
            "jump",
            "sleeping",
            "walking-forward",
            "walking-upstairs",
            "walking-downstairs",
            "walking-left",
            "walking-upstairs",
            "running",
            "standing"

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Load preferences before applying the theme
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        isHighContrastEnabled = preferences.getBoolean("high_contrast_option", false);
        applyTheme(isHighContrastEnabled);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the UI components
        btnPredict = findViewById(R.id.btnPredict);
        progressBar = findViewById(R.id.progressBar);
        btnVoiceCommand = findViewById(R.id.btnVoiceCommand);
        btnToggleHighContrast = findViewById(R.id.btnToggleHighContrast);
        switchLargerText = findViewById(R.id.switchLargerText);
        btnSelectFile = findViewById(R.id.btnSelectFile);
        resultTextView = findViewById(R.id.resultTextView);

        switchLargerText.setChecked(preferences.getBoolean("large_text_option", false));
        btnToggleHighContrast.setChecked(preferences.getBoolean("high_contrast_option", false));

        switchLargerText.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("large_text_option", isChecked);
            editor.apply();
            adjustTextSize(isChecked);
        });

        btnToggleHighContrast.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("high_contrast_option", isChecked);
            editor.apply();
            applyTheme(isChecked);
            recreate(); // Restart the activity to apply the theme change
        });

        try {
            tflite = new Interpreter(loadModelFile(), new Interpreter.Options());
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading model", Toast.LENGTH_LONG).show();
        }

        btnPredict.setOnClickListener(v -> performPrediction());
        btnVoiceCommand.setOnClickListener(v -> promptSpeechInput());
        btnSelectFile.setOnClickListener(v -> selectCSVFile());
    }

    private MappedByteBuffer loadModelFile() throws IOException {

        AssetFileDescriptor fileDescriptor = getAssets().openFd("har_model.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.getStartOffset(), fileDescriptor.getDeclaredLength());
    }

    private void performPrediction() {
        progressBar.setVisibility(View.VISIBLE);

        if (inputData == null) {
            showError("No input data available. Please select a CSV file.");
            progressBar.setVisibility(View.GONE);
            return;
        }

        float[][] outputData = new float[1][classLabels.length];

        // Debugging input data shape and content
        Log.d("MainActivity", "InputData shape: " + Arrays.deepToString(inputData));

        new Thread(() -> {
            try {
                tflite.run(inputData, outputData);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    showResults(outputData);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    showError("Error during prediction: " + e.getMessage());
                    Log.e("MainActivity", "Error during prediction", e);
                });
            }
        }).start();
    }

    private void fillInputDataFromCSV(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            List<String> lines = new ArrayList<>();
            String line;
            int rowIndex = 0;
            inputData = new float[1][100][6];


            String header = reader.readLine();

            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }

            Log.d("MainActivity", "Entire CSV Content:\n" + String.join("\n", lines));

            // Process each line
            for (String csvLine : lines) {
                if (rowIndex >= 100) {
                    break;
                }

                String[] values = csvLine.split(",");
                Log.d("MainActivity", "CSV Line " + rowIndex + ": " + Arrays.toString(values));

                if (values.length == 6) {
                    try {
                        for (int i = 0; i < values.length; i++) {
                            inputData[0][rowIndex][i] = Float.parseFloat(values[i]);
                        }
                        rowIndex++;
                    } catch (NumberFormatException e) {
                        Log.e("MainActivity", "Error parsing CSV line: " + csvLine, e);
                    }
                }
            }
        } catch (IOException e) {
            showError("Error reading CSV file: " + e.getMessage());
            Log.e("MainActivity", "Error reading CSV file: ", e);
        }
    }


    private void selectCSVFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("text/csv");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select CSV File"), REQ_CODE_PICK_CSV);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_CODE_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                String command = result.get(0);
                processVoiceCommand(command);
            }
        } else if (requestCode == REQ_CODE_PICK_CSV && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                if (inputStream != null) {
                    fillInputDataFromCSV(inputStream);
                    Toast.makeText(this, "File loaded successfully. Now click 'Predict' to proceed.", Toast.LENGTH_SHORT).show();
                } else {
                    showError("Error opening file.");
                }
            } catch (IOException e) {
                showError("Error reading file: " + e.getMessage());
            }
        }
    }

    private void showResults(float[][] outputData) {
        float[] probabilities = softmax(outputData[0]);

        int maxIndex = 0;
        float maxProbability = probabilities[0];
        for (int i = 1; i < probabilities.length; i++) {
            if (probabilities[i] > maxProbability) {
                maxProbability = probabilities[i];
                maxIndex = i;
            }
        }
        int percentage = (int) (maxProbability * 1000);

        String result = "Prediction Result:\n" +
                classLabels[maxIndex] + ": " +
                percentage + "%";
        float[] highestClassData = new float[probabilities.length];
        highestClassData[maxIndex] = probabilities[maxIndex];
        Intent intent = new Intent(MainActivity.this, resultactivity.class);
        intent.putExtra("result", result);
        intent.putExtra("chartData", highestClassData);
        startActivity(intent);
    }


    private float[] softmax(float[] logits) {
        float maxLogit = Float.NEGATIVE_INFINITY;
        for (float logit : logits) {
            if (logit > maxLogit) {
                maxLogit = logit;
            }
        }

        float sum = 0.0f;
        float[] softmaxValues = new float[logits.length];
        for (int i = 0; i < logits.length; i++) {
            softmaxValues[i] = (float) Math.exp(logits[i] - maxLogit);
            sum += softmaxValues[i];
        }

        for (int i = 0; i < softmaxValues.length; i++) {
            softmaxValues[i] /= sum;
        }

        return softmaxValues;
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.e("MainActivity", message);
    }

    private void applyTheme(boolean isHighContrastEnabled) {
        if (isHighContrastEnabled) {
            setTheme(R.style.AppTheme_HighContrast);
        } else {
            setTheme(R.style.AppTheme);
        }
    }

    private void adjustTextSize(boolean isLargeText) {
        float textSize = isLargeText ? 24f : 14f;
        btnPredict.setTextSize(textSize);
        btnVoiceCommand.setTextSize(textSize);
        btnToggleHighContrast.setTextSize(textSize);
        switchLargerText.setTextSize(textSize);
        btnSelectFile.setTextSize(textSize);
    }

    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your command");

        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Sorry, your device doesn't support speech input", Toast.LENGTH_SHORT).show();
        }
    }

    private void processVoiceCommand(String command) {
        // Implement your voice command processing logic here
        Toast.makeText(this, "Voice Command: " + command, Toast.LENGTH_SHORT).show();
    }
}