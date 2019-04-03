import java.io.IOException;
import java.util.Calendar;
import java.util.logging.*;

public class ServerLog {
    private static FileHandler fileTxt;
    private static SimpleFormatter formatterTxt;
    private static Logger logger;
    public ServerLog() {
        try {
            logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
            logger.setLevel(Level.INFO);
            fileTxt = new FileHandler("log/console.log");
            formatterTxt = new SimpleFormatter();
            fileTxt.setFormatter(formatterTxt);
            logger.addHandler(fileTxt);
        }
        catch (IOException e) {
            logger.warning("Failed to initialize logger handler.");
        }
    }

    public static void log(String method, String server, String referer, String url, String data) {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        StringBuilder message = new StringBuilder();
        message.append("[" + today + "]: ");
        message.append("|" + method + "|");
        message.append("|" + server + "|");
        message.append("|" + referer + "|");
        message.append("|" + url + "|");
        message.append("|" + data + "|");
    }
}