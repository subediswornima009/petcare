package com.swornima.petcare;

/**
 * A saved location (vet clinic, grooming salon, dog park, pet supply store,
 * or animal shelter) the owner has searched for and bookmarked, independent
 * of any single pet's profile.
 */
public class Place {
    private String id;
    private String userId;
    private String name;
    private String type;
    private String address;
    private String phone;
    private String openingHours;
    private String notes;
    private double latitude;
    private double longitude;
    private long createdAtMillis;

    public Place(String id, String userId, String name, String type, String address,
                 String phone, String openingHours, String notes,
                 double latitude, double longitude, long createdAtMillis) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.type = type;
        this.address = address;
        this.phone = phone;
        this.openingHours = openingHours;
        this.notes = notes;
        this.latitude = latitude;
        this.longitude = longitude;
        this.createdAtMillis = createdAtMillis;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getOpeningHours() { return openingHours; }
    public void setOpeningHours(String openingHours) { this.openingHours = openingHours; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public long getCreatedAtMillis() { return createdAtMillis; }
    public void setCreatedAtMillis(long createdAtMillis) { this.createdAtMillis = createdAtMillis; }
}
