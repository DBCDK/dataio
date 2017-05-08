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

package dk.dbc.dataio.jobstore.types.criteria;

import java.io.Serializable;

/**
 * Job listing ListCriteria implementation
 */
public class JobListCriteria extends ListCriteria<JobListCriteria.Field, JobListCriteria> implements Serializable {
    /**
     * Available criteria fields
     */
    public enum Field implements ListFilterField {
        /**
         * job id
         */
        JOB_ID,
        /**
         * job creation time
         */
        TIME_OF_CREATION,
        /**
         * job last modification time
         */
        TIME_OF_LAST_MODIFICATION,
        /**
         * job completion time
         */
        TIME_OF_COMPLETION,
        /*
         * jobs failed while processing
         */
        STATE_PROCESSING_FAILED,
        /*
         * jobs failed while delivering
         */
        STATE_DELIVERING_FAILED,
        /**
         * sink id for sink referenced by job
         */
        SINK_ID,
        /**
         * job specification
         */
        SPECIFICATION,
        /**
         * jobs failed with a fatal error
         */
        JOB_CREATION_FAILED,
        /**
         * record id
         */
        RECORD_ID,
    }
}
