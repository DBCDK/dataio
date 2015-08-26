package dk.dbc.dataio.gui.client.components.jobfilter;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.model.JobListCriteriaModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
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
    @Mock BaseJobFilter mockedBaseJobFilter1;
    @Mock BaseJobFilter mockedBaseJobFilter2;
    @Mock ChangeHandler mockedChangeHandler;
    @Mock JobFilterPanel mockedJobFilterPanel;
    @Mock BaseJobFilter mockedBaseJobFilterWidget;
    List<BaseJobFilter> twoJobFilters = new ArrayList<BaseJobFilter>();
    List<BaseJobFilter> emptyJobFilters = new ArrayList<BaseJobFilter>();


    @Before
    public void prepareJobFilters() {
        when(mockedBaseJobFilter1.getName()).thenReturn("Filter 1");
        when(mockedBaseJobFilter2.getName()).thenReturn("Filter 2");
        twoJobFilters.clear();
        twoJobFilters.add(mockedBaseJobFilter1);
        twoJobFilters.add(mockedBaseJobFilter2);
    }


    //
    // Tests starts here...
    //
    @Test
    public void constructor_instantiate_instantiatedCorrectly() {
        // Activate Subject Under Test
        JobFilter jobFilter = new JobFilter(new JobFilterList(twoJobFilters));

        // Verify test
        ArgumentCaptor<String> menuTextArgument = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Scheduler.ScheduledCommand> menuCommandArgument = ArgumentCaptor.forClass(Scheduler.ScheduledCommand.class);

        verify(jobFilter.filterMenu, times(2)).addItem(menuTextArgument.capture(), menuCommandArgument.capture());

        List<String> capturedValues = menuTextArgument.getAllValues();
        assertThat(capturedValues.size(), is(2));
        assertThat(capturedValues.get(0), is("Filter 1"));
        assertThat(capturedValues.get(1), is("Filter 2"));
    }

    @Test
    public void addChangeHandler_callAddChangeHandler_changeHandlerAdded() {
        // Test Preparation
        JobFilter jobFilter = new JobFilter(new JobFilterList(twoJobFilters));

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
    public void addChildJobFilter_callAddWithNullValue_addNoJobFilters() {
        // Test Preparation
        JobFilter jobFilter = new JobFilter(new JobFilterList(emptyJobFilters));

        // Activate Subject Under Test
        jobFilter.add(null);

        // Verify test
        verifyNoMoreInteractions(jobFilter.jobFilterPanel);
    }

    @Test
    public void addChildJobFilter_callAddWithNotNullValue_addJobFilter() {
        // Test Preparation
        JobFilter jobFilter = new JobFilter(new JobFilterList(emptyJobFilters));
        mockedBaseJobFilter1.filterPanel = mockedJobFilterPanel;
        jobFilter.changeHandler = null;

        // Activate Subject Under Test
        jobFilter.add(mockedBaseJobFilter1);

        // Verify test
        verify(jobFilter.jobFilterPanel).add(mockedJobFilterPanel);
        verifyNoMoreInteractions(mockedChangeHandler);

        // Setup a change handler
        jobFilter.changeHandler = mockedChangeHandler;

        // Activate Subject Under Test once again
        jobFilter.add(mockedBaseJobFilter1);

        // Verify test
        verify(mockedChangeHandler).onChange(any(ChangeEvent.class));
    }

    @Test
    public void removeChildJobFilter_callRemoveWithNullValue_removeNoJobFilters() {
        // Test Preparation
        JobFilter jobFilter = new JobFilter(new JobFilterList(emptyJobFilters));
        mockedBaseJobFilter1.filterPanel = mockedJobFilterPanel;
        jobFilter.add(mockedBaseJobFilter1);

        // Activate Subject Under Test
        jobFilter.remove(null);

        // Verify test
        verify(jobFilter.jobFilterPanel).add(mockedJobFilterPanel);
        verifyNoMoreInteractions(jobFilter.jobFilterPanel);
    }

    @Test
    public void removeChildJobFilter_callRemoveWithNullPanelValue_removeNoJobFilters() {
        // Test Preparation
        JobFilter jobFilter = new JobFilter(new JobFilterList(emptyJobFilters));
        mockedBaseJobFilter1.filterPanel = mockedJobFilterPanel;
        jobFilter.add(mockedBaseJobFilter1);
        mockedBaseJobFilter1.filterPanel = null;

        // Activate Subject Under Test

        jobFilter.remove(mockedBaseJobFilter1);

        // Verify test
        verify(jobFilter.jobFilterPanel).add(mockedJobFilterPanel);
        verifyNoMoreInteractions(jobFilter.jobFilterPanel);
    }

    @Test
    public void removeChildJobFilter_callRemoveWithNotNullValue_removeJobFilter() {
        // Test Preparation
        JobFilter jobFilter = new JobFilter(new JobFilterList(emptyJobFilters));
        mockedBaseJobFilter1.filterPanel = mockedJobFilterPanel;
        jobFilter.add(mockedBaseJobFilter1);
        jobFilter.changeHandler = mockedChangeHandler;

        // Activate Subject Under Test

        jobFilter.remove(mockedBaseJobFilter1);

        // Verify test
        verify(jobFilter.jobFilterPanel).add(mockedJobFilterPanel);
        verify(jobFilter.jobFilterPanel).remove(mockedJobFilterPanel);
        assertThat(mockedBaseJobFilter1.filterPanel, is(nullValue()));
        verify(mockedChangeHandler).onChange(any(ChangeEvent.class));
    }

    @Test
    public void getValue_getValueWithNoFilters_returnNonMergedModel() {
        // Test Preparation
        JobFilter jobFilter = new JobFilter(new JobFilterList(emptyJobFilters));

        // Activate Subject Under Test
        JobListCriteriaModel model = jobFilter.getValue();

        // Verify test
        assertDefaultJobListCriteriaModel(model);
    }

    @Test
    public void getValue_getValueWithOneSinkFilter_returnSinkMergedModel() {
        // Test Preparation
        JobFilter jobFilter = new JobFilter(new JobFilterList(twoJobFilters));
        JobListCriteriaModel filterModel = new JobListCriteriaModel();
        filterModel.setSinkId("12345");
        setupJobFilterToReturnModel(jobFilter, filterModel);

        // Activate Subject Under Test
        JobListCriteriaModel model = jobFilter.getValue();

        // Verify test
        assertJobListCriteriaModel(model, "12345", JobListCriteriaModel.JobSearchType.PROCESSING_FAILED,
                Arrays.asList("PERSISTENT", "ACCTEST", "TEST", "TRANSIENT"));
    }

    /*
     * Private methods
     */
    private void assertJobListCriteriaModel(JobListCriteriaModel model, String sinkId, JobListCriteriaModel.JobSearchType jobSearchType, List<String> jobTypes) {
        assertThat(model.getSinkId(), is(sinkId));
        assertThat(model.getSearchType(), is(jobSearchType));
        Set<String> modelJobTypes = model.getJobTypes();
        assertThat(modelJobTypes.size(), is(jobTypes.size()));
        for (String jobType: jobTypes) {
            assertThat(modelJobTypes.contains(jobType), is(true));
        }
    }

    private void assertDefaultJobListCriteriaModel(JobListCriteriaModel model) {
        assertJobListCriteriaModel(model, "0", JobListCriteriaModel.JobSearchType.PROCESSING_FAILED,
                Arrays.asList("PERSISTENT", "ACCTEST", "TEST", "TRANSIENT"));
    }

    private void setupJobFilterToReturnModel(JobFilter jobFilter, JobListCriteriaModel model) {
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
