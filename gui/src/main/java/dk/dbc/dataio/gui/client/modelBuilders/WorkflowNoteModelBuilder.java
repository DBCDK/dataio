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

package dk.dbc.dataio.gui.client.modelBuilders;

import dk.dbc.dataio.gui.client.model.WorkflowNoteModel;

public class WorkflowNoteModelBuilder {

    private boolean processed = false;
    private String assignee = "assignee";
    private String description = "description";

    /**
     * Sets the processed indicator for the workflow note
     * @param processed boolean
     * @return The WorkflowNoteModelBuilder object itself (for chaining)
     */
    public WorkflowNoteModelBuilder setProcessed(boolean processed) {
        this.processed = processed;
        return this;
    }

    /**
     * Sets the assignee for the workflow note
     * @param assignee the person assigned
     * @return The WorkflowNoteModelBuilder object itself (for chaining)
     */
    public WorkflowNoteModelBuilder setAssignee(String assignee) {
        this.assignee = assignee;
        return this;
    }

    /**
     * Sets the description for the workflow note
     * @param description the description
     * @return The WorkflowNoteModelBuilder object itself (for chaining)
     */
    public WorkflowNoteModelBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * Build the WorkflowNoteModel object
     * @return The WorkflowNoteModel object
     */
    public WorkflowNoteModel build() {
        return new WorkflowNoteModel(processed, assignee, description);
    }
}
