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

package com.guipenedo.pokeradar;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.maps.model.LatLng;
import com.guipenedo.pokeradar.activities.LoginActivity;
import com.guipenedo.pokeradar.activities.MainActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class Utils {

    public static String countdownFromMillis(Context context, long millis) {
        return String.format(Locale.US, context.getString(R.string.countdown),
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        );
    }

    public static String timeFromMillis(long millis) {
        Date date = new Date(millis);
        DateFormat formatter = new SimpleDateFormat("HH:mm:ss", Locale.US);
        return formatter.format(date);
    }

    public static LatLng locationToLatLng(Location location) {
        return new LatLng(location.getLatitude(), location.getLongitude());
    }

    public static void writeToFile(Activity context, String fileName, String body) {
        boolean hasPermission = (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        if (!hasPermission) {
            ActivityCompat.requestPermissions(context,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    112);
        }
        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File(sdCard.getAbsolutePath() + "/Android/data/pgoradar");
        dir.mkdirs();
        File file = new File(dir, fileName + ".txt");

        try {
            FileOutputStream f = new FileOutputStream(file, true);
            f.write(body.getBytes());
            f.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String loadJSONFromFile(Context context, String filename) {
        String json;
        try {
            InputStream is = context.getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    public static Bitmap bitmapForPokemon(Context context, int pokemonId) {
        return BitmapFactory.decodeResource(context.getResources(), resourceIdForPokemon(context, pokemonId));
    }

    public static int resourceIdForPokemon(Context context, int pokemonId) {
        return context.getResources().getIdentifier("p" + pokemonId, "drawable", context.getPackageName());
    }

    public static String formatPokemonName(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    public static double getIvRatio(int attack, int defense, int stamina) {
        return (attack + defense + stamina) / 45.0;
    }

    public static void relog(Context context) {
        SharedPreferences.Editor editor = context.getSharedPreferences(MainActivity.preferencesKey, Context.MODE_PRIVATE).edit();
        editor.putBoolean("login", false);
        editor.apply();
        Intent i = new Intent(context, LoginActivity.class);
        context.startActivity(i);
    }
}
