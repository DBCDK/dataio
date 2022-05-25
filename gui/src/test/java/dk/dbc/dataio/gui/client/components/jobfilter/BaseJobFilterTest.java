package dk.dbc.dataio.gui.client.components.jobfilter;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.events.JobFilterPanelEvent;
import dk.dbc.dataio.gui.client.places.AbstractBasePlace;
import dk.dbc.dataio.gui.client.resources.Resources;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Base Job Filter unit tests
 * <p/>
 * The test methods of this class uses the following naming convention:
 * <p/>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class BaseJobFilterTest {
    private String name;
    private Widget thisAsWidget;
    private JobFilter parentJobFilter;
    private JobFilterPanel filterPanel;
    private boolean invertFilter;
    private HandlerRegistration clickHandlerRegistration;
    private boolean instantiateJobFilterMethodCalled = false;

    @Mock
    private JobFilter mockedJobFilter;
    @Mock
    private JobFilterPanel mockedJobFilterPanel;
    @Mock
    private HandlerRegistration mockedClickHandlerRegistration;
    @Mock
    private AbstractBasePlace mockedPlace;
    @Mock
    private ChangeHandler mockedChangeHandler;


    class ConcreteBaseJobFilter extends BaseJobFilter {
        String storedName;

        ConcreteBaseJobFilter(String name, boolean invertFilter) {
            super(mock(Texts.class), mock(Resources.class), invertFilter);
            this.storedName = name;
        }

        @Override
        public String getName() {
            return storedName;
        }

        @Override
        public JobListCriteria getValue() {
            return new JobListCriteria();
        }

        @Override
        public void setParameter(String filterParameter) {
        }

        @Override
        public String getParameter() {
            return "parm";
        }

        public Texts getTexts() {
            return texts;
        }

        Widget getThisAsWidget() {
            return thisAsWidget;
        }

        void setParentJobFilter(JobFilter jobFilter) {
            parentJobFilter = jobFilter;
        }

        JobFilter getParentJobFilter() {
            return parentJobFilter;
        }

        void setFilterPanel(JobFilterPanel filterPanel) {
            this.filterPanel = filterPanel;
        }

        JobFilterPanel getFilterPanel() {
            return filterPanel;
        }

        boolean getInvertFilter() {
            return initialInvertFilterValue;
        }

        void setClickHandlerRegistration(HandlerRegistration reg) {
            this.clickHandlerRegistration = reg;
        }

        HandlerRegistration getClickHandlerRegistration() {
            return clickHandlerRegistration;
        }

        @Override
        public HandlerRegistration addChangeHandler(ChangeHandler changeHandler) {
            return null;
        }

        @Override
        public void localSetParameter(String filterParameter) {
        }
    }

    class BaseJobFilterWithOverriddenAddMethod extends ConcreteBaseJobFilter {
        BaseJobFilterWithOverriddenAddMethod(String name) {
            super(name, true);
            instantiateJobFilterMethodCalled = false;
        }

        @Override
        public void instantiateJobFilter(boolean notifyPlace) {
            instantiateJobFilterMethodCalled = true;
        }
    }

    private void getAttributes(ConcreteBaseJobFilter jobFilter) {
        name = jobFilter.getName();
        thisAsWidget = jobFilter.getThisAsWidget();
        parentJobFilter = jobFilter.getParentJobFilter();
        filterPanel = jobFilter.getFilterPanel();
        invertFilter = jobFilter.getInvertFilter();
        clickHandlerRegistration = jobFilter.getClickHandlerRegistration();
    }

    @Before
    public void clearAttributes() {
        name = null;
        thisAsWidget = null;
        parentJobFilter = null;
        filterPanel = null;
        clickHandlerRegistration = null;
    }


    //
    // Tests starts here...
    //
    @Test
    public void constructor_instantiateWithInvertFilter_instantiatedCorrectly() {
        // Activate Subject Under Test
        ConcreteBaseJobFilter jobFilter = new ConcreteBaseJobFilter("-test name-", true);

        // Verify test
        getAttributes(jobFilter);
        assertThat(name, is("-test name-"));
        assertThat(thisAsWidget, is(notNullValue()));
        assertThat(parentJobFilter, is(nullValue()));
        assertThat(filterPanel, is(nullValue()));
        assertThat(invertFilter, is(true));
        assertThat(clickHandlerRegistration, is(nullValue()));
    }

    @Test
    public void constructor_instantiateWithExcludeFilter_instantiatedCorrectly() {
        // Activate Subject Under Test
        ConcreteBaseJobFilter jobFilter = new ConcreteBaseJobFilter("-test name-", false);

        // Verify test
        getAttributes(jobFilter);
        assertThat(name, is("-test name-"));
        assertThat(thisAsWidget, is(notNullValue()));
        assertThat(parentJobFilter, is(nullValue()));
        assertThat(filterPanel, is(nullValue()));
        assertThat(invertFilter, is(false));
        assertThat(clickHandlerRegistration, is(nullValue()));
    }

    @Test
    public void getAddCommand_callGetAddCommandWithNullParent_returnNullValue() {
        // Test Preparation
        ConcreteBaseJobFilter jobFilter = new ConcreteBaseJobFilter("-test name-", true);

        // Activate Subject Under Test
        Scheduler.ScheduledCommand command = jobFilter.getAddCommand(null);

        // Verify test
        assertThat(command, is(nullValue()));
    }

    @Test
    public void getAddCommand_callGetAddCommandWithNotNullParentTestinstantiateJobFilter_ok() {
        // Test Preparation
        BaseJobFilterWithOverriddenAddMethod jobFilter = new BaseJobFilterWithOverriddenAddMethod("-test name-");

        // Activate Subject Under Test
        Scheduler.ScheduledCommand command = jobFilter.getAddCommand(mockedJobFilter);

        // Verify test
        assertThat(command, is(notNullValue()));
        assertThat(instantiateJobFilterMethodCalled, is(false));
        getAttributes(jobFilter);
        assertThat(parentJobFilter, is(mockedJobFilter));

        // Activate Command
        command.execute();

        // Verify Test
        assertThat(instantiateJobFilterMethodCalled, is(true));
        getAttributes(jobFilter);
        assertThat(parentJobFilter, is(mockedJobFilter));
    }

    @Test
    public void instantiateJobFilter_jobFilterPanelIsNotNull_noAction() {
        // Test Preparation
        ConcreteBaseJobFilter jobFilter = new ConcreteBaseJobFilter("-test name-", true);
        jobFilter.setFilterPanel(mockedJobFilterPanel);

        // Activate Subject Under Test
        jobFilter.instantiateJobFilter(true);

        // Verify test
        getAttributes(jobFilter);
        assertThat(filterPanel, is(mockedJobFilterPanel));  // No action at all...
    }

    @Test
    public void instantiateJobFilter_jobFilterPanelIsNullWithInvertFilter_okAction() {
        // Test Preparation
        ConcreteBaseJobFilter jobFilter = new ConcreteBaseJobFilter("-test name-", true);
        jobFilter.setFilterPanel(null);
        jobFilter.getAddCommand(mockedJobFilter);

        // Activate Subject Under Test
        jobFilter.instantiateJobFilter(true);

        // Verify test
        getAttributes(jobFilter);
        assertThat(filterPanel, is(notNullValue()));
        assertThat(filterPanel.isInvertFilter(), is(true));
        assertThat(clickHandlerRegistration, is(notNullValue()));
        verify(parentJobFilter).add(jobFilter);
    }

    @Test
    public void instantiateJobFilter_jobFilterPanelIsNullWithExcludeFilter_okAction() {
        // Test Preparation
        ConcreteBaseJobFilter jobFilter = new ConcreteBaseJobFilter("-test name-", false);
        jobFilter.setFilterPanel(null);
        jobFilter.getAddCommand(mockedJobFilter);

        // Activate Subject Under Test
        jobFilter.instantiateJobFilter(true);

        // Verify test
        getAttributes(jobFilter);
        assertThat(filterPanel, is(notNullValue()));
        assertThat(filterPanel.isInvertFilter(), is(false));
        assertThat(clickHandlerRegistration, is(notNullValue()));
        verify(parentJobFilter).add(jobFilter);
    }

    @Test
    public void removeJobFilter_jobFilterPanelIsNull_noAction() {
        // Test Preparation
        ConcreteBaseJobFilter jobFilter = new ConcreteBaseJobFilter("-test name-", true);
        jobFilter.setFilterPanel(null);
        jobFilter.setClickHandlerRegistration(mockedClickHandlerRegistration);
        jobFilter.setParentJobFilter(mockedJobFilter);

        // Activate Subject Under Test
        jobFilter.removeJobFilter(true);

        // Verify test
        verifyNoMoreInteractions(mockedClickHandlerRegistration);
        verifyNoMoreInteractions(mockedJobFilterPanel);
        verifyNoMoreInteractions(mockedJobFilter);
    }

    @Test
    public void removeJobFilter_jobFilterPanelIsNotNullParentPanelIsNullNotifyPlace_okAction() {
        // Test Preparation
        ConcreteBaseJobFilter jobFilter = new ConcreteBaseJobFilter("-test name-", true);
        jobFilter.setFilterPanel(mockedJobFilterPanel);
        jobFilter.setClickHandlerRegistration(mockedClickHandlerRegistration);
        jobFilter.setParentJobFilter(mockedJobFilter);
        mockedJobFilter.place = mockedPlace;

        // Activate Subject Under Test
        jobFilter.removeJobFilter(true);

        // Verify test
        verify(mockedPlace).addParameter("ConcreteBaseJobFilter", false, "parm");
        verifyNoMoreInteractions(mockedPlace);
        getAttributes(jobFilter);
        verify(mockedClickHandlerRegistration).removeHandler();
        verify(mockedJobFilterPanel).clear();
        verify(mockedJobFilter).remove(jobFilter);
        assertThat(clickHandlerRegistration, is(nullValue()));
    }

    @Test
    public void removeJobFilter_jobFilterPanelIsNotNullParentPanelIsNullDontNotifyPlace_okAction() {
        // Test Preparation
        ConcreteBaseJobFilter jobFilter = new ConcreteBaseJobFilter("-test name-", true);
        jobFilter.setFilterPanel(mockedJobFilterPanel);
        jobFilter.setClickHandlerRegistration(mockedClickHandlerRegistration);
        jobFilter.setParentJobFilter(mockedJobFilter);
        mockedJobFilter.place = mockedPlace;

        // Activate Subject Under Test
        jobFilter.removeJobFilter(false);

        // Verify test
        verifyNoMoreInteractions(mockedPlace);
        getAttributes(jobFilter);
        verify(mockedClickHandlerRegistration).removeHandler();
        verify(mockedJobFilterPanel).clear();
        verify(mockedJobFilter).remove(jobFilter);
        assertThat(clickHandlerRegistration, is(nullValue()));
    }

    @Test
    public void filterChanged_parentJobFilterNull_noAction() {
        // Test Preparation
        ConcreteBaseJobFilter jobFilter = new ConcreteBaseJobFilter("-test name-", true);
        jobFilter.setParentJobFilter(null);

        // Activate Subject Under Test
        jobFilter.filterChanged();

        // Verify test
        verifyNoMoreInteractions(mockedPlace);
    }

    @Test
    public void filterChanged_parentJobFilterPlaceNull_noAction() {
        // Test Preparation
        ConcreteBaseJobFilter jobFilter = new ConcreteBaseJobFilter("-test name-", true);
        jobFilter.setParentJobFilter(mockedJobFilter);
        mockedJobFilter.place = null;

        // Activate Subject Under Test
        jobFilter.filterChanged();

        // Verify test
        verifyNoMoreInteractions(mockedPlace);
    }

    @Test
    public void filterChanged_filterPanelNull_noAction() {
        // Test Preparation
        ConcreteBaseJobFilter jobFilter = new ConcreteBaseJobFilter("-test name-", true);
        jobFilter.setParentJobFilter(mockedJobFilter);
        mockedJobFilter.place = mockedPlace;
        jobFilter.filterPanel = null;

        // Activate Subject Under Test
        jobFilter.filterChanged();

        // Verify test
        verify(mockedPlace).removeParameter(ConcreteBaseJobFilter.class.getSimpleName());
        verifyNoMoreInteractions(mockedPlace);
    }

    @Test
    public void filterChanged_filterPanelNotNull_noAction() {
        // Test Preparation
        ConcreteBaseJobFilter jobFilter = new ConcreteBaseJobFilter("-test name-", true);
        jobFilter.setParentJobFilter(mockedJobFilter);
        mockedJobFilter.place = mockedPlace;
        jobFilter.filterPanel = mockedJobFilterPanel;

        // Activate Subject Under Test
        jobFilter.filterChanged();

        // Verify test
        verify(mockedPlace).addParameter(ConcreteBaseJobFilter.class.getSimpleName(), false, "parm");
        verifyNoMoreInteractions(mockedPlace);
    }

    @Test
    public void setParameter_callDefaultSetParameter_noException() {
        // Test Preparation
        JobStatusFilter jobFilter = new JobStatusFilter("-test name-", true);

        // Activate Subject Under Test
        jobFilter.setParameter("test");

        // Verify test
        verifyNoMoreInteractions(mockedPlace);
        verifyNoMoreInteractions(mockedJobFilter);
        verifyNoMoreInteractions(mockedJobFilterPanel);
    }

    @Test
    public void getParameter_callDefaultGetParameter_noException() {
        // Test Preparation
        JobStatusFilter jobFilter = new JobStatusFilter("-test name-", true);

        // Activate Subject Under Test
        String value = jobFilter.getParameter();

        // Verify test
        assertThat(value, is(""));
        verifyNoMoreInteractions(mockedPlace);
        verifyNoMoreInteractions(mockedJobFilter);
        verifyNoMoreInteractions(mockedJobFilterPanel);
    }

    @Test
    public void addChangeHandler_callDefaultAddChangeHandler_noException() {
        // Test Preparation
        JobStatusFilter jobFilter = new JobStatusFilter("-test name-", true);

        // Activate Subject Under Test
        jobFilter.addChangeHandler(mockedChangeHandler);

        // Verify test
        verify(mockedChangeHandler).onChange(null);
        verifyNoMoreInteractions(mockedChangeHandler);
        verifyNoMoreInteractions(mockedPlace);
        verifyNoMoreInteractions(mockedJobFilter);
        verifyNoMoreInteractions(mockedJobFilterPanel);
    }

    @Test
    public void handleFilterPanelEvent_removeButton_remove() {
        // Test Preparation
        ConcreteBaseJobFilter jobFilter = new ConcreteBaseJobFilter("-test name-", true);
        jobFilter.setFilterPanel(mockedJobFilterPanel);
        jobFilter.setClickHandlerRegistration(mockedClickHandlerRegistration);
        jobFilter.setParentJobFilter(mockedJobFilter);
        mockedJobFilter.place = mockedPlace;

        // Activate Subject Under Test
        jobFilter.handleFilterPanelEvent(JobFilterPanelEvent.JobFilterPanelButton.REMOVE_BUTTON);

        // Verify test
        verify(mockedPlace).addParameter("ConcreteBaseJobFilter", false, "parm");
        verifyNoMoreInteractions(mockedPlace);
        getAttributes(jobFilter);
        verify(mockedClickHandlerRegistration).removeHandler();
        verify(mockedJobFilterPanel).clear();
        verify(mockedJobFilter).remove(jobFilter);
        assertThat(clickHandlerRegistration, is(nullValue()));
    }

    @Test
    public void handleFilterPanelEvent_plusButton_filterChanged() {
        // Test Preparation
        ConcreteBaseJobFilter jobFilter = new ConcreteBaseJobFilter("-test name-", true);
        jobFilter.setParentJobFilter(mockedJobFilter);
        mockedJobFilter.place = mockedPlace;
        jobFilter.filterPanel = mockedJobFilterPanel;

        // Activate Subject Under Test
        jobFilter.handleFilterPanelEvent(JobFilterPanelEvent.JobFilterPanelButton.PLUS_BUTTON);

        // Verify test
        verify(mockedPlace).addParameter(ConcreteBaseJobFilter.class.getSimpleName(), false, "parm");
        verifyNoMoreInteractions(mockedPlace);
    }

    @Test
    public void handleFilterPanelEvent_minusButton_filterChanged() {
        // Test Preparation
        ConcreteBaseJobFilter jobFilter = new ConcreteBaseJobFilter("-test name-", true);
        jobFilter.setParentJobFilter(mockedJobFilter);
        mockedJobFilter.place = mockedPlace;
        jobFilter.filterPanel = mockedJobFilterPanel;

        // Activate Subject Under Test
        jobFilter.handleFilterPanelEvent(JobFilterPanelEvent.JobFilterPanelButton.MINUS_BUTTON);

        // Verify test
        verify(mockedPlace).addParameter(ConcreteBaseJobFilter.class.getSimpleName(), false, "parm");
        verifyNoMoreInteractions(mockedPlace);
    }

}
