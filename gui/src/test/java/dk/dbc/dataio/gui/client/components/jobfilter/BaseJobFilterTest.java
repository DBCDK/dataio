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
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwtmockito.GwtMockitoTestRunner;
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
    private HandlerRegistration clickHandlerRegistration;
    private boolean addJobFilterMethodCalled = false;

    @Mock ImageResource mockedImageResource;
    @Mock JobFilter mockedJobFilter;
    @Mock JobFilterPanel mockedJobFilterPanel;
    @Mock HandlerRegistration mockedClickHandlerRegistration;


    class ConcreteBaseJobFilter extends BaseJobFilter {
        String storedName;
        ConcreteBaseJobFilter(String name) {
            super(mock(Texts.class), mock(Resources.class));
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
            return "";
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
    }

    class BaseJobFilterWithOverriddenAddMethod extends ConcreteBaseJobFilter {
        BaseJobFilterWithOverriddenAddMethod(String name) {
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
        thisAsWidget = jobFilter.getThisAsWidget();
        parentJobFilter = jobFilter.getParentJobFilter();
        filterPanel = jobFilter.getFilterPanel();
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
    public void constructor_instantiate_instantiatedCorrectly() {
        // Activate Subject Under Test
        ConcreteBaseJobFilter jobFilter = new ConcreteBaseJobFilter("-test name-");

        // Verify test
        getAttributes(jobFilter);
        assertThat(name, is("-test name-"));
        assertThat(thisAsWidget, is(notNullValue()));
        assertThat(parentJobFilter, is(nullValue()));
        assertThat(filterPanel, is(nullValue()));
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
        Scheduler.ScheduledCommand command = jobFilter.getAddCommand(mockedJobFilter);

        // Verify test
        assertThat(command, is(notNullValue()));
        assertThat(addJobFilterMethodCalled, is(false));
        getAttributes(jobFilter);
        assertThat(parentJobFilter, is(mockedJobFilter));

        // Activate Command
        command.execute();

        // Verify Test
        assertThat(addJobFilterMethodCalled, is(true));
        getAttributes(jobFilter);
        assertThat(parentJobFilter, is(mockedJobFilter));
    }

    @Test
    public void addJobFilter_jobFilterPanelIsNotNull_noAction() {
        // Test Preparation
        ConcreteBaseJobFilter jobFilter = new ConcreteBaseJobFilter("-test name-");
        jobFilter.setFilterPanel(mockedJobFilterPanel);

        // Activate Subject Under Test
        jobFilter.addJobFilter();

        // Verify test
        getAttributes(jobFilter);
        assertThat(filterPanel, is(mockedJobFilterPanel));  // No action at all...
    }

    @Test
    public void addJobFilter_jobFilterPanelIsNull_okAction() {
        // Test Preparation
        ConcreteBaseJobFilter jobFilter = new ConcreteBaseJobFilter("-test name-");
        jobFilter.setFilterPanel(null);
        jobFilter.getAddCommand(mockedJobFilter);

        // Activate Subject Under Test
        jobFilter.addJobFilter();

        // Verify test
        getAttributes(jobFilter);
        assertThat(filterPanel, is(notNullValue()));
        assertThat(clickHandlerRegistration, is(notNullValue()));
        verify(parentJobFilter).add(jobFilter);
    }

    @Test
    public void removeJobFilter_jobFilterPanelIsNull_noAction() {
        // Test Preparation
        ConcreteBaseJobFilter jobFilter = new ConcreteBaseJobFilter("-test name-");
        jobFilter.setFilterPanel(null);
        jobFilter.setClickHandlerRegistration(mockedClickHandlerRegistration);
        jobFilter.setParentJobFilter(mockedJobFilter);

        // Activate Subject Under Test
        jobFilter.removeJobFilter();

        // Verify test
        verifyNoMoreInteractions(mockedClickHandlerRegistration);
        verifyNoMoreInteractions(mockedJobFilterPanel);
        verifyNoMoreInteractions(mockedJobFilter);
    }

    @Test
    public void removeJobFilter_jobFilterPanelIsNotNullParentPanelIsNull_okAction() {
        // Test Preparation
        ConcreteBaseJobFilter jobFilter = new ConcreteBaseJobFilter("-test name-");
        jobFilter.setFilterPanel(mockedJobFilterPanel);
        jobFilter.setClickHandlerRegistration(mockedClickHandlerRegistration);
        jobFilter.setParentJobFilter(mockedJobFilter);

        // Activate Subject Under Test
        jobFilter.removeJobFilter();

        // Verify test
        getAttributes(jobFilter);
        verify(mockedClickHandlerRegistration).removeHandler();
        verify(mockedJobFilterPanel).clear();
        verify(mockedJobFilter).remove(jobFilter);
        assertThat(clickHandlerRegistration, is(nullValue()));
    }

}
