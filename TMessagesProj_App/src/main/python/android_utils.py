"""android_utils (IthaGram) - hilo de UI, listeners, log."""
from java import dynamic_proxy, jclass

AndroidUtilities = jclass("org.telegram.messenger.AndroidUtilities")
_Log = jclass("android.util.Log")
_Runnable = jclass("java.lang.Runnable")
_OnClickListenerIface = jclass("android.view.View$OnClickListener")
_OnLongClickListenerIface = jclass("android.view.View$OnLongClickListener")


def dp(value):
    try:
        return AndroidUtilities.dp(float(value))
    except Exception:
        return 0


def get_string(key, res=0):
    try:
        LocaleController = jclass("org.telegram.messenger.LocaleController")
        if res:
            return LocaleController.getString(str(key), int(res))
        return LocaleController.getString(str(key))
    except Exception:
        return str(key)


class R(dynamic_proxy(_Runnable)):
    def __init__(self, fn):
        super().__init__()
        self._fn = fn

    def run(self):
        try:
            self._fn()
        except Exception as e:
            log("R.run error: %s" % e)


class OnClickListener(dynamic_proxy(_OnClickListenerIface)):
    def __init__(self, fn):
        super().__init__()
        self._fn = fn

    def onClick(self, view):
        self._fn(view)


class OnLongClickListener(dynamic_proxy(_OnLongClickListenerIface)):
    def __init__(self, fn):
        super().__init__()
        self._fn = fn

    def onLongClick(self, view):
        return bool(self._fn(view))


def run_on_ui_thread(func, delay=0):
    if func is None:
        return
    runnable = R(func)
    if delay:
        AndroidUtilities.runOnUIThread(runnable, int(delay))
    else:
        AndroidUtilities.runOnUIThread(runnable)


def log(data):
    try:
        _Log.i("IthaPlugin", data if isinstance(data, str) else str(data))
    except Exception:
        pass


def copy_to_clipboard(text):
    try:
        if AndroidUtilities.addToClipboard(str(text)):
            from ui.bulletin import BulletinHelper
            BulletinHelper.show_info(get_string("TextCopied"))
    except Exception:
        pass
