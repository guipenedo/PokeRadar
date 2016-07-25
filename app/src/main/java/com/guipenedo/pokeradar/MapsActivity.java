package com.guipenedo.pokeradar;

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
import com.guipenedo.pokeradar.module.PokemonMarker;
import com.guipenedo.pokeradar.module.Team;
import com.guipenedo.pokeradar.scan.ScanCompleteCallback;
import com.guipenedo.pokeradar.scan.ScanSettings;
import com.guipenedo.pokeradar.scan.ScanTask;
import com.guipenedo.pokeradar.scan.ScanUpdateCallback;
import com.guipenedo.pokeradar.settings.MainSettingsActivity;
import com.pokegoapi.api.map.MapObjects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import POGOProtos.Map.Fort.FortDataOuterClass;
import POGOProtos.Map.Pokemon.MapPokemonOuterClass;
import POGOProtos.Map.SpawnPointOuterClass;
import io.fabric.sdk.android.Fabric;

public class MapsActivity extends AppCompatActivity implements LocationListener, OnMapReadyCallback, GoogleMap.OnMapLongClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ScanUpdateCallback, ScanCompleteCallback {

    public static final String preferencesKey = "PGORADAR";
    private Handler handler = new Handler();
    private String username, password;

    //display items
    private HashMap<String, PokemonMarker> pokemonMarkers = new HashMap<>();
    private HashMap<String, Runnable> updateTasks = new HashMap<>();
    private Set<Long> pokemon = new TreeSet<>();
    private Set<String> forts = new TreeSet<>(), spawnpoints = new TreeSet<>();

    //maps stuff
    private GoogleMap mMap;
    private LatLng location;
    private GoogleApiClient googleApiClient;
    private Marker center;
    private Circle scanRange;
    private Polygon scanArea;
    private Set<Marker> markers = new HashSet<>();
    private String countdownMarker;
    private final double gpsOffset = 0.0015;

    //ui elements
    private ProgressBar progressBar;
    private Button scanButton;
    private ProgressBar scanProgressBar;
    private Button cancelScanButton;

    private ScanTask scanTask;
    private boolean scanning = false;

