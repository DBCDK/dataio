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
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Error Job Filter unit tests
 * <p/>
 * The test methods of this class uses the following naming convention:
 * <p/>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class ErrorJobFilterTest {
    @Mock Texts mockedTexts;
    @Mock Resources mockedResources;
    @Mock ValueChangeEvent<String> mockedValueChangeEvent;
    @Mock ChangeHandler mockedChangeHandler;
    @Mock ValueChangeHandler<String> mockedErrorJobFilterValueChangeHandler;
    @Mock HandlerRegistration mockedHandlerRegistration;


    //
    // Tests starts here...
    //
    @Test
    public void constructor_instantiate_instantiatedCorrectly() {
        // Activate Subject Under Test
        ErrorJobFilter jobFilter = new ErrorJobFilter(mockedTexts, mockedResources);

        // Verify test
        assertThat(jobFilter.texts, is(mockedTexts));
        assertThat(jobFilter.resources, is(mockedResources));
    }


    @Test
    public void getName_callGetName_fetchesStoredName() {
        // Constants
        final String MOCKED_NAME = "name from mocked Texts";

        // Test Preparation
        ErrorJobFilter jobFilter = new ErrorJobFilter(mockedTexts, mockedResources);
        when(mockedTexts.errorFilter_name()).thenReturn(MOCKED_NAME);

        // Activate Subject Under Test
        String jobFilterName = jobFilter.getName();

        // Verify test
        assertThat(jobFilterName, is(MOCKED_NAME));
    }

    @Test
    public void addChangeHandler_callAddChangeHandler_changeHandlerAdded() {
        // Test Preparation
        ErrorJobFilter jobFilter = new ErrorJobFilter(mockedTexts, mockedResources);

        // Activate Subject Under Test
        HandlerRegistration handlerRegistration = jobFilter.addChangeHandler(mockedChangeHandler);

        // Verify test
        assertThat(jobFilter.callbackChangeHandler, is(mockedChangeHandler));
        verify(mockedChangeHandler).onChange(null);
        assertThat(handlerRegistration, not(nullValue()));
    }

    @Test
    public void addChangeHandler_callHandlerRegistrationRemoveHandler_changeHandlerRemoved() {
        // Test Preparation
        ErrorJobFilter jobFilter = new ErrorJobFilter(mockedTexts, mockedResources);

        // Activate Subject Under Test
        HandlerRegistration handlerRegistration = jobFilter.addChangeHandler(mockedChangeHandler);
        handlerRegistration.removeHandler();

        // Verify test
        assertThat(jobFilter.callbackChangeHandler, nullValue());
    }

    @Test
    public void changeHandlerCallback_default_callback() {
        // Test Preparation
        ErrorJobFilter jobFilter = new ErrorJobFilter(mockedTexts, mockedResources);
        jobFilter.addChangeHandler(mockedChangeHandler);

        // Activate Subject Under Test
        jobFilter.checkboxValueChanged(null);

        // Verify test
        verify(mockedChangeHandler, times(2)).onChange(null);
    }

    @Test
    public void getValue_defaultValue_returnEmptyJobListCriteria() {
        // Test Preparation
        ErrorJobFilter jobFilter = new ErrorJobFilter(mockedTexts, mockedResources);

        // Activate Subject Under Test
        JobListCriteria criteria = jobFilter.getValue();

        // Verify test
        assertThat(criteria, is(new JobListCriteria()));
    }

    @Test
    public void getValue_processingValue_returnProcessingJobListCriteria() {
        // Test Preparation
        ErrorJobFilter jobFilter = new ErrorJobFilter(mockedTexts, mockedResources);
        when(jobFilter.processingCheckBox.getValue()).thenReturn(true);

        // Activate Subject Under Test
        JobListCriteria criteria = jobFilter.getValue();

        // Verify test
        assertThat(criteria, is(new JobListCriteria().
                where(new ListFilter<>(JobListCriteria.Field.STATE_PROCESSING_FAILED))
        ));
    }

    @Test
    public void getValue_deliveringValue_returnDeliveringJobListCriteria() {
        // Test Preparation
        ErrorJobFilter jobFilter = new ErrorJobFilter(mockedTexts, mockedResources);
        when(jobFilter.deliveringCheckBox.getValue()).thenReturn(true);

        // Activate Subject Under Test
        JobListCriteria criteria = jobFilter.getValue();

        // Verify test
        assertThat(criteria, is(new JobListCriteria().
                where(new ListFilter<>(JobListCriteria.Field.STATE_DELIVERING_FAILED))
        ));
    }

    @Test
    public void getValue_jobCreationValue_returnJobCreationJobListCriteria() {
        // Test Preparation
        ErrorJobFilter jobFilter = new ErrorJobFilter(mockedTexts, mockedResources);
        when(jobFilter.jobCreationCheckBox.getValue()).thenReturn(true);

        // Activate Subject Under Test
        JobListCriteria criteria = jobFilter.getValue();

        // Verify test
        assertThat(criteria, is(new JobListCriteria().
                where(new ListFilter<>(JobListCriteria.Field.WITH_FATAL_ERROR))
        ));
    }

    @Test
    public void getValue_combinedValue_returnCombinedJobListCriteria() {
        // Test Preparation
        ErrorJobFilter jobFilter = new ErrorJobFilter(mockedTexts, mockedResources);
        when(jobFilter.processingCheckBox.getValue()).thenReturn(true);
        when(jobFilter.jobCreationCheckBox.getValue()).thenReturn(true);

        // Activate Subject Under Test
        JobListCriteria criteria = jobFilter.getValue();

        // Verify test
        assertThat(criteria, is(new JobListCriteria().
                where(new ListFilter<>(JobListCriteria.Field.STATE_PROCESSING_FAILED)).
                where(new ListFilter<>(JobListCriteria.Field.WITH_FATAL_ERROR))
        ));
    }

}
