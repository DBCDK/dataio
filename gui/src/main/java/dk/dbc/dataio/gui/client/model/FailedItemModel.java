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

package dk.dbc.dataio.gui.client.model;

import java.io.Serializable;

/**
 * Failed Items Model<br>
 * Holds data to to be used, when showing list of Failed Items
 */
public class FailedItemModel implements Serializable {
    private static final long serialVersionUID = -3264661042687015529L;
    private String jobId;
    private String chunkId;
    private String itemId;
    private String chunkifyState;
    private String processingState;
    private String deliveryState;

    public FailedItemModel(String jobId, String chunkId, String itemId, String chunkifyState, String processingState, String deliveryState) {
        this.jobId = jobId;
        this.chunkId = chunkId;
        this.itemId = itemId;
        this.chunkifyState = chunkifyState;
        this.processingState = processingState;
        this.deliveryState = deliveryState;
    }

    public FailedItemModel() {
        this("", "", "", "", "", "");
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getChunkId() {
        return chunkId;
    }

    public void setChunkId(String chunkId) {
        this.chunkId = chunkId;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getChunkifyState() {
        return chunkifyState;
    }

    public void setChunkifyState(String chunkifyState) {
        this.chunkifyState = chunkifyState;
    }

    public String getProcessingState() {
        return processingState;
    }

    public void setProcessingState(String processingState) {
        this.processingState = processingState;
    }

    public String getDeliveryState() {
        return deliveryState;
    }

    public void setDeliveryState(String deliveryState) {
        this.deliveryState = deliveryState;
    }
}
