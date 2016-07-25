package com.guipenedo.pokeradar.scan;

import com.google.android.gms.maps.model.LatLng;
import com.pokegoapi.api.map.MapObjects;

public interface ScanUpdateCallback {
    void scanUpdate(MapObjects objects, int progress, LatLng location);
}