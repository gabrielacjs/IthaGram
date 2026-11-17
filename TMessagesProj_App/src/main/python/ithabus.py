"""ithabus (IthaGram) - registro de hooks de eventos, items de menu y eventos de app.

Aqui se guardan las cosas que los plugins registran (add_hook, add_menu_item).
Telegram las consulta desde los puntos de enganche (se agregan por separado).
Cada funcion execute_* es segura: si un plugin falla, se registra y se sigue.
"""
import traceback

from java import dynamic_proxy, jclass, jint

from android_utils import log
from client_utils import _set_hook_account

IthaHooks = jclass("org.telegram.messenger.IthaHooks")

_event_hooks = {}   # plugin_id -> {nombre: (match_substring, priority)}
_menu_items = {}    # plugin_id -> {item_id: dict}
_counter = [0]


def _plugins():
    try:
        import ithaengine
        return ithaengine.get_plugins()
    except Exception:
        return {}


def _refresh_flags():
    """Avisa a Java si hay plugins escuchando, para no llamar a Python de balde."""
    try:
        send = False
        other = False
        for hooks in _event_hooks.values():
            for name in hooks:
                if name == "send_message_hook":
                    send = True
                else:
                    other = True
        IthaHooks.sendActive = send
        IthaHooks.updatesActive = other
        IthaHooks.menuActive = any(bool(v) for v in _menu_items.values())
    except Exception as e:
        log("ithabus flags error: %s" % e)


def add_event_hook(pid, name, match_substring=False, priority=0):
    _event_hooks.setdefault(pid, {})[str(name)] = (bool(match_substring), int(priority))
    _refresh_flags()


def remove_event_hook(pid, name):
    _event_hooks.get(pid, {}).pop(str(name), None)
    _refresh_flags()


def add_menu_item(pid, data):
    item_id = data.get("item_id")
    if not item_id:
        _counter[0] += 1
        item_id = "%s_item_%d" % (pid, _counter[0])
        data["item_id"] = item_id
    _menu_items.setdefault(pid, {})[item_id] = data
    _refresh_flags()
    return item_id


def remove_menu_item(pid, item_id):
    ok = _menu_items.get(pid, {}).pop(item_id, None) is not None
    _refresh_flags()
    return ok


def remove_plugin(pid):
    _event_hooks.pop(pid, None)
    _menu_items.pop(pid, None)
    _refresh_flags()


def _matches(name, substr, event_name):
    return name == str(event_name) or (substr and name in str(event_name))


def _subscribed(pid, event_name):
    hooks = _event_hooks.get(pid)
    if not hooks:
        return False
    for name, (substr, _prio) in hooks.items():
        if _matches(name, substr, event_name):
            return True
    return False


def _ordered(event_name):
    out = []
    for pid, plugin in list(_plugins().items()):
        if _subscribed(pid, event_name):
            prio = 0
            for name, (substr, p) in _event_hooks[pid].items():
                if _matches(name, substr, event_name):
                    prio = max(prio, p)
            out.append((-prio, pid, plugin))
    out.sort(key=lambda t: (t[0], t[1]))
    return [(pid, plugin) for _p, pid, plugin in out]


def execute_send_message(account, params):
    """Devuelve (cancelar, params). `params` es el SendMessageParams de Telegram."""
    _set_hook_account(account)
    for pid, plugin in _ordered("send_message_hook"):
        try:
            res = plugin.on_send_message_hook(account, params)
            strat = getattr(res, "strategy", "DEFAULT")
            if strat == "CANCEL":
                return True, params
            if strat in ("MODIFY", "MODIFY_FINAL") and getattr(res, "params", None) is not None:
                params = res.params
                if strat == "MODIFY_FINAL":
                    break
        except Exception:
            log("[%s] on_send_message_hook error: %s" % (pid, traceback.format_exc()))
    return False, params


