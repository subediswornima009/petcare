package com.swornima.petcare;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import com.swornima.petcare.db.AppDatabase;
import com.swornima.petcare.db.CareTaskEntity;
import com.swornima.petcare.db.CareTaskJson;
import com.swornima.petcare.db.PetEntity;
import com.swornima.petcare.db.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * "Manage My Care Tasks" screen - satisfies the core requirement to
 * delete, edit and create reusable care tasks (feeding, walking, grooming,
 * medication, healthcare) independently of any single pet. Tasks created
 * here can then be attached to a pet's routine in CreatePetActivity.
 */
public class MyItemsActivity extends AppCompatActivity {

    private static final String[] CATEGORIES = {"Feeding", "Exercise", "Grooming", "Medication", "Healthcare"};
    private static final String[] FREQUENCIES = {"Daily", "Weekly", "Monthly", "One-off"};

    private RecyclerView recyclerView;
    private CareTaskAdapter adapter;
    private List<CareTask> careTaskList;
    private FloatingActionButton fabAdd;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private AppDatabase db;
    private String userId;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_items);

        db = AppDatabase.getInstance(this);
        userId = new SessionManager(this).getCurrentUserId();

        BottomNavHelper.setup(this, BottomNavHelper.Tab.CARE_ROUTINE);

        recyclerView = findViewById(R.id.recyclerView);
        fabAdd = findViewById(R.id.fabAdd);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);

        careTaskList = new ArrayList<>();
        adapter = new CareTaskAdapter(this, careTaskList, new CareTaskAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(CareTask careTask) {
                Intent intent = new Intent(MyItemsActivity.this, RoutineDetailActivity.class);
                intent.putExtra("task_id", careTask.getId());
                startActivity(intent);
            }

            @Override
            public void onEditClick(CareTask careTask) {
                showAddEditDialog(careTask);
            }

            @Override
            public void onDeleteClick(CareTask careTask) {
                deleteCareTask(careTask);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> showAddEditDialog(null));

        fetchCareTasks();
    }

    private void fetchCareTasks() {
        progressBar.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            List<CareTaskEntity> entities = db.careTaskDao().getAllForUser(userId);
            List<CareTask> fetched = new ArrayList<>();
            for (CareTaskEntity e : entities) {
                CareTask t = new CareTask(e.id, e.name, e.category, e.notes, e.userId, e.time, e.frequency, e.supplies);
                copyDatesFromEntity(t, e);
                t.setCompleted(e.completed);
                fetched.add(t);
            }
            mainHandler.post(() -> {
                progressBar.setVisibility(View.GONE);
                careTaskList.clear();
                careTaskList.addAll(fetched);
                adapter.notifyDataSetChanged();
                toggleEmptyView();
            });
        });
    }

    private void toggleEmptyView() {
        if (careTaskList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showAddEditDialog(CareTask careTask) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_care_task, null);
        builder.setView(dialogView);

        EditText etName = dialogView.findViewById(R.id.etTaskName);
        TextInputLayout tilTaskName = dialogView.findViewById(R.id.tilTaskName);
        AutoCompleteTextView etCategory = dialogView.findViewById(R.id.etCategory);
        TextInputLayout tilCategory = dialogView.findViewById(R.id.tilCategory);
        AutoCompleteTextView etFrequency = dialogView.findViewById(R.id.etFrequency);
        EditText etTime = dialogView.findViewById(R.id.etTime);
        TextInputLayout tilTime = dialogView.findViewById(R.id.tilTime);
        EditText etAppointmentDate = dialogView.findViewById(R.id.etAppointmentDate);
        TextInputLayout tilAppointmentDate = dialogView.findViewById(R.id.tilAppointmentDate);
        EditText etGroomingDate = dialogView.findViewById(R.id.etGroomingDate);
        TextInputLayout tilGroomingDate = dialogView.findViewById(R.id.tilGroomingDate);
        EditText etVaccinationDueDate = dialogView.findViewById(R.id.etVaccinationDueDate);
        TextInputLayout tilVaccinationDueDate = dialogView.findViewById(R.id.tilVaccinationDueDate);
        EditText etMedicationStartDate = dialogView.findViewById(R.id.etMedicationStartDate);
        TextInputLayout tilMedicationStartDate = dialogView.findViewById(R.id.tilMedicationStartDate);
        EditText etMedicationEndDate = dialogView.findViewById(R.id.etMedicationEndDate);
        TextInputLayout tilMedicationEndDate = dialogView.findViewById(R.id.tilMedicationEndDate);
        EditText etSupplies = dialogView.findViewById(R.id.etSupplies);
        EditText etNotes = dialogView.findViewById(R.id.etNotes);
        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);

        etCategory.setAdapter(new ArrayAdapter<>(this, R.layout.item_dropdown, CATEGORIES));
        etFrequency.setAdapter(new ArrayAdapter<>(this, R.layout.item_dropdown, FREQUENCIES));

        etTime.setOnClickListener(v -> TimePickerHelper.show(getSupportFragmentManager(), etTime));
        tilTime.setEndIconOnClickListener(v -> TimePickerHelper.show(getSupportFragmentManager(), etTime));
        setupDatePicker(etAppointmentDate, tilAppointmentDate);
        setupDatePicker(etGroomingDate, tilGroomingDate);
        setupDatePicker(etVaccinationDueDate, tilVaccinationDueDate);
        setupDatePicker(etMedicationStartDate, tilMedicationStartDate);
        setupDatePicker(etMedicationEndDate, tilMedicationEndDate);

        if (careTask != null) {
            tvTitle.setText("Edit Care Task");
            etName.setText(careTask.getName());
            etCategory.setText(careTask.getCategory(), false);
            etFrequency.setText(careTask.getFrequency(), false);
            etTime.setText(careTask.getTime());
            etAppointmentDate.setText(careTask.getAppointmentDate());
            etGroomingDate.setText(careTask.getGroomingDate());
            etVaccinationDueDate.setText(careTask.getVaccinationDueDate());
            etMedicationStartDate.setText(careTask.getMedicationStartDate());
            etMedicationEndDate.setText(careTask.getMedicationEndDate());
            etSupplies.setText(careTask.getSupplies());
            etNotes.setText(careTask.getNotes());
        }

        builder.setPositiveButton(careTask == null ? "Add" : "Update", (dialog, which) -> {
            // Will be overridden
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        // Override to prevent closing on validation error
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String category = etCategory.getText().toString().trim();
            String frequency = etFrequency.getText().toString().trim();
            String time = etTime.getText().toString().trim();
            String appointmentDate = etAppointmentDate.getText().toString().trim();
            String groomingDate = etGroomingDate.getText().toString().trim();
            String vaccinationDueDate = etVaccinationDueDate.getText().toString().trim();
            String medicationStartDate = etMedicationStartDate.getText().toString().trim();
            String medicationEndDate = etMedicationEndDate.getText().toString().trim();
            String supplies = etSupplies.getText().toString().trim();
            String notes = etNotes.getText().toString().trim();

            tilTaskName.setError(null);
            tilCategory.setError(null);

            if (TextUtils.isEmpty(name)) {
                tilTaskName.setError("Name required");
                Toast.makeText(this, "Please enter a care task name.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(category)) {
                tilCategory.setError("Category required");
                Toast.makeText(this, "Please select a care task category.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (careTask == null) {
                saveCareTask(name, category, notes, time, frequency, supplies,
                        appointmentDate, groomingDate, vaccinationDueDate, medicationStartDate, medicationEndDate);
            } else {
                updateCareTask(careTask, name, category, notes, time, frequency, supplies,
                        appointmentDate, groomingDate, vaccinationDueDate, medicationStartDate, medicationEndDate);
            }
            dialog.dismiss();
        });
    }

    private void saveCareTask(String name, String category, String notes, String time, String frequency, String supplies,
                              String appointmentDate, String groomingDate, String vaccinationDueDate,
                              String medicationStartDate, String medicationEndDate) {
        progressBar.setVisibility(View.VISIBLE);
        CareTaskEntity entity = new CareTaskEntity();
        entity.id = UUID.randomUUID().toString();
        entity.name = name;
        entity.category = category;
        entity.notes = notes;
        entity.time = time;
        entity.frequency = frequency;
        entity.supplies = supplies;
        entity.appointmentDate = appointmentDate;
        entity.groomingDate = groomingDate;
        entity.vaccinationDueDate = vaccinationDueDate;
        entity.medicationStartDate = medicationStartDate;
        entity.medicationEndDate = medicationEndDate;
        entity.userId = userId;
        entity.completed = false;

        executor.execute(() -> {
            db.careTaskDao().upsert(entity);
            mainHandler.post(() -> {
                Toast.makeText(MyItemsActivity.this, "Care task added!", Toast.LENGTH_SHORT).show();
                fetchCareTasks(); // Refresh list
            });
        });
    }

    private void updateCareTask(CareTask careTask, String name, String category, String notes, String time, String frequency, String supplies,
                                String appointmentDate, String groomingDate, String vaccinationDueDate,
                                String medicationStartDate, String medicationEndDate) {
        progressBar.setVisibility(View.VISIBLE);
        CareTaskEntity entity = new CareTaskEntity();
        entity.id = careTask.getId();
        entity.userId = userId;
        entity.name = name;
        entity.category = category;
        entity.notes = notes;
        entity.time = time;
        entity.frequency = frequency;
        entity.supplies = supplies;
        entity.appointmentDate = appointmentDate;
        entity.groomingDate = groomingDate;
        entity.vaccinationDueDate = vaccinationDueDate;
        entity.medicationStartDate = medicationStartDate;
        entity.medicationEndDate = medicationEndDate;
        entity.completed = careTask.isCompleted();

        executor.execute(() -> {
            db.careTaskDao().upsert(entity);
            propagateCareTaskUpdate(entity);
            mainHandler.post(() -> {
                Toast.makeText(MyItemsActivity.this, "Care task updated!", Toast.LENGTH_SHORT).show();
                fetchCareTasks();
            });
        });
    }

    private void deleteCareTask(CareTask careTask) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Care Task")
                .setMessage("Are you sure you want to delete " + careTask.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    progressBar.setVisibility(View.VISIBLE);
                    executor.execute(() -> {
                        db.careTaskDao().deleteById(careTask.getId(), userId);
                        mainHandler.post(() -> {
                            Toast.makeText(MyItemsActivity.this, "Care task deleted successfully.", Toast.LENGTH_SHORT).show();
                            fetchCareTasks();
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupDatePicker(EditText editText, TextInputLayout layout) {
        editText.setOnClickListener(v -> DatePickerHelper.show(editText));
        layout.setEndIconOnClickListener(v -> DatePickerHelper.show(editText));
    }

    private void copyDatesFromEntity(CareTask task, CareTaskEntity entity) {
        task.setAppointmentDate(entity.appointmentDate);
        task.setGroomingDate(entity.groomingDate);
        task.setVaccinationDueDate(entity.vaccinationDueDate);
        task.setMedicationStartDate(entity.medicationStartDate);
        task.setMedicationEndDate(entity.medicationEndDate);
    }

    private void propagateCareTaskUpdate(CareTaskEntity updatedEntity) {
        List<PetEntity> pets = db.petDao().getAllForUser(userId);
        for (PetEntity petEntity : pets) {
            List<CareTask> oldTasks = CareTaskJson.fromJson(petEntity.careTasksJson);
            List<CareTask> updatedTasks = new ArrayList<>();
            boolean changed = false;

            for (CareTask task : oldTasks) {
                if (updatedEntity.id != null && updatedEntity.id.equals(task.getId())) {
                    CareTask updatedTask = careTaskFromEntity(updatedEntity);
                    updatedTask.setCompleted(task.isCompleted());
                    updatedTask.setCompletedAtMillis(task.getCompletedAtMillis());
                    updatedTasks.add(updatedTask);
                    changed = true;
                } else {
                    updatedTasks.add(task);
                }
            }

            if (changed) {
                ReminderScheduler.cancelPetReminders(this, petEntity.id, oldTasks);
                petEntity.careTasksJson = CareTaskJson.toJson(updatedTasks);
                db.petDao().upsert(petEntity);
                ReminderScheduler.schedulePetReminders(this, petFromEntity(petEntity, updatedTasks));
            }
        }
    }

    private CareTask careTaskFromEntity(CareTaskEntity entity) {
        CareTask task = new CareTask(entity.id, entity.name, entity.category, entity.notes, entity.userId,
                entity.time, entity.frequency, entity.supplies);
        copyDatesFromEntity(task, entity);
        task.setCompleted(entity.completed);
        return task;
    }

    private Pet petFromEntity(PetEntity entity, List<CareTask> tasks) {
        return new Pet(entity.id, entity.userId, entity.name, entity.breed, entity.age, entity.weight,
                entity.dietaryPreferences, entity.allergies, entity.favouriteToys, entity.notes,
                entity.imageUrl, tasks, entity.latitude, entity.longitude, entity.locationName, entity.locationType);
    }
}
