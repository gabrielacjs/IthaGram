package org.telegram.messenger.regular;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Keep;

import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

// IthaGram: arranque de Python (plugins). Se llama por reflexion desde ApplicationLoader.
@Keep
public class IthaPy {
    @Keep
    public static void start(Context context) {
        try {
            if (!Python.isStarted()) {
                Python.start(new AndroidPlatform(context));
            }
            String r = Python.getInstance().getModule("ithahello").callAttr("hello").toString();
            Log.i("IthaPy", "OK: " + r);
        } catch (Throwable t) {
            Log.e("IthaPy", "FALLO", t);
        }
    }
}
