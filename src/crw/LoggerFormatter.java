package crw;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 *
 * @author nbb
 */
public class LoggerFormatter extends Formatter {

    @Override
    public String format(LogRecord lr) {
        StringBuilder b = new StringBuilder();
        StackTraceElement[] trace = (new Throwable()).getStackTrace();

        b.append(lr.getLevel());
        b.append(" ");
        b.append(lr.getMillis());
        b.append(" ");
//        b.append(lr.getSequenceNumber()
//        b.append(" ");
        b.append(lr.getSourceClassName());
        b.append(".");
        b.append(lr.getSourceMethodName());
        b.append(" ");
        if (trace.length >= 7) {
            b.append("(" + trace[6].getLineNumber() + ")");
        }
        b.append(" \"");
        b.append(lr.getMessage());
        b.append("\"");
        b.append(System.getProperty("line.separator"));
        return b.toString();
    }
}
