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

import java.util.Date;


public class ItemInfoSnapshot {

    private int itemNumber;
    private short itemId;
    private int chunkId;
    private int jobId;
    private Date timeOfCreation;
    private Date timeOfLastModification ;
    private Date timeOfCompletion;
    private State state;
    private WorkflowNote workflowNote;
    private RecordInfo recordInfo;
    private String trackingId;

    @JsonCreator
    public ItemInfoSnapshot(@JsonProperty("itemNumber") int itemNumber,
                            @JsonProperty("itemId") short itemId,
                            @JsonProperty("chunkId") int chunkId,
                            @JsonProperty("jobId") int jobId,
                            @JsonProperty("timeOfCreation") Date timeOfCreation,
                            @JsonProperty("timeOfLastModification") Date timeOfLastModification,
                            @JsonProperty("timeOfCompletion") Date timeOfCompletion,
                            @JsonProperty("state") State state,
                            @JsonProperty("workflowNote") WorkflowNote workflowNote,
                            @JsonProperty("recordInfo") RecordInfo recordInfo,
                            @JsonProperty("trackingId") String trackingId) {

        this.itemNumber = itemNumber;
        this.itemId = itemId;
        this.chunkId = chunkId;
        this.jobId = jobId;
        this.timeOfCreation = (timeOfCreation == null) ? null : new Date(timeOfCreation.getTime());
        this.timeOfLastModification = (timeOfLastModification ==  null) ? null : new Date(timeOfLastModification.getTime());
        this.timeOfCompletion = (timeOfCompletion == null) ? null : new Date(timeOfCompletion.getTime());
        this.state = state;
        this.workflowNote = workflowNote;
        this.recordInfo = recordInfo;
        this.trackingId = trackingId;
    }

    public int getItemNumber() {
        return itemNumber;
    }

    public short getItemId() {
        return itemId;
    }

    public int getChunkId() {
        return chunkId;
    }

    public int getJobId() {
        return jobId;
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

    public State getState() {
        return state;
    }

    public WorkflowNote getWorkflowNote() {
        return workflowNote;
    }

    public RecordInfo getRecordInfo() {
        return recordInfo;
    }

    public String getTrackingId() {
        return trackingId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemInfoSnapshot)) return false;

        ItemInfoSnapshot that = (ItemInfoSnapshot) o;

        return itemNumber == that.itemNumber
                && itemId == that.itemId
                && chunkId == that.chunkId
                && jobId == that.jobId
                && !(timeOfCreation != null ? !timeOfCreation.equals(that.timeOfCreation) : that.timeOfCreation != null)
                && !(timeOfLastModification != null ? !timeOfLastModification.equals(that.timeOfLastModification) : that.timeOfLastModification != null)
                && !(timeOfCompletion != null ? !timeOfCompletion.equals(that.timeOfCompletion) : that.timeOfCompletion != null)
                && !(state != null ? !state.equals(that.state) : that.state != null)
                && !(workflowNote != null ? !workflowNote.equals(that.workflowNote) : that.workflowNote != null)
                && !(recordInfo != null ? !recordInfo.equals(that.recordInfo) : that.recordInfo != null)
                && !(trackingId != null ? !trackingId.equals(that.trackingId) : that.trackingId != null);
    }

    @Override
    public int hashCode() {
        int result = itemNumber;
        result = 31 * result + (int) itemId;
        result = 31 * result + chunkId;
        result = 31 * result + jobId;
        result = 31 * result + (timeOfCreation != null ? timeOfCreation.hashCode() : 0);
        result = 31 * result + (timeOfLastModification != null ? timeOfLastModification.hashCode() : 0);
        result = 31 * result + (timeOfCompletion != null ? timeOfCompletion.hashCode() : 0);
        result = 31 * result + (state != null ? state.hashCode() : 0);
        result = 31 * result + (workflowNote != null ? workflowNote.hashCode() : 0);
        result = 31 * result + (recordInfo != null ? recordInfo.hashCode() : 0);
        result = 31 * result + (trackingId != null ? trackingId.hashCode() : 0);
        return result;
    }
}
