package com.revel.chat;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;

public class FragmentMainPage extends AppCompatActivity {
    TextView chat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment_main_page);

        Window window = this.getWindow();
        window.setStatusBarColor(Color.parseColor("#00574B"));

        chat = findViewById(R.id.chat);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame, new ContactFragment()).commit();

    }

}