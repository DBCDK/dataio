package dk.dbc.dataio.gui.client.components.jobfilter;

import com.google.gwt.core.client.GWT;

import java.util.Arrays;
import java.util.List;

/**
 * This class is the configuration of which filters constitutes the list of Job Filters
 * in the Jobs List
 */
final public class JobFilterList {
    JobFilterGinjector ginjector;
    private List<? extends BaseJobFilter> jobFilterList;

    /**
     * Constructor for the JobFilterList
     * Here, the list of all available Job Filters are listed.
     * Add new Job Filters to the end of the list
     */
    public JobFilterList() {
        ginjector = GWT.create(JobFilterGinjector.class);
        jobFilterList = Arrays.asList(
            ginjector.getSinkJobFilter()
                // Add new Job Filters here...
        );
    }

    public JobFilterList(List<? extends BaseJobFilter> jobFilterList) {
        this.jobFilterList = jobFilterList;
    }

    /**
     * Getter for the Job Filter List
     * @return The list of Job Filters
     */
    public List<? extends BaseJobFilter> getJobFilterList() {
        return jobFilterList;
    }
}
