package dk.dbc.dataio.gui.client.components.jobfilter;

import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.resources.Resources;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
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
public class SinkJobFilterTest {
    @Mock Texts mockedTexts;
    @Mock Resources mockedResources;
    @Mock FlowStoreProxyAsync mockedFlowStoreProxy;
    @Mock ValueChangeEvent<String> mockedValueChangeEvent;
    @Mock ChangeHandler mockedChangeHandler;
    @Mock ValueChangeHandler<String> mockedSinkJobFilterValueChangeHandler;
    @Mock HandlerRegistration mockedSinkListHandlerRegistration;


    //
    // Tests starts here...
    //
    @Test
    public void constructor_instantiate_instantiatedCorrectly() {
        // Activate Subject Under Test
        SinkJobFilter jobFilter = new SinkJobFilter(mockedTexts, mockedResources, mockedFlowStoreProxy);

        // Verify test
        assertThat(jobFilter.texts, is(mockedTexts));
        assertThat(jobFilter.resources, is(mockedResources));
        assertThat(jobFilter.flowStoreProxy, is(mockedFlowStoreProxy));
        verify(mockedFlowStoreProxy).findAllSinks(any(SinkJobFilter.FetchSinksCallback.class));
    }

    @Test
    public void changeFilterSelection_callFilterSelectionChanged_setSinkIdInModel() {
        // Constants
        final String SELECTED_KEY = "Elephant";

        // Test Preparation
        SinkJobFilter jobFilter = new SinkJobFilter(mockedTexts, mockedResources, mockedFlowStoreProxy);
        when(jobFilter.sinkList.getSelectedKey()).thenReturn(SELECTED_KEY);

        // Activate Subject Under Test
        jobFilter.filterSelectionChanged(mockedValueChangeEvent);

        // Verify test
        assertThat(jobFilter.jobListCriteriaModel.getSinkId(), is(SELECTED_KEY));
    }

    @Test
    public void getName_callGetName_fetchesStoredName() {
        // Constants
        final String MOCKED_NAME = "name from mocked Texts";

        // Test Preparation
        SinkJobFilter jobFilter = new SinkJobFilter(mockedTexts, mockedResources, mockedFlowStoreProxy);
        when(mockedTexts.sinkFilter_name()).thenReturn(MOCKED_NAME);

        // Activate Subject Under Test
        String jobFilterName = jobFilter.getName();

        // Verify test
        assertThat(jobFilterName, is(MOCKED_NAME));
    }

    @Test
    public void addValueChangeHandler_callAddValueChangeHandler_valueChangeHandlerAdded() {
        // Test Preparation
        SinkJobFilter jobFilter = new SinkJobFilter(mockedTexts, mockedResources, mockedFlowStoreProxy);
        when(jobFilter.sinkList.addChangeHandler(any(ChangeHandler.class))).thenReturn(mockedSinkListHandlerRegistration);

        // Activate Subject Under Test
        HandlerRegistration handlerRegistration = jobFilter.addChangeHandler(mockedChangeHandler);

        // Verify test
        assertThat(jobFilter.sinkJobValueChangeHandler, is(mockedChangeHandler));
        verify(jobFilter.sinkList).addChangeHandler(mockedChangeHandler);
        assertThat(handlerRegistration, not(nullValue()));
        assertThat(handlerRegistration, is(mockedSinkListHandlerRegistration));
    }

    class ConcreteSinkJobFilter extends SinkJobFilter {
        public ConcreteSinkJobFilter(Texts texts, Resources resources, FlowStoreProxyAsync flowStoreProxy) {
            super(texts, resources, flowStoreProxy);
        }
        public FetchSinksCallback fetchSinksCallback = new FetchSinksCallback();
    }

    @Test
    public void fetchSinksCallback_callOnFilteredFailure_noAction() {
        // Test Preparation
        ConcreteSinkJobFilter jobFilter = new ConcreteSinkJobFilter(mockedTexts, mockedResources, mockedFlowStoreProxy);

        // Activate Subject Under Test
        jobFilter.fetchSinksCallback.onFilteredFailure(new Throwable("Test-Throwable"));

        // Verify test
        verifyNoMoreInteractions(jobFilter.sinkList);
    }

    @Test
    public void fetchSinksCallback_callOnSuccess_modelsAdded() {
        // Test Preparation
        ConcreteSinkJobFilter jobFilter = new ConcreteSinkJobFilter(mockedTexts, mockedResources, mockedFlowStoreProxy);
        List<SinkModel> testModels = new ArrayList<SinkModel>();
        testModels.add(new SinkModel(123L, 234L, "FirstName", "FirstResource", "FirstDescription"));
        testModels.add(new SinkModel(321L, 432L, "SecondName", "SecondResource", "SecondDescription"));

        // Activate Subject Under Test
        jobFilter.fetchSinksCallback.onSuccess(testModels);

        // Verify test
        verify(jobFilter.sinkList).addAvailableItem("FirstName", "123");
        verify(jobFilter.sinkList).addAvailableItem("SecondName", "321");
        verify(jobFilter.sinkList).setEnabled(true);
    }

}
