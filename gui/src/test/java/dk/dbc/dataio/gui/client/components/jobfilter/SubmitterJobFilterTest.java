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

package dk.dbc.dataio.gui.client.components.jobfilter;

import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.resources.Resources;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * Test for SubmitterJobFilter
 */

@RunWith(GwtMockitoTestRunner.class)
public class SubmitterJobFilterTest {
    @Mock Texts mockedTexts;
    @Mock Resources mockedResources;
    @Mock ValueChangeEvent<String> mockedValueChangeEvent;
    @Mock ChangeHandler mockedChangeHandler;
    @Mock ValueChangeHandler<String> mockedSubmitterJobFilterValueChangeHandler;
    @Mock HandlerRegistration mockedSubmitterHandlerRegistration;

    final String DEFAULT_SUBMITTER_NAME = "default submitter name";

    @Test
    public void getName_callGetName_fetchesStoredName() {
        // Constants
        final String MOCKED_NAME = "name from mocked Texts";

        // Test Preparation
        SubmitterJobFilter jobFilter = new SubmitterJobFilter(mockedTexts, mockedResources);
        when(mockedTexts.submitterFilter_name()).thenReturn(MOCKED_NAME);

        // Activate Subject Under Test
        String jobFilterName = jobFilter.getName();

        // Verify test
        assertThat(jobFilterName, is(MOCKED_NAME));
    }

    @Test
    public void addValueChangeHandler_callAddValueChangeHandler_valueChangeHandlerAdded() {
        // Test Preparation
        SubmitterJobFilter jobFilter = new SubmitterJobFilter(mockedTexts, mockedResources);
        when(jobFilter.submitter.addValueChangeHandler(any(ValueChangeHandler.class))).thenReturn(mockedSubmitterHandlerRegistration);

        // Activate Subject Under Test
        HandlerRegistration handlerRegistration = jobFilter.addChangeHandler(mockedChangeHandler);

        // Verify test
        verify(jobFilter.submitter).addChangeHandler(mockedChangeHandler);
        assertThat(handlerRegistration, not(nullValue()));
    }


}