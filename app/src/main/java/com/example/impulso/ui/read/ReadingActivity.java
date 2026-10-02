package com.example.impulso.ui.read;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.impulso.R;
import com.example.impulso.ui.Themes;

public class ReadingActivity extends AppCompatActivity {
    public static final String EXTRA_TEXT = "text";
    public static final String EXTRA_AUTHOR = "author";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Themes.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reading);
        TextView quote = findViewById(R.id.reading_quote);
        TextView author = findViewById(R.id.reading_author);
        View controls = findViewById(R.id.reading_close);
        quote.setText(getIntent().getStringExtra(EXTRA_TEXT));
        author.setText(getIntent().getStringExtra(EXTRA_AUTHOR));
        controls.setVisibility(View.GONE);
        findViewById(R.id.reading_root).setOnClickListener(v ->
                controls.setVisibility(controls.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
        controls.setOnClickListener(v -> finish());
    }
}
