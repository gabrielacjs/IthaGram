import asyncio
import html
import io
import logging
import os
import random
import re

from telegram import (InlineKeyboardButton, InlineKeyboardMarkup,
                      InputMediaPhoto, Update)
from telegram.ext import (Application, CallbackQueryHandler, CommandHandler,
                          ContextTypes, MessageHandler, filters)

from mockup import generar, generar_combo, hoja_logos, leer_logo, lista_logos
from portada import generar_portada
from preview_skel import pantalla_pc_tema, pantallas_tema

# IDs permitidos separados por coma (tú y tus amigos). Vacío = cualquiera.
PERMITIDOS = {int(x) for x in os.getenv("ALLOWED_IDS", "").replace(" ", "").split(",") if x}

# Publicación en el canal: solo el dueño puede publicar (aunque el bot sea privado para más gente)
CANAL = os.getenv("CHANNEL_ID", "@IthaGram")
DUENO = int(os.getenv("OWNER_ID", "5209800309"))
LINK_RE = re.compile(r"(?:https?://)?t\.me/addtheme/[A-Za-z0-9_\-]+")

log = logging.getLogger("bot")

EFECTOS = {
    "circulos": "⚪ Círculos",
    "estrellas": "⭐ Estrellas",
    "destellos": "✨ Destellos",
    "corazones": "💗 Corazones",
    "copos": "❄️ Copos",
    "ninguno": "🚫 Ninguno",
}


def autorizado(update: Update) -> bool:
    return not PERMITIDOS or update.effective_user.id in PERMITIDOS


# ---------- teclados ----------
def _filas_efectos(prefijo: str, actual: str):
    botones = [InlineKeyboardButton(("✅ " if k == actual else "") + v, callback_data=f"{prefijo}:{k}")
               for k, v in EFECTOS.items()]
    return [botones[i:i + 2] for i in range(0, len(botones), 2)]


def kb_elegir(actual: str) -> InlineKeyboardMarkup:
    """Menú inicial (antes de generar)."""
    return InlineKeyboardMarkup(_filas_efectos("fx", actual))


def kb_foto() -> InlineKeyboardMarkup:
    """Botones bajo la imagen ya generada."""
    return InlineKeyboardMarkup([[
        InlineKeyboardButton("🎨 Cambiar efecto", callback_data="ver"),
        InlineKeyboardButton("📎 Sin compresión", callback_data="doc"),
    ], [
        InlineKeyboardButton("📢 Publicar en canal", callback_data="pub:gen"),
    ]])


def kb_editar(actual: str) -> InlineKeyboardMarkup:
    """Menú bajo la imagen: al elegir, se edita la misma imagen (sin mensajes nuevos)."""
    filas = _filas_efectos("ed", actual)
    filas.append([InlineKeyboardButton("🎲 Otra distribución", callback_data="ed:otra"),
                  InlineKeyboardButton("⬅️ Volver", callback_data="ed:volver")])
    return InlineKeyboardMarkup(filas)


def kb_logos(n: int) -> InlineKeyboardMarkup:
    """Botones numerados (uno por plantilla) + 'Sin logo'."""
    nums = [InlineKeyboardButton(str(i), callback_data=f"lg:{i}") for i in range(1, n + 1)]
    filas = [nums[i:i + 6] for i in range(0, len(nums), 6)]
    filas.append([InlineKeyboardButton("🚫 Sin logo", callback_data="lg:none")])
    return InlineKeyboardMarkup(filas)


# ---------- comandos ----------
async def start(update: Update, context: ContextTypes.DEFAULT_TYPE):
    if not autorizado(update):
        return
    context.user_data["imgs"] = []
    context.user_data["listo"] = False
    context.user_data["modo"] = None
    await update.message.reply_text(
        "1️⃣ Mándame las *capturas*:\n"
        "   • *Celulares* (verticales): las 2 juntas en un álbum.\n"
        "   • *PC* (apaisada): una sola captura.\n"
        "2️⃣ Luego eliges el *logo* de las plantillas (o mandas el tuyo, o /sinlogo).\n"
        "3️⃣ Elige el efecto y listo.\n\n"
        "El bot detecta solo: si la 1ª imagen es más ancha que alta → modo PC.\n"
        "🖥📱 Si el tema es para *PC y celular*: usa /combo y manda 1 captura de PC + 2 de celular.\n"
        "Tip: si las mandas como *archivo* no pierden calidad.\n"
        "/reset empezar de nuevo · /id tu ID",
        parse_mode="Markdown")


