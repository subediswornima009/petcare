package com.swornima.petcare;

import java.util.List;

/**
 * Pet represents a single pet profile (e.g. Emily's Golden Retriever "Max").
 * It replaces the old "Workout" model from the fitness app. A Pet stores
 * biographical details plus a list of CareTask items that together make up
 * its personalised care routine/checklist, and an optional geotagged
 * location (e.g. its preferred vet clinic) used for the Geotagging feature.
 */
public class Pet {
    private String id;
    private String userId;
    private String name;
    private String breed;
    private String age;
    private String weight;
    private String dietaryPreferences;
    private String allergies;
    private String favouriteToys;
    private String notes;
    private String imageUrl;
    private List<CareTask> careTasks;

    public Pet() {
        // Required for Firestore
    }

    public Pet(String id, String userId, String name, String breed, String age, String weight,
                String dietaryPreferences, String allergies, String favouriteToys, String notes,
                String imageUrl, List<CareTask> careTasks) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.breed = breed;
        this.age = age;
        this.weight = weight;
        this.dietaryPreferences = dietaryPreferences;
        this.allergies = allergies;
        this.favouriteToys = favouriteToys;
        this.notes = notes;
        this.imageUrl = imageUrl;
        this.careTasks = careTasks;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }

    public String getAge() { return age; }
    public void setAge(String age) { this.age = age; }

    public String getWeight() { return weight; }
    public void setWeight(String weight) { this.weight = weight; }

    public String getDietaryPreferences() { return dietaryPreferences; }
    public void setDietaryPreferences(String dietaryPreferences) { this.dietaryPreferences = dietaryPreferences; }

    public String getAllergies() { return allergies; }
    public void setAllergies(String allergies) { this.allergies = allergies; }

    public String getFavouriteToys() { return favouriteToys; }
    public void setFavouriteToys(String favouriteToys) { this.favouriteToys = favouriteToys; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public List<CareTask> getCareTasks() { return careTasks; }
    public void setCareTasks(List<CareTask> careTasks) { this.careTasks = careTasks; }

    // Geotagging fields - linked location e.g. preferred vet clinic, groomer, dog park
    private double latitude;
    private double longitude;
    private String locationName;
    private String locationType; // Vet Clinic, Grooming Salon, Dog Park, Pet Supply Store, Animal Shelter

    public Pet(String id, String userId, String name, String breed, String age, String weight,
                String dietaryPreferences, String allergies, String favouriteToys, String notes,
                String imageUrl, List<CareTask> careTasks, double latitude, double longitude,
                String locationName, String locationType) {
        this(id, userId, name, breed, age, weight, dietaryPreferences, allergies, favouriteToys,
                notes, imageUrl, careTasks);
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationName = locationName;
        this.locationType = locationType;
    }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    public String getLocationType() { return locationType; }
    public void setLocationType(String locationType) { this.locationType = locationType; }
}
