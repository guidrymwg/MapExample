package com.lightcone.mapexample;

import java.net.MalformedURLException;
import java.net.URL;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;


public class RouteMapper extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleMap.OnMarkerClickListener, OnMapReadyCallback,
        GoogleMap.OnInfoWindowClickListener,
        GoogleMap.OnMapLongClickListener,
        com.google.android.gms.maps.GoogleMap.OnMapClickListener {

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private static final String TAG = "Mapper";
    private GoogleApiClient locationClient;
    private GoogleMap map;
    private LatLng map_center = new LatLng(35.955, -83.9275);
    private int zoomOffset = 5;

    private int numberRoutePoints = -1;
    private int totalWaypoints = -1;
    private LatLng routePoints[];
    private int routeGrade[];
    private Polyline[] route;
    private String warnPointSnippet[];
    private LatLng warnPointLatLng[];

    private static final int numberAccessMarkers = 4;
    private static final int numberFoodMarkers = 3;

    private LatLng[] accessLoc = new LatLng[numberAccessMarkers];
    private Marker[] accessMarker = new Marker[numberAccessMarkers];
    private String[] accessMarkerTitle = new String[numberAccessMarkers];
    private String[] accessMarkerSnippet = new String[numberAccessMarkers];
    private Uri[] uriAccess = new Uri[numberAccessMarkers];

    private LatLng[] foodLoc = new LatLng[numberFoodMarkers];
    private Marker[] foodMarker = new Marker[numberFoodMarkers];
    private String[] foodMarkerTitle = new String[numberFoodMarkers];
    private String[] foodMarkerSnippet = new String[numberFoodMarkers];
    private Uri[] uriFood = new Uri[numberFoodMarkers];

    private boolean accessInitiallyVisible = false;
    private boolean foodInitiallyVisible = false;
    private boolean accessIsVisible = false;
    private boolean foodIsVisible = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.routemapper);

        // Create top toolbar

        Toolbar toolbar = (Toolbar) findViewById(R.id.route_map);
        // Remove default toolbar title and replace with an icon
        if (toolbar != null) {
            toolbar.setNavigationIcon(R.mipmap.ic_launcher);
        }
        // Note: getColor(color) deprecated as of API 23
        toolbar.setTitleTextColor(getResources().getColor(R.color.barTextColor));
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        // Get a handle to the Map Fragment

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.the_map);
        mapFragment.getMapAsync(this);

		/* Create a new GoogleApiClient instance. The first 'this' in the args is the
		 * present context; the next two indicate that the present class will handle
		 * the callbacks associated with connection and connection errors, respectively
		 * (see the onConnected, onDisconnected, and onConnectionError callbacks below).
		 * You cannot use the location client until the onConnected callback
		 * fires, indicating a valid connection.  At that point you can access location
		 * services such as present position and location updates.
		 */

        locationClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

    }


    // Inflate toolbar menu.  Actions handled below in onOptionsItemSelected method.

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.routemapper_menu, menu);
        return true;
    }

    // Fired by system when map fragment is ready.
    @Override
    public void onMapReady(GoogleMap googleMap) {

        map=googleMap;

        // Add a click listener to the map
        map.setOnMapClickListener(this);

        // Add a long-press listener to the map
        map.setOnMapLongClickListener(this);

        // Add symbol overlays (initially invisible)
        addAccessSymbols();
        addFoodSymbols();
    }


    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

	/* The following two lifecycle methods conserve resources by ensuring that location
	 * services are connected when the map is visible and disconnected when not. */

    // Called by system when Activity becomes visible, so connect location client.

    @Override
    protected void onStart() {
        super.onStart();
        locationClient.connect();
    }

    // Called by system when Activity is no longer visible, so disconnect location
    // client, which invalidates it.

    @Override
    protected void onStop() {
        locationClient.disconnect();
        super.onStop();
    }

    // The following three callbacks indicate connections, disconnections, and
    // connection errors, respectively.


	/* Called by Location Services when the request to connect the
	 * client finishes successfully. At this point, you can
	 * request current location or begin periodic location updates. */

    @Override
    public void onConnected(Bundle dataBundle) {

        // Display the connection status
        Toast.makeText(this, getString(R.string.connected_toast), Toast.LENGTH_SHORT).show();
        if (map != null) {
            initializeMap();
        } else {
            Toast.makeText(this, getString(R.string.nomap_error), Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    // Called by Location Services if the connection to the location client drops out

    //@Override
    public void onDisconnected() {
        // Display the connection status
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
                // Log the error
                e.printStackTrace();
            }
        } else {
            // If no resolution is available, display a dialog with the error.
            showErrorDialog(connectionResult.getErrorCode());
        }
    }

    public void showErrorDialog(int errorCode) {
        Log.e(TAG, "Error_Code =" + errorCode);
    }

    // Method to initialize the map.

    private void initializeMap() {

        // Move camera view and zoom to location
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(map_center,
                map.getMaxZoomLevel()-zoomOffset));

        // Initialize type of map
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        // Initialize 3D buildings enabled for map view
        map.setBuildingsEnabled(false);

        // Initialize whether indoor maps are shown if available
        map.setIndoorEnabled(false);

        // Initialize traffic overlay
        map.setTrafficEnabled(false);

        // Add map marker click listener
        map.setOnMarkerClickListener(this);

        // Add map maker info window click listener
        map.setOnInfoWindowClickListener(this);

        // Disable rotation gestures
        map.getUiSettings().setRotateGesturesEnabled(false);

        // Enable zoom controls on map [in addition to gesture controls like spread or double-
        // tap with 1 finger (to zoom in), and pinch or double-tap with two fingers (to zoom out)].

        map.getUiSettings().setZoomControlsEnabled(true);

    }

    // Method to read and parse route data from server as XML.  The network request
    // will be executed on a background thread to avoid locking up the UI
    // if the network response is slow. Once the data are returned over the network,
    // the background thread will call the method overlayRoute() to overlay the route
    // on the map.

    public void loadRouteData(){

        // The constructor new URL(url) throws MalformedURLException, so must enclose
        // everything in a try-catch to handle the exception.

        try {

            // Specify the URL of the server program producing the XML
            String url = "http://eagle.phys.utk.edu/reubendb/UTRoute.php";

            // Specify the starting and ending points of route in GET data string.
            // Lat and long expected in microdegrees for server program (divide by 1e6
            // to get corresponding values in degrees).

            String data = "?lat1=35952967&lon1=-83929158&lat2=35956567&lon2=-83925450";

            // Execute the request on a background thread
            new RouteLoader().execute(new URL(url+data));

        } catch (MalformedURLException e) {
            Log.i(TAG, "Failed to generate valid URL");
        }
    }


    // Overlay a route.  This method is executed only after loadRouteData() completes
    // on background thread.

    public void overlayRoute() {

        int lw = 10;

        // Define the route as a line with multiple segments
        route = new Polyline[numberRoutePoints];
        PolylineOptions routeOptions;

        // Color legend for path slope.  Increasing values of the index
        // indicate steeper paths

        int[] slopeColor = new int[4];
        slopeColor[0] = Color.parseColor("#cc5ea2c6");
        slopeColor[1] = Color.parseColor("#ccebc05c");
        slopeColor[2] = Color.parseColor("#ccbb6255");
        slopeColor[3] = Color.parseColor("#ccd27451");

        // Add each segment of the route to the map with appropriate color
        for (int i = 0; i < numberRoutePoints - 1; i++) {
            routeOptions = new PolylineOptions().width(lw);
            routeOptions.add(routePoints[i]);
            routeOptions.add(routePoints[i + 1]);
            routeOptions.color(slopeColor[routeGrade[i] - 1]);
            route[i] = map.addPolyline(routeOptions);
        }

        // Circle at beginning of route
        CircleOptions circleOptions = new CircleOptions()
                .center(routePoints[0])
                .radius(6)
                .strokeWidth(0)
                .strokeColor(slopeColor[0])
                .fillColor(Color.DKGRAY)
                .zIndex(100);
        map.addCircle(circleOptions);

        // Circle at end of route
        circleOptions = circleOptions.center(routePoints[numberRoutePoints - 1]);
        map.addCircle(circleOptions);

        // Add warning areas

        for (int i = 0; i < totalWaypoints; i++) {
            map.addMarker(new MarkerOptions()
                    .title("WARNING")
                    .snippet(warnPointSnippet[i])
                    .position(warnPointLatLng[i])
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            );
        }
    }

    // Method to overlay access symbols

    public void addAccessSymbols(){

        // Set up access markers overlay

        accessLoc[0] = new LatLng(35.953700,-83.926158);
        accessLoc[1] = new LatLng(35.954000,-83.928200);
        accessLoc[2] = new LatLng(35.955000,-83.927558);
        accessLoc[3] = new LatLng(35.954000,-83.927158);

        accessMarkerTitle[0] = "Access Marker 1";
        accessMarkerTitle[1] = "Access Marker 2";
        accessMarkerTitle[2] = "Access Marker 3";
        accessMarkerTitle[3] = "Access Marker 4";

        accessMarkerSnippet[0] = "Access snippet 1";
        accessMarkerSnippet[1] = "Access snippet 2";
        accessMarkerSnippet[2] = "Access snippet 3";
        accessMarkerSnippet[3] = "Access snippet 4";

        uriAccess[0] = Uri.parse("http://www.google.com");
        uriAccess[1] = Uri.parse("http://whitehouse.gov");
        uriAccess[2] = Uri.parse("http://www.amazon.com");
        uriAccess[3] = Uri.parse("http://www.stackoverflow.com");

        for(int i=0; i<numberAccessMarkers; i++){
            accessMarker[i] = map.addMarker(new MarkerOptions()
                    .position(accessLoc[i])
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.accessibility))
                    .draggable(false)
                    .alpha(0.7f)
                    .snippet(accessMarkerSnippet[i])
                    .title(accessMarkerTitle[i]));
            accessMarker[i].setVisible(accessInitiallyVisible);
        }

    }

    // Method to toggle visibility of access symbols

    public void toggleAccessSymbols(){

        accessIsVisible = !accessIsVisible;
        for (int i=0; i<numberAccessMarkers; i++){
            accessMarker[i].setVisible(accessIsVisible);
        }

    }

    // Method to overlay food symbols

    public void addFoodSymbols(){

        // Set up food markers overlay

        Log.i(TAG,"Adding food symbols");

        foodLoc[0] = new LatLng(35.952967,-83.929158);
        foodLoc[1] = new LatLng(35.953000,-83.928000);
        foodLoc[2] = new LatLng(35.955000,-83.929158);

        foodMarkerTitle[0]="Food Marker 1";
        foodMarkerTitle[1]="Food Marker 2";
        foodMarkerTitle[2]="Food Marker 3";

        foodMarkerSnippet[0] = "Food snippet 1";
        foodMarkerSnippet[1] = "Food snippet 2";
        foodMarkerSnippet[2] = "Food snippet 3";

        uriFood[0] = Uri.parse("http://antwrp.gsfc.nasa.gov/apod/astropix.html");
        uriFood[1] = Uri.parse("http://www.youtube.com");
        uriFood[2] = Uri.parse("http://www.kayak.com");

        for(int i=0; i<numberFoodMarkers; i++){
            foodMarker[i] = map.addMarker(new MarkerOptions()
                    .position(foodLoc[i])
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.knifefork_small))
                    .draggable(false)
                    .alpha(0.7f)
                    .snippet(foodMarkerSnippet[i])
                    .title(foodMarkerTitle[i]));
            foodMarker[i].setVisible(foodInitiallyVisible);
        };
    }

    // Method to toggle visibility of food symbols

    public void toggleFoodSymbols(){

        foodIsVisible = !foodIsVisible;
        for(int i=0; i<numberFoodMarkers; i++){
            foodMarker[i].setVisible(foodIsVisible);
        }
    }

    // Handle click events on markers

    @Override
    public boolean onMarkerClick(Marker marker) {

        Log.i(TAG, "Marker ID="+marker.getId());
        Log.i(TAG, "Marker title="+marker.getTitle());

        // By returning false we get the default behavior, which is to open a window
        // displaying the title and snippet associated with this marker. If instead
        // we handle the marker click in a custom way, we should return true.

        return false;
    }

    // Handle clicks on info windows that pop up when markers are clicked.  First
    // identify the marker associated with the window by title and then take
    // appropriate action.  In this case we illustrate by opening different
    // web pages that have no connection to the symbols, but in a realistic application
    // these would be links to relevant information for the symbol.

    @Override
    public void onInfoWindowClick(Marker marker) {

        int markerIndex = -1;

        Log.i(TAG, "Info window:  Marker ID="+marker.getId());
        Log.i(TAG, "Info window: Marker title="+marker.getTitle());

        // Check whether it is an access marker

        for(int i=0; i<numberAccessMarkers; i++){
            if(marker.getTitle().equals(accessMarkerTitle[i])){
                markerIndex = i;
                Log.i(TAG,"Index of access marker whose window was clicked="+markerIndex);
                Intent j = new Intent(Intent.ACTION_VIEW);
                j.setData(uriAccess[markerIndex]);
                startActivity(j);
                marker.hideInfoWindow();
                break;
            }
        }

        // Check whether it is a food marker

        for(int i=0; i<numberFoodMarkers; i++){
            if(marker.getTitle().equals(foodMarkerTitle[i])){
                markerIndex = i;
                Log.i(TAG,"Index of food marker whose window was clicked="+markerIndex);
                Intent j = new Intent(Intent.ACTION_VIEW);
                j.setData(uriFood[markerIndex]);
                startActivity(j);
                marker.hideInfoWindow();
                break;
            }
        }
    }

    // Callback that executes when map is tapped, passing in the latitude
    // and longitude coordinates of the tap.  This will be invoked
    // only if no overlays on the map intercept the click first.

    @Override
    public void onMapClick(LatLng latlng) {
        Log.i(TAG, "Map tapped: Latitude="+latlng.latitude
                +" Longitude="+latlng.longitude);
    }

    // This callback fires for long clicks on the map, passing in the LatLng coordinates
    // of the click.  We will use it to launch a StreetView of the position corresponding to the
    // long press on the map.

    @Override
    public void onMapLongClick(LatLng latlng) {

        double lat = latlng.latitude;
        double lon = latlng.longitude;

        // Launch a Google StreetView on current location
        showStreetView(lat,lon);

    }

     /* Open a Street View, if available. The user will have the choice of getting the Street View
	 in a browser, or with the StreetView app if it is installed. If no Street View exists for a
	 given location, this will present a blank page. */

    private void showStreetView(double lat, double lon ){
        String uriString = "google.streetview:cbll="+lat+","+lon;
        Intent streetView = new Intent(android.content.Intent.ACTION_VIEW,Uri.parse(uriString));
        startActivity(streetView);
    }

    // Handle clicks on toolbar menus

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (map == null) {
            Toast.makeText(this, getString(R.string.nomap_error), Toast.LENGTH_LONG).show();
            return false;
        }

        // Handle item selection
        switch (item.getItemId()) {
            // Load route
            case R.id.route_toggle:
                loadRouteData();
                return true;
            // Toggle satellite overlay
            case R.id.satellite_route:
                int mt = map.getMapType();
                if (mt == GoogleMap.MAP_TYPE_NORMAL) {
                    map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                } else {
                    map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                }
                return true;
            // Toggle access markers
            case R.id.hc_toggle:
                toggleAccessSymbols();
                return true;
            // Toggle eating markers
            case R.id.eat_toggle:
                toggleFoodSymbols();
                return true;
            // Settings page
            case R.id.route_action_settings:
                // Actions for settings page
                Intent j = new Intent(this, Settings.class);
                startActivity(j);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


	/* Inner class to implement single task on background thread without having to manage
	the threads directly. Launch with "new RouteLoader().execute(new URL(urlString)".
	Must be launched from the UI thread and may only be invoked once.  Adapted from
	example in Ch. 10 of Android Wireless Application Development. Use this to do data
	load from network on separate thread from main user interface to prevent locking
	main UI if there is network delay. The three argument types inside the < > are
	(1) a type for the input parameters (URL in this case), (2) a type for any published
	progress during the background task (String in this case), and (3) a type for the object
	returned from the background task (in this case it is type String). Each of these is
	understood to be an array of the corresponding type, so each can hold multiple entries. */

    private class RouteLoader extends AsyncTask<URL, String, String> {

        @Override
        protected String doInBackground(URL... params) {

            // params is an array of URLs, but we need only the first entry since we are
            // passing just one argument in new RouteLoader().execute(new URL(urlString)

            try {
                URL text = params[0];

                XmlPullParserFactory parserCreator;

                parserCreator = XmlPullParserFactory.newInstance();

                XmlPullParser parser = parserCreator.newPullParser();

                parser.setInput(text.openStream(), null);

                publishProgress("Parsing XML...");

                int parserEvent = parser.getEventType();
                int pointCounter = -1;
                int wptCounter = -1;

                double lat;
                double lon;
                int grade = -1;

                // Parse the XML returned on the network
                while (parserEvent != XmlPullParser.END_DOCUMENT) {
                    switch (parserEvent) {
                        case XmlPullParser.START_TAG:
                            String tag = parser.getName();
                            if(tag.compareTo("number")==0){
                                numberRoutePoints = Integer.parseInt(parser.getAttributeValue(null,"numpoints"));
                                totalWaypoints = Integer.parseInt(parser.getAttributeValue(null,"numwpts"));
                                routePoints = new LatLng[numberRoutePoints];
                                routeGrade = new int[numberRoutePoints];
                                Log.i(TAG, "   Total points = "+numberRoutePoints
                                        +" Total waypoints = "+totalWaypoints);

                                warnPointSnippet = new String[totalWaypoints];
                                warnPointLatLng = new LatLng[totalWaypoints];
                            }
                            if(tag.compareTo("trkpt")==0){
                                pointCounter ++;

                                // Divide by 1e6 because on server in microdegrees and need degrees here
                                lat = Double.parseDouble(parser.getAttributeValue(null,"lat"))/1e6;
                                lon = Double.parseDouble(parser.getAttributeValue(null,"lon"))/1e6;

                                grade = Integer.parseInt(parser.getAttributeValue(null,"grade"));
                                routePoints[pointCounter] = new LatLng(lat, lon);
                                routeGrade[pointCounter] = grade;
                                Log.i(TAG,"   trackpoint="+pointCounter+" latitude="+lat+" longitude="+lon
                                        +" grade="+grade);
                            } else if(tag.compareTo("wpt")==0) {

                                // Store waypoint information about potential hazards
                                wptCounter ++;
                                // Divide by 1e6 because on server in microdegrees and need degrees here
                                lat = Double.parseDouble(parser.getAttributeValue(null,"lat"))/1e6;
                                lon = Double.parseDouble(parser.getAttributeValue(null,"lon"))/1e6;
                                warnPointLatLng[wptCounter] = new LatLng(lat,lon);
                                warnPointSnippet[wptCounter] = parser.getAttributeValue(null,"description");
                                Log.i(TAG,"   waypoint="+wptCounter+" latitude="+lat+" longitude="+lon
                                        +" "+warnPointSnippet[wptCounter]);
                            }
                            break;
                    }
                    parserEvent = parser.next();
                }

            } catch (Exception e) {
                Log.i(TAG, "XML parsing failed", e);
                return "Failure";
            }

            return "Finished";
        }

        protected void onCancelled() {
            Log.i(TAG, "RouteLoader task Cancelled");
        }

        // Executed after the thread run by doInBackground has returned. The variable result
        // passed is the string value returned by doInBackground.

        protected void onPostExecute(String result) {
            // Now that route data are loaded, execute the method to overlay the route on the map
            Log.i(TAG, "Route data transfer complete");
            overlayRoute();
        }

        // Executes before the thread run by doInBackground.  This can be used to do any required
        // setup before the thread is executed.

        protected void onPreExecute() {
            Log.i(TAG,"Ready to load URL");
        }

        // May be used to update progress as the background thread executes.  We won't do anything
        // with it here since we are loading a very small XML file over the network but for a long
        // background task this can be used to keep the user informed of progress on the background
        // task.

        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            Log.i(TAG,"Progress:"+values[0]);
        }
    }
}
