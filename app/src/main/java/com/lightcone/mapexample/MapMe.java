package com.lightcone.mapexample;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;


public class MapMe extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        OnMapReadyCallback,
        GoogleMap.OnMapClickListener,
        OnMapLongClickListener, OnMarkerClickListener,
        GoogleMap.OnInfoWindowClickListener {

    // Update interval in milliseconds for location services
    private static final long UPDATE_INTERVAL = 5000;
    // Fastest update interval in milliseconds for location services
    private static final long FASTEST_INTERVAL = 1000;
    // Google Play diagnostics constant
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    // Speed threshold for orienting map in direction of motion (m/s)
    private static final double SPEED_THRESH = 1;
    private static final String TAG = "Mapper";

    // User defines value of REQUEST_LOCATION. It will identify a permission request
    // specifically for ACCESS_FINE_LOCATION.  Define a different integer for each
    // "dangerous" permission to be requested at runtime (in this example there is only one).

    final private int REQUEST_LOCATION = 2;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location myLocation;
    private double myLat;
    private double myLon;
    private GoogleMap map;
    private LatLng map_center;
    private int startZoom = 14;
    private float currentZoom;
    private float bearing;
    private float speed;
    private float acc;
    private Circle localCircle;
    private double lon;
    private double lat;
    static final int numberOptions = 10;
    String[] optionArray = new String[numberOptions];
    private static final int dialogIcon = R.mipmap.ic_launcher;

    // Set up shared preferences to persist data.  We will use it later to save
    // current zoom level if user leaves this activity, and restore it when she returns.

    SharedPreferences prefs;
    SharedPreferences.Editor prefsEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.mapme);

        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar4);
        // Remove default toolbar title and replace with an icon
        if (toolbar != null) {
            toolbar.setNavigationIcon(R.mipmap.ic_launcher);
        }
        // Note: getColor(color) deprecated as of API 23
        toolbar.setTitleTextColor(getResources().getColor(R.color.barTextColor));
        toolbar.setTitle("Map Location");
        setSupportActionBar(toolbar);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        // [by the onMapReady(GoogleMap googleMap) callback].

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapme_map);
        mapFragment.getMapAsync(this);

        /* Create new GoogleApiClient for location services. The first 'this' in args is
		 * present context; the next two 'this' args indicate that this class will handle
		 * callbacks associated with connections and connection errors, respectively
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

        // Get a shared preferences and ShredPreferences editor
        prefs = getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
        prefsEditor = prefs.edit();

        // Keep screen on while this map location tracking activity is running
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    // Callback executed when the map is ready, passing in the map reference googleMap.
    // First set up map, then check for runtime permission on location.

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        setupMap();
        checkForPermissions();
    }

    public void setupMap(){

        // Initialize type of map to normal
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        // Initialize 3D buildings enabled for map view
        map.setBuildingsEnabled(false);

        // Initialize whether indoor maps are shown if available
        map.setIndoorEnabled(false);

        // Initialize traffic overlay
        map.setTrafficEnabled(false);

        // Disable rotation gestures
        map.getUiSettings().setRotateGesturesEnabled(false);

        // Enable zoom controls on map [in addition to gesture controls like spread or double-
        // tap with 1 finger (to zoom in), and pinch or double-tap with two fingers (to zoom out)].

        map.getUiSettings().setZoomControlsEnabled(true);

        // Set the initial zoom level of the map and store in prefs
        currentZoom = startZoom;
        storeZoom(currentZoom);

        // Add a click listener to the map
        map.setOnMapClickListener(this);

        // Add a long-press listener to the map
        map.setOnMapLongClickListener(this);

        // Add Marker click listener to the map
        map.setOnMarkerClickListener(this);

        // Add marker info window click listener
        map.setOnInfoWindowClickListener(this);
    }

    /* Method to check for runtime permissions.  For Android 6 (API 23) and
     beyond, we must check for the "dangerous" permission ACCESS_FINE_LOCATION at
     runtime (in addition to declaring it in the manifest file).  The following code checks
     for this permission.  If it has already been granted, it proceeds as normal.  If it
     has not yet been granted by the user, the user is presented with an opportunity to grant
     it. In general, the rest of this class will not execute until the user has granted such permission. */

    public void checkForPermissions(){

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
            Log.i(TAG, "checkForPermission: Permission has been granted");
        }
    }

    // Create Toolbar menu.

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if present.
        getMenuInflater().inflate(R.menu.mapme_menu, menu);
        return true;
    }


    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Restore previous zoom level (default to max zoom level if
        // no prefs stored)

        if (prefs.contains("KEY_ZOOM") && map != null) {
            currentZoom = prefs.getFloat("KEY_ZOOM", map.getMaxZoomLevel());
        }

        // Keep screen on while this map location tracking activity is running
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }


	/* The following two lifecycle methods conserve resources by ensuring that
	 * location services are connected when the map is visible and disconnected when
	 * it is not. */

    // Called by system when Activity becomes visible, so connect location client.

    @Override
    protected void onStart() {
        super.onStart();
        if(mGoogleApiClient != null) mGoogleApiClient.connect();
    }

    // Called by system when Activity no longer visible, so disconnect client,

    @Override
    protected void onStop() {

        // Save the current zoom level for next session
        if (map != null) {
            currentZoom = map.getCameraPosition().zoom;
            storeZoom(currentZoom);
        }

        // If the client is connected, remove location updates and disconnect
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }

        // Turn off the screen-always-on request
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        super.onStop();
    }

    // The following three callbacks indicate connections, disconnections, and
    // connection errors, respectively.

	/* Called by Location Services when the request to connect the client finishes successfully.
	At this point, you can request current location or begin periodic location updates. */

    @Override
    public void onConnected(Bundle dataBundle) {

        Toast.makeText(this, getString(R.string.connected_toast), Toast.LENGTH_SHORT).show();

        initializeLocation();

        // Center map on current location
        map_center = new LatLng(myLat, myLon);
        if (map != null) {
            initializeMap();
        } else {
            Toast.makeText(this, getString(R.string.nomap_error), Toast.LENGTH_LONG).show();
        }
    }

    // Called after onConnected returns, indicating that GoogleApiClient is ready to
    // accept location requests

    public void initializeLocation(){

        // We already checked for runtime permissions when onMapReady returned, but formally we
        // must check for it again here.

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            return;
        }

        // Request location updates
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);

        // Get current location and move the camera there
        myLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        myLat = myLocation.getLatitude();
        myLon = myLocation.getLongitude();
        map.setMyLocationEnabled(true);

        // Retrieve last used zoom from SharedPreferences
        if (prefs.contains("KEY_ZOOM") && map != null) {
            currentZoom = prefs.getFloat("KEY_ZOOM", map.getMaxZoomLevel());
        }

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(myLat,myLon), currentZoom));
        storeZoom(currentZoom);
    }

    // Method to store current value of zoom in preferences

    private void storeZoom(float zoom){
        prefsEditor.putFloat("KEY_ZOOM", zoom);
        prefsEditor.commit();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    // Called by Location Services if the connection to location client fails

    //@Override
    public void onDisconnected() {
        Toast.makeText(this, getString(R.string.disconnected_toast), Toast.LENGTH_SHORT).show();
    }

    // Called by Location Services if the attempt to connect to Location Services fails.

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

		/* Google Play services can resolve some errors it detects.
		 * If the error has a resolution, try sending an Intent to
		 * start a Google Play services activity that can resolve the error. */

        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
                // Thrown if Google Play services canceled the original PendingIntent
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            // If no resolution is available, display a dialog with the error.
            showErrorDialog(connectionResult.getErrorCode());
        }
    }

    public void showErrorDialog(int errorCode) {
        Log.e(TAG, "Error_Code =" + errorCode);
        // Create an error dialog display here
    }

    // Method to initialize the map. Called after onMapReady returns.

    private void initializeMap() {


        // We already checked for runtime permissions when onMapReady returned, but formally we
        // must check for it again here.

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Move camera view and zoom to location
        map.setMyLocationEnabled(true);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(map_center,currentZoom));
    }

    // Starts location tracking
    private void startTracking(){
        mGoogleApiClient.connect();
        Toast.makeText(this, "Location tracking started", Toast.LENGTH_SHORT).show();
    }

    // Stops location tracking
    private void stopTracking(){
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        Toast.makeText(this, "Location tracking halted", Toast.LENGTH_SHORT).show();
    }

	/* Method to add map marker at give latitude and longitude.  The third arg is a float
	 * variable defining color for the marker.  Pre-defined marker colors may be found at
	 *     http://developer.android.com/reference/com/google/android/gms/maps/model
	 *     /BitmapDescriptorFactory.html
	 * and should be specified in the format BitmapDescriptorFactory.HUE_RED, which is
	 * the default color, but various other ones are defined there such as HUE_ORANGE,
	 * HUE_BLUE, HUE_GREEN, ... The arguments title and snippet are for the window that
	 * will open if one clicks on the marker. */

    private void addMapMarker (double lat, double lon, float markerColor,
                               String title, String snippet){
        if(map != null){
            Marker marker = map.addMarker(new MarkerOptions()
                    .title(title)
                    .snippet(snippet)
                    .position(new LatLng(lat,lon))
                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
            );
            marker.setDraggable(false);
            marker.showInfoWindow();
        } else {
            Toast.makeText(this, getString(R.string.nomap_error), Toast.LENGTH_LONG).show();
        }
    }

    // Decimal output formatting class that uses Java DecimalFormat. See
    // http://developer.android.com/reference/java/text/DecimalFormat.html.
    // The string "formatPattern" specifies the output formatting pattern for
    // the float or double. For example, 35.8577877288 will be returned
    // as the string "35.85779" if formatPattern = "0.00000", and as
    // the string "3.586E01" if formatPattern = "0.000E00".

    public String formatDecimal(double number, String formatPattern){

        DecimalFormat df = new DecimalFormat(formatPattern);

        // The method format(number) with a single argument is inherited by
        // DecimalFormat from NumberFormat.

        return df.format(number);
    }

	/* Method to change properties of camera. Use
	 *
	 * map.getCameraPosition().target
	 * map.getCameraPosition().zoom
	 * map.getCameraPosition().bearing
	 * map.getCameraPosition().tilt
	 *
	 * to get the current values of the camera position (target, which is a LatLng),
	 * zoom, bearing, and tilt, respectively.  This permits changing only a subset of
	 * the camera properties by passing the current values for all arguments you do not
	 * wish to change. */

    private void changeCamera(GoogleMap map, LatLng center, float zoom,
                              float bearing, float tilt, boolean animate) {

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(center)         // Sets the center of the map
                .zoom(zoom)             // Sets the zoom
                .bearing(bearing)       // Sets the bearing of the camera
                .tilt(tilt)             // Sets the tilt of the camera relative to nadir
                .build();               // Creates a CameraPosition from the builder

        // Move (if variable animate is false) or animate (if animate is true) to new
        // camera properties. Move is sudden; animate is smooth.

        if(animate){
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        } else {
            map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    // Following callback associated with implementing LocationListener.
    // It fires when a location change is detected, passing in the new
    // location as the variable "newLocation".

    @Override
    public void onLocationChanged(Location newLocation) {

        bearing = newLocation.getBearing();
        speed = newLocation.getSpeed();
        acc = newLocation.getAccuracy();

        // Get latitude and longitude of updated location
        double lat = newLocation.getLatitude();
        double lon = newLocation.getLongitude();
        LatLng currentLatLng = new LatLng(lat, lon);

        // Return a bundle of additional location information, if available.
        // This will return null if no extras are available, so check for
        // null before using this Bundle.

        Bundle locationExtras = newLocation.getExtras();

        // If there is no satellite info, return -1 for number of satellites.
        // Note that the satellite info generally no longer works with current
        // location providers.  Under Android 24 there will be a new class GnssStatus that
        // will allow detailed location provider information like number of satellites
        // and more. See
        // https://developer.android.com/reference/android/location/GnssStatus.html
        // for documentation.

        int numberSatellites = -1;
        if(locationExtras != null){
            Log.i(TAG, "Extras:"+locationExtras.toString());
            if(locationExtras.containsKey("satellites")){
                numberSatellites = locationExtras.getInt("satellites");
            }
        }

        // Store zoom in Prefs
        storeZoom(map.getCameraPosition().zoom);

        // Log some basic location information
        Log.i(TAG,"Lat="+formatDecimal(lat,"0.00000")
                +" Lon="+formatDecimal(lon,"0.00000")
                +" Bearing="+formatDecimal(bearing, "0.0")
                +" deg Speed="+formatDecimal(speed, "0.0")+" m/s"
                +" Accuracy="+formatDecimal(acc, "0.0")+" m"
                +" Zoom="+map.getCameraPosition().zoom
                +" Sats="+numberSatellites);

        if(map != null) {

            // Animate camera to reflect location update. Orient the view in the
            // direction of motion, but only if the velocity is above a threshold
            // to prevent random rotations of view when velocity direction is
            // not well defined.

            if(speed < SPEED_THRESH) {

                // Smoothly move the camera view to center on the updated location
                // without changing bearing, tilt, or zoom of camera.

                map.animateCamera(CameraUpdateFactory.newLatLng(currentLatLng));

            } else {

                // Animate motion to the updated position and also orient camera in
                // direction of motion using current bearing, keeping the same tilt
                // and zoom.  Note: bearing is the horizontal direction of travel
                // for the device; it has nothing to do with orientation of the device.

                changeCamera(map, currentLatLng, map.getCameraPosition().zoom,
                        bearing, map.getCameraPosition().tilt, true);
            }
        } else {
            Toast.makeText(this, getString(R.string.nomap_error), Toast.LENGTH_LONG).show();
        }
    }


    // Method to reverse-geocode location passed as latitude and longitude. It returns a string which
    // is the first reverse-geocode location in the returned list.  (The full list is output to the
    // logcat stream.) This method returns null if no geocoder backend is available.

    private String reverseGeocodeLocation(double latitude, double longitude){

        // Use to suppress country in returned address for brevity
        boolean omitCountry = true;

        // String to hold single address that will be returned
        String returnString = "";

        Geocoder gcoder = new Geocoder(this);

        // Note that the Geocoder uses synchronous network access, so in a serious application
        // it would be best to put it on a background thread to prevent blocking the main UI if network
        // access is slow. Here we are just giving an example of how to use it so, for simplicity, we
        // don't put it on a separate thread.  See the class RouteMapper in this package for an example
        // of making a network access on a background thread. Geocoding is implemented by a backend
        // that is not part of the core Android framework, so it is not guaranteed to be present on
        // every device.  Thus we use the static method Geocoder.isPresent() to test for presence of
        // the required backend on the given platform.

        try{
            List<Address> results = null;
            if(Geocoder.isPresent()){
                results = gcoder.getFromLocation(latitude, longitude, numberOptions);
            } else {
                Log.i(TAG,"No geocoder accessible on this device");
                return null;
            }
            Iterator<Address> locations = results.iterator();
            String raw = "\nRaw String:\n";
            String country;
            int opCount = 0;
            while(locations.hasNext()){
                Address location = locations.next();
                if(opCount==0 && location != null){
                    lat = location.getLatitude();
                    lon = location.getLongitude();
                }
                country = location.getCountryName();
                if(country == null) {
                    country = "";
                } else {
                    country =  ", "+country;
                }
                raw += location+"\n";
                optionArray[opCount] = location.getAddressLine(0)+", "
                        +location.getAddressLine(1)+country+"\n";
                if(opCount == 0){
                    if(omitCountry){
                        returnString = location.getAddressLine(0)+", "
                                +location.getAddressLine(1)+"\n";
                    } else {
                        returnString = optionArray[opCount];
                    }
                }
                opCount ++;
            }
            // Log the basic information returned
            Log.i(TAG, raw);
            Log.i(TAG,"\nOptions:\n");
            for(int i=0; i<opCount; i++){
                Log.i(TAG,"("+(i+1)+") "+optionArray[i]);
            }
            Log.i(TAG,"lat="+lat+" lon="+lon);

        } catch (IOException e){
            Log.e(TAG, "I/O Failure",e);
        }

        // Return the first location entry in the list.  A more sophisticated implementation
        // would present all location entries in optionArray to the user for choice when more
        // than one is returned by the geodecoder.

        return returnString;
    }


    // Callback that fires when map is tapped, passing in the latitude
    // and longitude coordinates of the tap (actually the point on the ground
    // projected from the screen tap).  This will be invoked only if no overlays
    // on the map intercept the click first.  Here we will just issue a Toast
    // displaying the map coordinates that were tapped.  See the onMapLongClick
    // handler for an example of additional actions that could be taken.

    @Override
    public void onMapClick(LatLng latlng) {
        String f = "0.0000";
        double lat = latlng.latitude;
        double lon = latlng.longitude;
        Toast.makeText(this, "Latitude="+formatDecimal(lat,f)+" Longitude="
                +formatDecimal(lon,f), Toast.LENGTH_LONG).show();
    }

    // This callback fires for long clicks on the map, passing in the LatLng coordinates

    @Override
    public void onMapLongClick(LatLng latlng) {

        double lat = latlng.latitude;
        double lon = latlng.longitude;

        String title = reverseGeocodeLocation(latlng.latitude, latlng.longitude);

        Log.i(TAG,"Reverse geocode="+title);

        String snippet="Tap marker to delete; tap window for Street View";

        // Add an orange marker on map at position of tap (default marker color is red).
        addMapMarker(lat, lon, BitmapDescriptorFactory.HUE_ORANGE, title, snippet);

        // Add a circle centered on the marker given the current position uncertainty
        // Keep a reference to the returned circle so we can remove it later.

        localCircle = addCircle (lat, lon, acc, "#00000000", "#40ff9900");
    }


	/* Add a circle at (lat, lon) with specified radius. Stroke and fill colors are specified
	 * as strings. Valid strings are those valid for the argument of Color.parseColor(string):
	 * for example, "#RRGGBB", "#AARRGGBB", "red", "blue", ...*/

    private Circle addCircle(double lat, double lon, float radius,
                             String strokeColor, String fillColor){

        if(map == null){
            Toast.makeText(this, getString(R.string.nomap_error), Toast.LENGTH_LONG).show();
            return null;
        }

        CircleOptions circleOptions = new CircleOptions()
                .center( new LatLng(lat, lon) )
                .radius( radius )
                .strokeWidth(1)
                .fillColor(Color.parseColor(fillColor))
                .strokeColor(Color.parseColor(strokeColor));

        // Add circle to map and return reference to the Circle for possible later use
        return map.addCircle(circleOptions);
    }

    // Process clicks on markers

    @Override
    public boolean onMarkerClick(Marker marker) {

        // Remove the marker and its info window and circle if marker clicked
        marker.remove();
        localCircle.remove();
        // Return true to prevent default behavior of opening info window
        return true;
    }

    // Process clicks on the marker info window

    @Override
    public void onInfoWindowClick(Marker marker) {

        double lat = marker.getPosition().latitude;
        double lon = marker.getPosition().longitude;

        // Launch a Street View on current location
        showStreetView(lat,lon);

        // Remove marker and circle
        marker.remove();
        localCircle.remove();
    }

	/* Open a Street View, if available. The user will have the choice of getting the Street View
	 * in a browser, or with the StreetView app if it is installed. If no Street View exists for
	 * a given location, this will present a blank page. */

    private void showStreetView(double lat, double lon ){
        String uriString = "google.streetview:cbll="+lat+","+lon;
        Intent streetView = new Intent(android.content.Intent.ACTION_VIEW,Uri.parse(uriString));
        startActivity(streetView);
    }

    /*Following method invoked by the system after the user response to a runtime permission request
     (Android 6, API 23 and beyond implement such runtime permissions). The system passes to this
     method the user's response, which you then should act upon in this method.  This method can respond
     to more than one type permission.  The user-defined integer requestCode (passed in the call to
     ActivityCompat.requestPermissions) distinguishes which permission is being processed. */

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        Log.i(TAG, "Permission result: requestCode=" + requestCode);

        switch(requestCode){

            // The permission response was for fine location
            case REQUEST_LOCATION :

                // If the request was canceled by user, the results arrays are empty
                if(grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                    // Permission was granted. Do the location task that triggered permission request
                    initializeLocation();

                } else {

                    // The permission was denied.  Warn the user of the consequences and give
                    // them one last time to enable the permission.

                    showTaskDialog(1, "Warning!",
                            "This part of the app will not function without this permission!",
                            dialogIcon, this, "OK, Do Over", "Refuse Permission");
                }
                break;
        }
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
     * argument from AlertDialog.Builder below will cause the default dialog theme to be used. */

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

    // Method to execute if user chooses negative button. This returns to main
    // activity since no permission has been given to execute required methods of this class.

    private void negativeTask(int id){

        // Use id to distinguish if more than one usage of the showTaskDialog in same class

        switch(id) {

            case 1:
                String warn = "The purpose of this part of the app is to determine your ";
                warn += "location.  Hence it cannot function unless you grant the requested ";
                warn += "permission. Returning to app homescreen. To enable this ";
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

    // Method to execute if user chooses positive button ("OK, Do Over"). This starts the
    // map initialization, which will present the location permissions dialog again.

    private void positiveTask(int id){

        // Use id to distinguish if more than one usage of the alert dialog
        switch(id) {

            case 1:
                // User agreed to enable so go back to permissions check
                checkForPermissions();
                break;

            case 2:
                break;
        }
    }


    // Handle events in the toolbar menu

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (map == null) {
            Toast.makeText(this, getString(R.string.nomap_error), Toast.LENGTH_LONG).show();
            return false;
        }

        // Handle item selection
        switch (item.getItemId()) {

            // Toggle traffic overlay
            case R.id.traffic_mapme:
                map.setTrafficEnabled(!map.isTrafficEnabled());
                return true;

            // Toggle satellite overlay
            case R.id.satellite_mapme:
                int mt = map.getMapType();
                if (mt == GoogleMap.MAP_TYPE_NORMAL) {
                    map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                } else {
                    map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                }
                return true;

            // Toggle 3D building display
            case R.id.building_mapme:
                map.setBuildingsEnabled(!map.isBuildingsEnabled());
                // Change camera tilt to view from angle if 3D
                if (map.isBuildingsEnabled()) {
                    changeCamera(map, map.getCameraPosition().target, currentZoom,
                            map.getCameraPosition().bearing, 45, true);
                } else {
                    changeCamera(map, map.getCameraPosition().target, currentZoom,
                            map.getCameraPosition().bearing, 0, true);
                }
                return true;

            // Toggle whether indoor maps displayed
            case R.id.indoor_mapme:
                map.setIndoorEnabled(!map.isIndoorEnabled());
                return true;

            // Toggle tracking enabled
            case R.id.track_mapme:
                if (mGoogleApiClient != null) {
                    if (mGoogleApiClient.isConnected()) {
                        stopTracking();
                    } else {
                        startTracking();
                    }
                }
                return true;
            // Settings page
            case R.id.action_settings:
                Intent j = new Intent(this, Settings.class);
                startActivity(j);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}