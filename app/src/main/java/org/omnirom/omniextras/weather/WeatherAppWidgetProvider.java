/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.omnirom.omniextras.weather;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import org.omnirom.omniextras.R;
import org.omnirom.omniextras.alarmclock.CustomAnalogAppWidgetConfigure;
import org.omnirom.omniextras.alarmclock.CustomAppWidgetProvider;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class WeatherAppWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "WeatherAppWidgetProvider";
    public static final boolean LOGGING = true;
    private static final String REFRESH_BROADCAST = "org.omnirom.omniextras.WEATHER_REFRESH";

    public static class WeatherUpdateService extends Service {
        private final BroadcastReceiver mWeatherChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (LOGGING) {
                    Log.i(TAG, "WeatherUpdateService:onReceive: " + action);
                }
                if (action.equals("org.omnirom.omnijaws.WEATHER_UPDATE")) {
                    updateAllWeather(context);
                }
                if (action.equals(REFRESH_BROADCAST)) {
                    OmniJawsClient weatherClient = new OmniJawsClient(context);
                    weatherClient.updateWeather(true);
                }
            }
        };

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public void onCreate() {
            super.onCreate();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("org.omnirom.omnijaws.WEATHER_UPDATE");
            intentFilter.addAction(REFRESH_BROADCAST);
            registerReceiver(mWeatherChangedReceiver, intentFilter);

            OmniJawsClient weatherClient = new OmniJawsClient(this);
            weatherClient.updateWeather(true);

            if (LOGGING) {
                Log.i(TAG, "WeatherUpdateService:onCreate");
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            unregisterReceiver(mWeatherChangedReceiver);
            if (LOGGING) {
                Log.i(TAG, "WeatherUpdateService:onDestroy");
            }
        }
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        if (LOGGING) {
            Log.i(TAG, "onEnabled");
        }
        context.startService(new Intent(context, WeatherUpdateService.class));
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        if (LOGGING) {
            Log.i(TAG, "onDisabled");
        }
        context.stopService(new Intent(context, WeatherUpdateService.class));
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        for (int id : appWidgetIds) {
            if (LOGGING) {
                Log.i(TAG, "onDeleted: " + id);
            }
            WeatherAppWidgetConfigure.clearPrefs(context, id);
        }
    }

    @Override
    public void onRestored(Context context, int[] oldWidgetIds, int[] newWidgetIds) {
        int i = 0;
        for (int oldWidgetId : oldWidgetIds) {
            if (LOGGING) {
                Log.i(TAG, "onRestored " + oldWidgetId + " " + newWidgetIds[i]);
            }
            WeatherAppWidgetConfigure.remapPrefs(context, oldWidgetId, newWidgetIds[i]);
            i++;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        context.startService(new Intent(context, WeatherUpdateService.class));
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, Bundle newOptions) {
        if (LOGGING) {
            Log.i(TAG, "onAppWidgetOptionsChanged");
        }
        updateWeather(context, appWidgetManager, appWidgetId);
    }

    public static void updateAfterConfigure(Context context, int appWidgetId) {
        if (LOGGING) {
            Log.i(TAG, "updateAfterConfigure");
        }
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        updateWeather(context, appWidgetManager, appWidgetId);
    }

    public static void updateAllWeather(Context context) {
        if (LOGGING) {
            Log.i(TAG, "updateAllWeather at = " + new Date());
        }
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        if (appWidgetManager != null) {
            ComponentName componentName = new ComponentName(context, WeatherAppWidgetProvider.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);
            for (int appWidgetId : appWidgetIds) {
                updateWeather(context, appWidgetManager, appWidgetId);
            }
        }
    }

    private static void updateWeather(
            Context context, AppWidgetManager appWidgetManager, int appWidgetId) {

        if (LOGGING) {
            Log.i(TAG, "updateWeather " + appWidgetId);
        }
        OmniJawsClient weatherClient = new OmniJawsClient(context);
        weatherClient.queryWeather();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String iconPack = prefs.getString(WeatherAppWidgetConfigure.KEY_ICON_PACK + "_" + appWidgetId, "");
        if (!TextUtils.isEmpty(iconPack)) {
            weatherClient.loadIconPackage(iconPack);
        }
        OmniJawsClient.WeatherInfo weatherData = weatherClient.getWeatherInfo();
        if (weatherData == null) {
            Log.i(TAG, "updateWeather weatherData == null");
            return;
        }
        Log.i(TAG, "updateWeather " + weatherData.toString());


        RemoteViews widget = new RemoteViews(context.getPackageName(), R.layout.weather_appwidget);
        Bundle newOptions = appWidgetManager.getAppWidgetOptions(appWidgetId);
        int minHeight = context.getResources().getDimensionPixelSize(R.dimen.weather_widget_height);
        int minWidth = context.getResources().getDimensionPixelSize(R.dimen.weather_widget_width);

        int currentHeight = minHeight;
        int currentWidth = minWidth;

        if (newOptions != null) {
            currentHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minHeight);
            currentWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minWidth);
        }
        boolean showDays = currentHeight > minHeight ? true : false;
        boolean showLocalDetails = currentHeight > minHeight ? true : false;

        Intent refreshIntent = new Intent();
        refreshIntent.setAction(REFRESH_BROADCAST);
        widget.setOnClickPendingIntent(R.id.refresh,
                PendingIntent.getBroadcast(context, 0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        Long timeStamp = weatherData.timeStamp;
        String format = DateFormat.is24HourFormat(context) ? "HH:mm" : "hh:mm a";
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        widget.setTextViewText(R.id.current_weather_timestamp, sdf.format(timeStamp));

        sdf = new SimpleDateFormat("EE");
        Calendar cal = Calendar.getInstance();
        String dayShort = sdf.format(new Date(cal.getTimeInMillis()));

        Drawable d = weatherClient.getWeatherConditionImage(weatherData.forecasts.get(0).conditionCode);
        BitmapDrawable bd = overlay(context.getResources(), d, weatherData.forecasts.get(0).low, weatherData.forecasts.get(0).high,
                weatherData.tempUnits);
        widget.setImageViewBitmap(R.id.forecast_image_0, bd.getBitmap());
        widget.setTextViewText(R.id.forecast_text_0, dayShort);
        widget.setViewVisibility(R.id.forecast_text_0, showDays ? View.VISIBLE : View.GONE);

        cal.add(Calendar.DATE, 1);
        dayShort = sdf.format(new Date(cal.getTimeInMillis()));

        d = weatherClient.getWeatherConditionImage(weatherData.forecasts.get(1).conditionCode);
        bd = overlay(context.getResources(), d, weatherData.forecasts.get(1).low, weatherData.forecasts.get(1).high,
                weatherData.tempUnits);
        widget.setImageViewBitmap(R.id.forecast_image_1, bd.getBitmap());
        widget.setTextViewText(R.id.forecast_text_1, dayShort);
        widget.setViewVisibility(R.id.forecast_text_1, showDays ? View.VISIBLE : View.GONE);

        cal.add(Calendar.DATE, 1);
        dayShort = sdf.format(new Date(cal.getTimeInMillis()));

        d = weatherClient.getWeatherConditionImage(weatherData.forecasts.get(2).conditionCode);
        bd = overlay(context.getResources(), d, weatherData.forecasts.get(2).low, weatherData.forecasts.get(2).high,
                weatherData.tempUnits);
        widget.setImageViewBitmap(R.id.forecast_image_2, bd.getBitmap());
        widget.setTextViewText(R.id.forecast_text_2, dayShort);
        widget.setViewVisibility(R.id.forecast_text_2, showDays ? View.VISIBLE : View.GONE);

        cal.add(Calendar.DATE, 1);
        dayShort = sdf.format(new Date(cal.getTimeInMillis()));

        d = weatherClient.getWeatherConditionImage(weatherData.forecasts.get(3).conditionCode);
        bd = overlay(context.getResources(), d, weatherData.forecasts.get(3).low, weatherData.forecasts.get(3).high,
                weatherData.tempUnits);
        widget.setImageViewBitmap(R.id.forecast_image_3, bd.getBitmap());
        widget.setTextViewText(R.id.forecast_text_3, dayShort);
        widget.setViewVisibility(R.id.forecast_text_3, showDays ? View.VISIBLE : View.GONE);

        cal.add(Calendar.DATE, 1);
        dayShort = sdf.format(new Date(cal.getTimeInMillis()));

        d = weatherClient.getWeatherConditionImage(weatherData.forecasts.get(4).conditionCode);
        bd = overlay(context.getResources(), d, weatherData.forecasts.get(4).low, weatherData.forecasts.get(4).high,
                weatherData.tempUnits);
        widget.setImageViewBitmap(R.id.forecast_image_4, bd.getBitmap());
        widget.setTextViewText(R.id.forecast_text_4, dayShort);
        widget.setViewVisibility(R.id.forecast_text_4, showDays ? View.VISIBLE : View.GONE);

        d = weatherClient.getWeatherConditionImage(weatherData.conditionCode);
        bd = overlay(context.getResources(), d, weatherData.temp, null, weatherData.tempUnits);
        widget.setImageViewBitmap(R.id.current_image, bd.getBitmap());
        widget.setTextViewText(R.id.current_text, context.getResources().getText(R.string.omnijaws_current_text));
        widget.setViewVisibility(R.id.current_text, showDays ? View.VISIBLE : View.GONE);

        widget.setViewVisibility(R.id.current_weather_line, showLocalDetails ? View.VISIBLE : View.GONE);
        widget.setTextViewText(R.id.current_weather_city, weatherData.city);
        widget.setTextViewText(R.id.current_weather_data, weatherData.windSpeed + " " + weatherData.windUnits + " " + weatherData.windDirection + " - " +
                weatherData.humidity);

        appWidgetManager.updateAppWidget(appWidgetId, widget);
    }

    private static BitmapDrawable overlay(Resources resources, Drawable image, String min, String max, String tempUnits) {
        final Canvas canvas = new Canvas();
        canvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.ANTI_ALIAS_FLAG,
                Paint.FILTER_BITMAP_FLAG));
        final float density = resources.getDisplayMetrics().density;
        final int footerHeight = Math.round(18 * density);
        final int imageWidth = image.getIntrinsicWidth();
        final int imageHeight = image.getIntrinsicHeight();
        final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        Typeface font = Typeface.create("sans-serif-condensed", Typeface.NORMAL);
        textPaint.setTypeface(font);
        textPaint.setColor(resources.getColor(R.color.omni_jaws_detailed_temperature_color));
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setShadowLayer(5, 0, 2, Color.BLACK);
        final int textSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, resources.getDisplayMetrics());
        textPaint.setTextSize(textSize);
        final int height = imageHeight + footerHeight;
        final int width = imageWidth;

        final Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bmp);
        image.setBounds(0, 0, imageWidth, imageHeight);
        image.draw(canvas);

        String str = null;
        if (max != null) {
            str = min + "/" + max + tempUnits;
        } else {
            str = min + tempUnits;
        }
        Rect bounds = new Rect();
        textPaint.getTextBounds(str, 0, str.length(), bounds);
        canvas.drawText(str, width / 2 - bounds.width() / 2, height - textSize / 2, textPaint);

        return new BitmapDrawable(resources, bmp);
    }
}
