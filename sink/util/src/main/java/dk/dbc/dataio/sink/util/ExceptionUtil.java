package dk.dbc.dataio.sink.util;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Created by ThomasBerg on 20/10/15.
 */
public class ExceptionUtil {

    public static String stackTraceAsString(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }
}
