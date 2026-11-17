package tw.nekomimi.nekogram.helpers;

import org.telegram.messenger.FileLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.UserConfig;

import java.io.File;

/** IthaGram: solo lo que usa el motor de plugins. */
public class MessageHelper {
    public static String getPathToMessage(MessageObject messageObject) {
        if (messageObject == null || messageObject.messageOwner == null) {
            return null;
        }
        File file = FileLoader.getInstance(UserConfig.selectedAccount).getPathToMessage(messageObject.messageOwner);
        return file != null ? file.getAbsolutePath() : null;
    }
}
