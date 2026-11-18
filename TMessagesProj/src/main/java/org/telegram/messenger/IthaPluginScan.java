package org.telegram.messenger;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * IthaGram: revision de seguridad de un plugin (.plugin = codigo Python) ANTES de instalarlo.
 * Analisis estatico local: busca llamadas peligrosas en el texto. No sale a internet ni manda nada a ningun lado.
 * No es una garantia: un plugin muy ofuscado podria esconder cosas. "Sin hallazgos" no significa "seguro".
 */
public class IthaPluginScan {

    public static final int OK = 0;
    public static final int WARN = 1;
    public static final int DANGER = 2;

    public static class Finding {
        public final int level;
        public final String text;
        public final int line;

        Finding(int level, String text, int line) {
            this.level = level;
            this.text = text;
            this.line = line;
        }
    }

    public static class Result {
        public int level = OK;
        public final List<Finding> findings = new ArrayList<>();

        void add(int level, String text, int line) {
            findings.add(new Finding(level, text, line));
            if (level > this.level) {
                this.level = level;
            }
        }

        /** Texto listo para mostrar en un dialogo. */
        public String format() {
            StringBuilder sb = new StringBuilder();
            if (level == OK) {
                sb.append("✅ Revision de seguridad: no se encontro nada sospechoso. (Esto no garantiza que sea seguro.)");
                return sb.toString();
            }
            if (level == DANGER) {
                sb.append("⛔ NO INSTALAR. Este plugin contiene funciones peligrosas:");
            } else {
                sb.append("⚠️ Precaucion. Este plugin usa funciones delicadas:");
            }
            for (int pass = 2; pass >= 1; pass--) {
                for (Finding f : findings) {
                    if (f.level != pass) {
                        continue;
                    }
                    sb.append("\n").append(pass == DANGER ? "⛔ " : "⚠️ ").append(f.text);
                    if (f.line > 0) {
                        sb.append(" (linea ").append(f.line).append(")");
                    }
                }
            }
            return sb.toString();
        }
    }

    private static class Rule {
        final int level;
        final String title;
        final Pattern pattern;

        Rule(int level, String title, String regex) {
            this.level = level;
            this.title = title;
            this.pattern = Pattern.compile(regex);
        }
    }

    private static final List<Rule> RULES = new ArrayList<>();

    private static void add(int level, String title, String regex) {
        RULES.add(new Rule(level, title, regex));
    }

    private static final Pattern EXEC = Pattern.compile("(?<![\\w.])(?:exec|eval)\\s*\\(");
    private static final Pattern OBF = Pattern.compile(
            "b64decode|marshal\\.loads|zlib\\.decompress|codecs\\.decode|fromhex|binascii|unhexlify|a85decode|b85decode");
    private static final Pattern NET = Pattern.compile(
            "(?:import|from)\\s+(?:requests|urllib|http\\.client|aiohttp|httpx|socket|websocket|ftplib|smtplib)"
                    + "|HttpURLConnection|OkHttp|java\\.net\\.URL|urlopen\\s*\\(");
    private static final Pattern URL = Pattern.compile("https?://([A-Za-z0-9.\\-]+)");
    private static final String[] SAFE_HOSTS = {
            "t.me", "telegram.me", "telegram.org", "telegra.ph", "github.com", "raw.githubusercontent.com"
    };

