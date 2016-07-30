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

import com.guipenedo.pokeradar.activities.MapsActivity;
import com.guipenedo.pokeradar.module.MapWrapper;
import com.guipenedo.pokeradar.module.PGym;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.gym.Gym;
import com.pokegoapi.api.map.Map;
import com.pokegoapi.api.map.MapObjects;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;
import com.pokegoapi.auth.PtcCredentialProvider;

import java.util.HashSet;
import java.util.Iterator;

import POGOProtos.Map.Fort.FortDataOuterClass;
import POGOProtos.Map.Pokemon.MapPokemonOuterClass;
import POGOProtos.Map.Pokemon.WildPokemonOuterClass;
import okhttp3.OkHttpClient;

public class ScanTask extends AsyncTask<Void, MapWrapper, Exception> {

    ScanUpdateCallback updateCallback;
    ScanSettings settings;
    ScanCompleteCallback completeCallback;

    public ScanTask(ScanSettings settings, ScanUpdateCallback updateCallback, ScanCompleteCallback completeCallback) {
        this.settings = settings;
        this.updateCallback = updateCallback;
        this.completeCallback = completeCallback;
    }

    int pos = 0;

    @Override
    protected Exception doInBackground(Void... voids) {
        OkHttpClient httpClient = MapsActivity.getHttp();
        PokemonGo go;
        try {
            go = new PokemonGo(new PtcCredentialProvider(httpClient, settings.username,
                    settings.password), httpClient);
        } catch (Exception e) {
            return e;
        }

        while (pos < settings.locations.size()) {
            if (isCancelled()) return null;
            go.setLatitude(settings.locations.get(pos).latitude);
            go.setLongitude(settings.locations.get(pos).longitude);
            pos++;
            try {
                Map map = go.getMap();
                MapObjects objects = map.getMapObjects();
                MapWrapper mapWrapper = new MapWrapper();
                if (settings.pokemon) {
                    HashSet<CatchablePokemon> catchablePokemons = new HashSet<>();
                    Iterator var3 = objects.getCatchablePokemons().iterator();

                    while(var3.hasNext()) {
                        MapPokemonOuterClass.MapPokemon wildPokemon = (MapPokemonOuterClass.MapPokemon)var3.next();
                        catchablePokemons.add(new CatchablePokemon(go, wildPokemon));
                    }

                    var3 = objects.getWildPokemons().iterator();

                    while(var3.hasNext()) {
                        WildPokemonOuterClass.WildPokemon wildPokemon1 = (WildPokemonOuterClass.WildPokemon)var3.next();
                        catchablePokemons.add(new CatchablePokemon(go, wildPokemon1));
                    }

                    mapWrapper.getPokemon().addAll(catchablePokemons);
                }
                if (settings.pokestops)
                    mapWrapper.getPokestops().addAll(objects.getPokestops());
                if (settings.spawnpoints) {
                    mapWrapper.getSpawnpoints().addAll(objects.getDecimatedSpawnPoints());
                    mapWrapper.getSpawnpoints().addAll(objects.getSpawnPoints());
                }
                if (settings.gyms)
                    for (FortDataOuterClass.FortData fortdata : objects.getGyms()) {
                        Thread.sleep(300);
                        mapWrapper.getGyms().add(new PGym(new Gym(go, fortdata)));
                    }
                publishProgress(mapWrapper);

                Thread.sleep(settings.delay);
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
    protected void onPostExecute(Exception result) {
        completeCallback.scanComplete(result);
    }
}
