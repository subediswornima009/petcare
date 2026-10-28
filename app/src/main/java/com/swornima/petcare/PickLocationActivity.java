package com.swornima.petcare;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Implements the Geotagging desirable feature: lets the owner search for or
 * tap a location on a Google Map to link a veterinary clinic, grooming
 * salon, dog park, pet supply store or animal shelter to a pet record (or,
 * from PlacesActivity, to a standalone saved place). The chosen coordinates
 * and resolved name are returned to the caller via setResult().
 */
public class PickLocationActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private boolean mapReady = false;
    private SearchView searchView;
    private MaterialButton btnConfirm;
    private View cardSelected;
    private TextView tvSelectedLocation;
    private LatLng selectedLatLng;
    private String selectedLocationName;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pick_location);

        searchView = findViewById(R.id.searchView);
        btnConfirm = findViewById(R.id.btnConfirmLocation);
        cardSelected = findViewById(R.id.cardSelected);
        tvSelectedLocation = findViewById(R.id.tvSelectedLocation);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setupSearch();

        btnConfirm.setOnClickListener(v -> {
            if (selectedLatLng != null) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("latitude", selectedLatLng.latitude);
                resultIntent.putExtra("longitude", selectedLatLng.longitude);
                resultIntent.putExtra("locationName", selectedLocationName != null ? selectedLocationName : "Pinned Location");
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "Search for or tap a location first", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mapReady = true;

        double lat = getIntent().getDoubleExtra("latitude", 0);
        double lng = getIntent().getDoubleExtra("longitude", 0);

        if (lat != 0 && lng != 0) {
            String existingName = getIntent().getStringExtra("locationName");
            setSelection(new LatLng(lat, lng), existingName != null ? existingName : "Pinned Location", true);
        }

        // Tap to pin - e.g. the vet clinic or dog park location
        mMap.setOnMapClickListener(latLng -> {
            setSelection(latLng, "Pinned Location", true);
            resolveAddressForPin(latLng);
        });
    }

    /**
     * Updates the marker, the "Selected Location" summary card and enables
     * the Confirm button. Called both when the user taps the map and when a
     * search succeeds, so the two flows behave identically.
     */
    private void setSelection(LatLng latLng, String name, boolean moveCamera) {
        selectedLatLng = latLng;
        selectedLocationName = name;

        if (mMap != null) {
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(latLng).title(name));
            if (moveCamera) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            }
        }

        cardSelected.setVisibility(View.VISIBLE);
        tvSelectedLocation.setText(name);
        btnConfirm.setEnabled(true);
    }

    private void resolveAddressForPin(LatLng latLng) {
        if (!Geocoder.isPresent()) return;

        executor.execute(() -> {
            String resolvedName = null;
            try {
                Geocoder geocoder = new Geocoder(PickLocationActivity.this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    if (address.getAddressLine(0) != null) {
                        resolvedName = address.getAddressLine(0);
                    } else if (address.getFeatureName() != null) {
                        resolvedName = address.getFeatureName();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            String finalName = resolvedName;
            runOnUiThread(() -> {
                // Only update if this pin is still the current selection
                // (user might have tapped elsewhere or searched again while resolving).
                if (finalName != null && latLng.equals(selectedLatLng)) {
                    setSelection(latLng, finalName, false);
                }
            });
        });
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                String location = query != null ? query.trim() : "";

                if (location.isEmpty()) {
                    return false;
                }

                if (!mapReady) {
                    Toast.makeText(PickLocationActivity.this, "Map is still loading, please wait a moment", Toast.LENGTH_SHORT).show();
                    return true;
                }

                if (!Geocoder.isPresent()) {
                    Toast.makeText(PickLocationActivity.this,
                            "Location search isn't available on this device. Tap a spot on the map instead.",
                            Toast.LENGTH_LONG).show();
                    return true;
                }

                searchView.clearFocus();
                Toast.makeText(PickLocationActivity.this, "Searching...", Toast.LENGTH_SHORT).show();

                executor.execute(() -> {
                    List<Address> addressList = null;
                    try {
                        Geocoder geocoder = new Geocoder(PickLocationActivity.this, Locale.getDefault());
                        addressList = geocoder.getFromLocationName(location, 1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    List<Address> finalAddressList = addressList;
                    runOnUiThread(() -> {
                        if (finalAddressList != null && !finalAddressList.isEmpty()) {
                            Address address = finalAddressList.get(0);
                            LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                            // Keep what the user typed as the place name - that's what
                            // they're trying to save (e.g. "Kathmandu Vet Clinic"), not
                            // necessarily the geocoder's raw street address.
                            setSelection(latLng, location, true);
                        } else {
                            Toast.makeText(PickLocationActivity.this,
                                    "No results for \"" + location + "\". Try a different search or tap the map.",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                });

                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }
}
