"""client_utils (IthaGram) - acceso a fragments y colas de Telegram."""
from java import jclass
from android_utils import R, log


def get_last_fragment():
    try:
        LaunchActivity = jclass("org.telegram.ui.LaunchActivity")
    except Exception:
        return None
    for name in ("getSafeLastFragment", "getLastFragment"):
        try:
            f = getattr(LaunchActivity, name)()
            if f is not None:
                return f
        except Exception:
            pass
    return None


def run_on_queue(fn, delay=0):
    if fn is None:
        return
    try:
        queue = jclass("org.telegram.messenger.Utilities").globalQueue
        if delay:
            queue.postRunnable(R(fn), int(delay))
        else:
            queue.postRunnable(R(fn))
    except Exception as e:
        log("run_on_queue error: %s" % e)
