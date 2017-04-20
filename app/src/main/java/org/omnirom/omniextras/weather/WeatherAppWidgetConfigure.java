/*
 *  Copyright (C) 2016 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.omnirom.omniextras.weather;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.View;

import org.omnirom.omniextras.R;

import java.util.ArrayList;
import java.util.List;

public class WeatherAppWidgetConfigure extends PreferenceActivity {

    public static final String KEY_ICON_PACK = "weather_icon_pack";
    private static final String DEFAULT_WEATHER_ICON_PACKAGE = "org.omnirom.omniextras";
    private static final String CHRONUS_ICON_PACK_INTENT = "com.dvtonder.chronus.ICON_PACK";
    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If this activity was started with an intent without an app widget ID,
        // finish with an error.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        addPreferencesFromResource(R.xml.weather_appwidget_configure);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        final ListPreference iconPack = (ListPreference) findPreference(KEY_ICON_PACK) ;

        String settingHeaderPackage = DEFAULT_WEATHER_ICON_PACKAGE;

        List<String> entries = new ArrayList<String>();
        List<String> values = new ArrayList<String>();
        getAvailableWeatherIconPacks(entries, values);
        iconPack.setEntries(entries.toArray(new String[entries.size()]));
        iconPack.setEntryValues(values.toArray(new String[values.size()]));

        int valueIndex = iconPack.findIndexOfValue(settingHeaderPackage);
        if (valueIndex == -1) {
            // no longer found
            settingHeaderPackage = DEFAULT_WEATHER_ICON_PACKAGE;
            valueIndex = iconPack.findIndexOfValue(settingHeaderPackage);
        }
        iconPack.setValueIndex(valueIndex >= 0 ? valueIndex : 0);
        iconPack.setSummary(iconPack.getEntry());
        iconPack.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                prefs.edit().putString(KEY_ICON_PACK + "_" + String.valueOf(mAppWidgetId), (String)newValue).commit();
                int valueIndex = iconPack.findIndexOfValue((String)newValue);
                iconPack.setSummary(iconPack.getEntries()[valueIndex]);
                return false;
            }
        });
    }

    public void handleOkClick(View v) {
        WeatherAppWidgetProvider.updateAfterConfigure(this, mAppWidgetId);
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }

    public static void clearPrefs(Context context, int id) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().remove(KEY_ICON_PACK + "_" + id).commit();
    }

    public static void remapPrefs(Context context, int oldId, int newId) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String oldValue = prefs.getString(KEY_ICON_PACK + "_" + oldId, "");
        prefs.edit().putString(KEY_ICON_PACK + "_" + newId, oldValue).commit();
        prefs.edit().remove(KEY_ICON_PACK + "_" + oldId).commit();
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return false;
    }

    private void getAvailableWeatherIconPacks(List<String> entries, List<String> values) {
        Intent i = new Intent();
        PackageManager packageManager = getPackageManager();
        i.setAction("org.omnirom.WeatherIconPack");
        for (ResolveInfo r : packageManager.queryIntentActivities(i, 0)) {
            String packageName = r.activityInfo.packageName;
            if (values.contains(r.activityInfo.name)) {
                continue;
            }
            if (packageName.equals(DEFAULT_WEATHER_ICON_PACKAGE)) {
                values.add(0, r.activityInfo.name);
            } else {
                values.add(r.activityInfo.name);
            }
            String label = r.activityInfo.loadLabel(getPackageManager()).toString();
            if (label == null) {
                label = r.activityInfo.packageName;
            }
            if (packageName.equals(DEFAULT_WEATHER_ICON_PACKAGE)) {
                entries.add(0, label);
            } else {
                entries.add(label);
            }
        }
        i = new Intent(Intent.ACTION_MAIN);
        i.addCategory(CHRONUS_ICON_PACK_INTENT);
        for (ResolveInfo r : packageManager.queryIntentActivities(i, 0)) {
            String packageName = r.activityInfo.packageName;
            if (values.contains(packageName + ".weather")) {
                continue;
            }
            values.add(packageName + ".weather");
            String label = r.activityInfo.loadLabel(getPackageManager()).toString();
            if (label == null) {
                label = r.activityInfo.packageName;
            }
            entries.add(label);
        }
    }
}
