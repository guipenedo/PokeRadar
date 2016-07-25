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

import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.map.MapObjects;
import com.pokegoapi.auth.PTCLogin;
import com.pokegoapi.exceptions.LoginFailedException;

import POGOProtos.Networking.Envelopes.RequestEnvelopeOuterClass;
import okhttp3.OkHttpClient;

public class ScanTask extends AsyncTask<Void, MapObjects, Boolean> {

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
    protected Boolean doInBackground(Void... voids) {
        OkHttpClient httpClient = new OkHttpClient();
        RequestEnvelopeOuterClass.RequestEnvelope.AuthInfo auth;
        try {
            auth = new PTCLogin(httpClient).login(settings.username, settings.password);
        } catch (LoginFailedException e) {
            return false;
        }

        while (pos < settings.locations.size()) {
            if (isCancelled()) return false;
            PokemonGo go = new PokemonGo(auth, httpClient);
            go.setLatitude(settings.locations.get(pos).latitude);
            go.setLongitude(settings.locations.get(pos).longitude);
            pos++;

            try {
                publishProgress(go.getMap().getMapObjects());
                Thread.sleep(settings.delay);
            } catch (Exception ignored) {
            }
        }
        return true;
    }

    @Override
    protected void onProgressUpdate(MapObjects... objects) {
        if (objects.length < 1 || isCancelled()) return;

        MapObjects object = objects[0];
        updateCallback.scanUpdate(object, (100 * pos) / settings.locations.size(), settings.locations.get(pos - 1));
    }

    @Override
    protected void onPostExecute(Boolean result) {
        completeCallback.scanComplete(result);
    }
}
