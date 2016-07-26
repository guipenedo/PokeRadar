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
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.guipenedo.pokeradar.DefenderPokemonAdapter;
import com.guipenedo.pokeradar.R;
import com.guipenedo.pokeradar.module.PGym;
import com.guipenedo.pokeradar.module.Team;

import java.util.ArrayList;

public class GymDetailsActivity extends AppCompatActivity {
    PGym gym;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gym_details);
        Intent i = getIntent();
        gym = i.getParcelableExtra("gymDetails");

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Team team = Team.fromTeamColor(gym.getTeam());

        ImageView imageView = (ImageView) findViewById(R.id.teamImage);
        imageView.setBackgroundResource(team.getImageResId(this));

        TextView teamName = (TextView) findViewById(R.id.teamName);
        teamName.setText(team.getName());

        TextView points = (TextView) findViewById(R.id.gymPoints);
        points.setText(String.format(getString(R.string.gym_details_points), gym.getPoints()));

        ListView listView = (ListView) findViewById(R.id.defenderList);
        listView.setHeaderDividersEnabled(true);
        listView.setAdapter(new DefenderPokemonAdapter(this, new ArrayList<>(gym.getDefendingPokemon())));

        /*
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                Intent intent = new Intent(getBaseContext(), PokemonDetailsActivity.class);
                intent.putExtra("id", gym.getDefendingPokemon().get(position).getPokemonId().getNumber());
                startActivity(intent);
            }
        });*/
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
