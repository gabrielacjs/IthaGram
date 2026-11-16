"""ui.bulletin (IthaGram) - avisos breves."""
from java import jclass
from android_utils import run_on_ui_thread, log


def _show(kind, text):
    def go():
        try:
            from client_utils import get_last_fragment
            fragment = get_last_fragment()
            BulletinFactory = jclass("org.telegram.ui.Components.BulletinFactory")
            if fragment is None:
                raise RuntimeError("sin fragment")
            factory = BulletinFactory.of(fragment)
            if kind == "error":
                factory.createErrorBulletin(str(text)).show()
            else:
                raw = jclass("org.telegram.messenger.R$raw")
                icon = raw.contact_check if kind == "success" else raw.chats_infotip
                factory.createSimpleBulletin(icon, str(text)).show()
        except Exception as e:
            try:
                Toast = jclass("android.widget.Toast")
                ctx = jclass("org.telegram.messenger.ApplicationLoader").applicationContext
                Toast.makeText(ctx, str(text), 0).show()
            except Exception:
                log("bulletin error: %s" % e)
    run_on_ui_thread(go)


class BulletinHelper:
    @staticmethod
    def show_error(text, *args, **kwargs):
        _show("error", text)

    @staticmethod
    def show_info(text, *args, **kwargs):
        _show("info", text)

    @staticmethod
    def show_success(text, *args, **kwargs):
        _show("success", text)

    @staticmethod
    def show_simple(text, *args, **kwargs):
        _show("info", text)
