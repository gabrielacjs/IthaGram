"""ithasettings - pantalla de ajustes de un plugin, construida con vistas de Android."""
from java import dynamic_proxy, jarray, jclass

import ithaengine
from android_utils import OnClickListener, OnLongClickListener, dp, log, run_on_ui_thread

LinearLayout = jclass("android.widget.LinearLayout")
TextView = jclass("android.widget.TextView")
ImageView = jclass("android.widget.ImageView")
EditText = jclass("android.widget.EditText")
View = jclass("android.view.View")
Gravity = jclass("android.view.Gravity")
Mode = jclass("android.graphics.PorterDuff$Mode")
Theme = jclass("org.telegram.ui.ActionBar.Theme")
AndroidUtilities = jclass("org.telegram.messenger.AndroidUtilities")
UserConfig = jclass("org.telegram.messenger.UserConfig")
TextCheckCell = jclass("org.telegram.ui.Cells.TextCheckCell")
Builder = jclass("org.telegram.ui.ActionBar.AlertDialog$Builder")
RDrawable = jclass("org.telegram.messenger.R$drawable")
_DialogClick = jclass("android.content.DialogInterface$OnClickListener")
_CharSequence = jclass("java.lang.CharSequence")

_open = {}  # id del plugin -> (contenedor, fragment)


class _Dlg(dynamic_proxy(_DialogClick)):
    def __init__(self, fn):
        super().__init__()
        self._fn = fn

    def onClick(self, dialog, which):
        try:
            self._fn(int(which))
        except Exception as e:
            log("dialogo error: %s" % e)


def _color(name, fallback="key_windowBackgroundWhiteBlackText"):
    try:
        return int(Theme.getColor(getattr(Theme, name)))
    except Exception:
        try:
            return int(Theme.getColor(getattr(Theme, fallback)))
        except Exception:
            return 0xFF000000 - (1 << 32)


def fill_settings(pid, container, fragment):
    _open[pid] = (container, fragment)
    _rebuild(pid)


def close_settings(pid):
    _open.pop(pid, None)


def rebuild_open(pid):
    if pid in _open:
        run_on_ui_thread(lambda: _rebuild(pid))


def _rebuild(pid):
    entry = _open.get(pid)
    plugin = ithaengine.get_plugins().get(pid)
    if entry is None or plugin is None:
        return
    container, fragment = entry
    ctx = container.getContext()
    container.removeAllViews()
    try:
        items = plugin.create_settings() or []
    except Exception as e:
        log("[%s] create_settings error: %s" % (pid, e))
        items = []
    for it in items:
        if it is None:
            continue
        try:
            v = _build(ctx, plugin, it, fragment)
        except Exception as e:
            log("[%s] error construyendo %s: %s" % (pid, getattr(it, "type", "?"), e))
            v = None
        if v is not None:
            container.addView(v, LinearLayout.LayoutParams(-1, -2))


def _call(fn, *args):
    if fn is None:
        return None
    try:
        return fn(*args)
    except TypeError:
        try:
            return fn()
        except Exception as e:
            log("callback error: %s" % e)
    except Exception as e:
        log("callback error: %s" % e)
    return None


def _text(ctx, text, size, color, bold=False):
    tv = TextView(ctx)
    tv.setText(str(text))
    tv.setTextSize(float(size))
    tv.setTextColor(color)
    if bold:
        tv.setTypeface(AndroidUtilities.bold())
    return tv


def _header(ctx, it):
    tv = _text(ctx, it.text, 14, _color("key_windowBackgroundWhiteBlueHeader"), True)
    tv.setPadding(dp(20), dp(18), dp(20), dp(8))
    return tv


def _divider(ctx, it):
    if it.text:
        tv = _text(ctx, it.text, 13, _color("key_windowBackgroundWhiteGrayText"))
        tv.setPadding(dp(20), dp(10), dp(20), dp(10))
        tv.setBackgroundColor(_color("key_windowBackgroundGray"))
        return tv
    v = View(ctx)
    v.setBackgroundColor(_color("key_windowBackgroundGray"))
    v.setMinimumHeight(dp(12))
    return v


