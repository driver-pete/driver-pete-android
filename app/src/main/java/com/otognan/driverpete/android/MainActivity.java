package com.otognan.driverpete.android;


import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.squareup.okhttp.OkHttpClient;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.OkClient;
import retrofit.client.Response;
import retrofit.mime.TypedByteArray;
import retrofit.mime.TypedInput;


public class MainActivity extends ActionBarActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private static final String LOG_TAG = "MainActivity";
    private static final String serverUrl = "https://192.168.1.2:8443";
    //private static final String serverUrl = "https://testbeanstalkenv-taz59dxmiu.elasticbeanstalk.com";

    private static final double locationDistanceThreshold = 50.;

    // Milliseconds per second
    private static final int MILLISECONDS_PER_SECOND = 1000;
    // Update frequency in seconds
    public static final int UPDATE_INTERVAL_IN_SECONDS = 10;
    // Update frequency in milliseconds
    private static final long UPDATE_INTERVAL =
            MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;
    // The fastest update frequency, in seconds
    private static final int FASTEST_INTERVAL_IN_SECONDS = 5;
    // A fast frequency ceiling in milliseconds
    private static final long FASTEST_INTERVAL =
            MILLISECONDS_PER_SECOND * FASTEST_INTERVAL_IN_SECONDS;

    // Update frequency in seconds in the sleep mode
    public static final int SLEEP_UPDATE_INTERVAL_IN_SECONDS = 40;
    // Update frequency in milliseconds in the sleep mode
    private static final long SLEEP_UPDATE_INTERVAL =
            MILLISECONDS_PER_SECOND * SLEEP_UPDATE_INTERVAL_IN_SECONDS;
    // The fastest update frequency in the sleep mode, in seconds
    private static final int FASTEST_SLEEP_INTERVAL_IN_SECONDS = 20;
    // A fast frequency ceiling in milliseconds
    private static final long FASTEST_SLEEP_INTERVAL =
            MILLISECONDS_PER_SECOND * FASTEST_SLEEP_INTERVAL_IN_SECONDS;


    private static final int LOGIN_ACTIVITY_RESULT_ID = 1;
    private static final int EMAIL_ACTIVITY_RESULT_ID = 2;
    private static final int EDIT_ENDPOINT_ACTIVITY_RESULT_ID = 3;

    // Define an object that holds accuracy and frequency parameters
    private GoogleApiClient mGoogleApiClient;

    private Trajectory currentTrajectory = new Trajectory();

    private int stationaryLocationsCounter = 0;
    // switch to sleep mode after this number of stationary locations received
    private static final int STATIONARY_LOCATION_SWITCH_THRESHOLD_SECONDS = 180;
    private boolean isInSleepMode = false;


    private static final int SEND_DATA_EVERY_SECONDS = 120;
    private static final int MINIMAL_TRAJECTORY_SIZE_TO_SEND = 50;
    private static final int SERVER_TIMEOUT_TO_CHECK_CONNECTIVITY = 2;

    private Handler timerHandler;

    private TrajectoryEndpoint endpointA;
    private TrajectoryEndpoint endpointB;

    private LogService logService;
    private List<String> logCache = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(this, LogService.class);
        bindService(intent, logServiceConnection, BIND_AUTO_CREATE);

        setContentView(R.layout.activity_main);

        this.updateLoginStatus();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        this.screenLog("Connecting to google api..\n");
        mGoogleApiClient.connect();

        final Handler timerHandler = new Handler();
        class TimerRunnable implements Runnable {
            @Override
            public void run() {
                if (MainActivity.this.currentTrajectory.size() > MINIMAL_TRAJECTORY_SIZE_TO_SEND) {
                    MainActivity.this.serverAPI(SERVER_TIMEOUT_TO_CHECK_CONNECTIVITY).currentUser(new Callback<User>() {
                        @Override
                        public void success(User user, Response response) {
                            try {
                                MainActivity.this.sendToServer();
                                MainActivity.this.screenLog("Sent to server by timer\n");
                            } catch (Exception e) {
                                MainActivity.this.screenLog("Not sending by timer - exception\n");
                                e.printStackTrace();
                            }
                            timerHandler.postDelayed(TimerRunnable.this, SEND_DATA_EVERY_SECONDS * 1000);
                        }

                        @Override
                        public void failure(RetrofitError error) {
                            MainActivity.this.screenLog("Not sending from timer - no connectivity\n");
                            timerHandler.postDelayed(TimerRunnable.this, SEND_DATA_EVERY_SECONDS * 1000);
                        }
                    });
                } else {
                    MainActivity.this.screenLog("Not sending from timer - too small\n");
                    timerHandler.postDelayed(TimerRunnable.this, SEND_DATA_EVERY_SECONDS * 1000);
                }
            }
        }
        ;
        timerHandler.postDelayed(new TimerRunnable(), SEND_DATA_EVERY_SECONDS * 1000);

        RadioGroup routesRadioGroup = (RadioGroup) findViewById(R.id.routesRadioGroup);
        routesRadioGroup.clearCheck();
        routesRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.routeRadioAtoB:
                        findViewById(R.id.routesAtoBListView).setVisibility(View.VISIBLE);
                        findViewById(R.id.routesBtoAListView).setVisibility(View.INVISIBLE);
                        break;
                    case R.id.routeRadioBtoA:
                        findViewById(R.id.routesAtoBListView).setVisibility(View.INVISIBLE);
                        findViewById(R.id.routesBtoAListView).setVisibility(View.VISIBLE);
                        break;
                    default:
                        findViewById(R.id.routesAtoBListView).setVisibility(View.INVISIBLE);
                        findViewById(R.id.routesBtoAListView).setVisibility(View.INVISIBLE);
                        break;
                }
            }
        });

        this.refreshData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_login:
                this.logIn();
                return true;
            case R.id.action_logout:
                this.logOut();
                return true;
            case R.id.action_send_to_server:
                try {
                    this.sendToServer();
                } catch (Exception e) {
                    e.printStackTrace();
                    this.showAlert("Failed to send to server", e.getMessage());
                }
                return true;
            case R.id.action_refresh_data:
                this.refreshData();
                return true;
            case R.id.action_reprocess_all_data:
                this.reprocessAllData();
                return true;
            case R.id.action_show_log:
                this.showLog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        this.screenLog("GoogleApiClient connection has failed. Setting up fake messages..");

        this.timerHandler = new Handler();
        Runnable timerRunnable = new Runnable() {
            @Override
            public void run() {
                Location location = new Location("Google");
                location.setTime(System.currentTimeMillis());
                location.setLatitude(34.34);
                location.setLongitude(-117.02);
                MainActivity.this.onLocationChanged(location);
                timerHandler.postDelayed(this, 10000);
            }
        };
        timerHandler.postDelayed(timerRunnable, 0);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(LOG_TAG, "Received location: " + Trajectory.locationToString(location));
        this.screenLog(Trajectory.locationToShortString(location) + "\n");
        if (this.currentTrajectory.size() > 0) {
            Location lastLocaton = this.currentTrajectory.lastLocation();
            if (lastLocaton.distanceTo(location) < locationDistanceThreshold) {
                Log.d(LOG_TAG, Trajectory.locationToShortString(location) + " is ignored");
                if (!this.isInSleepMode) {
                    this.stationaryLocationsCounter += 1;
                    Log.d(LOG_TAG, "Stationary counter: " + this.stationaryLocationsCounter);
                    int stepsCounter = STATIONARY_LOCATION_SWITCH_THRESHOLD_SECONDS / UPDATE_INTERVAL_IN_SECONDS;
                    if (this.stationaryLocationsCounter > stepsCounter) {
                        this.subscribeToLocations(true);
                    }
                }
                return;
            }
        }

        // Do not switch to fast trajectories immediately but wait for 2 iterations
        if (this.stationaryLocationsCounter > 2) {
            this.stationaryLocationsCounter = 2;
        }

        if (this.stationaryLocationsCounter > 0) {
            this.stationaryLocationsCounter -= 1;
            Log.d(LOG_TAG, "Stationary counter: " + this.stationaryLocationsCounter);
        } else {
            if (this.isInSleepMode) {
                this.subscribeToLocations(false);
            }
            this.screenLog(Trajectory.locationToShortString(location) + "\n");
            this.currentTrajectory.addLocation(location);
        }
    }

    public void sendToServer() throws Exception {
        byte[] content = this.currentTrajectory.compress();
        byte[] encodedBytes = Base64.encode(content, Base64.DEFAULT);
        TypedInput in = new TypedByteArray("application/octet-stream", encodedBytes);

        final String label = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss_z").format(new Date());
        this.serverAPI().uploadCompressedTrajectory(label, in,
                new Callback<Response>() {
                    @Override
                    public void success(Response returnResponse, Response response) {
                        ((TextView) findViewById(R.id.trajectoryStatus)).setText("Uploaded at " + label);
                        Location lastLocation = MainActivity.this.currentTrajectory.lastLocation();
                        MainActivity.this.currentTrajectory.clear();
                        MainActivity.this.clearLog();
                        //Re-add last location to preserve continuity in logic
                        MainActivity.this.currentTrajectory.addLocation(lastLocation);
                        MainActivity.this.screenLog(Trajectory.locationToShortString(lastLocation) + "\n");
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        ((TextView) findViewById(R.id.trajectoryStatus)).setText("Failed to upload at " + label + " " + error.toString());
                    }
                });
    }

    private void screenLog(String message) {
        if (this.logService != null) {
            this.logService.addLog(message);
        } else {
            this.logCache.add(message);
        }
    }

    private void clearLog() {
        if (this.logService != null) {
            this.logService.clearLog();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        this.screenLog("Location services connected\n");
        subscribeToLocations(false);
    }

    private void subscribeToLocations(boolean sleepMode) {
        if (this.timerHandler != null) {
            // simulation mode - do not subscirbe
            return;
        }
        this.isInSleepMode = sleepMode;
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);

        if (sleepMode) {
            this.screenLog("Subscribing to locations in sleep mode\n");
        } else {
            this.screenLog("Subcribing to fast locations\n");
        }
        LocationRequest locationRequest;
        locationRequest = LocationRequest.create();
        // Use high accuracy
        locationRequest.setPriority(
                LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Set the update interval
        if (sleepMode) {
            locationRequest.setInterval(SLEEP_UPDATE_INTERVAL);
        } else {
            locationRequest.setInterval(UPDATE_INTERVAL);
        }


        // Set the fastest update interval
        if (sleepMode) {
            locationRequest.setFastestInterval(FASTEST_SLEEP_INTERVAL);
        } else {
            locationRequest.setFastestInterval(FASTEST_INTERVAL);
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        this.screenLog("GoogleApiClient connection has been suspend");
    }


    public void logIn() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("serverUrl", serverUrl);
        this.startActivityForResult(intent, LOGIN_ACTIVITY_RESULT_ID);
    }

    public void logOut() {
        SharedPreferences settings = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.remove("token");
        editor.commit();
        updateLoginStatus();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == LOGIN_ACTIVITY_RESULT_ID) {
            if (resultCode == RESULT_OK) {
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
        } else if (requestCode == EMAIL_ACTIVITY_RESULT_ID) {
            Log.d(LOG_TAG, "Email result: " + Integer.toString(resultCode));
        } else if (requestCode == EDIT_ENDPOINT_ACTIVITY_RESULT_ID) {
            if (resultCode == RESULT_OK) {
                this.onEndpointEditingFinished(data);
            }
        }
    }

    private String getCurrentToken() {
        SharedPreferences settings = this.getPreferences(Context.MODE_PRIVATE);
        return settings.getString("token", null);
    }

    private void updateLoginStatus() {
        boolean loggedIn = this.getCurrentToken() != null;
        final ActionBar actionBar = getSupportActionBar();
        if (loggedIn) {
            actionBar.setTitle("logged in as ..");
            this.serverAPI().currentUser(new Callback<User>() {
                @Override
                public void success(User user, Response response) {
                    actionBar.setTitle("logged in as " + user.getUsername());
                }

                @Override
                public void failure(RetrofitError error) {
                    actionBar.setTitle("login user failure");
                }
            });
        } else {
            actionBar.setTitle("logged out.");
        }
    }

    private DriverPeteServer serverAPI() {
        return this.serverAPI(0);
    }

    private DriverPeteServer serverAPI(int timeoutSeconds) {
        final String token = this.getCurrentToken();
        RequestInterceptor requestInterceptor = new RequestInterceptor() {
            @Override
            public void intercept(RequestFacade request) {
                request.addHeader("X-AUTH-TOKEN", token);
            }
        };

        OkHttpClient httpClient = UnsafeHttpsClient.getUnsafeOkHttpClient();
        if (timeoutSeconds != 0) {
            httpClient.setReadTimeout(timeoutSeconds, TimeUnit.SECONDS);
            httpClient.setConnectTimeout(timeoutSeconds, TimeUnit.SECONDS);
        }
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(serverUrl)
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setClient(new OkClient(httpClient))
                .setRequestInterceptor(requestInterceptor)
                .build();


        return restAdapter.create(DriverPeteServer.class);
    }

    public void reprocessAllData() {
        this.serverAPI().deleteProcessedUserData(new Callback<Response>() {
            @Override
            public void success(Response response, Response response2) {
                MainActivity.this.serverAPI().reprocessAllUserData(new Callback<Response>() {
                    @Override
                    public void success(Response returnResponse, Response response) {
                        MainActivity.this.showAlert("Processing is done", "Everything is fine");
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        MainActivity.this.showAlert("Processing error", error.getMessage());
                    }
                });
            }

            @Override
            public void failure(RetrofitError error) {
                MainActivity.this.showAlert("Delete endpoints error", error.getMessage());
            }
        });

    }

    private void showAlert(String title, String message) {
        final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setIcon(R.drawable.notification_template_icon_bg);
        alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                alertDialog.cancel();
            }
        });
        alertDialog.show();
    }

    private void refreshData() {
        this.refreshEndpoints();
        this.refreshRoutes();
    }

    private void refreshEndpoints() {
        this.serverAPI().trajectoryEndpoints(new Callback<List<TrajectoryEndpoint>>() {
            @Override
            public void success(List<TrajectoryEndpoint> trajectoryEndpoints, Response response) {
                if (trajectoryEndpoints.size() > 0) {
                    MainActivity.this.endpointA = trajectoryEndpoints.get(0);
                } else {
                    MainActivity.this.endpointA = null;
                }
                if (trajectoryEndpoints.size() > 1) {
                    MainActivity.this.endpointB = trajectoryEndpoints.get(1);
                } else {
                    MainActivity.this.endpointB = null;
                }

                MainActivity.this.updateEndpointGui(MainActivity.this.endpointA,
                        R.id.locationALabelText, R.id.locationAAddress, R.id.editAButton);
                MainActivity.this.updateEndpointGui(MainActivity.this.endpointB,
                        R.id.locationBLabelText, R.id.locationBAddress, R.id.editBButton);
            }

            @Override
            public void failure(RetrofitError error) {
                MainActivity.this.showAlert("Failed to get endpoints", error.getMessage());
                MainActivity.this.endpointA = null;
                MainActivity.this.endpointB = null;
                MainActivity.this.updateEndpointGui(MainActivity.this.endpointA,
                        R.id.locationALabelText, R.id.locationAAddress, R.id.editAButton);
                MainActivity.this.updateEndpointGui(MainActivity.this.endpointB,
                        R.id.locationBLabelText, R.id.locationBAddress, R.id.editBButton);
            }
        });
    }


    private void updateEndpointGui(TrajectoryEndpoint endpoint, int labelTextId, int addressTextId, int editButtonId) {
        TextView labelText = ((TextView) findViewById(labelTextId));
        TextView addressText = ((TextView) findViewById(addressTextId));
        Button button = ((Button) findViewById(editButtonId));
        if (endpoint != null) {
            labelText.setText(endpoint.getLabel());
            addressText.setText(endpoint.getAddress());
            addressText.setVisibility(View.VISIBLE);
            button.setVisibility(View.VISIBLE);
        } else {
            addressText.setVisibility(View.INVISIBLE);
            button.setVisibility(View.INVISIBLE);
            labelText.setText("<NO ENDPOINT LOCATION FOUND>");
        }
    }

    public void editAEndpoint(View view) {
        Intent intent = new Intent(this, EndpointEditorActivity.class);
        intent.putExtra("label", ((TextView) findViewById(R.id.locationALabelText)).getText());
        intent.putExtra("address", ((TextView) findViewById(R.id.locationAAddress)).getText());
        intent.putExtra("isLocationA", true);
        this.startActivityForResult(intent, EDIT_ENDPOINT_ACTIVITY_RESULT_ID);
    }

    public void editBEndpoint(View view) {
        Intent intent = new Intent(this, EndpointEditorActivity.class);
        intent.putExtra("label", ((TextView) findViewById(R.id.locationBLabelText)).getText());
        intent.putExtra("address", ((TextView) findViewById(R.id.locationBAddress)).getText());
        intent.putExtra("isLocationA", false);
        this.startActivityForResult(intent, EDIT_ENDPOINT_ACTIVITY_RESULT_ID);
    }

    private void onEndpointEditingFinished(Intent data) {
        boolean isLocationA = data.getBooleanExtra("isLocationA", true);
        TrajectoryEndpoint endpoint = isLocationA ? this.endpointA : this.endpointB;
        endpoint.setLabel(data.getStringExtra("label"));
        endpoint.setAddress(data.getStringExtra("address"));

        if (isLocationA) {
            MainActivity.this.updateEndpointGui(MainActivity.this.endpointA,
                    R.id.locationALabelText, R.id.locationAAddress, R.id.editAButton);
        } else {
            MainActivity.this.updateEndpointGui(MainActivity.this.endpointB,
                    R.id.locationBLabelText, R.id.locationBAddress, R.id.editBButton);
        }

        ((RadioButton)findViewById(R.id.routeRadioAtoB)).setText(
                this.endpointA.getLabel() + " to " + this.endpointB.getLabel());
        ((RadioButton)findViewById(R.id.routeRadioBtoA)).setText(
                this.endpointB.getLabel() + " to " + this.endpointA.getLabel());

        this.serverAPI().editEndpoint(endpoint, new Callback<Response>() {
            @Override
            public void success(Response response, Response response2) {
            }

            @Override
            public void failure(RetrofitError error) {
                MainActivity.this.showAlert("Failed to edit endpoint", error.getMessage());
            }
        });
    }

    private void showLog() {
        this.startActivity(new Intent(this, LogActivity.class));
    }

    private ServiceConnection logServiceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className,
                                       IBinder binder) {
            LogService.LogServiceBinder b = (LogService.LogServiceBinder) binder;
            logService = b.getService();
            for (String message : MainActivity.this.logCache) {
                logService.addLog(message);
            }
            MainActivity.this.logCache.clear();
        }

        public void onServiceDisconnected(ComponentName className) {
            logService = null;
        }
    };


    private void refreshRoutes() {

        this.serverAPI().routes(true, new Callback<List<Route>>() {
            @Override
            public void success(final List<Route> routesAtoB, Response response) {

                MainActivity.this.serverAPI().routes(false, new Callback<List<Route>>() {
                    @Override
                    public void success(List<Route> routesBtoA, Response response) {

                        MainActivity.this.updateRoutesGui(routesAtoB, routesBtoA);
                    }
                    @Override
                    public void failure(RetrofitError error) {
                        MainActivity.this.showAlert("Failed to get B to A routes", error.getMessage());
                    }
                });
            }
            @Override
            public void failure(RetrofitError error) {
                MainActivity.this.showAlert("Failed to get A to B routes", error.getMessage());
            }
        });

    }

    private void updateRoutesGui(List<Route> routesAtoB, List<Route> routesBtoA) {

        boolean routesFound = (routesAtoB.size() > 0 || routesBtoA.size() > 0);
        findViewById(R.id.noRoutesFoundTextView).setVisibility(!routesFound? View.VISIBLE:View.INVISIBLE);
        findViewById(R.id.routesRadioLayout).setVisibility(routesFound ? View.VISIBLE : View.INVISIBLE);

        RadioGroup routesRadioGroup = (RadioGroup) findViewById(R.id.routesRadioGroup);
        if (!routesFound) {
            // uncheck radio group to be able to determine the state whether routes
            // just appeared or they existed and there is no need to change check state
            routesRadioGroup.check(-1);
            return;
        }

        Route.sortByDuration(routesAtoB);
        Route.sortByDuration(routesBtoA);

        ListView routesAtoBListView = (ListView) findViewById(R.id.routesAtoBListView);
        routesAtoBListView.setAdapter(new RouteArrayAdapter(this, routesAtoB));

        ListView routesBtoAListView = (ListView) findViewById(R.id.routesBtoAListView);
        routesBtoAListView.setAdapter(new RouteArrayAdapter(this, routesBtoA));

        ((RadioButton)findViewById(R.id.routeRadioAtoB)).setText(
                this.endpointA.getLabel()+" to " + this.endpointB.getLabel());
        ((RadioButton)findViewById(R.id.routeRadioBtoA)).setText(
                this.endpointB.getLabel()+" to " + this.endpointA.getLabel());

        if (routesRadioGroup.getCheckedRadioButtonId() == -1) {
            if (routesAtoB.size() > 0) {
                routesRadioGroup.check(R.id.routeRadioAtoB);
            } else {
                routesRadioGroup.check(R.id.routeRadioBtoA);
            }
        }


//        routesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view,
//                                    int position, long id) {
//
//                // ListView Clicked item index
//                int itemPosition = position;
//
//                // ListView Clicked item value
//                String itemValue = (String) routesListView.getItemAtPosition(position);
//
//                // Show Alert
//                Toast.makeText(getApplicationContext(),
//                        "Position :" + itemPosition + "  ListItem : " + itemValue, Toast.LENGTH_SHORT)
//                        .show();
//
//            }
//        });
    }

    public class RouteArrayAdapter extends ArrayAdapter<Route> {

        public RouteArrayAdapter(Context context, List<Route> values) {
            super(context, R.layout.route_item_layout, values);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = getLayoutInflater();
            View rowView = inflater.inflate(R.layout.route_item_layout, parent, false);
            TextView durationTextView = (TextView) rowView.findViewById(R.id.routeItemDurationTextView);
            long minDuration = (this.getItem(position).getFinishDate() - this.getItem(position).getStartDate()) / 1000 / 60;
            durationTextView.setText(minDuration + " min");

            TextView startTextView = (TextView) rowView.findViewById(R.id.routeItemStartTextView);
            String startDate = new SimpleDateFormat("HH:mm", Locale.US).format(
                    this.getItem(position).getStartDate());
            startTextView.setText("Started at " + startDate);

            return rowView;
        }
    }


}
