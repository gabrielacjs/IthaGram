"""base_plugin (IthaGram) - API de plugins compatible con exteraGram.

Los nombres y las firmas siguen la API de exteraGram. Por debajo:
- hook_method usa org.telegram.messenger.IthaHooks (puntos de enganche en el
  codigo de Telegram, no hooks nativos).
- add_hook / add_menu_item / on_app_event se guardan en `ithabus`; Telegram los
  consulta desde sus puntos de enganche.
"""
from java import dynamic_proxy, jclass

import ithabus
import plugin_settings
from android_utils import log

IthaHooks = jclass("org.telegram.messenger.IthaHooks")
_Callback = jclass("org.telegram.messenger.IthaHooks$Callback")


class PluginError(Exception):
    def __init__(self, message, plugin_id):
        super().__init__("[%s] %s" % (plugin_id, message))
        self.message = message
        self.plugin_id = plugin_id


class HookStrategy:
    DEFAULT = "DEFAULT"
    CANCEL = "CANCEL"
    MODIFY = "MODIFY"
    MODIFY_FINAL = "MODIFY_FINAL"


class AppEvent(str):
    def __new__(cls, value):
        return str.__new__(cls, value)

    START = "app_start"
    STOP = "app_stop"
    PAUSE = "app_pause"
    RESUME = "app_resume"


class MenuItemType:
    MESSAGE_CONTEXT_MENU = "message_context_menu"
    DRAWER_MENU = "drawer_menu"
    MAIN_MENU = "main_menu"
    CHAT_ACTION_MENU = "chat_action_menu"
    PROFILE_ACTION_MENU = "profile_action_menu"


class MenuItemData(dict):
    def __init__(self, menu_type, text, on_click, item_id=None, icon=None,
                 subtext=None, condition=None, priority=0):
        super().__init__()
        self["menu_type"] = menu_type
        self["text"] = text
        self["on_click"] = on_click
        self["item_id"] = item_id
        self["icon"] = icon
        self["subtext"] = subtext
        self["condition"] = condition
        self["priority"] = priority


class HookResult:
    """Lo que devuelven los metodos *_hook de un plugin."""

    def __init__(self, strategy=HookStrategy.DEFAULT, request=None, response=None,
                 update=None, updates=None, params=None, error=None, message=None):
        self.strategy = strategy
        self.request = request
        self.response = response
        self.update = update
        self.updates = updates
        self.params = params
        self.error = error
        self.message = message

    def get(self, key, default=None):
        return getattr(self, key, default)


# ---- filtros de hooks (se guardan; de momento no se aplican) ----
class _Filter:
    def __init__(self, kind, index=None, value=None, clazz=None, filters=None, condition=None):
        self.kind = kind
        self.index = index
        self.value = value
        self.clazz = clazz
        self.filters = filters
        self.condition = condition


class HookFilter:
    RESULT_IS_NULL = _Filter("result_is_null")
    RESULT_IS_TRUE = _Filter("result_is_true")
    RESULT_IS_FALSE = _Filter("result_is_false")
    RESULT_NOT_NULL = _Filter("result_not_null")

    @staticmethod
    def ResultIsInstanceOf(clazz):
        return _Filter("result_is_instance_of", clazz=clazz)

    @staticmethod
    def ResultEqual(value):
        return _Filter("result_equal", value=value)

    @staticmethod
    def ResultNotEqual(value):
        return _Filter("result_not_equal", value=value)

    @staticmethod
    def ArgumentIsNull(index):
        return _Filter("argument_is_null", index=index)

    @staticmethod
    def ArgumentNotNull(index):
        return _Filter("argument_not_null", index=index)

    @staticmethod
    def ArgumentIsFalse(index):
        return _Filter("argument_is_false", index=index)

    @staticmethod
    def ArgumentIsTrue(index):
        return _Filter("argument_is_true", index=index)

    @staticmethod
    def ArgumentIsInstanceOf(index, clazz):
        return _Filter("argument_is_instance_of", index=index, clazz=clazz)

    @staticmethod
    def ArgumentEqual(index, value):
        return _Filter("argument_equal", index=index, value=value)

    @staticmethod
    def ArgumentNotEqual(index, value):
        return _Filter("argument_not_equal", index=index, value=value)

    @staticmethod
    def Condition(condition, object=None):
        return _Filter("condition", condition=condition, value=object)

    @staticmethod
    def Or(*filters):
        return _Filter("or", filters=list(filters))


