package com.example.trackingapp;

import static android.content.ContentValues.TAG;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.trackingapp.databinding.ActivityMapsBinding;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;


import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    TextView calculatedRisk;

    private static final int LOCATION_PERMISSION_CODE = 101;

    public Location currentLocation;

    public class Crime {    /** create a class to store Crime */
        String name;
        int risk;
        LatLng location;
    }

    /** public variable to be accessible from all the functions */
    public List<Crime> crimeList = new ArrayList<>();      // to store all the crime
    public List<Crime> crimeInCircle = new ArrayList<>();  // to store crime within the range
    public int distanceConsidered = 350;    // can change this value to test the radius
                                            // green = 250, yellow = 270, red = 350

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

        if (isLocationPermissionGranted()) {
            // Obtain the SupportMapFragment and get notified when the map is ready to be used.
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);

            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            currentLocation = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            double currentLongitude = currentLocation.getLongitude();
            double currentLatitude = currentLocation.getLatitude();

            Log.d("GOOGLE_MAP_TAG", "Address has Longitude " +
                    ":::  " + String.valueOf(currentLongitude) + " Latitude   " + String.valueOf(currentLatitude));


        } else {
            requestLocationPermission();
        }

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

        DatabaseReference myRef = FirebaseDatabase.getInstance().getReference().child("Places");
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                loadFromDatabase(dataSnapshot);   /** Here is to load data from firebase to crimeList */
                placeMarker();                    /** Here is to place markers with different color */
                getCrimeInRange();                /** Here is to get the crime within the range */
                int color = getColor();           /** Here is to calculate the risk level */
                drawCircle(color);                /** Here is to draw the circle with selected color */

                Log.d("answer123", String.valueOf(crimeInCircle.size()));

            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED){
            mMap.setMyLocationEnabled(true);
        }

        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
    }
    private boolean isLocationPermissionGranted(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED){
            return true;
        }
        else{
            return false;
        }
    }
    private void requestLocationPermission(){
        ActivityCompat.requestPermissions(this,
                new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
    }

    private void loadFromDatabase(DataSnapshot dataSnapshot){
        /** Here is to load data from firebase to crimeList */
        for(DataSnapshot ds : dataSnapshot.getChildren()) {
            Crime temp = new Crime();                   /** create a crime and store all the related values into */
            Double lat, lon;
            temp.name = ds.child("Name").getValue(String.class);
            lat = ds.child("Latitude").getValue(Double.class);
            lon = ds.child("Longitude").getValue(Double.class);
            temp.risk = ds.child("Risk").getValue(Integer.class);
            temp.location = new LatLng(lat, lon);
            crimeList.add(temp);                        /** add the crime to crimeList */
        }
    }
    private void placeMarker(){
        /** Here is to place markers with different color */
        for(Crime crime : crimeList){
            float markerColor = 0;
            if (crime.risk == 1){                       /** set marker color according to risk value */
                markerColor = BitmapDescriptorFactory.HUE_CYAN;
            }
            else if (crime.risk == 2){
                markerColor = BitmapDescriptorFactory.HUE_ORANGE;
            }
            else if (crime.risk == 3){
                markerColor = BitmapDescriptorFactory.HUE_RED;
            }
            else {
                markerColor = BitmapDescriptorFactory.HUE_GREEN;
            }
            mMap.addMarker(new MarkerOptions()          /** add marker with selected color */
                    .position(crime.location)
                    .title(crime.name)
                    .icon(BitmapDescriptorFactory
                            .defaultMarker(markerColor)));
        }
    }
    private void getCrimeInRange(){
        /** Here is to get the crime within the range */

        final int R = 6371000;                          // here is the formula only
        double mylat = currentLocation.getLatitude();
        double mylon = currentLocation.getLongitude();
        for(Crime crime : crimeList){
            double lat = crime.location.latitude;
            double lon = crime.location.longitude;
            double dlat = Math.toRadians(lat - mylat);
            double dlon = Math.toRadians(lon - mylon);
            double a = Math.sin(dlat/2) * Math.sin(dlat/2)
                    + Math.cos(Math.toRadians(mylat)) * Math.cos(Math.toRadians(mylat))
                    * Math.sin(dlon/2) * Math.sin(dlon/2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
            double d = R * c;
            if (d < distanceConsidered){                /** if within range, add into the crimeInCircle */
                crimeInCircle.add(crime);
                Log.d("answer123", crime.name);
            }
        }
    }
    private int getColor(){
        int crimeLevel=0;
        for (Crime crime : crimeInCircle)               // sum the total risk value
            crimeLevel = crimeLevel + crime.risk;

        calculatedRisk = (TextView) findViewById(R.id.displayRisk);

        // change color accordingly
        if (crimeLevel >= 0 && crimeLevel < 5)   {
            int color = Color.GREEN;
            calculatedRisk.setText("The risk in your place is low");
            calculatedRisk.setBackgroundColor(color);
            return color;
        }
        else if (crimeLevel >= 5 && crimeLevel < 10){
            int color = Color.YELLOW;
            calculatedRisk.setText("The risk in your place is medium");
            calculatedRisk.setBackgroundColor(color);
            return color;
        }
        else if (crimeLevel >= 10){
            int color = Color.RED;
            calculatedRisk.setText("The risk in your place is high");
            calculatedRisk.setBackgroundColor(color);
            return color;
        }
        else
            return 0;
    }
    private void drawCircle(int color){      /** Here is to draw the circle with selected color */
        LatLng currentLatLng =                          // get the LatLng of user location
                new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        Circle circle = mMap.addCircle(new CircleOptions()
                .center(currentLatLng)
                .radius(distanceConsidered)
                .fillColor(0x30000000)
                .strokeColor(color));

        Log.d("answer123", String.valueOf(crimeInCircle.size()));
    }
}