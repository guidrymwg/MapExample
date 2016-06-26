package com.lightcone.mapexample;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

/* A list of indoor locations with maps enabled may be found at
 *
 *    https://support.google.com/gmm/answer/1685827?hl=en
 *
 * If the map view is centered on one of these locations with high enough
 * zoom, and map.isIndoorEnabled() is true, the interior map will be
 * displayed with a floor selector if there is more than one floor.  We
 * illustrate here with the interior of the 2-story Honolulu International
 * Airport.
*/

public class IndoorExample extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private LatLng honolulu_airport = new LatLng(21.332, -157.92);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.indoorexample);

        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar2);
        // Remove default toolbar title and replace with an icon
        if (toolbar != null) {
            toolbar.setNavigationIcon(R.mipmap.ic_launcher);
        }
        // Note: getColor(color) deprecated as of API 23
        toolbar.setTitleTextColor(getResources().getColor(R.color.barTextColor));
        toolbar.setTitle("Indoor Maps");
        setSupportActionBar(toolbar);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used
        // in onMapReady().

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.indoor_map);
        mapFragment.getMapAsync(this);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; adds items to the action bar if present.
        getMenuInflater().inflate(R.menu.showmap_menu, menu);
        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        initializeMap();
    }

    // Method to initialize the map.  Check for map!=null before using.

    private void initializeMap(){

        // Move camera view and zoom to location
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(honolulu_airport, 18));

        // Initialize type of map
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        // Initialize 3D buildings enabled for map view
        map.setBuildingsEnabled(false);

        // Initialize whether indoor maps are shown if available
        map.setIndoorEnabled(true);

        // Initialize traffic overlay
        map.setTrafficEnabled(false);

        // Disable rotation gestures
        map.getUiSettings().setRotateGesturesEnabled(false);

        // Enable zoom controls on map [in addition to gesture controls like spread or double-
        // tap with 1 finger (to zoom in), and pinch or double-tap with two fingers (to zoom out)].

        map.getUiSettings().setZoomControlsEnabled(true);
    }

    // Method to animate camera properties change

    private void changeCamera(GoogleMap map, LatLng center, float zoom,
                              float bearing, float tilt) {

        // Change properties of camera
        CameraPosition cameraPosition = new CameraPosition.Builder()
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


    // Handle clicks on toolbar menus
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(map == null) {
            Toast.makeText(this, getString(R.string.nomap_error),
                    Toast.LENGTH_LONG).show();
            return false;
        }

        // Handle item selection
        switch (item.getItemId()) {
            // Toggle traffic overlay
            case R.id.traffic:
                map.setTrafficEnabled(!map.isTrafficEnabled());
                return true;
            // Toggle satellite overlay
            case R.id.satellite:
                int mt = map.getMapType();
                if(mt == GoogleMap.MAP_TYPE_NORMAL){
                    map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                } else {
                    map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                }
                return true;
            // Toggle 3D building display
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
                Intent j = new Intent(this, Settings.class);
                startActivity(j);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
