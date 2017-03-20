package nl.lucmulder.watt.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import android.view.View;
import android.widget.TextView;

import com.android.volley.Request;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.socket.client.IO;

import org.json.JSONObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import nl.lucmulder.watt.R;
import nl.lucmulder.watt.app.enums.Period;
import nl.lucmulder.watt.app.objects.Usage;
import nl.lucmulder.watt.lib.CircularProgressBar;
import nl.lucmulder.watt.utils.ColorUtils;

public class MainActivity extends AppCompatActivity {

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

    private int electricityPeriod = Period.DAY;
    private int gasPeriod = Period.DAY;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences settings = getSharedPreferences("TOKENS", 0);
        if (settings.getString("token", null) == null) {

            Log.d(TAG, "Going back to login");

            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            return;
        } else {
//            Log.d(TAG, settings.getString("token", null));
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

//                        Log.d(TAG, "" + tab.getPosition());

                        if (tab.getPosition() == 0) {
//                            startTimer();
                        }
                    }

                    @Override
                    public void onTabUnselected(TabLayout.Tab tab) {
                        super.onTabUnselected(tab);
                        int tabIconColor = ContextCompat.getColor(getApplicationContext(), R.color.white);
                        tab.getIcon().setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN);

                        if (tab.getPosition() == 0) {
                            stopTimer();
                        }
                    }

                    @Override
                    public void onTabReselected(TabLayout.Tab tab) {
                        super.onTabReselected(tab);
                    }
                }
        );
    }

    protected void startTimer() {
        if (!timerRunning) {
            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    Log.i(TAG, "Update every 5 seconds " + new Date().toString());
                    getData();
                }
            }, 100, 5000);
        }
        timerRunning = true;
    }

    protected void stopTimer() {
        if (timer != null && timerRunning) {
            timer.cancel();
            timerRunning = false;
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
//        timer.cancel();
        stopTimer();
    }

    @Override
    protected void onPause() {
        super.onPause();
//        timer.cancel();
        stopTimer();

    }

    @Override
    protected void onStart() {
        super.onStart();
//        startTimer();

        final Socket socket;
        try {
            socket = IO.socket("http://vps.lucmulder.nl:4000");

            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    socket.emit("join", 1);
                    Log.d(TAG, "connected");
//                    socket.disconnect();
                }

            }).on("needToUpdate", new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    Log.d(TAG, "Need to update");
                    getData();
                }

            }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    Log.d(TAG, "disconnected");
                }

            });
            socket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
