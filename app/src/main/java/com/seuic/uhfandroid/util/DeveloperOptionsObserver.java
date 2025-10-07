package com.seuic.uhfandroid.util;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;

public class DeveloperOptionsObserver extends ContentObserver {
    private Context context;
    private OnDeveloperOptionsChangedListener listener;

    public DeveloperOptionsObserver(Context context, Handler handler, OnDeveloperOptionsChangedListener listener) {
        super(handler);
        this.context = context;
        this.listener = listener;
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        super.onChange(selfChange, uri);
        if (uri != null && uri.equals(Settings.Secure.getUriFor(Settings.Global.DEVELOPMENT_SETTINGS_ENABLED))) {
            boolean developerOptionsEnabled = Settings.Global.getInt(context.getContentResolver(),
                    Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1;
            listener.onDeveloperOptionsChanged(developerOptionsEnabled);
        }
    }

    public interface OnDeveloperOptionsChangedListener {
        void onDeveloperOptionsChanged(boolean enabled);
    }
}
