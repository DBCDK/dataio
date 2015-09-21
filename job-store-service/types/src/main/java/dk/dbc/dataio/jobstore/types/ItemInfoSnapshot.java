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

    @JsonCreator
    public ItemInfoSnapshot(@JsonProperty("itemNumber") int itemNumber,
                            @JsonProperty("itemId") short itemId,
                            @JsonProperty("chunkId") int chunkId,
                            @JsonProperty("jobId") int jobId,
                            @JsonProperty("timeOfCreation") Date timeOfCreation,
                            @JsonProperty("timeOfLastModification") Date timeOfLastModification,
                            @JsonProperty("timeOfCompletion") Date timeOfCompletion,
                            @JsonProperty("state") State state) {

        this.itemNumber = itemNumber;
        this.itemId = itemId;
        this.chunkId = chunkId;
        this.jobId = jobId;
        this.timeOfCreation = (timeOfCreation == null) ? null : new Date(timeOfCreation.getTime());
        this.timeOfLastModification = (timeOfLastModification ==  null) ? null : new Date(timeOfLastModification.getTime());
        this.timeOfCompletion = (timeOfCompletion == null) ? null : new Date(timeOfCompletion.getTime());
        this.state = state;
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
}
