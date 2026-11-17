package com.exteragram.messenger.utils.network;

import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;

/** IthaGram: sin actualizacion remota del SDK (el SDK va dentro de la app). */
public class RemoteUtils {
    public static void searchMessages(String query, TLRPC.MessagesFilter filter, Utilities.Callback2<TLRPC.messages_Messages, TLRPC.TL_error> callback) {
        TLRPC.TL_error error = new TLRPC.TL_error();
        error.code = 0;
        error.text = "DISABLED";
        callback.run(null, error);
    }

    public static void searchMessages(String query, TLRPC.MessagesFilter filter, Utilities.Callback2<TLRPC.messages_Messages, TLRPC.TL_error> callback, int timeout) {
        searchMessages(query, filter, callback);
    }
}
