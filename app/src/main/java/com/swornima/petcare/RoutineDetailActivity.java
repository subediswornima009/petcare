package com.swornima.petcare;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.swornima.petcare.db.AppDatabase;
import com.swornima.petcare.db.CareTaskEntity;
import com.swornima.petcare.db.SessionManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RoutineDetailActivity extends AppCompatActivity {

    private TextView tvRoutineName, tvCategory, tvSchedule, tvDates, tvSupplies, tvNotes;
    private ProgressBar progressBar;
    private AppDatabase db;
    private String userId;
    private String taskId;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routine_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        db = AppDatabase.getInstance(this);
        userId = new SessionManager(this).getCurrentUserId();
        taskId = getIntent().getStringExtra("task_id");

        tvRoutineName = findViewById(R.id.tvRoutineName);
        tvCategory = findViewById(R.id.tvCategory);
        tvSchedule = findViewById(R.id.tvSchedule);
        tvDates = findViewById(R.id.tvDates);
        tvSupplies = findViewById(R.id.tvSupplies);
        tvNotes = findViewById(R.id.tvNotes);
        progressBar = findViewById(R.id.progressBar);

        loadRoutine();
    }

    private void loadRoutine() {
        progressBar.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            CareTaskEntity entity = db.careTaskDao().getById(taskId, userId);
            mainHandler.post(() -> {
                progressBar.setVisibility(View.GONE);
                if (entity == null) {
                    Toast.makeText(this, "Routine not found.", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                bindRoutine(entity);
            });
        });
    }

    private void bindRoutine(CareTaskEntity task) {
        tvRoutineName.setText(task.name);
        tvCategory.setText(valueOrDefault(task.category, "Uncategorised"));
        tvSchedule.setText("Time\n" + valueOrDefault(task.time, "Not set") +
                "\n\nFrequency\n" + valueOrDefault(task.frequency, "Not set"));
        tvDates.setText("Vet appointment\n" + valueOrDefault(task.appointmentDate, "Not set") +
                "\n\nGrooming date\n" + valueOrDefault(task.groomingDate, "Not set") +
                "\n\nVaccination due\n" + valueOrDefault(task.vaccinationDueDate, "Not set") +
                "\n\nMedication period\n" + valueOrDefault(task.medicationStartDate, "Not set") +
                " to " + valueOrDefault(task.medicationEndDate, "Not set"));
        tvSupplies.setText(valueOrDefault(task.supplies, "No supplies listed."));
        tvNotes.setText(valueOrDefault(task.notes, "No notes added."));
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
