package com.otognan.driverpete.android_app;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class RouteDetailsActivity extends ActionBarActivity
        implements OnMapReadyCallback {


    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_details);

        this.token = this.getIntent().getExtras().getString("token");

        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.routeMap);
        mapFragment.getMapAsync(this);

        //findViewById(R.id.he)
    }


    private DriverPeteServer serverAPI() {
        return DriverPeteServerInstance.getInstance(this.token, 0);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        map.addMarker(new MarkerOptions()
                .position(new LatLng(0, 0))
                .title("Marker"));
    }
}
