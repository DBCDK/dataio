package dk.dbc.dataio.gui.client.components.jobfilter;

import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.resources.Resources;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
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
    public void uiHandlerSelectionChanged_callFilterItemsRadioButtonPressedTestCallback_changeCallbackCalled() {
        // Test Preparation
        SuppressSubmitterJobFilter jobFilter = new SuppressSubmitterJobFilter(mockedTexts, mockedResources);
        when(jobFilter.showAllSubmittersButton.getValue()).thenReturn(true);
        when(jobFilter.suppressSubmittersButton.getValue()).thenReturn(false);
        jobFilter.callbackChangeHandler = mockedChangeHandler;

        // Activate Subject Under Test
        jobFilter.filterItemsRadioButtonPressed(new TestClickEvent());

        // Verify Test
        verify(jobFilter.callbackChangeHandler).onChange(null);
    }

    @Test
    public void uiHandlerSelectionChanged_callFilterItemsRadioButtonPressedNoRadioButtonsSet_noJobListCriteriaSet() {
        // Test Preparation
        SuppressSubmitterJobFilter jobFilter = new SuppressSubmitterJobFilter(mockedTexts, mockedResources);
        when(jobFilter.showAllSubmittersButton.getValue()).thenReturn(false);
        when(jobFilter.suppressSubmittersButton.getValue()).thenReturn(false);

        // Activate Subject Under Test
        jobFilter.filterItemsRadioButtonPressed(new TestClickEvent());

        // Verify Test
// Verify that Criteria has been setup correctly - No Criteria set
// To be implemented when ListCriteria has been refactored
    }

    @Test
    public void uiHandlerSelectionChanged_callFilterItemsRadioButtonPressedAllRadioButtonsSet_allJobListCriteriaSet() {
        // Test Preparation
        SuppressSubmitterJobFilter jobFilter = new SuppressSubmitterJobFilter(mockedTexts, mockedResources);
        when(jobFilter.showAllSubmittersButton.getValue()).thenReturn(true);
        when(jobFilter.suppressSubmittersButton.getValue()).thenReturn(true);

        // Activate Subject Under Test
        jobFilter.filterItemsRadioButtonPressed(new TestClickEvent());

        // Verify Test
// Verify that Criteria has been setup correctly - Show All Submitters Criteria set
// To be implemented when ListCriteria has been refactored
    }

    @Test
    public void uiHandlerSelectionChanged_callFilterItemsRadioButtonPressedAllSubmittersRadioButtonsSet_allSubmittersJobListCriteriaSet() {
        // Test Preparation
        SuppressSubmitterJobFilter jobFilter = new SuppressSubmitterJobFilter(mockedTexts, mockedResources);
        when(jobFilter.showAllSubmittersButton.getValue()).thenReturn(true);
        when(jobFilter.suppressSubmittersButton.getValue()).thenReturn(false);

        // Activate Subject Under Test
        jobFilter.filterItemsRadioButtonPressed(new TestClickEvent());

        // Verify Test
// Verify that Criteria has been setup correctly - Show All Submitters Criteria set
// To be implemented when ListCriteria has been refactored
    }

    @Test
    public void uiHandlerSelectionChanged_callFilterItemsRadioButtonPressedSuppressSubmitterRadioButtonsSet_suppressSubmittersJobListCriteriaSet() {
        // Test Preparation
        SuppressSubmitterJobFilter jobFilter = new SuppressSubmitterJobFilter(mockedTexts, mockedResources);
        when(jobFilter.showAllSubmittersButton.getValue()).thenReturn(false);
        when(jobFilter.suppressSubmittersButton.getValue()).thenReturn(true);

        // Activate Subject Under Test
        jobFilter.filterItemsRadioButtonPressed(new TestClickEvent());

        // Verify Test
// Verify that Criteria has been setup correctly - Show All Submitters Criteria set
// To be implemented when ListCriteria has been refactored
    }

    @Test
    public void getName_callGetName_fetchesStoredName() {
        // Constants
        final String MOCKED_NAME = "name from mocked Texts";

        // Test Preparation
        SuppressSubmitterJobFilter jobFilter = new SuppressSubmitterJobFilter(mockedTexts, mockedResources);
        when(mockedTexts.suppressSubmitterFilter_name()).thenReturn(MOCKED_NAME);

        // Activate Subject Under Test
        String jobFilterName = jobFilter.getName();

        // Verify test
        assertThat(jobFilterName, is(MOCKED_NAME));
    }

    @Test
    public void addChangeHandler_callAddChangeHandler_changeHandlerAdded() {
        // Test Preparation
        SuppressSubmitterJobFilter jobFilter = new SuppressSubmitterJobFilter(mockedTexts, mockedResources);

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