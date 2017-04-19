/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.omnirom.omniextras.alarmclock;

import android.app.AlarmManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.style.AbsoluteSizeSpan;
import android.widget.RemoteViews;

import org.omnirom.omniextras.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

public class WidgetUtils {
    static final String TAG = "WidgetUtils";

    // Decide if to show the list of world clock.
    // Check to see if the widget size is big enough, if it is return true.
    public static boolean showList(Context context, int id) {
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        if (widgetManager == null) {
            // no manager to make the calculation, show the list anyway
            return true;
        }
        Bundle options = widgetManager.getAppWidgetOptions(id);
        if (options == null) {
            // no data to make the calculation, show the list anyway
            return true;
        }
        Resources res = context.getResources();
        String whichHeight = res.getConfiguration().orientation ==
                Configuration.ORIENTATION_PORTRAIT
                ? AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT
                : AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT;
        int height = options.getInt(whichHeight);
        if (height == 0) {
            // no data to make the calculation, show the list anyway
            return true;
        }
        float density = res.getDisplayMetrics().density;
        // Estimate height of date text box
        float lblBox = 1.35f * res.getDimension(R.dimen.label_font_size);
        float neededSize = res.getDimension(R.dimen.digital_widget_list_min_fixed_height) +
                2 * lblBox +
                res.getDimension(R.dimen.digital_widget_list_min_scaled_height);
        return ((density * height) > neededSize);
    }

    /***
     * Set the format of the time on the clock accrding to the locale
     *
     * @param clock        - view to format
     * @param amPmFontSize - size of am/pm label, zero size means no am/om label
     * @param clockId      - id of TextClock view as defined in the clock's layout.
     */
    public static void setTimeFormat(RemoteViews clock, int amPmFontSize, int clockId, int secondsSize) {
        if (clock != null) {
            // Set the best format for 12 hours mode according to the locale
            clock.setCharSequence(clockId, "setFormat12Hour", get12ModeFormat(amPmFontSize, secondsSize));
            // Set the best format for 24 hours mode according to the locale
            clock.setCharSequence(clockId, "setFormat24Hour", get24ModeFormat(secondsSize));
        }
    }

