package com.swornima.petcare;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.swornima.petcare.db.AppDatabase;
import com.swornima.petcare.db.PetEntity;
import com.swornima.petcare.db.SessionManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Dedicated medical record screen for a pet. Stores vaccination history,
 * medication records, vet visit notes, next vaccination date, allergies and
 * treatment notes against the pet profile in the local Room database.
 */
public class MedicalRecordsActivity extends AppCompatActivity {

    private TextInputEditText etPetAllergies, etVaccinationHistory, etMedicationRecords,
            etVetVisitNotes, etNextVaccinationDate, etTreatmentNotes;
    private ProgressBar progressBar;
    private MaterialButton btnSaveMedical;

    private AppDatabase db;
    private String userId;
    private String petId;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medical_records);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Medical Records");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        db = AppDatabase.getInstance(this);
        userId = new SessionManager(this).getCurrentUserId();
        petId = getIntent().getStringExtra("pet_id");

        if (TextUtils.isEmpty(petId) || TextUtils.isEmpty(userId)) {
            Toast.makeText(this, "Unable to load medical records", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        btnSaveMedical.setOnClickListener(v -> saveMedicalRecord());
        loadMedicalRecord();
    }

    private void initializeViews() {
        etPetAllergies = findViewById(R.id.etPetAllergies);
        etVaccinationHistory = findViewById(R.id.etVaccinationHistory);
        etMedicationRecords = findViewById(R.id.etMedicationRecords);
        etVetVisitNotes = findViewById(R.id.etVetVisitNotes);
        etNextVaccinationDate = findViewById(R.id.etNextVaccinationDate);
        etTreatmentNotes = findViewById(R.id.etTreatmentNotes);
        btnSaveMedical = findViewById(R.id.btnSaveMedical);
        progressBar = findViewById(R.id.progressBar);
    }

    private void loadMedicalRecord() {
        progressBar.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            PetEntity pet = db.petDao().getById(petId, userId);
            mainHandler.post(() -> {
                progressBar.setVisibility(View.GONE);
                if (pet == null) {
                    Toast.makeText(this, "Pet not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                etPetAllergies.setText(pet.allergies);
                etVaccinationHistory.setText(pet.vaccinationHistory);
                etMedicationRecords.setText(pet.medicationRecords);
                etVetVisitNotes.setText(pet.vetVisitNotes);
                etNextVaccinationDate.setText(pet.nextVaccinationDate);
                etTreatmentNotes.setText(pet.treatmentNotes);
            });
        });
    }

    private void saveMedicalRecord() {
        progressBar.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            PetEntity pet = db.petDao().getById(petId, userId);
            if (pet != null) {
                pet.allergies = valueOf(etPetAllergies);
                pet.vaccinationHistory = valueOf(etVaccinationHistory);
                pet.medicationRecords = valueOf(etMedicationRecords);
                pet.vetVisitNotes = valueOf(etVetVisitNotes);
                pet.nextVaccinationDate = valueOf(etNextVaccinationDate);
                pet.treatmentNotes = valueOf(etTreatmentNotes);
                db.petDao().upsert(pet);
            }

            mainHandler.post(() -> {
                progressBar.setVisibility(View.GONE);
                if (pet == null) {
                    Toast.makeText(this, "Unable to save medical records", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Medical records saved", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        });
    }

    private String valueOf(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }
}
