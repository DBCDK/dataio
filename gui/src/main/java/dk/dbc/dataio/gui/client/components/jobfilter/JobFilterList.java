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

import com.google.gwt.core.client.GWT;

import java.util.Arrays;
import java.util.List;

/**
 * This class is the configuration of which filters constitutes the list of Job Filters
 * in the Jobs List
 */
final class JobFilterList {
    private List<JobFilterItem> jobFilterList;

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
     * Add new Job Filters to the end of the list
     */
    JobFilterList() {
        JobFilterGinjector ginjector = GWT.create(JobFilterGinjector.class);
        jobFilterList = Arrays.asList(
                new JobFilterItem(ginjector.getSinkJobFilter(), false),
                new JobFilterItem(ginjector.getSubmitterJobFilter(), false),
                new JobFilterItem(ginjector.getSuppressSubmitterJobFilter(), true),
                new JobFilterItem(ginjector.getDateJobFilter(), false),
                new JobFilterItem(ginjector.getErrorJobFilter(), false)
                // Add new Job Filters here...
        );
    }

    JobFilterList(List<JobFilterItem> jobFilterList) {
        this.jobFilterList = jobFilterList;
    }

    /**
     * Getter for the Job Filter List
     * @return The list of Job Filters
     */
    List<JobFilterItem> getJobFilterList() {
        return jobFilterList;
    }
}
