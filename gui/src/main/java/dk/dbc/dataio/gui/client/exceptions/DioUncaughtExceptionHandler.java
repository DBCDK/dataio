package dk.dbc.dataio.gui.client.exceptions;

import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.event.shared.UmbrellaException;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Catches all uncaught exceptions, and logs them
 * Inspiration from: http://www.summa-tech.com/blog/2012/06/11/7-tips-for-exception-handling-in-gwt/
 */
public class DioUncaughtExceptionHandler implements UncaughtExceptionHandler {
    private static final Logger logger = Logger.getLogger("");

    @Override
    public void onUncaughtException(Throwable e) {
        Throwable unwrapped = unwrap(e);
        if (e != null && e.getMessage() != null && e.getMessage().endsWith("prompt aborted by user")) {
            logger.log(Level.INFO, "'Prompt By User' exception swallowed");
        } else {
            logger.log(Level.SEVERE, "Uncaught GWT Exception", unwrapped);
        }
    }

    /**
     * Unwrap Umbrella Exceptions (if the exception is an Umbrella Exception)
     *
     * @param e The exception
     * @return The unwrapped exception
     */
    private Throwable unwrap(Throwable e) {
        if (e instanceof UmbrellaException) {
            UmbrellaException umbrellaException = (UmbrellaException) e;
            if (umbrellaException.getCauses().size() == 1) {
                return unwrap(umbrellaException.getCauses().iterator().next());
            }
        }
        return e;
    }
}
