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

import java.util.Date;

public class JobInfoSnapshot {

    private int jobId;
    private boolean eoj;
    private boolean fatalError;
    private int partNumber;
    private int numberOfChunks;
    private int numberOfItems;
    private Date timeOfCreation;
    private Date timeOfLastModification ;
    private Date timeOfCompletion;
    private JobSpecification specification;
    private State state;
    private FlowStoreReferences flowStoreReferences;

    @JsonCreator
    public JobInfoSnapshot(@JsonProperty ("jobId")int jobId,
                           @JsonProperty ("eoj")boolean eoj,
                           @JsonProperty ("hasFatalError")boolean fatalError,
                           @JsonProperty ("partNumber")int partNumber,
                           @JsonProperty ("numberOfChunks")int numberOfChunks,
                           @JsonProperty ("numberOfItems")int numberOfItems,
                           @JsonProperty ("timeOfCreation")Date timeOfCreation,
                           @JsonProperty ("timeOfLastModification")Date timeOfLastModification,
                           @JsonProperty ("timeOfCompletion")Date timeOfCompletion,
                           @JsonProperty ("specification") JobSpecification specification,
                           @JsonProperty ("state")State state,
                           @JsonProperty ("flowStoreReferences") FlowStoreReferences flowStoreReferences) {

        this.jobId = jobId;
        this.eoj = eoj;
        this.fatalError = fatalError;
        this.partNumber = partNumber;
        this.numberOfChunks = numberOfChunks;
        this.numberOfItems = numberOfItems;
        this.timeOfCreation = (timeOfCreation == null) ? null : new Date(timeOfCreation.getTime());
        this.timeOfLastModification = (timeOfLastModification ==  null) ? null : new Date(timeOfLastModification.getTime());
        this.timeOfCompletion = (timeOfCompletion == null) ? null : new Date(timeOfCompletion.getTime());
        this.specification = specification;
        this.state = state;
        this.flowStoreReferences = flowStoreReferences;
    }

    public int getJobId() {
        return jobId;
    }

    public boolean isEoj() {
        return eoj;
    }

    @JsonProperty
    public boolean hasFatalError() {
        return fatalError;
    }

    public int getPartNumber() {
        return partNumber;
    }

    public int getNumberOfChunks() {
        return numberOfChunks;
    }

    public int getNumberOfItems() {
        return numberOfItems;
    }

    public Date getTimeOfCreation() {
        return this.timeOfCreation == null? null : new Date(this.timeOfCreation.getTime());
    }

    public Date getTimeOfLastModification() {
        return this.timeOfLastModification == null? null : new Date(this.timeOfLastModification.getTime());
    }

    public Date getTimeOfCompletion() {
        return this.timeOfCompletion == null? null : new Date(this.timeOfCompletion.getTime());
    }

    public JobSpecification getSpecification() {
        return specification;
    }

    public State getState() {
        return state;
    }

    public FlowStoreReferences getFlowStoreReferences() {
        return flowStoreReferences;
    }
}
