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

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class ScanSettings {
    public final String username, password;
    public List<LatLng> locations;
    public boolean gyms = true, pokemon = true, pokestops = true, spawnpoints = true;

    public ScanSettings(String username, String password, List<LatLng> locations) {
        this.username = username;
        this.password = password;
        this.locations = locations;
    }
}