package dk.dbc.dataio.gui.client.exceptions;

import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is a specialization of the generic AsyncCallback class
 * The only difference is, that the FilteredAsyncCallback class filters out the
 * RPC Status Code Exception failure.
 *
 * @param <T> the type of the object
 */
public abstract class FilteredAsyncCallback<T> implements AsyncCallback<T> {
    private static final Logger logger = Logger.getLogger(FilteredAsyncCallback.class.getName());

    /**
     * This method is called whenever the caller wants to signal a failure to
     * the receiver.
     * If overridden, this method handles all failures.
     *
     * @param caught the throwable caught
     */
    @Override
    public void onFailure(Throwable caught) {
        if (caught.getClass().getName().contains("rpc.StatusCodeException") &&
                caught.getMessage().trim().equals("0")) {
            logger.log(Level.WARNING, "RPC Status Code Exception swallowed", caught);
        } else {
            onFilteredFailure(caught);
        }
    }

    /**
     * This method is called whenever the caller wants to signal a failure to
     * the receiver.
     * If overridden, this method handles all failures except RPC Status Code Exceptions.
     *
     * @param caught the throwable caught
     */
    public abstract void onFilteredFailure(Throwable caught);

}
