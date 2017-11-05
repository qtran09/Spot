package idc.searchparty2;

//Look at these beautiful imports
import android.Manifest;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.content.IntentSender;
import android.support.v4.content.ContextCompat;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.CircleOptions;
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
import android.location.Location;
import android.view.View;

/**
 *  This class is the class that hosts the entirety of the functionality of the map and its
 *  listeners. The functionality of this class can be split into two parts, one for each usage of
 *  the crucial APIs used in this class: Location and Nearby.
 *
 *  Location takes a Google API key and fetches requests for the user's current location processes
 *  relevant information regarding the location such as the user's change in position and the user's
 *  position in Latitude and Longitude, granted that the user allows and enables the required
 *  permissions required for this functionality. Furthermore, additional related functionality is
 *  added to draw in circles specifying where each user has been before, notifying all other users
 *  in the group or connection.
 *
 *  Nearby serves as the methodology used by the application to communicate with other devices using
 *  the same application. Nearby functionality can be split into two parts as well: Advertising and
 *  Discovery. Advertising is used by the applications that are the initial creators of the group or
 *  connection and simply broadcasts the existence of the application to be picked up by other
 *  devices. This part will also alert the user if a discovering application wishes to establish a
 *  connection, prompting the user who may accept or reject that request and adding another tier of
 *  security. Discovery simply detects advertising devices and requests access into the group. After
 *  a connection is made, the locations of every user will be visible to every other user.
 *
 *  This class implements interfaces that specify the map functionality, result callbacks for
 *  connections, and listeners.
 */
public class MapsActivity extends FragmentActivity implements
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    private Marker mCurrLocationMarker;
    private LinkedList<LatLng> LLList;
    private LinkedList<String> endpointIDs;

    private String nickname;
    private String typeJoinCreate;
    private final String SERVICE_ID = "Search.Party.com";

    private double[] coords;

    private final PayloadCallback mPayloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {
                    coords = toDoubleArray(payload.asBytes());
                    LatLng node = new LatLng(coords[0], coords[1]);
                    mMap.addCircle(new CircleOptions()
                            .center(node)
                            .radius(5)
                            .strokeWidth(5)
                            .strokeColor(Color.BLACK)
                            .fillColor(Color.argb(50, 255, 0, 0)));

                }

                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                    Log.i("SELF", "onPayloadTransferUpdate called: " + update.getStatus());
                }
            };
    private double[] toDoubleArray(byte[] arr){
        int times = Double.SIZE/Byte.SIZE;
        double[] doubles = new double[arr.length/times];
        for(int i=0;i<doubles.length;i++){
            doubles[i] = ByteBuffer.wrap(arr,i*times,times).getDouble();
        }
        return doubles;
    }
    private byte[] toByteArray(double[] doubleArray){
        int times = Double.SIZE / Byte.SIZE;
        byte[] bytes = new byte[doubleArray.length * times];
        for(int i=0;i<doubleArray.length;i++){
            ByteBuffer.wrap(bytes, i*times, times).putDouble(doubleArray[i]);
        }
        return bytes;
    }

    /**
     *  This object is instrumental with regards to the implementation of advertising devices. It
     *  specifies the details of what happens when a connection is being attempted. Specifically, it
     *  will alert the user if a connection is being attempted by a discovering device, sending back
     *  the user appropriated response.
     */
    private final ConnectionLifecycleCallback mConnectionLifecycleCallback =
            new ConnectionLifecycleCallback() {

                /**
                 *  Executes when a connection is being attempted, alerts the user, awaits a user
                 *  response, sending back an error if the user denies the attempt or establishing
                 *  a connection otherwise.
                 *
                 * @param endpointId        The unique ID of the discovering device attempting the
                 *                          connection
                 * @param connectionInfo    A packet with information regarding the discovering
                 *                          device
                 */
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    Log.i("SELF", "onConnectionInitiated");
                    final String endpoint = endpointId;
                    new AlertDialog.Builder(MapsActivity.this)
                            .setTitle("Accept connection to " + connectionInfo.getEndpointName())
                            .setMessage("Confirm if the code " + connectionInfo.getAuthenticationToken() + " is also displayed on the other device")
                            .setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // The user confirmed, so we can accept the connection.
                                    Nearby.Connections.acceptConnection(mGoogleApiClient, endpoint, mPayloadCallback);
                                    endpointIDs.add(endpoint);
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

                /**
                 *  This method executes when a result has been reached regarding the connection
                 *  attempt. It will simply log the result of connection
                 *
                 * @param endpointId    The unique ID of the discovering device attempting the
                 *                      connection
                 * @param result        A result object specifying whether there was a success or
                 *                      not
                 */
                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    Log.i("SELF", "onConnectionResult");
                    switch (result.getStatus().getStatusCode()) {
                        case ConnectionsStatusCodes.STATUS_OK:
                            // We're connected! Can now start sending and receiving data.
                            Log.i("SELF", "CONNECTION WITH OTHER SUCCESSFUL YAY");
                            break;
                        case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                            // The connection was rejected by one or both sides.
                            Log.i("SELF", "CONNECTION WITH OTHER UNSUCCESSFUL");
                            break;
                    }
                }

                /**
                 *  Executes when a discovering device disconnects
                 *
                 * @param endpointId    The unique ID of the disconnecting device
                 */
                @Override
                public void onDisconnected(String endpointId) {
                    Log.i("SELF", "onDisconnected");
                    // We've been disconnected from this endpoint. No more data can be
                    // sent or received.
                }
            };

    /**
     *  This object is important in the implementation of discovering devices. It specifies what
     *  will happen once an advertising device is discovered. The discovering device will send a
     *  request to the advertising application with information regarding the device and will
     *  log a message specifying a success or failure.
     */
    private final EndpointDiscoveryCallback mEndpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {


                @Override
                public void onEndpointFound(
                        String endpointId, DiscoveredEndpointInfo discoveredEndpointInfo) {
                    Log.i("SELF", "ENDPOINT FOUND");
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
                                                Log.i("SELF", "REQUESTED CONNECTION");
                                            } else {
                                                // Nearby Connections failed to request the connection.
                                                Log.i("SELF", "FAILED REQUESTED CONNECTION");

                                            }
                                        }
                                    });
                }

                @Override
                public void onEndpointLost(String endpointId) {
                    // A previously discovered endpoint has gone away.
                    Log.i("SELF", "ENDPOINT LOST");

                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        Intent intent = getIntent();
        String[] data_c = intent.getStringArrayExtra(CreateSearch.MESSAGE_NAME);
        String[] data_j = intent.getStringArrayExtra(JoinSearch.MESSAGE_NAME_JOIN);
        if(data_c == null){
            this.nickname = data_j[1];
            this.typeJoinCreate = data_j[0];
        }
        else{
            this.nickname = data_c[1];
            this.typeJoinCreate = data_c[0];
        }
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

        if (ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MapsActivity.this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    1);
        }
        mLocationRequest = createLocationRequest();
        endpointIDs = new LinkedList<String>();
        LLList = new LinkedList<LatLng>();

        Log.i("f", String.valueOf(mGoogleApiClient.isConnected()));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        Log.i("main","mainfunction");
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if(typeJoinCreate.equals("1")){
            this.startDiscovery();
        }
        else{
            this.startAdvertising();
        }
