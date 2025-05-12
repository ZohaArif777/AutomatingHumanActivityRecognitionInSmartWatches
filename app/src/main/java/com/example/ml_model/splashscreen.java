package com.example.ml_model;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class splashscreen extends AppCompatActivity {
    private static final int SPLASH_TIME_OUT = 3000; // 3 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splashscreen);

        // Start the main activity after the splash screen duration
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(splashscreen.this, MainActivity.class);
            startActivity(intent);
            finish(); // Close splash screen activity
        }, SPLASH_TIME_OUT);
    }
}
