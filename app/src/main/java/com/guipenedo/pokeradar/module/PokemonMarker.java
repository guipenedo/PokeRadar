package com.guipenedo.pokeradar.module;

import POGOProtos.Enums.TeamColorOuterClass;

public class PokemonMarker {
    public String text;
    public long timestamp;
    public TeamColorOuterClass.TeamColor team;
    public long prestige;
    public MarkerType type;

    public PokemonMarker(MarkerType type) {
        this.type = type;
    }

    public enum MarkerType {
        CENTER, POKESTOP, LUREDPOKESTOP, POKEMON, GYM;
    }
}