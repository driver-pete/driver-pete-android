package com.otognan.driverpete.android_app;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.location.Location;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.SimpleDateFormat;
import java.util.Locale;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedByteArray;

public class RouteDetailsActivity extends ActionBarActivity
        implements OnMapReadyCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_details);

        TrajectoryEndpoint from = (TrajectoryEndpoint)this.getIntent().getExtras().getSerializable("routeFrom");
        TrajectoryEndpoint to = (TrajectoryEndpoint)this.getIntent().getExtras().getSerializable("routeTo");
        ((TextView) findViewById(R.id.routeLocationsTextView)).setText(
                "Route from " + from.getLabel() + " to " + to.getLabel()
        );

        Route route = (Route)this.getIntent().getExtras().getSerializable("route");

        String startDate = new SimpleDateFormat("HH:mm z, EEE, MMM d, ''yy", Locale.US).format(route.getStartDate());
        ((TextView) findViewById(R.id.routeStartDateTextView)).setText(
                "Started at " + startDate
        );

        long minDuration = (route.getFinishDate() - route.getStartDate()) / 1000 / 60;
        ((TextView) findViewById(R.id.routeDurationTextView)).setText(
                "Duration: " + minDuration + " minutes."
        );

        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.routeMap);
        mapFragment.getView().setVisibility(View.INVISIBLE);
        mapFragment.getMapAsync(this);
    }


    private DriverPeteServer serverAPI() {
        return DriverPeteServerInstance.getInstance(
                this.getIntent().getExtras().getString("token"), 0);
    }

    @Override
    public void onMapReady(final GoogleMap map) {
        Route route = (Route)this.getIntent().getExtras().getSerializable("route");
        this.serverAPI().binaryRoute(route.getId(), new Callback<Response>() {
            @Override
            public void success(Response response, Response response2) {
                TypedByteArray body = (TypedByteArray) response.getBody();
                try {
                    Trajectory route = Trajectory.decompress(Base64
                            .decode(body.getBytes(), Base64.DEFAULT));
                    RouteDetailsActivity.this.showRouteOnMap(map, route);

                } catch (Exception e) {
                    e.printStackTrace();
                    RouteDetailsActivity.this.showAlert("Can not read route", e.getMessage());
                }
            }

            @Override
            public void failure(RetrofitError error) {
                RouteDetailsActivity.this.showAlert("Can not download route", error.getMessage());
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

    private void showRouteOnMap(GoogleMap map, Trajectory route) {
        PolylineOptions polylineOptions = new PolylineOptions();
        for(Location l: route.getLocations()) {
            polylineOptions.add(new LatLng(l.getLatitude(), l.getLongitude()));
        }
        // forest green
        polylineOptions.color(0xff228b22);

        LatLngBounds.Builder bc = new LatLngBounds.Builder();
        for (LatLng item : polylineOptions.getPoints()) {
            bc.include(item);
        }

        map.moveCamera(CameraUpdateFactory.newLatLngBounds(bc.build(), 50));
        map.addPolyline(polylineOptions);

        TrajectoryEndpoint from = (TrajectoryEndpoint)this.getIntent().getExtras().getSerializable("routeFrom");
        TrajectoryEndpoint to = (TrajectoryEndpoint)this.getIntent().getExtras().getSerializable("routeTo");

        map.addMarker(new MarkerOptions()
                .position(new LatLng(from.getLatitude(), from.getLongitude()))
                .title(from.getLabel()));

        map.addMarker(new MarkerOptions()
                .position(new LatLng(to.getLatitude(), to.getLongitude()))
                .title(to.getLabel()));

        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.routeMap);
        mapFragment.getView().setVisibility(View.VISIBLE);
    }
}

