package dk.dbc.dataio.gui.client.components.jobfilter;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.components.TitledDecoratorPanelWithButton;
import dk.dbc.dataio.gui.client.model.JobListCriteriaModel;
import dk.dbc.dataio.gui.client.resources.Resources;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
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
    String name;
    Widget thisAsWidget;
    FlowPanel parentPanel;
    TitledDecoratorPanelWithButton decoratorPanel;
    HandlerRegistration clickHandlerRegistration;
    boolean addJobFilterMethodCalled = false;

    @Mock Texts mockedTexts;
    @Mock Resources mockedResources;
    @Mock FlowPanel mockedPanel;
    @Mock HandlerRegistration mockedClickHandlerRegistration;


    class ConcreteBaseJobFilter extends BaseJobFilter {
        String storedName;
        public ConcreteBaseJobFilter(String name) {
            super(mockedTexts, mockedResources);
            this.storedName = name;
        }
        @Override
        public String getName() {
            return storedName;
        }
        @Override
        public HandlerRegistration addValueChangeHandler(ValueChangeHandler<JobListCriteriaModel> valueChangeHandler) {
            return null;
        }
        public Texts getTexts() {
            return texts;
        }
        public Resources getResources() {
            return resources;
        }
        public Widget getThisAsWidget() {
            return thisAsWidget;
        }
        public void setParentPanel(FlowPanel panel) {
            this.parentPanel = panel;
        }
        public FlowPanel getParentPanel() {
            return parentPanel;
        }
        public void setDecoratorPanel(TitledDecoratorPanelWithButton decoratorPanel) {
            this.decoratorPanel = decoratorPanel;
        }
        public TitledDecoratorPanelWithButton getDecoratorPanel() {
            return decoratorPanel;
        }
        public void setClickHandlerRegistration(HandlerRegistration reg) {
            this.clickHandlerRegistration = reg;
        }
        public HandlerRegistration getClickHandlerRegistration() {
            return clickHandlerRegistration;
        }

        @Override
        public HandlerRegistration addChangeHandler(ChangeHandler changeHandler) {
            return null;
        }
    }

    class BaseJobFilterWithOverriddenAddMethod extends ConcreteBaseJobFilter {
        public BaseJobFilterWithOverriddenAddMethod(String name) {
            super(name);
            addJobFilterMethodCalled = false;
        }
        @Override
        public void addJobFilter() {
            addJobFilterMethodCalled = true;
        }
    }

    private void getAttributes(ConcreteBaseJobFilter jobFilter) {
        name = jobFilter.getName();
        mockedTexts = jobFilter.getTexts();
        mockedResources = jobFilter.getResources();
        thisAsWidget = jobFilter.getThisAsWidget();
        parentPanel = jobFilter.getParentPanel();
        decoratorPanel = jobFilter.getDecoratorPanel();
        clickHandlerRegistration = jobFilter.getClickHandlerRegistration();
    }

    @Before
    public void clearAttributes() {
        name = null;
        thisAsWidget = null;
        parentPanel = null;
        decoratorPanel = null;
        clickHandlerRegistration = null;
    }


    //
    // Tests starts here...
    //
    @Test
    public void constructor_instantiate_instantiatedCorrectly() {
        // Activate Subject Under Test
        ConcreteBaseJobFilter jobFilter = new ConcreteBaseJobFilter("-test name-");

        // Verify test
        getAttributes(jobFilter);
        assertThat(name, is("-test name-"));
        assertThat(mockedTexts, is(notNullValue()));
        assertThat(mockedResources, is(notNullValue()));
        assertThat(thisAsWidget, is(notNullValue()));
        assertThat(parentPanel, is(nullValue()));
        assertThat(decoratorPanel, is(nullValue()));
        assertThat(clickHandlerRegistration, is(nullValue()));
    }

    @Test
    public void getAddCommand_callGetAddCommandWithNullParent_returnNullValue() {
        // Test Preparation
        ConcreteBaseJobFilter jobFilter = new ConcreteBaseJobFilter("-test name-");

        // Activate Subject Under Test
        Scheduler.ScheduledCommand command = jobFilter.getAddCommand(null);

        // Verify test
        assertThat(command, is(nullValue()));
    }

    @Test
    public void getAddCommand_callGetAddCommandWithNotNullParentTestAddJobFilter_ok() {
        // Test Preparation
        BaseJobFilterWithOverriddenAddMethod jobFilter = new BaseJobFilterWithOverriddenAddMethod("-test name-");

        // Activate Subject Under Test
        Scheduler.ScheduledCommand command = jobFilter.getAddCommand(mockedPanel);

        // Verify test
        assertThat(command, is(CoreMatchers.notNullValue()));
        assertThat(addJobFilterMethodCalled, is(false));
        getAttributes(jobFilter);
        assertThat(parentPanel, is(mockedPanel));

        // Activate Command
        command.execute();

        // Verify Test
        assertThat(addJobFilterMethodCalled, is(true));
        getAttributes(jobFilter);
        assertThat(parentPanel, is(mockedPanel));
    }

    @Test
    public void addJobFilter_clickHandlerRegistrationIsNotNull_noAction() {
        // Test Preparation
        ConcreteBaseJobFilter jobFilter = new ConcreteBaseJobFilter("-test name-");
        jobFilter.getAddCommand(mockedPanel);
        jobFilter.setClickHandlerRegistration(new HandlerRegistration() {
            @Override
            public void removeHandler() {
            }
        });
        jobFilter.setDecoratorPanel(null);

        // Activate Subject Under Test
        jobFilter.addJobFilter();

        // Verify test
        getAttributes(jobFilter);
        // Now no action is expected, because handlerRegistration is not null - we simulate, that it has been registered already
        assertThat(decoratorPanel, is(nullValue()));
    }

    @Test
    public void addJobFilter_clickHandlerRegistrationIsNull_okAction() {
        // Test Preparation
        ConcreteBaseJobFilter jobFilter = new ConcreteBaseJobFilter("-test name-");
        jobFilter.setClickHandlerRegistration(null);
        jobFilter.setDecoratorPanel(null);
        jobFilter.setParentPanel(mockedPanel);

        // Activate Subject Under Test
        jobFilter.addJobFilter();

        // Verify test
        getAttributes(jobFilter);
        assertThat(decoratorPanel, is(notNullValue()));
        assertThat(clickHandlerRegistration, is(notNullValue()));
        verify(parentPanel).add(decoratorPanel);
    }

    @Test
    public void removeJobFilter_clickHandlerRegistrationIsNull_noAction() {
        // Test Preparation
        ConcreteBaseJobFilter jobFilter = new ConcreteBaseJobFilter("-test name-");
        jobFilter.setParentPanel(mockedPanel);
        jobFilter.setClickHandlerRegistration(null);

        // Activate Subject Under Test
        jobFilter.removeJobFilter();

        // Verify test
        verifyNoMoreInteractions(mockedPanel);
    }

    @Test
    public void removeJobFilter_clickHandlerRegistrationIsNotNullParentPanelIsNull_okAction() {
        // Test Preparation
        ConcreteBaseJobFilter jobFilter = new ConcreteBaseJobFilter("-test name-");
        jobFilter.setParentPanel(null);
        jobFilter.setClickHandlerRegistration(mockedClickHandlerRegistration);

        // Activate Subject Under Test
        jobFilter.removeJobFilter();

        // Verify test
        verify(mockedClickHandlerRegistration).removeHandler();
        getAttributes(jobFilter);
        assertThat(clickHandlerRegistration, is(nullValue()));
    }

    @Test
    public void removeJobFilter_clickHandlerRegistrationIsNotNullParentPanelIsNotNull_okAction() {
        // Test Preparation
        ConcreteBaseJobFilter jobFilter = new ConcreteBaseJobFilter("-test name-");
        jobFilter.setParentPanel(mockedPanel);
        jobFilter.setClickHandlerRegistration(mockedClickHandlerRegistration);

        // Activate Subject Under Test
        jobFilter.removeJobFilter();

        // Verify test
        verify(mockedClickHandlerRegistration).removeHandler();
        getAttributes(jobFilter);
        assertThat(clickHandlerRegistration, is(nullValue()));
        verify(mockedPanel).remove(decoratorPanel);
    }

}
