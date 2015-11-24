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

import dk.dbc.dataio.jobstore.types.WorkflowNote;

public class WorkflowNoteBuilder {

    private boolean processed = false;
    private String assignee = "initials";
    private String description = "description";

    public WorkflowNoteBuilder setProcessed(boolean processed) {
        this.processed = processed;
        return this;
    }

    public WorkflowNoteBuilder setAssignee(String assignee) {
        this.assignee = assignee;
        return this;
    }

    public WorkflowNoteBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public WorkflowNote build() {
        return new WorkflowNote(processed, assignee, description);
    }
}