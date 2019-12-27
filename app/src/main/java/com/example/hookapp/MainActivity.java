package com.example.hookapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
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
//系统   如何找到一个资源
        Intent intent = new Intent();
//        Class.forName    融合的过程
        intent.setComponent(new ComponentName("cn.com.shopec.plugin",
                "cn.com.shopec.plugin.PluginMainActivity"));
        startActivity(intent);
    }

    public void jumpLogin(View view) {
        startActivity(new Intent(this,LoginActivity.class));
    }
}
