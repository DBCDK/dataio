/*
 *
 *  * DataIO - Data IO
 *  * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 *  * Denmark. CVR: 15149043
 *  *
 *  * This file is part of DataIO.
 *  *
 *  * DataIO is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * DataIO is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package dk.dbc.dataio.gui.client.components.jobfilter;

import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.resources.Resources;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 *
 * Test for ItemJobFilter
 */

@RunWith(GwtMockitoTestRunner.class)
public class ItemJobFilterTest {
    @Mock Texts mockedTexts;
    @Mock Resources mockedResources;
    @Mock ValueChangeEvent<String> mockedValueChangeEvent;
    @Mock ChangeHandler mockedChangeHandler;
    @Mock ValueChangeHandler<String> mockedItemJobFilterValueChangeHandler;
    @Mock HandlerRegistration mockedItemHandlerRegistration;


    @Test
    public void getName_callGetName_fetchesStoredName() {
        // Constants
        final String MOCKED_NAME = "name from mocked Texts";

        // Test Preparation
        ItemJobFilter jobFilter = new ItemJobFilter(mockedTexts, mockedResources, "");
        when(mockedTexts.itemFilter_name()).thenReturn(MOCKED_NAME);

        // Activate Subject Under Test
        String jobFilterName = jobFilter.getName();

        // Verify test
        assertThat(jobFilterName, is(MOCKED_NAME));
    }

    @Test
    public void getValue_nullValue_noValueSet() {
        // Test Preparation
        ItemJobFilter jobFilter = new ItemJobFilter(mockedTexts, mockedResources, "");
        when(jobFilter.item.getValue()).thenReturn(null);

        // Activate Subject Under Test
        JobListCriteria criteria = jobFilter.getValue();

        // Verify test
        verify(jobFilter.item).getValue();
        verifyNoMoreInteractions(jobFilter.item);
        assertThat(criteria, is(new JobListCriteria()));
    }

    @Test
    public void getValue_emptyValue_noValueSet() {
        // Test Preparation
        ItemJobFilter jobFilter = new ItemJobFilter(mockedTexts, mockedResources, "");
        when(jobFilter.item.getValue()).thenReturn("");

        // Activate Subject Under Test
        JobListCriteria criteria = jobFilter.getValue();

        // Verify test
        verify(jobFilter.item).getValue();
        verifyNoMoreInteractions(jobFilter.item);
        assertThat(criteria, is(new JobListCriteria()));
    }

    @Test
    public void getValue_zeroValue_noValueSet() {
        // Test Preparation
        ItemJobFilter jobFilter = new ItemJobFilter(mockedTexts, mockedResources, "");
        when(jobFilter.item.getValue()).thenReturn("0");

        // Activate Subject Under Test
        JobListCriteria criteria = jobFilter.getValue();

        // Verify test
        verify(jobFilter.item).getValue();
        verifyNoMoreInteractions(jobFilter.item);
        assertThat(criteria, is(new JobListCriteria().where(new ListFilter<>(JobListCriteria.Field.RECORD_ID, ListFilter.Op.IN, "0"))));
    }

    @Test
    public void getValue_validValue_valueSet() {
        // Test Preparation
        ItemJobFilter jobFilter = new ItemJobFilter(mockedTexts, mockedResources, "");
        when(jobFilter.item.getValue()).thenReturn("7654");

        // Activate Subject Under Test
        JobListCriteria criteria = jobFilter.getValue();

        // Verify test
        verify(jobFilter.item).getValue();
        verifyNoMoreInteractions(jobFilter.item);
        assertThat(criteria, is(new JobListCriteria().where(new ListFilter<>(JobListCriteria.Field.RECORD_ID, ListFilter.Op.IN, "7654"))));
    }

    @Test
    public void setParameterData_emptyValue_noItemSet() {
        // Activate Subject Under Test
        ItemJobFilter jobFilter = new ItemJobFilter(mockedTexts, mockedResources, "");

        // Verify test
        verifyNoMoreInteractions(jobFilter.item);
    }

    @Test
    public void setParameterData_zeroValue_zeroItemSet() {
        // Activate Subject Under Test
        ItemJobFilter jobFilter = new ItemJobFilter(mockedTexts, mockedResources, "0");

        // Verify test
        verify(jobFilter.item).setValue("0", true);
        verifyNoMoreInteractions(jobFilter.item);
    }

    @Test
    public void setParameterData_nonZeroValue_nonZeroItemSet() {
        // Activate Subject Under Test
        ItemJobFilter jobFilter = new ItemJobFilter(mockedTexts, mockedResources, "8888");

        // Verify test
        verify(jobFilter.item).setValue("8888", true);
        verifyNoMoreInteractions(jobFilter.item);
    }

    @Test
    public void getParameterData_validValue_correctValueFetched() {
        // Test Preparation
        ItemJobFilter jobFilter = new ItemJobFilter(mockedTexts, mockedResources, "8888");
        when(jobFilter.item.getValue()).thenReturn("4321");

        // Activate Subject Under Test
        String result = jobFilter.getParameter();

        // Verify test
        assertThat(result, is("4321"));
    }

    @Test
    public void setFocus_trueValue_focusEnabled() {
        // Test Preparation
        ItemJobFilter jobFilter = new ItemJobFilter(mockedTexts, mockedResources, "");

        // Activate Subject Under Test
        jobFilter.setFocus(true);

        // Verify test
        verify(jobFilter.item).setFocus(true);
        verifyNoMoreInteractions(jobFilter.item);
    }

    @Test
    public void setFocus_falseValue_focusDisabled() {
        // Test Preparation
        ItemJobFilter jobFilter = new ItemJobFilter(mockedTexts, mockedResources, "");

        // Activate Subject Under Test
        jobFilter.setFocus(false);

        // Verify test
        verify(jobFilter.item).setFocus(false);
        verifyNoMoreInteractions(jobFilter.item);
    }

    @Test
    public void addValueChangeHandler_callAddValueChangeHandler_valueChangeHandlerAdded() {
        // Test Preparation
        ItemJobFilter jobFilter = new ItemJobFilter(mockedTexts, mockedResources, "");
        when(jobFilter.item.addValueChangeHandler(any(ValueChangeHandler.class))).thenReturn(mockedItemHandlerRegistration);

        // Activate Subject Under Test
        HandlerRegistration handlerRegistration = jobFilter.addChangeHandler(mockedChangeHandler);

        // Verify test
        verify(jobFilter.item).addChangeHandler(mockedChangeHandler);
        assertThat(handlerRegistration, not(nullValue()));
    }


}