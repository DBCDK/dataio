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

package dk.dbc.dataio.flowstore.entity;

import dk.dbc.dataio.commons.utils.test.json.FlowBinderContentJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.SubmitterContentJsonBuilder;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
  * FlowBinder unit tests
  * <p>
  * The test methods of this class uses the following naming convention:
  *
  *  unitOfWork_stateUnderTest_expectedBehavior
  */
public class FlowBinderTest {
    @Test
    public void setContent_jsonDataArgIsValidFlowBinderContentJson_setsNameIndexValue() throws Exception {
        final String name = "testbinder";
        final String flowBinderContent = new FlowBinderContentJsonBuilder()
                .setName(name)
                .build();

        final FlowBinder binder = new FlowBinder();
        binder.setContent(flowBinderContent);
        assertThat(binder.getNameIndexValue(), is(name));
    }

    @Test
    public void setContent_jsonDataArgIsValidFlowBinderContentJson_setsSubmitterIds() throws Exception {
        final Set<Long> submitterIds = new HashSet<>(2);
        submitterIds.add(42L);
        submitterIds.add(43L);
        final String flowBinderContent = new FlowBinderContentJsonBuilder()
                .setSubmitterIds(new ArrayList<>(submitterIds))
                .build();

        final FlowBinder binder = new FlowBinder();
        binder.setContent(flowBinderContent);
        assertThat(binder.getSubmitterIds(), is(submitterIds));
    }

    @Test
    public void setContent_jsonDataArgIsValidFlowBinderContentJson_setsFlowId() throws Exception {
        final long flowId = 42L;
        final String flowBinderContent = new FlowBinderContentJsonBuilder()
                .setFlowId(flowId)
                .build();

        final FlowBinder binder = new FlowBinder();
        binder.setContent(flowBinderContent);
        assertThat(binder.getFlowId(), is(flowId));
    }

    @Test(expected = JSONBException.class)
    public void setContent_jsonDataArgIsInvalidFlowBinderContentJson_throws() throws Exception {
        final FlowBinder binder = new FlowBinder();
        binder.setContent("{}");
    }

    @Test(expected = JSONBException.class)
    public void setContent_jsonDataArgIsInvalidJson_throws() throws Exception {
        final FlowBinder binder = new FlowBinder();
        binder.setContent("{");
    }

    @Test(expected = JSONBException.class)
    public void setContent_jsonDataArgIsEmpty_throws() throws Exception {
        final FlowBinder binder = new FlowBinder();
                binder.setContent("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void setContent_jsonDataArgIsNull_throws() throws Exception {
        final FlowBinder binder = new FlowBinder();
        binder.setContent(null);
    }

    @Test(expected = NullPointerException.class)
    public void generateSearchIndexEntries_flowBinderArgIsNull_throws() throws Exception {
        FlowBinder.generateSearchIndexEntries(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void generateSearchIndexEntries_flowBinderHasNoContent_throws() throws Exception {
        final FlowBinder binder = new FlowBinder();
        FlowBinder.generateSearchIndexEntries(binder);
    }

    @Test
    public void generateSearchIndexEntries_flowBinderIsAttachedToMultipleSubmitters_createsSearchIndexEntryForEachSubmitter() throws Exception {
        final String packaging = "packaging";
        final String format = "format";
        final String charset = "charset";
        final String destination = "destination";
        final Submitter submitter1 = new Submitter();
        final Submitter submitter2 = new Submitter();
        submitter1.setContent(new SubmitterContentJsonBuilder().build());
        submitter2.setContent(new SubmitterContentJsonBuilder().build());

        Set<Submitter> submitters = new HashSet<>(2);
        submitters.add(submitter1);
        submitters.add(submitter2);
        final String flowBinderContent = new FlowBinderContentJsonBuilder()
                .setPackaging(packaging)
                .setFormat(format)
                .setCharset(charset)
                .setDestination(destination)
                .build();

        final FlowBinder binder = new FlowBinder();
        binder.setContent(flowBinderContent);
        binder.setSubmitters(submitters);
        final List<FlowBinderSearchIndexEntry> entries = FlowBinder.generateSearchIndexEntries(binder);
        assertThat(entries.size(), is(2));
    }
}
