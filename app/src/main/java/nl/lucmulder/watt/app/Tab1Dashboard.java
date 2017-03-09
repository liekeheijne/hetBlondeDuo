package nl.lucmulder.watt.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;
import java.util.Locale;
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


    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private View view;
    private FragmentActivity myContext;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.tab1dashboard, container, false);

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        myContext=(FragmentActivity) context;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.view = view;

        mSectionsPagerAdapter = new SectionsPagerAdapter(myContext.getSupportFragmentManager());
        mViewPager = (ViewPager) view.findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

//        ViewPager myPager = (ViewPager) view.findViewById(R.id.pager);
//        PagerAdapter adapter = new CircularPagerAdapter(myPager, new int[]{R.layout.fragment_dashboard_power, R.layout.fragment_dashboard_electricity, R.layout.fragment_dashboard_gas});
//        myPager.setAdapter(adapter);
//        myPager.setOnPageChangeListener(new CircularViewPagerHandler(myPager));
//        myPager.setCurrentItem(3);


    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a DummySectionFragment (defined as a static inner class
            // below) with the page number as its lone argument.
            Fragment fragment = null;
            if(position == 0){
                fragment = new dashboard_power();

            }
            if(position == 1){
                fragment = new dashboard_electricity();
            }
            if(position == 2){
                fragment = new dashboard_gas();
            }

            return fragment;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return ("Teste1").toUpperCase(l);
                case 1:
                    return ("Teste2").toUpperCase(l);
                case 2:
                    return ("Teste3").toUpperCase(l);
            }
            return null;
        }

    }

        @Override
    public void onStop() {
        super.onStop();

    }

    @Override
    public void onStart() {
        super.onStart();


    }


    }
