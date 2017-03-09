package nl.lucmulder.watt.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.Request;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import nl.lucmulder.watt.R;
import nl.lucmulder.watt.app.objects.Usage;
import nl.lucmulder.watt.lib.CircularProgressBar;
import nl.lucmulder.watt.utils.ColorUtils;

public class MainActivity extends AppCompatActivity{

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private static final String TAG = "MainActivity";
    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    private Usage usage = null;
    private Timer timer = new Timer();
    private boolean timerRunning = true;

    private CircularProgressBar circularProgressBarPower = null;
    private CircularProgressBar circularProgressElectric = null;
    private CircularProgressBar circularProgressGas = null;

    private TextView powerView = null;
    private TextView gasView = null;
    private TextView electricView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences settings = getSharedPreferences("TOKENS", 0);
        if(settings.getString("token", null) == null){

            Log.d(TAG, "Going back to login");

            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            return;
        }else{
            Log.d(TAG, settings.getString("token", null));
            timerRunning = false;
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        TextView txt = (TextView) findViewById(R.id.app_name);
//        Typeface font = Typeface.createFromAsset(getAssets(), "Megrim.ttf");
//        txt.setTypeface(font);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
        tabLayout.getTabAt(0).setIcon(R.drawable.dashboard_icon);
        tabLayout.getTabAt(1).setIcon(R.drawable.medaltab);
        int tabIconColor = ContextCompat.getColor(getApplicationContext(), R.color.colorAccent);
        tabLayout.getTabAt(0).getIcon().setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN);
        tabIconColor = ContextCompat.getColor(getApplicationContext(), R.color.white);
        tabLayout.getTabAt(1).getIcon().setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN);


        tabLayout.setOnTabSelectedListener(
                new TabLayout.ViewPagerOnTabSelectedListener(mViewPager) {

                    @Override
                    public void onTabSelected(TabLayout.Tab tab) {
                        super.onTabSelected(tab);
                        int tabIconColor = ContextCompat.getColor(getApplicationContext(), R.color.colorAccent);
                        tab.getIcon().setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN);
                    }

                    @Override
                    public void onTabUnselected(TabLayout.Tab tab) {
                        super.onTabUnselected(tab);
                        int tabIconColor = ContextCompat.getColor(getApplicationContext(), R.color.white);
                        tab.getIcon().setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN);
                    }

                    @Override
                    public void onTabReselected(TabLayout.Tab tab) {
                        super.onTabReselected(tab);
                    }
                }
        );


        timerRunning = true;
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask(){
            @Override
            public void run(){
                Log.i(TAG, "Update every 10 seconds " + new Date().toString());
                getDataTest();
            }
        },100,5000);


    }


    @Override
    protected void onStop() {
        super.onStop();
//        timer.cancel();
        if(timer != null && timerRunning){
            timer.cancel();
            timerRunning = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
//        timer.cancel();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(!timerRunning){
            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask(){
                @Override
                public void run(){
                    Log.i(TAG, "Onstart update every 10 seconds"  + new Date().toString());
                    getDataTest();
                }
            },100,5000);
            timerRunning = true;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            switch (position){
                case 0:
                    return new Tab1Dashboard();
                case 1:
                    return new Tab2Challenges();
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            // Show 2 total pages.
            return 2;
        }
    }

    public void getDataTest(){

        RequestActionListenerInterface doRequest = new RequestActionListenerInterface() {
            @Override
            public void actionPerformed(JSONObject response) {
                if(response == null){
                    Log.d(TAG, "ERROR response = null");
                }else{
                    Log.d(TAG, "It all went well");

                    String responseString = response.toString();
                    //create ObjectMapper instance
                    ObjectMapper objectMapper = new ObjectMapper();
//
//                            //convert json string to object

                    try {
                        usage = objectMapper.readValue(responseString, Usage.class);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    circularProgressBarPower = (CircularProgressBar) findViewById(R.id.circularProgressPower);
                    circularProgressElectric = (CircularProgressBar) findViewById(R.id.circularProgressElectric);
                    circularProgressGas = (CircularProgressBar) findViewById(R.id.circularProgressGas);

                    powerView = (TextView) findViewById(R.id.powerView);
                    gasView = (TextView) findViewById(R.id.gasView);
                    electricView = (TextView) findViewById(R.id.electricView);


                    int currentWattage = Integer.parseInt(usage.huidig);
                    float currentElectric = Float.parseFloat(usage.sinceMorningElectr);
                    float currentGas = Float.parseFloat(usage.sinceMorningGas);
                    int max = Integer.parseInt(usage.maxToday);
                    Log.d(TAG, "First measurement " + usage.first.timestamp);

                    SimpleDateFormat format = new SimpleDateFormat(
                            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.GERMAN);
                    format.setTimeZone(TimeZone.getTimeZone("UTC"));

                    try {
                        Date firstDate = format.parse(usage.first.timestamp);

                        float electricNow = Float.parseFloat(usage.dal) + Float.parseFloat(usage.piek);
                        float electricThen = Float.parseFloat(usage.first.dal) + Float.parseFloat(usage.first.piek);

                        float gasNow = Float.parseFloat(usage.gas);
                        float gasThen = Float.parseFloat(usage.first.gas);

                        int days = (int)( (new Date().getTime() - firstDate.getTime()) / (1000 * 60 * 60 * 24));
                        float averageElectric = (electricNow - electricThen)/ days;
                        float averageGas = (gasNow - gasThen)/ days;
                        float percentageOfDay = ((float) new Date().getHours() / 24) + ((float) new Date().getMinutes() / (60 * 24));
                        Log.d(TAG, "percentageOfDay " + percentageOfDay);
                        float realisticAverageElectric = averageElectric * percentageOfDay;
                        float realisticAverageGas = averageGas * percentageOfDay;
                        Log.d(TAG, "averageGas " + averageGas);
                        Log.d(TAG, "realisticAverageGas " + realisticAverageGas);
                        float powerPercentage = (float) currentWattage/max*100;
                        float electricPercentage = currentElectric/realisticAverageElectric*50;
                        float gasPercentage = currentGas/realisticAverageGas*50;

                        if(powerPercentage > 100){
                            powerPercentage = 100;
                        }

                        if(electricPercentage > 100){
                            electricPercentage = 100;
                        }

                        if(gasPercentage > 100){
                            gasPercentage = 100;
                        }

                        if(circularProgressBarPower != null){
                            circularProgressBarPower.setProgressColor(ColorUtils.getColor(powerPercentage/100));
                            circularProgressBarPower.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.blue));
                            circularProgressBarPower.setProgress(Math.round(powerPercentage), currentWattage + " W");
                            circularProgressBarPower.setImage(R.drawable.flash);
                        }

                        if(circularProgressElectric != null){
                            circularProgressElectric.setProgressColor(ColorUtils.getColor(electricPercentage/100));
                            circularProgressElectric.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.blue));
                            circularProgressElectric.setProgress(Math.round(electricPercentage), ((float)Math.round(currentElectric*100)/100)+ " KW/h");
                            circularProgressElectric.setImage(R.drawable.battery);
                        }

                        if(circularProgressGas != null){
                            circularProgressGas.setProgressColor(ColorUtils.getColor(gasPercentage/100));
                            circularProgressGas.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.blue));
                            circularProgressGas.setProgress(Math.round(gasPercentage), ((float)Math.round(currentGas*100)/100)+ " M3");
                            circularProgressGas.setImage(R.drawable.flame);
                        }

                        Typeface font = Typeface.createFromAsset(getAssets(), "Megrim.ttf");

                        if(powerView != null){
                            powerView.setText(currentWattage + " W");
                            powerView.setTypeface(font);
                        }

                        if(gasView != null){
                            gasView.setText(Html.fromHtml(((float)Math.round(currentGas*100)/100)+ " M<sup>3</sup>"));
//                            gasView.setText(((float)Math.round(currentGas*100)/100)+ " M3");
                            gasView.setTypeface(font);
                        }

                        if(electricView != null){
                            electricView.setText(((float)Math.round(currentElectric*100)/100)+ " KW/h");
                            electricView.setTypeface(font);
                        }

                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        RequestHelper requestHelper = new RequestHelper(this);
        requestHelper.doRequest(Request.Method.GET, "all", null, doRequest);
    }
}