def hook_filters(*filters):
    def decorator(fn):
        fn.__hook_filters__ = list(filters)
        return fn
    return decorator


def fn_hook_filters(*filters):
    return hook_filters(*filters)


# ---- hooks de metodos ----
class MethodHook:
    def before_hooked_method(self, param):
        pass

    def after_hooked_method(self, param):
        pass


class MethodReplacement(MethodHook):
    """Aqui el reemplazo se ejecuta antes del metodo original."""

    def replace_hooked_method(self, param):
        return None

    def before_hooked_method(self, param):
        self.replace_hooked_method(param)


class XposedHook(MethodHook):
    def __init__(self, before=None, after=None):
        self._before = before
        self._after = after

    def before_hooked_method(self, param):
        if self._before is not None:
            return self._before(param)

    def after_hooked_method(self, param):
        if self._after is not None:
            return self._after(param)


class BaseHook(MethodHook):
    def __init__(self, plugin, *, before=None, after=None, before_filters=None, after_filters=None):
        self.plugin = plugin
        self.before = before
        self.after = after
        self.before_filters = before_filters or []
        self.after_filters = after_filters or []

    def before_hooked_method(self, param):
        if self.before is not None:
            return self.before(param)

    def after_hooked_method(self, param):
        if self.after is not None:
            return self.after(param)


class _HookCallback(dynamic_proxy(_Callback)):
    def __init__(self, hook, plugin_id):
        super().__init__()
        self._hook = hook
        self._plugin_id = plugin_id

    def before(self, param):
        try:
            self._hook.before_hooked_method(param)
        except Exception as e:
            log("[%s] before_hooked_method error: %s" % (self._plugin_id, e))

    def after(self, param):
        try:
            self._hook.after_hooked_method(param)
        except Exception as e:
            log("[%s] after_hooked_method error: %s" % (self._plugin_id, e))


class _HookHandle:
    def __init__(self, java_handle, callback):
        self.java_handle = java_handle
        self.callback = callback


