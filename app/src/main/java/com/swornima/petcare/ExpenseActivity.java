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
import com.swornima.petcare.db.CareTaskJson;
import com.swornima.petcare.db.ExpenseEntity;
import com.swornima.petcare.db.PetEntity;
import com.swornima.petcare.db.SessionManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Tracks pet-related expenses (food, grooming, veterinary visits, toys,
 * medications) as described in the PetCare scenario, so owners can monitor
 * spending per pet. This complements the core requirements and is not part
 * of the graded desirable feature (Geotagging is), but supports the wider
 * "manage pet care" experience described in the brief.
 */
public class ExpenseActivity extends AppCompatActivity {

    private static final String[] CATEGORIES = {"Food", "Grooming", "Veterinary", "Toys", "Medication", "Other"};

    private RecyclerView recyclerView;
    private ExpenseAdapter adapter;
    private List<Expense> expenseList;
    private List<Pet> petList;
    private FloatingActionButton fabAdd;
    private ProgressBar progressBar;
    private TextView tvEmpty, tvTotalSpent, tvMonthSpent, tvPerPetSummary;
    private ExpenseBarChartView expenseBarChart;
    private AppDatabase db;
    private String userId;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense);

        db = AppDatabase.getInstance(this);
        userId = new SessionManager(this).getCurrentUserId();

        BottomNavHelper.setup(this, BottomNavHelper.Tab.EXPENSES);

        recyclerView = findViewById(R.id.recyclerView);
        fabAdd = findViewById(R.id.fabAdd);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvTotalSpent = findViewById(R.id.tvTotalSpent);
        tvMonthSpent = findViewById(R.id.tvMonthSpent);
        tvPerPetSummary = findViewById(R.id.tvPerPetSummary);
        expenseBarChart = findViewById(R.id.expenseBarChart);

        expenseList = new ArrayList<>();
        petList = new ArrayList<>();

        adapter = new ExpenseAdapter(this, expenseList, new ExpenseAdapter.OnItemActionListener() {
            @Override
            public void onItemClick(Expense expense) {
                Intent intent = new Intent(ExpenseActivity.this, ExpenseDetailActivity.class);
                intent.putExtra("expense_id", expense.getId());
                startActivity(intent);
            }

            @Override
            public void onEditClick(Expense expense) {
                showExpenseDialog(expense);
            }

            @Override
            public void onDeleteClick(Expense expense) {
                deleteExpense(expense);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> {
            if (petList.isEmpty()) {
                Toast.makeText(this, "Add a pet first before logging expenses.", Toast.LENGTH_SHORT).show();
            } else {
                showExpenseDialog(null);
            }
        });

        fetchPets();
        fetchExpenses();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchPets();
    }

    private void fetchPets() {
        executor.execute(() -> {
            List<PetEntity> entities = db.petDao().getAllForUser(userId);
            List<Pet> fetched = new ArrayList<>();
            for (PetEntity e : entities) {
                Pet p = new Pet(e.id, e.userId, e.name, e.breed, e.age, e.weight, e.dietaryPreferences,
                        e.allergies, e.favouriteToys, e.notes, e.imageUrl, CareTaskJson.fromJson(e.careTasksJson),
                        e.latitude, e.longitude, e.locationName, e.locationType);
                fetched.add(p);
            }
            mainHandler.post(() -> {
                petList.clear();
                petList.addAll(fetched);
            });
        });
    }

    private void fetchExpenses() {
        progressBar.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            List<ExpenseEntity> entities = db.expenseDao().getAllForUser(userId);
            List<PetEntity> pets = db.petDao().getAllForUser(userId);
            Map<String, String> petImagesById = new LinkedHashMap<>();
            for (PetEntity pet : pets) {
                petImagesById.put(pet.id, pet.imageUrl);
            }
            List<Expense> fetched = new ArrayList<>();
            double total = 0;
            double monthTotal = 0;
            Map<String, Double> petTotals = new LinkedHashMap<>();
            Map<String, Double> monthlyTotals = buildEmptyMonthlyTotals();
            Calendar monthStart = Calendar.getInstance();
            monthStart.set(Calendar.DAY_OF_MONTH, 1);
            monthStart.set(Calendar.HOUR_OF_DAY, 0);
            monthStart.set(Calendar.MINUTE, 0);
            monthStart.set(Calendar.SECOND, 0);
            monthStart.set(Calendar.MILLISECOND, 0);
            for (ExpenseEntity e : entities) {
                Expense expense = new Expense(e.id, e.userId, e.petId, e.petName, petImagesById.get(e.petId), e.category,
                        e.description, e.notes, e.amount, e.dateMillis);
                fetched.add(expense);
                total += expense.getAmount();
                if (expense.getDateMillis() >= monthStart.getTimeInMillis()) {
                    monthTotal += expense.getAmount();
                }
                String petName = expense.getPetName() == null || expense.getPetName().trim().isEmpty()
                        ? "Unknown pet" : expense.getPetName().trim();
                Double current = petTotals.get(petName);
                petTotals.put(petName, (current == null ? 0 : current) + expense.getAmount());
                String monthKey = monthKey(expense.getDateMillis());
                if (monthlyTotals.containsKey(monthKey)) {
                    monthlyTotals.put(monthKey, monthlyTotals.get(monthKey) + expense.getAmount());
                }
            }
            double finalTotal = total;
            double finalMonthTotal = monthTotal;
            String petSummary = buildPerPetSummary(petTotals);
            List<ExpenseBarChartView.Bar> chartBars = buildChartBars(monthlyTotals);

            mainHandler.post(() -> {
                progressBar.setVisibility(View.GONE);
                expenseList.clear();
                expenseList.addAll(fetched);
                adapter.notifyDataSetChanged();
                tvTotalSpent.setText(String.format(Locale.getDefault(), "$%.2f", finalTotal));
                tvMonthSpent.setText(String.format(Locale.getDefault(), "This month\n$%.2f", finalMonthTotal));
                tvPerPetSummary.setText(petSummary);
                expenseBarChart.setBars(chartBars);
                toggleEmptyView();
            });
        });
    }

    private Map<String, Double> buildEmptyMonthlyTotals() {
        Map<String, Double> monthlyTotals = new LinkedHashMap<>();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        for (int i = 5; i >= 0; i--) {
            Calendar month = (Calendar) cal.clone();
            month.add(Calendar.MONTH, -i);
            monthlyTotals.put(monthKey(month.getTimeInMillis()), 0.0);
        }
        return monthlyTotals;
    }

    private List<ExpenseBarChartView.Bar> buildChartBars(Map<String, Double> monthlyTotals) {
        List<ExpenseBarChartView.Bar> bars = new ArrayList<>();
        for (Map.Entry<String, Double> entry : monthlyTotals.entrySet()) {
            bars.add(new ExpenseBarChartView.Bar(entry.getKey(), entry.getValue()));
        }
        return bars;
    }

    private String monthKey(long dateMillis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(dateMillis);
        return cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault());
    }

    private String buildPerPetSummary(Map<String, Double> petTotals) {
        if (petTotals.isEmpty()) {
            return "Per pet\n$0.00";
        }

        String topPet = null;
        double topTotal = 0;
        for (Map.Entry<String, Double> entry : petTotals.entrySet()) {
            if (topPet == null || entry.getValue() > topTotal) {
                topPet = entry.getKey();
                topTotal = entry.getValue();
            }
        }
        return String.format(Locale.getDefault(), "%s\n$%.2f", topPet, topTotal);
    }

    private void toggleEmptyView() {
        if (expenseList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showExpenseDialog(Expense editingExpense) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_expense, null);
        builder.setView(dialogView);

        AutoCompleteTextView etPet = dialogView.findViewById(R.id.etExpensePet);
        TextInputLayout tilPet = dialogView.findViewById(R.id.tilExpensePet);
        AutoCompleteTextView etCategory = dialogView.findViewById(R.id.etExpenseCategory);
        TextInputLayout tilCategory = dialogView.findViewById(R.id.tilExpenseCategory);
        EditText etDescription = dialogView.findViewById(R.id.etExpenseDescription);
        EditText etAmount = dialogView.findViewById(R.id.etExpenseAmount);
        EditText etNotes = dialogView.findViewById(R.id.etExpenseNotes);
        TextInputLayout tilAmount = dialogView.findViewById(R.id.tilExpenseAmount);

        List<String> petNames = new ArrayList<>();
        for (Pet p : petList) petNames.add(p.getName());
        etPet.setAdapter(new ArrayAdapter<>(this, R.layout.item_dropdown, petNames));
        etCategory.setAdapter(new ArrayAdapter<>(this, R.layout.item_dropdown, CATEGORIES));

        if (editingExpense != null) {
            etPet.setText(editingExpense.getPetName(), false);
            etCategory.setText(editingExpense.getCategory(), false);
            etDescription.setText(editingExpense.getDescription());
            etAmount.setText(String.format(Locale.US, "%.2f", editingExpense.getAmount()));
            etNotes.setText(editingExpense.getNotes());
        }

        builder.setPositiveButton(editingExpense == null ? "Add" : "Update", (dialog, which) -> {});
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String petName = etPet.getText().toString().trim();
            String category = etCategory.getText().toString().trim();
            String description = etDescription.getText().toString().trim();
            String amountStr = etAmount.getText().toString().trim();
            String notes = etNotes.getText().toString().trim();

            tilPet.setError(null);
            tilCategory.setError(null);
            tilAmount.setError(null);

            if (TextUtils.isEmpty(petName)) {
                tilPet.setError("Select a pet");
                Toast.makeText(this, "Please select a pet for this expense.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(category)) {
                tilCategory.setError("Select a category");
                Toast.makeText(this, "Please select an expense category.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(amountStr)) {
                tilAmount.setError("Enter an amount");
                Toast.makeText(this, "Please enter an expense amount.", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
            } catch (NumberFormatException e) {
                tilAmount.setError("Invalid amount");
                Toast.makeText(this, "Please enter a valid amount.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (amount <= 0) {
                tilAmount.setError("Amount must be greater than 0");
                Toast.makeText(this, "Expense amount must be greater than 0.", Toast.LENGTH_SHORT).show();
                return;
            }

            String petId = null;
            for (Pet p : petList) {
                if (p.getName().equals(petName)) {
                    petId = p.getId();
                    break;
                }
            }

            if (TextUtils.isEmpty(description)) {
                description = category;
            }

            if (editingExpense == null) {
                saveExpense(petId, petName, category, description, notes, amount);
            } else {
                updateExpense(editingExpense.getId(), petId, petName, category, description, notes, amount);
            }
            dialog.dismiss();
        });
    }

    private void saveExpense(String petId, String petName, String category, String description, String notes, double amount) {
        progressBar.setVisibility(View.VISIBLE);
        ExpenseEntity entity = new ExpenseEntity();
        entity.id = UUID.randomUUID().toString();
        entity.userId = userId;
        entity.petId = petId;
        entity.petName = petName;
        entity.category = category;
        entity.description = description;
        entity.notes = notes;
        entity.amount = amount;
        entity.dateMillis = System.currentTimeMillis();

        executor.execute(() -> {
            db.expenseDao().insert(entity);
            mainHandler.post(() -> {
                Toast.makeText(ExpenseActivity.this, "Expense recorded!", Toast.LENGTH_SHORT).show();
                fetchExpenses();
            });
        });
    }

    private void updateExpense(String expenseId, String petId, String petName, String category,
                               String description, String notes, double amount) {
        progressBar.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            db.expenseDao().updateById(expenseId, userId, petId, petName, category, description, notes, amount);
            mainHandler.post(() -> {
                Toast.makeText(ExpenseActivity.this, "Expense updated!", Toast.LENGTH_SHORT).show();
                fetchExpenses();
            });
        });
    }

    private void deleteExpense(Expense expense) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Expense")
                .setMessage("Are you sure you want to delete this expense record?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    executor.execute(() -> {
                        db.expenseDao().deleteById(expense.getId(), userId);
                        mainHandler.post(() -> {
                            Toast.makeText(this, "Expense deleted successfully.", Toast.LENGTH_SHORT).show();
                            fetchExpenses();
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
