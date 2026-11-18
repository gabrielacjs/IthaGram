"""IthaGram: revisa un .plugin SIN ejecutarlo y estima si funcionara. Se usa desde el dialogo de instalar."""
import ast, re, sys

APP_VERSION = "13.2.2"
from ithachecker_sdk import SDK
SDK_TOP = {m.split(".")[0] for m in SDK} | {"dev_server"}
JAVA_PREFIX = ("java", "javax", "android", "androidx", "org", "com", "kotlin", "dalvik", "jdk", "libcore", "sun")
SUPPORTED_MENUS = {"chat_action_menu", "profile_action_menu", "message_context_menu"}
MENU_CONST = {"MESSAGE_CONTEXT_MENU": "message_context_menu", "DRAWER_MENU": "drawer_menu", "MAIN_MENU": "main_menu",
              "CHAT_ACTION_MENU": "chat_action_menu", "PROFILE_ACTION_MENU": "profile_action_menu"}
BASE_METHODS = set(SDK["base_plugin"]) | {"on_plugin_load", "on_plugin_unload", "on_app_event", "get_setting", "set_setting",
    "export_settings", "import_settings", "reload_settings", "create_settings", "logger", "log", "client", "pre_request_hook",
    "post_request_hook", "on_update_hook", "on_updates_hook", "on_send_message_hook", "add_hook", "add_on_send_message_hook",
    "remove_hook", "hook_method", "hook_all_methods", "hook_all_constructors", "unhook_method", "add_menu_item",
    "remove_menu_item", "add_file_hook", "remove_file_hook", "add_intent_hook", "remove_intent_hook"}
COLOR_CALLS = {"setTextColor", "setBackgroundColor", "setColor", "setTint", "setColorFilter", "setHintTextColor", "setCardBackgroundColor"}
# Paquetes pip que la app ya trae incluidos (Chaquopy: pip install "requests" y sus dependencias)
BUNDLED_PIP = {"requests", "urllib3", "certifi", "idna", "charset-normalizer"}
BUNDLED_MODS = {"requests", "urllib3", "certifi", "idna", "charset_normalizer"}
DANGEROUS = {"eval", "exec", "compile", "__import__"}
DANGEROUS_ATTR = {("os", "system"), ("os", "popen"), ("os", "remove"), ("os", "rmdir"), ("shutil", "rmtree"), ("os", "chmod")}
DANGEROUS_MODS = {"subprocess", "ctypes", "socket", "pty", "marshal", "pickle"}
NET_MODS = {"requests", "urllib", "urllib.request", "http.client", "httpx", "aiohttp", "websocket", "websockets"}

try:
    STDLIB = set(sys.stdlib_module_names)
except AttributeError:
    STDLIB = set()

CLASS_EXISTS = None  # funcion opcional: nombre completo -> bool (en la app usa las clases reales)

_classes = None
def classes():
    global _classes
    if _classes is None:
        _classes = set()
    return _classes

def vkey(v):
    return tuple(int(x) if x.isdigit() else 0 for x in re.findall(r"\d+|[A-Za-z]+", str(v))[:4])

