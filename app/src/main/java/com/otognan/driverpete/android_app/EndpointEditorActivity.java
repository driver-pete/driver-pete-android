package com.otognan.driverpete.android_app;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class EndpointEditorActivity extends ActionBarActivity
        implements OnMapReadyCallback {

    private boolean isLocationA;
    private TrajectoryEndpoint endpoint;
    private GoogleMap currentMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_endpoint_editor);

        this.isLocationA = this.getIntent().getExtras().getBoolean("isLocationA");
        this.endpoint = (TrajectoryEndpoint)this.getIntent().getSerializableExtra("endpoint");

        ((EditText)findViewById(R.id.locationLabelEditText)).setText(this.endpoint.getLabel());
        final EditText addressEditText = (EditText)findViewById(R.id.locationAddressLabelText);
        addressEditText.setText(this.endpoint.getAddress());
        addressEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                        actionId == EditorInfo.IME_ACTION_DONE ||
                        event.getAction() == KeyEvent.ACTION_DOWN &&
                                event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    if (!event.isShiftPressed()) {


                        Geocoder geo = new Geocoder(EndpointEditorActivity.this.getApplicationContext(), Locale.getDefault());
                        try {
                            List<Address> addresses = geo.getFromLocationName(addressEditText.getText().toString(), 1);
                            if (addresses.size() > 0) {
                                Address address = addresses.get(0);
                                EndpointEditorActivity.this.endpoint.setLatitude(address.getLatitude());
                                EndpointEditorActivity.this.endpoint.setLongitude(address.getLongitude());
                                if (EndpointEditorActivity.this.currentMap != null) {
                                    EndpointEditorActivity.this.onMapReady(EndpointEditorActivity.this.currentMap);
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        return true;
                    }
                }
                return false; // pass on to other listeners.
            }
        });

        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.endpointMap);
        mapFragment.getView().setVisibility(View.INVISIBLE);
        mapFragment.getMapAsync(this);

    }

    public void onSubmitButton(View view) {
        Intent returnIntent = new Intent();
        TrajectoryEndpoint endpoint = new TrajectoryEndpoint();
        endpoint.setLabel(((EditText) findViewById(R.id.locationLabelEditText)).getText().toString());
        endpoint.setAddress(((EditText) findViewById(R.id.locationAddressLabelText)).getText().toString());
        returnIntent.putExtra("endpoint", endpoint);
        returnIntent.putExtra("isLocationA", this.isLocationA);
        setResult(RESULT_OK, returnIntent);
        this.finish();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.currentMap = map;
        map.clear();
        LatLng latLng = new LatLng(this.endpoint.getLatitude(), this.endpoint.getLongitude());
        map.moveCamera(CameraUpdateFactory.newLatLng(latLng));

        map.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title(this.endpoint.getLabel())
                        .draggable(true)
        );

        map.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {}
            @Override
            public void onMarkerDrag(Marker marker) {}
            @Override
            public void onMarkerDragEnd(Marker marker) {
                EndpointEditorActivity.this.endpoint.setLatitude(marker.getPosition().latitude);
                EndpointEditorActivity.this.endpoint.setLongitude(marker.getPosition().longitude);

                Geocoder geo = new Geocoder(EndpointEditorActivity.this.getApplicationContext(), Locale.getDefault());
                try {
                    List<Address> addresses = geo.getFromLocation(
                            marker.getPosition().latitude, marker.getPosition().longitude, 1);
                    if (addresses.size() > 0) {
                        String address = addresses.get(0).getAddressLine(0);
                        ((EditText) findViewById(R.id.locationAddressLabelText)).setText(address);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.endpointMap);
        mapFragment.getView().setVisibility(View.VISIBLE);
    }
}
