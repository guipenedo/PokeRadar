package com.guipenedo.pokeradar.settings;

import android.content.res.Configuration;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.guipenedo.pokeradar.Utils;

public class PokemonFilterSettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(this);
        PreferenceCategory category = new PreferenceCategory(this);
        category.setTitle("Filter pokemons");
        screen.addPreference(category);
        try {
            JSONArray pokemonList = new JSONArray(Utils.loadJSONFromFile(this, "pokemon.json"));
            for (int i = 0; i < pokemonList.length(); i++){
                JSONObject pokemon = pokemonList.getJSONObject(i);
                CheckBoxPreference checkBox = new CheckBoxPreference(this);
                checkBox.setTitle(pokemon.getString("Name"));
                checkBox.setIcon(new BitmapDrawable(getResources(), Utils.bitmapForPokemon(this, Integer.parseInt(pokemon.getString("Number")))));
                checkBox.setDefaultValue(true);
                checkBox.setSummary("Show " + pokemon.getString("Name"));
                checkBox.setKey("pref_key_show_pokemon_" + Integer.parseInt(pokemon.getString("Number")));
                category.addPreference(checkBox);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        setPreferenceScreen(screen);
    }


    private AppCompatDelegate mDelegate;

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getDelegate().onPostCreate(savedInstanceState);
    }

    public ActionBar getSupportActionBar() {
        return getDelegate().getSupportActionBar();
    }

    public void setSupportActionBar(@Nullable Toolbar toolbar) {
        getDelegate().setSupportActionBar(toolbar);
    }

    @Override
    public MenuInflater getMenuInflater() {
        return getDelegate().getMenuInflater();
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        getDelegate().setContentView(layoutResID);
    }

    @Override
    public void setContentView(View view) {
        getDelegate().setContentView(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().setContentView(view, params);
    }

    @Override
    public void addContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().addContentView(view, params);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        getDelegate().onPostResume();
    }

    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);
        getDelegate().setTitle(title);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getDelegate().onConfigurationChanged(newConfig);
    }

    @Override
    protected void onStop() {
        super.onStop();
        getDelegate().onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getDelegate().onDestroy();
    }

    public void invalidateOptionsMenu() {
        getDelegate().invalidateOptionsMenu();
    }

    private AppCompatDelegate getDelegate() {
        if (mDelegate == null) {
            mDelegate = AppCompatDelegate.create(this, null);
        }
        return mDelegate;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(PokemonFilterSettingsActivity.this);
        }
        return (super.onOptionsItemSelected(menuItem));
    }
}