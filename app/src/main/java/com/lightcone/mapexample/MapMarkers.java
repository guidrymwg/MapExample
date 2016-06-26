package com.lightcone.mapexample;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class MapMarkers extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        OnMapReadyCallback,GoogleMap.OnInfoWindowClickListener {

    private GoogleMap map;
    private static final String TAG = "Mapper";
    private final LatLng honolulu = new LatLng(21.31,-157.85000);
    private final LatLng waikiki = new LatLng(21.275,-157.825000);
    private final LatLng diamond_head = new LatLng(21.261941,-157.805901);
    private final LatLng map_center = new LatLng(21.3,-157.825);

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mapmarkers);

        // Set up Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar3);
        // Remove default toolbar title and replace with an icon
        if (toolbar != null) {
            toolbar.setNavigationIcon(R.mipmap.ic_launcher);
        }
        // Note: getColor(color) deprecated as of API 23
        toolbar.setTitleTextColor(getResources().getColor(R.color.barTextColor));
        toolbar.setTitle("Map Example");
        setSupportActionBar(toolbar);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used
        // in onMapReady().

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.markers_map);
        mapFragment.getMapAsync(this);
    }


    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.showmap_menu, menu);
        return true;
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        initializeMap();
    }

    // Method to initialize the map

    private void initializeMap(){

        // Move camera view and zoom to location
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(map_center, 13));

        // Initialize type of map
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        // Initialize 3D buildings enabled for map view
        map.setBuildingsEnabled(false);

        // Initialize whether indoor maps are shown if available
        map.setIndoorEnabled(false);

        // Initialize traffic overlay
        map.setTrafficEnabled(false);

        // Enable rotation gestures
        map.getUiSettings().setRotateGesturesEnabled(true);

        // Enable zoom controls on map [in addition to gesture controls like spread or double-
        // tap with 1 finger (to zoom in), and pinch or double-tap with two fingers (to zoom out)].

        map.getUiSettings().setZoomControlsEnabled(true);

        addMapMarkers();

        // Add marker info window click listener
        map.setOnInfoWindowClickListener(this);
    }

    // Method to animate camera properties change

    private void changeCamera(final GoogleMap map, final LatLng center, final float zoom,
                              final float bearing, final float tilt) {

        // Change properties of camera
        final CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(center)         // Sets the center of the map
                .zoom(zoom)             // Sets the zoom
                .bearing(bearing)       // Sets the orientation of the camera
                .tilt(tilt)             // Sets the tilt of the camera relative to nadir
                .build();               // Creates a CameraPosition from the builder
        if(map != null){
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        } else {
            Toast.makeText(this, getString(R.string.nomap_error),
                    Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public void onInfoWindowClick(final Marker marker) {

        String address = null;
        final String title = marker.getTitle();
        if(title.equals("Honolulu")){
            address = "http://www.honolulu.gov/government/";
        } else if (title.equals("Waikiki")) {
            address = "http://en.wikipedia.org/wiki/Waikiki";
        } else if (title.equals("Diamond Head")) {
            address = "http://en.wikipedia.org/wiki/Diamond_Head,_Hawaii";
        }

        marker.hideInfoWindow();

        final Intent link = new Intent(Intent.ACTION_VIEW);
        link.setData(Uri.parse(address));
        startActivity(link);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    // Method to add map markers. See
    //     http://developer.android.com/reference/com/google/android/gms/maps/model
    //      /BitmapDescriptorFactory.html
    // for additional marker color options.

    private void addMapMarkers(){

        MapsInitializer.initialize(this);

        // Add some location markers
        map.addMarker(new MarkerOptions()
                .title("Honolulu")
                .snippet("Capitol of the state of Hawaii")
                .position(honolulu)
        ).setDraggable(true);

        map.addMarker(new MarkerOptions()
                .title("Diamond Head")
                .snippet("Extinct volcano; iconic landmark")
                .position(diamond_head)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
        );

        map.addMarker(new MarkerOptions()
                .title("Waikiki")
                .snippet("A world-famous beach")
                .position(waikiki)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
        );

    }

    // Handle menu clicks in toolbar

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            // Toggle traffic overlay
            case R.id.traffic:
                map.setTrafficEnabled(!map.isTrafficEnabled());
                return true;
            // Toggle satellite overlay
            case R.id.satellite:
                final int mt = map.getMapType();
                if(mt == GoogleMap.MAP_TYPE_NORMAL){
                    map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                } else {
                    map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                }
                return true;
            // Toggle 3D building display (best when showing map instead of satellite)
            case R.id.building:
                map.setBuildingsEnabled(!map.isBuildingsEnabled());
                // Change camera tilt to view from angle if 3D
                if(map.isBuildingsEnabled()){
                    changeCamera(map, map.getCameraPosition().target,
                            map.getCameraPosition().zoom,
                            map.getCameraPosition().bearing, 45);
                } else {
                    changeCamera(map, map.getCameraPosition().target,
                            map.getCameraPosition().zoom,
                            map.getCameraPosition().bearing, 0);
                }
                return true;
            // Toggle whether indoor maps displayed
            case R.id.indoor:
                map.setIndoorEnabled(!map.isIndoorEnabled());
                return true;
            // Settings page
            case R.id.action_settings:
                // Actions for settings page
                final Intent j = new Intent(this, Settings.class);
                startActivity(j);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
