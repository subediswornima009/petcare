package com.swornima.petcare;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.swornima.petcare.db.AppDatabase;
import com.swornima.petcare.db.CareTaskJson;
import com.swornima.petcare.db.CareTaskEntity;
import com.swornima.petcare.db.PetEntity;
import com.swornima.petcare.db.SessionManager;
import com.swornima.petcare.db.UserEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Home screen - the entry point of PetCare once the user is logged in.
 * Shows a quick summary of all pets and their care task completion status,
 * and provides navigation to every other core screen of the app.
 */
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_NOTIFICATION_PERMISSION = 202;

    private TextView tvGreeting;
    private CardView cardMyPets, cardCreatePet, cardManageTasks, cardExpenses;

    private TextView tvTotalCount, tvCompletedCount, tvInProgressCount;

    private AppDatabase db;
    private SessionManager sessionManager;
    private String userId;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = AppDatabase.getInstance(this);
        sessionManager = new SessionManager(this);

        BottomNavHelper.setup(this, BottomNavHelper.Tab.HOME);

        // Initialize Views
        tvGreeting = findViewById(R.id.tvGreeting);

        // Stats
        tvTotalCount = findViewById(R.id.tvTotalCount);
        tvCompletedCount = findViewById(R.id.tvCompletedCount);
        tvInProgressCount = findViewById(R.id.tvInProgressCount);

        // Cards
        cardMyPets = findViewById(R.id.cardMyPets);
        cardCreatePet = findViewById(R.id.cardCreatePet);
        cardManageTasks = findViewById(R.id.cardManageTasks);
        cardExpenses = findViewById(R.id.cardExpenses);

        userId = sessionManager.getCurrentUserId();

        if (userId == null) {
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        requestNotificationPermissionIfNeeded();
        ReminderReceiver.createNotificationChannel(this);
        fetchUserName();
        fetchStats();
        setupRecentPets();
        setupRecentRoutines();

        // Navigation
        cardMyPets.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), PetListActivity.class)));
        cardCreatePet.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), CreatePetActivity.class)));
        cardManageTasks.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), MyItemsActivity.class)));
        cardExpenses.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), ExpenseActivity.class)));

        // Profile
        findViewById(R.id.ivProfile)
                .setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), ProfileActivity.class)));
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATION_PERMISSION);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (userId == null) return;
        fetchUserName(); // Update name in case changed in Profile
        fetchStats(); // Update stats when returning to home
        setupRecentPets();
        setupRecentRoutines();
    }

    private void fetchStats() {
        executor.execute(() -> {
            List<PetEntity> pets = db.petDao().getAllForUser(userId);
            int total = pets.size();
            int completed = 0;
            int inProgress = 0;

            for (PetEntity p : pets) {
                List<CareTask> tasks = CareTaskJson.fromJson(p.careTasksJson);
                if (ChecklistResetHelper.resetExpiredTasks(tasks)) {
                    p.careTasksJson = CareTaskJson.toJson(tasks);
                    db.petDao().upsert(p);
                }
                if (!tasks.isEmpty()) {
                    boolean allDone = true;
                    for (CareTask t : tasks) {
                        if (!t.isCompleted()) {
                            allDone = false;
                            break;
                        }
                    }
                    if (allDone) completed++;
                    else inProgress++;
                } else {
                    inProgress++;
                }
            }

            int finalCompleted = completed;
            int finalInProgress = inProgress;
            mainHandler.post(() -> {
                tvTotalCount.setText(String.valueOf(total));
                tvCompletedCount.setText(String.valueOf(finalCompleted));
                tvInProgressCount.setText(String.valueOf(finalInProgress));
            });
        });
    }

    private void fetchUserName() {
        executor.execute(() -> {
            UserEntity user = db.userDao().findByUid(userId);
            if (user != null) {
                String fullName = user.fullName;
                mainHandler.post(() -> {
                    if (fullName != null && !fullName.trim().isEmpty()) {
                        tvGreeting.setText("Hello, " + fullName);
                    }
                });
            }
        });
    }

    private void setupRecentPets() {
        TextView tvRecentWork = findViewById(R.id.tvRecentWork);
        androidx.recyclerview.widget.RecyclerView rvRecentPets = findViewById(R.id.rvRecentPets);

        rvRecentPets.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));

        executor.execute(() -> {
            List<PetEntity> entities = db.petDao().getRecentForUser(userId, 2);
            List<Pet> recentList = new ArrayList<>();
            for (PetEntity e : entities) {
                recentList.add(toPet(e));
            }

            mainHandler.post(() -> {
                if (recentList.isEmpty()) {
                    tvRecentWork.setVisibility(View.GONE);
                    rvRecentPets.setVisibility(View.GONE);
                } else {
                    tvRecentWork.setVisibility(View.VISIBLE);
                    rvRecentPets.setVisibility(View.VISIBLE);

                    PetAdapter adapter = new PetAdapter(this, recentList,
                            new PetAdapter.OnItemClickListener() {
                                @Override
                                public void onEditClick(Pet pet) {
                                    Intent intent = new Intent(MainActivity.this, CreatePetActivity.class);
                                    intent.putExtra("pet_id", pet.getId());
                                    startActivity(intent);
                                }

                                @Override
                                public void onDeleteClick(Pet pet) {
                                    new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                                            .setTitle("Delete Pet")
                                            .setMessage("Are you sure you want to delete this pet's profile and care routine?")
                                            .setPositiveButton("Delete", (dialog, which) -> executor.execute(() -> {
                                                ReminderScheduler.cancelPetReminders(MainActivity.this, pet.getId(), pet.getCareTasks());
                                                db.petDao().deleteById(pet.getId(), userId);
                                                mainHandler.post(() -> {
                                                    Toast.makeText(MainActivity.this, "Pet deleted successfully.",
                                                            Toast.LENGTH_SHORT).show();
                                                    setupRecentPets(); // Refresh
                                                    fetchStats(); // Refresh stats
                                                });
                                            }))
                                            .setNegativeButton("Cancel", null)
                                            .show();
                                }

                                @Override
                                public void onItemClick(Pet pet) {
                                    Intent intent = new Intent(MainActivity.this, PetDetailActivity.class);
                                    intent.putExtra("pet_id", pet.getId());
                                    startActivity(intent);
                                }
                            });
                    rvRecentPets.setAdapter(adapter);
                }
            });
        });
    }

    private void setupRecentRoutines() {
        TextView tvRecentRoutines = findViewById(R.id.tvRecentRoutines);
        androidx.recyclerview.widget.RecyclerView rvRecentRoutines = findViewById(R.id.rvRecentRoutines);

        rvRecentRoutines.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));

        executor.execute(() -> {
            List<CareTaskEntity> entities = db.careTaskDao().getAllForUser(userId);
            List<CareTask> routines = new ArrayList<>();
            int count = Math.min(2, entities.size());
            for (int i = 0; i < count; i++) {
                CareTaskEntity e = entities.get(i);
                CareTask task = new CareTask(e.id, e.name, e.category, e.notes, e.userId, e.time, e.frequency, e.supplies);
                task.setAppointmentDate(e.appointmentDate);
                task.setGroomingDate(e.groomingDate);
                task.setVaccinationDueDate(e.vaccinationDueDate);
                task.setMedicationStartDate(e.medicationStartDate);
                task.setMedicationEndDate(e.medicationEndDate);
                task.setCompleted(e.completed);
                routines.add(task);
            }

            mainHandler.post(() -> {
                if (routines.isEmpty()) {
                    tvRecentRoutines.setVisibility(View.GONE);
                    rvRecentRoutines.setVisibility(View.GONE);
                } else {
                    tvRecentRoutines.setVisibility(View.VISIBLE);
                    rvRecentRoutines.setVisibility(View.VISIBLE);
                    CareTaskAdapter adapter = new CareTaskAdapter(this, routines, new CareTaskAdapter.OnItemClickListener() {
                        @Override
                        public void onItemClick(CareTask careTask) {
                            Intent intent = new Intent(MainActivity.this, RoutineDetailActivity.class);
                            intent.putExtra("task_id", careTask.getId());
                            startActivity(intent);
                        }

                        @Override
                        public void onEditClick(CareTask careTask) {
                            startActivity(new Intent(MainActivity.this, MyItemsActivity.class));
                        }

                        @Override
                        public void onDeleteClick(CareTask careTask) {
                            startActivity(new Intent(MainActivity.this, MyItemsActivity.class));
                        }
                    });
                    rvRecentRoutines.setAdapter(adapter);
                }
            });
        });
    }

    private Pet toPet(PetEntity e) {
        Pet p = new Pet(e.id, e.userId, e.name, e.breed, e.age, e.weight, e.dietaryPreferences,
                e.allergies, e.favouriteToys, e.notes, e.imageUrl, CareTaskJson.fromJson(e.careTasksJson),
                e.latitude, e.longitude, e.locationName, e.locationType);
        return p;
    }
}
