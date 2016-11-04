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

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dev.util.collect.HashMap;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Sink Job Filter unit tests
 * <p/>
 * The test methods of this class uses the following naming convention:
 * <p/>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class JobFilterTest {
    @Mock JobFilter mockedParentJobFilter;
    @Mock Scheduler.ScheduledCommand mockedAddCommand1;
    @Mock Scheduler.ScheduledCommand mockedAddCommand2;
    @Mock JobFilterList.JobFilterItem mockedJobFilterItem1;
    @Mock JobFilterList.JobFilterItem mockedJobFilterItem2;
    @Mock BaseJobFilter mockedJobFilter1;
    @Mock BaseJobFilter mockedJobFilter2;
    @Mock ChangeHandler mockedChangeHandler;
    @Mock JobFilterPanel mockedJobFilterPanel;
    @Mock BaseJobFilter mockedBaseJobFilterWidget;
    private List<JobFilterList.JobFilterItem> twoJobFilters = new ArrayList<>();
    private List<JobFilterList.JobFilterItem> emptyJobFilters = new ArrayList<>();

    private boolean addCommand1Executed;
    private boolean addCommand2Executed;

    @Before
    public void prepareJobFilters() {
        mockedJobFilterItem1.jobFilter = mockedJobFilter1;
        mockedJobFilterItem2.jobFilter = mockedJobFilter2;
        when(mockedJobFilter1.getName()).thenReturn("Filter 1");
        when(mockedJobFilter2.getName()).thenReturn("Filter 2");
        when(mockedJobFilter1.getAddCommand(any(JobFilter.class))).thenReturn(mockedAddCommand1);
        when(mockedJobFilter2.getAddCommand(any(JobFilter.class))).thenReturn(mockedAddCommand2);
        addCommand1Executed = false;
        addCommand2Executed = false;
        Mockito.doAnswer(invocationOnMock -> {
            addCommand1Executed = true;
            return null;
        }).when(mockedAddCommand1).execute();
        Mockito.doAnswer(invocationOnMock -> {
            addCommand2Executed = true;
            return null;
        }).when(mockedAddCommand2).execute();
        mockedJobFilterItem1.activeOnStartup = false;
        mockedJobFilterItem2.activeOnStartup = true;
        twoJobFilters.clear();
        twoJobFilters.add(mockedJobFilterItem1);
        twoJobFilters.add(mockedJobFilterItem2);
    }


    //
    // Tests starts here...
    //

    @Test
    public void constructor_emptyConstructor_instantiatedCorrectly() {
        // Activate Subject Under Test
        JobFilter jobFilter = new JobFilter();
        JobFilterList jobFilterList = new JobFilterList();

        // Verify test
        assertThat(jobFilter.availableJobFilters, is(notNullValue()));
        assertThat(jobFilter.availableJobFilters.getJobFilterList().size(), is(jobFilterList.getJobFilterList().size()));
    }

    @Test
    public void constructor_nullJobFilterList_instantiatedCorrectly() {
        // Activate Subject Under Test
        JobFilter jobFilter = new JobFilter(null);

        // Verify test
        assertThat(jobFilter.availableJobFilters, is(nullValue()));
    }

    @Test
    public void constructor_validJobFilterList_instantiatedCorrectly() {
        // Activate Subject Under Test
        JobFilterList jobFilterList = new JobFilterList(twoJobFilters);
        JobFilter jobFilter = new JobFilter(jobFilterList);

        // Verify test
        assertThat(jobFilter.availableJobFilters, is(jobFilterList));
    }

    @Test
    public void onLoad_instantiate_instantiatedCorrectly() {
        // Test preparation
        JobFilter jobFilter = new JobFilter(new JobFilterList(twoJobFilters));

        // Activate Subject Under Test
        jobFilter.onLoad();

        // Verify test
        ArgumentCaptor<String> menuTextArgument = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Scheduler.ScheduledCommand> menuCommandArgument = ArgumentCaptor.forClass(Scheduler.ScheduledCommand.class);

        verify(jobFilter.filterMenu, times(2)).addItem(menuTextArgument.capture(), menuCommandArgument.capture());

        List<String> capturedValues = menuTextArgument.getAllValues();
        assertThat(capturedValues.size(), is(2));
        assertThat(capturedValues.get(0), is("Filter 1"));
        assertThat(capturedValues.get(1), is("Filter 2"));
        assertThat(addCommand1Executed, is(false));
        assertThat(addCommand2Executed, is(true));
    }

    @Test
    public void addChangeHandler_callAddChangeHandler_changeHandlerAdded() {
        // Test Preparation
        JobFilter jobFilter = new JobFilter(new JobFilterList(twoJobFilters));
        jobFilter.onLoad();

        // Activate Subject Under Test
        HandlerRegistration handlerRegistration = jobFilter.addChangeHandler(mockedChangeHandler);

        // Verify test
        assertThat(jobFilter.changeHandler, is(notNullValue()));
        assertThat(handlerRegistration, is(notNullValue()));

        // Call HandlerRegistration->removeHandler
        handlerRegistration.removeHandler();

        // Verify Test
        assertThat(jobFilter.changeHandler, is(nullValue()));
    }

    @Test
    public void add_callAddWithNullValue_addNoJobFilters() {
        // Test Preparation
        JobFilter jobFilter = new JobFilter(new JobFilterList(emptyJobFilters));
        jobFilter.onLoad();

        // Activate Subject Under Test
        jobFilter.add(null);

        // Verify test
        verifyNoMoreInteractions(jobFilter.jobFilterPanel);
    }

    @Test
    public void add_callAddWithNotNullValue_addJobFilter() {
        // Test Preparation
        JobFilter jobFilter = new JobFilter(new JobFilterList(emptyJobFilters));
        jobFilter.onLoad();
        mockedJobFilterItem1.jobFilter.filterPanel = mockedJobFilterPanel;
        jobFilter.changeHandler = null;

        // Activate Subject Under Test
        jobFilter.add(mockedJobFilter1);

        // Verify test
        verify(jobFilter.jobFilterPanel).add(mockedJobFilterPanel);
        verify(mockedJobFilter1).addChangeHandler(any(ChangeHandler.class));
        verify(mockedJobFilter1).setFocus(true);
        verifyNoMoreInteractions(mockedChangeHandler);

        // Setup a change handler
        jobFilter.changeHandler = mockedChangeHandler;

        // Activate Subject Under Test once again
        jobFilter.add(mockedJobFilter1);

        // Verify test
        verify(mockedChangeHandler).onChange(any(ChangeEvent.class));
        verify(mockedJobFilter1, times(2)).addChangeHandler(any(ChangeHandler.class));
        verify(mockedJobFilter1, times(2)).setFocus(true);
        verifyNoMoreInteractions(mockedJobFilter1);
    }

    @Test
    public void remove_callRemoveWithNullValue_removeNoJobFilters() {
        // Test Preparation
        JobFilter jobFilter = new JobFilter(new JobFilterList(emptyJobFilters));
        jobFilter.onLoad();
        mockedJobFilter1.filterPanel = mockedJobFilterPanel;
        jobFilter.add(mockedJobFilter1);

        // Activate Subject Under Test
        jobFilter.remove(null);

        // Verify test
        verify(jobFilter.jobFilterPanel).add(mockedJobFilterPanel);
        verifyNoMoreInteractions(jobFilter.jobFilterPanel);
    }

    @Test
    public void remove_callRemoveWithNullPanelValue_removeNoJobFilters() {
        // Test Preparation
        JobFilter jobFilter = new JobFilter(new JobFilterList(emptyJobFilters));
        jobFilter.onLoad();
        mockedJobFilter1.filterPanel = mockedJobFilterPanel;
        jobFilter.add(mockedJobFilter1);
        mockedJobFilter1.filterPanel = null;

        // Activate Subject Under Test

        jobFilter.remove(mockedJobFilter1);

        // Verify test
        verify(jobFilter.jobFilterPanel).add(mockedJobFilterPanel);
        verifyNoMoreInteractions(jobFilter.jobFilterPanel);
    }

    @Test
    public void remove_callRemoveWithNotNullValue_removeJobFilter() {
        // Test Preparation
        JobFilter jobFilter = new JobFilter(new JobFilterList(emptyJobFilters));
        jobFilter.onLoad();
        mockedJobFilter1.filterPanel = mockedJobFilterPanel;
        jobFilter.add(mockedJobFilter1);
        jobFilter.changeHandler = mockedChangeHandler;

        // Activate Subject Under Test

        jobFilter.remove(mockedJobFilter1);

        // Verify test
        verify(jobFilter.jobFilterPanel).add(mockedJobFilterPanel);
        verify(jobFilter.jobFilterPanel).remove(mockedJobFilterPanel);
        assertThat(mockedJobFilter1.filterPanel, is(nullValue()));
        verify(mockedChangeHandler).onChange(any(ChangeEvent.class));
    }

    @Test
    public void getValue_getValueWithNoFilters_returnNonMergedModel() {
        // Test Preparation
        JobFilter jobFilter = new JobFilter(new JobFilterList(emptyJobFilters));
        jobFilter.onLoad();

        // Activate Subject Under Test
        JobListCriteria model = jobFilter.getValue();

        // Verify test
        assertThat(model.getFiltering().size(), is(0));
    }

    @Test
    public void getValue_getValueWithOneSinkFilter_returnSinkMergedModel() {
        // Test Preparation
        JobFilter jobFilter = new JobFilter(new JobFilterList(twoJobFilters));
        jobFilter.onLoad();
        JobListCriteria filterModel = new JobListCriteria()
                .where(new ListFilter<>(JobListCriteria.Field.SINK_ID, ListFilter.Op.EQUAL, "12345"));

        setupJobFilterToReturnModel(jobFilter, filterModel);

        // Activate Subject Under Test
        JobListCriteria model = jobFilter.getValue();
        assertThat("filter is correct", model, is(filterModel));
    }

    @Test(expected = NullPointerException.class)
    public void setupFilterParameters_nullInput_throw() {
        // Test Preparation
        JobFilter jobFilter = new JobFilter(new JobFilterList(twoJobFilters));
        jobFilter.onLoad();

        // Activate Subject Under Test
        jobFilter.setupFilterParameters(null);
    }

    @Test
    public void setupFilterParameters_emptyInput_noAction() {
        // Test Preparation
        List<JobFilterList.JobFilterItem> mockedJobFilters = mock(List.class);
        JobFilter jobFilter = new JobFilter(new JobFilterList(mockedJobFilters));
        jobFilter.onLoad();

        // Activate Subject Under Test
        jobFilter.setupFilterParameters(new HashMap<>());

        // Verify test
        verify(mockedJobFilters).forEach(any());
        verifyNoMoreInteractions(mockedJobFilters);
    }

    @Test
    public void setupFilterParameters_twoValidParametersOneActual_addActualFilterParameter() {
        // Test Preparation
        JobFilter jobFilter = new JobFilter(new JobFilterList(twoJobFilters));
        jobFilter.onLoad();
        mockedJobFilterItem1.jobFilter = mockedJobFilter1;
        mockedJobFilterItem2.jobFilter = mockedJobFilter2;

        // Activate Subject Under Test
        Map<String, String> testParameters = new HashMap<>();
        testParameters.put("non-existing-classname", "filterparameter=1");
        testParameters.put(mockedJobFilter2.getClass().getSimpleName(), "filterparameter=2");
        jobFilter.setupFilterParameters(testParameters);

        // Verify test
        verify(mockedJobFilter1).getName();
        verify(mockedJobFilter1).getAddCommand(any());
        verify(mockedJobFilter1).removeJobFilter();
        verify(mockedJobFilter1).setParameter("filterparameter=2");
        verify(mockedJobFilter1).addJobFilter();
        verifyNoMoreInteractions(mockedJobFilter1);
        verify(mockedJobFilter2).getName();
        verify(mockedJobFilter2, times(2)).getAddCommand(any());  // Filter 2 has activeOnStartup set to true
        verify(mockedJobFilter2).removeJobFilter();
        verifyNoMoreInteractions(mockedJobFilter2);
    }

    /*
     * Private stuff
     */

    private void setupJobFilterToReturnModel(JobFilter jobFilter, JobListCriteria model) {
        when(jobFilter.jobFilterPanel.iterator()).thenReturn(new Iterator<Widget>() {
            boolean next = true;
            @Override
            public boolean hasNext() {
                if (next) {
                    next = false;
                    return true;
                } else {
                    return false;
                }
            }
            @Override
            public Widget next() {
                return mockedJobFilterPanel;
            }
        });
        when(mockedJobFilterPanel.iterator()).thenReturn(new Iterator<Widget>() {
            boolean next = true;
            @Override
            public boolean hasNext() {
                if (next) {
                    next = false;
                    return true;
                } else {
                    return false;
                }
            }
            @Override
            public Widget next() {
                return mockedBaseJobFilterWidget;
            }
        });
        when(mockedBaseJobFilterWidget.getValue()).thenReturn(model);
    }

}
