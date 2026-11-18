"""IthaGram: arreglos automaticos para que los plugins funcionen sin tocarlos.

Se activa una vez al arrancar el SDK de plugins (install). Al importar el archivo de un plugin, antes de
compilarlo, corrige patrones que fallan en Android reciente:

  * Colores: Chaquopy convierte un int de Python en el overload "long" de setTextColor/setBackgroundColor/...
    (ColorLong) y revienta con 'Invalid ID'. Se envuelve el argumento con jint(...) y se pasa a entero con signo.

Si algo falla al transformar, el plugin se compila tal cual (nunca se rompe por esto).
"""
import ast
import builtins
import importlib.machinery as _machinery

# metodos que reciben UN color como unico argumento
_COLOR_METHODS = {
    "setTextColor", "setBackgroundColor", "setColor", "setHintTextColor", "setLinkTextColor",
    "setHighlightColor", "setCardBackgroundColor", "setStrokeColor", "setTint", "drawColor",
    "setBackgroundTintList_color", "setTitleTextColor", "setSubtitleTextColor",
}
_HELPER = "__itha_color__"
_installed = False

# Nombres que algunos plugins importan de un modulo del SDK donde aqui no existen, pero si estan en otro.
# (modulo destino, nombre, modulo origen)
_ALIASES = [
    ("android_utils", "get_context", "client_utils"),
    ("hook_utils", "MethodHook", "base_plugin"),
]


def _apply_aliases():
    """Cada modulo destino recibe un __getattr__ (PEP 562) que busca el nombre en el modulo origen.
    Solo se consulta si el nombre NO existe ya en el destino, y el origen se importa recien al usarlo."""
    import importlib
    wanted = {}
    for dst, name, src in _ALIASES:
        wanted.setdefault(dst, {})[name] = src
    for dst, names in wanted.items():
        try:
            module = importlib.import_module(dst)
            previous = getattr(module, "__getattr__", None)

            def lookup(attr, names=names, previous=previous, dst=dst):
                if attr in names:
                    return getattr(importlib.import_module(names[attr]), attr)
                if previous is not None:
                    return previous(attr)
                raise AttributeError("module '%s' has no attribute '%s'" % (dst, attr))

            module.__getattr__ = lookup
        except Exception:
            pass


def _itha_color(v):
    """Si es un int de Python lo vuelve jint (entero de 32 bits con signo); lo demas pasa sin tocar."""
    if isinstance(v, int) and not isinstance(v, bool):
        from java import jint
        v &= 0xFFFFFFFF
        if v > 0x7FFFFFFF:
            v -= 1 << 32
        return jint(v)
    return v


class _ColorFix(ast.NodeTransformer):
    changed = 0

    def visit_Call(self, node):
        self.generic_visit(node)
        f = node.func
        if isinstance(f, ast.Attribute) and f.attr in _COLOR_METHODS and len(node.args) == 1 and not node.keywords \
                and not isinstance(node.args[0], ast.Starred):
            arg = node.args[0]
            wrapped = ast.Call(func=ast.Name(id=_HELPER, ctx=ast.Load()), args=[arg], keywords=[])
            ast.copy_location(wrapped, arg)
            node.args[0] = wrapped
            self.changed += 1
        return node


def transform_source(data, path="<plugin>"):
    """Devuelve el codigo compilado con los arreglos aplicados, o None si no hace falta / no se pudo."""
    try:
        text = data.decode("utf-8") if isinstance(data, (bytes, bytearray)) else data
        tree = ast.parse(text, path)
        fix = _ColorFix()
        fix.visit(tree)
        if not fix.changed:
            return None
        ast.fix_missing_locations(tree)
        return compile(tree, path, "exec", dont_inherit=True)
    except Exception:
        return None


def _is_plugin_source(data):
    return b"BasePlugin" in data and b"class BasePlugin" not in data


def _ensure_tmp():
    """Chaquopy apunta TMPDIR a cache/chaquopy/tmp pero no crea la carpeta: sin ella tempfile falla en los plugins."""
    try:
        import os
        import tempfile
        d = os.environ.get("TMPDIR")
        if d:
            os.makedirs(d, exist_ok=True)
        tempfile.tempdir = None  # que vuelva a calcular la carpeta temporal
    except Exception:
        pass


def install():
    """Engancha el cargador de modulos .py. Es seguro llamarlo varias veces."""
    global _installed
    if _installed:
        return
    _installed = True
    _ensure_tmp()
    setattr(builtins, _HELPER, _itha_color)
    _apply_aliases()
    original = _machinery.SourceFileLoader.get_code

    def get_code(self, fullname):
        try:
            path = self.get_filename(fullname)
            if path.endswith(".py"):
                data = self.get_data(path)
                if _is_plugin_source(data):
                    code = transform_source(data, path)
                    if code is not None:
                        return code  # se salta el .pyc (podria ser una copia sin arreglar)
        except Exception:
            pass
        return original(self, fullname)

    _machinery.SourceFileLoader.get_code = get_code
