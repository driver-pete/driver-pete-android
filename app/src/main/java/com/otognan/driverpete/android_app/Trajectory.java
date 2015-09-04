package com.otognan.driverpete.android_app;


import android.location.Location;

import java.io.BufferedReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class Trajectory {

    private ArrayList<Location> locations = new ArrayList<Location>();

    public void addLocation(Location location) {
        this.locations.add(location);
    }

    Location lastLocation() {
        return this.locations.get(this.locations.size()-1);
    }

    public static String locationToString(Location location) {
        return String.format(Locale.US, "%s %f %f",
                new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss_z", Locale.US).format(location.getTime()),
                location.getLatitude(),
                location.getLongitude());
    }

    public static Location locationFromString(String locationStr) throws ParseException {
        String[] parts = locationStr.split(" ");

        Location result = new Location((String)null);

        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss_z", Locale.US);
        Date date = formatter.parse(parts[0]);
        result.setTime(date.getTime());
        result.setLatitude(Double.parseDouble(parts[1]));
        result.setLongitude(Double.parseDouble(parts[2]));
        return result;
    }

    public static String locationToShortString(Location location) {
        return String.format(Locale.US, "%s %f %f",
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
        String content = this.dumpToString();
        return Compress.compress(content);
    }

    public static Trajectory decompress(byte[] trajectoryBytes) throws Exception {
        BufferedReader bf = Compress.decompress(trajectoryBytes);

        Trajectory result = new Trajectory();
        String line;
        while ((line=bf.readLine())!=null) {
            result.addLocation(Trajectory.locationFromString(line));
        }

        return result;
    }


    public void clear() {
        this.locations.clear();
    }

    public int size() { return this.locations.size(); }

    public List<Location> getLocations() {
        return this.locations;
    }

}
