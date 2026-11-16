"""plugin_settings (IthaGram) - ajustes de cada plugin guardados en JSON."""
import json
import os

_dir = None
_cache = {}


def configure(directory):
    global _dir
    _dir = directory
    try:
        os.makedirs(directory, exist_ok=True)
    except Exception:
        pass


def _path(plugin_id):
    return os.path.join(_dir, "%s.json" % plugin_id) if _dir else None


def _load(plugin_id):
    if plugin_id in _cache:
        return _cache[plugin_id]
    data = {}
    p = _path(plugin_id)
    try:
        if p and os.path.exists(p):
            with open(p, "r", encoding="utf-8") as f:
                data = json.load(f)
    except Exception:
        data = {}
    _cache[plugin_id] = data
    return data


def get_setting(plugin_id, key, default=None):
    return _load(plugin_id).get(key, default)


def set_setting(plugin_id, key, value):
    data = _load(plugin_id)
    data[key] = value
    p = _path(plugin_id)
    if not p:
        return
    try:
        tmp = p + ".tmp"
        with open(tmp, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False)
        os.replace(tmp, p)
    except Exception:
        pass