def _row(ctx, icon, title, subtitle, red=False, accent=False):
    row = LinearLayout(ctx)
    row.setOrientation(LinearLayout.HORIZONTAL)
    row.setGravity(Gravity.CENTER_VERTICAL)
    row.setBackgroundColor(_color("key_windowBackgroundWhite"))
    row.setPadding(dp(20), dp(12), dp(20), dp(12))
    if icon:
        try:
            rid = int(getattr(RDrawable, str(icon)))
            iv = ImageView(ctx)
            iv.setImageResource(rid)
            iv.setColorFilter(_color("key_windowBackgroundWhiteGrayIcon"), Mode.SRC_IN)
            row.addView(iv, LinearLayout.LayoutParams(dp(24), dp(24)))
            tvspace = View(ctx)
            row.addView(tvspace, LinearLayout.LayoutParams(dp(16), 1))
        except Exception:
            pass
    col = LinearLayout(ctx)
    col.setOrientation(LinearLayout.VERTICAL)
    if red:
        color = _color("key_text_RedRegular")
    elif accent:
        color = _color("key_windowBackgroundWhiteBlueText4")
    else:
        color = _color("key_windowBackgroundWhiteBlackText")
    col.addView(_text(ctx, title, 16, color), LinearLayout.LayoutParams(-1, -2))
    if subtitle:
        sub = _text(ctx, subtitle, 13, _color("key_windowBackgroundWhiteGrayText"))
        col.addView(sub, LinearLayout.LayoutParams(-1, -2))
    row.addView(col, LinearLayout.LayoutParams(0, -2, 1.0))
    return row


def _build(ctx, plugin, it, fragment):
    kind = getattr(it, "type", None)
    if kind == "header":
        return _header(ctx, it)
    if kind == "divider":
        return _divider(ctx, it)
    if kind == "switch":
        return _switch(ctx, plugin, it)
    if kind == "selector":
        return _selector(ctx, plugin, it, fragment)
    if kind in ("input", "edit_text"):
        return _input(ctx, plugin, it, fragment)
    if kind == "text":
        return _text_row(ctx, it)
    if kind == "custom":
        return _custom(ctx, it)
    return None


def _switch(ctx, plugin, it):
    cell = TextCheckCell(ctx)
    cell.setBackgroundColor(_color("key_windowBackgroundWhite"))
    checked = bool(plugin.get_setting(it.key, it.default))
    if it.subtext:
        cell.setTextAndValueAndCheck(str(it.text), str(it.subtext), checked, True, False)
    else:
        cell.setTextAndCheck(str(it.text), checked, False)

    def click(v):
        nv = not cell.isChecked()
        cell.setChecked(nv)
        plugin.set_setting(it.key, nv)
        _call(it.on_change, nv)

    cell.setOnClickListener(OnClickListener(click))
    if it.on_long_click:
        cell.setOnLongClickListener(OnLongClickListener(lambda v: _call(it.on_long_click, v)))
    return cell


def _selector(ctx, plugin, it, fragment):
    items = [str(x) for x in (it.items or [])]
    idx = int(plugin.get_setting(it.key, it.default))
    row = _row(ctx, it.icon, it.text, items[idx] if 0 <= idx < len(items) else "")

    def click(v):
        def chosen(which):
            plugin.set_setting(it.key, which)
            _call(it.on_change, which)
            run_on_ui_thread(lambda: _rebuild(plugin.id))

        b = Builder(ctx)
        b.setTitle(str(it.text))
        b.setItems(jarray(_CharSequence)(items), _Dlg(chosen))
        fragment.showDialog(b.create())

    row.setOnClickListener(OnClickListener(click))
    return row


def _input(ctx, plugin, it, fragment):
    key = it.key
    default = getattr(it, "default", "")
    cur = str(plugin.get_setting(key, default) or "")
    title = getattr(it, "text", None) or getattr(it, "hint", "")
    row = _row(ctx, getattr(it, "icon", None), title, cur if cur else getattr(it, "subtext", None))

    def click(v):
        et = EditText(ctx)
        et.setText(cur)
        et.setSingleLine(True)
        et.setTextColor(_color("key_dialogTextBlack"))

        def save(which):
            val = str(et.getText().toString())
            plugin.set_setting(key, val)
            _call(it.on_change, val)
            run_on_ui_thread(lambda: _rebuild(plugin.id))

        b = Builder(ctx)
        b.setTitle(str(title))
        b.setView(et)
        b.setPositiveButton("OK", _Dlg(save))
        b.setNegativeButton("Cancelar", None)
        fragment.showDialog(b.create())

    row.setOnClickListener(OnClickListener(click))
    return row


def _text_row(ctx, it):
    row = _row(ctx, it.icon, it.text, it.subtext, red=bool(it.red), accent=bool(it.accent))
    if it.on_click:
        row.setOnClickListener(OnClickListener(lambda v: _call(it.on_click, v)))
    if it.on_long_click:
        row.setOnLongClickListener(OnLongClickListener(lambda v: bool(_call(it.on_long_click, v))))
    return row


def _custom(ctx, it):
    if it.view is not None:
        return it.view
    f = it.factory
    if f is None or not hasattr(f, "create_view"):
        return None
    account = int(UserConfig.selectedAccount)
    args = list(it.factory_args) if it.factory_args else []
    view = f.create_view(ctx, None, account, 0, None) if f.create_view else None
    if view is None:
        return None
    if f.bind_view:
        f.bind_view(view, None, False, None, None)
    if it.on_click:
        view.setOnClickListener(OnClickListener(lambda v: _call(it.on_click, v)))
    return view