async def _attheme_citado(update: Update):
    """Si el comando cita un .attheme, devuelve sus bytes; si no, None."""
    reply = update.message.reply_to_message
    doc = getattr(reply, "document", None) if reply else None
    if not doc or not (doc.file_name or "").lower().endswith(".attheme"):
        return None
    archivo = await doc.get_file()
    return bytes(await archivo.download_as_bytearray())


async def combo(update: Update, context: ContextTypes.DEFAULT_TYPE):
    if not autorizado(update):
        return
    context.user_data["imgs"] = []
    context.user_data["listo"] = False
    context.user_data["modo"] = "combo"
    tema = await _attheme_citado(update)
    if tema is not None:
        # Sin capturas: las pantallas se dibujan con los colores del tema y siguen el flujo normal (logo, efecto)
        try:
            pc_, lista_, chat_ = await asyncio.to_thread(pantallas_tema, tema)
        except Exception:
            log.exception("No pude dibujar las pantallas del tema")
            await update.message.reply_text("❌ No pude armar las pantallas con ese tema.")
            return
        await update.message.reply_text("🎨 Dibujé las pantallas desde el tema (PC + 2 celulares).")
        await procesar(update.message, context, [pc_, lista_, chat_])
        return
    await update.message.reply_text(
        "🖥📱 Modo *combo*: mándame 1 captura de *PC* (horizontal) + 2 de *celular* (verticales).\n"
        "Pueden ir juntas en un álbum o por separado, en cualquier orden. Luego eliges el *logo*.\n"
        "💡 También podés citar un tema .attheme y mandar /combo: dibujo las pantallas yo.",
        parse_mode="Markdown")


