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

package dk.dbc.dataio.jobstore.test.types;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.State;

import java.util.Date;

public class JobInfoSnapshotBuilder {
    private int jobId = 1;
    private boolean eoj = true;
    private boolean fatalError = false;
    private int partNumber = 0;
    private int numberOfChunks = 2;
    private int numberOfItems = 11;
    private Date timeOfCreation = new Date();
    private Date timeOfLastModification = new Date();
    private Date timeOfCompletion = new Date();
    private JobSpecification specification = new JobSpecificationBuilder().build();
    private State state = new State();
    private FlowStoreReferences flowStoreReferences = new FlowStoreReferencesBuilder().build();

    public JobInfoSnapshotBuilder setEoj(boolean eoj) {
        this.eoj = eoj;
        return this;
    }

    public JobInfoSnapshotBuilder setFatalError(boolean fatalError) {
        this.fatalError = fatalError;
        return this;
    }

    public JobInfoSnapshotBuilder setJobId(int jobId) {
        this.jobId = jobId;
        return this;
    }

    public JobInfoSnapshotBuilder setNumberOfChunks(int numberOfChunks) {
        this.numberOfChunks = numberOfChunks;
        return this;
    }

    public JobInfoSnapshotBuilder setNumberOfItems(int numberOfItems) {
        this.numberOfItems = numberOfItems;
        return this;
    }

    public JobInfoSnapshotBuilder setPartNumber(int partNumber) {
        this.partNumber = partNumber;
        return this;
    }

    public JobInfoSnapshotBuilder setFlowStoreReferences(FlowStoreReferences flowStoreReferences) {
        this.flowStoreReferences = flowStoreReferences;
        return this;
    }

    public JobInfoSnapshotBuilder setSpecification(JobSpecification specification) {
        this.specification = specification;
        return this;
    }

    public JobInfoSnapshotBuilder setState(State state) {
        this.state = state;
        return this;
    }

    public JobInfoSnapshotBuilder setTimeOfCompletion(Date timeOfCompletion) {
        this.timeOfCompletion = (timeOfCompletion == null) ? null : new Date(timeOfCompletion.getTime());
        return this;
    }

    public JobInfoSnapshotBuilder setTimeOfCreation(Date timeOfCreation) {
        this.timeOfCreation = (timeOfCreation == null) ? null : new Date(timeOfCreation.getTime());
        return this;
    }

    public JobInfoSnapshotBuilder setTimeOfLastModification(Date timeOfLastModification) {
        this.timeOfLastModification = (timeOfLastModification == null) ? null : new Date(timeOfLastModification.getTime());
        return this;
    }

    public JobInfoSnapshot build() {
        return new JobInfoSnapshot(jobId, eoj, fatalError, partNumber, numberOfChunks, numberOfItems, timeOfCreation,
                timeOfLastModification, timeOfCompletion, specification, state, flowStoreReferences);
    }
}
