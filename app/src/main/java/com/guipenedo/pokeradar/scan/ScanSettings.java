package com.guipenedo.pokeradar.scan;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class ScanSettings {
    final String username, password;
    List<LatLng> locations;
    int delay;

    public ScanSettings(String username, String password, List<LatLng> locations, int delay) {
        this.username = username;
        this.password = password;
        this.locations = locations;
        this.delay = delay;
    }
}