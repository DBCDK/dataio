package dk.dbc.dataio.gui.client.components.jobfilter;

import java.util.Arrays;
import java.util.List;

/**
 * This class is the configuration of which filters constitutes the list of Job Filters
 * in the Jobs List
 */
final public class JobFilterList {
    /**
     * The list of Job Filters to be used in the Jobs List
     */
    private List<? extends BaseJobFilter> jobFilterList = Arrays.asList(
        new SinkJobFilter()
        // Add new Job Filters here...
            );

    /**
     * Getter for the Job Filter List
     * @return The list of Job Filters
     */
    public List<? extends BaseJobFilter> getJobFilterList() {
        return jobFilterList;
    }
}
