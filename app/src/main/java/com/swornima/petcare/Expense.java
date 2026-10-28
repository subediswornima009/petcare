package com.swornima.petcare;

/**
 * Expense represents a single pet-related cost record (food, grooming,
 * veterinary visits, toys, medications, etc.) so that owners like Emily can
 * monitor the cost of caring for each pet individually. Stored under
 * users/{uid}/expenses in Cloud Firestore.
 */
public class Expense {
    private String id;
    private String userId;
    private String petId;
    private String petName;
    private String petImageUrl;
    private String category; // Food, Grooming, Veterinary, Toys, Medication, Other
    private String description;
    private String notes;
    private double amount;
    private long dateMillis;

    public Expense() {
        // Required for Firestore
    }

    public Expense(String id, String userId, String petId, String petName, String category,
                    String description, String notes, double amount, long dateMillis) {
        this(id, userId, petId, petName, null, category, description, notes, amount, dateMillis);
    }

    public Expense(String id, String userId, String petId, String petName, String petImageUrl, String category,
                    String description, String notes, double amount, long dateMillis) {
        this.id = id;
        this.userId = userId;
        this.petId = petId;
        this.petName = petName;
        this.petImageUrl = petImageUrl;
        this.category = category;
        this.description = description;
        this.notes = notes;
        this.amount = amount;
        this.dateMillis = dateMillis;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPetId() { return petId; }
    public void setPetId(String petId) { this.petId = petId; }

    public String getPetName() { return petName; }
    public void setPetName(String petName) { this.petName = petName; }

    public String getPetImageUrl() { return petImageUrl; }
    public void setPetImageUrl(String petImageUrl) { this.petImageUrl = petImageUrl; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public long getDateMillis() { return dateMillis; }
    public void setDateMillis(long dateMillis) { this.dateMillis = dateMillis; }
}
