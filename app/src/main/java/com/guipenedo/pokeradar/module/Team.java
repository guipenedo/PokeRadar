package com.guipenedo.pokeradar.module;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import POGOProtos.Enums.TeamColorOuterClass;

public enum Team {
    YELLOW(Color.YELLOW, "Team Instinct"), BLUE(Color.BLUE, "Team Mystic"), RED(Color.RED, "Team Valor"), NONE(Color.YELLOW, "No team");

    private int color;
    private String name;

    Team(int color, String name) {
        this.color = color;
        this.name = name;
    }

    public static Team fromTeamColor(TeamColorOuterClass.TeamColor color) {
        switch (color) {
            case YELLOW:
                return YELLOW;
            case BLUE:
                return BLUE;
            case RED:
                return RED;
            default:
                return NONE;
        }
    }

    public int getColor() {
        return color;
    }

    public String getName() {
        return name;
    }

    public BitmapDescriptor getImage(Context context) {
        int resourceID = context.getResources().getIdentifier(name.toLowerCase().replaceAll(" ", "_"), "drawable", context.getPackageName());
        return BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(context.getResources(), resourceID));
    }
}
