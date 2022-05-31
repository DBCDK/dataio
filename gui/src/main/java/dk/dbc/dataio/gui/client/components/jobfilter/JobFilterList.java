package dk.dbc.dataio.gui.client.components.jobfilter;

import dk.dbc.dataio.gui.client.pages.job.show.ShowAcctestJobsPlace;
import dk.dbc.dataio.gui.client.pages.job.show.ShowJobsPlace;
import dk.dbc.dataio.gui.client.pages.job.show.ShowPeriodicJobsPlace;
import dk.dbc.dataio.gui.client.pages.job.show.ShowTestJobsPlace;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is the configuration of which filters constitutes the list of Job Filters
 * in the Jobs List
 */
final class JobFilterList {
    private Map<String, List<JobFilterItem>> jobFilters = new HashMap<>();

    class JobFilterItem {
        BaseJobFilter jobFilter;
        boolean activeOnStartup;

        JobFilterItem(BaseJobFilter jobFilter, boolean activeOnStartup) {
            this.jobFilter = jobFilter;
            this.activeOnStartup = activeOnStartup;
        }
    }

    /**
     * Constructor for the JobFilterList
     * Here, the list of all available Job Filters are listed.
     * Add new Job Filters to the end of each of the lists for the Jobs, Test Jobs and Acctest Jobs
     */
    JobFilterList() {
        jobFilters.put(ShowJobsPlace.class.getSimpleName(), Arrays.asList(
                new JobFilterItem(new SinkJobFilter("", false), false),
                new JobFilterItem(new SubmitterJobFilter("", false), false),
                new JobFilterItem(new DateJobFilter("7", false), true),
                new JobFilterItem(new ErrorJobFilter("processing,delivering,jobcreation", false), false),
                new JobFilterItem(new JobStatusFilter("active", false), false),
                new JobFilterItem(new ItemJobFilter("", false), false)
                // Add new Job Filters here...
        ));
        jobFilters.put(ShowPeriodicJobsPlace.class.getSimpleName(), Arrays.asList(
                new JobFilterItem(new SinkJobFilter("", false), false),
                new JobFilterItem(new SubmitterJobFilter("", false), false),
                new JobFilterItem(new DateJobFilter("7", false), true),
                new JobFilterItem(new ErrorJobFilter("processing,delivering,jobcreation", false), false),
                new JobFilterItem(new JobStatusFilter("active", false), false),
                new JobFilterItem(new ItemJobFilter("", false), false)
                // Add new Job Filters here...
        ));
        jobFilters.put(ShowTestJobsPlace.class.getSimpleName(), Arrays.asList(
                new JobFilterItem(new SinkJobFilter("", false), false),
                new JobFilterItem(new SubmitterJobFilter("", false), false),
                new JobFilterItem(new DateJobFilter("", false), false),
                new JobFilterItem(new ErrorJobFilter("processing,delivering,jobcreation", false), false),
                new JobFilterItem(new JobStatusFilter("active", false), false),
                new JobFilterItem(new ItemJobFilter("", false), false)
                // Add new Job Filters here...
        ));
        jobFilters.put(ShowAcctestJobsPlace.class.getSimpleName(), Arrays.asList(
                new JobFilterItem(new SinkJobFilter("", false), false),
                new JobFilterItem(new SubmitterJobFilter("", false), false),
                new JobFilterItem(new DateJobFilter("", false), false),
                new JobFilterItem(new ErrorJobFilter("processing,delivering,jobcreation", false), false),
                new JobFilterItem(new JobStatusFilter("active", false), false),
                new JobFilterItem(new ItemJobFilter("", false), false)
                // Add new Job Filters here...
        ));
    }

    JobFilterList(Map<String, List<JobFilterItem>> jobFilters) {
        this.jobFilters = jobFilters;
    }

    /**
     * Getter for the Job Filter List
     *
     * @param place The Place Class for the jobs list in question
     * @return The list of Job Filters
     */
    List<JobFilterItem> getJobFilters(String place) {
        return jobFilters.get(place);
    }
}
