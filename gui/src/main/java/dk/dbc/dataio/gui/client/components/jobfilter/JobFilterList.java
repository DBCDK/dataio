/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.gui.client.components.jobfilter;

import dk.dbc.dataio.gui.client.pages.job.show.ShowAcctestJobsPlace;
import dk.dbc.dataio.gui.client.pages.job.show.ShowJobsPlace;
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
     * @param place The Place Class for the jobs list in question
     * @return The list of Job Filters
     */
    List<JobFilterItem> getJobFilters(String place) {
        return jobFilters.get(place);
    }
}
