package com.android.alliance;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.provider.Settings;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.TextAppearanceSpan;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.util.ImageUtils;

import java.lang.CharSequence;
import java.lang.InterruptedException;
import java.lang.Math;
import java.lang.Object;
import java.lang.Runnable;
import java.lang.Thread;
import java.util.Arrays;

public class AllianceUtils {

    private static final Object mScreenshotLock = new Object();
    private static ServiceConnection mScreenshotConnection = null;

    public static int dpToPx(Context context, int dp) {
        return (int) ((dp * context.getResources().getDisplayMetrics().density) + 0.5);
    }

    public static int pxToDp(Context context, int px) {
        return (int) ((px / context.getResources().getDisplayMetrics().density) + 0.5);
    }

    public static boolean isNavBarDefault(Context context) {
        return context.getResources().getBoolean(com.android.internal.R.bool.config_showNavigationBar);
    }

    public static int getBlendColor(int from, int to, float ratio) {
        final float inverseRatio = 1f - ratio;
        final float a = Color.alpha(to) * ratio + Color.alpha(from) * inverseRatio;
        final float r = Color.red(to) * ratio + Color.red(from) * inverseRatio;
        final float g = Color.green(to) * ratio + Color.green(from) * inverseRatio;
        final float b = Color.blue(to) * ratio + Color.blue(from) * inverseRatio;

        return Color.argb((int) a, (int) r, (int) g, (int) b);
    } 

    public static boolean isColorDark(int color) {
        double a = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        if (a >= 0.5) {
            return true;
        } else {
            return false;
        }
    }

    public static int getLightenOrDarkenColor(int color) {
        boolean isDark = isColorDark(color);
        float factor = isDark ? 0.1f : 0.8f;
        int a = Color.alpha(color);
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        int newColor;

        if (isDark) {
            newColor = Color.argb(a,
                    (int) ((r * (1 - factor) / 255 + factor) * 255),
                    (int) ((g * (1 - factor) / 255 + factor) * 255),
                    (int) ((b * (1 - factor) / 255 + factor) * 255));
        } else {
            newColor = Color.argb(a,
                    Math.max((int) (r * factor), 0),
                    Math.max((int) (g * factor), 0),
                    Math.max((int) (b * factor), 0));
        }
        return newColor;
    }

    public static ColorMatrixColorFilter getColorFilter(int color) {
        float r = Color.red(color) / 255f;
        float g = Color.green(color) / 255f;
        float b = Color.blue(color) / 255f;

        ColorMatrix cm = new ColorMatrix(new float[] {
            r, 0, 0, 0, 0,
            0, g, 0, 0, 0,
            0, 0, b, 0, 0,
            0, 0, 0, 1, 0,
        });
        ColorMatrixColorFilter cf = new ColorMatrixColorFilter(cm);
        return cf;
    }

