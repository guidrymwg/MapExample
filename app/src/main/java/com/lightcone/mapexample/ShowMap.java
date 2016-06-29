package com.lightcone.mapexample;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;
import android.Manifest;


public class ShowMap extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    // Update interval in milliseconds for location services
    private static final long UPDATE_INTERVAL = 5000;
    // Fastest update interval in milliseconds for location services
    private static final long FASTEST_INTERVAL = 1000;

    // User defines value of REQUEST_LOCATION. It will identify a permission request
    // specifically for ACCESS_FINE_LOCATION.  Define a different integer for each
    // "dangerous" permission that you will request at runtime (in this example there
    // is only one).

    final private int REQUEST_LOCATION = 2; // User-defined integer

    private static final String TAG = "Mapper";
    private static double lat;
    private static double lon;
    private static int zm;
    private static boolean trk;
    private static LatLng map_center;
    private GoogleMap map;
    private GoogleApiClient mGoogleApiClient;
    private Location myLocation;
    private double myLat;
    private double myLon;
    private LocationRequest mLocationRequest;
    private static final int dialogIcon = R.mipmap.ic_launcher;



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

        // Obtain the SupportMapFragment.  We will be notified when the map is ready to
        // be used in onMapReady().

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.the_map);
        mapFragment.getMapAsync(this);

        /* Create new location client. The first 'this' in args is the present
		 * context; the next two 'this' args indicate that this class will handle
		 * callbacks associated with connection and connection errors, respectively
		 * (see the onConnected, onDisconnected, and onConnectionError callbacks below).
		 * You cannot use the location client until the onConnected callback
		 * fires, indicating a valid connection.  At that point you can access location
		 * services such as present position and location updates. */

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create();
        // Set request for high accuracy
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        // Set update interval
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        // Set fastest update interval that we can accept
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

    }

    // This method will be called when the map is ready.  Only then can we
    // manipulate the map object.

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        setupMap();
        initializeLocation();
    }

    // Method to set up map.

    public void setupMap() {

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

        // Enable zoom controls on map [in addition to gesture controls like spread or double-
        // tap with 1 finger (to zoom in), and pinch or double-tap with two fingers (to zoom out)].

        map.getUiSettings().setZoomControlsEnabled(true);

        // Add a long-press listener to the map
        map.setOnMapLongClickListener(this);

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(map_center, zm));
    }

    /* Method to initialize location services for the map.  For Android 6 (API 23) and
     beyond, we must check for the "dangerous" permission ACCESS_FINE_LOCATION at
     runtime (in addition to declaring it in the manifest file).  The following code checks
     for this permission.  If it has already been granted, it proceeds as normal.  If it
     has not yet been granted by the user, the user is presented with an opportunity to grant
     it. In general, the rest of this class will not execute until the user has granted such permission. */

    private void initializeLocation() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            /* Permission has not been granted by user previously.  Request it now. The system
             will present a dialog to the user requesting the permission, with options "accept",
             "deny", and a box to check "don't ask again". When the user chooses, the system
             will then fire the onRequestPermissionsResult() callback, passing in the user-defined
             integer defining the type of permission request (REQUEST_LOCATION in this case)
             and the "accept" or "deny" user response.  We deal appropriately
             with the user response in our override of onRequestPermissionsResult() below.*/

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        } else {
            Log.i(TAG, "Permission has been granted");

            myLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        }
    }



    /*Following method invoked by the system after the user response to a runtime permission request
     (Android 6, API 23 and beyond implement such runtime permissions). The system passes to this
     method the user's response, which you then should act upon in this method.  This method can respond
     to more than one type permission.  The user-defined integer requestCode (passed in the call to
     ActivityCompat.requestPermissions) distinguishes which permission is being processed. */

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        // Since this method may handle more than one type of permission, distinguish which one by a
        // switch on the requestCode passed back to you by the system.

        switch (requestCode) {

            // The permission response was for fine location
            case REQUEST_LOCATION:
                Log.i(TAG, "Fine location permission granted: requestCode=" + requestCode);
                // If the request was canceled by user, the results arrays are empty
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // Permission was granted. Do the location task that triggered the
                    // permission request

                    initializeLocation();

                } else {
                    Log.i(TAG, "onRequestPermissionsResult - permission denied: requestCode=" + requestCode);

                    // The permission was denied.  Warn the user of the consequences and give
                    // them one last time to enable the permission.

                    showTaskDialog(1, "Warning!",
                            "This part of the app will not function without this permission!",
                            dialogIcon, this, "OK, Do Over", "Refuse Permission");
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

    // Callback from GoogleApiClient indicating that a connection has been established

    @Override
    public void onConnected(Bundle bundle) {

        // Indicate that a connection has been established
        Toast.makeText(this, getString(R.string.connected_toast), Toast.LENGTH_SHORT).show();

        // Be certain that permission has previously been given for fine location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission
                (this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);

        myLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        myLat = myLocation.getLatitude();
        myLon = myLocation.getLongitude();

        Log.i(TAG,"Location Services Connected: myLat="+myLat+" myLon="+myLon);

        if (trk) {
            startLocationUpdates();
        }
    }

    protected void startLocationUpdates() {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    // Following callback associated with implementing LocationListener.
    // It fires when a location change is detected, passing in the new
    // location as the variable "newLocation".

    @Override
    public void onLocationChanged(Location location) {

    }

    /**
     * Method showTaskDialog() creates a custom alert dialog. This dialog presents text defining
     * a choice to the user and has buttons for a binary choice. Pressing the rightmost button
     * will execute the method positiveTask(id) and pressing the leftmost button will execute the
     * method negativeTask(id). You should define appropriate actions in each. (If the
     * negativeTask(id) method is empty the default action is just to close the dialog window.)
     * The argument id is a user-defined integer distinguishing multiple uses of this method in
     * the same class.  The programmer should switch on id in the response methods
     * positiveTask(id) and negativeTask(id) to decide which alert dialog to respond to.
     * This version of AlertDialog.Builder allows a theme to be specified. Removing the theme
     * argument from the AlertDialog.Builder below will cause the default dialog theme to be used. */

    private void showTaskDialog(int id, String title, String message, int icon, Context context,
                                String positiveButtonText, String negativeButtonText){

        final int fid=id;  // Must be final to access from anonymous inner class below

        AlertDialog.Builder builder = new AlertDialog.Builder(context,R.style.MyDialogTheme);
        builder.setMessage(message).setTitle(title).setIcon(icon);

        // Add the right button
        builder.setPositiveButton(positiveButtonText, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                positiveTask(fid);
            }
        });
        // Add the left button
        builder.setNegativeButton(negativeButtonText, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                negativeTask(fid);
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // Method to be executed if user chooses negative button. This returns to main
    // activity since there is no permission to execute this class.

    private void negativeTask(int id){

        // Use id to distinguish if more than one usage of the alert dialog
        switch(id) {

            case 1:
                // Warning that this part of app not enabled
                String warn ="Returning to main page. To enable this ";
                warn += "part of the app you may manually enable Location permissions in ";
                warn += " Settings > App > MapExample > Permissions.";
                // New single-button dialog
                showTaskDialog(2,"Task not enabled!", warn, dialogIcon, this, "", "OK");
                break;

            case 2:
                // Return to main page since permission was denied
                Intent i = new Intent(this, MainActivity.class);
                startActivity(i);
                break;
        }

    }

    // Method to execute if user chooses positive button ("OK, I'll Do It"). This starts the map
    // initialization, which will present the location permissions dialog again.

    private void positiveTask(int id){

        // Use id to distinguish if more than one usage of the alert dialog
        switch(id) {

            case 1:
                // User agreed to enable location
                initializeLocation();
                break;

            case 2:
                break;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the tool bar if it is present.
        getMenuInflater().inflate(R.menu.showmap_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onMapLongClick(LatLng latlng) {
        double lat = latlng.latitude;
        double lon = latlng.longitude;

        // Launch a StreetView on current location
        showStreetView(lat,lon);
    }

    /* Open a Street View, if available. The user will have the choice of getting the Street View
	 * in a browser, or with the StreetView app if it is installed. If no Street View exists
	 * for a given location, this will present a blank page. */

    private void showStreetView(double lat, double lon ){
        String uriString = "google.streetview:cbll="+lat+","+lon;
        Intent streetView = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(uriString));
        startActivity(streetView);
    }

    /* The following two lifecycle methods conserve resources by ensuring that location services
    are connected when the map is visible and disconnected when it is not.*/

    // Called by system when Activity becomes visible, so connect location client.

    @Override
    protected void onStart() {
        super.onStart();
        if(mGoogleApiClient != null) mGoogleApiClient.connect();
    }

    // Called by system when Activity is no longer visible, so disconnect location
    // client.

    @Override
    protected void onStop() {

        // If the client is connected, remove location updates and disconnect
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        mGoogleApiClient.disconnect();

        // Turn off the screen-always-on request
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        super.onStop();
    }

    // Deal with selections in the options menu

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
}
