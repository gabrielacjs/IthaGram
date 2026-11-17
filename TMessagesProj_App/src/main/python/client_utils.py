"""client_utils (IthaGram) - acceso a fragments y colas de Telegram."""
import threading

from java import dynamic_proxy, jclass, jlong
from android_utils import R, log

_hook_account_local = threading.local()


def get_hook_account():
    """Cuenta del hook que se esta ejecutando (o None fuera de un hook)."""
    return getattr(_hook_account_local, "_value", None)


def _set_hook_account(account):
    try:
        _hook_account_local._value = int(account) if account is not None else None
    except Exception:
        _hook_account_local._value = None


def get_account(account=None):
    """Numero de cuenta: el dado, el del hook en curso o la cuenta activa."""
    if account is None:
        account = get_hook_account()
    if account is None:
        account = jclass("org.telegram.messenger.UserConfig").selectedAccount
    return int(account)


def _instance(class_name, account):
    return jclass(class_name).getInstance(get_account(account))


def get_account_instance(account=None):
    return _instance("org.telegram.messenger.AccountInstance", account)


def get_messages_controller(account=None):
    return _instance("org.telegram.messenger.MessagesController", account)


def get_contacts_controller(account=None):
    return _instance("org.telegram.messenger.ContactsController", account)


def get_media_data_controller(account=None):
    return _instance("org.telegram.messenger.MediaDataController", account)


def get_connections_manager(account=None):
    return _instance("org.telegram.tgnet.ConnectionsManager", account)


def get_location_controller(account=None):
    return _instance("org.telegram.messenger.LocationController", account)


def get_notifications_controller(account=None):
    return _instance("org.telegram.messenger.NotificationsController", account)


def get_messages_storage(account=None):
    return _instance("org.telegram.messenger.MessagesStorage", account)


def get_send_messages_helper(account=None):
    return _instance("org.telegram.messenger.SendMessagesHelper", account)


def get_file_loader(account=None):
    return _instance("org.telegram.messenger.FileLoader", account)


def get_secret_chat_helper(account=None):
    return _instance("org.telegram.messenger.SecretChatHelper", account)


def get_download_controller(account=None):
    return _instance("org.telegram.messenger.DownloadController", account)


def get_notification_center(account=None):
    return _instance("org.telegram.messenger.NotificationCenter", account)


def get_user_config(account=None):
    return _instance("org.telegram.messenger.UserConfig", account)


def get_notifications_settings(account=None):
    return jclass("org.telegram.messenger.MessagesController").getNotificationsSettings(get_account(account))


def _make_delegate(fn, account):
    """Crea el RequestDelegate al momento de usarlo (asi un cambio de nombre no rompe la importacion)."""
    Delegate = jclass("org.telegram.tgnet.RequestDelegate")

    class _RequestDelegate(dynamic_proxy(Delegate)):
        def __init__(self):
            super().__init__()

        def run(self, response, error):
            _set_hook_account(account)
            try:
                fn(response, error)
            except Exception:
                import traceback
                log("send_request callback error: %s" % traceback.format_exc())
            finally:
                _set_hook_account(None)

    return _RequestDelegate()


def send_request(request, fn=None, account=None):
    """Envia una peticion TL a Telegram. fn(response, error) se llama al terminar."""
    acc = get_account(account)
    manager = get_connections_manager(acc)
    if fn is None:
        return manager.sendRequest(request, None)
    return manager.sendRequest(request, _make_delegate(fn, acc))


def send_message(params, parse_mode=None, account=None):
    """Envia un SendMessageParams ya armado."""
    if parse_mode:
        log("send_message: parse_mode todavia no esta disponible en IthaGram")
    return get_send_messages_helper(account).sendMessage(params)


def send_text(peer, text, parse_mode=None, account=None, **kwargs):
    """Envia un texto a `peer` (id de dialogo)."""
    SendMessageParams = jclass("org.telegram.messenger.SendMessagesHelper$SendMessageParams")
    params = SendMessageParams.of(str(text), jlong(int(peer)))
    return send_message(params, parse_mode=parse_mode, account=account)


class AccountClient:
    """Cliente de una cuenta concreta; lo devuelve BasePlugin.client()."""

    def __init__(self, account):
        self.account = get_account(account)

    def __repr__(self):
        return "AccountClient(account=%r)" % (self.account,)

    def __eq__(self, other):
        return isinstance(other, AccountClient) and other.account == self.account

    def __hash__(self):
        return hash(self.account)

    def get_account_instance(self):
        return get_account_instance(self.account)

    def get_messages_controller(self):
        return get_messages_controller(self.account)

    def get_contacts_controller(self):
        return get_contacts_controller(self.account)

    def get_media_data_controller(self):
        return get_media_data_controller(self.account)

    def get_connections_manager(self):
        return get_connections_manager(self.account)

    def get_location_controller(self):
        return get_location_controller(self.account)

    def get_notifications_controller(self):
        return get_notifications_controller(self.account)

    def get_messages_storage(self):
        return get_messages_storage(self.account)

    def get_send_messages_helper(self):
        return get_send_messages_helper(self.account)

    def get_file_loader(self):
        return get_file_loader(self.account)

    def get_secret_chat_helper(self):
        return get_secret_chat_helper(self.account)

    def get_download_controller(self):
        return get_download_controller(self.account)

    def get_notifications_settings(self):
        return get_notifications_settings(self.account)

    def get_notification_center(self):
        return get_notification_center(self.account)

    def get_user_config(self):
        return get_user_config(self.account)

    def send_request(self, request, fn=None):
        return send_request(request, fn, account=self.account)

    def send_message(self, params, parse_mode=None):
        return send_message(params, parse_mode=parse_mode, account=self.account)

    def send_text(self, peer, text, parse_mode=None, **kwargs):
        return send_text(peer, text, parse_mode=parse_mode, account=self.account, **kwargs)


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
