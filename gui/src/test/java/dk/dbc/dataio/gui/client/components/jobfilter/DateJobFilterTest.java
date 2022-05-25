package dk.dbc.dataio.gui.client.components.jobfilter;

import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.resources.Resources;
import dk.dbc.dataio.gui.client.util.Format;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Date;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Date Job Filter unit tests
 * <p/>
 * The test methods of this class uses the following naming convention:
 * <p/>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class DateJobFilterTest {
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
        DateJobFilter jobFilter = new DateJobFilter(mockedTexts, mockedResources, "", true);

        // Verify test
        assertThat(jobFilter.texts, is(mockedTexts));
        assertThat(jobFilter.resources, is(mockedResources));
        verify(jobFilter.fromDate).setValue(any(String.class));
        verifyNoMoreInteractions(jobFilter.fromDate);
        verify(jobFilter.toDate).setValue("");
        verifyNoMoreInteractions(jobFilter.toDate);
    }


    @Test
    public void dateChanged_callDateChangedChangeHandlerIsNull_nop() {
        // Test Preparation
        DateJobFilter jobFilter = new DateJobFilter(mockedTexts, mockedResources, "", true);
        jobFilter.callbackChangeHandler = null;

        // Activate Subject Under Test
        jobFilter.dateChanged(null);  // Event is not used, so is set to null

        // Verify test
        // Nothing to verify - apart from assuring no exceptions is thrown
    }

    @Test
    public void dateChanged_callDateChangedChangeHandlerIsValid_ok() {
        // Test Preparation
        DateJobFilter jobFilter = new DateJobFilter(mockedTexts, mockedResources, "", true);
        jobFilter.callbackChangeHandler = mockedChangeHandler;

        // Activate Subject Under Test
        jobFilter.dateChanged(null);  // Event is not used, so is set to null

        // Verify test
        verify(mockedChangeHandler).onChange(null);
        verifyNoMoreInteractions(mockedChangeHandler);
    }


    @Test
    public void getName_callGetName_fetchesStoredName() {
        // Constants
        final String MOCKED_NAME = "name from mocked Texts";

        // Test Preparation
        DateJobFilter jobFilter = new DateJobFilter(mockedTexts, mockedResources, "", true);
        when(mockedTexts.jobDateFilter_name()).thenReturn(MOCKED_NAME);

        // Activate Subject Under Test
        String jobFilterName = jobFilter.getName();

        // Verify test
        assertThat(jobFilterName, is(MOCKED_NAME));
    }


    @Test
    public void getValue_fromDateEmptyAndToDateEmpty_emptyCriteria() {
        // Test Preparation
        DateJobFilter jobFilter = new DateJobFilter(mockedTexts, mockedResources, "", true);
        when(jobFilter.fromDate.getValue()).thenReturn("");
        when(jobFilter.toDate.getValue()).thenReturn("");

        // Activate Subject Under Test
        JobListCriteria criteria = jobFilter.getValue();

        // Verify test
        verify(jobFilter.fromDate, times(1)).getValue();
        verify(jobFilter.toDate, times(1)).getValue();
        assertThat(criteria, equalTo(new JobListCriteria()));
    }


    @Test
    public void getValue_fromDateNotEmptyAndToDateEmpty_fromCriteria() {
        final String FROM_DATE = "2015-10-21 10:00:00";

        // Test Preparation
        DateJobFilter jobFilter = new DateJobFilter(mockedTexts, mockedResources, "", true);
        when(jobFilter.fromDate.getValue()).thenReturn(FROM_DATE);
        when(jobFilter.toDate.getValue()).thenReturn("");

        // Activate Subject Under Test
        JobListCriteria criteria = jobFilter.getValue();

        // Verify test
        verify(jobFilter.fromDate, times(1)).getValue();
        verify(jobFilter.toDate, times(1)).getValue();
        JobListCriteria expectedCriteria = new JobListCriteria().where(
                new ListFilter<>(
                        JobListCriteria.Field.TIME_OF_CREATION,
                        ListFilter.Op.GREATER_THAN,
                        Format.parseLongDateAsDate(FROM_DATE)
                )
        );
        assertThat(criteria, equalTo(expectedCriteria));
    }

    @Test
    public void getValue_fromDateEmptyAndToDateNotEmpty_fromCriteria() {
        final String TO_DATE = "2015-10-21 20:00:00";

        // Test Preparation
        DateJobFilter jobFilter = new DateJobFilter(mockedTexts, mockedResources, "", true);
        when(jobFilter.fromDate.getValue()).thenReturn("");
        when(jobFilter.toDate.getValue()).thenReturn(TO_DATE);

        // Activate Subject Under Test
        JobListCriteria criteria = jobFilter.getValue();

        // Verify test
        verify(jobFilter.fromDate, times(1)).getValue();
        verify(jobFilter.toDate, times(1)).getValue();
        JobListCriteria expectedCriteria = new JobListCriteria().where(
                new ListFilter<>(
                        JobListCriteria.Field.TIME_OF_CREATION,
                        ListFilter.Op.LESS_THAN,
                        Format.parseLongDateAsDate(TO_DATE)
                )
        );
        assertThat(criteria, equalTo(expectedCriteria));
    }

    @Test
    public void getValue_fromDateNotEmptyAndToDateNotEmpty_fromCriteria() {
        final String FROM_DATE = "2015-10-21 10:00:00";
        final String TO_DATE = "2015-10-21 20:00:00";

        // Test Preparation
        DateJobFilter jobFilter = new DateJobFilter(mockedTexts, mockedResources, "", true);
        when(jobFilter.fromDate.getValue()).thenReturn(FROM_DATE);
        when(jobFilter.toDate.getValue()).thenReturn(TO_DATE);

        // Activate Subject Under Test
        JobListCriteria criteria = jobFilter.getValue();

        // Verify test
        verify(jobFilter.fromDate, times(1)).getValue();
        verify(jobFilter.toDate, times(1)).getValue();
        JobListCriteria expectedCriteria = new JobListCriteria().where(
                new ListFilter<>(
                        JobListCriteria.Field.TIME_OF_CREATION,
                        ListFilter.Op.GREATER_THAN,
                        Format.parseLongDateAsDate(FROM_DATE)
                )
        ).and(
                new ListFilter<>(
                        JobListCriteria.Field.TIME_OF_CREATION,
                        ListFilter.Op.LESS_THAN,
                        Format.parseLongDateAsDate(TO_DATE)
                )
        );
        assertThat(criteria, equalTo(expectedCriteria));
    }

    @Test
    public void setParameterData_emptyParameter_noDatesSet() {
        // Activate Subject Under Test
        DateJobFilter jobFilter = new DateJobFilter(mockedTexts, mockedResources, "", true);
        // setParameter is called upon initialization

        // Verify test
        verify(jobFilter.fromDate).setValue(calculateDate(2));  // Upon initialization
        verify(jobFilter.toDate).setValue("");  // Upon initialization
        verifyNoMoreInteractions(jobFilter.fromDate);
        verifyNoMoreInteractions(jobFilter.toDate);
    }

    @Test
    public void setParameterData_fromDateParameterAsDays_fromDateSet() {
        // Activate Subject Under Test
        DateJobFilter jobFilter = new DateJobFilter(mockedTexts, mockedResources, "20", true);
        // setParameter is called upon initialization

        // Verify test
        verify(jobFilter.fromDate).setValue(calculateDate(2));  // Upon initialization
        verify(jobFilter.toDate).setValue("");  // Upon initialization
        verify(jobFilter.fromDate).setValue(calculateDate(20), true);
        verifyNoMoreInteractions(jobFilter.fromDate);
        verifyNoMoreInteractions(jobFilter.toDate);
    }

    @Test
    public void setParameterData_fromDateParameterAsDate_fromDateSet() {
        // Activate Subject Under Test
        DateJobFilter jobFilter = new DateJobFilter(mockedTexts, mockedResources, "2016-10-16 22:11:00", true);
        // setParameter is called upon initialization

        // Verify test
        verify(jobFilter.fromDate).setValue(calculateDate(2));  // Upon initialization
        verify(jobFilter.toDate).setValue("");  // Upon initialization
        verify(jobFilter.fromDate).setValue("2016-10-16 22:11:00", true);
        verifyNoMoreInteractions(jobFilter.fromDate);
        verifyNoMoreInteractions(jobFilter.toDate);
    }

    @Test
    public void setParameterData_twoDatesParameterAsDays_twoDatesSet() {
        // Activate Subject Under Test
        DateJobFilter jobFilter = new DateJobFilter(mockedTexts, mockedResources, "32,1", true);
        // setParameter is called upon initialization

        // Verify test
        verify(jobFilter.fromDate).setValue(calculateDate(2));  // Upon initialization
        verify(jobFilter.toDate).setValue("");  // Upon initialization
        verify(jobFilter.fromDate).setValue(calculateDate(32), true);
        verify(jobFilter.toDate).setValue(calculateDate(1), true);
        verifyNoMoreInteractions(jobFilter.fromDate);
        verifyNoMoreInteractions(jobFilter.toDate);
    }

    @Test
    public void setParameterData_twoDatesParameterAsDates_twoDatesSet() {
        // Activate Subject Under Test
        DateJobFilter jobFilter = new DateJobFilter(mockedTexts, mockedResources, "2016-10-16 22:11:00,2016-10-16 22:11:01", true);
        // setParameter is called upon initialization

        // Verify test
        verify(jobFilter.fromDate).setValue(calculateDate(2));  // Upon initialization
        verify(jobFilter.toDate).setValue("");  // Upon initialization
        verify(jobFilter.fromDate).setValue("2016-10-16 22:11:00", true);
        verify(jobFilter.toDate).setValue("2016-10-16 22:11:01", true);
        verifyNoMoreInteractions(jobFilter.fromDate);
        verifyNoMoreInteractions(jobFilter.toDate);
    }

    @Test
    public void setParameterData_twoDatesParameterAsDateAndDays_twoDatesSet() {
        // Activate Subject Under Test
        DateJobFilter jobFilter = new DateJobFilter(mockedTexts, mockedResources, "2016-10-16 22:11:00,123", true);
        // setParameter is called upon initialization

        // Verify test
        verify(jobFilter.fromDate).setValue(calculateDate(2));  // Upon initialization
        verify(jobFilter.toDate).setValue("");  // Upon initialization
        verify(jobFilter.fromDate).setValue("2016-10-16 22:11:00", true);
        verify(jobFilter.toDate).setValue(calculateDate(123), true);
        verifyNoMoreInteractions(jobFilter.fromDate);
        verifyNoMoreInteractions(jobFilter.toDate);
    }

    @Test
    public void setParameterData_toDateParameterAsDays_toDateSet() {
        // Activate Subject Under Test
        DateJobFilter jobFilter = new DateJobFilter(mockedTexts, mockedResources, ",345", true);
        // setParameter is called upon initialization

        // Verify test
        verify(jobFilter.fromDate).setValue(calculateDate(2));  // Upon initialization
        verify(jobFilter.toDate).setValue("");  // Upon initialization
        verify(jobFilter.fromDate).setValue("", true);
        verify(jobFilter.toDate).setValue(calculateDate(345), true);
        verifyNoMoreInteractions(jobFilter.fromDate);
        verifyNoMoreInteractions(jobFilter.toDate);
    }

    @Test
    public void setParameterData_toDateParameterAsDate_toDateSet() {
        // Activate Subject Under Test
        DateJobFilter jobFilter = new DateJobFilter(mockedTexts, mockedResources, ",2016-10-16 22:11:02", true);
        // setParameter is called upon initialization

        // Verify test
        verify(jobFilter.fromDate).setValue(calculateDate(2));  // Upon initialization
        verify(jobFilter.toDate).setValue("");  // Upon initialization
        verify(jobFilter.fromDate).setValue("", true);
        verify(jobFilter.toDate).setValue("2016-10-16 22:11:02", true);
        verifyNoMoreInteractions(jobFilter.fromDate);
        verifyNoMoreInteractions(jobFilter.toDate);
    }

    @Test
    public void getParameter_emptyValue_correctValueFetched() {
        // Test Preparation
        DateJobFilter jobFilter = new DateJobFilter(mockedTexts, mockedResources, "", true);
        when(jobFilter.fromDate.getValue()).thenReturn("");
        when(jobFilter.toDate.getValue()).thenReturn("");

        // Activate Subject Under Test
        String result = jobFilter.getParameter();

        // Verify test
        assertThat(result, is(""));
    }

    @Test
    public void getParameter_validFromEmptyTo_correctValueFetched() {
        // Test Preparation
        DateJobFilter jobFilter = new DateJobFilter(mockedTexts, mockedResources, "", true);
        when(jobFilter.fromDate.getValue()).thenReturn("A");
        when(jobFilter.toDate.getValue()).thenReturn("");

        // Activate Subject Under Test
        String result = jobFilter.getParameter();

        // Verify test
        assertThat(result, is("A"));
    }

    @Test
    public void getParameter_emptyFromValidTo_correctValueFetched() {
        // Test Preparation
        DateJobFilter jobFilter = new DateJobFilter(mockedTexts, mockedResources, "", true);
        when(jobFilter.fromDate.getValue()).thenReturn("");
        when(jobFilter.toDate.getValue()).thenReturn("B");

        // Activate Subject Under Test
        String result = jobFilter.getParameter();

        // Verify test
        assertThat(result, is(",B"));
    }

    @Test
    public void getParameter_validFromValidTo_correctValueFetched() {
        // Test Preparation
        DateJobFilter jobFilter = new DateJobFilter(mockedTexts, mockedResources, "", true);
        when(jobFilter.fromDate.getValue()).thenReturn("A");
        when(jobFilter.toDate.getValue()).thenReturn("B");

        // Activate Subject Under Test
        String result = jobFilter.getParameter();

        // Verify test
        assertThat(result, is("A,B"));
    }

    @Test
    public void addValueChangeHandler_callAddValueChangeHandler_valueChangeHandlerAdded() {
        // Test Preparation
        DateJobFilter jobFilter = new DateJobFilter(mockedTexts, mockedResources, "", true);

        // Activate Subject Under Test
        HandlerRegistration handlerRegistration = jobFilter.addChangeHandler(mockedChangeHandler);

        // Verify test
        assertThat(jobFilter.callbackChangeHandler, is(mockedChangeHandler));
        verify(mockedChangeHandler).onChange(null);
        assertThat(handlerRegistration, not(nullValue()));

        // Call returned HandlerRegistration object and test result
        handlerRegistration.removeHandler();
        assertThat(jobFilter.callbackChangeHandler, is(nullValue()));
    }


    /*
     * Private methods
     */
    private String calculateDate(int days) {
        final Integer ONE_DAY_IN_MILLISECONDS = 24 * 60 * 60 * 1000;
        final String DEFAULT_EMPTY_TIME = "00:00:00";
        String date = Format.formatLongDate(new Date(System.currentTimeMillis() - days * ONE_DAY_IN_MILLISECONDS));
        return date.substring(0, date.length() - DEFAULT_EMPTY_TIME.length()) + DEFAULT_EMPTY_TIME;
    }

}
