package com.swornima.petcare;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.swornima.petcare.db.AppDatabase;
import com.swornima.petcare.db.CareTaskJson;
import com.swornima.petcare.db.PetEntity;
import com.swornima.petcare.db.SessionManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Shows the care message before choosing a contact and sending it by SMS. */
public class DelegationPreviewActivity extends AppCompatActivity {

    private static final int REQUEST_READ_CONTACTS = 200;
    private static final int REQUEST_SEND_SMS = 201;
    private static final int REQUEST_PICK_CONTACT = 202;

    private TextView tvPetName;
    private TextView tvSelectedContact;
    private TextView tvMessagePreview;
    private Button btnSelectContact;
    private Button btnSendMessage;
    private ProgressBar loadingIndicator;

    private String petId;
    private String phoneNumber;
    private String contactName;
    private String message;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delegation_preview);

        Toolbar toolbar = findViewById(R.id.toolbarDelegation);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Delegate Care");
        }

        tvPetName = findViewById(R.id.tvDelegationPetName);
        tvSelectedContact = findViewById(R.id.tvSelectedContact);
        tvMessagePreview = findViewById(R.id.tvMessagePreview);
        btnSelectContact = findViewById(R.id.btnSelectContact);
        btnSendMessage = findViewById(R.id.btnSendMessage);
        loadingIndicator = findViewById(R.id.delegationLoadingIndicator);

        petId = getIntent().getStringExtra("pet_id");
        if (petId == null) {
            Toast.makeText(this, "Error loading pet", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnSelectContact.setOnClickListener(v -> selectContact());
        btnSendMessage.setOnClickListener(v -> sendMessage());
        loadPetMessage();
    }

    private void loadPetMessage() {
        loadingIndicator.setVisibility(View.VISIBLE);
        btnSelectContact.setEnabled(false);
        executor.execute(() -> {
            String userId = new SessionManager(this).getCurrentUserId();
            PetEntity entity = AppDatabase.getInstance(this).petDao().getById(petId, userId);
            if (entity == null) {
                mainHandler.post(() -> {
                    Toast.makeText(this, "Pet not found", Toast.LENGTH_SHORT).show();
                    finish();
                });
                return;
            }

            Pet pet = new Pet(entity.id, entity.userId, entity.name, entity.breed, entity.age,
                    entity.weight, entity.dietaryPreferences, entity.allergies, entity.favouriteToys,
                    entity.notes, entity.imageUrl, CareTaskJson.fromJson(entity.careTasksJson),
                    entity.latitude, entity.longitude, entity.locationName, entity.locationType);
            message = buildDelegationMessage(pet);
            mainHandler.post(() -> {
                tvPetName.setText("Choose a contact to receive " + valueOrDefault(pet.getName(), "this pet") + "'s care checklist");
                tvMessagePreview.setText(message);
                loadingIndicator.setVisibility(View.GONE);
                btnSelectContact.setEnabled(true);
            });
        });
    }

    private void selectContact() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS}, REQUEST_READ_CONTACTS);
            return;
        }
        launchContactPicker();
    }

    private void launchContactPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult(intent, REQUEST_PICK_CONTACT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_PICK_CONTACT || resultCode != RESULT_OK || data == null) return;

        Uri contactUri = data.getData();
        String[] projection = {
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };
        try (Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                contactName = cursor.getString(cursor.getColumnIndexOrThrow(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(
                        ContactsContract.CommonDataKinds.Phone.NUMBER));
                tvSelectedContact.setText("Selected contact: " + contactName + "\n" + phoneNumber);
                btnSendMessage.setEnabled(true);
            } else {
                Toast.makeText(this, "Could not read contact number", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Could not read contact", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            String message = requestCode == REQUEST_SEND_SMS
                    ? "SMS permission is required to send the care checklist"
                    : "Contacts permission is required to choose a recipient";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            return;
        }

        if (requestCode == REQUEST_READ_CONTACTS) {
            launchContactPicker();
        } else if (requestCode == REQUEST_SEND_SMS) {
            sendSms();
        }
    }

    private void sendMessage() {
        if (phoneNumber == null || message == null) {
            Toast.makeText(this, "Select a contact first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS}, REQUEST_SEND_SMS);
        } else {
            sendSms();
        }
    }

    private void sendSms() {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<String> parts = new ArrayList<>(smsManager.divideMessage(message));
            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null);
            Toast.makeText(this, "Care checklist sent to " + contactName, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to send SMS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String buildDelegationMessage(Pet pet) {
        StringBuilder body = new StringBuilder();
        body.append("PetCare Care Checklist\n");
        body.append("Pet: ").append(valueOrDefault(pet.getName(), "Unnamed pet")).append("\n");
        if (hasText(pet.getBreed())) body.append("Breed: ").append(pet.getBreed().trim()).append("\n");
        if (hasText(pet.getDietaryPreferences())) body.append("Diet: ").append(pet.getDietaryPreferences().trim()).append("\n");
        if (hasText(pet.getAllergies())) body.append("Allergies: ").append(pet.getAllergies().trim()).append("\n");
        if (hasText(pet.getLocationName())) body.append("Preferred location: ").append(pet.getLocationName().trim()).append("\n");

        Map<String, List<CareTask>> tasksByCategory = groupCareTasksByCategory(pet.getCareTasks());
        if (tasksByCategory.isEmpty()) {
            body.append("\nNo care tasks assigned.\n");
        } else {
            body.append("\nCare Checklist:\n");
            for (Map.Entry<String, List<CareTask>> entry : tasksByCategory.entrySet()) {
                body.append("\n").append(entry.getKey().toUpperCase(Locale.US)).append("\n");
                for (CareTask task : entry.getValue()) appendDelegationTask(body, task);
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
                if (!grouped.containsKey(category)) grouped.put(category, new ArrayList<>());
                grouped.get(category).add(task);
            }
        }
        Map<String, List<CareTask>> nonEmpty = new LinkedHashMap<>();
        for (Map.Entry<String, List<CareTask>> entry : grouped.entrySet()) {
            if (!entry.getValue().isEmpty()) nonEmpty.put(entry.getKey(), entry.getValue());
        }
        return nonEmpty;
    }

    private void appendDelegationTask(StringBuilder body, CareTask task) {
        body.append("- ").append(valueOrDefault(task.getName(), "Care task"));
        List<String> scheduleParts = new ArrayList<>();
        if (hasText(task.getTime())) scheduleParts.add(task.getTime().trim());
        if (hasText(task.getFrequency())) scheduleParts.add(task.getFrequency().trim());
        if (!scheduleParts.isEmpty()) body.append(" | Schedule: ").append(joinParts(scheduleParts));
        List<String> dateParts = new ArrayList<>();
        addPartIfPresent(dateParts, "Vet", task.getAppointmentDate());
        addPartIfPresent(dateParts, "Grooming", task.getGroomingDate());
        addPartIfPresent(dateParts, "Vaccination", task.getVaccinationDueDate());
        if (hasText(task.getMedicationStartDate()) || hasText(task.getMedicationEndDate())) {
            dateParts.add("Medication " + safeDate(task.getMedicationStartDate()) + " to " + safeDate(task.getMedicationEndDate()));
        }
        if (!dateParts.isEmpty()) body.append(" | Dates: ").append(joinParts(dateParts));
        if (hasText(task.getSupplies())) body.append(" | Supplies: ").append(task.getSupplies().trim());
        if (hasText(task.getNotes())) body.append(" | Notes: ").append(task.getNotes().trim());
        body.append("\n");
    }

    private void addPartIfPresent(List<String> parts, String label, String value) {
        if (hasText(value)) parts.add(label + " " + value.trim());
    }

    private String joinParts(List<String> parts) {
        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) joined.append(", ");
            joined.append(parts.get(i));
        }
        return joined.toString();
    }

    private boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }

    private String valueOrDefault(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private String safeDate(String value) { return hasText(value) ? value.trim() : "not set"; }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
