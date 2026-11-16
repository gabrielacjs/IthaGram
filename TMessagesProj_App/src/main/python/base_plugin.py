"""base_plugin (IthaGram) - BasePlugin, MethodHook y utilidades de hooks.

Los hooks no son nativos: se registran en org.telegram.messenger.IthaHooks y
Telegram avisa en puntos concretos (onResume de fragments, onActivityResult).
"""
from java import dynamic_proxy, jclass

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


class MethodHook:
    def before_hooked_method(self, param):
        pass

    def after_hooked_method(self, param):
        pass


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
    id = ""
    name = ""
    description = ""
    author = ""
    version = ""
    icon = ""
    min_version = ""

    def __init__(self):
        self._itha_hooks = []

    # ---- ciclo de vida ----
    def on_plugin_load(self):
        pass

    def on_plugin_unload(self):
        pass

    def create_settings(self):
        return []

    # ---- ajustes ----
    def get_setting(self, key, default=None):
        return plugin_settings.get_setting(self.id, key, default)

    def set_setting(self, key, value, reload_settings=False):
        plugin_settings.set_setting(self.id, key, value)
        if reload_settings:
            try:
                import ithaengine
                ithaengine.reload_settings(self.id)
            except Exception:
                pass
        return True

    def log(self, message):
        log("[%s] %s" % (self.id, message))

    # ---- hooks ----
    def hook_method(self, method, hook, priority=0):
        try:
            cb = _HookCallback(hook, self.id)
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

    def unhook_method(self, handle):
        try:
            if handle is None:
                return
            IthaHooks.unregister(handle.java_handle)
            if handle in getattr(self, "_itha_hooks", []):
                self._itha_hooks.remove(handle)
        except Exception as e:
            log("[%s] unhook_method error: %s" % (self.id, e))

    def _itha_unhook_all(self):
        for h in list(getattr(self, "_itha_hooks", [])):
            self.unhook_method(h)
