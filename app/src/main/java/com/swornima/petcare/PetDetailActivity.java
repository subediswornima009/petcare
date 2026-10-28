package com.swornima.petcare;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;
import com.google.android.material.chip.ChipGroup;
import com.swornima.petcare.db.AppDatabase;
import com.swornima.petcare.db.CareTaskJson;
import com.swornima.petcare.db.PetEntity;
import com.swornima.petcare.db.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Pet Detail screen: shows the pet's profile plus its auto-generated care
 * checklist (grouped implicitly by category on each row) and its geotagged
 * location if one has been pinned.
 *
 * This screen also implements the core "Item Delegation" requirement: the
 * "Delegate Care" button opens a contact picker and sends the pet's full
 * care checklist as a real SMS message via {@link SmsManager}, requesting
 * SEND_SMS and READ_CONTACTS permissions at runtime as required on
 * Android 6.0+.
 */
public class PetDetailActivity extends AppCompatActivity {

    private static final int REQUEST_SMS_PERMISSION = 100;
    private static final int REQUEST_PICK_CONTACT = 101;

    private ImageView ivPetImage;
    private TextView tvName, tvBreed, tvStatus, tvProgressCount, tvLocationTitle, tvLocationName;
    private TextView tvPhotoEmpty;
    private ProgressBar progressBar, loadingIndicator;
    private RecyclerView recyclerView, rvPetPhotos;
    private ChipGroup chipGroupCategoryFilter;

    private PetChecklistAdapter adapter;
    private PetPhotoAdapter photoAdapter;
    private Pet currentPet;
    private final List<CareTask> filteredTasks = new ArrayList<>();
    private final List<String> petPhotoUris = new ArrayList<>();
    private String selectedCategory = "All";
    private String petId;
    private String pendingPhoneNumber;
    private boolean checklistWasComplete;
    private boolean photoPickerOpen;

    private AppDatabase db;
    private String userId;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ActivityResultLauncher<Intent> pickPetPhotoLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                    addPetPhoto(result.getData().getData());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pet_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");

        db = AppDatabase.getInstance(this);
        userId = new SessionManager(this).getCurrentUserId();

        petId = getIntent().getStringExtra("pet_id");
        if (petId == null) {
            Toast.makeText(this, "Error loading pet", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        fetchPetDetails();

        findViewById(R.id.btnDelegateCare).setOnClickListener(v -> {
            Intent intent = new Intent(this, DelegationPreviewActivity.class);
            intent.putExtra("pet_id", petId);
            startActivity(intent);
        });
        findViewById(R.id.btnMedicalRecords).setOnClickListener(v -> {
            Intent intent = new Intent(this, MedicalRecordsActivity.class);
            intent.putExtra("pet_id", petId);
            startActivity(intent);
        });
        findViewById(R.id.btnAddPetPhoto).setOnClickListener(v -> openGalleryForPetPhoto());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (petId != null && db != null) {
            // Returning from the gallery can happen before the photo result
            // callback finishes. Do not reload the old database list and
            // overwrite the photo that is about to be added.
            if (photoPickerOpen) {
                photoPickerOpen = false;
                return;
            }
            fetchPetDetails();
        }
    }

    private void openGalleryForPetPhoto() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        photoPickerOpen = true;
        pickPetPhotoLauncher.launch(intent);
    }

