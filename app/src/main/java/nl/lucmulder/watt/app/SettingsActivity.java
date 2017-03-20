package nl.lucmulder.watt.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import nl.lucmulder.watt.R;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        SharedPreferences settings = getSharedPreferences("TOKENS", 0);

        String user = settings.getString("user", null);

        TextView textView = (TextView)  findViewById(R.id.welcome);
        Typeface font = Typeface.createFromAsset(getAssets(), "Megrim.ttf");
        textView.setTypeface(font);
        textView.setText("Welkom " + user + "! \nWil je iets wijzigen?");

        Button logOutBUtton = (Button) findViewById(R.id.logout);
        logOutBUtton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logOut();
            }
        });

    }

    private void logOut(){
        SharedPreferences.Editor editor = getSharedPreferences("TOKENS", 0).edit();
        editor.remove("token");
        editor.remove("refresh_token");
        editor.commit();
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }
}
