package com.example.trackingapp;

import static android.content.ContentValues.TAG;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.trackingapp.databinding.ActivityMapsBinding;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;


import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;

    List<Address> listGeoCoder;

    private static final int LOCATION_PERMISSION_CODE = 101;

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
            Location currentLocation = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            double currentLongitude = currentLocation.getLongitude();
            double currentLatitude = currentLocation.getLatitude();

            Log.d("GOOGLE_MAP_TAG", "Address has Longitude " +
                    ":::  " + String.valueOf(currentLongitude) + " Latitude   " + String.valueOf(currentLatitude));

            try {
                listGeoCoder = new Geocoder(this).getFromLocationName("Desasiswa Restu M02, Universiti Sains Malaysia, Halaman Bukit Gambir 4, Gelugor, Penang", 1);
            } catch (Exception e) {
                e.printStackTrace();
            }

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

        final ArrayList<String> name = new ArrayList<>();
        final List<Double> longitudeList = new ArrayList<>();
        final List<Double> latitudeList = new ArrayList<>();
        ArrayList<MyLatLngData> locations = new ArrayList<>();
        DatabaseReference myRef = FirebaseDatabase.getInstance().getReference().child("Places");
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                for(DataSnapshot ds : dataSnapshot.getChildren()) {
                    int i = 0;
                    name.add(ds.child("Name").getValue(String.class));
                    longitudeList.add(ds.child("Longitude").getValue(Double.class));
                    latitudeList.add(ds.child("Latitude").getValue(Double.class));
                    locations.add(new MyLatLngData(name.get(i), latitudeList.get(i), longitudeList.get(i)));
                    i++;
                }
                // Add markers and move the camera
                for(MyLatLngData location : locations){
                    mMap.addMarker(new MarkerOptions()
                            .position(location.getLatLng())
                            .title("Marker in " + location.getName()));  // here you could use location.getTitle()
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(location.getLatLng()));
                }
                Log.d("TAG", "Country: " + name + " / Longitude: " + longitudeList + " / Latitude: " + latitudeList);
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

    private static class MyLatLngData {
        private String name;
        private LatLng latlng;

        public MyLatLngData(String name, double lat, double lng) {
            this.name = name;
            this.latlng = new LatLng(lat, lng);
        }

        // add getters and setters or make fields public....e.g.
        public LatLng getLatLng() {
            return latlng;
        }

        public void setLatLng(LatLng latlng) {
            this.latlng = latlng;
        }

        public String getName() {
            return name;
        }
    }
}