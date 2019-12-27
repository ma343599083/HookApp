package cn.com.shopec.plugin;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class PluginMainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugin_main);
        ((TextView)findViewById(R.id.tv_content)).setText(getResources().getString(R.string.content));
        ((ImageView)findViewById(R.id.iv_content)).setImageResource(R.mipmap.ic_launcher);
        ((ImageView)findViewById(R.id.iv_content)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(PluginMainActivity.this,SecondActivity.class));
            }
        });
    }
}