    //settings
    private SharedPreferences mainPrefs;
    private boolean showGyms, showPokemons, showPokestops, showSpawnpoints, showScanCenters, showScanArea;
    private int secondsBetweenRequests, steps;

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
                if (scanTask != null && !scanTask.isCancelled())
                    scanTask.cancel(true);
                setScanning(false);
                clearAll();
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
        pokemon.clear();
        spawnpoints.clear();
        pokemonMarkers.clear();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem settings = menu.add(0, 0, 0, "Settings").setIcon(R.drawable.cog);
        settings.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                Intent i = new Intent(getBaseContext(), MainSettingsActivity.class);
                startActivity(i);
                return true;
            }
        });
        MenuItem info = menu.add(0, 0, 0, "About").setIcon(R.drawable.info_circle);
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
        MenuItem logOut = menu.add(0, 0, 0, "Log out").setIcon(R.drawable.sign_out);
        logOut.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                SharedPreferences.Editor editor = getSharedPreferences(MapsActivity.preferencesKey, Context.MODE_PRIVATE).edit();
                editor.putBoolean("login", false);
                editor.apply();
                relog();
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
        clearAll();
        update();
        stopTasks();
        Toast.makeText(this, "Expected scan time: " + Utils.countdownFromMillis(1000 * secondsBetweenRequests * (1 + steps * 2) * (1 + steps * 2)), Toast.LENGTH_LONG).show();

        ScanSettings settings = new ScanSettings(username, password, getScanLocations(location, steps), secondsBetweenRequests * 1000);
        scanTask = new ScanTask(settings, this, this);
        scanTask.execute();
    }

    @Override
    public void scanComplete(Boolean result) {
        if (!result)
            Toast.makeText(MapsActivity.this, "Could not fetch data! Are the servers down?", Toast.LENGTH_LONG).show();
        setScanning(false);
    }

    @Override
    public void scanUpdate(MapObjects object, int progress, LatLng location) {
        if (showPokestops)
            for (FortDataOuterClass.FortData data : object.getPokestops())
                if (data.getLureInfo().getLureExpiresTimestampMs() > 0)
                    addLuredPokestop(data);
                else
                    addPokestop(data);
        if (showGyms)
            for (FortDataOuterClass.FortData data : object.getGyms())
                addGym(data);
        if (showPokemons)
            for (MapPokemonOuterClass.MapPokemon data : object.getCatchablePokemons())
                addPokemon(data);
        if (showSpawnpoints) {
            for (SpawnPointOuterClass.SpawnPoint data : object.getSpawnPoints())
                addSpawnpoint(data);
            for (SpawnPointOuterClass.SpawnPoint data : object.getDecimatedSpawnPoints())
                addSpawnpoint(data);
        }
        if (showScanCenters)
            addMarker("Scan center", location, null, false);

        scanRange.setCenter(location);
        progressBar.setProgress(progress);
    }

    private List<LatLng> getScanLocations(LatLng location, int t) {
        t *= 2;
        List<LatLng> locations = new ArrayList<>();
        int x = 0, y = 0, dx = 0, dy = -1;
        int maxI = t * t * 4;
        int h = t / 2;

        for (int i = 0; i < maxI; i++) {
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

    private class UpdateRunnable implements Runnable {
        Marker marker;

        public UpdateRunnable(Marker marker) {
            this.marker = marker;
        }

        @Override
        public void run() {

            updateTasks.remove(marker.getId());

            PokemonMarker pokemonMarker = pokemonMarkers.get(marker.getId());
            if (pokemonMarker == null) return;

            if (pokemonMarker.type == PokemonMarker.MarkerType.LUREDPOKESTOP) {
                addPokestop(marker.getPosition());
                removeMarker(marker);
            } else if (pokemonMarker.type == PokemonMarker.MarkerType.POKEMON) {
                updatePokemon(marker);
            }
        }
    }

    public void setScanning(boolean scanning) {
        this.scanning = scanning;
        if (scanRange != null) scanRange.remove();
        if (scanning) {
            scanRange = mMap.addCircle(new CircleOptions().center(location).strokeColor(Color.argb(32, 29, 132, 181)).radius(50));
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

    public void centerCamera() {
        if (location != null) {
            CameraPosition.Builder builder = new CameraPosition.Builder();
            builder.zoom(18);
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
                return true;
            }
        });
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if (!marker.getTitle().equals("Center")) {
                    PokemonMarker poMarker = pokemonMarkers.get(marker.getId());
                    if (poMarker != null && poMarker.text != null)
                        Toast.makeText(MapsActivity.this, poMarker.text, Toast.LENGTH_SHORT).show();
                }
                return false;
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

                final PokemonMarker marker = pokemonMarkers.get(m.getId());
                boolean countdown = false;
                if (marker != null) {
                    if (marker.type == PokemonMarker.MarkerType.CENTER) {
                        TextView littleNotice = new TextView(mContext);
                        littleNotice.setText("Scans are centered on this location.\n Long press on the map or drag it to change center.");
                        littleNotice.setGravity(Gravity.CENTER);
                        info.addView(littleNotice);
                    } else if (marker.type == PokemonMarker.MarkerType.LUREDPOKESTOP) {
                        countdown = true;
                        TextView remainingTime = new TextView(mContext);
                        String text = Utils.countdownFromMillis(marker.timestamp - System.currentTimeMillis()) + " remaining";
                        remainingTime.setText(text);
                        remainingTime.setGravity(Gravity.CENTER);
                        info.addView(remainingTime);
                        TextView expireTime = new TextView(mContext);
                        expireTime.setText(Utils.timeFromMillis(marker.timestamp));
                        expireTime.setGravity(Gravity.CENTER);
                        info.addView(expireTime);
                    } else if (marker.type == PokemonMarker.MarkerType.GYM) {
                        Team team = Team.fromTeamColor(marker.team);
                        TextView teamName = new TextView(mContext);
                        teamName.setText(team.getName());
                        teamName.setTextColor(team.getColor());
                        teamName.setGravity(Gravity.CENTER);
                        info.addView(teamName);
                        TextView prestige = new TextView(mContext);
                        prestige.setText("Points: " + marker.prestige);
                        prestige.setGravity(Gravity.CENTER);
                        info.addView(prestige);
                    } else if (marker.type == PokemonMarker.MarkerType.POKEMON) {
                        countdown = true;
                        final TextView remainingTime = new TextView(mContext);
                        String text = "Despawns in: " + Utils.countdownFromMillis(marker.timestamp - System.currentTimeMillis());
                        remainingTime.setText(text);
                        remainingTime.setGravity(Gravity.CENTER);
                        info.addView(remainingTime);
                        TextView expireTime = new TextView(mContext);
                        expireTime.setText(Utils.timeFromMillis(marker.timestamp));
                        expireTime.setGravity(Gravity.CENTER);
                        info.addView(expireTime);
                    }
                    if (countdown && (countdownMarker == null || !countdownMarker.equals(m.getId()))) {
                        countdownMarker = m.getId();
                        new CountDownTimer(marker.timestamp - System.currentTimeMillis(), 1000) {

                            public void onTick(long millisUntilFinished) {
                                System.out.println("UPDATING " + m.getId());
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
        center = addMarker("Center", location, new PokemonMarker(PokemonMarker.MarkerType.CENTER), true);
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

    public Marker addMarker(String title, LatLng position, PokemonMarker pokemonMarker, boolean draggable) {
        Marker marker = mMap.addMarker(new MarkerOptions().position(position).title(title).draggable(draggable));
        pokemonMarkers.put(marker.getId(), pokemonMarker);
        markers.add(marker);
        return marker;
    }

    public Marker addMarker(@NonNull String title, @NonNull LatLng position, PokemonMarker pokemonMarker, @NonNull BitmapDescriptor icon) {
        Marker marker = mMap.addMarker(new MarkerOptions().position(position).title(title).icon(icon));
        pokemonMarkers.put(marker.getId(), pokemonMarker);
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
        location = Utils.locationToLatLng(LocationServices.FusedLocationApi.getLastLocation(googleApiClient));
        update();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    protected void onStart() {
        googleApiClient.connect();
        super.onStart();
        mainPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        showPokemons = mainPrefs.getBoolean("pref_key_show_pokemon", true);
        showGyms = mainPrefs.getBoolean("pref_key_show_gyms", true);
        showPokestops = mainPrefs.getBoolean("pref_key_show_pokestops", true);
        showSpawnpoints = mainPrefs.getBoolean("pref_key_show_spawnpoints", false);
        showScanCenters = mainPrefs.getBoolean("pref_key_show_centers", false);
        secondsBetweenRequests = Integer.parseInt(mainPrefs.getString("pref_key_delay", "2"));
        steps = Integer.parseInt(mainPrefs.getString("pref_key_steps", "4"));
        showScanArea = mainPrefs.getBoolean("pref_key_show_scanarea", false);

        Crashlytics.setBool("showGyms", showGyms);
        Crashlytics.setBool("showPokestops", showPokestops);
        Crashlytics.setBool("showSpawnpoints", showSpawnpoints);
        Crashlytics.setBool("showPokemon", showPokemons);
        Crashlytics.setInt("request_delay", secondsBetweenRequests);
        Crashlytics.setInt("steps", steps);

        SharedPreferences sharedPref = getSharedPreferences(preferencesKey, Context.MODE_PRIVATE);

        boolean login = sharedPref.getBoolean("login", false);

        if (login) {
            username = sharedPref.getString("username", null);
            password = sharedPref.getString("password", null);
        } else {
            relog();
        }
        updateMarkers();
    }

    public void updateMarkers() {
        for (Marker m : new ArrayList<>(markers)) {
            PokemonMarker marker = pokemonMarkers.get(m.getId());
            if (marker == null) continue;
            if (marker.type == PokemonMarker.MarkerType.LUREDPOKESTOP) {
                if (marker.timestamp > System.currentTimeMillis()) {
                    addLuredPokestop(m.getPosition(), marker.timestamp);
                } else
                    addPokestop(m.getPosition());
                removeMarker(m);
            } else if (marker.type == PokemonMarker.MarkerType.POKEMON) {
                updatePokemon(m);
            }
        }
    }

    public void addPokestop(FortDataOuterClass.FortData data) {
        if (fortExists(data)) return;
        addPokestop(new LatLng(data.getLatitude(), data.getLongitude()));
    }

    public void addPokestop(LatLng location) {
        addMarker("POKESTOP", location, new PokemonMarker(PokemonMarker.MarkerType.POKESTOP), BitmapDescriptorFactory.fromResource(R.drawable.pokestop));
    }

    public void addLuredPokestop(FortDataOuterClass.FortData data) {
        if (fortExists(data)) return;
        LatLng location = new LatLng(data.getLatitude(), data.getLongitude());
        addLuredPokestop(location, data.getLureInfo().getLureExpiresTimestampMs());
        forts.add(data.getId());
    }

    public void addLuredPokestop(LatLng location, long timestamp) {
        PokemonMarker marker = new PokemonMarker(PokemonMarker.MarkerType.LUREDPOKESTOP);
        marker.timestamp = timestamp;
        Marker m = addMarker("LURED POKESTOP", location, marker, BitmapDescriptorFactory.fromResource(R.drawable.luredpokestop));
        updateMarkerAtTime(m, marker.timestamp);
    }

    public void addGym(FortDataOuterClass.FortData data) {
        if (fortExists(data)) return;
        LatLng location = new LatLng(data.getLatitude(), data.getLongitude());
        PokemonMarker marker = new PokemonMarker(PokemonMarker.MarkerType.GYM);
        marker.prestige = data.getGymPoints();
        marker.team = data.getOwnedByTeam();
        addMarker("GYM", location, marker, Team.fromTeamColor(marker.team).getImage(MapsActivity.this));
        forts.add(data.getId());
    }

    private void addSpawnpoint(SpawnPointOuterClass.SpawnPoint data) {
        if (spawnpointExists(data)) return;
        LatLng location = new LatLng(data.getLatitude(), data.getLongitude());
        addMarker("SPAWNPOINT", location, null, BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        spawnpoints.add(data.getLatitude() + ":" + data.getLongitude());
    }

    private boolean spawnpointExists(SpawnPointOuterClass.SpawnPoint data) {
        return spawnpoints.contains(data.getLatitude() + ":" + data.getLongitude());
    }

    private boolean fortExists(FortDataOuterClass.FortData data) {
        return forts.contains(data.getId());
    }

    public void addPokemon(MapPokemonOuterClass.MapPokemon data) {
        if (pokemon.contains(data.getEncounterId()) || !mainPrefs.getBoolean("pref_key_show_pokemon_" + data.getPokemonIdValue(), true))
            return;
        PokemonMarker marker = new PokemonMarker(PokemonMarker.MarkerType.POKEMON);

        LatLng location = new LatLng(data.getLatitude(), data.getLongitude());
        marker.timestamp = data.getExpirationTimestampMs();
        Marker m = addMarker(data.getPokemonId().toString(), location, marker, BitmapDescriptorFactory.fromBitmap(Utils.bitmapForPokemon(this, data.getPokemonIdValue())));
        updatePokemon(m);
        pokemon.add(data.getEncounterId());
    }

    public void updatePokemon(Marker m) {
        PokemonMarker pokemonMarker = pokemonMarkers.get(m.getId());
        long interval = pokemonMarker.timestamp - System.currentTimeMillis();

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

    public void relog() {
        Intent i = new Intent(getBaseContext(), LoginActivity.class);
        startActivity(i);
    }

    public void removeMarker(Marker m) {
        markers.remove(m);
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

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }
}
