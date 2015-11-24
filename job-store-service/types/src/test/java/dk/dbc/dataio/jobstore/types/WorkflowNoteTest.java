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

import org.junit.Test;

public class WorkflowNoteTest {

    final static boolean PROCESSED = false;
    final static String ASSIGNEE = "initials";
    final static String DESCRIPTION = "description";

    @Test(expected = NullPointerException.class)
    public void constructor_assigneeArgIsNull_throws() {
        new WorkflowNote(PROCESSED, null, DESCRIPTION);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_assigneeArgIsEmpty_throws() {
        new WorkflowNote(PROCESSED, "", DESCRIPTION);
    }

    @Test
    public void constructor_descriptionArgIsNull_WorkflowNoteCreated() {
        new WorkflowNote(PROCESSED, ASSIGNEE, null);
    }

    @Test
    public void constructor_descriptionArgIsEmpty_WorkflowNoteCreated() {
        new WorkflowNote(PROCESSED, ASSIGNEE, null);
    }

    @Test
    public void constructor_allArgsAreValid_WorkflowNoteCreated() {
        new WorkflowNote(PROCESSED, ASSIGNEE, DESCRIPTION);
    }
}
