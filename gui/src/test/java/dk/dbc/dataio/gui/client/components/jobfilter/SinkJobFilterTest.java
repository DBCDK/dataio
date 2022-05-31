package dk.dbc.dataio.gui.client.components.jobfilter;

import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.modelBuilders.SinkModelBuilder;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.resources.Resources;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.ArgumentMatchers.any;
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
    @Mock
    private Texts mockedTexts;
    @Mock
    private Resources mockedResources;
    @Mock
    private FlowStoreProxyAsync mockedFlowStoreProxy;
    @Mock
    private ChangeHandler mockedChangeHandler;
    @Mock
    private HandlerRegistration mockedSinkListHandlerRegistration;


    //
    // Tests starts here...
    //
    @Test
    public void constructor_instantiate_instantiatedCorrectly() {
        // Activate Subject Under Test
        SinkJobFilter jobFilter = new SinkJobFilter(mockedTexts, mockedResources, "", mockedFlowStoreProxy, true);

        // Verify test
        assertThat(jobFilter.texts, is(mockedTexts));
        assertThat(jobFilter.resources, is(mockedResources));
        assertThat(jobFilter.flowStoreProxy, is(mockedFlowStoreProxy));
        verify(mockedFlowStoreProxy).findAllSinks(any(SinkJobFilter.FetchSinksCallback.class));
    }


    @Test
    public void getName_callGetName_fetchesStoredName() {
        // Constants
        final String MOCKED_NAME = "name from mocked Texts";

        // Test Preparation
        SinkJobFilter jobFilter = new SinkJobFilter(mockedTexts, mockedResources, "", mockedFlowStoreProxy, true);
        when(mockedTexts.sinkFilter_name()).thenReturn(MOCKED_NAME);

        // Activate Subject Under Test
        String jobFilterName = jobFilter.getName();

        // Verify test
        assertThat(jobFilterName, is(MOCKED_NAME));
    }

    @Test
    public void getValue_beforeSinksHaveBeenFetchedAndFilterValid_validCriteria() {
        // Test Preparation
        SinkJobFilter jobFilter = new SinkJobFilter(mockedTexts, mockedResources, "345", mockedFlowStoreProxy, true);

        // Activate Subject Under Test
        JobListCriteria criteria = jobFilter.getValue();

        // Verify test
        verifyNoMoreInteractions(jobFilter.sinkList);
        assertThat(criteria, equalTo(new JobListCriteria().where(new ListFilter<>(JobListCriteria.Field.SINK_ID, ListFilter.Op.EQUAL, "345"))));
    }

    @Test
    public void getValue_beforeSinksHaveBeenFetchedAndZeroFilter_emptyCriteria() {
        // Test Preparation
        SinkJobFilter jobFilter = new SinkJobFilter(mockedTexts, mockedResources, "0", mockedFlowStoreProxy, true);

        // Activate Subject Under Test
        JobListCriteria criteria = jobFilter.getValue();

        // Verify test
        verifyNoMoreInteractions(jobFilter.sinkList);
        assertThat(criteria, equalTo(new JobListCriteria()));
    }

    @Test
    public void getValue_beforeSinksHaveBeenFetchedAndEmptyFilter_emptyCriteria() {
        // Test Preparation
        SinkJobFilter jobFilter = new SinkJobFilter(mockedTexts, mockedResources, "", mockedFlowStoreProxy, true);

        // Activate Subject Under Test
        JobListCriteria criteria = jobFilter.getValue();

        // Verify test
        verifyNoMoreInteractions(jobFilter.sinkList);
        assertThat(criteria, equalTo(new JobListCriteria()));
    }

    @Test
    public void getValue_nullListEmpty_emptyCriteria() {
        // Test Preparation
        SinkJobFilter jobFilter = new SinkJobFilter(mockedTexts, mockedResources, null, mockedFlowStoreProxy, true);
        when(jobFilter.sinkList.getSelectedKey()).thenReturn(null);

        // Activate Subject Under Test
        JobListCriteria criteria = jobFilter.getValue();

        // Verify test
        verify(jobFilter.sinkList).getSelectedKey();
        verifyNoMoreInteractions(jobFilter.sinkList);
        assertThat(criteria, equalTo(new JobListCriteria()));
    }

    @Test
    public void getValue_emptyListEmpty_emptyCriteria() {
        // Test Preparation
        SinkJobFilter jobFilter = new SinkJobFilter(mockedTexts, mockedResources, null, mockedFlowStoreProxy, true);
        when(jobFilter.sinkList.getSelectedKey()).thenReturn("");

        // Activate Subject Under Test
        JobListCriteria criteria = jobFilter.getValue();

        // Verify test
        verify(jobFilter.sinkList).getSelectedKey();
        verifyNoMoreInteractions(jobFilter.sinkList);
        assertThat(criteria, equalTo(new JobListCriteria()));
    }

    @Test
    public void getValue_zeroListEmpty_emptyCriteria() {
        // Test Preparation
        SinkJobFilter jobFilter = new SinkJobFilter(mockedTexts, mockedResources, null, mockedFlowStoreProxy, true);
        when(jobFilter.sinkList.getSelectedKey()).thenReturn("0");

        // Activate Subject Under Test
        JobListCriteria criteria = jobFilter.getValue();

        // Verify test
        verify(jobFilter.sinkList).getSelectedKey();
        verifyNoMoreInteractions(jobFilter.sinkList);
        assertThat(criteria, equalTo(new JobListCriteria()));
    }

    @Test
    public void getValue_nonZeroListEmpty_validCriteria() {
        // Test Preparation
        SinkJobFilter jobFilter = new SinkJobFilter(mockedTexts, mockedResources, null, mockedFlowStoreProxy, true);
        when(jobFilter.sinkList.getSelectedKey()).thenReturn("123");

        // Activate Subject Under Test
        JobListCriteria criteria = jobFilter.getValue();

        // Verify test
        verify(jobFilter.sinkList).getSelectedKey();
        verifyNoMoreInteractions(jobFilter.sinkList);
        assertThat(criteria, equalTo(new JobListCriteria().where(new ListFilter<>(JobListCriteria.Field.SINK_ID, ListFilter.Op.EQUAL, "123"))));
    }

    @Test
    public void setParameterData_emptyParameter_noSinkSet() {
        // Test Preparation
        SinkJobFilter jobFilter = new SinkJobFilter(mockedTexts, mockedResources, "123", mockedFlowStoreProxy, true);

        // Activate Subject Under Test
        jobFilter.setParameter("");

        // Verify test
        verify(jobFilter.sinkList).setSelectedValue("");
        verifyNoMoreInteractions(jobFilter.sinkList);
    }

    @Test
    public void setParameterData_zeroParameter_zeroSinkSet() {
        // Test Preparation
        SinkJobFilter jobFilter = new SinkJobFilter(mockedTexts, mockedResources, "123", mockedFlowStoreProxy, true);

        // Activate Subject Under Test
        jobFilter.setParameter("0");

        // Verify test
        verify(jobFilter.sinkList).setSelectedValue("0");
        verifyNoMoreInteractions(jobFilter.sinkList);
    }

    @Test
    public void setParameterData_nonZeroParameter_validSinkSet() {
        // Test Preparation
        SinkJobFilter jobFilter = new SinkJobFilter(mockedTexts, mockedResources, "123", mockedFlowStoreProxy, true);

        // Activate Subject Under Test
        jobFilter.setParameter("321");

        // Verify test
        verify(jobFilter.sinkList).setSelectedValue("321");
        verifyNoMoreInteractions(jobFilter.sinkList);
    }

    @Test
    public void getParameterData_validValue_correctValueFetched() {
        // Test Preparation
        SinkJobFilter jobFilter = new SinkJobFilter(mockedTexts, mockedResources, "4321", mockedFlowStoreProxy, true);

        // Activate Subject Under Test
        String result = jobFilter.getParameter();

        // Verify test
        assertThat(result, is("4321"));
    }

    @Test
    public void addValueChangeHandler_callAddValueChangeHandler_valueChangeHandlerAdded() {
        // Test Preparation
        SinkJobFilter jobFilter = new SinkJobFilter(mockedTexts, mockedResources, "", mockedFlowStoreProxy, true);
        when(jobFilter.sinkList.addChangeHandler(any(ChangeHandler.class))).thenReturn(mockedSinkListHandlerRegistration);

        // Activate Subject Under Test
        HandlerRegistration handlerRegistration = jobFilter.addChangeHandler(mockedChangeHandler);

        // Verify test
        verify(jobFilter.sinkList).addChangeHandler(mockedChangeHandler);
        assertThat(handlerRegistration, not(nullValue()));
        assertThat(handlerRegistration, is(mockedSinkListHandlerRegistration));
    }

    @Test
    public void fetchSinksCallback_callOnFilteredFailure_noAction() {
        // Test Preparation
        ConcreteSinkJobFilter jobFilter = new ConcreteSinkJobFilter(mockedTexts, mockedResources, "", mockedFlowStoreProxy);

        // Activate Subject Under Test
        jobFilter.fetchSinksCallback.onFilteredFailure(new Throwable("Test-Throwable"));

        // Verify test
        verifyNoMoreInteractions(jobFilter.sinkList);
    }

    @Test
    public void fetchSinksCallback_callOnSuccess_modelsAdded() {
        // Test Preparation
        ConcreteSinkJobFilter jobFilter = new ConcreteSinkJobFilter(mockedTexts, mockedResources, "", mockedFlowStoreProxy);
        List<SinkModel> testModels = new ArrayList<>();
        testModels.add(new SinkModelBuilder().setId(123L).setName("FirstName").build());
        testModels.add(new SinkModelBuilder().setId(321L).setName("SecondName").build());

        // Activate Subject Under Test
        jobFilter.fetchSinksCallback.onSuccess(testModels);

        // Verify test
        verify(jobFilter.sinkList).addAvailableItem("FirstName", "123");
        verify(jobFilter.sinkList).addAvailableItem("SecondName", "321");
        verify(jobFilter.sinkList).setEnabled(true);
    }

    class ConcreteSinkJobFilter extends SinkJobFilter {
        ConcreteSinkJobFilter(Texts texts, Resources resources, String parameter, FlowStoreProxyAsync flowStoreProxy) {
            super(texts, resources, parameter, flowStoreProxy, true);
        }

        FetchSinksCallback fetchSinksCallback = new FetchSinksCallback();
    }

}