    static {
        // ---- PELIGROSO ----
        add(DANGER, "Borra la cuenta de Telegram", "deleteAccount|account_deleteAccount|TL_account_deleteAccount");
        add(DANGER, "Cierra sesiones o la sesion actual",
                "auth_logOut|auth_resetAuthorization|account_resetAuthorization|resetAuthorization|performLogout|clearConfig|(?<![\\w])logOut\\s*\\(");
        add(DANGER, "Lee archivos de sesion o datos privados de la app",
                "tgnet\\d*\\.dat|userconfing|userconfig|cache4\\.db|shared_prefs|getDatabasePath|accountsettings");
        add(DANGER, "Ejecuta comandos del sistema",
                "subprocess|os\\.system|os\\.popen|Runtime\\.getRuntime|ProcessBuilder");
        // ---- PRECAUCION ----
        add(WARN, "Envia mensajes por su cuenta",
                "sendMessage|SendMessagesHelper|send_message|messages_sendMessage|messages_sendMedia|forwardMessages");
        add(WARN, "Borra mensajes o chats",
                "deleteMessages|deleteDialog|messages_deleteHistory|deleteHistory|delete_messages");
        add(WARN, "Se une a canales o grupos solo",
                "joinChannel|channels_joinChannel|importChatInvite|messages_importChatInvite");
        add(WARN, "Cambia tu perfil o nombre de usuario",
                "account_updateProfile|account_updateUsername|updateUsername");
        add(WARN, "Accede a tus contactos o tu telefono", "ContactsController|clientPhone|getCurrentUser\\(\\)\\.phone");
        add(WARN, "Borra archivos o carpetas", "os\\.remove|os\\.unlink|shutil\\.rmtree|rmtree");
        add(WARN, "Elige funciones por nombre dinamico (puede esconder acciones)", "getattr\\s*\\([^,\\n]+,\\s*[^\"'\\s)]");
        add(WARN, "Arma texto con chr() (tipica tecnica de ofuscacion)", "chr\\s*\\(\\s*\\d+\\s*\\)\\s*\\+|join\\s*\\(\\s*chr");
        add(WARN, "Contiene textos codificados en hexadecimal", "(?:\\\\x[0-9a-fA-F]{2}){8,}");
    }

    private static int lineOf(String src, int index) {
        int line = 1;
        for (int i = 0; i < index && i < src.length(); i++) {
            if (src.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    public static Result scan(String src) {
        Result res = new Result();
        if (src == null || src.length() == 0) {
            return res;
        }
        try {
            for (Rule r : RULES) {
                Matcher m = r.pattern.matcher(src);
                if (m.find()) {
                    res.add(r.level, r.title, lineOf(src, m.start()));
                }
            }
            Matcher exec = EXEC.matcher(src);
            boolean hasExec = exec.find();
            int execLine = hasExec ? lineOf(src, exec.start()) : 0;
            Matcher obf = OBF.matcher(src);
            boolean hasObf = obf.find();
            int obfLine = hasObf ? lineOf(src, obf.start()) : 0;
            if (hasExec && hasObf) {
                res.add(DANGER, "Ejecuta codigo oculto (exec/eval junto con base64/zlib/marshal)", execLine);
            } else if (hasExec) {
                res.add(WARN, "Ejecuta codigo dinamico (exec/eval)", execLine);
            } else if (hasObf) {
                res.add(WARN, "Decodifica datos ocultos (base64/zlib)", obfLine);
            }
            Matcher net = NET.matcher(src);
            if (net.find()) {
                Set<String> hosts = new LinkedHashSet<>();
                Matcher u = URL.matcher(src);
                while (u.find() && hosts.size() < 3) {
                    String h = u.group(1).toLowerCase();
                    boolean safe = false;
                    for (String s : SAFE_HOSTS) {
                        if (h.equals(s) || h.endsWith("." + s)) {
                            safe = true;
                            break;
                        }
                    }
                    if (!safe) {
                        hosts.add(h);
                    }
                }
                String t = "Se conecta a internet";
                if (!hosts.isEmpty()) {
                    StringBuilder hs = new StringBuilder();
                    for (String h : hosts) {
                        if (hs.length() > 0) {
                            hs.append(", ");
                        }
                        hs.append(h);
                    }
                    t += ": " + hs;
                }
                res.add(WARN, t, lineOf(src, net.start()));
            }
        } catch (Throwable t) {
            res.add(WARN, "No se pudo analizar el plugin por completo", 0);
        }
        return res;
    }
}
