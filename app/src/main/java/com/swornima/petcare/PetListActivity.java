package com.swornima.petcare;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.swornima.petcare.db.AppDatabase;
import com.swornima.petcare.db.CareTaskJson;
import com.swornima.petcare.db.PetEntity;
import com.swornima.petcare.db.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * "My Pets" screen - lists every pet profile belonging to the signed-in
 * owner and lets them open, edit or delete (core "delete items" and
 * "edit items" requirements) each one.
 */
public class PetListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PetAdapter adapter;
    private List<Pet> petList;
    private ProgressBar progressBar;
    private View tvEmpty;
    private FloatingActionButton fabAdd;

    private AppDatabase db;
    private String userId;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pet_list);

        db = AppDatabase.getInstance(this);
        userId = new SessionManager(this).getCurrentUserId();

        BottomNavHelper.setup(this, BottomNavHelper.Tab.MY_PETS);

        recyclerView = findViewById(R.id.rvPets);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        fabAdd = findViewById(R.id.fabAdd);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        petList = new ArrayList<>();
        adapter = new PetAdapter(this, petList, new PetAdapter.OnItemClickListener() {
            @Override
            public void onEditClick(Pet pet) {
                Intent intent = new Intent(PetListActivity.this, CreatePetActivity.class);
                intent.putExtra("pet_id", pet.getId());
                startActivity(intent);
            }

            @Override
            public void onDeleteClick(Pet pet) {
                new AlertDialog.Builder(PetListActivity.this)
                        .setTitle("Delete Pet")
                        .setMessage("Are you sure you want to delete this pet's profile and care routine?")
                        .setPositiveButton("Delete", (dialog, which) -> deletePet(pet))
                        .setNegativeButton("Cancel", null)
                        .show();
            }

            @Override
            public void onItemClick(Pet pet) {
                // Open Detail View
                Intent intent = new Intent(PetListActivity.this, PetDetailActivity.class);
                intent.putExtra("pet_id", pet.getId());
                startActivity(intent);
            }
        });
        recyclerView.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> {
            startActivity(new Intent(PetListActivity.this, CreatePetActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchPets();
    }

    private void fetchPets() {
        progressBar.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            List<PetEntity> entities = db.petDao().getAllForUser(userId);
            List<Pet> fetched = new ArrayList<>();
            for (PetEntity e : entities) {
                fetched.add(toPet(e));
            }

            mainHandler.post(() -> {
                progressBar.setVisibility(View.GONE);
                petList.clear();
                petList.addAll(fetched);
                adapter.notifyDataSetChanged();

                if (petList.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                }
            });
        });
    }

    private void deletePet(Pet pet) {
        executor.execute(() -> {
            ReminderScheduler.cancelPetReminders(this, pet.getId(), pet.getCareTasks());
            db.petDao().deleteById(pet.getId(), userId);
            mainHandler.post(() -> {
                Toast.makeText(this, "Pet deleted", Toast.LENGTH_SHORT).show();
                fetchPets();
            });
        });
    }

    private Pet toPet(PetEntity e) {
        return new Pet(e.id, e.userId, e.name, e.breed, e.age, e.weight, e.dietaryPreferences,
                e.allergies, e.favouriteToys, e.notes, e.imageUrl, CareTaskJson.fromJson(e.careTasksJson),
                e.latitude, e.longitude, e.locationName, e.locationType);
    }
}
