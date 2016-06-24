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

import java.util.ArrayList;
import java.util.List;

public class ItemModel extends GenericBackendModel {

    public enum LifeCycle { PARTITIONING, PROCESSING, DELIVERING, DONE }

    private String itemNumber;
    private String itemId;
    private String chunkId;
    private String jobId;
    private String recordId;
    private LifeCycle lifeCycle;
    private List<DiagnosticModel> diagnosticModels;
    private boolean diagnosticFatal;
    private WorkflowNoteModel workflowNoteModel;
    private String trackingId;

    public ItemModel(
            String itemNumber,
            String itemId,
            String chunkId,
            String jobId,
            String recordId,
            LifeCycle lifeCycle,
            List<DiagnosticModel> diagnosticModels,
            boolean diagnosticFatal,
            WorkflowNoteModel workflowNoteModel,
            String trackingId) {


        this.itemNumber = itemNumber;
        this.itemId = itemId;
        this.chunkId = chunkId;
        this.jobId = jobId;
        this.recordId = recordId;
        this.lifeCycle = lifeCycle;
        this.diagnosticModels = diagnosticModels;
        this.diagnosticFatal = diagnosticFatal;
        this.workflowNoteModel = workflowNoteModel;
        this.trackingId = trackingId;
    }

    public ItemModel() {
        this("1", "0", "0", "0", "0", LifeCycle.PARTITIONING, new ArrayList<DiagnosticModel>(), false, (WorkflowNoteModel)null, "0");
    }

    public String getItemNumber() {
        return itemNumber;
    }

    public String getItemId() {
        return itemId;
    }

    public String getChunkId() {
        return chunkId;
    }

    public String getJobId() {
        return jobId;
    }

    public String getRecordId() {
        return recordId;
    }

    public LifeCycle getStatus() {
        return lifeCycle;
    }

    public List<DiagnosticModel> getDiagnosticModels() {
        return diagnosticModels;
    }

    public boolean isDiagnosticFatal() {
        return diagnosticFatal;
    }

    public WorkflowNoteModel getWorkflowNoteModel() {
        return workflowNoteModel;
    }

    public void setWorkflowNoteModel(WorkflowNoteModel workflowNoteModel) {
        this.workflowNoteModel = workflowNoteModel;
    }

    public String getTrackingId() {
        return trackingId;
    }
}
