package com.segunfamisa.lekkiikoyiwatchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

public class DigitalWatchFaceService extends CanvasWatchFaceService {

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }


    /**
     * Service callback methods
     */
    private class Engine extends CanvasWatchFaceService.Engine {

        static final int MSG_UPDATE_TIME = 0;

        String TIME_SEPARATOR = ":";

        Calendar mCalendar;

        // device feature
        boolean mLowBitAmbient;
        boolean mBurnInProtection;

        // graphic objects
        Bitmap mBackgroundBitmap;
        Bitmap mBackgroundScaledBitmap;

        Paint mHourPaint;
        Paint mMinutePaint;
        Paint mSeparatorPaint;
        Paint mDatePaint;
        Paint mBackgroundPaint;

        int width;
        int height;
        float mSeparatorWidth;
        float mYOffset;
        float mLineHeight;

        boolean mShouldDrawSeparator;

        // custom timer variables
        private long INTERACTIVE_UPDATE_RATE_MS = 300;

        // handler to update time once a second in interactive mode.
        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        invalidate();

                        if (isShouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = INTERACTIVE_UPDATE_RATE_MS -
                                    (timeMs % INTERACTIVE_UPDATE_RATE_MS);

                            mUpdateTimeHandler
                                    .sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };

        private boolean mRegisteredTimeZoneReceiver;

        // receiver to update the timezone
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // get default timezone and redraw watchface
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            // configure UI system
            configureUISystem();

            // inialize and load background image
            initializeBackground();

            // initalize paints.
            initializePaints();

            // allocate calendar
            mCalendar = Calendar.getInstance();
        }

        private void configureUISystem() {
            setWatchFaceStyle(new WatchFaceStyle.Builder(DigitalWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build()
            );
        }

        private void initializePaints() {
            Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/digital-7-mono.ttf");

            // create graphic styles
            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(Color.BLACK);


            mHourPaint = new Paint();
            mHourPaint.setARGB(255, 255, 255, 255);
            mHourPaint.setStrokeWidth(5.0f);
            mHourPaint.setAntiAlias(true);
            mHourPaint.setStrokeCap(Paint.Cap.ROUND);
            mHourPaint.setTypeface(tf);

            mMinutePaint = new Paint();
            mMinutePaint.setARGB(255, 255, 255, 255);
            mMinutePaint.setStrokeWidth(5.0f);
            mMinutePaint.setAntiAlias(true);
            mMinutePaint.setStrokeCap(Paint.Cap.ROUND);
            mMinutePaint.setTypeface(tf);

            mSeparatorPaint = new Paint();
            mSeparatorPaint.setARGB(255, 255, 255, 255);
            mSeparatorPaint.setStrokeWidth(5.0f);
            mSeparatorPaint.setAntiAlias(true);
            mSeparatorPaint.setTypeface(tf);

            mDatePaint = new Paint();
            mDatePaint.setARGB(255, 255, 255, 255);
            mDatePaint.setStrokeWidth(5.0f);
            mDatePaint.setAntiAlias(true);
        }

        private void initializeBackground() {
            Resources res = DigitalWatchFaceService.this.getResources();
            Drawable backgroundDrawable = res.getDrawable(R.drawable.watch_face_bg, null);

            mBackgroundBitmap = ((BitmapDrawable) backgroundDrawable).getBitmap();
        }

        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);

            if (isShouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        private boolean isShouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);

            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
        }


        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            /** wearable switched between modes */
            if (mLowBitAmbient) {
                boolean antiAlias = !inAmbientMode;

                mHourPaint.setAntiAlias(antiAlias);
                mMinutePaint.setAntiAlias(antiAlias);
                mSeparatorPaint.setAntiAlias(antiAlias);
                mDatePaint.setAntiAlias(antiAlias);
            }

            invalidate();
            updateTimer();
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();

            // redraw watch face
            invalidate();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (visible) {
                registerReceiver();

                // update timezone
                mCalendar.setTimeZone(TimeZone.getDefault());
            } else {
                unregisterReceiver();
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (mBackgroundScaledBitmap == null || mBackgroundScaledBitmap.getWidth() != width
                    || mBackgroundScaledBitmap.getHeight() != height) {
                mBackgroundScaledBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
                        width, height, true);
            }

            this.width = width;
            this.height = height;

            super.onSurfaceChanged(holder, format, width, height);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            Resources res = getResources();

            float timeTextSize = res.getDimension(R.dimen.time_text_size);
            float dateTextSize = res.getDimension(R.dimen.date_text_size);

            // set textsizes
            mDatePaint.setTextSize(dateTextSize);

            mHourPaint.setTextSize(timeTextSize);
            mMinutePaint.setTextSize(timeTextSize);
            mSeparatorPaint.setTextSize(timeTextSize);

            mSeparatorWidth = mSeparatorPaint.measureText(TIME_SEPARATOR);

            mYOffset = res.getDimension(R.dimen.y_offset);

            mLineHeight = res.getDimension(R.dimen.line_height);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {

            // update time
            mCalendar.setTimeInMillis(System.currentTimeMillis());

            int seconds = mCalendar.get(Calendar.SECOND);
            int minutes = mCalendar.get(Calendar.MINUTE);
            int hours = mCalendar.get(Calendar.HOUR_OF_DAY);

            String minuteText = formatTime(minutes);
            String hourText = formatTime(hours);

            mShouldDrawSeparator = (System.currentTimeMillis() % 1000) < 500;

            if (isInAmbientMode()) {
                // draw the background
                canvas.drawBitmap(mBackgroundBitmap, bounds.left, bounds.top, mBackgroundPaint);
            } else {
                canvas.drawRect(bounds.left, bounds.top, bounds.right, bounds.bottom, mBackgroundPaint);
            }

            // draw the time
            // build the time
            String time = hourText + TIME_SEPARATOR + minuteText;

            // measure the width.
            float timeWidth = mHourPaint.measureText(hourText) + mSeparatorPaint.measureText(TIME_SEPARATOR)
                    + mMinutePaint.measureText(minuteText);

            float x = (bounds.width() - timeWidth) / 2f;

            // draw hour
            canvas.drawText(hourText, x, mYOffset, mHourPaint);

            x += mHourPaint.measureText(hourText);

            // draw separator if is ambient or should draw separator
            if (isInAmbientMode() || mShouldDrawSeparator) {
                canvas.drawText(TIME_SEPARATOR, x, mYOffset, mSeparatorPaint);
            }

            x += mSeparatorPaint.measureText(TIME_SEPARATOR);

            // draw minute text
            canvas.drawText(minuteText, x, mYOffset, mMinutePaint);

            float y = mYOffset + mLineHeight;

            // draw date
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
            String date = sdf.format(mCalendar.getTime());

            x = (bounds.width() - mDatePaint.measureText(date)) / 2f;

            canvas.drawText(date, x, y, mDatePaint);

        }

        private String formatTime(int time) {
            if (time > 9) {
                return String.valueOf(time);
            } else {
                return "0" + String.valueOf(time);
            }
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private void registerReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            DigitalWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }

            mRegisteredTimeZoneReceiver = false;
            DigitalWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }
    }
}
