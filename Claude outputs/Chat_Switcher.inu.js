// ==InuPlugin==
// @name         Chat Switcher
// @id           Gabiitha.chat-switcher
// @author       Gabiitha
// @version      3.0.3
// @description  Panel lateral atajo rapido a tus contactos. 
// @icon         inu://swap
// @grant        unsafe.jvm
// @grant        unsafe.xposed
// @plugin-api   1
// @platform     android
// ==/InuPlugin==
(function () {
  const TAG = "[ChatSwitcher]";
  // ───────────── Clases ─────────────
  const ChatActivity       = inu.jvm.cls("org.telegram.ui.ChatActivity");
  const BaseFragment       = inu.jvm.cls("org.telegram.ui.ActionBar.BaseFragment");
  const ApplicationLoader  = inu.jvm.cls("org.telegram.messenger.ApplicationLoader");
  const AndroidUtilities   = inu.jvm.cls("org.telegram.messenger.AndroidUtilities");
  const MessagesController = inu.jvm.cls("org.telegram.messenger.MessagesController");
  const UserConfig         = inu.jvm.cls("org.telegram.messenger.UserConfig");
  const UserObject         = inu.jvm.cls("org.telegram.messenger.UserObject");
  const Theme              = inu.jvm.cls("org.telegram.ui.ActionBar.Theme");
  const BackupImageView    = inu.jvm.cls("org.telegram.ui.Components.BackupImageView");
  const AvatarDrawable     = inu.jvm.cls("org.telegram.ui.Components.AvatarDrawable");
  const View               = inu.jvm.cls("android.view.View");
  const Bundle             = inu.jvm.cls("android.os.Bundle");
  const TextView           = inu.jvm.cls("android.widget.TextView");
  const LinearLayout       = inu.jvm.cls("android.widget.LinearLayout");
  const FrameLayout        = inu.jvm.cls("android.widget.FrameLayout");
  const ScrollView         = inu.jvm.cls("android.widget.ScrollView");
  const FrameLP            = inu.jvm.cls("android.widget.FrameLayout$LayoutParams");
  const LinearLP           = inu.jvm.cls("android.widget.LinearLayout$LayoutParams");
  const GradientDrawable   = inu.jvm.cls("android.graphics.drawable.GradientDrawable");
  const Toast              = inu.jvm.cls("android.widget.Toast");
  const LongCls            = inu.jvm.cls("java.lang.Long");
  const JSystem            = inu.jvm.cls("java.lang.System");
  const MAKE_TOAST = "makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;";
  // ───────────── Utilidades ─────────────
  const vHash = (v) => v == null ? 0 : JSystem.callStatic("identityHashCode", v);
  const toInt = (x) => { const n = typeof x === "number" ? x : parseInt(String(x), 10); return Number.isFinite(n) ? n : 0; };
  const toNum = (x) => { const n = typeof x === "number" ? x : Number(String(x)); return Number.isFinite(n) ? n : 0; };
  const clamp = (v, a, b) => Math.max(a, Math.min(b, v));
  const jstr = (x) => {
    try {
      if (x == null) return "";
      if (typeof x === "string") return x;
      if (typeof x.call === "function") return String(x.call("toString"));
      return String(x);
    } catch (_) { return ""; }
  };
  function dp(v) {
    try { return AndroidUtilities.callStatic("dp(F)I", Number(v)); }
    catch (_) { return Math.round(Number(v) * 2); }
  }
  function appContext() { return ApplicationLoader.getStaticField("applicationContext"); }
  function toast(msg) {
    try { Toast.callStatic(MAKE_TOAST, appContext(), String(msg), 0).call("show"); }
    catch (_) { console.log(TAG, msg); }
  }
  function getAccount() {
    try { return toInt(UserConfig.getStaticField("selectedAccount")); }
    catch (_) { return 0; }
  }
  // ───────────── Multi-idioma ─────────────
  function getAppLang() {
    try {
      const LocaleController = inu.jvm.cls("org.telegram.messenger.LocaleController");
      let code = "";
      try {
        const inst = LocaleController.callStatic("getInstance");
        const info = inst.call("getCurrentLocaleInfo");
        if (info) {
          code = jstr(info.getField("pluralLangCode")) || jstr(info.getField("shortName")) || jstr(info.getField("name"));
        }
      } catch (_) {}
      if (!code) {
        try { code = jstr(LocaleController.callStatic("getLanguage")); } catch (_) {}
      }
      if (!code) {
        try {
          const Locale = inu.jvm.cls("java.util.Locale");
          code = jstr(Locale.callStatic("getDefault").call("getLanguage"));
        } catch (_) {}
      }
      code = (code || "en").toLowerCase().replace(/[_-].*$/, "").slice(0, 2);
      return code || "en";
    } catch (_) { return "en"; }
  }
  const STRINGS = {
    en: {
      title: "Chat Switcher",
      panel: "Side Panel",
      enable: "Enable panel",
      mode: "List mode",
      modeRecent: "Recent (Visited)",
      modeAll: "All (up to 200)",
      modeCustom: "Custom",
      count: "Count (Recent / Custom)",
      bubble: "Bubble",
      position: "Vertical position",
      posTop: "Top",
      posCenter: "Center",
      posBottom: "Bottom",
      opacity: "Bubble opacity",
      visible: "Visible chats now: ",
      addCurrent: "Add current chat to Custom",
      clearCustom: "Clear custom list",
      clearHistory: "Clear visit history",
      openChatFirst: "Open a chat first",
      noChatId: "Could not get current chat",
      alreadyInList: "Already in custom list",
      added: "✓ Added: ",
      removed: "✓ Removed: ",
      customCleared: "Custom list cleared",
      historyCleared: "Visit history cleared",
      cannotOpen: "Could not open chat",
      errorSwitch: "Error switching chat"
    },
    es: {
      title: "Chat Switcher",
      panel: "Panel Lateral",
      enable: "Activar panel",
      mode: "Modo de lista",
      modeRecent: "Recientes (Visitados)",
      modeAll: "Todos (hasta 200)",
      modeCustom: "Personalizado",
      count: "Cantidad (Recientes / Personalizado)",
      bubble: "Burbujita",
      position: "Posición vertical",
      posTop: "Arriba",
      posCenter: "Centro",
      posBottom: "Abajo",
      opacity: "Opacidad de la burbujita",
      visible: "Chats visibles ahora: ",
      addCurrent: "Agregar chat actual a Personalizado",
      clearCustom: "Limpiar lista personalizada",
      clearHistory: "Limpiar historial de visitas",
      openChatFirst: "Abre un chat primero",
      noChatId: "No se pudo obtener el chat actual",
      alreadyInList: "Ya está en la lista personalizada",
      added: "✓ Agregado: ",
      removed: "✓ Eliminado: ",
      customCleared: "Lista personalizada eliminada",
      historyCleared: "Historial eliminado",
      cannotOpen: "No se pudo abrir el chat",
      errorSwitch: "Error al cambiar de chat"
    },
    pt: {
      title: "Chat Switcher",
      panel: "Painel Lateral",
      enable: "Ativar painel",
      mode: "Modo de lista",
      modeRecent: "Recentes (Visitados)",
      modeAll: "Todos (até 200)",
      modeCustom: "Personalizado",
      count: "Quantidade (Recentes / Personalizado)",
      bubble: "Bolinha",
      position: "Posição vertical",
      posTop: "Cima",
      posCenter: "Centro",
      posBottom: "Baixo",
      opacity: "Opacidade da bolinha",
      visible: "Chats visíveis agora: ",
      addCurrent: "Adicionar chat atual ao Personalizado",
      clearCustom: "Limpar lista personalizada",
      clearHistory: "Limpar histórico de visitas",
      openChatFirst: "Abra um chat primeiro",
      noChatId: "Não foi possível obter o chat atual",
      alreadyInList: "Já está na lista personalizada",
      added: "✓ Adicionado: ",
      removed: "✓ Removido: ",
      customCleared: "Lista personalizada removida",
      historyCleared: "Histórico removido",
      cannotOpen: "Não foi possível abrir o chat",
      errorSwitch: "Erro ao trocar de chat"
    },
    ru: {
      title: "Chat Switcher",
      panel: "Боковая панель",
      enable: "Включить панель",
      mode: "Режим списка",
      modeRecent: "Недавние (Посещённые)",
      modeAll: "Все (до 200)",
      modeCustom: "Избранное",
      count: "Количество (Недавние / Избранное)",
      bubble: "Кнопка",
      position: "Вертикальное положение",
      posTop: "Сверху",
      posCenter: "По центру",
      posBottom: "Снизу",
      opacity: "Прозрачность кнопки",
      visible: "Чатов сейчас: ",
      addCurrent: "Добавить текущий чат в Избранное",
      clearCustom: "Очистить избранное",
      clearHistory: "Очистить историю посещений",
      openChatFirst: "Сначала откройте чат",
      noChatId: "Не удалось получить текущий чат",
      alreadyInList: "Уже в избранном",
      added: "✓ Добавлено: ",
      removed: "✓ Удалено: ",
      customCleared: "Избранное очищено",
      historyCleared: "История очищена",
      cannotOpen: "Не удалось открыть чат",
      errorSwitch: "Ошибка переключения чата"
    },
    de: {
      title: "Chat Switcher",
      panel: "Seitenleiste",
      enable: "Panel aktivieren",
      mode: "Listenmodus",
      modeRecent: "Zuletzt (Besucht)",
      modeAll: "Alle (bis 200)",
      modeCustom: "Benutzerdefiniert",
      count: "Anzahl (Zuletzt / Benutzerdefiniert)",
      bubble: "Blase",
      position: "Vertikale Position",
      posTop: "Oben",
      posCenter: "Mitte",
      posBottom: "Unten",
      opacity: "Deckkraft der Blase",
      visible: "Sichtbare Chats jetzt: ",
      addCurrent: "Aktuellen Chat hinzufügen",
      clearCustom: "Benutzerdefinierte Liste löschen",
      clearHistory: "Besuchsverlauf löschen",
      openChatFirst: "Öffne zuerst einen Chat",
      noChatId: "Aktueller Chat konnte nicht ermittelt werden",
      alreadyInList: "Bereits in der Liste",
      added: "✓ Hinzugefügt: ",
      removed: "✓ Entfernt: ",
      customCleared: "Liste gelöscht",
      historyCleared: "Verlauf gelöscht",
      cannotOpen: "Chat konnte nicht geöffnet werden",
      errorSwitch: "Fehler beim Wechseln"
    },
    fr: {
      title: "Chat Switcher",
      panel: "Panneau latéral",
      enable: "Activer le panneau",
      mode: "Mode de liste",
      modeRecent: "Récents (Visités)",
      modeAll: "Tous (jusqu'à 200)",
      modeCustom: "Personnalisé",
      count: "Nombre (Récents / Personnalisé)",
      bubble: "Bulle",
      position: "Position verticale",
      posTop: "Haut",
      posCenter: "Centre",
      posBottom: "Bas",
      opacity: "Opacité de la bulle",
      visible: "Chats visibles maintenant : ",
      addCurrent: "Ajouter le chat actuel au Personnalisé",
      clearCustom: "Vider la liste personnalisée",
      clearHistory: "Vider l'historique des visites",
      openChatFirst: "Ouvrez d'abord un chat",
      noChatId: "Impossible d'obtenir le chat actuel",
      alreadyInList: "Déjà dans la liste",
      added: "✓ Ajouté : ",
      removed: "✓ Supprimé : ",
      customCleared: "Liste personnalisée effacée",
      historyCleared: "Historique effacé",
      cannotOpen: "Impossible d'ouvrir le chat",
      errorSwitch: "Erreur de changement de chat"
    },
    it: {
      title: "Chat Switcher",
      panel: "Pannello laterale",
      enable: "Attiva pannello",
      mode: "Modalità lista",
      modeRecent: "Recenti (Visitati)",
      modeAll: "Tutti (fino a 200)",
      modeCustom: "Personalizzato",
      count: "Quantità (Recenti / Personalizzato)",
      bubble: "Bollino",
      position: "Posizione verticale",
      posTop: "Alto",
      posCenter: "Centro",
      posBottom: "Basso",
      opacity: "Opacità del bollino",
      visible: "Chat visibili ora: ",
      addCurrent: "Aggiungi chat attuale a Personalizzato",
      clearCustom: "Svuota lista personalizzata",
      clearHistory: "Svuota cronologia visite",
      openChatFirst: "Apri prima una chat",
      noChatId: "Impossibile ottenere la chat attuale",
      alreadyInList: "Già nella lista",
      added: "✓ Aggiunto: ",
      removed: "✓ Rimosso: ",
      customCleared: "Lista personalizzata cancellata",
      historyCleared: "Cronologia cancellata",
      cannotOpen: "Impossibile aprire la chat",
      errorSwitch: "Errore nel cambio chat"
    },
    tr: {
      title: "Chat Switcher",
      panel: "Yan Panel",
      enable: "Paneli etkinleştir",
      mode: "Liste modu",
      modeRecent: "Son (Ziyaret edilen)",
      modeAll: "Tümü (200'e kadar)",
      modeCustom: "Özel",
      count: "Sayı (Son / Özel)",
      bubble: "Baloncuk",
      position: "Dikey konum",
      posTop: "Üst",
      posCenter: "Orta",
      posBottom: "Alt",
      opacity: "Baloncuk opaklığı",
      visible: "Şu an görünen sohbetler: ",
      addCurrent: "Mevcut sohbeti Özel'e ekle",
      clearCustom: "Özel listeyi temizle",
      clearHistory: "Ziyaret geçmişini temizle",
      openChatFirst: "Önce bir sohbet açın",
      noChatId: "Mevcut sohbet alınamadı",
      alreadyInList: "Zaten listede",
      added: "✓ Eklendi: ",
      removed: "✓ Kaldırıldı: ",
      customCleared: "Özel liste temizlendi",
      historyCleared: "Geçmiş temizlendi",
      cannotOpen: "Sohbet açılamadı",
      errorSwitch: "Sohbet değiştirme hatası"
    },
    ar: {
      title: "Chat Switcher",
      panel: "اللوحة الجانبية",
      enable: "تفعيل اللوحة",
      mode: "وضع القائمة",
      modeRecent: "الأخيرة (تمت زيارتها)",
      modeAll: "الكل (حتى 200)",
      modeCustom: "مخصص",
      count: "العدد (الأخيرة / مخصص)",
      bubble: "الفقاعة",
      position: "الموضع العمودي",
      posTop: "أعلى",
      posCenter: "وسط",
      posBottom: "أسفل",
      opacity: "شفافية الفقاعة",
      visible: "المحادثات الظاهرة الآن: ",
      addCurrent: "إضافة المحادثة الحالية إلى المخصص",
      clearCustom: "مسح القائمة المخصصة",
      clearHistory: "مسح سجل الزيارات",
      openChatFirst: "افتح محادثة أولاً",
      noChatId: "تعذر الحصول على المحادثة الحالية",
      alreadyInList: "موجود بالفعل في القائمة",
      added: "✓ تمت الإضافة: ",
      removed: "✓ تمت الإزالة: ",
      customCleared: "تم مسح القائمة المخصصة",
      historyCleared: "تم مسح السجل",
      cannotOpen: "تعذر فتح المحادثة",
      errorSwitch: "خطأ في تبديل المحادثة"
    },
    zh: {
      title: "Chat Switcher",
      panel: "侧边面板",
      enable: "启用面板",
      mode: "列表模式",
      modeRecent: "最近（访问过）",
      modeAll: "全部（最多200）",
      modeCustom: "自定义",
      count: "数量（最近 / 自定义）",
      bubble: "气泡",
      position: "垂直位置",
      posTop: "顶部",
      posCenter: "中间",
      posBottom: "底部",
      opacity: "气泡不透明度",
      visible: "当前可见聊天：",
      addCurrent: "将当前聊天添加到自定义",
      clearCustom: "清除自定义列表",
      clearHistory: "清除访问历史",
      openChatFirst: "请先打开一个聊天",
      noChatId: "无法获取当前聊天",
      alreadyInList: "已在自定义列表中",
      added: "✓ 已添加：",
      removed: "✓ 已移除：",
      customCleared: "自定义列表已清除",
      historyCleared: "历史已清除",
      cannotOpen: "无法打开聊天",
      errorSwitch: "切换聊天出错"
    }
  };
  const LANG = getAppLang();
  const S = STRINGS[LANG] || STRINGS.en;
  function t(key) { return S[key] || STRINGS.en[key] || key; }
  // ───────────── Colores seguros ─────────────
  function sanitizeColor(c, fallback) {
    if (typeof c !== "number" || !Number.isFinite(c)) return fallback | 0;
    if (Math.abs(c) < 0x10000) return fallback | 0;
    if ((c & 0xff000000) === 0) return (0xff000000 | c) | 0;
    return c | 0;
  }
  function themeColor(keyName, fallback) {
    const fb = fallback | 0;
    try {
      let key = null;
      try { key = Theme.getStaticField(keyName); } catch (_) {}
      if (typeof key === "number" && key >= 0) {
        try {
          const c = Theme.callStatic("getColor(I)I", key);
          return sanitizeColor(c, fb);
        } catch (_) {}
        try {
          const c = Theme.callStatic("getColor", key);
          return sanitizeColor(c, fb);
        } catch (_) {}
      }
    } catch (e) {
      console.warn(TAG, "themeColor failed for", keyName, e);
    }
    return fb;
  }
  function withAlpha(color, alphaByte) {
    const c = sanitizeColor(color, 0xff1c1c1e);
    return (((alphaByte & 0xff) << 24) | (c & 0x00ffffff)) | 0;
  }
  function getGlassColors() {
    const bg      = themeColor("key_windowBackgroundWhite", 0xff1c1c1e);
    const text    = themeColor("key_windowBackgroundWhiteBlackText", 0xffffffff);
    const accent  = themeColor("key_chats_actionBackground", 0xff5c9ce6);
    const accent2 = themeColor("key_windowBackgroundWhiteBlueText", 0xff5c9ce6);
    const divider = themeColor("key_divider", 0x33ffffff);
    const finalBg     = sanitizeColor(bg, 0xff1c1c1e);
    const finalText   = sanitizeColor(text, 0xffffffff);
    const finalAccent = sanitizeColor(accent || accent2, 0xff5c9ce6);
    return {
      panelBg:    withAlpha(finalBg, 0xcc),
      handleBg:   finalAccent,
      handleIcon: 0xffffffff | 0,
      text:       finalText,
      accent:     finalAccent,
      divider:    sanitizeColor(divider, 0x33ffffff),
      addBtnBg:   withAlpha(0xffffff, 0x22)
    };
  }
  // ───────────── Config ─────────────
  const P = "csw5.";
  const DEFAULTS = {
    enabled: "true",
    mode: "recent",
    position: "1",
    count: "20",
    opacity: "90"
  };
  const cfg     = (k) => localStorage.getItem(P + k) ?? DEFAULTS[k];
  const cfgBool = (k) => cfg(k) === "true";
  const cfgInt  = (k) => { const n = parseInt(cfg(k), 10); return Number.isFinite(n) ? n : parseInt(DEFAULTS[k], 10); };
  const cfgSet  = (k, v) => { localStorage.setItem(P + k, String(v)); };
  function storageKey(k) {
    return P + "a" + getAccount() + "." + k;
  }
  function parseIdNameList(raw) {
    try {
      const arr = JSON.parse(raw || "[]");
      if (!Array.isArray(arr)) return [];
      return arr
        .map((e) => (typeof e === "number" || typeof e === "string")
          ? { id: toNum(e), name: "" }
          : { id: toNum(e && e.id), name: String((e && e.name) || "") })
        .filter((e) => e.id !== 0);
    } catch (_) { return []; }
  }
  function loadAccountList(kind) {
    const key = storageKey(kind);
    let raw = localStorage.getItem(key);
    if (raw == null) {
      const legacy = localStorage.getItem(P + kind);
      if (legacy != null) {
        localStorage.setItem(key, legacy);
        localStorage.removeItem(P + kind);
        raw = legacy;
      }
    }
    return parseIdNameList(raw);
  }
  // ───────────── Estado ─────────────
  const unhooks = [];
  const docks = new Map();
  const clickActions = new Map();
  let lastChatFragment = null;
  let lastAccount = getAccount();
  let active = true;
  // ───────────── Historial / custom (por cuenta) ─────────────
  function getHistory() {
    return loadAccountList("history");
  }
  function saveHistory(list) {
    const max = clamp(cfgInt("count") + 15, 15, 80);
    localStorage.setItem(storageKey("history"), JSON.stringify(list.slice(0, max)));
  }
  function recordChat(id, name) {
    id = toNum(id);
    if (!id) return;
    const list = getHistory();
    const prev = list.find((e) => e.id === id);
    const finalName = (name && name.trim()) || (prev ? prev.name : "");
    saveHistory([{ id, name: finalName }, ...list.filter((e) => e.id !== id)]);
  }
  function updateChatName(id, name) {
    id = toNum(id);
    name = String(name || "").trim();
    if (!id || !name) return;
    const hist = getHistory();
    const he = hist.find((x) => x.id === id);
    if (he && he.name !== name) { he.name = name; saveHistory(hist); }
    const cust = getCustomList();
    const ce = cust.find((x) => x.id === id);
    if (ce && ce.name !== name) { ce.name = name; saveCustomList(cust); }
  }
  function getCustomList() {
    return loadAccountList("custom");
  }
  function saveCustomList(list) {
    localStorage.setItem(storageKey("custom"), JSON.stringify(list.slice(0, 50)));
  }
  function addCurrentToCustom() {
    if (!lastChatFragment) { toast(t("openChatFirst")); return; }
    const id = getCurrentDialogId(lastChatFragment);
    if (!id) { toast(t("noChatId")); return; }
    const list = getCustomList();
    if (list.some((e) => e.id === id)) { toast(t("alreadyInList")); return; }
    let name = "";
    try {
      const ab = lastChatFragment.call("getActionBar");
      if (ab) name = jstr(ab.call("getTitle")).trim();
    } catch (_) {}
    if (!name || name === "Telegram") {
      const obj = tlObject(id);
      name = displayName({ id, name: "" }, obj);
    }
    list.unshift({ id, name: name || "" });
    saveCustomList(list);
    refreshCurrent();
    toast(t("added") + (name || id));
  }
  function removeFromCustom(id) {
    id = toNum(id);
    if (!id) return;
    const list = getCustomList();
    const found = list.find((e) => e.id === id);
    const name = found ? (found.name || String(id)) : String(id);
    const newList = list.filter((e) => e.id !== id);
    saveCustomList(newList);
    for (const st of docks.values()) {
      if (st.open) {
        try { fillPanel(st); } catch (_) {}
      }
    }
    toast(t("removed") + name);
  }
  function clearCustom() {
    localStorage.removeItem(storageKey("custom"));
    refreshCurrent();
    toast(t("customCleared"));
  }
  function clearHistory() {
    localStorage.removeItem(storageKey("history"));
    refreshCurrent();
    toast(t("historyCleared"));
  }
  // ───────────── Telegram ─────────────
  function getController() {
    try { return MessagesController.callStatic("getInstance", getAccount()); }
    catch (_) { return null; }
  }
  // Lee el elemento i de una lista de Telegram (LongSparseArray usa valueAt, ArrayList usa get)
  function dialogAt(list, i) {
    let d = null;
    try { d = list.call("valueAt", i); } catch (_) {}
    if (!d) { try { d = list.call("get", i); } catch (_) {} }
    return d;
  }
  function getAllDialogs() {
    const result = [];
    try {
      const mc = getController();
      if (!mc) return result;
      // allDialogs ya viene ordenada por actividad: no hace falta recorrer todos los chats
      const candidates = ["allDialogs", "dialogs_dict", "dialogs", "dialogsServerOnly"];
      let list = null;
      for (const name of candidates) {
        try { list = mc.getField(name); if (list) break; } catch (_) {}
      }
      if (!list) return result;
      const size = Math.min(toInt(list.call("size")), 300);
      for (let i = 0; i < size && result.length < 200; i++) {
        try {
          const dialog = dialogAt(list, i);
          if (!dialog) continue;
          let id = 0;
          try { id = toNum(dialog.call("getId")); } catch (_) {}
          if (!id) try { id = toNum(dialog.getField("id")); } catch (_) {}
          if (!id) try { id = toNum(dialog.getField("dialog_id")); } catch (_) {}
          if (id) result.push({ id, name: "" });
        } catch (_) {}
      }
    } catch (e) {
      console.warn(TAG, "getAllDialogs failed:", e);
    }
    return result;
  }
  // No leidos: se consulta solo cada chat que se muestra (antes se recorrian TODOS los chats y tardaba demasiado)
  function getDialogsDict() {
    try {
      const mc = getController();
      return mc ? mc.getField("dialogs_dict") : null;
    } catch (_) { return null; }
  }
  function unreadFor(dict, id) {
    if (!dict) return 0;
    let dialog = null;
    try { dialog = dict.call("get(J)Ljava/lang/Object;", id); } catch (_) {}
    if (!dialog) { try { dialog = dict.call("get", id); } catch (_) {} }
    if (!dialog) return 0;
    let count = 0;
    try { count = toInt(dialog.getField("unread_count")); } catch (_) {}
    if (count <= 0) {
      try { if (dialog.getField("unread_mark")) count = 1; } catch (_) {}
    }
    return count > 0 ? count : 0;
  }
  function isGoodName(name) {
    if (!name) return false;
    name = String(name).trim();
    if (!name || name.startsWith("Usuario ") || name.startsWith("Chat ") || name === "Telegram") return false;
    return true;
  }
  function getDisplayList() {
    const mode = cfg("mode");
    if (mode === "custom") return getCustomList().slice(0, clamp(cfgInt("count"), 5, 50));
    if (mode === "all") return getAllDialogs();
    return getHistory().slice(0, clamp(cfgInt("count"), 5, 50));
  }
  function openDialog(fragment, dialogId) {
    dialogId = toNum(dialogId);
    if (!dialogId || !fragment) return;
    try {
      const activity = fragment.call("getParentActivity");
      if (!activity) return;
      const args = new Bundle();
      if (dialogId > 0) args.call("putLong", "user_id", dialogId);
      else args.call("putLong", "chat_id", -dialogId);
      const chatFragment = new ChatActivity(args);
      try { activity.call("presentFragment", chatFragment, true); return; } catch (_) {}
      try { activity.call("presentFragment(Lorg/telegram/ui/ActionBar/BaseFragment;Z)Z", chatFragment, true); return; } catch (_) {}
      try { activity.call("presentFragment(Lorg/telegram/ui/ActionBar/BaseFragment;ZZ)Z", chatFragment, true, false); return; } catch (_) {}
      try { activity.call("presentFragment", chatFragment); return; } catch (_) {}
      try {
        const parent = fragment.call("getParentLayout");
        if (parent) {
          try { parent.call("presentFragment", chatFragment, true); return; } catch (_) {}
          parent.call("presentFragment", chatFragment);
          return;
        }
      } catch (_) {}
      toast(t("cannotOpen"));
    } catch (e) {
      console.warn(TAG, "openDialog error:", e);
      toast(t("errorSwitch"));
    }
  }
  function getCurrentDialogId(fragment) {
    try { return toNum(fragment.call("getDialogId")); } catch (_) {}
    try { return toNum(fragment.call("getDialogId()J")); } catch (_) {}
    return 0;
  }
  function captureTitle(fragment, id) {
    try {
      const ab = fragment.call("getActionBar");
      if (!ab) return;
      const s = jstr(ab.call("getTitle")).trim();
      if (s && s !== "Telegram") updateChatName(id, s);
    } catch (_) {}
  }
  function tlObject(id) {
    try {
      const mc = getController();
      if (!mc) return null;
      const boxed = LongCls.callStatic("valueOf(J)Ljava/lang/Long;", Math.abs(id));
      return id > 0
        ? mc.call("getUser(Ljava/lang/Long;)Lorg/telegram/tgnet/TLRPC$User;", boxed)
        : mc.call("getChat(Ljava/lang/Long;)Lorg/telegram/tgnet/TLRPC$Chat;", boxed);
    } catch (_) { return null; }
  }
  function displayName(entry, obj) {
    if (isGoodName(entry.name)) return entry.name;
    try {
      if (obj) {
        if (entry.id > 0) {
          try {
            const s = jstr(UserObject.callStatic("getUserName(Lorg/telegram/tgnet/TLRPC$User;)Ljava/lang/String;", obj)).trim();
            if (s) return s;
          } catch (_) {}
          try {
            const first = jstr(obj.getField("first_name")).trim();
            const last  = jstr(obj.getField("last_name")).trim();
            const full = (first + " " + last).trim();
            if (full) return full;
          } catch (_) {}
        } else {
          try {
            const title = jstr(obj.getField("title")).trim();
            if (title) return title;
          } catch (_) {}
        }
      }
    } catch (_) {}
    return entry.id > 0 ? "Usuario " + entry.id : "Chat " + Math.abs(entry.id);
  }
  const FALLBACK_COLORS = [0xff5c9ce6, 0xff7bc862, 0xffe17076, 0xffeda86c, 0xffa695e7, 0xff6ec9cb, 0xffee7aae];
  function makeAvatar(activity, entry, obj, name) {
    const size = dp(42);
    try {
      if (obj) {
        const iv = new BackupImageView(activity);
        iv.call("setRoundRadius", dp(21));
        const ad = new AvatarDrawable();
        ad.call("setInfo", obj);
        iv.call("setForUserOrChat", obj, ad);
        return { view: iv, size };
      }
    } catch (e) {
      console.warn(TAG, "Avatar real falló:", e);
    }
    const tv = new TextView(activity);
    const initial = (String(name || "?").trim()[0] || "?").toUpperCase();
    tv.call("setText", initial);
    tv.call("setGravity", 17);
    tv.call("setTextSize", 1, 16);
    tv.call("setTextColor", 0xffffffff | 0);
    tv.call("setBackground", (() => {
      const g = new GradientDrawable();
      g.call("setShape", 1);
      g.call("setColor", FALLBACK_COLORS[Math.abs(entry.id) % FALLBACK_COLORS.length] | 0);
      return g;
    })());
    return { view: tv, size };
  }
  function makeItem(activity, fragment, entry, onPicked, colors, unreadCount) {
    const obj = tlObject(entry.id);
    const name = displayName(entry, obj);
    if (cfg("mode") === "all" && !obj && !isGoodName(name)) return null;
    if (isGoodName(name)) updateChatName(entry.id, name);
    const item = new LinearLayout(activity);
    item.call("setOrientation", 0);
    item.call("setGravity", 16);
    item.call("setPadding", dp(10), dp(7), dp(8), dp(7));
    item.call("setClickable", true);
    const avData = makeAvatar(activity, entry, obj, name);
    item.call("addView", avData.view, new LinearLP(avData.size, avData.size));
    const label = new TextView(activity);
    label.call("setText", name);
    label.call("setTextSize", 1, 13);
    label.call("setTextColor", sanitizeColor(colors.text, 0xffffffff));
    label.call("setPadding", dp(10), 0, dp(6), 0);
    label.call("setSingleLine", true);
    item.call("addView", label, new LinearLP(0, -2, 1));
    if (unreadCount > 0) {
      const badge = new TextView(activity);
      const badgeText = unreadCount > 99 ? "99+" : String(unreadCount);
      badge.call("setText", badgeText);
      badge.call("setGravity", 17);
      badge.call("setTextSize", 1, 11);
      badge.call("setTextColor", 0xffffffff | 0);
      badge.call("setSingleLine", true);
      badge.call("setPadding", dp(5), 0, dp(5), 0);
      badge.call("setBackground", (() => {
        const g = new GradientDrawable();
        g.call("setShape", 0);
        g.call("setCornerRadius", dp(10));
        g.call("setColor", sanitizeColor(colors.accent, 0xff5c9ce6) | 0);
        return g;
      })());
      const badgeW = badgeText.length === 1 ? dp(20) : -2;
      const badgeLp = new LinearLP(badgeW, dp(20));
      badgeLp.call("setMargins", dp(2), 0, dp(2), 0);
      item.call("addView", badge, badgeLp);
    }
    if (cfg("mode") === "custom") {
      const xBtn = new TextView(activity);
      xBtn.call("setText", "✕");
      xBtn.call("setGravity", 17);
      xBtn.call("setTextSize", 1, 15);
      xBtn.call("setTextColor", 0xffff5555 | 0);
      xBtn.call("setPadding", dp(4), dp(2), dp(4), dp(2));
      xBtn.call("setClickable", true);
      item.call("addView", xBtn, new LinearLP(dp(32), dp(32)));
      clickActions.set(vHash(xBtn), () => {
        removeFromCustom(entry.id);
      });
    }
    clickActions.set(vHash(item), () => { onPicked(); openDialog(fragment, entry.id); });
    return item;
  }
  function makeAddCurrentItem(activity, fragment, colors) {
    const item = new LinearLayout(activity);
    item.call("setOrientation", 0);
    item.call("setGravity", 17);
    item.call("setPadding", dp(12), dp(10), dp(12), dp(10));
    item.call("setClickable", true);
    item.call("setBackground", (() => {
      const g = new GradientDrawable();
      g.call("setShape", 0);
      g.call("setColor", sanitizeColor(colors.addBtnBg, 0x33ffffff));
      g.call("setCornerRadius", dp(10));
      return g;
    })());
    const label = new TextView(activity);
    label.call("setText", "＋  " + t("addCurrent"));
    label.call("setTextSize", 1, 13);
    label.call("setTextColor", sanitizeColor(colors.accent, 0xff5c9ce6));
    label.call("setGravity", 17);
    item.call("addView", label, new LinearLP(-1, -2));
    clickActions.set(vHash(item), () => {
      addCurrentToCustom();
      try {
        const root = fragment.call("getFragmentView");
        if (root) {
          const st = docks.get(vHash(root));
          if (st) fillPanel(st);
        }
      } catch (_) {}
    });
    return item;
  }
  // ───────────── Posición ─────────────
  function dockGravity() {
    const pos = clamp(cfgInt("position"), 0, 2);
    return 5 | (pos === 0 ? 48 : pos === 2 ? 80 : 16);
  }
  function getMaxPanelHeight(pos) {
    try {
      const point = AndroidUtilities.getStaticField("displaySize");
      let sh = toInt(point.getField("y"));
      if (!sh || sh < 400) sh = 1920;
      let status = 0;
      try { status = toInt(AndroidUtilities.getStaticField("statusBarHeight")); } catch (_) {}
      const reserved = status + dp(120) + (pos === 2 ? dp(90) : dp(40));
      const avail = Math.max(dp(180), sh - reserved);
      if (pos === 2) return Math.min(avail, dp(300));
      return Math.min(avail, dp(380));
    } catch (_) {
      return pos === 2 ? dp(280) : dp(340);
    }
  }
  // ───────────── Panel ─────────────
  function disposeDock(root) {
    if (!root) return;
    const h = vHash(root);
    const st = docks.get(h);
    if (!st) return;
    try {
      const p = st.dock.call("getParent");
      if (p) {
        p.call("removeView", st.dock);
        if (st.overlay) p.call("removeView", st.overlay);
      }
    } catch (_) {}
    clickActions.delete(vHash(st.handle));
    if (st.overlay) clickActions.delete(vHash(st.overlay));
    for (const rh of st.rows) clickActions.delete(rh);
    docks.delete(h);
  }
  function fillPanel(st) {
    for (const rh of st.rows) clickActions.delete(rh);
    st.rows = [];
    st.list.call("removeAllViews");
    const gen = (st.gen = (st.gen || 0) + 1);
    const colors = getGlassColors();
    if (cfg("mode") === "custom") {
      try {
        const addBtn = makeAddCurrentItem(st.activity, st.fragment, colors);
        st.rows.push(vHash(addBtn));
        st.list.call("addView", addBtn, new LinearLP(-1, -2));
        const sep = new View(st.activity);
        sep.call("setBackgroundColor", sanitizeColor(colors.divider, 0x33ffffff));
        st.list.call("addView", sep, new LinearLP(-1, dp(1)));
      } catch (e) { console.warn(TAG, "addBtn:", e); }
    }
    const entries = getDisplayList();
    const dict = getDialogsDict();
    let i = 0;
    // Se arma en tandas cortas: Inu corta cualquier llamada que ocupe mas de 250 ms seguidos
    const step = () => {
      if (!active || st.gen !== gen || !st.open) return;
      const t0 = Date.now();
      while (i < entries.length && Date.now() - t0 < 60) {
        const entry = entries[i++];
        try {
          const item = makeItem(st.activity, st.fragment, entry, () => setOpen(st, false), colors, unreadFor(dict, entry.id));
          if (!item) continue;
          st.rows.push(vHash(item));
          st.list.call("addView", item, new LinearLP(-1, -2));
        } catch (e) { console.warn(TAG, "item:", e); }
      }
      if (i < entries.length) {
        try { st.root.call("postDelayed", inu.jvm.runnable(step), 16); } catch (_) {}
      }
    };
    step();
  }
  function setOpen(st, open) {
    st.open = open;
    try {
      if (open) {
        fillPanel(st);
        st.panel.call("setVisibility", 0);
        st.handle.call("setText", "›");
        if (st.overlay) st.overlay.call("setVisibility", 0);
      } else {
        st.panel.call("setVisibility", 8);
        st.handle.call("setText", "‹");
        if (st.overlay) st.overlay.call("setVisibility", 8);
      }
    } catch (e) { console.warn(TAG, "setOpen:", e); }
  }
  function installDock(fragment) {
    if (!active || !fragment) return;
    let root = null;
    try { root = fragment.call("getFragmentView"); } catch (_) {}
    if (!root) return;
    if (docks.get(vHash(root))) return;
    disposeDock(root);
    if (!cfgBool("enabled")) return;
    let activity = null;
    try { activity = fragment.call("getParentActivity"); } catch (_) {}
    if (!activity) return;
    try {
      const colors = getGlassColors();
      const overlay = new View(activity);
      overlay.call("setClickable", true);
      overlay.call("setVisibility", 8);
      const olp = new FrameLP(-1, -1);
      root.call("addView", overlay, olp);
      const dock = new LinearLayout(activity);
      dock.call("setOrientation", 0);
      dock.call("setGravity", 16);
      const handle = new TextView(activity);
      handle.call("setText", "‹");
      handle.call("setGravity", 17);
      handle.call("setTextSize", 1, 18);
      handle.call("setTextColor", 0xffffffff | 0);
      handle.call("setClickable", true);
      handle.call("setBackground", (() => {
        const g = new GradientDrawable();
        g.call("setShape", 1);
        g.call("setColor", sanitizeColor(colors.handleBg, 0xff5c9ce6));
        try { g.call("setStroke", dp(1), 0x40ffffff | 0); } catch (_) {}
        return g;
      })());
      const alpha = clamp(cfgInt("opacity"), 20, 100) / 100;
      try { handle.call("setAlpha", alpha); } catch (_) {}
      try { handle.call("setAlpha(F)V", alpha); } catch (_) {}
      dock.call("addView", handle, new LinearLP(dp(38), dp(38)));
      const panel = new LinearLayout(activity);
      panel.call("setOrientation", 1);
      panel.call("setBackground", (() => {
        const g = new GradientDrawable();
        g.call("setShape", 0);
        g.call("setColor", sanitizeColor(colors.panelBg, 0xcc1c1c1e));
        g.call("setCornerRadius", dp(16));
        try { g.call("setStroke", dp(1), 0x28ffffff | 0); } catch (_) {}
        return g;
      })());
      panel.call("setVisibility", 8);
      panel.call("setPadding", dp(4), dp(6), dp(4), dp(6));
      const scroll = new ScrollView(activity);
      const list = new LinearLayout(activity);
      list.call("setOrientation", 1);
      scroll.call("addView", list, new FrameLP(-1, -2));
      const pos = clamp(cfgInt("position"), 0, 2);
      const maxH = getMaxPanelHeight(pos);
      panel.call("addView", scroll, new LinearLP(dp(220), maxH));
      dock.call("addView", panel, new LinearLP(-2, -2));
      let topM = dp(70);
      let botM = dp(70);
      if (pos === 0) {
        topM = dp(70);
        botM = dp(20);
      } else if (pos === 2) {
        topM = dp(20);
        botM = dp(110);
      } else {
        topM = dp(70);
        botM = dp(70);
      }
      const lp = new FrameLP(-2, -2, dockGravity());
      lp.call("setMargins", 0, topM, dp(6), botM);
      root.call("addView", dock, lp);
      const st = { dock, handle, panel, list, fragment, activity, root, overlay, open: false, rows: [] };
      docks.set(vHash(root), st);
      clickActions.set(vHash(handle), () => setOpen(st, !st.open));
      clickActions.set(vHash(overlay), () => setOpen(st, false));
    } catch (e) {
      console.warn(TAG, "installDock:", e);
    }
  }
  function tryInstall(fragment) {
    if (!fragment) return;
    try { lastChatFragment = fragment; installDock(fragment); }
    catch (e) { console.warn(TAG, "tryInstall:", e); }
  }
  function refreshCurrent() {
    if (!lastChatFragment) return;
    try {
      let root = null;
      try { root = lastChatFragment.call("getFragmentView"); } catch (_) {}
      if (root) disposeDock(root);
      installDock(lastChatFragment);
    } catch (_) {}
  }
  function onAccountMaybeChanged() {
    const acc = getAccount();
    if (acc === lastAccount) return;
    lastAccount = acc;
    refreshCurrent();
  }
  // ───────────── Hooks ─────────────
  try {
    const m = View.getDeclaredMethod("performClick()Z");
    unhooks.push(inu.xposed.hookMethod(m, {
      before(ctx) {
        const fn = clickActions.get(vHash(ctx.thisObject));
        if (!fn) return;
        try { if (typeof ctx.setResult === "function") ctx.setResult(true); } catch (_) {}
        try { fn(); } catch (e) { console.warn(TAG, "click:", e); }
      }
    }));
  } catch (e) { console.warn(TAG, "hook performClick:", e); }
  try {
    const m = ChatActivity.getDeclaredMethod("onResume()V");
    unhooks.push(inu.xposed.hookMethod(m, {
      after(ctx) {
        try {
          const fragment = ctx.thisObject;
          lastChatFragment = fragment;
          onAccountMaybeChanged();
          const id = getCurrentDialogId(fragment);
          let title = "";
          try {
            const ab = fragment.call("getActionBar");
            if (ab) title = jstr(ab.call("getTitle")).trim();
          } catch (_) {}
          recordChat(id, title);
          const root = (() => { try { return fragment.call("getFragmentView"); } catch (_) { return null; } })();
          if (root) {
            root.call("post", inu.jvm.runnable(() => tryInstall(fragment)));
            root.call("postDelayed", inu.jvm.runnable(() => tryInstall(fragment)), 200);
            root.call("postDelayed", inu.jvm.runnable(() => tryInstall(fragment)), 600);
            root.call("postDelayed", inu.jvm.runnable(() => { try { captureTitle(fragment, id); } catch (_) {} }), 700);
          } else tryInstall(fragment);
        } catch (e) { console.warn(TAG, "onResume:", e); }
      }
    }));
  } catch (e) { console.warn(TAG, "hook onResume:", e); }
  try {
    const m = ChatActivity.getDeclaredMethod("onBecomeFullyVisible()V");
    unhooks.push(inu.xposed.hookMethod(m, {
      after(ctx) {
        try {
          const fragment = ctx.thisObject;
          lastChatFragment = fragment;
          onAccountMaybeChanged();
          const root = (() => { try { return fragment.call("getFragmentView"); } catch (_) { return null; } })();
          if (root) {
            root.call("post", inu.jvm.runnable(() => tryInstall(fragment)));
            root.call("postDelayed", inu.jvm.runnable(() => tryInstall(fragment)), 250);
          } else tryInstall(fragment);
        } catch (e) { console.warn(TAG, "onBecomeFullyVisible:", e); }
      }
    }));
  } catch (e) { console.warn(TAG, "hook onBecomeFullyVisible:", e); }
  try {
    const m = BaseFragment.getDeclaredMethod("onBecomeFullyVisible()V");
    unhooks.push(inu.xposed.hookMethod(m, {
      after(ctx) {
        try {
          const fragment = ctx.thisObject;
          if (!ChatActivity.callStatic("class").call("isInstance", fragment)) return;
          lastChatFragment = fragment;
          onAccountMaybeChanged();
          const root = (() => { try { return fragment.call("getFragmentView"); } catch (_) { return null; } })();
          if (root) root.call("post", inu.jvm.runnable(() => tryInstall(fragment)));
        } catch (_) {}
      }
    }));
  } catch (_) {}
  // ───────────── Ajustes ─────────────
  function makeAction(id, text, onClick) {
    if (typeof inu.ui.button === "function") return inu.ui.button({ id, text, onClick });
    if (typeof inu.ui.text === "function") return inu.ui.text({ id, text, onClick });
    return inu.ui.check({ id, text: "➤ " + text, checked: false, onChange: () => onClick() });
  }
  const page = inu.ui.settingsPage({
    title: t("title"),
    items: () => {
      const mode = cfg("mode");
      const items = [
        inu.ui.header(t("panel")),
        inu.ui.check({
          id: "enabled", text: t("enable"), checked: cfgBool("enabled"),
          onChange: (v) => { cfgSet("enabled", v); refreshCurrent(); }
        }),
        inu.ui.select({
          id: "mode", text: t("mode"),
          items: [t("modeRecent"), t("modeAll"), t("modeCustom")],
          selected: mode === "all" ? 1 : mode === "custom" ? 2 : 0,
          onChange: (v) => {
            const m = toInt(v);
            cfgSet("mode", m === 1 ? "all" : m === 2 ? "custom" : "recent");
            refreshCurrent();
          }
        }),
        inu.ui.slider({
          id: "count", text: t("count"),
          min: 5, max: 50, step: 1, value: clamp(cfgInt("count"), 5, 50),
          format: (v) => String(v),
          onChange: (v) => { cfgSet("count", v); refreshCurrent(); }
        }),
        inu.ui.header(t("bubble")),
        inu.ui.select({
          id: "position", text: t("position"),
          items: [t("posTop"), t("posCenter"), t("posBottom")],
          selected: clamp(cfgInt("position"), 0, 2),
          onChange: (v) => {
            cfgSet("position", v);
            refreshCurrent();
          }
        }),
        inu.ui.slider({
          id: "opacity", text: t("opacity"),
          min: 25, max: 100, step: 5, value: clamp(cfgInt("opacity"), 25, 100),
          format: (v) => v + "%",
          onChange: (v) => { cfgSet("opacity", v); refreshCurrent(); }
        }),
        inu.ui.separator(t("visible") + (mode === "all" ? "200" : getDisplayList().length)),
      ];
      if (mode === "custom") {
        items.push(makeAction("add_current", t("addCurrent"), addCurrentToCustom));
        items.push(makeAction("clear_custom", t("clearCustom"), clearCustom));
      } else {
        items.push(makeAction("clear_history", t("clearHistory"), clearHistory));
      }
      return items;
    }
  });
  inu.registerSettings(page);
  inu.onUnload(() => {
    active = false;
    for (const st of Array.from(docks.values())) {
      try {
        const p = st.dock.call("getParent");
        if (p) {
          p.call("removeView", st.dock);
          if (st.overlay) p.call("removeView", st.overlay);
        }
      } catch (_) {}
    }
    docks.clear();
    clickActions.clear();
    for (const u of unhooks) { try { u(); } catch (_) {} }
    unhooks.length = 0;
    lastChatFragment = null;
  });
})();