    public static boolean isShowingAlarm(Context context, int id, boolean defaultValue) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(CustomAppWidgetConfigure.KEY_SHOW_ALARM + "_" + id, defaultValue);
    }

    public static boolean isShowingDate(Context context, int id, boolean defaultValue) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(CustomAppWidgetConfigure.KEY_SHOW_DATE + "_" + id, defaultValue);
    }

    public static boolean isClockShadow(Context context, int id) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(CustomAppWidgetConfigure.KEY_CLOCK_SHADOW + "_" + id, true);
    }

    public static Bitmap createTextBitmap(final String text, final Typeface typeface, final float textSizePixels, final int textColor, boolean shadow, float letterSpacing) {
        final TextPaint textPaint = new TextPaint();
        textPaint.setTypeface(typeface);
        textPaint.setTextSize(textSizePixels);
        textPaint.setAntiAlias(true);
        textPaint.setSubpixelText(true);
        textPaint.setColor(textColor);
        textPaint.setTextAlign(Paint.Align.CENTER);
        if (shadow) {
            textPaint.setShadowLayer(5.0f, 0.0f, 0.0f, Color.BLACK);
        }
        if (letterSpacing != -1) {
            textPaint.setLetterSpacing(letterSpacing);
        }
        int textHeight = (int) (textPaint.descent() - textPaint.ascent());
        int textOffset = (int) ((textHeight / 2) - textPaint.descent());
        Bitmap myBitmap = Bitmap.createBitmap((int) textPaint.measureText(text), (int) textSizePixels, Bitmap.Config.ARGB_8888);
        Canvas myCanvas = new Canvas(myBitmap);
        myCanvas.drawText(text, myBitmap.getWidth() / 2, myBitmap.getHeight() / 2 + textOffset, textPaint);
        return myBitmap;
    }

    public static Bitmap createTimeBitmap(String timeFormat, final Typeface typeface, final float textSizePixels, final int textColor,
                                          boolean shadow, float letterSpacing, boolean showAmPm) {
        if (showAmPm) {
            // remove any a
            timeFormat = timeFormat.replaceAll("a", "").trim();
        }
        SimpleDateFormat sdf = new SimpleDateFormat(timeFormat, Locale.getDefault());
        String currTime = sdf.format(new Date());
        String amPmString = "";

        if (showAmPm) {
            SimpleDateFormat amPmFormat = new SimpleDateFormat("a", Locale.getDefault());
            amPmString = amPmFormat.format(new Date());
        }
        final TextPaint textPaint = new TextPaint();
        textPaint.setTypeface(typeface);
        textPaint.setTextSize(textSizePixels);
        textPaint.setAntiAlias(true);
        textPaint.setSubpixelText(true);
        textPaint.setColor(textColor);
        textPaint.setTextAlign(Paint.Align.LEFT);
        if (shadow) {
            textPaint.setShadowLayer(5.0f, 0.0f, 0.0f, Color.BLACK);
        }
        if (letterSpacing != -1) {
            textPaint.setLetterSpacing(letterSpacing);
        }

        final TextPaint smallTextPaint = new TextPaint();
        smallTextPaint.setTypeface(typeface);
        smallTextPaint.setTextSize(textSizePixels / 3);
        smallTextPaint.setAntiAlias(true);
        smallTextPaint.setSubpixelText(true);
        smallTextPaint.setColor(textColor);
        smallTextPaint.setTextAlign(Paint.Align.LEFT);
        if (shadow) {
            smallTextPaint.setShadowLayer(5.0f, 0.0f, 0.0f, Color.BLACK);
        }
        if (letterSpacing != -1) {
            smallTextPaint.setLetterSpacing(letterSpacing);
        }

        int textHeight = (int) (textPaint.descent() - textPaint.ascent());
        int textOffset = (int) ((textHeight / 2) - textPaint.descent());
        float timeStringSize = textPaint.measureText(currTime);
        float timeStringSizeTotal = timeStringSize;
        float amPmStringSize = smallTextPaint.measureText(amPmString);
        int totalWidth = (int) (timeStringSizeTotal + amPmStringSize);
        int startOffset = (int) (timeStringSizeTotal - timeStringSize);
        Bitmap myBitmap = Bitmap.createBitmap(totalWidth, (int) textSizePixels, Bitmap.Config.ARGB_8888);
        Canvas myCanvas = new Canvas(myBitmap);
        myCanvas.drawText(currTime, startOffset, myBitmap.getHeight() / 2 + textOffset, textPaint);

        if (showAmPm) {
            myCanvas.drawText(amPmString, startOffset + timeStringSize, myBitmap.getHeight() / 2 + textOffset, smallTextPaint);
        }
        return myBitmap;
    }

    public static Typeface getClockFont(Context context, int id) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String font = prefs.getString(CustomAppWidgetConfigure.KEY_CLOCK_FONT + "_" + id, null);
        if (font != null) {
            try {
                return Typeface.createFromFile(font);
            } catch (Exception e) {
            }
        }
        return Typeface.create("sans-serif-light", Typeface.NORMAL);
    }

    public static int getClockColor(Context context, int id) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(CustomAppWidgetConfigure.KEY_CLOCK_COLOR + "_" + id, Color.WHITE);
    }

    public static Bitmap createAnalogClockBitmap(Context context, boolean showAlarm, boolean showDate) {
        Resources r = context.getResources();

        Calendar calendar = new GregorianCalendar();
        float hours = calendar.get(Calendar.HOUR_OF_DAY);
        float minutes = calendar.get(Calendar.MINUTE);
        hours = hours + minutes / 60.0f;

        Paint circlePaint = new Paint();
        circlePaint.setAntiAlias(true);
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setColor(r.getColor(R.color.colorPrimary));

        Paint remaingCirclePaint = new Paint();
        remaingCirclePaint.setAntiAlias(true);
        remaingCirclePaint.setStyle(Paint.Style.STROKE);
        remaingCirclePaint.setColor(r.getColor(R.color.colorAccent));

        Paint bgPaint = new Paint();
        bgPaint.setAntiAlias(true);
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setColor(r.getColor(R.color.analog_clock_bg_color));

        Paint hourPaint = new Paint();
        hourPaint.setAntiAlias(true);
        hourPaint.setStyle(Paint.Style.STROKE);
        hourPaint.setColor(r.getColor(R.color.analog_clock_hour_hand_color));

        Paint minutePaint = new Paint();
        minutePaint.setAntiAlias(true);
        minutePaint.setStyle(Paint.Style.STROKE);
        minutePaint.setColor(r.getColor(R.color.analog_clock_minute_hand_color));

        Paint centerDotPaint = new Paint();
        centerDotPaint.setAntiAlias(true);
        centerDotPaint.setStyle(Paint.Style.FILL);
        centerDotPaint.setColor(r.getColor(R.color.colorAccent));

        Paint alarmPaint = new Paint();
        alarmPaint.setAntiAlias(true);
        alarmPaint.setStyle(Paint.Style.STROKE);
        alarmPaint.setColor(r.getColor(R.color.analog_clock_alarm_color));

        float textSizePixels = r.getDimension(R.dimen.analog_widget_font_size);
        Typeface typeface = Typeface.create("sans-serif-light", Typeface.NORMAL);

        final TextPaint textPaint = new TextPaint();
        textPaint.setTypeface(typeface);
        textPaint.setTextSize(textSizePixels);
        textPaint.setAntiAlias(true);
        textPaint.setSubpixelText(true);
        textPaint.setColor(r.getColor(android.R.color.white));
        textPaint.setTextAlign(Paint.Align.CENTER);

        final int circleStrokeWidth = r.getDimensionPixelSize(R.dimen.widget_clock_circle_size);
        final int handEndLength = r.getDimensionPixelSize(R.dimen.widget_clock_hand_end_length);
        final int width = r.getDimensionPixelSize(R.dimen.custom_analog_widget_size);

        circlePaint.setStrokeWidth(circleStrokeWidth);
        remaingCirclePaint.setStrokeWidth(circleStrokeWidth);
        hourPaint.setStrokeWidth(r.getDimensionPixelSize(R.dimen.widget_clock_hour_hand_width));
        minutePaint.setStrokeWidth(r.getDimensionPixelSize(R.dimen.widget_clock_minute_hand_width));
        alarmPaint.setStrokeWidth(circleStrokeWidth);

        Bitmap myBitmap = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(myBitmap);

        int availableWidth = width;
        int availableHeight = width;

        int x = availableWidth / 2;
        int y = availableHeight / 2;

        float radius = availableHeight / 2 - circleStrokeWidth;
        RectF arcRect = new RectF();
        arcRect.top = y - radius;
        arcRect.bottom = y + radius;
        arcRect.left = x - radius;
        arcRect.right = x + radius;
        canvas.drawArc(arcRect, 0, 360, true, bgPaint);
        canvas.drawArc(arcRect, 0, 360, false, circlePaint);
        float minuteStartAngle = minutes / 60.0f * 360.0f;
        if (minuteStartAngle < 90) {
            canvas.drawArc(arcRect, 270f + minuteStartAngle, 90f - minuteStartAngle, false, remaingCirclePaint);
            canvas.drawArc(arcRect, 0f, 270f, false, remaingCirclePaint);
        } else {
            canvas.drawArc(arcRect, minuteStartAngle - 90f, 360f - minuteStartAngle, false, remaingCirclePaint);
        }

        if (showDate) {
            CharSequence dateFormat = DateFormat.getBestDateTimePattern(Locale.getDefault(),
                    context.getString(R.string.abbrev_wday_month_day_no_year));
            SimpleDateFormat sdf = new SimpleDateFormat(dateFormat.toString(), Locale.getDefault());
            String currDate = sdf.format(new Date()).toUpperCase();

            Path path = new Path();
            RectF arcRectText = new RectF(arcRect);
            arcRectText.inset(2 * textSizePixels, 2 * textSizePixels);
            path.addArc(arcRectText, 180f, 180f);
            canvas.drawTextOnPath(currDate, path, 0, 0, textPaint);
        }
        if (showAlarm) {
            long nextAlamMilis = getNextAlarmInMillis(context);
            if (nextAlamMilis != -1) {
                Calendar alarmTime = Calendar.getInstance();
                alarmTime.setTimeInMillis(nextAlamMilis);
                Calendar twelveHoursFromNow = Calendar.getInstance();
                twelveHoursFromNow.setTime(new Date());
                twelveHoursFromNow.add(Calendar.HOUR_OF_DAY, 12);

                if (false && alarmTime.before(twelveHoursFromNow)) {
                    float hour = alarmTime.get(Calendar.HOUR_OF_DAY);
                    float minute = alarmTime.get(Calendar.MINUTE);
                    hour = hour + minute / 60.0f;
                    float angle = hour / 12.0f * 360.0f - 90;
                    RectF arcRectInset = new RectF(arcRect);
                    arcRectInset.inset(circleStrokeWidth, circleStrokeWidth);
                    canvas.drawArc(arcRectInset, angle - 4, 4, false, alarmPaint);
                }

                String alarmTimeString = getFormattedTime(context, alarmTime).toUpperCase();

                Path path = new Path();
                RectF arcRectText = new RectF(arcRect);
                arcRectText.inset(textSizePixels, textSizePixels);
                path.addArc(arcRectText, 180f, -180f);
                canvas.drawTextOnPath(alarmTimeString, path, 0, 0, textPaint);
            }
        }
        drawHand(canvas, hourPaint, x, y, radius * 0.70f, hours / 12.0f * 360.0f - 90, handEndLength);
        drawHand(canvas, minutePaint, x, y, radius + circleStrokeWidth / 2, minutes / 60.0f * 360.0f - 90, handEndLength);
        canvas.drawCircle(x, y, hourPaint.getStrokeWidth(), centerDotPaint);

        return myBitmap;
    }

    private static void drawHand(Canvas canvas, Paint mHandPaint, int x, int y, float length, float angle, int mHandEndLength) {
        canvas.save();
        canvas.rotate(angle, x, y);
        canvas.drawLine(x, y, x + length, y, mHandPaint);
        canvas.drawLine(x, y, x - mHandEndLength, y, mHandPaint);
        canvas.restore();
    }

    private static void drawTextOnCanvas(Canvas canvas, float xPos, float yPos, final String text,
                                         final Typeface typeface, final float textSizePixels, final int textColor,
                                         boolean shadow, float letterSpacing) {
        final TextPaint textPaint = new TextPaint();
        textPaint.setTypeface(typeface);
        textPaint.setTextSize(textSizePixels);
        textPaint.setAntiAlias(true);
        textPaint.setSubpixelText(true);
        textPaint.setColor(textColor);
        textPaint.setTextAlign(Paint.Align.CENTER);

        if (shadow) {
            textPaint.setShadowLayer(5.0f, 0.0f, 0.0f, Color.BLACK);
        }
        if (letterSpacing != -1) {
            textPaint.setLetterSpacing(letterSpacing);
        }

        int textHeight = (int) (textPaint.descent() - textPaint.ascent());
        int textOffset = (int) ((textHeight / 2) - textPaint.descent());
        canvas.drawText(text, xPos, yPos + textOffset, textPaint);
    }

    public static Bitmap createDataAlarmBitmap(final Context context, final Typeface typeface,
                                               final float textSizePixels, final int textColor,
                                               boolean shadow, float letterSpacing, boolean showDate,
                                               boolean showAlarm) {

        String nextAlarm = getNextAlarm(context);
        boolean hasAlarm = !TextUtils.isEmpty(nextAlarm);
        if (hasAlarm) {
            nextAlarm = nextAlarm.toUpperCase();
        }

        CharSequence dateFormat = DateFormat.getBestDateTimePattern(Locale.getDefault(),
                context.getString((showAlarm && hasAlarm) ? R.string.abbrev_wday_month_day_no_year :
                        R.string.full_wday_month_day_no_year));

        String currDate = "";

        if (showDate) {
            SimpleDateFormat sdf = new SimpleDateFormat(dateFormat.toString(), Locale.getDefault());
            currDate = sdf.format(new Date()).toUpperCase();
        }

        final TextPaint textPaint = new TextPaint();
        textPaint.setTypeface(typeface);
        textPaint.setTextSize(textSizePixels);
        if (letterSpacing != -1) {
            textPaint.setLetterSpacing(letterSpacing);
        }
        Bitmap b = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_alarm_small);

        float separatorWidth = textPaint.measureText(" ");

        float dateWidth = showDate ? textPaint.measureText(currDate) + 2 * separatorWidth : 0;
        float alarmWidth = (showAlarm && hasAlarm) ? textPaint.measureText(nextAlarm) + 3 * separatorWidth + b.getWidth() : 0;
        float totalWidth = dateWidth + alarmWidth;
        float totalHeight = Math.max(b.getHeight(), textSizePixels);

        if (totalWidth != 0 && totalHeight != 0) {
            Bitmap myBitmap = Bitmap.createBitmap((int) totalWidth, (int) totalHeight, Bitmap.Config.ARGB_8888);
            Canvas myCanvas = new Canvas(myBitmap);
            if (showDate) {
                drawTextOnCanvas(myCanvas, dateWidth / 2, myBitmap.getHeight() / 2, currDate, typeface, textSizePixels, textColor, shadow, letterSpacing);
            }

            if (hasAlarm && showAlarm) {
                Paint bitmapPaint = new Paint();
                ColorFilter filter = new PorterDuffColorFilter(textColor, PorterDuff.Mode.SRC_IN);
                bitmapPaint.setColorFilter(filter);
                myCanvas.drawBitmap(b, dateWidth + separatorWidth, 0, bitmapPaint);
                drawTextOnCanvas(myCanvas, dateWidth + 2 * separatorWidth + alarmWidth / 2,
                        myBitmap.getHeight() / 2, nextAlarm, typeface, textSizePixels, textColor, shadow,
                        letterSpacing);
            }
            return myBitmap;
        }
        return null;
    }

    /***
     * @param amPmFontSize - size of am/pm label (label removed is size is 0).
     * @return format string for 12 hours mode time
     */
    public static CharSequence get12ModeFormat(int amPmFontSize, int secondsSize) {
        String skeleton = secondsSize != 0 ? "hmsa" : "hma";
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton);
        // Remove the am/pm
        if (amPmFontSize <= 0) {
            pattern = pattern.replaceAll("a", "").trim();
        }
        // Replace spaces with "Hair Space"
        pattern = pattern.replaceAll(" ", "\u200A");

        // Build a spannable so that the seconds and am/pm will be formatted
        Spannable sp = new SpannableString(pattern);
        int amPmPos = pattern.indexOf('a');
        int secPos = pattern.indexOf('s');
        if (secPos == -1 && amPmPos == -1) {
            return pattern;
        }
        if (secPos != -1) {
            sp.setSpan(new AbsoluteSizeSpan(secondsSize), secPos - 1, secPos + 2,
                    Spannable.SPAN_POINT_MARK);
        }
        if (amPmPos != -1) {
            sp.setSpan(new AbsoluteSizeSpan(amPmFontSize), amPmPos, amPmPos + 1,
                    Spannable.SPAN_POINT_MARK);
        }
        return sp;
    }

    public static CharSequence get24ModeFormat(int secondsSize) {
        String skeleton = secondsSize != 0 ? "Hms" : "Hm";
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton);
        // Replace spaces with "Hair Space"
        pattern = pattern.replaceAll(" ", "\u200A");

        // Build a spannable so that the seconds will be formatted
        int secPos = pattern.indexOf('s');
        if (secPos == -1) {
            return pattern;
        }
        Spannable sp = new SpannableString(pattern);
        sp.setSpan(new AbsoluteSizeSpan(secondsSize), secPos - 1, secPos + 2,
                Spannable.SPAN_POINT_MARK);
        return sp;
    }

    /**
     * @return The next alarm from {@link AlarmManager}
     */
    public static String getNextAlarm(Context context) {
        String timeString = null;
        final AlarmManager.AlarmClockInfo info = ((AlarmManager) context.getSystemService(
                Context.ALARM_SERVICE)).getNextAlarmClock();
        if (info != null) {
            final long triggerTime = info.getTriggerTime();
            final Calendar alarmTime = Calendar.getInstance();
            alarmTime.setTimeInMillis(triggerTime);
            timeString = getFormattedTime(context, alarmTime);
        }
        return timeString;
    }

    public static String getFormattedTime(Context context, Calendar time) {
        String skeleton = DateFormat.is24HourFormat(context) ? "EHm" : "Ehma";
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton);
        return (String) DateFormat.format(pattern, time);
    }

    /**
     * @return The next alarm from {@link AlarmManager}
     */
    public static long getNextAlarmInMillis(Context context) {
        final AlarmManager.AlarmClockInfo info = ((AlarmManager) context.getSystemService(
                Context.ALARM_SERVICE)).getNextAlarmClock();
        if (info != null) {
            return info.getTriggerTime();
        }
        return -1;
    }

    public static CharSequence getRaw12ModeFormat(boolean withSeconds) {
        String skeleton = withSeconds ? "hmsa" : "hma";
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton);
        return pattern;
    }

    public static CharSequence getRaw24ModeFormat(boolean withSeconds) {
        String skeleton = withSeconds ? "Hms" : "Hm";
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton);
        return pattern;
    }

    public static Intent getCalendarIntent(final Date date) {
        if (date != null) {
            Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();
            builder.appendPath("time");
            builder.appendPath(Long.toString(date.getTime()));
            Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
            return intent;
        } else {
            Intent calIntent = new Intent(Intent.ACTION_MAIN);
            calIntent.addCategory(Intent.CATEGORY_APP_CALENDAR);
            return calIntent;
        }
    }

    public static Intent getClockIntent() {
        Intent clockIntent = new Intent(Intent.ACTION_MAIN);
        ComponentName cn = new ComponentName("org.omnirom.deskclock", "org.omnirom.deskclock.DeskClock");
        clockIntent.setComponent(cn);
        return clockIntent;
    }
}

