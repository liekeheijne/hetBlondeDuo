package nl.lucmulder.watt.app;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import nl.lucmulder.watt.R;
import nl.lucmulder.watt.app.objects.Token;

/**
 * Created by lcm on 6-3-2017.
 */

public class RequestHelper {

    Context context;
    private static final String TAG = "RequestHelper";
//    private final ProgressDialog pDialog;

    public RequestHelper(Context context){
//        pDialog = new ProgressDialog(context);
        this.context = context;
    }

    public void doRequest(int RequestType, String subUrl, JSONObject json, RequestActionListenerInterface actionPerformed) {
        // Tag used to cancel the request
        String tag_json_obj = "json_obj_req";


//        pDialog.setMessage("Loading...");
//        pDialog.show();

        SharedPreferences settings = context.getSharedPreferences("TOKENS", 0);
        if(settings.getString("expires", null) == null){
            Log.d(TAG, "expires null");
            Intent intent = new Intent(context.getApplicationContext(), MainActivity.class);
            context.startActivity(intent);
        }else{
            long expiresMillis = Long.parseLong(settings.getString("expires", null));

            expiresMillis = expiresMillis - (5*60*1000); //subtract 5 minutes for ensurance;

            Date expires = new Date(expiresMillis);
            Log.d(TAG, "Expires: "+ expires.toString());
            Log.d(TAG, "Now: "+ new Date());
            if(expires.before(new Date())){ //tokens expired;
                Log.d(TAG, "Token expired, getting new one");
                getRefreshTokens(RequestType, subUrl, json, actionPerformed);
            }else{
                Log.d(TAG, "Straight to request");
                doInternalRequest(RequestType, subUrl, json, actionPerformed);
            }
        }
    }

    private void doInternalRequest(int RequestType, String subUrl, JSONObject json, RequestActionListenerInterface actionPerformed){
        String tag_json_obj = "json_obj_req";

        SharedPreferences settings = context.getSharedPreferences("TOKENS", 0);

        String user = settings.getString("user", null);
        String token = settings.getString("token", null);

        String url = "http://vps.lucmulder.nl:3000/" + subUrl + "?user=" + user + "&token=" + token;

        final RequestActionListenerInterface action = actionPerformed;
        final Context finalContext = context;

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(RequestType,
                url, json,
                new Response.Listener<JSONObject>() {

                    @Override
                        public void onResponse(JSONObject response) {
//                        pDialog.hide();
                        action.actionPerformed(response);
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
//                pDialog.hide();
                action.actionPerformed(null);
            }
        });

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(jsonObjReq, tag_json_obj);
    }

    private void getRefreshTokens(int RequestType, String subUrl, JSONObject json, RequestActionListenerInterface actionPerformed) {
        String tag_json_obj = "json_obj_req";

        String url = "http://vps.lucmulder.nl:3000/auth/token";


        JSONObject js = new JSONObject();
        try {
            SharedPreferences settings = context.getSharedPreferences("TOKENS", 0);
            String user = settings.getString("user", null);
            String requestToken = settings.getString("refresh_token", null);
            Log.d(TAG, "Refreshtoken: " + requestToken);
            js.put("username", user);
            js.put("refresh_token", requestToken);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        final int FinalRequestType = RequestType;
        final String finalSubUrl = subUrl;
        final JSONObject finalJson = json;
        final RequestActionListenerInterface finalActionPerformed = actionPerformed;

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
                url, js,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, "Response: " + response.toString());

                        String responseString = response.toString();
                        //create ObjectMapper instance
                        ObjectMapper objectMapper = new ObjectMapper();
//
//                            //convert json string to object
                        Token token = null;
                        try {
                            token = objectMapper.readValue(responseString, Token.class);
                            SharedPreferences settings = context.getSharedPreferences("TOKENS", 0);
                            SharedPreferences.Editor editor = settings.edit();
                            editor.putString("token", token.token);
                            editor.putString("refresh_token", token.refresh_token);
                            editor.putString("expires", token.expires);
                            editor.commit();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (token != null) {
                            doInternalRequest(FinalRequestType, finalSubUrl, finalJson, finalActionPerformed);
                        } else {
//                            pDialog.hide();
                            Intent intent = new Intent(context.getApplicationContext(), MainActivity.class);
                            context.startActivity(intent);
                        }
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Intent intent = new Intent(context.getApplicationContext(), MainActivity.class);
                context.startActivity(intent);
            }


        }) {
            /**
             * Passing some request headers
             * */
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/json; charset=utf-8");
                return headers;
            }
        };

// Adding request to request queue
        AppController.getInstance().addToRequestQueue(jsonObjReq, tag_json_obj);
    }
}
