package com.example.hookapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void jump1(View view) {
        startActivity(new Intent(this,SecondActivity.class));
    }

    public void jumpLogin(View view) {
        startActivity(new Intent(this,LoginActivity.class));
    }
}
