package idc.searchparty2;

import android.Manifest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
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
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.content.IntentSender.SendIntentException;
import android.support.v4.content.ContextCompat;
import android.os.Looper;

import com.google.android.gms.common.ConnectionResult;
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
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Connections;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.Task;
import android.location.Location;

import static com.google.android.gms.internal.zzaou.onReceive;

public class MapsActivity extends FragmentActivity implements
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    private double latitudeValue = 0.0;
    private double longitudeValue = 0.0;
    private FusedLocationProviderClient mFusedLocationClient;
    private Marker mCurrLocationMarker;
    private LinkedList<LatLng> LLList;

    private String nickname;
    private String typeJoinCreate;
    private final String SERVICE_ID = "kappa";

    private MapsActivity thisClass;

    private final PayloadCallback mPayloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {

                }

                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {

                }
            };

    private final ConnectionLifecycleCallback mConnectionLifecycleCallback =
            new ConnectionLifecycleCallback() {

                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    final String endpoint = endpointId;
                    new AlertDialog.Builder(thisClass)
                            .setTitle("Accept connection to " + connectionInfo.getEndpointName())
                            .setMessage("Confirm if the code " + connectionInfo.getAuthenticationToken() + " is also displayed on the other device")
                            .setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // The user confirmed, so we can accept the connection.
                                    Nearby.Connections.acceptConnection(mGoogleApiClient, endpoint, mPayloadCallback);
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // The user canceled, so we should reject the connection.
                                    Nearby.Connections.rejectConnection(mGoogleApiClient, endpoint);
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    switch (result.getStatus().getStatusCode()) {
                        case ConnectionsStatusCodes.STATUS_OK:
                            // We're connected! Can now start sending and receiving data.
                            Log.i("SELF", "CONNECTION SUCCESSFUL YAY");
                            break;
                        case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                            // The connection was rejected by one or both sides.
                            Log.i("SELF", "CONNECTION UNSUCCESSFUL");
                            break;
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    // We've been disconnected from this endpoint. No more data can be
                    // sent or received.
                }
            };

    private final EndpointDiscoveryCallback mEndpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(
                        String endpointId, DiscoveredEndpointInfo discoveredEndpointInfo) {
                    String name = nickname;
                    Nearby.Connections.requestConnection(
                            mGoogleApiClient,
                            name,
                            endpointId,
                            mConnectionLifecycleCallback)
                            .setResultCallback(
                                    new ResultCallback<Status>() {
                                        @Override
                                        public void onResult(@NonNull Status status) {
                                            if (status.isSuccess()) {
                                                // We successfully requested a connection. Now both sides
                                                // must accept before the connection is established.
                                            } else {
                                                // Nearby Connections failed to request the connection.
                                            }
                                        }
                                    });
                }

                @Override
                public void onEndpointLost(String endpointId) {
                    // A previously discovered endpoint has gone away.
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        Intent intent = getIntent();
        String[] data = intent.getStringArrayExtra(CreateSearch.MESSAGE_NAME);
        if(data[0].equals("0"))
            this.nickname = data[3];
        else
            this.nickname = data[1];
        this.typeJoinCreate = data[0];

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addApi(LocationServices.API)
                    .addApi(Nearby.CONNECTIONS_API)
                    .build();
        }
        mGoogleApiClient.connect();

        mLocationRequest = createLocationRequest();
        //mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        LLList = new LinkedList<LatLng>();

        Log.i("f", String.valueOf(mGoogleApiClient.isConnected()));
        thisClass = this;
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
        this.startAdvertising();
        if(typeJoinCreate.equals("1")){
            startDiscovery();
        }
        Log.i("onConnected","SearchMap Connected");
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult result) {
                final Status status = result.getStatus();
                Log.i("onResult","SearchMap Connected " + status.getStatusCode());
                Log.i("drawLines",""+(ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED));
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        //mMap.setMyLocationEnabled(true);
                        startLocationUpdates();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        ActivityCompat.requestPermissions(MapsActivity.this,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                1);
                        //mMap.setMyLocationEnabled(true);
                        startLocationUpdates();
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        break;
                }
            }
        });
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
        if(node != null)
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

    private void startAdvertising(){
        Nearby.Connections.startAdvertising(
                mGoogleApiClient,
                this.nickname == null ? "no nickname" : this.nickname,
                SERVICE_ID,
                mConnectionLifecycleCallback,
                new AdvertisingOptions(Strategy.P2P_CLUSTER))
                .setResultCallback(
                        new ResultCallback<Connections.StartAdvertisingResult>() {
                            @Override
                            public void onResult(@NonNull Connections.StartAdvertisingResult result) {
                                if (result.getStatus().isSuccess()) {
                                    // We're advertising!
                                    Log.i("SELF ADVERTISING", "CONNECTED");
                                } else {
                                    // We were unable to start advertising.
                                    Log.i("SELF ADVERTISING", "NOT CONNECTED");
                                }
                            }
                        });
    }


    private void startDiscovery() {
        Nearby.Connections.startDiscovery(
                mGoogleApiClient,
                SERVICE_ID,
                mEndpointDiscoveryCallback,
                new DiscoveryOptions(Strategy.P2P_CLUSTER))
                .setResultCallback(
                        new ResultCallback<Status>() {
                            @Override
                            public void onResult(@NonNull Status status) {
                                if (status.isSuccess()) {
                                    // We're discovering!
                                    Log.i("SELF DISCOVERING", "DISCOVERING");
                                } else {
                                    // We were unable to start discovering.
                                    Log.i("SELF DISCOVERING", "NOT DISCOVERING");
                                }
                            }
                        });
    }





    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
