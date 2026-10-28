package com.swornima.petcare;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.squareup.picasso.Picasso;
import com.swornima.petcare.db.AppDatabase;
import com.swornima.petcare.db.ExpenseEntity;
import com.swornima.petcare.db.PetEntity;
import com.swornima.petcare.db.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExpenseDetailActivity extends AppCompatActivity {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private TextView tvDescription, tvAmount, tvMeta, tvNotes;
    private ImageView ivPetImage;
    private ProgressBar progressBar;
    private AppDatabase db;
    private String userId;
    private String expenseId;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        db = AppDatabase.getInstance(this);
        userId = new SessionManager(this).getCurrentUserId();
        expenseId = getIntent().getStringExtra("expense_id");

        tvDescription = findViewById(R.id.tvDescription);
        tvAmount = findViewById(R.id.tvAmount);
        tvMeta = findViewById(R.id.tvMeta);
        tvNotes = findViewById(R.id.tvNotes);
        ivPetImage = findViewById(R.id.ivPetImage);
        progressBar = findViewById(R.id.progressBar);

        loadExpense();
    }

    private void loadExpense() {
        progressBar.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            ExpenseEntity expense = db.expenseDao().getById(expenseId, userId);
            PetEntity pet = expense == null || expense.petId == null ? null : db.petDao().getById(expense.petId, userId);
            mainHandler.post(() -> {
                progressBar.setVisibility(View.GONE);
                if (expense == null) {
                    Toast.makeText(this, "Expense not found.", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                tvDescription.setText(expense.description);
                tvAmount.setText(String.format(Locale.getDefault(), "$%.2f", expense.amount));
                tvMeta.setText("Pet: " + valueOrDefault(expense.petName, "Unknown") +
                        "\nCategory: " + valueOrDefault(expense.category, "Other") +
                        "\nDate: " + dateFormat.format(expense.dateMillis));
                tvNotes.setText(valueOrDefault(expense.notes, "No notes added."));
                ivPetImage.setImageResource(R.drawable.logo_icon);
                if (pet != null && pet.imageUrl != null && !pet.imageUrl.trim().isEmpty()) {
                    Picasso.get()
                            .load(pet.imageUrl)
                            .placeholder(R.drawable.logo_icon)
                            .error(R.drawable.logo_icon)
                            .into(ivPetImage);
                }
            });
        });
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
