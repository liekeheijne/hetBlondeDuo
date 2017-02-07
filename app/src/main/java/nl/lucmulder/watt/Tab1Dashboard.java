package nl.lucmulder.watt;

import android.graphics.Typeface;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by lcm on 7-2-2017.
 */

public class Tab1Dashboard extends Fragment{

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.tab1dashboard, container, false);

        TextView txt = (TextView) rootView.findViewById(R.id.section_label);
        Typeface font = Typeface.createFromAsset(getActivity().getAssets(), "Megrim.ttf");
        txt.setTypeface(font);
        return rootView;
    }
}
