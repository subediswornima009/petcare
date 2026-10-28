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
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.squareup.picasso.Picasso;
import com.swornima.petcare.db.AppDatabase;
import com.swornima.petcare.db.CareTaskEntity;
import com.swornima.petcare.db.CareTaskJson;
import com.swornima.petcare.db.PetEntity;
import com.swornima.petcare.db.PlaceEntity;
import com.swornima.petcare.db.SessionManager;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Create / Edit a pet profile and build its personalised care routine.
 * Satisfies the core "Create a pet care routine" requirement: users can add
 * daily/weekly care tasks, required supplies, schedules, notes and an
 * optional photo of the pet. Also implements the Geotagging desirable
 * feature by letting the owner pin a linked location (vet clinic, grooming
 * salon, dog park, pet supply store or animal shelter) to the pet record.
 */
public class CreatePetActivity extends AppCompatActivity {

    private static final String[] LOCATION_TYPES = {"Veterinary Clinic", "Grooming Salon", "Dog Park", "Pet Supply Store", "Animal Shelter"};
    private static final String[] CATEGORIES = {"Feeding", "Exercise", "Grooming", "Medication", "Healthcare"};
    private static final String[] FREQUENCIES = {"Daily", "Weekly", "Monthly", "One-off"};

    private TextInputEditText etName, etBreed, etAge, etWeight, etDiet, etAllergies, etToys, etNotes;
    private ImageView ivPetImage;
    private ProgressBar progressBarImage, progressBar;
    private FrameLayout layoutImage;
    private View layoutPlaceholder;
    private RecyclerView rvCareTasks;
    private TextView tvNoCareTasks;
    private MaterialButton btnSelectTask, btnCreateTask, btnSavePet, btnPickLocation, btnSelectSavedPlace;
    private TextView tvLocationPreview;

    private List<CareTask> addedCareTasks;
    private CareTaskAdapter addedCareTasksAdapter;
    private String uploadedImageUrl = "";

    private AppDatabase db;
    private String userId;
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static final String IMGBB_API_KEY = "551429baaa9ed821581d0a992f9163e3";

    private String editPetId;
    private boolean isEditMode = false;

