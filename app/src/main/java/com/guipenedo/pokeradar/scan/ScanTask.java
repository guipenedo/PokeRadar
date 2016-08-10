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

package com.guipenedo.pokeradar.scan;

import android.os.AsyncTask;

import com.guipenedo.pokeradar.activities.MainActivity;
import com.guipenedo.pokeradar.module.MapWrapper;
import com.guipenedo.pokeradar.module.PGym;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.gym.Gym;
import com.pokegoapi.api.map.Map;
import com.pokegoapi.api.map.MapObjects;

import java.util.HashSet;

import POGOProtos.Map.Fort.FortDataOuterClass;

public class ScanTask extends AsyncTask<Void, MapWrapper, Void> {

    ScanUpdateCallback updateCallback;
    ScanSettings settings;
    ScanCompleteCallback completeCallback;
    HashSet<String> ids = new HashSet<>();
    int pos = 0;

    public ScanTask(ScanSettings settings, ScanUpdateCallback updateCallback, ScanCompleteCallback completeCallback) {
        this.settings = settings;
        this.updateCallback = updateCallback;
        this.completeCallback = completeCallback;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        PokemonGo go = MainActivity.go;
        if (go == null)
            return null;
        long time = System.currentTimeMillis();

        while (pos < settings.locations.size()) {
            if (isCancelled()) return null;
            go.setLatitude(settings.locations.get(pos).latitude);
            go.setLongitude(settings.locations.get(pos).longitude);
            pos++;
            try {
                Map map = new Map(go);
                MapObjects objects = map.getMapObjects(9);
                MapWrapper mapWrapper = new MapWrapper();
                if (settings.pokemon)
                    mapWrapper.getPokemon().addAll(map.getCatchablePokemon());
                if (settings.pokestops)
                    mapWrapper.getPokestops().addAll(objects.getPokestops());
                if (settings.spawnpoints) {
                    mapWrapper.getSpawnpoints().addAll(map.getDecimatedSpawnPoints());
                    mapWrapper.getSpawnpoints().addAll(map.getSpawnPoints());
                }
                if (settings.gyms)
                    for (FortDataOuterClass.FortData fortdata : objects.getGyms()) {
                        if (!ids.contains(fortdata.getId())) {
                            Thread.sleep(350);
                            mapWrapper.getGyms().add(new PGym(new Gym(go, fortdata)));
                            ids.add(fortdata.getId());
                        }
                    }
                publishProgress(mapWrapper);

                if (objects.getPokestops().size() > 0) {
                    System.out.println("SUCCESSFUL REQUEST! TIME: " + (System.currentTimeMillis() - time) / 1000);
                    time = System.currentTimeMillis();
                }
                System.out.println("POKESTOP COUNT: " + objects.getPokestops().size());
                System.out.println("SLEEPING " + ((long) go.getSettings().getMapSettings().getMinRefresh()));
                //Thread.sleep((long) go.getSettings().getMapSettings().getMinRefresh());
                Thread.sleep(6200);
                //Thread.sleep(10000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(MapWrapper... objects) {
        if (objects.length < 1 || isCancelled()) return;

        MapWrapper map = objects[0];
        updateCallback.scanUpdate(map, (100 * pos) / settings.locations.size(), settings.locations.get(pos - 1));
    }

    @Override
    protected void onPostExecute(Void ee) {
        completeCallback.scanComplete();
    }
}
