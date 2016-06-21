package com.lightcone.mapexample;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import android.Manifest;


public class ShowMap extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,     // Is this needed?
        OnMapReadyCallback {
    final private int REQUEST_LOCATION = 2;
    private static final String TAG = "Mapper";
    private static double lat;
    private static double lon;
    private static int zm;
    private static boolean trk;
    private static LatLng map_center;
    private GoogleMap map;
    private GoogleApiClient mGoogleApiClient;
    private Location myLocation;
    private LocationRequest mLocationRequest;
    private static final int launcherIcon = R.mipmap.ic_launcher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.showmap);

        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        // Remove default toolbar title and replace with an icon
        if (toolbar != null) {
            toolbar.setNavigationIcon(R.mipmap.ic_launcher);
        }
        // Note: getColor(color) deprecated as of API 23
        toolbar.setTitleTextColor(getResources().getColor(R.color.barTextColor));
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        Log.i(TAG, "Obtain map fragment");

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.the_map);
        mapFragment.getMapAsync(this);

        /* Create new location client. The first 'this' in args is the present
		 * context; the next two 'this' args indicate that this class will handle
		 * callbacks associated with connection and connection errors, respectively
		 * (see the onConnected, onDisconnected, and onConnectionError callbacks below).
		 * You cannot use the location client until the onConnected callback
		 * fires, indicating a valid connection.  At that point you can access location
		 * services such as present position and location updates.
		 */

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.i(TAG, "map ready");
        map = googleMap;
        initializeMap();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the tool bar if it is present.
        getMenuInflater().inflate(R.menu.showmap_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (map == null) {
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
                if (mt == GoogleMap.MAP_TYPE_NORMAL) {
                    map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                } else {
                    map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                }
                return true;
            // Toggle 3D building display
            case R.id.building:
                map.setBuildingsEnabled(!map.isBuildingsEnabled());
                // Change camera tilt to view from angle if 3D
                if (map.isBuildingsEnabled()) {
                    changeCamera(map, map.getCameraPosition().target,
                            map.getCameraPosition().zoom,
                            map.getCameraPosition().bearing, 45, true);
                } else {
                    changeCamera(map, map.getCameraPosition().target,
                            map.getCameraPosition().zoom,
                            map.getCameraPosition().bearing, 0, true);
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

    // Method to initialize the map.  Check for fine-location permission before calling location services

    private void initializeMap() {

        Log.i(TAG, "lat=" + map_center.latitude + " lon=" + map_center.longitude
                + " fine permission="
                + ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) + " granted=" + PackageManager.PERMISSION_GRANTED);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission has not been granted by user previously.  Request it now.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION);
        } else {

            Log.i(TAG, "Permission has been granted");

            // permission has been granted, continue as usual
            myLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

            Log.i(TAG, "Location enabled.  Requested lat=" + map_center.latitude
                    + " Requested lon=" + map_center.longitude);

            setupMap();
        }
    }

    // Method to set up map.  The moveCamera to location command requires that permission to
    // access fine location has been given by user (at runtime for Android 6, API 23 and beyond; at install
    // for earlier versions of Android).

    public void setupMap(){

        //            map.setMyLocationEnabled(trk);

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(map_center, zm));
//
        // Initialize type of map
        map.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        // Initialize 3D buildings enabled for map view
        map.setBuildingsEnabled(false);

        // Initialize whether indoor maps are shown if available
        map.setIndoorEnabled(false);

        // Initialize traffic overlay
        map.setTrafficEnabled(false);

        // Enable rotation gestures
        map.getUiSettings().setRotateGesturesEnabled(true);
    }

    /*Following method invoked by the system after the user response to a runtime permission request
     (Android 6, API 23 and beyond implement such runtime permissions). The system passes to this
     method the user's response, which you then should act upon in this method.  This method can respond
     to more than one type permission.  The variable requestCode distinguishes which permission is being
     processed. */

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        Log.i(TAG, "Permission result: requestCode="+requestCode);

        // Since this method may handle more than one type of permission, distinguish which one by a
        // switch on the requestCode provided by the system.

        switch(requestCode){

            // The permission response was for fine location
            case REQUEST_LOCATION :
                Log.i(TAG, "Fine location permission granted: requestCode="+requestCode);
                // If the request was canceled by user, the results arrays are empty
                if(grantResults.length > 0
                 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                    // Permission was granted. Do the location task that triggered the permission request

                    setupMap();

                } else {
                    Log.i(TAG, "Fine location permission denied: requestCode="+requestCode);

                    // The permission was denied.  Disable functionality depending on the permission.

                    showTaskDialog("Warning!", "This app will not function without this permission!",
                            launcherIcon, this, "Refuse Permission", "Give Permission");

                }
                return;

        }
    }


	/* Method to change properties of camera. If your GoogleMaps instance is called map,
	 * you can use
	 *
	 * map.getCameraPosition().target
	 * map.getCameraPosition().zoom
	 * map.getCameraPosition().bearing
	 * map.getCameraPosition().tilt
	 *
	 * to get the current values of the camera position (target, which is a LatLng),
	 * zoom, bearing, and tilt, respectively.  This permits changing only a subset of
	 * the camera properties by passing the current values for all arguments you do not
	 * wish to change.
	 *
	 * */

    private void changeCamera(GoogleMap map, LatLng center, float zoom,
                              float bearing, float tilt, boolean animate) {

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(center)         // Sets the center of the map
                .zoom(zoom)             // Sets the zoom
                .bearing(bearing)       // Sets the bearing of the camera
                .tilt(tilt)             // Sets the tilt of the camera relative to nadir
                .build();               // Creates a CameraPosition from the builder

        // Move (if variable animate is false) or animate (if animate is true) to new
        // camera properties.

        if (animate) {
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        } else {
            map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    // Set these data using this static method before launching this class with an Intent:
    // for example, ShowMap.putMapData(30,150,18,true);

    public static void putMapData(double latitude, double longitude, int zoom, boolean track) {
        lat = latitude;
        lon = longitude;
        zm = zoom;
        trk = track;
        map_center = new LatLng(lat, lon);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "Connected");

        if (trk) {
            startLocationUpdates();
        }
    }

    protected void startLocationUpdates() {

        // See https://developer.android.com/training/location/receive-location-updates.html

//        LocationServices.FusedLocationApi.requestLocationUpdates(
//                mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    @Override
    public void onLocationChanged(Location location) {

    }

    /**
     * Method showTaskDialog() creates a custom launch dialog popped up by a press on
     * a button.  This dialog presents a summary of the task to the user and has buttons to either
     * launch the task or cancel the dialog. Which task to present is controlled by the value of the
     * int buttonPressed, which is stored if a button is pressed and is used in the switch
     * statement in launchTask(). This version of AlertDialog.Builder allows a theme to be specified.
     */

    private void showTaskDialog(String title, String message, int icon, Context context,
                                String positiveButtonText, String negativeButtonText){

        AlertDialog.Builder builder = new AlertDialog.Builder(context,R.style.MyDialogTheme);
        builder.setMessage(message).setTitle(title).setIcon(icon);
        // Add the buttons
        builder.setPositiveButton(positiveButtonText, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                launchTask();
            }
        });
        builder.setNegativeButton(negativeButtonText, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Can execute additional code here if desired
                // Default is cancellation of dialog window.
                initializeMap();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // Method to execute if user chooses positive button ("Refuse permission" in this case).
    // For this example we will just return to the main page.

    private void launchTask(){
        Toast.makeText(ShowMap.this, "Permission refused. Returning to main page.", Toast.LENGTH_SHORT).show();
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
    }

}
