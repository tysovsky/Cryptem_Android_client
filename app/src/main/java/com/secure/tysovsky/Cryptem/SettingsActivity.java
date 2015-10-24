package com.secure.tysovsky.Cryptem;

import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

public class SettingsActivity extends AppCompatActivity {

    CheckBox decryptNotifications;
    SharedPreferences preferences;
    TextView username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.primary)));

        preferences = getSharedPreferences("CryptemSP", MODE_PRIVATE);

        username = (TextView)findViewById(R.id.settings_username);
        username.setText(getIntent().getExtras().getString("username"));

        decryptNotifications = (CheckBox)findViewById(R.id.checkbox_encrypt_notifications);
        decryptNotifications.setOnCheckedChangeListener(decryptNotificationsChaged);

    }


    CompoundButton.OnCheckedChangeListener decryptNotificationsChaged = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                preferences.edit().putBoolean("decrypt_notification", checked).commit();

        }
    };

}
