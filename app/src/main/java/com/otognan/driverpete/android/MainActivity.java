package com.otognan.driverpete.android;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;


public class MainActivity extends Activity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private static final String LOG_TAG = "MainActivity";

    // Milliseconds per second
    private static final int MILLISECONDS_PER_SECOND = 1000;
    // Update frequency in seconds
    public static final int UPDATE_INTERVAL_IN_SECONDS = 5;
    // Update frequency in milliseconds
    private static final long UPDATE_INTERVAL =
            MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;
    // The fastest update frequency, in seconds
    private static final int FASTEST_INTERVAL_IN_SECONDS = 1;
    // A fast frequency ceiling in milliseconds
    private static final long FASTEST_INTERVAL =
            MILLISECONDS_PER_SECOND * FASTEST_INTERVAL_IN_SECONDS;


    private static final int LOGIN_ACTIVITY_RESULT_ID = 1;

    // Define an object that holds accuracy and frequency parameters
    private GoogleApiClient mGoogleApiClient;

    private List<String> data;

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        this.screenLog("GoogleApiClient connection has failed");
    }

    @Override
    public void onLocationChanged(Location location) {
        String message = String.format("%s %f %f\n",
                new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date()),
                location.getLatitude(),
                location.getLongitude());
        this.data.add(message);
        this.screenLog(message);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.data = new LinkedList<String>();

        setContentView(R.layout.activity_main);

        TextView textView = (TextView) findViewById(R.id.textView);
        textView.setMovementMethod(new ScrollingMovementMethod());

        this.updateLoginStatus();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        this.screenLog("Connecting to google api..");
        mGoogleApiClient.connect();
    }

    public void sendZipMessage(View view) {

        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("message/rfc822");
        i.putExtra(Intent.EXTRA_EMAIL, new String[]{"otognan@gmail.com"});
        i.putExtra(Intent.EXTRA_SUBJECT, "new Zip GPS");
        i.putExtra(Intent.EXTRA_TEXT, "");

        String full_data = "";
        for (String s : this.data)
        {
            full_data += s;
        }

        FileOutputStream outputStream;
        String textFileName = "gps.txt";

        try {
            outputStream = this.openFileOutput(textFileName, Context.MODE_WORLD_READABLE);
            outputStream.write(full_data.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        String[] arrayw = new String[1];
        arrayw[0] = getFileStreamPath(textFileName).getAbsolutePath();
        Compress compress = new Compress(arrayw, "gps.zip");
        compress.zip(this);

        Uri fileUri = Uri.fromFile(getFileStreamPath("gps.zip"));
        i.putExtra(Intent.EXTRA_STREAM, fileUri);


        try {
            startActivityForResult(Intent.createChooser(i, "Send zip mail..."), 0);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
        }
    }

    public void sendTextMessage(View view) {

        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("message/rfc822");
        i.putExtra(Intent.EXTRA_EMAIL  , new String[]{"otognan@gmail.com"});
        i.putExtra(Intent.EXTRA_SUBJECT, "new Text GPS");


        String full_data = "";
        for (String s : this.data)
        {
            full_data += s;
        }

        i.putExtra(Intent.EXTRA_TEXT, full_data);

        try {
            startActivity(Intent.createChooser(i, "Send text mail..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
        }
    }

    private void screenLog(String message) {
        TextView textView = (TextView) findViewById(R.id.textView);
        textView.append(message);
    }

    @Override
    public void onConnected(Bundle bundle) {
        this.screenLog("Location services connected");
        LocationRequest locationRequest;
        locationRequest = LocationRequest.create();
        // Use high accuracy
        locationRequest.setPriority(
                LocationRequest.PRIORITY_HIGH_ACCURACY);
        // Set the update interval to 5 seconds
        locationRequest.setInterval(UPDATE_INTERVAL);
        // Set the fastest update interval to 1 second
        locationRequest.setFastestInterval(FASTEST_INTERVAL);
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        this.screenLog("GoogleApiClient connection has been suspend");
    }


    public void logIn(View view) {
        final String serverUrl = "https://192.168.1.2:8443";
        //final String serverUrl = "https://testbeanstalkenv-taz59dxmiu.elasticbeanstalk.com";

        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("serverUrl", serverUrl);
        this.startActivityForResult(intent, LOGIN_ACTIVITY_RESULT_ID);
    }

    public void logOut(View view) {
        SharedPreferences settings = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.remove("token");
        editor.commit();
        updateLoginStatus();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == LOGIN_ACTIVITY_RESULT_ID) {
            if(resultCode == RESULT_OK){
                String token = data.getStringExtra("token");
                SharedPreferences settings = this.getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("token", token);
                editor.commit();
            }

            if (resultCode == RESULT_CANCELED) {
                Log.d(LOG_TAG, "No result from login activity");
            }

            updateLoginStatus();
        }
    }

    private String getCurrentToken() {
        SharedPreferences settings = this.getPreferences(Context.MODE_PRIVATE);
        return settings.getString("token", null);
    }

    private void updateLoginStatus() {
        TextView loginStatusTextView = (TextView) findViewById(R.id.loginStatusTextView);
        if (this.getCurrentToken() != null) {
            loginStatusTextView.setText("logged in.");
        } else {
            loginStatusTextView.setText("logged out.");
        }
    }
}
