package nl.lucmulder.watt.app;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import nl.lucmulder.watt.R;
import nl.lucmulder.watt.app.objects.Usage;
import nl.lucmulder.watt.lib.CircularProgressBar;

/**
 * Created by lcm on 7-2-2017.
 */

public class Tab1Dashboard extends Fragment{

    private final String TAG = "Tab1Dashboard";
    private Usage usage = null;
    private View view = null;
    private Timer timer = new Timer();

    private boolean timerRunning = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.tab1dashboard, container, false);

        return rootView;
    }



    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        this.view = view;
        timerRunning = true;
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask(){
            @Override
            public void run(){
                Log.i(TAG, "Update every 10 seconds");
                getDataTest();
            }
        },100,10000);
    }

    @Override
    public void onStop() {
        super.onStop();
        if(timer != null && timerRunning){
            timer.cancel();
            timerRunning = false;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if(!timerRunning){
            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask(){
                @Override
                public void run(){
                    Log.i(TAG, "Update every 10 seconds");
                    getDataTest();
                }
            },100,10000);
            timerRunning = true;
        }

    }

    private void getDataTest(){

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


                    CircularProgressBar circularProgressBar = (CircularProgressBar) view.findViewById(R.id.circularProgress);
//                    CircularProgressBar circularProgressBar2 = (CircularProgressBar) view.findViewById(R.id.circularProgress2);

                    int currentWattage = Integer.parseInt(usage.huidig);
                    int max = Integer.parseInt(usage.maxToday);
//                    int max = 700;

                    float percentage = (float) currentWattage/max*100;
                    circularProgressBar.setProgressColor(R.color.blue);
                    circularProgressBar.setTextColor(R.color.blue);
                    circularProgressBar.setProgress(Math.round(percentage), currentWattage+ " W");

//                    circularProgressBar2.setProgressColor(R.color.blue);
//                    circularProgressBar2.setTextColor(R.color.blue);
//                    circularProgressBar2.setProgress(Math.round(percentage), currentWattage+ " W");
                }
            }
        };

        RequestHelper requestHelper = new RequestHelper(getActivity());
        requestHelper.doRequest(Request.Method.GET, "all", null, doRequest);
    }
}
