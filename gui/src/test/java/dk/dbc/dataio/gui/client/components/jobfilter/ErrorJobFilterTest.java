package dk.dbc.dataio.gui.client.components.jobfilter;

import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.resources.Resources;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
    @Mock
    private Texts mockedTexts;
    @Mock
    private Resources mockedResources;
    @Mock
    private ChangeHandler mockedChangeHandler;


    //
    // Tests starts here...
    //
    @Test
    public void constructor_instantiate_instantiatedCorrectly() {
        // Activate Subject Under Test
        ErrorJobFilter jobFilter = new ErrorJobFilter(mockedTexts, mockedResources, "", true);

        // Verify test
        assertThat(jobFilter.texts, is(mockedTexts));
        assertThat(jobFilter.resources, is(mockedResources));
    }


    @Test
    public void getName_callGetName_fetchesStoredName() {
        // Constants
        final String MOCKED_NAME = "name from mocked Texts";

        // Test Preparation
        ErrorJobFilter jobFilter = new ErrorJobFilter(mockedTexts, mockedResources, "", true);
        when(mockedTexts.errorFilter_name()).thenReturn(MOCKED_NAME);

        // Activate Subject Under Test
        String jobFilterName = jobFilter.getName();

        // Verify test
        assertThat(jobFilterName, is(MOCKED_NAME));
    }

    @Test
    public void getValue_defaultValue_returnEmptyJobListCriteria() {
        // Test Preparation
        ErrorJobFilter jobFilter = new ErrorJobFilter(mockedTexts, mockedResources, "", true);

        // Activate Subject Under Test
        JobListCriteria criteria = jobFilter.getValue();

        // Verify test
        assertThat(criteria, is(new JobListCriteria()));
    }

    @Test
    public void getValue_processingValue_returnProcessingJobListCriteria() {
        // Test Preparation
        ErrorJobFilter jobFilter = new ErrorJobFilter(mockedTexts, mockedResources, "", true);
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
        ErrorJobFilter jobFilter = new ErrorJobFilter(mockedTexts, mockedResources, "", true);
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
        ErrorJobFilter jobFilter = new ErrorJobFilter(mockedTexts, mockedResources, "", true);
        when(jobFilter.jobCreationCheckBox.getValue()).thenReturn(true);

        // Activate Subject Under Test
        JobListCriteria criteria = jobFilter.getValue();

        // Verify test
        assertThat(criteria, is(new JobListCriteria().
                where(new ListFilter<>(JobListCriteria.Field.JOB_CREATION_FAILED))
        ));
    }

    @Test
    public void getValue_combinedValue_returnCombinedJobListCriteria() {
        // Test Preparation
        ErrorJobFilter jobFilter = new ErrorJobFilter(mockedTexts, mockedResources, "", true);
        when(jobFilter.processingCheckBox.getValue()).thenReturn(true);
        when(jobFilter.jobCreationCheckBox.getValue()).thenReturn(true);

        // Activate Subject Under Test
        JobListCriteria criteria = jobFilter.getValue();

        // Verify test
        assertThat(criteria, is(new JobListCriteria().
                where(new ListFilter<>(JobListCriteria.Field.STATE_PROCESSING_FAILED)).
                or(new ListFilter<>(JobListCriteria.Field.JOB_CREATION_FAILED))
        ));
    }

    @Test
    public void setParameterData_emptyParameter_noErrorsSet() {
        // Activate Subject Under Test
        ErrorJobFilter jobFilter = new ErrorJobFilter(mockedTexts, mockedResources, "", true);
        // setParameter is called upon initialization

        // Verify test
        verifyNoMoreInteractions(jobFilter.processingCheckBox);
        verifyNoMoreInteractions(jobFilter.deliveringCheckBox);
        verifyNoMoreInteractions(jobFilter.jobCreationCheckBox);
    }

    @Test
    public void setParameterData_processingParameter_processingSet() {
        // Activate Subject Under Test
        ErrorJobFilter jobFilter = new ErrorJobFilter(mockedTexts, mockedResources, "processing", true);
        // setParameter is called upon initialization

        // Verify test
        verify(jobFilter.processingCheckBox).setValue(false);  // Set upon initialization
        verify(jobFilter.deliveringCheckBox).setValue(false);  // Set upon initialization
        verify(jobFilter.jobCreationCheckBox).setValue(false);  // Set upon initialization
        verify(jobFilter.processingCheckBox).setValue(true);
        verifyNoMoreInteractions(jobFilter.processingCheckBox);
        verifyNoMoreInteractions(jobFilter.deliveringCheckBox);
        verifyNoMoreInteractions(jobFilter.jobCreationCheckBox);
    }

    @Test
    public void setParameterData_processingAndJobCreationParameter_processingAndJobCreationSet() {
        // Activate Subject Under Test
        ErrorJobFilter jobFilter = new ErrorJobFilter(mockedTexts, mockedResources, "processing,jobcreation", true);
        // setParameter is called upon initialization

        // Verify test
        verify(jobFilter.processingCheckBox).setValue(false);  // Set upon initialization
        verify(jobFilter.deliveringCheckBox).setValue(false);  // Set upon initialization
        verify(jobFilter.jobCreationCheckBox).setValue(false);  // Set upon initialization
        verify(jobFilter.processingCheckBox).setValue(true);
        verify(jobFilter.jobCreationCheckBox).setValue(true);
        verifyNoMoreInteractions(jobFilter.processingCheckBox);
        verifyNoMoreInteractions(jobFilter.deliveringCheckBox);
        verifyNoMoreInteractions(jobFilter.jobCreationCheckBox);
    }

    @Test
    public void setParameterData_processingAllParameterOrderMixed_processingAllSet() {
        // Activate Subject Under Test
        ErrorJobFilter jobFilter = new ErrorJobFilter(mockedTexts, mockedResources, "delivering,processing,jobcreation", true);
        // setParameter is called upon initialization

        // Verify test
        verify(jobFilter.processingCheckBox).setValue(false);  // Set upon initialization
        verify(jobFilter.deliveringCheckBox).setValue(false);  // Set upon initialization
        verify(jobFilter.jobCreationCheckBox).setValue(false);  // Set upon initialization
        verify(jobFilter.processingCheckBox).setValue(true);
        verify(jobFilter.deliveringCheckBox).setValue(true);
        verify(jobFilter.jobCreationCheckBox).setValue(true);
        verifyNoMoreInteractions(jobFilter.processingCheckBox);
        verifyNoMoreInteractions(jobFilter.deliveringCheckBox);
        verifyNoMoreInteractions(jobFilter.jobCreationCheckBox);
    }

    @Test
    public void setParameterData_misSpelledParameter_onlyCorrectlySpelledSet() {
        // Activate Subject Under Test
        ErrorJobFilter jobFilter = new ErrorJobFilter(mockedTexts, mockedResources, "deliveringggggggg,processing", true);
        // setParameter is called upon initialization

        // Verify test
        verify(jobFilter.processingCheckBox).setValue(false);  // Set upon initialization
        verify(jobFilter.deliveringCheckBox).setValue(false);  // Set upon initialization
        verify(jobFilter.jobCreationCheckBox).setValue(false);  // Set upon initialization
        verify(jobFilter.processingCheckBox).setValue(true);
        verifyNoMoreInteractions(jobFilter.processingCheckBox);
        verifyNoMoreInteractions(jobFilter.deliveringCheckBox);
        verifyNoMoreInteractions(jobFilter.jobCreationCheckBox);
    }

    @Test
    public void setParameterData_capitalLetterParameter_accepted() {
        // Activate Subject Under Test
        ErrorJobFilter jobFilter = new ErrorJobFilter(mockedTexts, mockedResources, "DELiveriNg", true);
        // setParameter is called upon initialization

        // Verify test
        verify(jobFilter.processingCheckBox).setValue(false);  // Set upon initialization
        verify(jobFilter.deliveringCheckBox).setValue(false);  // Set upon initialization
        verify(jobFilter.jobCreationCheckBox).setValue(false);  // Set upon initialization
        verify(jobFilter.deliveringCheckBox).setValue(true);
        verifyNoMoreInteractions(jobFilter.processingCheckBox);
        verifyNoMoreInteractions(jobFilter.deliveringCheckBox);
        verifyNoMoreInteractions(jobFilter.jobCreationCheckBox);
    }

    @Test
    public void getParameter_allUnset_correctValueFetched() {
        // Test Preparation
        ErrorJobFilter jobFilter = new ErrorJobFilter(mockedTexts, mockedResources, "", true);
        when(jobFilter.processingCheckBox.getValue()).thenReturn(false);
        when(jobFilter.deliveringCheckBox.getValue()).thenReturn(false);
        when(jobFilter.jobCreationCheckBox.getValue()).thenReturn(false);

        // Activate Subject Under Test
        String result = jobFilter.getParameter();

        // Verify test
        assertThat(result, is(""));
    }

    @Test
    public void getParameter_processingSet_correctValueFetched() {
        // Test Preparation
        ErrorJobFilter jobFilter = new ErrorJobFilter(mockedTexts, mockedResources, "", true);
        when(jobFilter.processingCheckBox.getValue()).thenReturn(true);
        when(jobFilter.deliveringCheckBox.getValue()).thenReturn(false);
        when(jobFilter.jobCreationCheckBox.getValue()).thenReturn(false);

        // Activate Subject Under Test
        String result = jobFilter.getParameter();

        // Verify test
        assertThat(result, is("processing"));
    }

    @Test
    public void getParameter_deliveringSet_correctValueFetched() {
        // Test Preparation
        ErrorJobFilter jobFilter = new ErrorJobFilter(mockedTexts, mockedResources, "", true);
        when(jobFilter.processingCheckBox.getValue()).thenReturn(false);
        when(jobFilter.deliveringCheckBox.getValue()).thenReturn(true);
        when(jobFilter.jobCreationCheckBox.getValue()).thenReturn(false);

        // Activate Subject Under Test
        String result = jobFilter.getParameter();

        // Verify test
        assertThat(result, is("delivering"));
    }

    @Test
    public void getParameter_jobcreationSet_correctValueFetched() {
        // Test Preparation
        ErrorJobFilter jobFilter = new ErrorJobFilter(mockedTexts, mockedResources, "", true);
        when(jobFilter.processingCheckBox.getValue()).thenReturn(false);
        when(jobFilter.deliveringCheckBox.getValue()).thenReturn(false);
        when(jobFilter.jobCreationCheckBox.getValue()).thenReturn(true);

        // Activate Subject Under Test
        String result = jobFilter.getParameter();

        // Verify test
        assertThat(result, is("jobcreation"));
    }

    @Test
    public void getParameter_allSet_correctValueFetched() {
        // Test Preparation
        ErrorJobFilter jobFilter = new ErrorJobFilter(mockedTexts, mockedResources, "", true);
        when(jobFilter.processingCheckBox.getValue()).thenReturn(true);
        when(jobFilter.deliveringCheckBox.getValue()).thenReturn(true);
        when(jobFilter.jobCreationCheckBox.getValue()).thenReturn(true);

        // Activate Subject Under Test
        String result = jobFilter.getParameter();

        // Verify test
        assertThat(result, is("processing,delivering,jobcreation"));
    }

    @Test
    public void addChangeHandler_callAddChangeHandler_changeHandlerAdded() {
        // Test Preparation
        ErrorJobFilter jobFilter = new ErrorJobFilter(mockedTexts, mockedResources, "", true);

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
        ErrorJobFilter jobFilter = new ErrorJobFilter(mockedTexts, mockedResources, "", true);

        // Activate Subject Under Test
        HandlerRegistration handlerRegistration = jobFilter.addChangeHandler(mockedChangeHandler);
        handlerRegistration.removeHandler();

        // Verify test
        assertThat(jobFilter.callbackChangeHandler, nullValue());
    }

    @Test
    public void changeHandlerCallback_default_callback() {
        // Test Preparation
        ErrorJobFilter jobFilter = new ErrorJobFilter(mockedTexts, mockedResources, "", true);
        jobFilter.addChangeHandler(mockedChangeHandler);

        // Activate Subject Under Test
        jobFilter.checkboxValueChanged(null);

        // Verify test
        verify(mockedChangeHandler, times(2)).onChange(null);
    }

}
