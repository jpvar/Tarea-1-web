import java.io.IOException;
import java.util.Calendar;
import java.util.logging.*;

public class ServerLog {
    private static FileHandler fileTxt;
    private static SimpleFormatter formatterTxt;
    private static Logger logger;
    public ServerLog() {
        try {
            logger = Logger.getLogger(Thread.currentThread().getStackTrace()[0].getClassName());
            logger.setLevel(Level.INFO);
            fileTxt = new FileHandler("log/console.log", true);
            formatterTxt = new SimpleFormatter();
            fileTxt.setFormatter(formatterTxt);
            logger.addHandler(fileTxt);
        }
        catch (IOException e) {
            logger.warning("Failed to initialize logger handler. " + e.toString());
        }
    }

    public static void log(String method, String server, String referer, String url, String data) {
        StringBuilder message = new StringBuilder();
        message.append("|" + method + "|");
        message.append(server + "|");
        message.append(referer + "|");
        message.append(url + "|");
        message.append(data);
        logger.info(String.valueOf(message));
    }
}