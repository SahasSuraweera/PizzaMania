package com.example.pizzamania;

public class BranchDBHelper {
    public String name;
    public String address;
    public double latitude;
    public double longitude;
    public String openingHours;
    public String openingDays;

    // Default constructor required by Firebase
    public BranchDBHelper() {}

    public BranchDBHelper(String name, String address, double latitude, double longitude, String openingHours, String openingDays) {
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.openingHours = openingHours;
        this.openingDays = openingDays;
    }

    public String getName() { return name; }
    public String getAddress() { return address; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getOpeningHours() { return openingHours; }
    public String getOpeningDays() { return openingDays; }
}
