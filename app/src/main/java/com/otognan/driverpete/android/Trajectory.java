package com.otognan.driverpete.android;


import android.location.Location;

import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class Trajectory {

    private List<Location> locations = new LinkedList<Location>();

    public void addLocation(Location location) {
        this.locations.add(location);
    }

    public static String locationToString(Location location) {
        return String.format("%s %f %f",
            new SimpleDateFormat("HH:mm:ss", Locale.US).format(location.getTime()),
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

}
