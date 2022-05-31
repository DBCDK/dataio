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
 * WorkflowNoteModel holds all GUI data related to showing the WorkflowNote Model
 */
public class WorkflowNoteModel implements Serializable {
    private boolean processed;
    private String assignee;
    private String description;

    public WorkflowNoteModel(boolean processed, String assignee, String description) {
        this.processed = processed;
        this.assignee = assignee;
        this.description = description;
    }

    public WorkflowNoteModel() {
        this(false, "", "");
    }


    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkflowNoteModel)) return false;

        WorkflowNoteModel that = (WorkflowNoteModel) o;

        if (processed != that.processed) return false;
        if (assignee != null ? !assignee.equals(that.assignee) : that.assignee != null) return false;
        return !(description != null ? !description.equals(that.description) : that.description != null);

    }

    @Override
    public int hashCode() {
        int result = processed ? 1 : 0;
        result = 31 * result + (assignee != null ? assignee.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }
}
