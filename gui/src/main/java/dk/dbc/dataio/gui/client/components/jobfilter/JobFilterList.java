package dk.dbc.dataio.gui.client.components.jobfilter;

import java.util.Arrays;
import java.util.List;

/**
 * This class is the configuration of which filters constitutes the list of Job Filters
 * in the Jobs List
 */
final public class JobFilterList {

    private JobFilterList() {}  // The constructor is private in order to prevent instantiation - implementing this class as a static class

    /**
     * The list of Job Filters
     * Add new Job Filters here...
     */
    private static List<? extends BaseJobFilter> jobFilterList = Arrays.asList(
        new SinkJobFilter()
    );

    /**
     * Getter for the Job Filter List
     * @return The list of Job Filters
     */
    public static List<? extends BaseJobFilter> getJobFilterList() {
        return jobFilterList;
    }
}
