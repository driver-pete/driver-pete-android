package com.otognan.driverpete.android;


import android.location.Location;

import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class Trajectory {

    private LinkedList<Location> locations = new LinkedList<Location>();

    public void addLocation(Location location) {
        this.locations.add(location);
    }

    Location lastLocation() {
        return this.locations.getLast();
    }

    public static String locationToString(Location location) {
        return String.format("%s %f %f",
                new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss", Locale.US).format(location.getTime()),
                location.getLatitude(),
                location.getLongitude());
    }

    public static String locationToShortString(Location location) {
        return String.format("%s %f %f",
                new SimpleDateFormat("HH-mm-ss", Locale.US).format(location.getTime()),
                location.getLatitude(),
                location.getLongitude());
    }

    public String dumpToString() {
        String stringData = "";
        for (Location l : this.locations)
        {
            stringData += Trajectory.locationToString(l) + "\n";
        }
        return stringData;
    }

    public byte[] compress() throws Exception {
        String string = this.dumpToString();
        return Compress.compress(string);
    }

    public void clear() {
        this.locations.clear();
    }

}
