package nl.lucmulder.watt.app;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import nl.lucmulder.watt.R;

/**
 * Created by lcm on 7-2-2017.
 */

public class Tab2Challenges extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.tab2challenges, container, false);
        return rootView;
    }
}