    public static Drawable getColoredDrawable(Drawable drawable, int color) {
        if (drawable instanceof VectorDrawable) {
            drawable.setTint(color);
            return drawable;
        }
        Bitmap colorBitmap = ((BitmapDrawable) drawable).getBitmap();
        Bitmap grayscaleBitmap = toGrayscale(colorBitmap);
        Paint paint = new Paint();
        PorterDuffColorFilter frontFilter = new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY);
        paint.setColorFilter(frontFilter);
        Canvas canvas = new Canvas(grayscaleBitmap);
        canvas.drawBitmap(grayscaleBitmap, 0, 0, paint);
        return new BitmapDrawable(grayscaleBitmap);
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable != null) {
            if (drawable instanceof BitmapDrawable) {
                return ((BitmapDrawable) drawable).getBitmap();
            } else {
                Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                drawable.draw(canvas);
                return bitmap;
            }
        } else {
            return null;
        }
    }

    private static Bitmap toGrayscale(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Bitmap grayBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(grayBitmap);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter cf = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(cf);
        canvas.drawBitmap(bitmap, 0, 0, paint);

        return grayBitmap;
    }

    public static void colorizeIcon(Context context, ImageView imageView, String key, int defaultColor) {
        final int color = Settings.System.getInt(context.getContentResolver(), key, defaultColor);
        if (imageView.getDrawable() != null) {
            if (color == 0) {
                imageView.setColorFilter(null);
            } else {
                imageView.setColorFilter(color, Mode.MULTIPLY);
            }
        }
    }

    public static void colorizeIconAtop(Context context, ImageView imageView, String key, int defaultColor) {
        final int color = Settings.System.getInt(context.getContentResolver(), key, defaultColor);
        if (imageView.getDrawable() != null) {
            if (color == 0) {
                imageView.setColorFilter(null);
            } else {
                imageView.setColorFilter(color, Mode.SRC_ATOP);
            }
        }
    }

    public static void colorizeIcon(Context context, ImageButton imageButton, String key, int defaultColor) {
        final int color = Settings.System.getInt(context.getContentResolver(), key, defaultColor);
        if (imageButton.getDrawable() != null) {
            if (color == 0) {
                imageButton.getDrawable().setColorFilter(null);
            } else {
                imageButton.getDrawable().setColorFilter(color, Mode.MULTIPLY);
            }
        }
        if (imageButton.getBackground() != null) {
            if (color == 0) {
                imageButton.getBackground().setColorFilter(null);
            } else {
                imageButton.getBackground().setColorFilter(color, Mode.MULTIPLY);
            }
        }
    }

    public static void colorizeText(Context context, TextView textView, String key, int defaultColor) {
        CharSequence resultCharSequence = null;
        CharSequence charSequence = textView.getText();
        final int color = Settings.System.getInt(context.getContentResolver(), key, defaultColor);

        if (charSequence instanceof Spanned) {
            Spanned spanned = (Spanned) charSequence;
            Object[] spans = spanned.getSpans(0, spanned.length(), Object.class);
            SpannableStringBuilder builder = new SpannableStringBuilder(spanned.toString());
            for (Object span : spans) {
                Object resultSpan = span;
                if (span instanceof TextAppearanceSpan) {
                    resultSpan = processTextAppearanceSpan(color, (TextAppearanceSpan) span);
                }
                builder.setSpan(resultSpan, spanned.getSpanStart(span), spanned.getSpanEnd(span), spanned.getSpanFlags(span));
            }
            resultCharSequence = builder;
        } else {
            resultCharSequence = charSequence;
        }

        if (resultCharSequence != null) {
            textView.setText(resultCharSequence);
        }
        textView.setTextColor(color);
    }

    private static TextAppearanceSpan processTextAppearanceSpan(TextAppearanceSpan span) {
        ColorStateList colorStateList = span.getTextColor();
        if (colorStateList != null) {
            int[] colors = colorStateList.getColors();
            boolean changed = false;
            for (int i = 0; i < colors.length; i++) {
                if (ImageUtils.isGrayscale(colors[i])) {
                    if (!changed) {
                        colors = Arrays.copyOf(colors, colors.length);
                    }
                    colors[i] = processColor(colors[i]);
                    changed = true;
                }
            }
            if (changed) {
                return new TextAppearanceSpan(
                        span.getFamily(), span.getTextStyle(), span.getTextSize(),
                        new ColorStateList(colorStateList.getStates(), colors),
                        span.getLinkTextColor());
            }
        }
        return span;
    }

    private static TextAppearanceSpan processTextAppearanceSpan(int color, TextAppearanceSpan span) {
        ColorStateList colorStateList = span.getTextColor();
        if (colorStateList != null) {
            int[] colors = colorStateList.getColors();
            boolean changed = false;
            for (int i = 0; i < colors.length; i++) {
                if (ImageUtils.isGrayscale(colors[i])) {
                    if (!changed) {
                        colors = Arrays.copyOf(colors, colors.length);
                    }
                    colors[i] = processColor(color);
                    changed = true;
                }
            }
            if (changed) {
                return new TextAppearanceSpan(span.getFamily(), span.getTextStyle(), span.getTextSize(), new ColorStateList(colorStateList.getStates(), colors), span.getLinkTextColor());
            }
        }
        return span;
    }

    private static int processColor(int color) {
        return Color.argb(Color.alpha(color), Color.red(color), Color.green(color), Color.blue(color));
    }

    public static int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    public static int getIconColorDark(Context context, String key) {
        final int color = Settings.System.getInt(context.getContentResolver(), key, 0x7a000000);
        return (153 << 24) | (color & 0x00ffffff);
    }

    public static boolean isLandscapePhone(Context context) {
        Configuration config = context.getResources().getConfiguration();
        return config.orientation == Configuration.ORIENTATION_LANDSCAPE
                && config.smallestScreenWidthDp < 600;
    }

    public static int getAudioMode(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        switch (audioManager.getRingerMode()) {
            case AudioManager.RINGER_MODE_NORMAL:
            default:
                return 0;
            case AudioManager.RINGER_MODE_SILENT:
                return 1;
            case AudioManager.RINGER_MODE_VIBRATE:
                return 2;
        }
    }

    public static void setAudioMode(Context context, int mode) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        switch (mode) {
            case 0:
            default:
                audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                break;
            case 1:
                audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                break;
            case 2:
                audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                break;
        }
    }

    public static boolean isAirplaneModeOn(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    private static Runnable getScreenshotRunnable(Context context) {
        final Context mContext = context;
        final Runnable screenshotTimeout = new Runnable() {
            @Override
            public void run() {
                synchronized(mScreenshotLock) {
                    if (mScreenshotConnection != null) {
                        mContext.unbindService(mScreenshotConnection);
                        mScreenshotConnection = null;
                    }
                }
            }
        };
        return screenshotTimeout;
    }

    public static void takeScreenshot(Context context) {
        final Handler screenshotHandler = new Handler();
        final Context mContext = context;
        synchronized (mScreenshotLock) {
            if (mScreenshotConnection != null) {
                return;
            }
            ComponentName component = new ComponentName("com.android.systemui", "com.android.systemui.screenshot.TakeScreenshotService");
            Intent intent = new Intent();
            intent.setComponent(component);
            ServiceConnection connection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    synchronized (mScreenshotLock) {
                        if (mScreenshotConnection != this) {
                            return;
                        }
                        Messenger messenger = new Messenger(service);
                        Message msg = Message.obtain(null, 1);
                        final ServiceConnection mConnection = this;
                        Handler handler = new Handler(screenshotHandler.getLooper()) {
                            @Override
                            public void handleMessage(Message msg) {
                                synchronized (mScreenshotLock) {
                                    if (mScreenshotConnection == mConnection) {
                                        mContext.unbindService(mScreenshotConnection);
                                        mScreenshotConnection = null;
                                        screenshotHandler.removeCallbacks(getScreenshotRunnable(mContext));
                                    }
                                }
                            }
                        };
                        msg.replyTo = new Messenger(handler);
                        msg.arg1 = msg.arg2 = 0;

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        try {
                            messenger.send(msg);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {}
            };

            if (mContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)) {
                mScreenshotConnection = connection;
                screenshotHandler.postDelayed(getScreenshotRunnable(mContext), 10000);
            }
        }
    }

    public static void colorizeBackground(Context context, ViewGroup viewGroup, String key, int defaultColor) {
        viewGroup.setBackgroundColor(Settings.System.getInt(context.getContentResolver(), key, defaultColor));
    }
}
