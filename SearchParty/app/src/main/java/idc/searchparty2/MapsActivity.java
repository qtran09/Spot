package idc.searchparty2;

import android.Manifest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
//import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.content.IntentSender.SendIntentException;
import android.content.IntentSender;
import android.support.v4.content.ContextCompat;
import android.os.Looper;


import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.tasks.Task;
import android.location.Location;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, LocationListener {

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    private double latitudeValue = 0.0;
    private double longitudeValue = 0.0;
    private FusedLocationProviderClient mFusedLocationClient;
    private Marker mCurrLocationMarker;
    private LinkedList<LatLng> LLList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addApi(LocationServices.API)
                    .build();
        }mGoogleApiClient.connect();
        Log.i("onCreate","Connected?: "+ mGoogleApiClient.isConnected());
        mLocationRequest = createLocationRequest();
        //mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        LLList = new LinkedList<LatLng>();
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        Log.i("main","mainfunction");
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i("onConnected","SearchMap Connected");
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        Log.i("onConnected","Connected?: "+ mGoogleApiClient.isConnected());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult result) {
                final Status status = result.getStatus();
                Log.i("onResult","SearchMap Connected " + status.getStatusCode());

                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        //mMap.setMyLocationEnabled(true);
                        requestPermissions();
                        startLocationUpdates();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        //mMap.setMyLocationEnabled(true);
                        try {
                        status.startResolutionForResult(
                                MapsActivity.this, 1000);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        requestPermissions();
                        startLocationUpdates();
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        break;
                }
            }
        });
    }

    private void requestPermissions(){
        if(ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MapsActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1);}
    }
    public void startLocationUpdates(){
        Log.i("drawLines","drawLines");
        Log.i("drawLines",""+(ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED));

        if (ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            Log.i("drawLines","requestingupdates");
//            LocationServices.FusedLocationApi.setMockMode(mGoogleApiClient, true);
//            Location mockloc = new Location("");
//            mockloc.setLatitude(0.0d);
//            mockloc.setLongitude(1.1d);
//            LocationServices.FusedLocationApi.setMockLocation(mGoogleApiClient, mockloc);

            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

//            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
//            Log.i("onLocationChanged",""+mLastLocation);
//
//            if (mLastLocation != null) {
//                Log.i("onLocationChanged",String.valueOf(mLastLocation.getLatitude()));
//                Log.i("onLocationChanged",String.valueOf(mLastLocation.getLongitude()));
//            }
        }
        else{
            try{
                TimeUnit.SECONDS.sleep(3);}
            catch(java.lang.InterruptedException e){

            }
            startLocationUpdates();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        Log.i("onLocationChangedLat",String.valueOf(mLastLocation.getLatitude()));
        Log.i("onLocationChangedLong",String.valueOf(mLastLocation.getLongitude()));
//        mMap.addCircle(new CircleOptions()
//                .center(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()))
//                .radius(25)
//                .strokeWidth(5)
//                .strokeColor(Color.BLACK)
//                .fillColor(Color.argb(25, 0, 0, 255)));
//        CameraPosition cameraPosition = new CameraPosition.Builder()
//                .target(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()))
//                .zoom(17)
//                .bearing(90)
//                .tilt(40)
//                .build();
//        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        //mMap.clear();
        LatLng node = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        LLList.add(node);
        drawCircles(LLList);

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()))
                .zoom(17)
                .bearing(90)
                .tilt(40)
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    private void drawCircles(LinkedList<LatLng> Positions){
        for(LatLng L : Positions){
            int offset = (255/Positions.size()) * Positions.indexOf(L);
            //Log.i("drawCircles",Arrays.toString(LLList.toArray()));
            Log.i("drawCircles",Positions.size()+" "+Positions.indexOf(L));
            Log.i("drawCircles2",""+offset);
            mMap.addCircle(new CircleOptions()
                    .center(L)
                    .radius(10)
                    .strokeWidth(5)
                    .strokeColor(Color.BLACK)
                    .fillColor(Color.argb(50, 0+offset, 0, 255-offset)));
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1 : {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    break;
                } else {
                    System.exit(0);
                }
            }
        }
    }

    public void onConnectionSuspended(int i) {
    }
    protected LocationRequest createLocationRequest() {
        LocationRequest temp = new LocationRequest();
        temp.setInterval(5000);
        temp.setFastestInterval(3000);
        temp.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return temp;
    }


    protected void onStart() {
        Log.i("onStart","SearchMap Started");
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }
}
