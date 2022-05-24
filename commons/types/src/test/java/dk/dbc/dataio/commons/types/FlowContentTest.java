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

package dk.dbc.dataio.commons.types;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * FlowContent unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class FlowContentTest {
    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final List<FlowComponent> COMPONENTS = Collections.singletonList(FlowComponentTest.newFlowComponentInstance());
    private static final Date TIME_OF_FLOW_COMPONENT_UPDATE = new Date();

    @Test(expected = NullPointerException.class)
    public void constructor_nameArgIsNull_throws() {
        new FlowContent(null, DESCRIPTION, COMPONENTS, TIME_OF_FLOW_COMPONENT_UPDATE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_nameArgIsEmpty_throws() {
        new FlowContent("", DESCRIPTION, COMPONENTS, TIME_OF_FLOW_COMPONENT_UPDATE);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_descriptionArgIsNull_throws() {
        new FlowContent(NAME, null, COMPONENTS, TIME_OF_FLOW_COMPONENT_UPDATE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_descriptionArgIsEmpty_throws() {
        new FlowContent(NAME, "", COMPONENTS, TIME_OF_FLOW_COMPONENT_UPDATE);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_componentsArgIsNull_throws() {
        new FlowContent(NAME, DESCRIPTION, null, TIME_OF_FLOW_COMPONENT_UPDATE);
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final FlowContent instance = new FlowContent(NAME, DESCRIPTION, COMPONENTS, TIME_OF_FLOW_COMPONENT_UPDATE);
        assertThat(instance, is(notNullValue()));
    }

    @Test
    public void constructor_componentsArgIsEmpty_returnsNewInstance() {
        final FlowContent instance = new FlowContent(NAME, DESCRIPTION, new ArrayList<>(0), TIME_OF_FLOW_COMPONENT_UPDATE);
        assertThat(instance, is(notNullValue()));
    }

    @Test
    public void constructor_timeOfFlowComponentUpdateArgIsNull_returnsNewInstance() {
        final FlowContent instance = new FlowContent(NAME, DESCRIPTION, COMPONENTS, null);
        assertThat(instance, is(notNullValue()));
    }

    @Test
    public void withFlowComponents_nullValuedFlowComponent_returnsEmptyListOfFlowComponents() {
        final FlowContent instance = new FlowContent(NAME, DESCRIPTION, COMPONENTS, null);
        final FlowComponent flowComponent = null;
        final FlowContent flowContent = instance.withComponents(flowComponent);
        assertThat(flowContent.getComponents(), is(Collections.emptyList()));
    }

    @Test
    public void withFlowComponents_returnsListOfFlowComponents() {
        final FlowContent instance = new FlowContent(NAME, DESCRIPTION, COMPONENTS, null);
        final FlowContent flowContent = instance.withComponents(FlowComponentTest.newFlowComponentInstance(), null);
        assertThat(flowContent.getComponents().size(), is(1));
    }

    @Test
    public void verify_defensiveCopyingOfComponentsList() {
        final List<FlowComponent> components = new ArrayList<>();
        components.add(FlowComponentTest.newFlowComponentInstance());
        final FlowContent instance = new FlowContent(NAME, DESCRIPTION, components, TIME_OF_FLOW_COMPONENT_UPDATE);
        assertThat(instance.getComponents().size(), is(1));
        components.add(null);
        final List<FlowComponent> returnedComponents = instance.getComponents();
        assertThat(returnedComponents.size(), is(1));
        returnedComponents.add(null);
        assertThat(instance.getComponents().size(), is(1));
    }

    public static FlowContent newFlowContentInstance() {
        return new FlowContent(NAME, DESCRIPTION, COMPONENTS);
    }
}
