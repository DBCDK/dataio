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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.resources.Resources;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

/**
 * Test for ActiveJobFilter
 */

@RunWith(GwtMockitoTestRunner.class)
public class JobStatusFilterTest {
    @Mock private Texts mockedTexts;
    @Mock private Resources mockedResources;

    class TestClickEvent extends ClickEvent {
        protected TestClickEvent() {
            super();
        }
    }

    @Test
    public void getName_callGetName_fetchesStoredName() {
        // Constants
        final String MOCKED_NAME = "name from mocked Texts";

        // Test Preparation
        JobStatusFilter jobFilter = new JobStatusFilter(mockedTexts, mockedResources, "", true);
        when(mockedTexts.jobStatusFilter_name()).thenReturn(MOCKED_NAME);

        // Activate Subject Under Test
        String jobFilterName = jobFilter.getName();

        // Verify test
        assertThat(jobFilterName, is(MOCKED_NAME));
    }

    @Test
    public void getValue_activeJobs_activeJobsCriteria() {
        // Test Preparation
        JobStatusFilter jobFilter = new JobStatusFilter(mockedTexts, mockedResources, "", true);
        when(jobFilter.activeRadioButton.getValue()).thenReturn(true);
        when(jobFilter.previewRadioButton.getValue()).thenReturn(false);
        when(jobFilter.doneRadioButton.getValue()).thenReturn(false);
        when(jobFilter.failedRadioButton.getValue()).thenReturn(false);

        final JobListCriteria expectedJobListCriteria = new JobListCriteria().where(new ListFilter<>(JobListCriteria.Field.TIME_OF_COMPLETION, ListFilter.Op.IS_NULL));

        // Activate Subject Under Test
        JobListCriteria criteria = jobFilter.getValue();

        // Verify test
        assertThat(criteria, is(expectedJobListCriteria));
    }

    @Test
    public void getValue_previewOnlyJobs_previewOnlyJobsCriteria() {
        // Test Preparation
        JobStatusFilter jobFilter = new JobStatusFilter(mockedTexts, mockedResources, "", true);
        when(jobFilter.activeRadioButton.getValue()).thenReturn(false);
        when(jobFilter.previewRadioButton.getValue()).thenReturn(true);
        when(jobFilter.doneRadioButton.getValue()).thenReturn(false);
        when(jobFilter.failedRadioButton.getValue()).thenReturn(false);

        final JobListCriteria expectedJobListCriteria = new JobListCriteria().where(new ListFilter<>(JobListCriteria.Field.PREVIEW_ONLY));

        // Activate Subject Under Test
        JobListCriteria criteria = jobFilter.getValue();

        // Verify test
        assertThat(criteria, is(expectedJobListCriteria));
    }

    @Test
    public void getValue_doneJobs_doneJobsCriteria() {
        // Test Preparation
        JobStatusFilter jobFilter = new JobStatusFilter(mockedTexts, mockedResources, "", true);
        when(jobFilter.activeRadioButton.getValue()).thenReturn(false);
        when(jobFilter.previewRadioButton.getValue()).thenReturn(false);
        when(jobFilter.doneRadioButton.getValue()).thenReturn(true);
        when(jobFilter.failedRadioButton.getValue()).thenReturn(false);

        final JobListCriteria expectedJobListCriteria = new JobListCriteria().where(new ListFilter<>(JobListCriteria.Field.TIME_OF_COMPLETION, ListFilter.Op.IS_NOT_NULL));

        // Activate Subject Under Test
        JobListCriteria criteria = jobFilter.getValue();

        // Verify test
        assertThat(criteria, is(expectedJobListCriteria));
    }

    @Test
    public void getValue_failedJobs_failedJobsCriteria() {
        // Test Preparation
        JobStatusFilter jobFilter = new JobStatusFilter(mockedTexts, mockedResources, "", true);
        when(jobFilter.activeRadioButton.getValue()).thenReturn(false);
        when(jobFilter.previewRadioButton.getValue()).thenReturn(false);
        when(jobFilter.doneRadioButton.getValue()).thenReturn(false);
        when(jobFilter.failedRadioButton.getValue()).thenReturn(true);

        final JobListCriteria expectedJobListCriteria = new JobListCriteria()
                .where(new ListFilter<>(JobListCriteria.Field.JOB_CREATION_FAILED))
                .or(new ListFilter<>(JobListCriteria.Field.STATE_PROCESSING_FAILED))
                .or(new ListFilter<>(JobListCriteria.Field.STATE_DELIVERING_FAILED));

        // Activate Subject Under Test
        JobListCriteria criteria = jobFilter.getValue();

        // Verify test
        assertThat(criteria, is(expectedJobListCriteria));
    }

}