def analyze(source: bytes):
    """Devuelve (veredicto, rojo[], amarillo[], info[]). veredicto: 'rojo' | 'amarillo' | 'verde'."""
    red, yel, info = [], [], []
    try:
        text = source.decode("utf-8")
    except UnicodeDecodeError:
        return "rojo", ["El archivo no es texto UTF-8: no parece un plugin de Python."], [], []
    try:
        tree = ast.parse(text)
    except SyntaxError as e:
        return "rojo", [f"Error de sintaxis en la línea {e.lineno}: {e.msg}."], [], []

    # ---- metadatos ----
    meta = {}
    for n in tree.body:
        if isinstance(n, ast.Assign) and len(n.targets) == 1 and isinstance(n.targets[0], ast.Name):
            k = n.targets[0].id
            if k.startswith("__") and k.endswith("__"):
                try:
                    meta[k.strip("_")] = ast.literal_eval(n.value)
                except Exception:
                    pass
    pid = meta.get("id")
    if not pid:
        red.append("Falta `__id__`.")
    elif not re.match(r"^[a-zA-Z][a-zA-Z0-9_-]{1,31}$", str(pid)):
        red.append(f"`__id__` inválido ('{pid}'): 2-32 caracteres, empieza con letra, solo letras/números/-/_.")
    if not meta.get("name"):
        red.append("Falta `__name__`.")
    mv = meta.get("min_version")
    if mv and vkey(mv) > vkey(APP_VERSION):
        red.append(f"Pide la app {mv} o más nueva; IthaGram es {APP_VERSION}.")
    av = meta.get("app_version")
    if av:
        info.append(f"Pide app_version `{av}` (el motor la valida contra {APP_VERSION}).")
    sv = meta.get("sdk_version")
    if sv:
        info.append(f"Pide sdk_version `{sv}`.")
    req = meta.get("requirements")
    if req:
        parts = [x.strip() for x in (req.split(",") if isinstance(req, str) else list(req)) if str(x).strip()]
        norm = lambda r: re.split(r"[\s\[<>=!~;@]", str(r).strip(), maxsplit=1)[0].lower().replace("_", "-")
        extra = [r for r in parts if norm(r) not in BUNDLED_PIP]
        if extra:
            red.append(f"Necesita paquetes pip ({', '.join(extra)}); el motor todavía no instala paquetes pip.")
        else:
            info.append("Pide paquetes pip que la app ya trae incluidos: " + ", ".join(parts) + ".")
    info.insert(0, f"**{meta.get('name', '?')}** v{meta.get('version', '?')} — autor: {meta.get('author', '?')} — id: `{pid}`")

    # ---- imports ----
    py_imports, java_refs = [], set()
    for n in ast.walk(tree):
        if isinstance(n, ast.Import):
            for a in n.names:
                py_imports.append((a.name, None, n.lineno))
        elif isinstance(n, ast.ImportFrom) and n.level == 0 and n.module:
            for a in n.names:
                py_imports.append((n.module, a.name, n.lineno))
    seen = set()
    for mod, name, line in py_imports:
        top = mod.split(".")[0]
        key = (mod, name)
        if key in seen:
            continue
        seen.add(key)
        if mod in SDK or top in SDK_TOP:
            if mod not in SDK and mod.split(".")[0] in ("ui", "elyxcore", "extera_utils"):
                red.append(f"Importa `{mod}` (línea {line}) y ese módulo no existe en el SDK del motor.")
            elif mod not in SDK and top in SDK_TOP:
                red.append(f"Importa `{mod}` (línea {line}) y ese módulo no existe en el SDK del motor.")
            elif name and name != "*" and SDK[mod] and name not in SDK[mod]:
                red.append(f"`from {mod} import {name}` (línea {line}): `{name}` no existe en ese módulo del SDK.")
            if top == "elyxcore":
                yel.append("Usa `elyxcore` (framework completo); no se ha probado en IthaGram.")
            if top == "dev_server":
                yel.append("Importa `dev_server` (herramienta de desarrollo).")
        elif top in STDLIB:
            if top in DANGEROUS_MODS:
                yel.append(f"Importa `{top}` (línea {line}): acceso de bajo nivel, revisa para qué lo usa.")
            if mod in NET_MODS or top in NET_MODS:
                yel.append(f"Usa red (`{mod}`, línea {line}).")
        elif top in JAVA_PREFIX or top in ("java",):
            full = mod if not name or name == "*" else mod + "." + name
            if top in ("org", "com") and (mod.startswith("org.telegram") or mod.startswith("com.exteragram")):
                java_refs.add((full, line))
        elif top in BUNDLED_MODS:
            yel.append(f"Usa red (`{top}`, línea {line}).")
        else:
            red.append(f"Importa `{top}` (línea {line}): paquete externo no incluido en el motor.")
    for n in ast.walk(tree):  # find_class("...")
        if isinstance(n, ast.Call) and isinstance(n.func, ast.Name) and n.func.id == "find_class" and n.args \
                and isinstance(n.args[0], ast.Constant) and isinstance(n.args[0].value, str):
            v = n.args[0].value
            if v.startswith(("org.telegram", "com.exteragram")):
                java_refs.add((v, n.lineno))
    cl = classes()
    if CLASS_EXISTS is not None or cl:
        missing = []
        for full, line in sorted(java_refs):
            base = full.split("$")[0]
            cands = {base} | {".".join(base.split(".")[:i]) for i in range(2, len(base.split(".")))}
            present = any(CLASS_EXISTS(c) for c in cands) if CLASS_EXISTS is not None else bool(cands & cl)
            if not present:
                missing.append(f"`{full}` (línea {line})")
        if missing:
            yel.append("Clases de Telegram que no encuentro en IthaGram (pueden venir de otra versión): " + ", ".join(missing[:8]))
    elif java_refs:
        info.append(f"Usa {len(java_refs)} clases de Telegram (sin lista `classes.txt` no las puedo comprobar).")

    # ---- clase del plugin ----
    plugin_cls = None
    for n in tree.body:
        if isinstance(n, ast.ClassDef):
            for b in n.bases:
                bn = b.id if isinstance(b, ast.Name) else (b.attr if isinstance(b, ast.Attribute) else "")
                if bn == "BasePlugin":
                    plugin_cls = n
    if plugin_cls is None:
        red.append("No encuentro ninguna clase que herede de `BasePlugin`.")
    else:
        own = set()
        for n in ast.walk(plugin_cls):
            if isinstance(n, (ast.FunctionDef, ast.AsyncFunctionDef, ast.ClassDef)):
                own.add(n.name)
            elif isinstance(n, ast.Attribute) and isinstance(n.ctx, ast.Store) and isinstance(n.value, ast.Name) and n.value.id == "self":
                own.add(n.attr)
        for n in plugin_cls.body:
            if isinstance(n, ast.Assign):
                for t in n.targets:
                    if isinstance(t, ast.Name):
                        own.add(t.id)
        unknown = set()
        for n in ast.walk(plugin_cls):
            if isinstance(n, ast.Call) and isinstance(n.func, ast.Attribute) and isinstance(n.func.value, ast.Name) \
                    and n.func.value.id == "self" and n.func.attr not in own and n.func.attr not in BASE_METHODS:
                unknown.add(n.func.attr)
        if unknown:
            red.append("Llama a métodos de `self` que no existen en el SDK ni en el plugin: " + ", ".join(f"`{u}`" for u in sorted(unknown)) + ".")
        used = sorted({n.func.attr for n in ast.walk(plugin_cls) if isinstance(n, ast.Call) and isinstance(n.func, ast.Attribute)
                       and isinstance(n.func.value, ast.Name) and n.func.value.id == "self" and n.func.attr in BASE_METHODS
                       and n.func.attr.startswith(("add_", "hook_", "remove_", "unhook"))})
        if used:
            info.append("Enganches/APIs que usa: " + ", ".join(f"`{u}`" for u in used))
        for n in ast.walk(plugin_cls):  # tipos de menu
            if isinstance(n, ast.Attribute) and isinstance(n.value, ast.Name) and n.value.id == "MenuItemType" and n.attr in MENU_CONST:
                if MENU_CONST[n.attr] not in SUPPORTED_MENUS:
                    yel.append(f"Usa el menú `{n.attr}`, que IthaGram todavía no tiene conectado (solo chat, perfil y mensaje).")
            if isinstance(n, ast.Constant) and isinstance(n.value, str) and n.value in ("drawer_menu", "main_menu"):
                yel.append(f"Usa el menú `{n.value}`, que IthaGram todavía no tiene conectado.")

    # ---- patrones conocidos y seguridad ----
    flagged_colors, danger, urls, names_used = set(), set(), set(), set()
    for n in ast.walk(tree):
        if isinstance(n, ast.Call):
            f = n.func
            if isinstance(f, ast.Attribute) and f.attr in COLOR_CALLS and n.args:
                a = n.args[0]
                if isinstance(a, (ast.Constant, ast.UnaryOp, ast.BinOp)) and not (isinstance(a, ast.Constant) and not isinstance(a.value, int)):
                    flagged_colors.add((f.attr, n.lineno))
            if isinstance(f, ast.Name) and f.id in DANGEROUS:
                danger.add(f"`{f.id}()` (línea {n.lineno})")
            if isinstance(f, ast.Attribute) and isinstance(f.value, ast.Name) and (f.value.id, f.attr) in DANGEROUS_ATTR:
                danger.add(f"`{f.value.id}.{f.attr}()` (línea {n.lineno})")
        if isinstance(n, ast.Constant) and isinstance(n.value, str):
            for u in re.findall(r"https?://([A-Za-z0-9.-]+)", n.value):
                urls.add(u.lower())
    if flagged_colors:
        ls = ", ".join(f"{a} (línea {l})" for a, l in sorted(flagged_colors)[:5])
        yel.append(f"Pasa un número directo a un método de color ({ls}): en Android reciente puede dar 'Invalid ID'; hay que envolverlo con `jint(...)`.")
    if danger:
        yel.append("Ejecuta código dinámico o borra/cambia archivos: " + ", ".join(sorted(danger)[:6]) + ". Revisa que sea legítimo.")
    if urls:
        yel.append("Se conecta o menciona estos dominios: " + ", ".join(sorted(urls)[:8]) + ".")
    if re.search(r"(exec|eval)\s*\(\s*(base64|zlib|codecs|bytes)", text) or re.search(r"b64decode", text) and "exec" in text:
        red.append("Parece ofuscado (decodifica y ejecuta código): no lo instales.")

    if red:
        return "rojo", red, yel, info
    if yel:
        return "amarillo", red, yel, info
    return "verde", red, yel, info

