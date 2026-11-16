"""ithaengine - cargador de plugins .plugin de IthaGram."""
import ast
import json
import os
import shutil
import sys
import traceback
import types

import plugin_settings
from android_utils import log
from base_plugin import BasePlugin

_plugins = {}   # id -> instancia (cargados)
_known = {}     # id -> info (path, nombre, ...)
_disabled = set()
_state_file = None
_dirs = []         # carpetas de plugins (la ultima es donde se instala)
_install_dir = None


def _parse_meta(src):
    meta = {}
    try:
        tree = ast.parse(src)
        for node in tree.body:
            if isinstance(node, ast.Assign) and len(node.targets) == 1 \
                    and isinstance(node.targets[0], ast.Name):
                name = node.targets[0].id
                if name.startswith("__") and name.endswith("__"):
                    try:
                        meta[name] = ast.literal_eval(node.value)
                    except Exception:
                        pass
    except Exception:
        pass
    return meta


def _load_state():
    global _disabled
    try:
        if _state_file and os.path.exists(_state_file):
            with open(_state_file, "r", encoding="utf-8") as f:
                _disabled = set(json.load(f).get("disabled", []))
    except Exception:
        _disabled = set()


def _save_state():
    try:
        if _state_file:
            with open(_state_file, "w", encoding="utf-8") as f:
                json.dump({"disabled": sorted(_disabled)}, f)
    except Exception:
        pass


def _register(path, overwrite=True):
    """Lee los metadatos del archivo y lo anota como conocido. Devuelve el id."""
    with open(path, "r", encoding="utf-8") as f:
        src = f.read()
    meta = _parse_meta(src)
    pid = str(meta.get("__id__") or os.path.splitext(os.path.basename(path))[0])
    if pid in _known and not overwrite:
        # mismo plugin en dos carpetas: gana la copia mas nueva
        try:
            keep = os.path.getmtime(_known[pid]["path"]) >= os.path.getmtime(path)
        except Exception:
            keep = True
        if keep:
            log("plugin '%s' repetido, se ignora %s" % (pid, path))
            return pid
        log("plugin '%s' repetido, se usa la copia mas nueva %s" % (pid, path))
    _known[pid] = {
        "id": pid,
        "path": path,
        "name": str(meta.get("__name__", pid)),
        "version": str(meta.get("__version__", "")),
        "author": str(meta.get("__author__", "")),
        "description": str(meta.get("__description__", "")),
        "icon": str(meta.get("__icon__", "")),
        "min_version": str(meta.get("__min_version__", "")),
    }
    return pid


def _load(pid):
    info = _known[pid]
    path = info["path"]
    with open(path, "r", encoding="utf-8") as f:
        src = f.read()
    mod = types.ModuleType("itha_plugin_" + pid)
    mod.__file__ = path
    sys.modules[mod.__name__] = mod
    exec(compile(src, path, "exec"), mod.__dict__)
    cls = None
    for v in list(mod.__dict__.values()):
        if isinstance(v, type) and v is not BasePlugin and issubclass(v, BasePlugin):
            cls = v
    if cls is None:
        raise RuntimeError("no hay clase BasePlugin en %s" % path)
    inst = cls()
    inst.id = pid
    inst.name = info["name"]
    inst.description = info["description"]
    inst.author = info["author"]
    inst.version = info["version"]
    inst.icon = info["icon"]
    inst.min_version = info["min_version"]
    _plugins[pid] = inst
    inst.on_plugin_load()
    log("plugin cargado: %s %s" % (inst.name, inst.version))


def _unload(pid):
    inst = _plugins.pop(pid, None)
    if inst is None:
        return
    try:
        inst.on_plugin_unload()
    except Exception:
        log("error en on_plugin_unload(%s): %s" % (pid, traceback.format_exc()))
    try:
        inst._itha_unhook_all()
    except Exception:
        pass
    sys.modules.pop("itha_plugin_" + pid, None)


def _try_load(pid):
    try:
        _load(pid)
        return ""
    except Exception:
        _plugins.pop(pid, None)
        err = traceback.format_exc()
        log("error cargando %s: %s" % (pid, err))
        return "No se pudo cargar el plugin: %s" % err.strip().splitlines()[-1]


