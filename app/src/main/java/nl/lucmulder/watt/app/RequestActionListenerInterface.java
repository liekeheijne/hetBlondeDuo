package nl.lucmulder.watt.app;

import com.android.volley.VolleyError;

import org.json.JSONObject;

/**
 * Created by lcm on 6-3-2017.
 */

public interface RequestActionListenerInterface {
    void actionPerformed(JSONObject response);
}
