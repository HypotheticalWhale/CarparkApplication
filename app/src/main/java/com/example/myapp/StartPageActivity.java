package com.example.myapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;

/**
 * This class implement the Start Page of the application
 */
public class StartPageActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    public void onClick(View view) {
        switch(view.getId())
        {
            case R.id.btnMainStart:
                Intent toHomepage = new Intent(this, HomepageActivity.class);
                startActivity(toHomepage);
                break;
            default:
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Remove title bar on top of screen
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        this.setContentView(R.layout.activity_startpage);

        // Start page "Start" button
        Button btnMainStart = findViewById(R.id.btnMainStart);
        btnMainStart.setOnClickListener(this);
    }
}