def render(verdict, red, yel, info):
    head = {"rojo": "🔴 No va a funcionar (o no es seguro)",
            "amarillo": "🟡 Probablemente funciona, pero hay cosas que revisar",
            "verde": "🟢 No detecté problemas"}[verdict]
    out = [head, ""]
    if info:
        out += [info[0], ""]
    if red:
        out += ["Problemas graves:"] + [f"• {x}" for x in red] + [""]
    if yel:
        out += ["A revisar:"] + [f"• {x}" for x in dict.fromkeys(yel)] + [""]
    if len(info) > 1:
        out += [f"• {x}" for x in info[1:]] + [""]
    if verdict == "verde":
        out.append("Esto es un análisis estático: no garantiza que funcione. La prueba final es instalarlo en la app.")
    return "\n".join(out).strip()

if False:
    for p in sys.argv[1:]:
        print("=====", p)
        print(render(*analyze(open(p, "rb").read())))


_LOADER = None


def set_loader(loader):
    """La app pasa su ClassLoader para comprobar las clases de Telegram."""
    global _LOADER
    _LOADER = loader


def _java_class_exists(name):
    # Class.forName(nombre, false, ...) comprueba que exista SIN ejecutar sus inicializadores estaticos
    try:
        from java.lang import Class
        Class.forName(name, False, _LOADER)
        return True
    except Exception:
        return False

CLASS_EXISTS = _java_class_exists


def check_text(source):
    """Entrada: texto del plugin. Salida: informe en texto plano para mostrar en un dialogo."""
    data = source.encode("utf-8") if isinstance(source, str) else bytes(source)
    out = render(*analyze(data))
    return out.replace("**", "").replace("`", "")