def execute_update(account, update_name, update):
    _set_hook_account(account)
    for pid, plugin in _ordered(update_name):
        try:
            res = plugin.on_update_hook(update_name, account, update)
            strat = getattr(res, "strategy", "DEFAULT")
            if strat == "CANCEL":
                return None
            if strat in ("MODIFY", "MODIFY_FINAL") and getattr(res, "update", None) is not None:
                update = res.update
                if strat == "MODIFY_FINAL":
                    break
        except Exception:
            log("[%s] on_update_hook error: %s" % (pid, traceback.format_exc()))
    return update


def execute_updates(account, container_name, updates):
    _set_hook_account(account)
    for pid, plugin in _ordered(container_name):
        try:
            res = plugin.on_updates_hook(container_name, account, updates)
            strat = getattr(res, "strategy", "DEFAULT")
            if strat == "CANCEL":
                return None
            if strat in ("MODIFY", "MODIFY_FINAL") and getattr(res, "updates", None) is not None:
                updates = res.updates
                if strat == "MODIFY_FINAL":
                    break
        except Exception:
            log("[%s] on_updates_hook error: %s" % (pid, traceback.format_exc()))
    return updates


def execute_pre_request(account, request_name, request):
    for pid, plugin in _ordered(request_name):
        try:
            res = plugin.pre_request_hook(request_name, account, request)
            strat = getattr(res, "strategy", "DEFAULT")
            if strat == "CANCEL":
                return None
            if strat in ("MODIFY", "MODIFY_FINAL") and getattr(res, "request", None) is not None:
                request = res.request
                if strat == "MODIFY_FINAL":
                    break
        except Exception:
            log("[%s] pre_request_hook error: %s" % (pid, traceback.format_exc()))
    return request


def execute_post_request(account, request_name, response, error):
    for pid, plugin in _ordered(request_name):
        try:
            res = plugin.post_request_hook(request_name, account, response, error)
            strat = getattr(res, "strategy", "DEFAULT")
            if strat in ("MODIFY", "MODIFY_FINAL"):
                if getattr(res, "response", None) is not None:
                    response = res.response
                if getattr(res, "error", None) is not None:
                    error = res.error
                if strat == "MODIFY_FINAL":
                    break
        except Exception:
            log("[%s] post_request_hook error: %s" % (pid, traceback.format_exc()))
    return response, error


def execute_app_event(event):
    for pid, plugin in list(_plugins().items()):
        try:
            plugin.on_app_event(event)
        except Exception:
            log("[%s] on_app_event error: %s" % (pid, traceback.format_exc()))


def menu_items_for(menu_type):
    out = []
    for pid, items in _menu_items.items():
        for data in items.values():
            if data.get("menu_type") == menu_type:
                out.append(data)
    out.sort(key=lambda d: -int(d.get("priority") or 0))
    return out


def process_updates(account, updates):
    """Llamado desde MessagesController.processUpdates. False = descartar todo el paquete."""
    res = execute_updates(account, updates.getClass().getSimpleName(), updates)
    if res is None:
        return False
    try:
        lst = updates.updates
    except Exception:
        return True
    try:
        i = 0
        while i < lst.size():
            u = lst.get(i)
            r = execute_update(account, u.getClass().getSimpleName(), u)
            if r is None:
                lst.remove(u)
                continue
            if r is not u:
                lst.set(i, r)
            i += 1
    except Exception:
        log("process_updates error: %s" % traceback.format_exc())
    return True


class _Bus(dynamic_proxy(jclass("org.telegram.messenger.IthaHooks$Bus"))):
    def sendMessage(self, account, params):
        try:
            cancel, params = execute_send_message(int(account), params)
            return None if cancel else params
        except Exception:
            log("bus.sendMessage error: %s" % traceback.format_exc())
            return params

    def updates(self, account, updates):
        try:
            return bool(process_updates(int(account), updates))
        except Exception:
            log("bus.updates error: %s" % traceback.format_exc())
            return True

    def appEvent(self, event):
        try:
            execute_app_event(str(event))
        except Exception:
            log("bus.appEvent error: %s" % traceback.format_exc())


_bus = _Bus()
try:
    IthaHooks.bus = _bus
except Exception as e:
    log("no se pudo instalar el bus: %s" % e)