    private void pickContactForDelegation() {
        if (currentPet == null)
            return;

        // Ask for SEND_SMS permission upfront; the actual send happens once
        // a contact has been chosen and permission is confirmed.
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult(intent, REQUEST_PICK_CONTACT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_CONTACT && resultCode == RESULT_OK && data != null) {
            Uri contactUri = data.getData();
            String[] projection = {android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER};
            android.database.Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int numberIndex = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER);
                pendingPhoneNumber = cursor.getString(numberIndex);
                cursor.close();

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, REQUEST_SMS_PERMISSION);
                } else {
                    showDelegationPreview(pendingPhoneNumber);
                }
            } else {
                Toast.makeText(this, "Could not read contact number", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_SMS_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showDelegationPreview(pendingPhoneNumber);
            } else {
                Toast.makeText(this, "SMS permission is required to delegate care tasks", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showDelegationPreview(String phoneNumber) {
        if (phoneNumber == null || currentPet == null) return;

        String message = buildDelegationMessage();
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Preview SMS")
                .setMessage(message)
                .setPositiveButton("Send", (dialog, which) -> sendDelegationSms(phoneNumber, message))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Builds a plain-text summary of the pet's care checklist (feeding
     * schedule, medication reminders, walking times, etc.) and sends it as
     * an SMS to the chosen contact using SmsManager. Long messages are
     * automatically split into multiple parts via divideMessage().
     */
    private void sendDelegationSms(String phoneNumber, String message) {
        if (phoneNumber == null || currentPet == null) return;

        try {
            SmsManager smsManager = SmsManager.getDefault();
            List<String> parts = smsManager.divideMessage(message);
            smsManager.sendMultipartTextMessage(phoneNumber, null, (ArrayList<String>) parts, null, null);
            Toast.makeText(this, "Care checklist sent via SMS!", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to send SMS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String buildDelegationMessage() {
        StringBuilder body = new StringBuilder();
        body.append("PetCare Care Checklist\n");
        body.append("Pet: ").append(valueOrDefault(currentPet.getName(), "Unnamed pet")).append("\n");
        if (hasText(currentPet.getBreed())) {
            body.append("Breed: ").append(currentPet.getBreed().trim()).append("\n");
        }
        if (hasText(currentPet.getDietaryPreferences())) {
            body.append("Diet: ").append(currentPet.getDietaryPreferences().trim()).append("\n");
        }
        if (hasText(currentPet.getAllergies())) {
            body.append("Allergies: ").append(currentPet.getAllergies().trim()).append("\n");
        }

        if (hasText(currentPet.getLocationName())) {
            body.append("Preferred location: ").append(currentPet.getLocationName().trim()).append("\n");
        }

        Map<String, List<CareTask>> tasksByCategory = groupCareTasksByCategory(currentPet.getCareTasks());
        if (tasksByCategory.isEmpty()) {
            body.append("\nNo care tasks assigned.\n");
        } else {
            body.append("\nCare Checklist:\n");
            for (Map.Entry<String, List<CareTask>> entry : tasksByCategory.entrySet()) {
                body.append("\n").append(entry.getKey().toUpperCase(Locale.US)).append("\n");
                for (CareTask task : entry.getValue()) {
                    appendDelegationTask(body, task);
                }
            }
        }

        body.append("\nSent via PetCare App");
        return body.toString();
    }

    private Map<String, List<CareTask>> groupCareTasksByCategory(List<CareTask> tasks) {
        Map<String, List<CareTask>> grouped = new LinkedHashMap<>();
        grouped.put("Feeding", new ArrayList<>());
        grouped.put("Exercise", new ArrayList<>());
        grouped.put("Grooming", new ArrayList<>());
        grouped.put("Medication", new ArrayList<>());
        grouped.put("Healthcare", new ArrayList<>());

        if (tasks != null) {
            for (CareTask task : tasks) {
                String category = valueOrDefault(task.getCategory(), "Other");
                if (!grouped.containsKey(category)) {
                    grouped.put(category, new ArrayList<>());
                }
                grouped.get(category).add(task);
            }
        }

        Map<String, List<CareTask>> nonEmptyGroups = new LinkedHashMap<>();
        for (Map.Entry<String, List<CareTask>> entry : grouped.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                nonEmptyGroups.put(entry.getKey(), entry.getValue());
            }
        }
        return nonEmptyGroups;
    }

    private void appendDelegationTask(StringBuilder body, CareTask task) {
        body.append("- ").append(valueOrDefault(task.getName(), "Care task"));

        List<String> scheduleParts = new ArrayList<>();
        if (hasText(task.getTime())) scheduleParts.add(task.getTime().trim());
        if (hasText(task.getFrequency())) scheduleParts.add(task.getFrequency().trim());
        if (!scheduleParts.isEmpty()) {
            body.append(" | Schedule: ").append(joinParts(scheduleParts));
        }

        List<String> dateParts = new ArrayList<>();
        addPartIfPresent(dateParts, "Vet", task.getAppointmentDate());
        addPartIfPresent(dateParts, "Grooming", task.getGroomingDate());
        addPartIfPresent(dateParts, "Vaccination", task.getVaccinationDueDate());
        if (hasText(task.getMedicationStartDate()) || hasText(task.getMedicationEndDate())) {
            dateParts.add("Medication " + safeDate(task.getMedicationStartDate()) + " to " + safeDate(task.getMedicationEndDate()));
        }
        if (!dateParts.isEmpty()) {
            body.append(" | Dates: ").append(joinParts(dateParts));
        }

        if (hasText(task.getSupplies())) {
            body.append(" | Supplies: ").append(task.getSupplies().trim());
        }
        if (hasText(task.getNotes())) {
            body.append(" | Notes: ").append(task.getNotes().trim());
        }
        body.append("\n");
    }

    private void addPartIfPresent(List<String> parts, String label, String value) {
        if (hasText(value)) {
            parts.add(label + " " + value.trim());
        }
    }

    private String joinParts(List<String> parts) {
        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) joined.append(", ");
            joined.append(parts.get(i));
        }
        return joined.toString();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String valueOrDefault(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private String safeDate(String value) {
        return hasText(value) ? value.trim() : "not set";
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void initializeViews() {
        ivPetImage = findViewById(R.id.ivPetImage);
        tvName = findViewById(R.id.tvPetName);
        tvBreed = findViewById(R.id.tvPetBreed);
        tvStatus = findViewById(R.id.tvStatus);
        tvProgressCount = findViewById(R.id.tvProgressCount);
        tvLocationTitle = findViewById(R.id.tvLocationTitle);
        tvLocationName = findViewById(R.id.tvLocationName);
        progressBar = findViewById(R.id.progressBarPet);
        loadingIndicator = findViewById(R.id.loadingIndicator);
        recyclerView = findViewById(R.id.rvPetCareTasks);
        rvPetPhotos = findViewById(R.id.rvPetPhotos);
        tvPhotoEmpty = findViewById(R.id.tvPhotoEmpty);
        chipGroupCategoryFilter = findViewById(R.id.chipGroupCategoryFilter);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        rvPetPhotos.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        photoAdapter = new PetPhotoAdapter(this, petPhotoUris, new PetPhotoAdapter.OnPhotoActionListener() {
            @Override
            public void onPhotoClick(String photoUri) {
                showFullPetPhoto(photoUri);
            }

            @Override
            public void onDeleteClick(String photoUri) {
                confirmDeletePetPhoto(photoUri);
            }
        });
        rvPetPhotos.setAdapter(photoAdapter);
    }

    private void fetchPetDetails() {
        loadingIndicator.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            PetEntity entity = db.petDao().getById(petId, userId);
            mainHandler.post(() -> {
                loadingIndicator.setVisibility(View.GONE);
                if (entity != null) {
                    currentPet = new Pet(entity.id, entity.userId, entity.name, entity.breed, entity.age,
                            entity.weight, entity.dietaryPreferences, entity.allergies, entity.favouriteToys,
                            entity.notes, entity.imageUrl, CareTaskJson.fromJson(entity.careTasksJson),
                            entity.latitude, entity.longitude, entity.locationName, entity.locationType);
                    petPhotoUris.clear();
                    List<String> storedPhotos = photoGalleryFromJson(entity.photoGalleryJson);
                    List<String> readablePhotos = readablePhotoUris(storedPhotos);
                    petPhotoUris.addAll(readablePhotos);
                    if (readablePhotos.size() != storedPhotos.size()) {
                        saveCleanedPhotoGallery(readablePhotos);
                    }
                    populateUI();
                } else {
                    Toast.makeText(this, "Pet not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        });
    }

    private void populateUI() {
        tvName.setText(currentPet.getName());
        tvBreed.setText(currentPet.getBreed());

        if (currentPet.getImageUrl() != null && !currentPet.getImageUrl().isEmpty()) {
            Picasso.get().load(currentPet.getImageUrl()).into(ivPetImage);
        } else {
            ivPetImage.setImageDrawable(null);
        }

        if (currentPet.getLatitude() != 0 && currentPet.getLongitude() != 0) {
            tvLocationTitle.setVisibility(View.VISIBLE);
            tvLocationName.setVisibility(View.VISIBLE);
            String label = currentPet.getLocationName() != null ? currentPet.getLocationName() : "Pinned Location";
            if (currentPet.getLocationType() != null && !currentPet.getLocationType().isEmpty()) {
                label = currentPet.getLocationType() + ": " + label;
            }
            tvLocationName.setText(label);

            tvLocationName.setOnClickListener(v -> {
                String uri = String.format(Locale.ENGLISH, "geo:%f,%f?q=%f,%f(%s)",
                        currentPet.getLatitude(), currentPet.getLongitude(),
                        currentPet.getLatitude(), currentPet.getLongitude(),
                        currentPet.getLocationName());
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                startActivity(intent);
            });
        } else {
            tvLocationTitle.setVisibility(View.GONE);
            tvLocationName.setVisibility(View.GONE);
        }

        if (currentPet.getCareTasks() == null)
            currentPet.setCareTasks(new ArrayList<>());

        boolean resetApplied = ChecklistResetHelper.resetExpiredTasks(currentPet.getCareTasks());
        if (resetApplied) {
            saveProgress();
        }

        filteredTasks.clear();
        filteredTasks.addAll(currentPet.getCareTasks());
        adapter = new PetChecklistAdapter(this, filteredTasks, (careTask, isChecked) -> {
            updateProgress();
            saveProgress();
            showChecklistCompletionMessageIfNeeded();
        });
        recyclerView.setAdapter(adapter);
        setupCategoryFilter();
        applyCategoryFilter();

        updateProgress();
        checklistWasComplete = isChecklistComplete();
        refreshPhotoGallery();
    }

    private void addPetPhoto(Uri sourcePhotoUri) {
        executor.execute(() -> {
            try {
                String storedPhotoUri = ImageStorageUtil.copyImageToPrivateStorage(this, sourcePhotoUri).toString();
                mainHandler.post(() -> {
                    if (petPhotoUris.contains(storedPhotoUri)) {
                        Toast.makeText(this, "This photo is already in the gallery.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    petPhotoUris.add(storedPhotoUri);
                    refreshPhotoGallery();
                    savePhotoGallery("Photo added successfully.");
                });
            } catch (Exception e) {
                mainHandler.post(() -> Toast.makeText(this, "Unable to add selected photo.", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void confirmDeletePetPhoto(String photoUri) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Photo")
                .setMessage("Are you sure you want to delete this pet photo?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    petPhotoUris.remove(photoUri);
                    ImageStorageUtil.deletePrivateImageIfOwned(this, photoUri);
                    savePhotoGallery("Photo deleted successfully.");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showFullPetPhoto(String photoUri) {
        ImageView imageView = new ImageView(this);
        try {
            imageView.setImageURI(Uri.parse(photoUri));
        } catch (SecurityException | IllegalArgumentException e) {
            imageView.setImageResource(R.drawable.ic_pet);
        }
        imageView.setAdjustViewBounds(true);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setBackgroundColor(getColor(R.color.black));
        int padding = (int) (12 * getResources().getDisplayMetrics().density);
        imageView.setPadding(padding, padding, padding, padding);

        FrameLayout container = new FrameLayout(this);
        container.setPadding(0, padding, 0, padding);
        container.addView(imageView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                (int) (420 * getResources().getDisplayMetrics().density)
        ));

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Pet Photo")
                .setView(container)
                .setPositiveButton("Close", null)
                .show();
    }

    private void savePhotoGallery(String successMessage) {
        String photoGalleryJson = photoGalleryToJson(new ArrayList<>(petPhotoUris));
        executor.execute(() -> {
            PetEntity entity = db.petDao().getById(petId, userId);
            if (entity != null) {
                entity.photoGalleryJson = photoGalleryJson;
                db.petDao().upsert(entity);
            }
            mainHandler.post(() -> {
                refreshPhotoGallery();
                Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void refreshPhotoGallery() {
        photoAdapter.notifyDataSetChanged();
        boolean empty = petPhotoUris.isEmpty();
        tvPhotoEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvPetPhotos.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private List<String> photoGalleryFromJson(String json) {
        List<String> photos = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) return photos;
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                String uri = array.optString(i, "");
                if (!uri.trim().isEmpty()) {
                    photos.add(uri);
                }
            }
        } catch (JSONException e) {
            return photos;
        }
        return photos;
    }

    private List<String> readablePhotoUris(List<String> photos) {
        List<String> readablePhotos = new ArrayList<>();
        if (photos == null) return readablePhotos;

        for (String photo : photos) {
            if (isReadablePhotoUri(photo)) {
                readablePhotos.add(photo);
            }
        }
        return readablePhotos;
    }

    private boolean isReadablePhotoUri(String photoUri) {
        if (!hasText(photoUri)) return false;

        try {
            Uri uri = Uri.parse(photoUri);
            if ("file".equals(uri.getScheme())) {
                return uri.getPath() != null && new java.io.File(uri.getPath()).exists();
            }
            try (java.io.InputStream ignored = getContentResolver().openInputStream(uri)) {
                return ignored != null;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private void saveCleanedPhotoGallery(List<String> readablePhotos) {
        executor.execute(() -> {
            PetEntity entity = db.petDao().getById(petId, userId);
            if (entity != null) {
                entity.photoGalleryJson = photoGalleryToJson(readablePhotos);
                db.petDao().upsert(entity);
            }
        });
    }

    private String photoGalleryToJson(List<String> photos) {
        JSONArray array = new JSONArray();
        if (photos != null) {
            for (String photo : photos) {
                array.put(photo);
            }
        }
        return array.toString();
    }

    private void setupCategoryFilter() {
        chipGroupCategoryFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                selectedCategory = "All";
            } else {
                int checkedId = checkedIds.get(0);
                if (checkedId == R.id.chipFeeding) selectedCategory = "Feeding";
                else if (checkedId == R.id.chipExercise) selectedCategory = "Exercise";
                else if (checkedId == R.id.chipGrooming) selectedCategory = "Grooming";
                else if (checkedId == R.id.chipMedication) selectedCategory = "Medication";
                else if (checkedId == R.id.chipHealthcare) selectedCategory = "Healthcare";
                else selectedCategory = "All";
            }
            applyCategoryFilter();
        });
    }

    private void applyCategoryFilter() {
        if (adapter == null || currentPet == null || currentPet.getCareTasks() == null) return;

        List<CareTask> visible = new ArrayList<>();
        for (CareTask task : currentPet.getCareTasks()) {
            if ("All".equals(selectedCategory) ||
                    (task.getCategory() != null && task.getCategory().equalsIgnoreCase(selectedCategory))) {
                visible.add(task);
            }
        }
        adapter.replaceTasks(visible);
    }

    private void updateProgress() {
        if (currentPet.getCareTasks() == null || currentPet.getCareTasks().isEmpty()) {
            tvStatus.setText("Empty");
            tvProgressCount.setText("0/0");
            progressBar.setProgress(0);
            return;
        }

        int total = currentPet.getCareTasks().size();
        int completed = 0;
        for (CareTask t : currentPet.getCareTasks()) {
            if (t.isCompleted())
                completed++;
        }

        int progress = (int) (((float) completed / total) * 100);
        progressBar.setProgress(progress);
        tvProgressCount.setText(completed + "/" + total + " Completed");

        if (completed == total) {
            tvStatus.setText("Completed");
            tvStatus.setTextColor(getColor(R.color.accent));
        } else {
            tvStatus.setText("In Progress");
            tvStatus.setTextColor(getColor(R.color.text_primary));
        }
    }

    private boolean isChecklistComplete() {
        if (currentPet == null || currentPet.getCareTasks() == null || currentPet.getCareTasks().isEmpty()) {
            return false;
        }

        for (CareTask task : currentPet.getCareTasks()) {
            if (!task.isCompleted()) {
                return false;
            }
        }
        return true;
    }

    private void showChecklistCompletionMessageIfNeeded() {
        boolean checklistComplete = isChecklistComplete();
        if (!checklistWasComplete && checklistComplete) {
            String petName = currentPet != null && currentPet.getName() != null
                    ? currentPet.getName()
                    : "your pet";
            Toast.makeText(this,
                    "All care tasks for " + petName + " are completed!",
                    Toast.LENGTH_LONG).show();
        }
        checklistWasComplete = checklistComplete;
    }

    private void saveProgress() {
        executor.execute(() -> {
            PetEntity entity = db.petDao().getById(petId, userId);
            if (entity != null) {
                entity.careTasksJson = CareTaskJson.toJson(currentPet.getCareTasks());
                db.petDao().upsert(entity);
            }
        });
    }
}