    private double selectedLat = 0;
    private double selectedLng = 0;
    private String selectedLocationName = null;
    private String selectedLocationType = null;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    // Reset view state for local preview
                    ivPetImage.setColorFilter(null);
                    ivPetImage.setImageTintList(null);
                    layoutPlaceholder.setVisibility(View.GONE);
                    ivPetImage.setImageURI(uri);
                    uploadImageToImgBB(uri);
                }
            }
    );

    private final ActivityResultLauncher<Intent> pickLocationLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedLat = result.getData().getDoubleExtra("latitude", 0);
                    selectedLng = result.getData().getDoubleExtra("longitude", 0);
                    selectedLocationName = result.getData().getStringExtra("locationName");
                    selectedLocationType = null;

                    updateLocationPreview();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_pet);

        db = AppDatabase.getInstance(this);
        userId = new SessionManager(this).getCurrentUserId();

        initializeViews();
        setupListeners();
        setupRecyclerView();

        if (getIntent().hasExtra("pet_id")) {
            editPetId = getIntent().getStringExtra("pet_id");
            isEditMode = true;
            btnSavePet.setText("Update Pet Profile");
            fetchPetDetails(editPetId);
        }
    }

    private void initializeViews() {
        etName = findViewById(R.id.etName);
        etBreed = findViewById(R.id.etBreed);
        etAge = findViewById(R.id.etAge);
        etWeight = findViewById(R.id.etWeight);
        etDiet = findViewById(R.id.etDiet);
        etAllergies = findViewById(R.id.etAllergies);
        etToys = findViewById(R.id.etToys);
        etNotes = findViewById(R.id.etNotes);
        ivPetImage = findViewById(R.id.ivPetImage);
        progressBarImage = findViewById(R.id.progressBarImage);
        progressBar = findViewById(R.id.progressBar);
        layoutImage = findViewById(R.id.layoutImage);
        layoutPlaceholder = findViewById(R.id.layoutPlaceholder);
        rvCareTasks = findViewById(R.id.rvCareTasks);
        tvNoCareTasks = findViewById(R.id.tvNoCareTasks);
        btnSelectTask = findViewById(R.id.btnSelectTask);
        btnCreateTask = findViewById(R.id.btnCreateTask);
        btnSavePet = findViewById(R.id.btnSavePet);
        btnSelectSavedPlace = findViewById(R.id.btnSelectSavedPlace);
        btnPickLocation = findViewById(R.id.btnPickLocation);
        tvLocationPreview = findViewById(R.id.tvLocationPreview);
    }

    private void setupRecyclerView() {
        addedCareTasks = new ArrayList<>();
        addedCareTasksAdapter = new CareTaskAdapter(this, addedCareTasks, new CareTaskAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(CareTask careTask) {
                Intent intent = new Intent(CreatePetActivity.this, RoutineDetailActivity.class);
                intent.putExtra("task_id", careTask.getId());
                startActivity(intent);
            }

            @Override
            public void onEditClick(CareTask careTask) {
                confirmRemoveRoutineTask(careTask);
            }

            @Override
            public void onDeleteClick(CareTask careTask) {
                confirmRemoveRoutineTask(careTask);
            }
        });
        rvCareTasks.setLayoutManager(new LinearLayoutManager(this));
        rvCareTasks.setAdapter(addedCareTasksAdapter);
    }

    private void setupListeners() {
        layoutImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        btnSelectTask.setOnClickListener(v -> showSelectTaskDialog());
        btnCreateTask.setOnClickListener(v -> showCreateTaskDialog());
        btnSavePet.setOnClickListener(v -> savePet());
        btnSelectSavedPlace.setOnClickListener(v -> showSelectSavedPlaceDialog());

        btnPickLocation.setOnClickListener(v -> {
            Intent intent = new Intent(this, PickLocationActivity.class);
            if (selectedLat != 0 && selectedLng != 0) {
                intent.putExtra("latitude", selectedLat);
                intent.putExtra("longitude", selectedLng);
                intent.putExtra("locationName", selectedLocationName);
            }
            pickLocationLauncher.launch(intent);
        });
    }

    private void fetchPetDetails(String id) {
        progressBar.setVisibility(View.VISIBLE);
        dbExecutor.execute(() -> {
            PetEntity entity = db.petDao().getById(id, userId);
            mainHandler.post(() -> {
                progressBar.setVisibility(View.GONE);
                if (entity != null) {
                    etName.setText(entity.name);
                    etBreed.setText(entity.breed);
                    etAge.setText(entity.age);
                    etWeight.setText(entity.weight);
                    etDiet.setText(entity.dietaryPreferences);
                    etAllergies.setText(entity.allergies);
                    etToys.setText(entity.favouriteToys);
                    etNotes.setText(entity.notes);
                    uploadedImageUrl = entity.imageUrl;

                    if (uploadedImageUrl != null && !uploadedImageUrl.isEmpty()) {
                        layoutPlaceholder.setVisibility(View.GONE);
                        ivPetImage.setColorFilter(null);
                        ivPetImage.setImageTintList(null);
                        Picasso.get().load(uploadedImageUrl).into(ivPetImage);
                    }

                    List<CareTask> tasks = CareTaskJson.fromJson(entity.careTasksJson);
                    if (!tasks.isEmpty()) {
                        addedCareTasks.addAll(tasks);
                        addedCareTasksAdapter.notifyDataSetChanged();
                        checkEmptyState();
                    }

                    if (entity.latitude != 0 && entity.longitude != 0) {
                        selectedLat = entity.latitude;
                        selectedLng = entity.longitude;
                        selectedLocationName = entity.locationName;
                        selectedLocationType = entity.locationType;
                        updateLocationPreview();
                    }
                } else {
                    Toast.makeText(this, "Failed to load pet", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void showSelectSavedPlaceDialog() {
        progressBar.setVisibility(View.VISIBLE);
        dbExecutor.execute(() -> {
            List<PlaceEntity> places = db.placeDao().getAllForUser(userId);
            List<String> labels = new ArrayList<>();
            for (PlaceEntity place : places) {
                labels.add(place.name + (TextUtils.isEmpty(place.type) ? "" : " - " + place.type));
            }

            mainHandler.post(() -> {
                progressBar.setVisibility(View.GONE);
                if (places.isEmpty()) {
                    Toast.makeText(this, "No saved places found. Pin a new location or add one from Places.", Toast.LENGTH_LONG).show();
                    return;
                }

                new AlertDialog.Builder(this)
                        .setTitle("Select Saved Place")
                        .setItems(labels.toArray(new String[0]), (dialog, which) -> {
                            PlaceEntity selectedPlace = places.get(which);
                            selectedLat = selectedPlace.latitude;
                            selectedLng = selectedPlace.longitude;
                            selectedLocationName = selectedPlace.name;
                            selectedLocationType = selectedPlace.type;
                            updateLocationPreview();
                            Toast.makeText(this, "Place linked to pet.", Toast.LENGTH_SHORT).show();
                        })
                        .show();
            });
        });
    }

    private void updateLocationPreview() {
        if (selectedLat == 0 && selectedLng == 0) {
            tvLocationPreview.setText("No Location Pinned");
            tvLocationPreview.setTextColor(getResources().getColor(R.color.text_secondary));
            return;
        }

        String label = TextUtils.isEmpty(selectedLocationName) ? "Pinned Location" : selectedLocationName;
        if (!TextUtils.isEmpty(selectedLocationType)) {
            label = selectedLocationType + ": " + label;
        }
        tvLocationPreview.setText(label);
        tvLocationPreview.setTextColor(getResources().getColor(R.color.primary));
    }

    private void checkEmptyState() {
        if (addedCareTasks.isEmpty()) {
            tvNoCareTasks.setVisibility(View.VISIBLE);
            rvCareTasks.setVisibility(View.GONE);
        } else {
            tvNoCareTasks.setVisibility(View.GONE);
            rvCareTasks.setVisibility(View.VISIBLE);
        }
    }

    private void showSelectTaskDialog() {
        progressBar.setVisibility(View.VISIBLE);
        dbExecutor.execute(() -> {
            List<CareTaskEntity> entities = db.careTaskDao().getAllForUser(userId);
            List<CareTask> allTasks = new ArrayList<>();
            List<String> taskNames = new ArrayList<>();
            for (CareTaskEntity e : entities) {
                CareTask t = new CareTask(e.id, e.name, e.category, e.notes, e.userId, e.time, e.frequency, e.supplies);
                copyDatesFromEntity(t, e);
                t.setCompleted(e.completed);
                allTasks.add(t);
                taskNames.add(t.getName());
            }

            mainHandler.post(() -> {
                progressBar.setVisibility(View.GONE);
                if (allTasks.isEmpty()) {
                    Toast.makeText(this, "No saved care tasks found.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String[] namesArray = taskNames.toArray(new String[0]);
                new AlertDialog.Builder(this)
                        .setTitle("Select Care Task")
                        .setItems(namesArray, (dialog, which) -> {
                            CareTask selected = allTasks.get(which);
                            addedCareTasks.add(selected);
                            addedCareTasksAdapter.notifyDataSetChanged();
                            checkEmptyState();
                        })
                        .show();
            });
        });
    }

    private void showCreateTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_care_task, null);
        builder.setView(dialogView);

        EditText etTaskName = dialogView.findViewById(R.id.etTaskName);
        TextInputLayout tilTaskName = dialogView.findViewById(R.id.tilTaskName);
        AutoCompleteTextView etCategory = dialogView.findViewById(R.id.etCategory);
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
        EditText etNotesField = dialogView.findViewById(R.id.etNotes);
        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        tvTitle.setText("New Care Task");

        etCategory.setAdapter(new ArrayAdapter<>(this, R.layout.item_dropdown, CATEGORIES));
        etFrequency.setAdapter(new ArrayAdapter<>(this, R.layout.item_dropdown, FREQUENCIES));

        etTime.setOnClickListener(v -> TimePickerHelper.show(getSupportFragmentManager(), etTime));
        tilTime.setEndIconOnClickListener(v -> TimePickerHelper.show(getSupportFragmentManager(), etTime));
        setupDatePicker(etAppointmentDate, tilAppointmentDate);
        setupDatePicker(etGroomingDate, tilGroomingDate);
        setupDatePicker(etVaccinationDueDate, tilVaccinationDueDate);
        setupDatePicker(etMedicationStartDate, tilMedicationStartDate);
        setupDatePicker(etMedicationEndDate, tilMedicationEndDate);

        builder.setPositiveButton("Add", (dialog, which) -> {});
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = etTaskName.getText().toString().trim();
            String category = etCategory.getText().toString().trim();
            String frequency = etFrequency.getText().toString().trim();
            String time = etTime.getText().toString().trim();
            String appointmentDate = etAppointmentDate.getText().toString().trim();
            String groomingDate = etGroomingDate.getText().toString().trim();
            String vaccinationDueDate = etVaccinationDueDate.getText().toString().trim();
            String medicationStartDate = etMedicationStartDate.getText().toString().trim();
            String medicationEndDate = etMedicationEndDate.getText().toString().trim();
            String supplies = etSupplies.getText().toString().trim();
            String notes = etNotesField.getText().toString().trim();

            if (TextUtils.isEmpty(name)) {
                tilTaskName.setError("Task name required");
                Toast.makeText(this, "Please enter a task name.", Toast.LENGTH_SHORT).show();
                return;
            }
            tilTaskName.setError(null);
            saveNewCareTaskToFirestore(name, category, notes, time, frequency, supplies,
                    appointmentDate, groomingDate, vaccinationDueDate, medicationStartDate, medicationEndDate, dialog);
        });
    }

    private void saveNewCareTaskToFirestore(String name, String category, String notes, String time, String frequency, String supplies,
                                            String appointmentDate, String groomingDate, String vaccinationDueDate,
                                            String medicationStartDate, String medicationEndDate, AlertDialog dialog) {
        progressBar.setVisibility(View.VISIBLE);
        String taskId = UUID.randomUUID().toString();
        CareTask newTask = new CareTask(taskId, name, category, notes, userId, time, frequency, supplies);
        newTask.setAppointmentDate(appointmentDate);
        newTask.setGroomingDate(groomingDate);
        newTask.setVaccinationDueDate(vaccinationDueDate);
        newTask.setMedicationStartDate(medicationStartDate);
        newTask.setMedicationEndDate(medicationEndDate);

        CareTaskEntity entity = new CareTaskEntity();
        entity.id = taskId;
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

        dbExecutor.execute(() -> {
            db.careTaskDao().upsert(entity);
            mainHandler.post(() -> {
                progressBar.setVisibility(View.GONE);
                addedCareTasks.add(newTask);
                addedCareTasksAdapter.notifyDataSetChanged();
                checkEmptyState();
                dialog.dismiss();
                Toast.makeText(this, "Care Task Created & Added", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void savePet() {
        String name = etName.getText().toString().trim();
        String breed = etBreed.getText().toString().trim();
        String age = etAge.getText().toString().trim();
        String weight = etWeight.getText().toString().trim();
        String diet = etDiet.getText().toString().trim();
        String allergies = etAllergies.getText().toString().trim();
        String toys = etToys.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etName.setError("Pet Name Required");
            Toast.makeText(this, "Please enter a pet name.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (addedCareTasks.isEmpty()) {
            Toast.makeText(this, "Please add at least one care task", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        String id;
        if (isEditMode && editPetId != null) {
            id = editPetId;
        } else {
            id = UUID.randomUUID().toString();
        }

        PetEntity entity = new PetEntity();
        entity.id = id;
        entity.userId = userId;
        entity.name = name;
        entity.breed = breed;
        entity.age = age;
        entity.weight = weight;
        entity.dietaryPreferences = diet;
        entity.allergies = allergies;
        entity.favouriteToys = toys;
        entity.notes = notes;
        entity.imageUrl = uploadedImageUrl;
        entity.careTasksJson = CareTaskJson.toJson(addedCareTasks);
        entity.latitude = selectedLat;
        entity.longitude = selectedLng;
        entity.locationName = selectedLocationName;
        entity.locationType = selectedLocationType;

        dbExecutor.execute(() -> {
            if (isEditMode) {
                PetEntity oldEntity = db.petDao().getById(id, userId);
                if (oldEntity != null) {
                    ReminderScheduler.cancelPetReminders(this, id, CareTaskJson.fromJson(oldEntity.careTasksJson));
                    entity.vaccinationHistory = oldEntity.vaccinationHistory;
                    entity.medicationRecords = oldEntity.medicationRecords;
                    entity.vetVisitNotes = oldEntity.vetVisitNotes;
                    entity.nextVaccinationDate = oldEntity.nextVaccinationDate;
                    entity.treatmentNotes = oldEntity.treatmentNotes;
                    entity.photoGalleryJson = oldEntity.photoGalleryJson;
                }
            }
            db.petDao().upsert(entity);
            Pet savedPet = new Pet(entity.id, entity.userId, entity.name, entity.breed, entity.age,
                    entity.weight, entity.dietaryPreferences, entity.allergies, entity.favouriteToys,
                    entity.notes, entity.imageUrl, addedCareTasks, entity.latitude, entity.longitude,
                    entity.locationName, entity.locationType);
            ReminderScheduler.schedulePetReminders(this, savedPet);
            mainHandler.post(() -> {
                progressBar.setVisibility(View.GONE);
                String msg = isEditMode ? "Pet Updated!" : "Pet Profile Saved!";
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    private void confirmRemoveRoutineTask(CareTask careTask) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Care Task")
                .setMessage("Are you sure you want to remove " + careTask.getName() + " from this pet routine?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    addedCareTasks.remove(careTask);
                    addedCareTasksAdapter.notifyDataSetChanged();
                    checkEmptyState();
                    Toast.makeText(this, "Care task removed from routine.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void uploadImageToImgBB(Uri imageUri) {
        progressBarImage.setVisibility(View.VISIBLE);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                byte[] imageBytes = getBytes(inputStream);

                OkHttpClient client = new OkHttpClient();

                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("image", "image.jpg",
                                RequestBody.create(imageBytes, MediaType.parse("image/*")))
                        .build();

                Request request = new Request.Builder()
                        .url("https://api.imgbb.com/1/upload?key=" + IMGBB_API_KEY)
                        .post(requestBody)
                        .build();

                Response response = client.newCall(request).execute();
                String responseData = response.body().string();

                if (response.isSuccessful()) {
                    JSONObject json = new JSONObject(responseData);
                    String url = json.getJSONObject("data").getString("url");

                    handler.post(() -> {
                        uploadedImageUrl = url;
                        progressBarImage.setVisibility(View.GONE);
                        ivPetImage.setColorFilter(null);
                        ivPetImage.setImageTintList(null);
                        layoutPlaceholder.setVisibility(View.GONE);
                        Picasso.get().load(url).into(ivPetImage);
                        Toast.makeText(CreatePetActivity.this, "Image uploaded!", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    handler.post(() -> {
                        progressBarImage.setVisibility(View.GONE);
                        Toast.makeText(CreatePetActivity.this, "Upload failed: " + response.message(), Toast.LENGTH_SHORT).show();
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                handler.post(() -> {
                    progressBarImage.setVisibility(View.GONE);
                    Toast.makeText(CreatePetActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
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
}
