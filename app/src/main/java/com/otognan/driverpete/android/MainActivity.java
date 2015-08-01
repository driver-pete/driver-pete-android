package com.otognan.driverpete.android;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.NoSuchElementException;

import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.OkClient;
import retrofit.client.Response;
import retrofit.mime.TypedByteArray;
import retrofit.mime.TypedInput;


public class MainActivity extends Activity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private static final String LOG_TAG = "MainActivity";
    //private static final String serverUrl = "https://192.168.1.2:8443";
    private static final String serverUrl = "https://testbeanstalkenv-taz59dxmiu.elasticbeanstalk.com";

    private static final double locationDistanceThreshold = 10.;

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
    private static final int EMAIL_ACTIVITY_RESULT_ID = 2;

    // Define an object that holds accuracy and frequency parameters
    private GoogleApiClient mGoogleApiClient;

    private Trajectory currentTrajectory = new Trajectory();


    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        this.screenLog("GoogleApiClient connection has failed. Setting up fake messages..");

        final Handler timerHandler = new Handler();
        Runnable timerRunnable = new Runnable() {
            @Override
            public void run() {
                Location location = new Location("Google");
                location.setTime(System.currentTimeMillis());
                location.setLatitude(34.34);
                location.setLongitude(-117.02);
                MainActivity.this.onLocationChanged(location);
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.postDelayed(timerRunnable, 0);
    }

    @Override
    public void onLocationChanged(Location location) {
        try {
            Location lastLocaton = this.currentTrajectory.lastLocation();
            if (lastLocaton.distanceTo(location) > locationDistanceThreshold) {
                this.currentTrajectory.addLocation(location);
                this.screenLog(Trajectory.locationToShortString(location) + "\n");
            } else {
                Log.d(LOG_TAG,Trajectory.locationToShortString(location) + " is ignored");
            }
        } catch (NoSuchElementException ex){
            this.currentTrajectory.addLocation(location);
            this.screenLog(Trajectory.locationToShortString(location) + "\n");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        TextView textView = (TextView) findViewById(R.id.textView);
        textView.setMovementMethod(new ScrollingMovementMethod());

        this.updateLoginStatus();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        this.screenLog("Connecting to google api..\n");
        mGoogleApiClient.connect();
    }

    public void sendZipMessage(View view) {

        String filename = "gps.myfile";
        try {
            FileOutputStream dest = this.openFileOutput(filename, Context.MODE_WORLD_READABLE);
            dest.write(this.currentTrajectory.compress());
            dest.close();
        } catch (Exception ex) {
            Log.e(LOG_TAG, "Failed to compress file trajectory");
            return;
        }

        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("message/rfc822");
        i.putExtra(Intent.EXTRA_EMAIL, new String[]{"otognan@gmail.com"});
        i.putExtra(Intent.EXTRA_SUBJECT, "new Zip GPS");
        i.putExtra(Intent.EXTRA_TEXT, "");

        Uri fileUri = Uri.fromFile(getFileStreamPath(filename));
        i.putExtra(Intent.EXTRA_STREAM, fileUri);

        try {
            startActivityForResult(Intent.createChooser(i, "Send zip mail..."), EMAIL_ACTIVITY_RESULT_ID);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
        }
    }

    public void sendToServer(View view) throws Exception {
        byte[] content = this.currentTrajectory.compress();
        byte[] encodedBytes = Base64.encode(content, Base64.DEFAULT);
        TypedInput in = new TypedByteArray("application/octet-stream", encodedBytes);

        final String label = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss").format(new Date());
        this.serverAPI().uploadCompressedTrajectory(label, in,
                new Callback<Response>() {
                    @Override public void success(Response returnResponse, Response response) {
                        ((TextView) findViewById(R.id.trajectoryStatus)).setText("Uploaded at " + label);
                        MainActivity.this.currentTrajectory.clear();
                        ((TextView) findViewById(R.id.textView)).setText("");
                    }
                    @Override public void failure(RetrofitError error) {
                        ((TextView) findViewById(R.id.trajectoryStatus)).setText("Failed to upload at " + label + " " + error.toString());
                    }
                });
    }

    private void screenLog(String message) {
        TextView textView = (TextView) findViewById(R.id.textView);
        textView.append(message);
    }

    @Override
    public void onConnected(Bundle bundle) {
        this.screenLog("Location services connected\n");
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
        } else if (requestCode ==  EMAIL_ACTIVITY_RESULT_ID) {
            Log.d(LOG_TAG, "Email result: " + Integer.toString(resultCode));
        }
    }

    private String getCurrentToken() {
        SharedPreferences settings = this.getPreferences(Context.MODE_PRIVATE);
        return settings.getString("token", null);
    }

    private void updateLoginStatus() {
        boolean loggedIn = this.getCurrentToken() != null;
        if (loggedIn) {
            ((TextView) findViewById(R.id.loginStatusTextView)).setText("logged in as ..");
            findViewById(R.id.loginButton).setVisibility(View.INVISIBLE);
            findViewById(R.id.logoutButton).setVisibility(View.VISIBLE);
            this.serverAPI().currentUser(new Callback<User>() {
                @Override public void success(User user, Response response) {
                    ((TextView) findViewById(R.id.loginStatusTextView)).setText("logged in as " + user.getUsername());
                }
                @Override public void failure(RetrofitError error) {
                    ((TextView) findViewById(R.id.loginStatusTextView)).setText("login user failure");
                }
            });
        } else {
            ((TextView) findViewById(R.id.loginStatusTextView)).setText("logged out.");
            findViewById(R.id.loginButton).setVisibility(View.VISIBLE);
            findViewById(R.id.logoutButton).setVisibility(View.INVISIBLE);
        }
    }

    private DriverPeteServer serverAPI() {
        final String token = this.getCurrentToken();
        RequestInterceptor requestInterceptor = new RequestInterceptor() {
            @Override
            public void intercept(RequestFacade request) {
                request.addHeader("X-AUTH-TOKEN", token);
            }
        };

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(serverUrl)
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setClient(new OkClient(UnsafeHttpsClient.getUnsafeOkHttpClient()))
                .setRequestInterceptor(requestInterceptor)
                .build();


       return restAdapter.create(DriverPeteServer.class);
    }
}
