package simplemounts.simplemounts.util.database;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Log {

    private final String msg;
    private final String source;
    private final Throwable exception;
    private final String PLUGIN_NAME = "Simple Mounts";

    /**
     * A class for storing and formatting exceptions thrown into an acceptable log format
     * @param msg
     * @param source
     * @param e
     */
    public Log(String msg, String source, Throwable e) {
        this.msg = msg;
        this.source = source;
        exception = e;
    }

    public String toString() {
        final int SIZE = 50;
        String s = "";
        for(int i = 0; i < SIZE; i++) s += "*";

        s += "\n";

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        s += "*" + dtf.format(now) + " | " + PLUGIN_NAME + " | " + source + " | \n";
        s += msg + "\n";
        for(int i = 0; i < SIZE; i++) s += "*";
        s += "\nException Msg\n";

        StringWriter sw = new StringWriter();
        exception.printStackTrace(new PrintWriter(sw));
        s += sw.toString();
        s += "\n";

        return s;

    }

    public String getMsg() {
        return msg;
    }

    public String getSource() {
        return source;
    }

    public Throwable getException() {
        return exception;
    }

    public String getPLUGIN_NAME() {
        return PLUGIN_NAME;
    }
}
