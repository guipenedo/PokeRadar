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

package com.guipenedo.pokeradar.module;

import android.os.Parcel;
import android.os.Parcelable;

import com.pokegoapi.api.gym.Gym;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;

import java.util.ArrayList;
import java.util.List;

import POGOProtos.Data.PokemonDataOuterClass;
import POGOProtos.Enums.TeamColorOuterClass;

public class PGym extends PMarker implements Parcelable {
    private long points;
    private TeamColorOuterClass.TeamColor team;
    private List<PokemonDataOuterClass.PokemonData> defendingPokemon = new ArrayList<>();

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(points);
        parcel.writeInt(team.getNumber());
        parcel.writeList(defendingPokemon);
    }

    public PGym (Parcel in){
        points = in.readLong();
        team = TeamColorOuterClass.TeamColor.forNumber(in.readInt());
        in.readList(defendingPokemon, null);
    }

    public static final Creator<PGym> CREATOR = new Creator<PGym>() {
        @Override
        public PGym createFromParcel(Parcel in) {
            return new PGym(in);
        }

        @Override
        public PGym[] newArray(int size) {
            return new PGym[size];
        }
    };

    public long getPoints() {
        return points;
    }

    public TeamColorOuterClass.TeamColor getTeam() {
        return team;
    }

    public List<PokemonDataOuterClass.PokemonData> getDefendingPokemon() {
        return defendingPokemon;
    }

    public PGym(long points, TeamColorOuterClass.TeamColor team, List<PokemonDataOuterClass.PokemonData> defendingPokemon) {
        super(MarkerType.GYM);
        this.points = points;
        this.team = team;
        this.defendingPokemon = defendingPokemon;
    }

    public PGym(Gym gym) throws LoginFailedException, RemoteServerException {
        this(gym.getPoints(), gym.getOwnedByTeam(), gym.getDefendingPokemon());
        setLatitude(gym.getLatitude());
        setLongitude(gym.getLongitude());
        setId(gym.getId());
    }

    @Override
    public int describeContents() {
        return 0;
    }

}
