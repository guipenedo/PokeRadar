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

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.guipenedo.pokeradar.R;
import com.guipenedo.pokeradar.module.PGym;

import POGOProtos.Data.PokemonDataOuterClass;

public class GymDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gym_details);
        Intent i = getIntent();
        PGym gymDetails = i.getParcelableExtra("gymDetails");
        System.out.println("gym points: " + gymDetails.getPoints());
        for (PokemonDataOuterClass.PokemonData data : gymDetails.getDefendingPokemon()){
            System.out.println("pokemon: " + data.getPokemonId().toString());
        }
    }

}