//        startTimer();
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
            switch (position) {
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


    public void getData() {

        RequestActionListenerInterface doRequest = new RequestActionListenerInterface() {
            @Override
            public void actionPerformed(JSONObject response) {
                if (response == null) {
                    Log.d(TAG, "ERROR response = null");
                } else {
//                    Log.d(TAG, "It all went well");

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

                    initPowerView();
                    initViewElectric();
                    initViewGas();

                }
            }
        };


        RequestHelper requestHelper = new RequestHelper(this);
        requestHelper.doRequest(Request.Method.GET, "all", null, doRequest);
    }

    private float getAverageDayElectric(Usage usage) {
        SimpleDateFormat format = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.GERMAN);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        float averageElectricDay = 0;
        float realisticAverageElectricDay = 0;
        try {
            Date firstDate = format.parse(usage.first.timestamp);

            float electricNow = Float.parseFloat(usage.dal) + Float.parseFloat(usage.piek);
            float electricThen = Float.parseFloat(usage.first.dal) + Float.parseFloat(usage.first.piek);

            int days = (int) ((new Date().getTime() - firstDate.getTime()) / (1000 * 60 * 60 * 24));

            averageElectricDay = (electricNow - electricThen) / days;

            float percentageOfDay = ((float) new Date().getHours() / 24) + ((float) new Date().getMinutes() / (60 * 24));

            realisticAverageElectricDay = averageElectricDay * percentageOfDay;

        } catch (ParseException e) {
            e.printStackTrace();
        }

        return realisticAverageElectricDay;
    }

    private float getAverageWeekElectric(Usage usage) {
        SimpleDateFormat format = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.GERMAN);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        float averageElectricWeek = 0;
        float realisticAverageElectricWeek = 0;

        try {
            Date firstDate = format.parse(usage.first.timestamp);

            float electricNow = Float.parseFloat(usage.dal) + Float.parseFloat(usage.piek);
            float electricThen = Float.parseFloat(usage.first.dal) + Float.parseFloat(usage.first.piek);
//            Log.d(TAG, "electricNow " + electricNow);
//            Log.d(TAG, "electricT/hen " + electricThen);

            int days = (int) ((new Date().getTime() - firstDate.getTime()) / (1000 * 60 * 60 * 24));
//            Log.d(TAG, "days " + days);
            int weeks = Math.round((float) days / 7);
//            Log.d(TAG, "weeks " + weeks);

            averageElectricWeek = (electricNow - electricThen) / weeks;

            Calendar myDate = Calendar.getInstance(); // set this up however you need it.
            int dow = myDate.get(Calendar.DAY_OF_WEEK);
            dow = dow - 1;
            if (dow < 1) {
                dow = 7;
            }

            float percentageOfWeek = ((float) dow / 7);

//            Log.d(TAG, "dow " + dow);

            realisticAverageElectricWeek = averageElectricWeek * percentageOfWeek;

        } catch (ParseException e) {
            e.printStackTrace();
        }

        return realisticAverageElectricWeek;
    }

    private float getAverageMonthElectric(Usage usage) {
        SimpleDateFormat format = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.GERMAN);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        float averageElectricMonth = 0;
        float realisticAverageElectricMonth = 0;
        try {
            Date firstDate = format.parse(usage.first.timestamp);

            float electricNow = Float.parseFloat(usage.dal) + Float.parseFloat(usage.piek);
            float electricThen = Float.parseFloat(usage.first.dal) + Float.parseFloat(usage.first.piek);

            int days = (int) ((new Date().getTime() - firstDate.getTime()) / (1000 * 60 * 60 * 24));

            Calendar c = Calendar.getInstance();
            int daysOfCurrentMonth = c.getActualMaximum(Calendar.DAY_OF_MONTH);
            int months = Math.round(days / 30);

            averageElectricMonth = (electricNow - electricThen) / months;

            float percentageOfMonth = (float) c.get(Calendar.DAY_OF_MONTH) / daysOfCurrentMonth;
//            Log.d(TAG, "Calendar.DAY_OF_MONTH " + c.get(Calendar.DAY_OF_MONTH));
//            Log.d(TAG, "daysOfCurrentMonth " + daysOfCurrentMonth);
//            Log.d(TAG, "percentageOfMonth " + percentageOfMonth);
            realisticAverageElectricMonth = averageElectricMonth * percentageOfMonth;

//            Log.d(TAG, "averageElectricMonth " + averageElectricMonth);
//            Log.d(TAG, "realisticAverageElectricMonth " + realisticAverageElectricMonth);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return realisticAverageElectricMonth;
    }

    private float getAverageDayGas(Usage usage) {
        SimpleDateFormat format = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.GERMAN);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        float averageGasDay = 0;
        float realisticAverageGasDay = 0;


        try {
            Date firstDate = format.parse(usage.first.timestamp);

            float gasNow = Float.parseFloat(usage.gas);
            float gasThen = Float.parseFloat(usage.first.gas);

            int days = (int) ((new Date().getTime() - firstDate.getTime()) / (1000 * 60 * 60 * 24));
            averageGasDay = (gasNow - gasThen) / days;

            float percentageOfDay = ((float) new Date().getHours() / 24) + ((float) new Date().getMinutes() / (60 * 24));

            realisticAverageGasDay = averageGasDay * percentageOfDay;

        } catch (ParseException e) {
            e.printStackTrace();
        }

        return realisticAverageGasDay;
    }

    private float getAverageWeekGas(Usage usage) {
        SimpleDateFormat format = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.GERMAN);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        float averageGasWeek = 0;
        float realisticAverageGasWeek = 0;
        try {
            Date firstDate = format.parse(usage.first.timestamp);

            float gasNow = Float.parseFloat(usage.gas);
            float gasThen = Float.parseFloat(usage.first.gas);

            int days = (int) ((new Date().getTime() - firstDate.getTime()) / (1000 * 60 * 60 * 24));
            int weeks = (int) days / 7;
            averageGasWeek = (gasNow - gasThen) / weeks;

            Calendar myDate = Calendar.getInstance(); // set this up however you need it.
            int dow = myDate.get(Calendar.DAY_OF_WEEK);
            dow = dow - 1;
            if (dow < 1) {
                dow = 7;
            }

            float percentageOfWeek = ((float) dow / 7);

            realisticAverageGasWeek = averageGasWeek * percentageOfWeek;

        } catch (ParseException e) {
            e.printStackTrace();
        }

        return realisticAverageGasWeek;
    }

    private float getAverageMonthGas(Usage usage) {
        SimpleDateFormat format = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.GERMAN);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        float averageGasMonth = 0;
        float realisticAverageGasMonth = 0;
        try {
            Date firstDate = format.parse(usage.first.timestamp);

            float gasNow = Float.parseFloat(usage.gas);
            float gasThen = Float.parseFloat(usage.first.gas);

            int days = (int) ((new Date().getTime() - firstDate.getTime()) / (1000 * 60 * 60 * 24));

            Calendar c = Calendar.getInstance();
            int daysOfCurrentMonth = c.getActualMaximum(Calendar.DAY_OF_MONTH);
            int months = Math.round(days / daysOfCurrentMonth);

            averageGasMonth = (gasNow - gasThen) / months;

            float percentageOfMonth = (float) c.get(Calendar.DAY_OF_MONTH) / daysOfCurrentMonth;

            realisticAverageGasMonth = averageGasMonth * percentageOfMonth;

        } catch (ParseException e) {
            e.printStackTrace();
        }

        return realisticAverageGasMonth;
    }

    private int getPercentageDayElectric(Usage usage) {

        float currentElectric = Float.parseFloat(usage.sinceMorning.electric);
        int electricPercentage = Math.round(currentElectric / getAverageDayElectric(usage) * 50);

        if (electricPercentage > 100) {
            electricPercentage = 100;
        }

        return electricPercentage;
    }

    private int getPercentageWeekElectric(Usage usage) {

        float currentElectric = Float.parseFloat(usage.thisWeek.electric);
        int electricPercentage = Math.round(currentElectric / getAverageWeekElectric(usage) * 50);
        Log.d(TAG, "" + currentElectric);
        Log.d(TAG, "" + getAverageWeekElectric(usage));
        if (electricPercentage > 100) {
            electricPercentage = 100;
        }

        return electricPercentage;
    }

    private int getPercentageMonthElectric(Usage usage) {

        float currentElectric = Float.parseFloat(usage.thisMonth.electric);
        int electricPercentage = Math.round(currentElectric / getAverageMonthElectric(usage) * 50);

        if (electricPercentage > 100) {
            electricPercentage = 100;
        }

        return electricPercentage;
    }

    private int getPercentageDayGas(Usage usage) {

        float currentGas = Float.parseFloat(usage.sinceMorning.gas);
        int gasPercentage = Math.round(currentGas / getAverageDayGas(usage) * 50);
        if (gasPercentage > 100) {
            gasPercentage = 100;
        }
        return gasPercentage;
    }

    private int getPercentageWeekGas(Usage usage) {

        float currentGas = Float.parseFloat(usage.thisWeek.gas);
        int gasPercentage = Math.round(currentGas / getAverageWeekGas(usage) * 50);
        if (gasPercentage > 100) {
            gasPercentage = 100;
        }
        return gasPercentage;
    }

    private int getPercentageMonthGas(Usage usage) {

        float currentGas = Float.parseFloat(usage.thisMonth.gas);
        int gasPercentage = Math.round(currentGas / getAverageMonthGas(usage) * 50);
        if (gasPercentage > 100) {
            gasPercentage = 100;
        }
        return gasPercentage;
    }

    private String getTextElectricDay(Usage usage) {
        return ((float) Math.round(Float.parseFloat(usage.sinceMorning.electric) * 100) / 100) + " KW/h";
    }

    private String getTextElectricWeek(Usage usage) {
        return ((float) Math.round(Float.parseFloat(usage.thisWeek.electric) * 100) / 100) + " KW/h";
    }

    private String getTextElectricMonth(Usage usage) {
        return ((float) Math.round(Float.parseFloat(usage.thisMonth.electric) * 100) / 100) + " KW/h";
    }

    private Spanned getTextGasDay(Usage usage) {
        return Html.fromHtml(((float) Math.round(Float.parseFloat(usage.sinceMorning.gas) * 100) / 100) + " M<sup>3</sup>");
    }

    private Spanned getTextGasWeek(Usage usage) {
        return Html.fromHtml(((float) Math.round(Float.parseFloat(usage.thisWeek.gas) * 100) / 100) + " M<sup>3</sup>");
    }

    private Spanned getTextGasMonth(Usage usage) {
        return Html.fromHtml(((float) Math.round(Float.parseFloat(usage.thisMonth.gas) * 100) / 100) + " M<sup>3</sup>");
    }

    private void initPowerView() {

        circularProgressBarPower = (CircularProgressBar) findViewById(R.id.circularProgressPower);
        powerView = (TextView) findViewById(R.id.powerView);

        int currentWattage = Integer.parseInt(usage.huidig);
        int max = Integer.parseInt(usage.maxToday);

        float powerPercentage = (float) currentWattage / max * 100;

        if (powerPercentage > 100) {
            powerPercentage = 100;
        }

        if (circularProgressBarPower != null) {
            circularProgressBarPower.setProgressColor(ColorUtils.getColor(powerPercentage / 100));
            circularProgressBarPower.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.blue));
            circularProgressBarPower.setProgress(Math.round(powerPercentage));
            circularProgressBarPower.setImage(R.drawable.flash);
            circularProgressBarPower.setImageOffset(0, 33);
        }

        Typeface font = Typeface.createFromAsset(getAssets(), "Megrim.ttf");

        if (powerView != null) {
            powerView.setText(currentWattage + " W");
            powerView.setTypeface(font);
        }
    }

    private void initViewElectric() {

        circularProgressElectric = (CircularProgressBar) findViewById(R.id.circularProgressElectric);
        electricView = (TextView) findViewById(R.id.electricView);

        if (circularProgressElectric != null) {
            circularProgressElectric.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
//                    Log.d(TAG, "Electric graph clicked");
                    electricityPeriod++;
                    if (electricityPeriod > 2) {
                        electricityPeriod = 0;
                    }
//                    Log.d(TAG, "electricityPeriod " + electricityPeriod);
//                    Log.d(TAG, "Period.DAY " + Period.DAY);

                    updateViewELectric();

                }
            });

            circularProgressElectric.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.blue));
            updateViewELectric();
            circularProgressElectric.setImage(R.drawable.battery);
        }

        Typeface font = Typeface.createFromAsset(getAssets(), "Megrim.ttf");
        if (electricView != null) {
            electricView.setTypeface(font);
        }
    }

    private void updateViewELectric() {
        switch (electricityPeriod) {
            case Period.DAY:
                circularProgressElectric.setText("Dag");
                circularProgressElectric.setProgressColor(ColorUtils.getColor((float) getPercentageDayElectric(usage) / 100));
                circularProgressElectric.setProgress(getPercentageDayElectric(usage));
                electricView.setText(getTextElectricDay(usage));
                break;
            case Period.WEEK:
                circularProgressElectric.setText("Week");
                circularProgressElectric.setProgressColor(ColorUtils.getColor((float) getPercentageWeekElectric(usage) / 100));
                circularProgressElectric.setProgress(getPercentageWeekElectric(usage));
                electricView.setText(getTextElectricWeek(usage));
                break;
            case Period.MONTH:
                circularProgressElectric.setText("Maand");
                circularProgressElectric.setProgressColor(ColorUtils.getColor((float) getPercentageMonthElectric(usage) / 100));
                circularProgressElectric.setProgress(getPercentageMonthElectric(usage));
                electricView.setText(getTextElectricMonth(usage));
                break;
        }
    }

    private void initViewGas() {

        circularProgressGas = (CircularProgressBar) findViewById(R.id.circularProgressGas);
        gasView = (TextView) findViewById(R.id.gasView);

        if (circularProgressGas != null) {
            circularProgressGas.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
//                    Log.d(TAG, "Gas graph clicked");
                    gasPeriod++;
                    if (gasPeriod > 2) {
                        gasPeriod = 0;
                    }
                    updateViewGas();
                }
            });

            circularProgressGas.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.blue));
            updateViewGas();

            circularProgressGas.setImage(R.drawable.flame);
        }

        Typeface font = Typeface.createFromAsset(getAssets(), "Megrim.ttf");
        if (gasView != null) {
            gasView.setTypeface(font);
        }
    }

    private void updateViewGas() {
        switch (gasPeriod) {
            case Period.DAY:
                circularProgressGas.setText("Dag");
                circularProgressGas.setProgressColor(ColorUtils.getColor((float) getPercentageDayGas(usage) / 100));
                circularProgressGas.setProgress(getPercentageDayGas(usage));
                gasView.setText(getTextGasDay(usage));

                break;
            case Period.WEEK:
                circularProgressGas.setText("Week");
                circularProgressGas.setProgressColor(ColorUtils.getColor((float) getPercentageWeekGas(usage) / 100));
                circularProgressGas.setProgress(getPercentageWeekGas(usage));
                gasView.setText(getTextGasWeek(usage));
                break;
            case Period.MONTH:
                circularProgressGas.setText("Maand");
                circularProgressGas.setProgressColor(ColorUtils.getColor((float) getPercentageMonthGas(usage) / 100));
                circularProgressGas.setProgress(getPercentageMonthGas(usage));
                gasView.setText(getTextGasMonth(usage));
                break;
        }

    }

}