//        this.startDiscovery();
//        this.startAdvertising();

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

            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

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

        if (!endpointIDs.isEmpty()) {
            double[] latlng = new double[]{mLastLocation.getLatitude(), mLastLocation.getLongitude()};
            final PendingResult<Status> statusPendingResult = Nearby.Connections.sendPayload(mGoogleApiClient, endpointIDs, Payload.fromBytes(toByteArray(latlng))); //?
        }

        Log.i("onLocationChangedLat", String.valueOf(mLastLocation.getLatitude()));
        Log.i("onLocationChangedLong", String.valueOf(mLastLocation.getLongitude()));

        LatLng node = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        mMap.addCircle(new CircleOptions()
                .center(node)
                .radius(5)
                .strokeWidth(5)
                .strokeColor(Color.BLACK)
                .fillColor(Color.argb(50, 0, 0, 255)));

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()))
                .zoom(18)
                .tilt(40)
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

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
        temp.setInterval(3000);
        temp.setFastestInterval(2000);
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
                new AdvertisingOptions(Strategy.P2P_STAR))
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
//        ActivityCompat.requestPermissions(MapsActivity.this,
//                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
//                1);
        Nearby.Connections.startDiscovery(

                mGoogleApiClient,
                SERVICE_ID,
                mEndpointDiscoveryCallback,
                new DiscoveryOptions(Strategy.P2P_STAR))
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
                                    Log.i("SELF DISCOVERING", String.valueOf(status.getStatusCode()));


                                }
                            }
                        });
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    public void notifyFound(View view){
        mMap.addMarker(new MarkerOptions()
                .position(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()))
                .title("Found by " + nickname + "!"));
    }

}
