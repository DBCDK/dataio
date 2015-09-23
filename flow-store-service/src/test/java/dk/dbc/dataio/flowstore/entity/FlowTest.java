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

import dk.dbc.dataio.commons.utils.test.json.FlowContentJsonBuilder;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

 /**
  * Flow unit tests
  * <p>
  * The test methods of this class uses the following naming convention:
  *
  *  unitOfWork_stateUnderTest_expectedBehavior
  */
public class FlowTest {
    @Test
    public void setContent_jsonDataArgIsValidFlowContentJson_setsNameIndexValue() throws Exception {
        final String name = "testflow";
        final String flowContent = new FlowContentJsonBuilder()
                .setName(name)
                .build();

        final Flow flow = new Flow();
        flow.setContent(flowContent);
        assertThat(flow.getNameIndexValue(), is(name));
    }

     @Test(expected = JSONBException.class)
     public void setContent_jsonDataArgIsInvalidFlowContent_throws() throws Exception {
         final Flow flow = new Flow();
         flow.setContent("{}");
     }

    @Test(expected = JSONBException.class)
    public void setContent_jsonDataArgIsInvalidJson_throws() throws Exception {
        final Flow flow = new Flow();
        flow.setContent("{");
    }

    @Test(expected = JSONBException.class)
    public void setContent_jsonDataArgIsEmpty_throws() throws Exception {
        final Flow flow = new Flow();
        flow.setContent("");
    }

    @Test(expected = NullPointerException.class)
    public void setContent_jsonDataArgIsNull_throws() throws Exception {
        final Flow flow = new Flow();
        flow.setContent(null);
    }

}
