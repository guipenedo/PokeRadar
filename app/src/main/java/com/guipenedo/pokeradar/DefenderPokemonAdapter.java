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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import POGOProtos.Data.PokemonDataOuterClass;

public class DefenderPokemonAdapter extends ArrayAdapter<PokemonDataOuterClass.PokemonData> {

    public DefenderPokemonAdapter(Context context, ArrayList<PokemonDataOuterClass.PokemonData> data) {
        super(context, 0, data);
    }

    @Override
    public View getView(int position, View convertView, final ViewGroup parent) {
        if (convertView == null)
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.defenderpokemon_listview_item, parent, false);
        PokemonDataOuterClass.PokemonData item = getItem(position);

        ImageView pokemonImage = (ImageView) convertView.findViewById(R.id.pokemonIcon);
        pokemonImage.setBackgroundResource(Utils.resourceIdForPokemon(convertView.getContext(), item.getPokemonId().getNumber()));

        TextView pokemonName = (TextView) convertView.findViewById(R.id.pokemonName);
        pokemonName.setText(Utils.formatPokemonName(item.getPokemonId().toString()));

        TextView pokemonCp = (TextView) convertView.findViewById(R.id.pokemonCp);
        pokemonCp.setText("CP: " + item.getCp());

        TextView attack = (TextView) convertView.findViewById(R.id.pokemonattack);
        attack.setText("Attack: " + item.getIndividualAttack());

        TextView stamina = (TextView) convertView.findViewById(R.id.pokemonstamina);
        stamina.setText("Stamina: " + item.getIndividualStamina());
        return convertView;
    }
}