# ---- menus ----
_MENU_BASE = 100000
_menu_actions = {}   # menu_type -> {opcion: data}  (cada tipo de menu tiene su propia lista)


def _icon_id(name):
    R = jclass("org.telegram.messenger.R$drawable")
    for n in (name, "msg_info", "msg_settings"):
        if not n:
            continue
        try:
            return int(getattr(R, str(n)))
        except Exception:
            continue
    return 0


def _context(fragment, message):
    ctx = {"fragment": fragment, "message": message, "messageObject": message}
    try:
        ctx["account"] = int(fragment.getCurrentAccount())
    except Exception:
        ctx["account"] = jclass("org.telegram.messenger.UserConfig").selectedAccount
    try:
        ctx["context"] = fragment.getParentActivity()
    except Exception:
        pass
    dialog_id = 0
    try:
        dialog_id = int(fragment.getDialogId())
    except Exception:
        pass
    if not dialog_id:
        # el perfil de una persona o grupo no siempre devuelve el id: se saca de sus argumentos
        try:
            args = fragment.getArguments()
            if args is not None:
                uid = int(args.getLong("user_id", 0))
                cid = int(args.getLong("chat_id", 0))
                did = int(args.getLong("dialog_id", 0))
                dialog_id = uid or (-cid if cid else 0) or did
        except Exception:
            pass
    ctx["dialog_id"] = dialog_id
    ctx["dialogId"] = dialog_id
    return ctx


_MVEL_WARNED = set()


def _condition_ok(data, ctx):
    """Evalua la `condition` de un item de menu: funcion o texto estilo MVEL (aproximado)."""
    cond = data.get("condition")
    if cond is None or cond == "":
        return True
    try:
        if callable(cond):
            return bool(cond(ctx))
        import re
        expr = str(cond)
        expr = expr.replace("&&", " and ").replace("||", " or ")
        expr = re.sub(r"!(?!=)", " not ", expr)
        expr = re.sub(r"\btrue\b", "True", expr)
        expr = re.sub(r"\bfalse\b", "False", expr)
        expr = re.sub(r"\bnull\b", "None", expr)
        return bool(eval(expr, {"__builtins__": {}}, dict(ctx)))
    except Exception:
        key = str(data.get("item_id"))
        if key not in _MVEL_WARNED:
            _MVEL_WARNED.add(key)
            log("condition de '%s' no se pudo evaluar, se muestra siempre: %s" % (key, traceback.format_exc().strip().splitlines()[-1]))
        return True


def fill_menu(menu_type, fragment, message, icons, items, options):
    actions = _menu_actions[menu_type] = {}
    Integer = jclass("java.lang.Integer")
    ctx = _context(fragment, message)
    n = 0
    for data in menu_items_for(menu_type):
        if not _condition_ok(data, ctx):
            continue
        opt = _MENU_BASE + n
        n += 1
        actions[opt] = data
        items.add(str(data.get("text") or ""))
        options.add(jint(opt))
        icons.add(jint(_icon_id(data.get("icon"))))


def click_menu(menu_type, fragment, option, message):
    data = _menu_actions.get(menu_type, {}).get(int(option))
    if data is None:
        return False
    try:
        cb = data.get("on_click")
        if cb is not None:
            cb(_context(fragment, message))
    except Exception:
        log("menu on_click error: %s" % traceback.format_exc())
    return True


class _MenuBus(dynamic_proxy(jclass("org.telegram.messenger.IthaHooks$MenuBus"))):
    def fill(self, menu_type, fragment, message, icons, items, options):
        try:
            fill_menu(str(menu_type), fragment, message, icons, items, options)
        except Exception:
            log("menubus.fill error: %s" % traceback.format_exc())

    def click(self, menu_type, fragment, option, message):
        try:
            return bool(click_menu(str(menu_type), fragment, option, message))
        except Exception:
            log("menubus.click error: %s" % traceback.format_exc())
            return False


_menu_bus = _MenuBus()
try:
    IthaHooks.menuBus = _menu_bus
except Exception as e:
    log("no se pudo instalar menuBus: %s" % e)