class BasePlugin:
    def __init__(self):
        self.id = None
        self.name = None
        self.enabled = False
        self.initialized = False
        self._itha_hooks = []

    @classmethod
    def _findPluginClass(cls, module):
        import inspect
        module_name = getattr(module, "__name__", None)
        candidates = []
        for name, obj in inspect.getmembers(module, inspect.isclass):
            if obj is BasePlugin:
                continue
            try:
                if not issubclass(obj, BasePlugin):
                    continue
            except TypeError:
                continue
            if module_name is not None and getattr(obj, "__module__", None) != module_name:
                continue
            candidates.append((-len(inspect.getmro(obj)), name, obj))
        if not candidates:
            return None
        candidates.sort(key=lambda item: (item[0], item[1]))
        return candidates[0][2]

    # ---- ciclo de vida ----
    def on_plugin_load(self):
        pass

    def on_plugin_unload(self):
        pass

    def on_app_event(self, event_type):
        pass

    def create_settings(self):
        return []

    # ---- ajustes ----
    def get_setting(self, key, default=None):
        return plugin_settings.get_setting(self.id, key, default)

    def set_setting(self, key, value, reload_settings=False):
        result = plugin_settings.set_setting(self.id, key, value)
        if reload_settings:
            self.reload_settings()
        return result

    def export_settings(self):
        return plugin_settings.get_all_settings(self.id)

    def import_settings(self, settings, reload_settings=True):
        result = plugin_settings.set_all_settings(self.id, settings)
        if reload_settings:
            self.reload_settings()
        return result

    def reload_settings(self):
        try:
            import ithaengine
            ithaengine.reload_settings(self.id)
        except Exception:
            pass

    # ---- log ----
    @property
    def logger(self):
        plugin = self

        class _L:
            def _w(self, msg, *a):
                plugin.log(msg % a if a else msg)
            debug = info = warning = error = exception = critical = _w
        return _L()

    def log(self, message):
        log("[%s] %s" % (self.id, message))

    def _cleanup(self):
        pass

    def client(self, account=None):
        self.log("client(): todavia no esta disponible en IthaGram")
        return None

    # ---- hooks de eventos (el plugin los sobrescribe) ----
    def pre_request_hook(self, request_name, account, request):
        return HookResult()

    def post_request_hook(self, request_name, account, response, error):
        return HookResult()

    def on_update_hook(self, update_name, account, update):
        return HookResult()

    def on_updates_hook(self, container_name, account, updates):
        return HookResult()

    def on_send_message_hook(self, account, params):
        return HookResult()

    def add_hook(self, name, match_substring=False, priority=0):
        ithabus.add_event_hook(self.id, name, match_substring, priority)

    def add_on_send_message_hook(self, priority=0):
        self.add_hook("send_message_hook", False, priority)

    def remove_hook(self, name):
        ithabus.remove_event_hook(self.id, name)

    # ---- hooks de metodos ----
    def hook_method(self, method, xposed_hook=None, priority=0, *,
                    before=None, after=None, before_filters=None, after_filters=None):
        try:
            callback = xposed_hook
            if callback is None:
                callback = BaseHook(self, before=before, after=after,
                                    before_filters=before_filters, after_filters=after_filters)
            cb = _HookCallback(callback, self.id)
            handle = IthaHooks.register(method.getDeclaringClass(), method.getName(), cb)
            if handle is None:
                log("[%s] hook_method: '%s' todavia no se puede enganchar en IthaGram" % (self.id, method.getName()))
                return None
            h = _HookHandle(handle, cb)
            if not hasattr(self, "_itha_hooks"):
                self._itha_hooks = []
            self._itha_hooks.append(h)
            return h
        except Exception as e:
            log("[%s] hook_method error: %s" % (self.id, e))
            return None

    def hook_all_methods(self, hook_class, method_name, xposed_hook=None, priority=0, *,
                         before=None, after=None, before_filters=None, after_filters=None):
        out = []
        try:
            methods = hook_class.getDeclaredMethods()
            for i in range(len(methods)):
                m = methods[i]
                if m.getName() == method_name:
                    h = self.hook_method(m, xposed_hook, priority, before=before, after=after,
                                         before_filters=before_filters, after_filters=after_filters)
                    if h is not None:
                        out.append(h)
        except Exception as e:
            self.log("hook_all_methods(%s) fallo: %s" % (method_name, e))
        return out

    def hook_all_constructors(self, hook_class, xposed_hook=None, priority=0, *,
                              before=None, after=None, before_filters=None, after_filters=None):
        self.log("hook_all_constructors: todavia no esta disponible en IthaGram")
        return []

    def unhook_method(self, handle):
        try:
            if handle is None:
                return
            IthaHooks.unregister(handle.java_handle)
            if handle in getattr(self, "_itha_hooks", []):
                self._itha_hooks.remove(handle)
        except Exception as e:
            log("[%s] unhook_method error: %s" % (self.id, e))

    # ---- menus ----
    def add_menu_item(self, menu_item_data):
        try:
            return ithabus.add_menu_item(self.id, menu_item_data)
        except Exception as e:
            self.log("add_menu_item fallo: %s" % e)
            return None

    def remove_menu_item(self, item_id):
        return ithabus.remove_menu_item(self.id, item_id)

    # ---- archivos e intents (todavia no disponibles) ----
    def add_file_hook(self, file_info):
        self.log("add_file_hook: todavia no esta disponible en IthaGram")
        return None

    def remove_file_hook(self, ext, secret=None):
        return False

    def add_intent_hook(self, info, type):
        self.log("add_intent_hook: todavia no esta disponible en IthaGram")
        return None

    def remove_intent_hook(self, handler_id):
        return False

    def _itha_unhook_all(self):
        for h in list(getattr(self, "_itha_hooks", [])):
            self.unhook_method(h)
        try:
            ithabus.remove_plugin(self.id)
        except Exception:
            pass