async def pc(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """Fuerza el modo PC (1 captura) aunque la imagen no sea claramente apaisada."""
    if not autorizado(update):
        return
    context.user_data["imgs"] = []
    context.user_data["listo"] = False
    context.user_data["modo"] = "pc"
    tema = await _attheme_citado(update)
    if tema is not None:
        try:
            pc_ = await asyncio.to_thread(pantalla_pc_tema, tema)
        except Exception:
            log.exception("No pude dibujar la pantalla del tema")
            await update.message.reply_text("❌ No pude armar la pantalla con ese tema.")
            return
        await update.message.reply_text("🎨 Dibujé la pantalla de PC desde el tema.")
        await procesar(update.message, context, [pc_])
        return
    await update.message.reply_text("💻 Modo *PC*: mándame 1 captura (la tomo como PC aunque sea cuadrada). Luego eliges el *logo*.",
                                    parse_mode="Markdown")


async def cel(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """/cel: modo celular (2 capturas). Citando un .attheme, dibuja las 2 pantallas sin capturas."""
    if not autorizado(update):
        return
    context.user_data["imgs"] = []
    context.user_data["listo"] = False
    context.user_data["modo"] = "cel"
    tema = await _attheme_citado(update)
    if tema is not None:
        try:
            _pc, lista_, chat_ = await asyncio.to_thread(pantallas_tema, tema)
        except Exception:
            log.exception("No pude dibujar las pantallas del tema")
            await update.message.reply_text("❌ No pude armar las pantallas con ese tema.")
            return
        await update.message.reply_text("🎨 Dibujé las 2 pantallas de celular desde el tema.")
        await procesar(update.message, context, [lista_, chat_])
        return
    await update.message.reply_text("📱 Modo *celular*: mándame las 2 capturas (verticales). Luego eliges el *logo*.\n"
                                    "💡 O citá un tema .attheme y mandá /cel: dibujo las pantallas yo.",
                                    parse_mode="Markdown")


async def portada(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """/portada: 2 capturas de celular -> portada 16:9 con el wordmark IthaGram (sin logo, colores del tema)."""
    if not autorizado(update):
        return
    context.user_data["imgs"] = []
    context.user_data["listo"] = False
    context.user_data["modo"] = "portada"
    tema = await _attheme_citado(update)
    if tema is not None:
        try:
            _pc, lista_, chat_ = await asyncio.to_thread(pantallas_tema, tema)
        except Exception:
            log.exception("No pude dibujar las pantallas del tema")
            await update.message.reply_text("❌ No pude armar las pantallas con ese tema.")
            return
        await update.message.reply_text("🎨 Dibujé las 2 pantallas desde el tema.")
        await procesar(update.message, context, [chat_, lista_])
        return
    await update.message.reply_text(
        "🌸 Modo *portada*: mándame las 2 capturas de celular (verticales): primero el *chat* y después la *lista de chats*.\n"
        "Pueden ir juntas en un álbum. No necesita logo: el nombre IthaGram sale en la portada.\n"
        "💡 O citá un tema .attheme y mandá /portada: dibujo las pantallas yo.",
        parse_mode="Markdown")


async def mi_id(update: Update, context: ContextTypes.DEFAULT_TYPE):
    await update.message.reply_text(f"Tu ID: {update.effective_user.id}")


async def reset(update: Update, context: ContextTypes.DEFAULT_TYPE):
    if autorizado(update):
        context.user_data["imgs"] = []
        context.user_data["listo"] = False
        context.user_data["modo"] = None
        context.user_data.pop("pub", None)
        context.user_data.pop("pub_estado", None)
        await update.message.reply_text("Listo, empezamos de cero. Mándame las capturas.")


async def sin_logo(update: Update, context: ContextTypes.DEFAULT_TYPE):
    if not autorizado(update):
        return
    imgs = context.user_data.get("imgs", [])
    if not imgs:
        await update.message.reply_text("Primero mándame al menos una captura.")
        return
    falta = _faltan(context.user_data.get("modo"), imgs)
    if falta:
        await update.message.reply_text(falta)
        return
    await pedir_efecto(update.message, context)


# ---------- publicar en el canal ----------
def es_dueno(update: Update) -> bool:
    return update.effective_user is not None and update.effective_user.id == DUENO


def armar_post(link: str) -> str:
    """Texto del post (HTML). 'Apply.' lleva el link del tema."""
    l = html.escape(link, quote=True)
    return (
        "#telegram #android #windows #Desktop #IthaGram\n\n"
        "🎨 More themes at @IthaGram\n\n"
        f'🎨 Theme : <a href="{l}">Apply</a>\n\n'
        "🍒 By #𝐼𝑡ℎ𝑎𝐺𝑟𝑎𝑚 🍒"
    )


def _para_foto(datos: bytes) -> io.BytesIO:
    """Telegram acepta fotos de hasta 10 MB: si el PNG pesa mucho, se pasa a JPEG."""
    from PIL import Image
    if len(datos) > 9_000_000:
        img = Image.open(io.BytesIO(datos)).convert("RGB")
        buf = io.BytesIO()
        img.save(buf, format="JPEG", quality=92)
        datos = buf.getvalue()
    f = io.BytesIO(datos)
    f.name = "preview.png"
    return f


def kb_publicar() -> InlineKeyboardMarkup:
    return InlineKeyboardMarkup([[
        InlineKeyboardButton("✅ Publicar", callback_data="pub:ok"),
        InlineKeyboardButton("❌ Cancelar", callback_data="pub:no"),
    ]])


async def publicar_cmd(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """/publicar: mandas una preview ya hecha + el link del tema."""
    if not es_dueno(update):
        return
    context.user_data["pub"] = {}
    context.user_data["pub_estado"] = "foto"
    await update.message.reply_text("📢 Mándame la *preview* (imagen o archivo). Luego te pido el link del tema.\n"
                                    "/cancelar para salir.", parse_mode="Markdown")


async def cancelar(update: Update, context: ContextTypes.DEFAULT_TYPE):
    if not es_dueno(update):
        return
    context.user_data.pop("pub", None)
    context.user_data.pop("pub_estado", None)
    await update.message.reply_text("Publicación cancelada.")


async def recibir_texto(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """Recibe el link del tema cuando se está armando un post."""
    if not es_dueno(update) or context.user_data.get("pub_estado") != "link":
        return
    m = LINK_RE.search(update.message.text or "")
    if not m:
        await update.message.reply_text("No veo un link tipo t.me/addtheme/... Mándalo de nuevo o /cancelar.")
        return
    link = m.group(0)
    if not link.startswith("http"):
        link = "https://" + link
    pub = context.user_data.setdefault("pub", {})
    pub["caption"] = armar_post(link)
    context.user_data["pub_estado"] = "confirmar"
    await update.message.reply_photo(_para_foto(pub["png"]), caption=pub["caption"],
                                     parse_mode="HTML", reply_markup=kb_publicar())


# ---------- recepción de imágenes ----------
CAPTURAS = {"pc": 1, "cel": 2, "combo": 3, "portada": 2}   # capturas que necesita cada modo (sin contar el logo)


async def pedir_logo(msg, context: ContextTypes.DEFAULT_TYPE, intro: str = ""):
    """Muestra las plantillas numeradas para elegir. También vale mandar un logo propio."""
    n = len(lista_logos())
    if n == 0:   # no hay carpeta logos/: flujo clásico
        await msg.reply_text(intro + "\nMándame el *logo* (o /sinlogo).", parse_mode="Markdown")
        return
    hoja = await asyncio.to_thread(hoja_logos, 5)
    await msg.reply_photo(
        _archivo(hoja),
        caption=f"{intro}\n🖼 Elige un logo con los botones, o mándame el tuyo como imagen.".strip(),
        reply_markup=kb_logos(n))


async def pedir_efecto(msg, context: ContextTypes.DEFAULT_TYPE):
    context.user_data["listo"] = True
    if context.user_data.get("modo") == "portada":     # la portada es limpia: por defecto sin adornos
        actual = context.user_data.get("efecto_portada", "ninguno")
    else:
        actual = context.user_data.get("efecto", "circulos")
    await msg.reply_text("🎨 Elige el efecto del fondo:", reply_markup=kb_elegir(actual))


def _es_apaisada(datos: bytes) -> bool:
    from PIL import Image
    import io
    img = Image.open(io.BytesIO(datos))
    return img.width > img.height


MARGEN_MIN = 0.08   # margen vacío grande (>= 8%): siempre se recorta
OSCURO = 18         # luminancia por debajo de la cual un píxel cuenta como "marco negro" (los temas oscuros suelen ser más claros que esto)


def _lado_marco(frac, lum, n: int) -> int:
    """Cuántas líneas del borde (de afuera hacia adentro) son marco oscuro.
    frac[i]: fracción (0-255) de píxeles oscuros de la línea i; lum[i]: luminancia media de la línea i.
    Un marco con degradado (negro que se aclara poco a poco hacia el contenido) se quita completo."""
    m = 0
    while m < n // 3 and frac[m] >= 230:          # línea (casi) toda oscura
        m += 1
    if m == 0:
        return 0
    if m < n // 3 and frac[m] >= 80:              # línea de transición (antialias): también se va
        m += 1
    vent = lum[m:m + max(40, n // 8)]
    vent = sorted(vent)
    ref = vent[len(vent) // 2] if vent else 0     # brillo típico del contenido que hay detrás
    # marco grande, o marco fino alrededor de un contenido CLARO (en tema oscuro un borde fino no es marco)
    if not (m / n >= MARGEN_MIN or ref >= 120):
        return 0
    # degradado: seguir mientras la línea sea bastante más oscura que el contenido
    tope = int(n * 0.12)
    while m < tope and m < n // 3 and lum[m] < 0.9 * ref:
        m += 1
    return m


def _recortar_bordes(datos: bytes) -> bytes:
    """Limpia los bordes de una captura:
    1) PNG con transparencia: recorta a la parte sólida (la sombra/halo semitransparente se iba a negro
       al pasar la imagen a RGB y salía como marco negro).
    2) Marco negro de una captura de PC (p. ej. bordes negros de 3-4% alrededor de un tema claro, o un PNG
       cuadrado con la ventana en el medio).
    Las capturas verticales (celular) nunca pierden bordes oscuros: en tema oscuro la barra de estado y el
    "Cancelar" quedan pegados a un borde negro y al recortarlo se cortarían."""
    from PIL import Image, ImageChops
    try:
        img = Image.open(io.BytesIO(datos))
        tiene_alfa = img.mode in ("RGBA", "LA") or (img.mode == "P" and "transparency" in img.info)
        base = img.convert("RGBA") if tiene_alfa else img.convert("RGB")
        w0, h0 = base.size
        cambio = False

        # 1) transparencia: quedarse con lo sólido
        if tiene_alfa:
            solido = base.getchannel("A").point(lambda v: 255 if v >= 230 else 0).getbbox()
            if solido and solido != (0, 0, w0, h0) and (solido[2] - solido[0]) >= 64 and (solido[3] - solido[1]) >= 64:
                base = base.crop(solido)
                w0, h0 = base.size
                cambio = True

        def _salida():
            if not cambio:
                return datos
            buf = io.BytesIO()
            base.save(buf, format="PNG")
            return buf.getvalue()

        if h0 > w0 * 1.25:      # vertical = celular: solo el recorte de transparencia
            return _salida()

        # 2) marco negro (solo no-verticales)
        L = base.convert("L")
        marco = L.point(lambda v: 255 if v < OSCURO else 0)
        if tiene_alfa:
            marco = ImageChops.lighter(marco, base.getchannel("A").point(lambda v: 255 if v < 20 else 0))
        fcol = list(marco.resize((w0, 1), Image.BOX).getdata())
        frow = list(marco.resize((1, h0), Image.BOX).getdata())
        lcol = list(L.resize((w0, 1), Image.BOX).getdata())
        lrow = list(L.resize((1, h0), Image.BOX).getdata())
        l = _lado_marco(fcol, lcol, w0)
        r = w0 - _lado_marco(fcol[::-1], lcol[::-1], w0)
        t = _lado_marco(frow, lrow, h0)
        b = h0 - _lado_marco(frow[::-1], lrow[::-1], h0)
        if (l, t, r, b) == (0, 0, w0, h0) or (r - l) < 64 or (b - t) < 64:
            return _salida()
        base = base.crop((l, t, r, b))
        cambio = True
        return _salida()
    except Exception:
        log.exception("No pude recortar bordes; uso la imagen original")
        return datos


def _faltan(modo, imgs) -> str | None:
    """Mensaje si todavía no hay suficientes capturas para el modo (sin contar el logo)."""
    if modo == "combo" and len(imgs) < 3:
        return "En modo combo necesitás 3 capturas: 1 de PC + 2 de celular. Mandá las que faltan o /reset."
    if modo == "portada" and len(imgs) < 2:
        return "En modo portada necesitás las 2 capturas (chat y lista). Mandá la que falta o /reset."
    if modo == "cel" and len(imgs) < 2:
        return "En modo celular necesitás las 2 capturas. Mandá la segunda o /reset."
    return None


async def procesar(msg, context: ContextTypes.DEFAULT_TYPE, nuevas: list):
    imgs = context.user_data.setdefault("imgs", [])
    imgs.extend(nuevas)

    # detectar modo con la primera imagen, ya sin márgenes negros (combo solo con /combo, PC forzado con /pc)
    if context.user_data.get("modo") is None and imgs:
        imgs[0] = _recortar_bordes(imgs[0])
        context.user_data["modo"] = "pc" if _es_apaisada(imgs[0]) else "cel"
    modo = context.user_data["modo"]
    # recortamos solo las capturas (no el logo, que va después)
    for i in range(min(len(imgs), CAPTURAS[modo])):
        imgs[i] = _recortar_bordes(imgs[i])

    if modo == "combo":
        # 3 capturas (1 PC + 2 celulares) + logo
        del imgs[4:]
        n = len(imgs)
        if n >= 3:
            trio = imgs[:3]
            pcs = [i for i in trio if _es_apaisada(i)]
            cels = [i for i in trio if not _es_apaisada(i)]
            if len(pcs) != 1:
                imgs.clear()
                await msg.reply_text(
                    "⚠️ Necesito exactamente 1 captura de PC (horizontal) y 2 de celular (verticales). "
                    "Mándalas de nuevo.")
                return
            imgs[:3] = pcs + cels          # orden fijo: PC, celular 1, celular 2
        if n < 3:
            await msg.reply_text(f"📦 Captura {n}/3 recibida (1 PC + 2 celulares).")
        elif n == 3:
            await pedir_logo(msg, context, "🖥📱 Capturas listas.")
        else:
            await pedir_efecto(msg, context)
        return

    if modo == "portada":
        # 2 capturas (chat, lista) y nada más: la portada no lleva logo
        del imgs[2:]
        if len(imgs) == 1:
            await msg.reply_text("🌸 Captura 1 recibida. Mándame la *captura 2* (la lista de chats).", parse_mode="Markdown")
        else:
            await pedir_efecto(msg, context)
        return

    # celular: 2 capturas + logo. PC: 1 captura + logo.
    del imgs[3:]
    n = len(imgs)

    if modo == "pc":
        # PC es siempre 1 captura. Si llegó de más, nos quedamos con la primera y la última (logo).
        if len(imgs) > 2:
            del imgs[1:-1]
        if len(imgs) == 1:
            await pedir_logo(msg, context, "💻 Captura de PC recibida.")
        else:
            # la segunda imagen es el logo
            await pedir_efecto(msg, context)
    else:
        # modo celular
        if n == 1:
            await msg.reply_text("📱 Captura 1 recibida. Mándame la *captura 2*.", parse_mode="Markdown")
        elif n == 2:
            await pedir_logo(msg, context, "📱 Capturas listas.")
        else:
            await pedir_efecto(msg, context)


async def cerrar_grupo(msg, context: ContextTypes.DEFAULT_TYPE, gid: str):
    """Espera un instante a que lleguen todas las fotos del álbum y las procesa juntas."""
    try:
        await asyncio.sleep(1.5)
    except asyncio.CancelledError:
        return
    try:
        lista = context.user_data.get("grupos", {}).pop(gid, [])
        context.user_data.get("tareas", {}).pop(gid, None)
        if lista:
            await procesar(msg, context, [d for _, d in sorted(lista, key=lambda x: x[0])])
    except Exception:
        log.exception("Error cerrando álbum")


async def recibir(update: Update, context: ContextTypes.DEFAULT_TYPE):
    if not autorizado(update):
        return
    msg = update.message
    # /publicar: la imagen que llega es la preview del post, no una captura para el mockup
    if es_dueno(update) and context.user_data.get("pub_estado") in ("foto", "link"):
        gid = msg.media_group_id
        if gid and context.user_data.get("pub_gid") == gid:
            return                              # resto del mismo álbum: se ignora
        if context.user_data.get("pub_estado") == "foto":
            archivo = await (msg.photo[-1] if msg.photo else msg.document).get_file()
            context.user_data.setdefault("pub", {})["png"] = bytes(await archivo.download_as_bytearray())
            context.user_data["pub_gid"] = gid
            context.user_data["pub_estado"] = "link"
            await msg.reply_text("🔗 Ahora mándame el link del tema (t.me/addtheme/...).")
        return
    if context.user_data.get("listo"):      # venía de una presentación terminada: empezar de nuevo
        context.user_data["imgs"] = []
        context.user_data["listo"] = False
        context.user_data["modo"] = None

    archivo = await (msg.photo[-1] if msg.photo else msg.document).get_file()
    datos = bytes(await archivo.download_as_bytearray())

    gid = msg.media_group_id
    if gid:
        grupos = context.user_data.setdefault("grupos", {})
        grupos.setdefault(gid, []).append((msg.message_id, datos))
        tareas = context.user_data.setdefault("tareas", {})
        if gid in tareas:
            tareas[gid].cancel()
        tareas[gid] = asyncio.create_task(cerrar_grupo(msg, context, gid))
    else:
        await procesar(msg, context, [datos])


# ---------- botones ----------
MAX_GUARDADAS = 4   # imágenes generadas que se recuerdan por usuario (para editar / sin compresión)


def _guardar(context, msg_id: int, **datos):
    """Guarda los datos de UNA imagen generada, ligados al id de su mensaje."""
    gen = context.user_data.setdefault("gen", {})
    gen.setdefault(msg_id, {}).update(datos)
    while len(gen) > MAX_GUARDADAS:
        gen.pop(next(iter(gen)))             # descarta la más vieja


def _gen(context, msg_id: int):
    return context.user_data.get("gen", {}).get(msg_id)


async def _png(imgs, modo: str, efecto: str, semilla: int):
    if modo == "portada":
        return await asyncio.to_thread(generar_portada, imgs[0], imgs[1], efecto, semilla)
    if modo == "combo":
        logo = imgs[3] if len(imgs) > 3 else None
        return await asyncio.to_thread(generar_combo, imgs[0], imgs[1], imgs[2], logo, efecto, semilla)
    if modo == "pc":
        logo = imgs[1] if len(imgs) > 1 else None
        return await asyncio.to_thread(generar, imgs[0], None, logo, efecto, semilla)
    # celular: siempre 2 capturas (+ logo opcional)
    logo = imgs[2] if len(imgs) > 2 else None
    return await asyncio.to_thread(generar, imgs[0], imgs[1] if len(imgs) > 1 else None, logo, efecto, semilla)


def _archivo(png: bytes) -> io.BytesIO:
    f = io.BytesIO(png)
    f.name = "presentacion.png"
    return f


async def botones(update: Update, context: ContextTypes.DEFAULT_TYPE):
    q = update.callback_query
    if not autorizado(update):
        await q.answer()
        return
    accion, _, valor = q.data.partition(":")
    msg_id = q.message.message_id

    # P) Publicar en el canal
    if accion == "pub":
        if not es_dueno(update):
            await q.answer("Solo la dueña del canal puede publicar.", show_alert=True)
            return
        if valor == "gen":                       # botón bajo una imagen generada
            g = _gen(context, msg_id)
            if not g:
                await q.answer("Esta imagen ya no está en memoria. Vuelve a generarla.", show_alert=True)
                return
            context.user_data["pub"] = {"png": g["png"]}
            context.user_data["pub_estado"] = "link"
            await q.answer()
            await q.message.reply_text("🔗 Mándame el link del tema (t.me/addtheme/...). /cancelar para salir.")
            return
        pub = context.user_data.get("pub") or {}
        if valor == "no":
            context.user_data.pop("pub", None)
            context.user_data.pop("pub_estado", None)
            await q.answer("Cancelado")
            await q.message.edit_caption("❌ Publicación cancelada.")
            return
        if valor == "ok":
            if not pub.get("png") or not pub.get("caption"):
                await q.answer("No hay un post pendiente. Usa /publicar.", show_alert=True)
                return
            try:
                await context.bot.send_photo(chat_id=CANAL, photo=_para_foto(pub["png"]),
                                             caption=pub["caption"], parse_mode="HTML")
            except Exception as e:
                log.exception("Error publicando en el canal")
                await q.answer(f"No pude publicar: {e}"[:190], show_alert=True)
                return
            context.user_data.pop("pub", None)
            context.user_data.pop("pub_estado", None)
            await q.answer("Publicado ✅")
            await q.message.edit_caption("✅ Publicado en el canal.")
            return

    # 0) Elección de logo (plantilla o sin logo)
    if accion == "lg":
        imgs = context.user_data.get("imgs", [])
        modo = context.user_data.get("modo") or "cel"
        if not imgs or _faltan(modo, imgs):
            await q.answer("Mándame primero las capturas.", show_alert=True)
            return
        del imgs[CAPTURAS[modo]:]            # por si ya había un logo puesto (doble toque)
        if valor != "none":
            logo = await asyncio.to_thread(leer_logo, int(valor)) if valor.isdigit() else None
            if logo:
                imgs.append(logo)
        await q.answer()
        await pedir_efecto(q.message, context)
        await q.message.delete()
        return

    # 1) Primera elección de efecto (menú que aparece antes de la imagen)
    if accion == "fx":
        imgs = context.user_data.get("imgs", [])
        if not imgs:
            await q.answer("Mándame primero las capturas.", show_alert=True)
            return
        modo = context.user_data.get("modo") or "cel"
        falta = _faltan(modo, imgs)
        if falta:
            await q.answer(falta, show_alert=True)
            return
        await q.answer()
        efecto, semilla = valor, random.randint(1, 10_000)
        context.user_data["efecto"] = efecto
        if modo == "portada":
            context.user_data["efecto_portada"] = efecto
        await q.edit_message_text("🎨 Generando tu presentación...")
        try:
            png = await _png(imgs, modo, efecto, semilla)
        except Exception:
            log.exception("Error generando")
            await q.edit_message_text("❌ No pude procesar las imágenes. Prueba con /reset.")
            return
        enviado = await q.message.reply_photo(_archivo(png), reply_markup=kb_foto())
        # todo lo de esta imagen queda ligado a SU mensaje (así cada botón actúa sobre su propia imagen)
        _guardar(context, enviado.message_id, imgs=imgs, modo=modo, png=png, efecto=efecto, semilla=semilla)
        context.user_data.update(imgs=[], listo=False, modo=None)
        await q.message.delete()
        return

    # 2) Mostrar el menú de efectos bajo la imagen
    if accion == "ver":
        g = _gen(context, msg_id)
        await q.answer()
        await q.edit_message_reply_markup(kb_editar(g["efecto"] if g else context.user_data.get("efecto", "circulos")))
        return

    # 3) Enviar la versión sin compresión DE ESTA imagen
    if accion == "doc":
        g = _gen(context, msg_id)
        if not g:
            await q.answer("Esta imagen ya no está en memoria (el bot se reinició o es muy antigua). "
                           "Vuelve a generarla.", show_alert=True)
            return
        await q.answer()
        await q.message.reply_document(_archivo(g["png"]), caption="Versión sin compresión")
        return

    # 4) Cambiar efecto editando la MISMA imagen
    if accion == "ed":
        if valor == "volver":
            await q.answer()
            await q.edit_message_reply_markup(kb_foto())
            return
        g = _gen(context, msg_id)
        if not g:
            await q.answer("Esta imagen ya no está en memoria. Vuelve a generarla.", show_alert=True)
            return
        efecto, semilla = g["efecto"], g["semilla"]
        if valor == "otra":
            semilla = random.randint(1, 10_000)
        elif valor in EFECTOS:
            if valor == efecto:
                await q.answer()            # mismo efecto: nada que cambiar
                return
            efecto = valor
        await q.answer("Generando…")
        try:
            png = await _png(g["imgs"], g["modo"], efecto, semilla)
            await q.message.edit_media(InputMediaPhoto(_archivo(png)), reply_markup=kb_foto())
            g.update(png=png, efecto=efecto, semilla=semilla)
            context.user_data["efecto_portada" if g["modo"] == "portada" else "efecto"] = efecto
        except Exception:
            log.exception("Error cambiando efecto")


def register(app):
    """Engancha los handlers del mockup/preview/publicar (mismos que tenía el bot original)."""
    app.add_handler(CommandHandler(["mockup", "preview"], start))   # el /start viejo de este bot
    app.add_handler(CommandHandler("reset", reset))
    app.add_handler(CommandHandler("sinlogo", sin_logo))
    app.add_handler(CommandHandler("combo", combo))
    app.add_handler(CommandHandler("pc", pc))
    app.add_handler(CommandHandler("cel", cel))
    app.add_handler(CommandHandler("portada", portada))
    app.add_handler(CommandHandler("id", mi_id))
    app.add_handler(CommandHandler("publicar", publicar_cmd))
    app.add_handler(CommandHandler("cancelar", cancelar))
    app.add_handler(CallbackQueryHandler(botones, pattern=r"^(lg:|fx:|ed:|pub:|ver$|doc$)"))
    # grupo 0, DESPUÉS del creador de temas: si estás esperando un wallpaper, la foto es del tema
    app.add_handler(MessageHandler(filters.PHOTO | filters.Document.IMAGE, recibir))
    # grupo 1: solo actúa cuando se está armando un /publicar (no pisa a /config ni a la paleta)
    app.add_handler(MessageHandler(filters.TEXT & ~filters.COMMAND, recibir_texto), group=1)
