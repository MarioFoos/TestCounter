package com.mlf.testcounter;

import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Color;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AutoSizeCounter textLife = findViewById(R.id.textViewLife);
        textLife.setValue(40);
        textLife.setBackgroundColor(Color.BLUE);
    }
}