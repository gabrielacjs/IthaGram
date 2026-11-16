"""ithaengine - cargador de plugins .plugin de IthaGram."""
import ast
import os
import sys
import traceback
import types

import plugin_settings
from android_utils import log
from base_plugin import BasePlugin

_plugins = {}


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


def load_file(path):
    with open(path, "r", encoding="utf-8") as f:
        src = f.read()
    meta = _parse_meta(src)
    pid = str(meta.get("__id__") or os.path.splitext(os.path.basename(path))[0])
    if pid in _plugins:
        log("plugin '%s' ya cargado" % pid)
        return
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
    inst.name = str(meta.get("__name__", pid))
    inst.description = str(meta.get("__description__", ""))
    inst.author = str(meta.get("__author__", ""))
    inst.version = str(meta.get("__version__", ""))
    inst.icon = str(meta.get("__icon__", ""))
    inst.min_version = str(meta.get("__min_version__", ""))
    _plugins[pid] = inst
    inst.on_plugin_load()
    log("plugin cargado: %s %s" % (inst.name, inst.version))


def unload(pid):
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


def get_plugins():
    return _plugins


def reload_settings(pid):
    pass


def start(plugin_dirs, state_dir):
    plugin_settings.configure(state_dir)
    for d in plugin_dirs:
        try:
            if not d or not os.path.isdir(d):
                continue
            for name in sorted(os.listdir(d)):
                if not name.endswith(".plugin"):
                    continue
                try:
                    load_file(os.path.join(d, name))
                except Exception:
                    log("error cargando %s: %s" % (name, traceback.format_exc()))
        except Exception:
            log("error leyendo %s: %s" % (d, traceback.format_exc()))
    log("motor de plugins iniciado (%d plugins)" % len(_plugins))
