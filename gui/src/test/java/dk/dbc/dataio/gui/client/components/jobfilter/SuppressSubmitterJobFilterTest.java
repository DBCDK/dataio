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
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.resources.Resources;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Test for SuppressSubmitterJobFilter
 */

@RunWith(GwtMockitoTestRunner.class)
public class SuppressSubmitterJobFilterTest {
    @Mock Texts mockedTexts;
    @Mock Resources mockedResources;
    @Mock ChangeHandler mockedChangeHandler;

    class TestClickEvent extends ClickEvent {
        protected TestClickEvent() {
            super();
        }
    }

    @Test
    public void filterItemsRadioButtonPressed_validCallback_changeCallbackCalled() {
        // Test Preparation
        SuppressSubmitterJobFilter jobFilter = new SuppressSubmitterJobFilter(mockedTexts, mockedResources, "");
        jobFilter.callbackChangeHandler = mockedChangeHandler;

        // Activate Subject Under Test
        jobFilter.filterItemsRadioButtonPressed(new TestClickEvent());

        // Verify Test
        verify(mockedChangeHandler).onChange(null);
        verifyNoMoreInteractions(mockedChangeHandler);
    }

    @Test
    public void filterItemsRadioButtonPressed_invalidCallback_changeCallbackNotCalled() {
        // Test Preparation
        SuppressSubmitterJobFilter jobFilter = new SuppressSubmitterJobFilter(mockedTexts, mockedResources, "");
        jobFilter.callbackChangeHandler = null;

        // Activate Subject Under Test
        jobFilter.filterItemsRadioButtonPressed(new TestClickEvent());

        // Verify Test
        verifyNoMoreInteractions(mockedChangeHandler);
    }

    @Test
    public void getName_callGetName_fetchesStoredName() {
        // Constants
        final String MOCKED_NAME = "name from mocked Texts";

        // Test Preparation
        SuppressSubmitterJobFilter jobFilter = new SuppressSubmitterJobFilter(mockedTexts, mockedResources, "");
        when(mockedTexts.suppressSubmitterFilter_name()).thenReturn(MOCKED_NAME);

        // Activate Subject Under Test
        String jobFilterName = jobFilter.getName();

        // Verify test
        assertThat(jobFilterName, is(MOCKED_NAME));
    }

    @Test
    public void getValue_suppressButtonNotSet_emptyCriteria() {
        // Test Preparation
        SuppressSubmitterJobFilter jobFilter = new SuppressSubmitterJobFilter(mockedTexts, mockedResources, "");
        when(jobFilter.suppressSubmittersButton.getValue()).thenReturn(false);

        // Activate Subject Under Test
        JobListCriteria criteria = jobFilter.getValue();

        // Verify test
        assertThat(criteria, is(new JobListCriteria()));  // Empty criteria
    }

    @Test
    public void getValue_suppressButtonSet_criteriaSet() {
        // Test Preparation
        SuppressSubmitterJobFilter jobFilter = new SuppressSubmitterJobFilter(mockedTexts, mockedResources, "");
        when(jobFilter.suppressSubmittersButton.getValue()).thenReturn(true);

        // Activate Subject Under Test
        JobListCriteria criteria = jobFilter.getValue();

        // Verify test
        assertThat(criteria, is(new JobListCriteria().where(new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_NOT_LEFT_CONTAINS, "{ \"submitterId\": 870970}"))));
    }

    @Test
    public void setParameterData_emptyValue_showAllButtonSet() {
        // Activate Subject Under Test
        SuppressSubmitterJobFilter jobFilter = new SuppressSubmitterJobFilter(mockedTexts, mockedResources, "");

        // Verify test
        verify(jobFilter.showAllSubmittersButton).setValue(false, true);
        verify(jobFilter.suppressSubmittersButton).setValue(true, true);
        verifyNoMoreInteractions(jobFilter.showAllSubmittersButton);
        verifyNoMoreInteractions(jobFilter.suppressSubmittersButton);
    }

    @Test
    public void setParameterData_nonEmptyValue_suppressButtonSet() {
        // Activate Subject Under Test
        SuppressSubmitterJobFilter jobFilter = new SuppressSubmitterJobFilter(mockedTexts, mockedResources, "non-empty");

        // Verify test
        verify(jobFilter.showAllSubmittersButton).setValue(true, true);
        verify(jobFilter.suppressSubmittersButton).setValue(false, true);
        verifyNoMoreInteractions(jobFilter.showAllSubmittersButton);
        verifyNoMoreInteractions(jobFilter.suppressSubmittersButton);
    }

    @Test
    public void getParameterData_trueValue_correctValueFetched() {
        // Test Preparation
        SuppressSubmitterJobFilter jobFilter = new SuppressSubmitterJobFilter(mockedTexts, mockedResources, "8888");
        when(jobFilter.showAllSubmittersButton.getValue()).thenReturn(true);

        // Activate Subject Under Test
        String result = jobFilter.getParameter();

        // Verify test
        assertThat(result, is("disable"));
    }

    @Test
    public void getParameterData_falseValue_correctValueFetched() {
        // Test Preparation
        SuppressSubmitterJobFilter jobFilter = new SuppressSubmitterJobFilter(mockedTexts, mockedResources, "8888");
        when(jobFilter.showAllSubmittersButton.getValue()).thenReturn(false);

        // Activate Subject Under Test
        String result = jobFilter.getParameter();

        // Verify test
        assertThat(result, is(""));
    }

    @Test
    public void addChangeHandler_callAddChangeHandler_changeHandlerAdded() {
        // Test Preparation
        SuppressSubmitterJobFilter jobFilter = new SuppressSubmitterJobFilter(mockedTexts, mockedResources, "");

        // Activate Subject Under Test
        HandlerRegistration handlerRegistration = jobFilter.addChangeHandler(mockedChangeHandler);

        // Verify test
        assertThat(jobFilter.callbackChangeHandler, is(mockedChangeHandler));
        assertThat(handlerRegistration, not(nullValue()));

        // Activate handlerRegistration's removeHandler
        handlerRegistration.removeHandler();

        // Verify test
        assertThat(jobFilter.callbackChangeHandler, is(nullValue()));
    }


}