def list_plugins_json():
    out = []
    for pid, info in sorted(_known.items(), key=lambda kv: kv[1]["name"].lower()):
        inst = _plugins.get(pid)
        has_settings = False
        if inst is not None:
            try:
                has_settings = type(inst).create_settings is not BasePlugin.create_settings
            except Exception:
                pass
        out.append({
            "id": pid, "name": info["name"], "version": info["version"],
            "author": info["author"], "description": info["description"],
            "enabled": pid not in _disabled, "loaded": inst is not None,
            "has_settings": has_settings,
        })
    return json.dumps(out)


def set_enabled(pid, enabled):
    if pid not in _known:
        return "Plugin desconocido"
    if enabled:
        _disabled.discard(pid)
        _save_state()
        if pid not in _plugins:
            return _try_load(pid)
    else:
        _disabled.add(pid)
        _save_state()
        _unload(pid)
    return ""


def _peek_id(path):
    with open(path, "r", encoding="utf-8") as f:
        src = f.read()
    meta = _parse_meta(src)
    return str(meta.get("__id__") or os.path.splitext(os.path.basename(path))[0])


def _store(path, pid):
    """Copia el archivo importado a la carpeta de plugins (sobre la copia vieja si ya estaba) para que sobreviva al reinicio."""
    old = _known.get(pid)
    if old is not None:
        target = old["path"]
    elif _install_dir:
        target = os.path.join(_install_dir, pid + ".plugin")
    else:
        return path
    try:
        if os.path.abspath(target) != os.path.abspath(path):
            os.makedirs(os.path.dirname(target), exist_ok=True)
            shutil.copyfile(path, target)
        return target
    except Exception:
        log("no se pudo copiar %s: %s" % (path, traceback.format_exc()))
        return path


def import_plugin(path):
    try:
        pid = _peek_id(path)
        _register(_store(path, pid))
    except Exception as e:
        return "Archivo no valido: %s" % e
    if pid in _plugins:
        _unload(pid)
    _disabled.discard(pid)
    _save_state()
    return _try_load(pid)


def remove_plugin(pid):
    info = _known.get(pid)
    if info is None:
        return ""
    _unload(pid)
    # se borran todas las copias (si quedara una en otra carpeta, volveria al reiniciar)
    paths = {info["path"]}
    for d in _dirs:
        try:
            for name in os.listdir(d):
                if name.endswith(".plugin"):
                    p = os.path.join(d, name)
                    if _peek_id(p) == pid:
                        paths.add(p)
        except Exception:
            pass
    for p in paths:
        try:
            os.remove(p)
        except Exception:
            pass
    _known.pop(pid, None)
    _disabled.discard(pid)
    _save_state()
    return ""


def get_plugins():
    return _plugins


def reload_settings(pid):
    try:
        import ithasettings
        ithasettings.rebuild_open(pid)
    except Exception:
        log("reload_settings error: %s" % traceback.format_exc())


def fill_settings(pid, container, fragment):
    import ithasettings
    ithasettings.fill_settings(pid, container, fragment)


def close_settings(pid):
    import ithasettings
    ithasettings.close_settings(pid)


def start(plugin_dirs, state_dir):
    global _state_file, _dirs, _install_dir
    _dirs = [d for d in plugin_dirs if d]
    _install_dir = _dirs[-1] if _dirs else None
    plugin_settings.configure(state_dir)
    _state_file = os.path.join(state_dir, "_plugins_state.json")
    _load_state()
    for d in plugin_dirs:
        try:
            if not d or not os.path.isdir(d):
                continue
            for name in sorted(os.listdir(d)):
                if not name.endswith(".plugin"):
                    continue
                path = os.path.join(d, name)
                try:
                    _register(path, overwrite=False)
                except Exception:
                    log("error leyendo %s: %s" % (name, traceback.format_exc()))
        except Exception:
            log("error leyendo %s: %s" % (d, traceback.format_exc()))
    for pid in list(_known):
        if pid not in _disabled:
            _try_load(pid)
    log("motor de plugins iniciado (%d plugins)" % len(_plugins))
