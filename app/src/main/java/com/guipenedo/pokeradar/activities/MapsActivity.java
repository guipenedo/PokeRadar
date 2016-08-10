/*
 * Copyright 2016 Guilherme Penedo
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.guipenedo.pokeradar.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.guipenedo.pokeradar.R;
import com.guipenedo.pokeradar.Utils;
import com.guipenedo.pokeradar.activities.settings.MainSettingsActivity;
import com.guipenedo.pokeradar.module.MapWrapper;
import com.guipenedo.pokeradar.module.PGym;
import com.guipenedo.pokeradar.module.PMarker;
import com.guipenedo.pokeradar.module.PPokemon;
import com.guipenedo.pokeradar.module.PPokestop;
import com.guipenedo.pokeradar.module.Team;
import com.guipenedo.pokeradar.scan.ScanCompleteCallback;
import com.guipenedo.pokeradar.scan.ScanSettings;
import com.guipenedo.pokeradar.scan.ScanTask;
import com.guipenedo.pokeradar.scan.ScanUpdateCallback;
import com.pokegoapi.api.map.Point;
import com.pokegoapi.api.map.fort.Pokestop;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import POGOProtos.Enums.PokemonIdOuterClass;
import POGOProtos.Map.Fort.FortDataOuterClass;
import io.fabric.sdk.android.Fabric;
import okhttp3.OkHttpClient;

public class MapsActivity extends AppCompatActivity implements LocationListener, OnMapReadyCallback, GoogleMap.OnMapLongClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ScanUpdateCallback, ScanCompleteCallback {


    private static OkHttpClient http;
    private final double gpsOffset = 0.0015;
    private Handler handler = new Handler();
    //display items
    private HashMap<String, PMarker> pokemonMarkers = new HashMap<>();
    private HashMap<String, Runnable> updateTasks = new HashMap<>();
    private Set<Long> pokemon = new HashSet<>();
    private HashMap<String, Marker> luredPokemon = new HashMap<>();
    private Set<String> forts = new HashSet<>(), spawnpoints = new HashSet<>();
    //maps stuff
    private GoogleMap mMap;
    private LatLng location;
    private GoogleApiClient googleApiClient;
    private Marker center, rangeCenter;
    private Circle scanRange;
    private Polygon scanArea;
    private Set<Marker> markers = new HashSet<>();
    private String countdownMarker;
    //ui elements
    private ProgressBar progressBar;
    private Button scanButton;
    private ProgressBar scanProgressBar;
    private Button cancelScanButton;
    private ScanTask scanMap;
    //settings
    private SharedPreferences mainPrefs;
    private boolean showGyms, showPokemons, showPokestops, showSpawnpoints, showScanArea;
    private int steps;
    private SharedPreferences.OnSharedPreferenceChangeListener preferencesChangelistener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            mainPrefs = prefs;
            loadPrefs();
            for (Marker m : new ArrayList<>(markers)) {
                if (m == null || !pokemonMarkers.containsKey(m.getId())) continue;
                PMarker marker = pokemonMarkers.get(m.getId());
                if ((marker.type == PMarker.MarkerType.POKEMON && !showPokemons)
                        || ((marker.type == PMarker.MarkerType.POKESTOP || marker.type == PMarker.MarkerType.LUREDPOKESTOP) && !showPokestops)
                        || (marker.type == PMarker.MarkerType.GYM && !showGyms)
                        || (marker.type == PMarker.MarkerType.SPAWNPOINT && !showSpawnpoints)) {
                    forts.remove(marker.getId());
                    luredPokemon.remove(marker.getId());
                    if (marker.type == PMarker.MarkerType.POKEMON)
                        pokemon.remove(Long.parseLong(marker.getId()));
                    spawnpoints.remove(marker.getId());

                    removeMarker(m);
                }
            }
            if (scanArea != null) scanArea.remove();
            if (showScanArea)
                scanArea = mMap.addPolygon(new PolygonOptions().addAll(createRectangle(location, steps * gpsOffset)).fillColor(Color.argb(20, 50, 0, 255)));
        }
    };

    public static OkHttpClient getHttp() {
        if (http == null)
            http = new OkHttpClient();
        return http;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_maps);

        getPermissions();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (googleApiClient == null)
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();

        scanButton = (Button) findViewById(R.id.scanButton);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scan();
            }
        });

        cancelScanButton = (Button) findViewById(R.id.cancelScanButton);
        cancelScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopTasks();
                if (scanMap != null && !scanMap.isCancelled())
                    scanMap.cancel(true);
                setScanning(false);
                update();
            }
        });

        scanProgressBar = (ProgressBar) findViewById(R.id.scanProgressBar);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
    }

    private void clearAll() {
        mMap.clear();
        markers.clear();
        forts.clear();
        luredPokemon.clear();
        pokemon.clear();
        spawnpoints.clear();
        pokemonMarkers.clear();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem settings = menu.add(0, 0, 0, R.string.settings).setIcon(R.drawable.cog);
        settings.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                Intent i = new Intent(getBaseContext(), MainSettingsActivity.class);
                startActivity(i);
                return true;
            }
        });
        MenuItem info = menu.add(0, 0, 0, R.string.about).setIcon(R.drawable.info_circle);
        info.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                Intent i = new Intent(getBaseContext(), AboutActivity.class);
                startActivity(i);
                return true;
            }
        });
        info.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        settings.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        MenuItem logOut = menu.add(0, 0, 0, R.string.log_out).setIcon(R.drawable.sign_out);
        logOut.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                Utils.relog(MapsActivity.this);
                return true;
            }
        });
        logOut.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        return true;
    }

    public void stopTasks() {
        for (Runnable r : updateTasks.values())
            handler.removeCallbacks(r);
        updateTasks.clear();
    }

    public void scan() {
        if (mMap == null || location == null) return;

        setScanning(true);
        update();
        Toast.makeText(this, String.format(getString(R.string.expected_scan_time), Utils.countdownFromMillis(this, 6200 * (1 + steps * 2) * (1 + steps * 2))), Toast.LENGTH_LONG).show();

        ScanSettings settings = new ScanSettings(MainActivity.username, MainActivity.password, getScanLocations(location, steps));
        settings.gyms = showGyms;
        settings.pokemon = showPokemons;
        settings.pokestops = showPokestops;
        settings.spawnpoints = showSpawnpoints;
        scanMap = new ScanTask(settings, this, this);
        scanMap.execute();
    }

    @Override
    public void scanComplete() {
        setScanning(false);
    }

    @Override
    public void scanUpdate(MapWrapper map, int progress, LatLng location) {
        if (showPokestops)
            for (Pokestop data : map.getPokestops())
                if (data.getFortData().getLureInfo().getLureExpiresTimestampMs() > 0)
                    addLuredPokestop(data.getFortData());
                else
                    addPokestop(data.getFortData());
        if (showGyms)
            for (PGym gym : map.getGyms())
                addGym(gym);
        if (showPokemons)
            for (CatchablePokemon pokemon : map.getPokemon())
                addPokemon(pokemon);
        if (showSpawnpoints)
            for (Point data : map.getSpawnpoints())
                addSpawnpoint(data);

        if (rangeCenter == null)
            rangeCenter = mMap.addMarker(new MarkerOptions().position(location).title("SCAN CENTER").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
        rangeCenter.setPosition(location);
        scanRange.setCenter(location);
        progressBar.setProgress(progress);
    }

    private List<LatLng> getScanLocations(LatLng location, int t) {
        t *= 2;
        List<LatLng> locations = new ArrayList<>();
        int x = 0, y = 0, dx = 0, dy = -1;
        int maxI = t * t * 4;
        int h = t / 2;

        for (int i = 0; i < Math.max(1, maxI); i++) {
            if ((-h <= x) && (x <= h) && (-h <= y) && (y <= h)) {
                locations.add(new LatLng(location.latitude + x * gpsOffset, location.longitude + y * gpsOffset));
            }

            if ((x == y) || ((x < 0) && (x == -y)) || ((x > 0) && (x == 1 - y))) {
                t = dx;
                dx = -dy;
                dy = t;
            }
            x += dx;
            y += dy;
        }

        return locations;
    }

    public void updateMarkerAtTime(Marker marker, long time) {
        updateMarker(marker, time - System.currentTimeMillis());
    }

    public void updateMarker(Marker marker, long interval) {
        UpdateRunnable runnable = new UpdateRunnable(marker);
        updateTasks.put(marker.getId(), runnable);
        handler.postDelayed(runnable, interval);
    }

    public void setScanning(boolean scanning) {
        if (scanRange != null) scanRange.remove();
        if (scanning) {
            scanRange = mMap.addCircle(new CircleOptions().center(location).strokeColor(Color.argb(50, 29, 132, 181)).fillColor(Color.argb(30, 29, 132, 181)).radius(50));
            scanButton.setVisibility(View.INVISIBLE);
            scanProgressBar.setVisibility(View.VISIBLE);
            cancelScanButton.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(0);
        } else {
            scanButton.setVisibility(View.VISIBLE);
            scanProgressBar.setVisibility(View.INVISIBLE);
            cancelScanButton.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.INVISIBLE);
            scanRange = null;
            if (rangeCenter != null) rangeCenter.remove();
            rangeCenter = null;
        }
    }

    public void getPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION}, 1400);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopTasks();
        if (scanMap != null && !scanMap.isCancelled())
            scanMap.cancel(true);
    }

    public void centerCamera() {
        if (location != null) {
            CameraPosition.Builder builder = new CameraPosition.Builder();
            builder.zoom(16);
            builder.target(location);

            this.mMap.animateCamera(CameraUpdateFactory.newCameraPosition(builder.build()));
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.setOnMapLongClickListener(this);
        mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                onConnected(null);
                update();
                return true;
            }
        });
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker m) {
                PMarker marker = pokemonMarkers.get(m.getId());
                if (marker == null || marker.type != PMarker.MarkerType.GYM) return;
                Intent gymIntent = new Intent(MapsActivity.this, GymDetailsActivity.class);
                gymIntent.putExtra("gymDetails", (PGym) marker);
                startActivity(gymIntent);
            }
        });
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            @Override
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            @Override
            public View getInfoContents(final Marker m) {

                Context mContext = MapsActivity.this;

                LinearLayout info = new LinearLayout(mContext);
                info.setOrientation(LinearLayout.VERTICAL);

                TextView title = new TextView(mContext);
                title.setText(m.getTitle());
                title.setTypeface(null, Typeface.BOLD);
                title.setGravity(Gravity.CENTER);
                info.addView(title);

                final PMarker marker = pokemonMarkers.get(m.getId());
                long timestamp = -1;
                if (marker != null) {
                    if (marker.type == PMarker.MarkerType.CENTER) {
                        TextView littleNotice = new TextView(mContext);
                        littleNotice.setText(R.string.scan_center_infowindow);
                        littleNotice.setGravity(Gravity.CENTER);
                        info.addView(littleNotice);
                    } else if (marker.type == PMarker.MarkerType.LUREDPOKESTOP) {
                        PPokestop pokestopMarker = (PPokestop) marker;
                        timestamp = pokestopMarker.getTimestamp();
                        TextView remainingTime = new TextView(mContext);
                        String text = String.format(getString(R.string.lured_remaining), Utils.countdownFromMillis(mContext, pokestopMarker.getTimestamp() - System.currentTimeMillis()));
                        remainingTime.setText(text);
                        remainingTime.setGravity(Gravity.CENTER);
                        info.addView(remainingTime);
                        TextView expireTime = new TextView(mContext);
                        expireTime.setText(Utils.timeFromMillis(pokestopMarker.getTimestamp()));
                        expireTime.setGravity(Gravity.CENTER);
                        info.addView(expireTime);
                    } else if (marker.type == PMarker.MarkerType.GYM) {
                        PGym gymMarker = (PGym) marker;
                        Team team = Team.fromTeamColor(gymMarker.getTeam());
                        TextView teamName = new TextView(mContext);
                        teamName.setText(team.getName());
                        teamName.setTextColor(team.getColor());
                        teamName.setGravity(Gravity.CENTER);
                        info.addView(teamName);
                        TextView prestige = new TextView(mContext);
                        prestige.setText(String.format(getString(R.string.gym_points), gymMarker.getPoints()));
                        prestige.setGravity(Gravity.CENTER);
                        info.addView(prestige);
                        TextView clickDetails = new TextView(mContext);
                        clickDetails.setText(R.string.gym_details);
                        clickDetails.setTypeface(null, Typeface.BOLD);
                        clickDetails.setGravity(Gravity.CENTER);
                        info.addView(clickDetails);
                    } else if (marker.type == PMarker.MarkerType.POKEMON) {
                        PPokemon pokemonMarker = (PPokemon) marker;
                        timestamp = pokemonMarker.getTimestamp();
                        final TextView remainingTime = new TextView(mContext);
                        remainingTime.setText(String.format(getString(R.string.pokemon_despawns_time), Utils.countdownFromMillis(mContext, pokemonMarker.getTimestamp() - System.currentTimeMillis())));
                        remainingTime.setGravity(Gravity.CENTER);
                        info.addView(remainingTime);
                        TextView expireTime = new TextView(mContext);
                        expireTime.setText(Utils.timeFromMillis(pokemonMarker.getTimestamp()));
                        expireTime.setGravity(Gravity.CENTER);
                        info.addView(expireTime);
                    }
                    if (timestamp != -1 && (countdownMarker == null || !countdownMarker.equals(m.getId()))) {
                        countdownMarker = m.getId();
                        new CountDownTimer(timestamp - System.currentTimeMillis(), 1000) {

                            public void onTick(long millisUntilFinished) {
                                if (markers.contains(m) && m.isInfoWindowShown())
                                    m.showInfoWindow();
                                else
                                    cancel();
                            }

                            @Override
                            public void onFinish() {
                                countdownMarker = null;
                            }
                        }.start();
                    }
                }
                return info;
            }
        });
        update();
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        location = latLng;
        update();
    }

    private void update() {
        if (mMap == null || location == null) return;

        if (center != null) removeMarker(center);
        if (scanArea != null) scanArea.remove();
        if (showScanArea)
            scanArea = mMap.addPolygon(new PolygonOptions().addAll(createRectangle(location, steps * gpsOffset)).fillColor(Color.argb(20, 50, 0, 255)));
        center = addMarker(getString(R.string.center_marker_title), location, new PMarker(PMarker.MarkerType.CENTER), true);
        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {

            }

            @Override
            public void onMarkerDrag(Marker marker) {

            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                location = marker.getPosition();
                update();
            }
        });
        centerCamera();
    }

    public Marker addMarker(String title, LatLng position, PMarker pMarker, boolean draggable) {
        Marker marker = mMap.addMarker(new MarkerOptions().position(position).title(title).draggable(draggable));
        pokemonMarkers.put(marker.getId(), pMarker);
        markers.add(marker);
        return marker;
    }

    public Marker addMarker(@NonNull String title, @NonNull LatLng position, PMarker pMarker, @NonNull BitmapDescriptor icon) {
        Marker marker = mMap.addMarker(new MarkerOptions().position(position).title(title).icon(icon));
        pokemonMarkers.put(marker.getId(), pMarker);
        markers.add(marker);
        return marker;
    }

    @Override
    public void onLocationChanged(Location location) {
        this.location = new LatLng(location.getLatitude(), location.getLongitude());
        update();
    }

    private List<LatLng> createRectangle(LatLng center, double offset) {
        return Arrays.asList(new LatLng(center.latitude - offset, center.longitude - offset),
                new LatLng(center.latitude - offset, center.longitude + offset),
                new LatLng(center.latitude + offset, center.longitude + offset),
                new LatLng(center.latitude + offset, center.longitude - offset));
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (LocationServices.FusedLocationApi.getLastLocation(googleApiClient) != null) {
            location = Utils.locationToLatLng(LocationServices.FusedLocationApi.getLastLocation(googleApiClient));
            if (center == null) update();
        } else
            Toast.makeText(this, R.string.error_location_services_disabled, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    protected void onStart() {
        googleApiClient.connect();
        super.onStart();
        mainPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        loadPrefs();

        mainPrefs.registerOnSharedPreferenceChangeListener(preferencesChangelistener);
        updateMarkers();
    }

    public void updateMarkers() {
        for (Marker m : new ArrayList<>(markers)) {
            PMarker marker = pokemonMarkers.get(m.getId());
            if (marker == null) continue;
            if (marker.type == PMarker.MarkerType.LUREDPOKESTOP) {
                updateMarker(m, 50);
            } else if (marker.type == PMarker.MarkerType.POKEMON) {
                updatePokemon(m);
            }
        }
    }

    public void addPokestop(FortDataOuterClass.FortData data) {
        if (fortExists(data.getId())) return;
        addPokestop(new LatLng(data.getLatitude(), data.getLongitude()), data.getId());
    }

    public void addPokestop(LatLng location, String id) {
        addMarker(getString(R.string.pokestop), location, new PMarker(PMarker.MarkerType.POKESTOP, id), BitmapDescriptorFactory.fromResource(R.drawable.pokestop));
    }

    public void addLuredPokestop(FortDataOuterClass.FortData data) {
        if (fortExists(data.getId())) return;
        LatLng location = new LatLng(data.getLatitude(), data.getLongitude());
        PPokemon marker = new PPokemon();
        marker.setId(location.toString());
        luredPokemon.put(data.getId(), addPokemon(location, marker, data.getLureInfo().getActivePokemonId()));
        addLuredPokestop(location, data.getId(), data.getLureInfo().getLureExpiresTimestampMs());
        forts.add(data.getId());
    }

    public void addLuredPokestop(LatLng location, String id, long timestamp) {
        PMarker marker = new PPokestop(timestamp);
        marker.setId(id);
        Marker m = addMarker(getString(R.string.lured_pokestop), location, marker, BitmapDescriptorFactory.fromResource(R.drawable.luredpokestop));
        updateMarkerAtTime(m, timestamp);
    }

    public void addGym(PGym gym) {
        if (fortExists(gym.getId())) return;
        LatLng location = new LatLng(gym.getLatitude(), gym.getLongitude());
        addMarker(getString(R.string.gym), location, gym, Team.fromTeamColor(gym.getTeam()).getImage(MapsActivity.this));
        forts.add(gym.getId());
    }

    private void addSpawnpoint(Point data) {
        if (spawnpointExists(data)) return;
        LatLng location = new LatLng(data.getLatitude(), data.getLongitude());
        addMarker(getString(R.string.spawnpoint), location, new PMarker(PMarker.MarkerType.SPAWNPOINT, spawnpointToString(data)), BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        spawnpoints.add(data.getLatitude() + ":" + data.getLongitude());
    }

    private boolean spawnpointExists(Point data) {
        return spawnpoints.contains(spawnpointToString(data));
    }

    private String spawnpointToString(Point data) {
        return data.getLatitude() + ":" + data.getLongitude();
    }

    private boolean fortExists(String id) {
        return forts.contains(id);
    }

    public void addPokemon(CatchablePokemon data) {
        if (pokemon.contains(data.getEncounterId()) || !mainPrefs.getBoolean("pref_key_show_pokemon_" + data.getPokemonId().getNumber(), true))
            return;
        LatLng location = new LatLng(data.getLatitude(), data.getLongitude());
        PMarker marker = new PPokemon(data);
        marker.setId(String.valueOf(data.getEncounterId()));
        Marker m = addMarker(Utils.formatPokemonName(data.getPokemonId().toString()), location, marker, BitmapDescriptorFactory.fromBitmap(Utils.bitmapForPokemon(this, data.getPokemonId().getNumber())));
        updatePokemon(m);
        pokemon.add(data.getEncounterId());
    }

    public Marker addPokemon(LatLng location, PPokemon marker, PokemonIdOuterClass.PokemonId id) {
        if (!mainPrefs.getBoolean("pref_key_show_pokemon_" + id.getNumber(), true))
            return null;
        return addMarker(Utils.formatPokemonName(id.toString()), location, marker, BitmapDescriptorFactory.fromBitmap(Utils.bitmapForPokemon(this, id.getNumber())));
    }

    public void updatePokemon(Marker m) {
        PMarker pMarker = pokemonMarkers.get(m.getId());
        if (pMarker == null) return;

        PPokemon marker = (PPokemon) pMarker;
        long interval = marker.getTimestamp() - System.currentTimeMillis();

        if (interval <= 0) {
            removeMarker(m);
        } else if (interval <= 10 * 1000) {
            m.setAlpha(0.2f);
            updateMarker(m, interval);
        } else if (interval <= 60 * 1000) {
            m.setAlpha(0.5f);
            updateMarker(m, interval - 10 * 1000);
        } else
            updateMarker(m, interval - 60 * 1000);
    }

    public void removeMarker(Marker m) {
        markers.remove(m);
        pokemonMarkers.remove(m.getId());
        if (updateTasks.containsKey(m.getId()))
            handler.removeCallbacks(updateTasks.get(m.getId()));
        m.remove();
    }

    @Override
    protected void onStop() {
        googleApiClient.disconnect();
        super.onStop();
        stopTasks();
    }

    public void loadPrefs() {
        showPokemons = mainPrefs.getBoolean("pref_key_show_pokemon", true);
        showGyms = mainPrefs.getBoolean("pref_key_show_gyms", true);
        showPokestops = mainPrefs.getBoolean("pref_key_show_pokestops", true);
        showSpawnpoints = mainPrefs.getBoolean("pref_key_show_spawnpoints", false);
        try {
            steps = Integer.parseInt(mainPrefs.getString("pref_key_steps", "4"));
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.error_steps_invalid, Toast.LENGTH_LONG).show();
            steps = 4;
        }
        showScanArea = mainPrefs.getBoolean("pref_key_show_scanarea", false);


        Crashlytics.setBool("showGyms", showGyms);
        Crashlytics.setBool("showPokestops", showPokestops);
        Crashlytics.setBool("showSpawnpoints", showSpawnpoints);
        Crashlytics.setBool("showPokemon", showPokemons);
        Crashlytics.setInt("steps", steps);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    private class UpdateRunnable implements Runnable {
        Marker marker;

        public UpdateRunnable(Marker marker) {
            this.marker = marker;
        }

        @Override
        public void run() {

            updateTasks.remove(marker.getId());

            final PMarker pMarker = pokemonMarkers.get(marker.getId());
            if (pMarker == null) return;

            if (pMarker.type == PMarker.MarkerType.LUREDPOKESTOP) {
                if (luredPokemon.get(pMarker.getId()) != null) {
                    removeMarker(luredPokemon.get(pMarker.getId()));
                    luredPokemon.remove(pMarker.getId());
                }
                /*ScanSettings settings = new ScanSettings(username, password, Collections.singletonList(marker.getPosition()));
                settings.spawnpoints = false;
                settings.pokemon = false;
                settings.gyms = false;
                ScanMap task = new ScanMap(settings, new ScanUpdateCallback() {
                    @Override
                    public void scanUpdate(MapWrapper map, int progress, LatLng location) {
                        for (Pokestop pokestop : map.getPokestops())
                            if (pokestop.getId().equals(pMarker.getId())){

                                return;
                            }
                    }
                }, new ScanCompleteCallback() {@Override public void scanComplete(Exception result) {}});
                task.execute();*/
                removeMarker(marker);
            } else if (pMarker.type == PMarker.MarkerType.POKEMON) {
                updatePokemon(marker);
            }
        }
    }
}
