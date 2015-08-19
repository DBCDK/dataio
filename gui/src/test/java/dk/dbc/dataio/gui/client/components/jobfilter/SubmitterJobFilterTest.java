package dk.dbc.dataio.gui.client.components.jobfilter;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.model.JobListCriteriaModel;
import dk.dbc.dataio.gui.client.resources.Resources;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * Created by ja7 on 19-08-15.
 * Test for SubmitterJobFilter
 */
@RunWith(GwtMockitoTestRunner.class)
public class SubmitterJobFilterTest {
    @Mock Texts mockedTexts;
    @Mock Resources mockedResources;
    @Mock ValueChangeEvent<String> mockedValueChangeEvent;
    @Mock ValueChangeHandler<JobListCriteriaModel> mockedValueChangeHandler;
    @Mock ValueChangeHandler<String> mockedSinkJobFilterValueChangeHandler;
    @Mock HandlerRegistration mockedSinkListHandlerRegistration;


    @Test
    public void getName_callGetName_fetchesStoredName() {
        // Constants
        final String MOCKED_NAME = "name from mocked Texts";

        // Test Preparation
        SubmitterJobFilter jobFilter = new SubmitterJobFilter(mockedTexts, mockedResources);
        when(mockedTexts.submitterFilter_name()).thenReturn(MOCKED_NAME);

        // Activate Subject Under Test
        String jobFilterName = jobFilter.getName();

        // Verify test
        assertThat(jobFilterName, is(MOCKED_NAME));
    }


    @Test
      public void addValueChangeHandler_callAddValueChangeHandler_valueChangeHandlerAdded() {
          // Test Preparation
          SubmitterJobFilter jobFilter = new SubmitterJobFilter(mockedTexts, mockedResources );
          when(jobFilter.submitter.addValueChangeHandler(any(ValueChangeHandler.class))).thenReturn(mockedSinkListHandlerRegistration);

          // Activate Subject Under Test
          HandlerRegistration handlerRegistration = jobFilter.addValueChangeHandler(mockedValueChangeHandler);

          // Verify test
          assertThat(jobFilter.submitterJobValueChangeHandler, is(mockedValueChangeHandler));
          assertThat(jobFilter.submitterHandlerRegistration, is(mockedSinkListHandlerRegistration));
          assertThat(handlerRegistration, not(nullValue()));
      }


}