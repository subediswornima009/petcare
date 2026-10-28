package com.swornima.petcare;

/**
 * CareTask represents a single pet care activity, e.g. feeding, walking,
 * grooming, medication or a vet visit. It replaces the old "Exercise" model
 * from the fitness app and is re-purposed to hold scheduling information
 * (time + frequency) plus supplies needed and free-text notes.
 *
 * Instances are stored as sub-documents inside a Pet's "careTasks" list in
 * Cloud Firestore (users/{uid}/pets/{petId}).
 */
public class CareTask {
    private String id;
    private String name;            // e.g. "Morning Feeding", "Evening Walk"
    private String category;        // Feeding, Exercise, Grooming, Medication, Healthcare
    private String supplies;        // e.g. "Dog food, water bowl"
    private String notes;           // instructions / notes
    private String userId;
    private String time;            // scheduled time, e.g. "08:00 AM"
    private String frequency;       // Daily, Weekly, Monthly, One-off
    private String appointmentDate; // vet or general appointment date
    private String groomingDate;
    private String vaccinationDueDate;
    private String medicationStartDate;
    private String medicationEndDate;
    private boolean completed;      // marks the task as "done" for today
    private long completedAtMillis; // when the current completion period was checked off

    public CareTask() {
        // Required for Firestore
    }

    public CareTask(String id, String name, String category, String notes, String userId,
                     String time, String frequency, String supplies) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.notes = notes;
        this.userId = userId;
        this.time = time;
        this.frequency = frequency;
        this.supplies = supplies;
        this.completed = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSupplies() { return supplies; }
    public void setSupplies(String supplies) { this.supplies = supplies; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public String getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(String appointmentDate) { this.appointmentDate = appointmentDate; }

    public String getGroomingDate() { return groomingDate; }
    public void setGroomingDate(String groomingDate) { this.groomingDate = groomingDate; }

    public String getVaccinationDueDate() { return vaccinationDueDate; }
    public void setVaccinationDueDate(String vaccinationDueDate) { this.vaccinationDueDate = vaccinationDueDate; }

    public String getMedicationStartDate() { return medicationStartDate; }
    public void setMedicationStartDate(String medicationStartDate) { this.medicationStartDate = medicationStartDate; }

    public String getMedicationEndDate() { return medicationEndDate; }
    public void setMedicationEndDate(String medicationEndDate) { this.medicationEndDate = medicationEndDate; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public long getCompletedAtMillis() { return completedAtMillis; }
    public void setCompletedAtMillis(long completedAtMillis) { this.completedAtMillis = completedAtMillis; }
}
