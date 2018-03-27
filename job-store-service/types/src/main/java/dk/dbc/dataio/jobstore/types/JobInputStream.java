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

package dk.dbc.dataio.jobstore.types;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.invariant.InvariantUtil;

public class JobInputStream {

    private final JobSpecification jobSpecification;
    private final boolean isEndOfJob;
    private final int partNumber;

    /**
     * Class constructor
     * @param jobSpecification the jobSpecification
     * @param isEndOfJob boolean indicating if this is last input resulting in end of job
     * @param partNumber the partNumber
     * @throws NullPointerException if given null-valued argument
     * @throws IllegalArgumentException if value of partNumber is less than 0
     */
    @JsonCreator
    public JobInputStream (@JsonProperty ("jobSpecification") JobSpecification jobSpecification,
                           @JsonProperty("isEndOfJob") boolean isEndOfJob,
                           @JsonProperty("partNumber") int partNumber) throws NullPointerException, IllegalArgumentException {

        this.jobSpecification = InvariantUtil.checkNotNullOrThrow(jobSpecification, "jobSpecification");
        this.isEndOfJob = isEndOfJob;
        if(partNumber < 0) {
            throw new IllegalArgumentException("partNumber must be greater than 0");
        }
        this.partNumber = partNumber;
    }

    public JobInputStream(JobSpecification jobSpecification) {
        this(jobSpecification, false, 0);
    }

    public JobSpecification getJobSpecification() {
        return jobSpecification;
    }

    public boolean getIsEndOfJob() {
        return isEndOfJob;
    }

    public int getPartNumber() {
        return partNumber;
    }
}
