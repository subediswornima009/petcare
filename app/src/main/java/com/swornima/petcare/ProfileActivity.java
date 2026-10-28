package com.swornima.petcare;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.swornima.petcare.db.AppDatabase;
import com.swornima.petcare.db.CareTaskEntity;
import com.swornima.petcare.db.CareTaskJson;
import com.swornima.petcare.db.ExpenseEntity;
import com.swornima.petcare.db.PasswordHasher;
import com.swornima.petcare.db.PetEntity;
import com.swornima.petcare.db.PlaceEntity;
import com.swornima.petcare.db.SessionManager;
import com.swornima.petcare.db.UserEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Account profile screen: update display name, change password, and (for
 * demo/testing purposes) generate sample pets and care tasks so markers can
 * quickly see the app populated with realistic PetCare data (Max the Golden
 * Retriever and Luna the cat, matching the assignment scenario).
 */
public class ProfileActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etCurrentPass, etNewPass;
    private MaterialButton btnUpdateProfile, btnUpdatePassword, btnGenerateData, btnLogout, btnChangePhoto;
    private ImageView ivProfilePhoto;
    private ProgressBar progressBar;

    private AppDatabase db;
    private SessionManager sessionManager;
    private String userId;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ActivityResultLauncher<Intent> pickProfilePhotoLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                    saveProfilePhoto(result.getData().getData());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        db = AppDatabase.getInstance(this);
        sessionManager = new SessionManager(this);
        userId = sessionManager.getCurrentUserId();

        initializeViews();
        loadUserProfile();
        setupListeners();
    }

    private void initializeViews() {
        etName = findViewById(R.id.etProfileName);
        etEmail = findViewById(R.id.etProfileEmail);
        etCurrentPass = findViewById(R.id.etCurrentPassword);
        etNewPass = findViewById(R.id.etNewPassword);
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);
        btnUpdateProfile = findViewById(R.id.btnUpdateProfile);
        btnUpdatePassword = findViewById(R.id.btnUpdatePassword);
        btnGenerateData = findViewById(R.id.btnGenerateData);
        btnLogout = findViewById(R.id.btnLogout);
        progressBar = findViewById(R.id.progressBar);
    }

    private void loadUserProfile() {
        if (userId == null)
            return;

        progressBar.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            UserEntity user = db.userDao().findByUid(userId);
            mainHandler.post(() -> {
                progressBar.setVisibility(View.GONE);
                if (user != null) {
                    etEmail.setText(user.email);
                    String fullName = user.fullName;
                    if (fullName != null)
                        etName.setText(fullName);
                    if (user.photoUri != null && !user.photoUri.trim().isEmpty()) {
                        ivProfilePhoto.setColorFilter(null);
                        ivProfilePhoto.setImageTintList(null);
                        try {
                            ivProfilePhoto.setImageURI(Uri.parse(user.photoUri));
                        } catch (SecurityException | IllegalArgumentException e) {
                            ivProfilePhoto.setImageResource(R.drawable.ic_pet);
                        }
                    }
                } else {
                    Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void setupListeners() {
        btnUpdateProfile.setOnClickListener(v -> updateProfile());
        btnUpdatePassword.setOnClickListener(v -> updatePassword());
        btnGenerateData.setOnClickListener(v -> generateSampleData());
        btnLogout.setOnClickListener(v -> logout());
        btnChangePhoto.setOnClickListener(v -> openGalleryForProfilePhoto());
        ivProfilePhoto.setOnClickListener(v -> openGalleryForProfilePhoto());
    }

    private void openGalleryForProfilePhoto() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        pickProfilePhotoLauncher.launch(intent);
    }

    private void saveProfilePhoto(Uri photoUri) {
        progressBar.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            UserEntity user = db.userDao().findByUid(userId);
            Uri savedPhotoUri = photoUri;
            if (user != null) {
                try {
                    Uri storedPhotoUri = ImageStorageUtil.copyImageToPrivateStorage(this, photoUri);
                    ImageStorageUtil.deletePrivateImageIfOwned(this, user.photoUri);
                    user.photoUri = storedPhotoUri.toString();
                    savedPhotoUri = storedPhotoUri;
                } catch (Exception e) {
                    user = null;
                }
            }
            if (user != null) {
                db.userDao().update(user);
            }
            final UserEntity savedUser = user;
            final Uri displayPhotoUri = savedPhotoUri;
            mainHandler.post(() -> {
                progressBar.setVisibility(View.GONE);
                if (savedUser == null) {
                    Toast.makeText(this, "Unable to save profile photo.", Toast.LENGTH_SHORT).show();
                } else {
                    ivProfilePhoto.setColorFilter(null);
                    ivProfilePhoto.setImageTintList(null);
                    ivProfilePhoto.setImageURI(displayPhotoUri);
                    Toast.makeText(this, "Profile photo updated.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void logout() {
        sessionManager.clear();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void updateProfile() {
        String newName = etName.getText().toString().trim();
        if (TextUtils.isEmpty(newName)) {
            etName.setError("Name required");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            UserEntity user = db.userDao().findByUid(userId);
            if (user != null) {
                user.fullName = newName;
                db.userDao().update(user);
            }
            mainHandler.post(() -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void updatePassword() {
        String current = etCurrentPass.getText().toString();
        String newPass = etNewPass.getText().toString();

        if (TextUtils.isEmpty(current)) {
            etCurrentPass.setError("Current password required");
            return;
        }
        if (TextUtils.isEmpty(newPass)) {
            etNewPass.setError("New password required");
            return;
        }
        if (newPass.length() < 6) {
            etNewPass.setError("Min 6 chars");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        executor.execute(() -> {
            UserEntity user = db.userDao().findByUid(userId);
            boolean matches = user != null && PasswordHasher.matches(current, user.passwordHash);

            if (matches) {
                user.passwordHash = PasswordHasher.hash(newPass);
                db.userDao().update(user);
            }

            mainHandler.post(() -> {
                progressBar.setVisibility(View.GONE);
                if (matches) {
                    Toast.makeText(this, "Password Updated", Toast.LENGTH_SHORT).show();
                    etCurrentPass.setText("");
                    etNewPass.setText("");
                } else {
                    etCurrentPass.setError("Incorrect Password");
                    Toast.makeText(this, "Authentication Failed", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void generateSampleData() {
        if (userId == null)
            return;

        progressBar.setVisibility(View.VISIBLE);
        String uid = userId;

        executor.execute(() -> {
            List<PetEntity> oldPets = db.petDao().getAllForUser(uid);
            for (PetEntity oldPet : oldPets) {
                ReminderScheduler.cancelPetReminders(this, oldPet.id, CareTaskJson.fromJson(oldPet.careTasksJson));
            }
            db.expenseDao().deleteAllForUser(uid);
            db.placeDao().deleteAllForUser(uid);
            db.petDao().deleteAllForUser(uid);
            db.careTaskDao().deleteAllForUser(uid);

            // 1. Create exactly 10 reusable care routines.
            List<CareTask> allTasks = new ArrayList<>();
            allTasks.add(new CareTask(null, "Morning Feeding", "Feeding",
                    "1 cup of dry kibble mixed with wet food.", uid, "07:00 AM", "Daily", "Dog food, water bowl"));
            allTasks.add(new CareTask(null, "Evening Feeding", "Feeding",
                    "Half cup of dry kibble.", uid, "06:00 PM", "Daily", "Dog food, water bowl"));
            allTasks.add(new CareTask(null, "Morning Walk", "Exercise",
                    "20-30 minute walk around the block.", uid, "07:30 AM", "Daily", "Leash, waste bags"));
            allTasks.add(new CareTask(null, "Evening Walk", "Exercise",
                    "Short walk before bedtime.", uid, "08:00 PM", "Daily", "Leash, waste bags"));
            allTasks.add(new CareTask(null, "Brushing", "Grooming",
                    "Brush coat to prevent matting and reduce shedding.", uid, "05:00 PM", "Weekly", "Pet brush"));
            allTasks.add(new CareTask(null, "Flea Treatment", "Medication",
                    "Apply topical flea and tick treatment.", uid, "09:00 AM", "Monthly", "Flea treatment"));
            allTasks.add(new CareTask(null, "Litter Box Cleaning", "Grooming",
                    "Scoop litter box and top up litter.", uid, "08:00 AM", "Daily", "Litter scoop, litter"));
            allTasks.add(new CareTask(null, "Annual Health Check", "Healthcare",
                    "Routine wellness check and vaccination review.", uid, "10:00 AM", "One-off", "Vaccination records"));
            allTasks.add(new CareTask(null, "Medication Dose", "Medication",
                    "Give prescribed tablet with food.", uid, "08:30 AM", "Daily", "Tablet, pill pocket"));
            allTasks.add(new CareTask(null, "Water Bowl Refresh", "Feeding",
                    "Wash and refill water bowl.", uid, "12:00 PM", "Daily", "Fresh water, clean bowl"));

            allTasks.get(4).setGroomingDate("2026-09-21");
            allTasks.get(5).setMedicationStartDate("2026-09-15");
            allTasks.get(5).setMedicationEndDate("2026-10-15");
            allTasks.get(7).setAppointmentDate("2026-10-05");
            allTasks.get(7).setVaccinationDueDate("2026-10-05");
            allTasks.get(8).setMedicationStartDate("2026-09-14");
            allTasks.get(8).setMedicationEndDate("2026-09-28");

            for (CareTask task : allTasks) {
                task.setId(UUID.randomUUID().toString());
                CareTaskEntity entity = new CareTaskEntity();
                entity.id = task.getId();
                entity.name = task.getName();
                entity.category = task.getCategory();
                entity.notes = task.getNotes();
                entity.time = task.getTime();
                entity.frequency = task.getFrequency();
                entity.supplies = task.getSupplies();
                entity.appointmentDate = task.getAppointmentDate();
                entity.groomingDate = task.getGroomingDate();
                entity.vaccinationDueDate = task.getVaccinationDueDate();
                entity.medicationStartDate = task.getMedicationStartDate();
                entity.medicationEndDate = task.getMedicationEndDate();
                entity.userId = uid;
                entity.completed = false;
                db.careTaskDao().upsert(entity);
            }

            List<CareTask> maxTasks = new ArrayList<>(allTasks.subList(0, 6));
            List<CareTask> lunaTasks = new ArrayList<>();
            lunaTasks.add(allTasks.get(0));
            lunaTasks.add(allTasks.get(6));
            lunaTasks.add(allTasks.get(4));
            lunaTasks.add(allTasks.get(7));
            List<CareTask> bellaTasks = new ArrayList<>();
            bellaTasks.add(allTasks.get(0));
            bellaTasks.add(allTasks.get(2));
            bellaTasks.add(allTasks.get(4));
            bellaTasks.add(allTasks.get(9));
            List<CareTask> charlieTasks = new ArrayList<>();
            charlieTasks.add(allTasks.get(1));
            charlieTasks.add(allTasks.get(5));
            charlieTasks.add(allTasks.get(8));
            charlieTasks.add(allTasks.get(7));
            List<CareTask> cocoTasks = new ArrayList<>();
            cocoTasks.add(allTasks.get(0));
            cocoTasks.add(allTasks.get(6));
            cocoTasks.add(allTasks.get(9));
            cocoTasks.add(allTasks.get(4));

            // 2. Create exactly 5 sample pets.
            Pet pet1 = new Pet(
                    UUID.randomUUID().toString(),
                    uid,
                    "Max",
                    "Golden Retriever",
                    "3 years",
                    "30 kg",
                    "Dry kibble, chicken and rice",
                    "None known",
                    "Tennis ball, rope toy",
                    "Loves the dog park on weekends.",
                    "https://images.unsplash.com/photo-1552053831-71594a27632d?auto=format&fit=crop&w=800",
                    maxTasks,
                    27.7172,
                    85.3240,
                    "Kathmandu Veterinary Clinic",
                    "Veterinary Clinic");

            Pet pet2 = new Pet(
                    UUID.randomUUID().toString(),
                    uid,
                    "Luna",
                    "Domestic Shorthair Cat",
                    "2 years",
                    "4 kg",
                    "Wet food, occasional treats",
                    "Sensitive to dairy",
                    "Feather wand, laser pointer",
                    "Prefers a quiet litter box location.",
                    "https://images.unsplash.com/photo-1533738363-b7f9aef128ce?auto=format&fit=crop&w=800",
                    lunaTasks,
                    27.7000,
                    85.3000,
                    "Central Pet Grooming Salon",
                    "Grooming Salon");

            Pet pet3 = new Pet(
                    UUID.randomUUID().toString(),
                    uid,
                    "Bella",
                    "Beagle",
                    "4 years",
                    "12 kg",
                    "Grain-free kibble",
                    "Chicken sensitivity",
                    "Squeaky duck, puzzle feeder",
                    "Needs extra exercise to stay calm.",
                    "https://images.unsplash.com/photo-1505628346881-b72b27e84530?auto=format&fit=crop&w=800",
                    bellaTasks,
                    27.7090,
                    85.3150,
                    "Greenway Dog Park",
                    "Dog Park");

            Pet pet4 = new Pet(
                    UUID.randomUUID().toString(),
                    uid,
                    "Charlie",
                    "Rabbit",
                    "1 year",
                    "2 kg",
                    "Hay, leafy greens, pellets",
                    "None known",
                    "Willow ball, tunnel",
                    "Keep nails trimmed and monitor teeth.",
                    "https://images.unsplash.com/photo-1585110396000-c9ffd4e4b308?auto=format&fit=crop&w=800",
                    charlieTasks,
                    27.7135,
                    85.3220,
                    "Happy Paws Veterinary Clinic",
                    "Veterinary Clinic");

            Pet pet5 = new Pet(
                    UUID.randomUUID().toString(),
                    uid,
                    "Coco",
                    "Persian Cat",
                    "5 years",
                    "5 kg",
                    "Wet food, hairball treats",
                    "Dust sensitivity",
                    "Cat tree, soft mouse toy",
                    "Daily brushing helps prevent matting.",
                    "https://images.unsplash.com/photo-1518791841217-8f162f1e1131?auto=format&fit=crop&w=800",
                    cocoTasks,
                    27.7160,
                    85.3050,
                    "Pet Supply Corner",
                    "Pet Supply Store");

            PetEntity pet1Entity = toEntity(pet1);
            pet1Entity.vaccinationHistory = "Rabies and DHPP boosters completed this year.";
            pet1Entity.medicationRecords = "Monthly flea and tick treatment.";
            pet1Entity.vetVisitNotes = "Healthy weight and coat. Review diet at next visit.";
            pet1Entity.nextVaccinationDate = "2026-11-15";
            pet1Entity.treatmentNotes = "Monitor ears after swimming.";

            PetEntity pet2Entity = toEntity(pet2);
            pet2Entity.vaccinationHistory = "Annual vaccination due soon.";
            pet2Entity.medicationRecords = "No regular medication.";
            pet2Entity.vetVisitNotes = "Sensitive stomach discussed with vet.";
            pet2Entity.nextVaccinationDate = "2026-10-05";
            pet2Entity.treatmentNotes = "Avoid dairy treats and monitor appetite.";

            PetEntity pet3Entity = toEntity(pet3);
            pet3Entity.vaccinationHistory = "Kennel cough vaccine completed.";
            pet3Entity.medicationRecords = "No daily medication.";
            pet3Entity.vetVisitNotes = "Weight check recommended next month.";
            pet3Entity.nextVaccinationDate = "2026-12-01";
            pet3Entity.treatmentNotes = "Avoid chicken-based treats.";

            PetEntity pet4Entity = toEntity(pet4);
            pet4Entity.vaccinationHistory = "Rabbit vaccination record pending.";
            pet4Entity.medicationRecords = "Short medication course starts this week.";
            pet4Entity.vetVisitNotes = "Dental check booked.";
            pet4Entity.nextVaccinationDate = "2026-10-20";
            pet4Entity.treatmentNotes = "Monitor appetite and droppings daily.";

            PetEntity pet5Entity = toEntity(pet5);
            pet5Entity.vaccinationHistory = "Annual feline booster completed last spring.";
            pet5Entity.medicationRecords = "Hairball paste twice weekly.";
            pet5Entity.vetVisitNotes = "Coat and skin condition reviewed.";
            pet5Entity.nextVaccinationDate = "2027-03-10";
            pet5Entity.treatmentNotes = "Brush daily and keep bedding dust-free.";

            db.petDao().upsert(pet1Entity);
            db.petDao().upsert(pet2Entity);
            db.petDao().upsert(pet3Entity);
            db.petDao().upsert(pet4Entity);
            db.petDao().upsert(pet5Entity);
            ReminderScheduler.schedulePetReminders(this, pet1);
            ReminderScheduler.schedulePetReminders(this, pet2);
            ReminderScheduler.schedulePetReminders(this, pet3);
            ReminderScheduler.schedulePetReminders(this, pet4);
            ReminderScheduler.schedulePetReminders(this, pet5);

            // 3. Create exactly 5 saved places for the geotagging demo.
            List<PlaceEntity> places = new ArrayList<>();
            places.add(newPlace(uid, "Kathmandu Veterinary Clinic", "Veterinary Clinic", "Lazimpat, Kathmandu",
                    "01-4441234", "Sun-Fri 9 AM-6 PM", "Preferred vet for Max.", 27.7172, 85.3240));
            places.add(newPlace(uid, "Central Pet Grooming Salon", "Grooming Salon", "Thamel, Kathmandu",
                    "01-4432211", "Daily 10 AM-7 PM", "Good for cats and long-haired pets.", 27.7000, 85.3000));
            places.add(newPlace(uid, "Greenway Dog Park", "Dog Park", "Baluwatar, Kathmandu",
                    "", "Open 6 AM-8 PM", "Bella's weekend exercise place.", 27.7090, 85.3150));
            places.add(newPlace(uid, "Pet Supply Corner", "Pet Supply Store", "Maharajgunj, Kathmandu",
                    "01-4429088", "Daily 9 AM-8 PM", "Food, litter, toys and medication supplies.", 27.7160, 85.3050));
            places.add(newPlace(uid, "Happy Paws Animal Shelter", "Animal Shelter", "Patan, Lalitpur",
                    "01-5523456", "Mon-Sat 10 AM-5 PM", "Emergency rescue contact.", 27.6800, 85.3188));
            for (PlaceEntity place : places) {
                db.placeDao().insert(place);
            }

            // 4. Create exactly 5 sample expenses.
            long now = System.currentTimeMillis();
            long day = TimeUnit.DAYS.toMillis(1);
            List<ExpenseEntity> expenses = new ArrayList<>();
            expenses.add(newExpense(uid, pet1.getId(), "Max", "Food", "20kg bag of dry kibble", 42.50, now - 2 * day));
            expenses.add(newExpense(uid, pet2.getId(), "Luna", "Food", "Wet food multipack (24 cans)", 28.75, now - 3 * day));
            expenses.add(newExpense(uid, pet3.getId(), "Bella", "Grooming", "Bath, brush and nail trim", 35.00, now - 10 * day));
            expenses.add(newExpense(uid, pet4.getId(), "Charlie", "Veterinary", "Dental check booking", 60.00, now - 18 * day));
            expenses.add(newExpense(uid, pet5.getId(), "Coco", "Medication", "Hairball paste and flea treatment", 22.00, now - 20 * day));

            for (ExpenseEntity expense : expenses) {
                db.expenseDao().insert(expense);
            }

            mainHandler.post(() -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Demo data generated: 5 pets, 10 routines, 5 places, 5 expenses.", Toast.LENGTH_LONG).show();
            });
        });
    }

    private ExpenseEntity newExpense(String userId, String petId, String petName, String category,
                                      String description, double amount, long dateMillis) {
        ExpenseEntity e = new ExpenseEntity();
        e.id = UUID.randomUUID().toString();
        e.userId = userId;
        e.petId = petId;
        e.petName = petName;
        e.category = category;
        e.description = description;
        e.amount = amount;
        e.dateMillis = dateMillis;
        return e;
    }

    private PlaceEntity newPlace(String userId, String name, String type, String address,
                                 String phone, String openingHours, String notes,
                                 double latitude, double longitude) {
        PlaceEntity place = new PlaceEntity();
        place.id = UUID.randomUUID().toString();
        place.userId = userId;
        place.name = name;
        place.type = type;
        place.address = address;
        place.phone = phone;
        place.openingHours = openingHours;
        place.notes = notes;
        place.latitude = latitude;
        place.longitude = longitude;
        place.createdAtMillis = System.currentTimeMillis();
        return place;
    }

    private PetEntity toEntity(Pet p) {
        PetEntity e = new PetEntity();
        e.id = p.getId();
        e.userId = p.getUserId();
        e.name = p.getName();
        e.breed = p.getBreed();
        e.age = p.getAge();
        e.weight = p.getWeight();
        e.dietaryPreferences = p.getDietaryPreferences();
        e.allergies = p.getAllergies();
        e.favouriteToys = p.getFavouriteToys();
        e.notes = p.getNotes();
        e.imageUrl = p.getImageUrl();
        e.careTasksJson = CareTaskJson.toJson(p.getCareTasks());
        e.latitude = p.getLatitude();
        e.longitude = p.getLongitude();
        e.locationName = p.getLocationName();
        e.locationType = p.getLocationType();
        return e;
    }
}
