package com.swornima.petcare;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;
import com.swornima.petcare.db.AppDatabase;
import com.swornima.petcare.db.PlaceEntity;
import com.swornima.petcare.db.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * "Places" screen - lets the owner search for and save locations (vet
 * clinics, grooming salons, dog parks, pet supply stores, animal shelters)
 * independent of any single pet, using the same map+search picker used when
 * linking a location to a pet.
 */
public class PlacesActivity extends AppCompatActivity {

    private static final String[] PLACE_TYPES = {"Veterinary Clinic", "Grooming Salon", "Dog Park", "Pet Supply Store", "Animal Shelter"};

    private RecyclerView recyclerView;
    private PlaceAdapter adapter;
    private List<Place> placeList;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private FloatingActionButton fabAdd;

    private AppDatabase db;
    private String userId;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private double pendingLat;
    private double pendingLng;
    private String pendingAddress;

    private final ActivityResultLauncher<Intent> pickLocationLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    pendingLat = result.getData().getDoubleExtra("latitude", 0);
                    pendingLng = result.getData().getDoubleExtra("longitude", 0);
                    pendingAddress = result.getData().getStringExtra("locationName");
                    showSavePlaceDialog();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_places);

        db = AppDatabase.getInstance(this);
        userId = new SessionManager(this).getCurrentUserId();

        BottomNavHelper.setup(this, BottomNavHelper.Tab.PLACES);

        recyclerView = findViewById(R.id.rvPlaces);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        fabAdd = findViewById(R.id.fabAdd);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        placeList = new ArrayList<>();
        adapter = new PlaceAdapter(this, placeList, new PlaceAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Place place) {
                String uri = String.format(Locale.ENGLISH, "geo:%f,%f?q=%f,%f(%s)",
                        place.getLatitude(), place.getLongitude(),
                        place.getLatitude(), place.getLongitude(), place.getName());
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
            }

            @Override
            public void onEditClick(Place place) {
                showSavePlaceDialog(place);
            }

            @Override
            public void onDeleteClick(Place place) {
                deletePlace(place);
            }
        });
        recyclerView.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> pickLocationLauncher.launch(new Intent(this, PickLocationActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchPlaces();
    }

    private void fetchPlaces() {
        progressBar.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            List<PlaceEntity> entities = db.placeDao().getAllForUser(userId);
            List<Place> fetched = new ArrayList<>();
            for (PlaceEntity e : entities) {
                fetched.add(new Place(e.id, e.userId, e.name, e.type, e.address,
                        e.phone, e.openingHours, e.notes, e.latitude, e.longitude, e.createdAtMillis));
            }

            mainHandler.post(() -> {
                progressBar.setVisibility(View.GONE);
                placeList.clear();
                placeList.addAll(fetched);
                adapter.notifyDataSetChanged();
                tvEmpty.setVisibility(placeList.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    private void showSavePlaceDialog() {
        showSavePlaceDialog(null);
    }

    private void showSavePlaceDialog(Place editingPlace) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_place, null);
        builder.setView(dialogView);

        TextInputEditText etName = dialogView.findViewById(R.id.etPlaceName);
        MaterialAutoCompleteTextView etType = dialogView.findViewById(R.id.etPlaceType);
        TextInputEditText etPhone = dialogView.findViewById(R.id.etPlacePhone);
        TextInputEditText etHours = dialogView.findViewById(R.id.etPlaceHours);
        TextInputEditText etNotes = dialogView.findViewById(R.id.etPlaceNotes);
        TextView tvAddressPreview = dialogView.findViewById(R.id.tvPlaceAddressPreview);
        TextInputLayout tilName = dialogView.findViewById(R.id.tilPlaceName);
        TextInputLayout tilType = dialogView.findViewById(R.id.tilPlaceType);

        etType.setAdapter(new ArrayAdapter<>(this, R.layout.item_dropdown, PLACE_TYPES));
        if (editingPlace == null) {
            etName.setText(pendingAddress);
            tvAddressPreview.setText(pendingAddress != null ? pendingAddress : "Pinned Location");
        } else {
            etName.setText(editingPlace.getName());
            etType.setText(editingPlace.getType(), false);
            etPhone.setText(editingPlace.getPhone());
            etHours.setText(editingPlace.getOpeningHours());
            etNotes.setText(editingPlace.getNotes());
            tvAddressPreview.setText(editingPlace.getAddress() != null ? editingPlace.getAddress() : "Pinned Location");
        }

        builder.setPositiveButton(editingPlace == null ? "Save" : "Update", (dialog, which) -> {});
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String type = etType.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String hours = etHours.getText().toString().trim();
            String notes = etNotes.getText().toString().trim();

            if (TextUtils.isEmpty(name)) {
                tilName.setError("Name required");
                Toast.makeText(this, "Please enter a place name.", Toast.LENGTH_SHORT).show();
                return;
            }
            tilName.setError(null);
            tilType.setError(null);

            if (TextUtils.isEmpty(type)) {
                tilType.setError("Service type required");
                Toast.makeText(this, "Please select a service type.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (editingPlace == null) {
                savePlace(name, type, pendingAddress, phone, hours, notes, pendingLat, pendingLng);
            } else {
                updatePlace(editingPlace.getId(), name, type, phone, hours, notes);
            }
            dialog.dismiss();
        });
    }

    private void savePlace(String name, String type, String address, String phone,
                           String openingHours, String notes, double lat, double lng) {
        PlaceEntity entity = new PlaceEntity();
        entity.id = UUID.randomUUID().toString();
        entity.userId = userId;
        entity.name = name;
        entity.type = type;
        entity.address = address;
        entity.phone = phone;
        entity.openingHours = openingHours;
        entity.notes = notes;
        entity.latitude = lat;
        entity.longitude = lng;
        entity.createdAtMillis = System.currentTimeMillis();

        executor.execute(() -> {
            db.placeDao().insert(entity);
            mainHandler.post(() -> {
                Toast.makeText(this, "Place saved!", Toast.LENGTH_SHORT).show();
                fetchPlaces();
            });
        });
    }

    private void updatePlace(String placeId, String name, String type, String phone,
                             String openingHours, String notes) {
        executor.execute(() -> {
            db.placeDao().updateDetails(placeId, userId, name, type, phone, openingHours, notes);
            mainHandler.post(() -> {
                Toast.makeText(this, "Place updated!", Toast.LENGTH_SHORT).show();
                fetchPlaces();
            });
        });
    }

    private void deletePlace(Place place) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Place")
                .setMessage("Are you sure you want to delete " + place.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> executor.execute(() -> {
                    db.placeDao().deleteById(place.getId(), userId);
                    mainHandler.post(() -> {
                        Toast.makeText(this, "Place deleted successfully.", Toast.LENGTH_SHORT).show();
                        fetchPlaces();
                    });
                }))
                .setNegativeButton("Cancel", null)
                .show();
    }
}
