package dk.dbc.dataio.gui.client.components.jobfilter;

import com.google.gwt.event.dom.client.ChangeHandler;
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Test for SubmitterJobFilter
 */

@RunWith(GwtMockitoTestRunner.class)
public class SubmitterJobFilterTest {
    @Mock
    private Texts mockedTexts;
    @Mock
    private Resources mockedResources;
    @Mock
    private ChangeHandler mockedChangeHandler;
    @Mock
    private HandlerRegistration mockedSubmitterHandlerRegistration;


    @Test
    public void getName_callGetName_fetchesStoredName() {
        // Constants
        final String MOCKED_NAME = "name from mocked Texts";

        // Test Preparation
        SubmitterJobFilter jobFilter = new SubmitterJobFilter(mockedTexts, mockedResources, "", true);
        when(mockedTexts.submitterFilter_name()).thenReturn(MOCKED_NAME);

        // Activate Subject Under Test
        String jobFilterName = jobFilter.getName();

        // Verify test
        assertThat(jobFilterName, is(MOCKED_NAME));
    }

    @Test
    public void getValue_nullValue_noValueSet() {
        // Test Preparation
        SubmitterJobFilter jobFilter = new SubmitterJobFilter(mockedTexts, mockedResources, "", true);
        when(jobFilter.submitter.getValue()).thenReturn(null);

        // Activate Subject Under Test
        JobListCriteria criteria = jobFilter.getValue();

        // Verify test
        verify(jobFilter.submitter).getValue();
        verifyNoMoreInteractions(jobFilter.submitter);
        assertThat(criteria, is(new JobListCriteria()));
    }

    @Test
    public void getValue_emptyValue_noValueSet() {
        // Test Preparation
        SubmitterJobFilter jobFilter = new SubmitterJobFilter(mockedTexts, mockedResources, "", true);
        when(jobFilter.submitter.getValue()).thenReturn("");

        // Activate Subject Under Test
        JobListCriteria criteria = jobFilter.getValue();

        // Verify test
        verify(jobFilter.submitter).getValue();
        verifyNoMoreInteractions(jobFilter.submitter);
        assertThat(criteria, is(new JobListCriteria()));
    }

    @Test
    public void getValue_zeroValue_noValueSet() {
        // Test Preparation
        SubmitterJobFilter jobFilter = new SubmitterJobFilter(mockedTexts, mockedResources, "", true);
        when(jobFilter.submitter.getValue()).thenReturn("0");

        // Activate Subject Under Test
        JobListCriteria criteria = jobFilter.getValue();

        // Verify test
        verify(jobFilter.submitter).getValue();
        verifyNoMoreInteractions(jobFilter.submitter);
        assertThat(criteria, is(new JobListCriteria().where(new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, "{ \"submitterId\": 0}"))));
    }

    @Test
    public void getValue_validValue_valueSet() {
        // Test Preparation
        SubmitterJobFilter jobFilter = new SubmitterJobFilter(mockedTexts, mockedResources, "", true);
        when(jobFilter.submitter.getValue()).thenReturn("7654");

        // Activate Subject Under Test
        JobListCriteria criteria = jobFilter.getValue();

        // Verify test
        verify(jobFilter.submitter).getValue();
        verifyNoMoreInteractions(jobFilter.submitter);
        assertThat(criteria, is(new JobListCriteria().where(new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, "{ \"submitterId\": 7654}"))));
    }

    @Test
    public void getValue_validMultiValue_multiValueSet() {
        // Test Preparation
        SubmitterJobFilter jobFilter = new SubmitterJobFilter(mockedTexts, mockedResources, "", true);
        when(jobFilter.submitter.getValue()).thenReturn("7654,321            ,              \t \n 54321");

        // Activate Subject Under Test
        JobListCriteria criteria = jobFilter.getValue();

        // Verify test
        verify(jobFilter.submitter).getValue();
        verifyNoMoreInteractions(jobFilter.submitter);
        assertThat(criteria, is(
                new JobListCriteria()
                        .where(new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, "{ \"submitterId\": 7654}"))
                        .or(new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, "{ \"submitterId\": 321}"))
                        .or(new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, "{ \"submitterId\": 54321}"))
        ));
    }

    @Test
    public void setParameterData_emptyValue_noSubmitterSet() {
        // Activate Subject Under Test
        SubmitterJobFilter jobFilter = new SubmitterJobFilter(mockedTexts, mockedResources, "", true);

        // Verify test
        verifyNoMoreInteractions(jobFilter.submitter);
    }

    @Test
    public void setParameterData_zeroValue_zeroSubmitterSet() {
        // Activate Subject Under Test
        SubmitterJobFilter jobFilter = new SubmitterJobFilter(mockedTexts, mockedResources, "0", true);

        // Verify test
        verify(jobFilter.submitter).setValue("0", true);
        verifyNoMoreInteractions(jobFilter.submitter);
    }

    @Test
    public void setParameterData_nonZeroValue_nonZeroSubmitterSet() {
        // Activate Subject Under Test
        SubmitterJobFilter jobFilter = new SubmitterJobFilter(mockedTexts, mockedResources, "8888", true);

        // Verify test
        verify(jobFilter.submitter).setValue("8888", true);
        verifyNoMoreInteractions(jobFilter.submitter);
    }

    @Test
    public void getParameterData_validValue_correctValueFetched() {
        // Test Preparation
        SubmitterJobFilter jobFilter = new SubmitterJobFilter(mockedTexts, mockedResources, "8888", true);
        when(jobFilter.submitter.getValue()).thenReturn("4321");

        // Activate Subject Under Test
        String result = jobFilter.getParameter();

        // Verify test
        assertThat(result, is("4321"));
    }

    @Test
    public void setFocus_trueValue_focusEnabled() {
        // Test Preparation
        SubmitterJobFilter jobFilter = new SubmitterJobFilter(mockedTexts, mockedResources, "", true);

        // Activate Subject Under Test
        jobFilter.setFocus(true);

        // Verify test
        verify(jobFilter.submitter).setFocus(true);
        verifyNoMoreInteractions(jobFilter.submitter);
    }

    @Test
    public void setFocus_falseValue_focusDisabled() {
        // Test Preparation
        SubmitterJobFilter jobFilter = new SubmitterJobFilter(mockedTexts, mockedResources, "", true);

        // Activate Subject Under Test
        jobFilter.setFocus(false);

        // Verify test
        verify(jobFilter.submitter).setFocus(false);
        verifyNoMoreInteractions(jobFilter.submitter);
    }

    @Test
    public void addValueChangeHandler_callAddValueChangeHandler_valueChangeHandlerAdded() {
        // Test Preparation
        SubmitterJobFilter jobFilter = new SubmitterJobFilter(mockedTexts, mockedResources, "", true);
        when(jobFilter.submitter.addValueChangeHandler(any(ValueChangeHandler.class))).thenReturn(mockedSubmitterHandlerRegistration);

        // Activate Subject Under Test
        HandlerRegistration handlerRegistration = jobFilter.addChangeHandler(mockedChangeHandler);

        // Verify test
        verify(jobFilter.submitter).addChangeHandler(mockedChangeHandler);
        assertThat(handlerRegistration, not(nullValue()));
    }


}
