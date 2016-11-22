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

import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
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
import static org.hamcrest.Matchers.equalTo;
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
        SinkJobFilter jobFilter = new SinkJobFilter(mockedTexts, mockedResources, "", mockedFlowStoreProxy);

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
        SinkJobFilter jobFilter = new SinkJobFilter(mockedTexts, mockedResources, "", mockedFlowStoreProxy);
        when(mockedTexts.sinkFilter_name()).thenReturn(MOCKED_NAME);

        // Activate Subject Under Test
        String jobFilterName = jobFilter.getName();

        // Verify test
        assertThat(jobFilterName, is(MOCKED_NAME));
    }

    @Test
    public void getValue_beforeSinksHaveBeenFetchedAndFilterValid_validCriteria() {
        // Test Preparation
        SinkJobFilter jobFilter = new SinkJobFilter(mockedTexts, mockedResources, "345", mockedFlowStoreProxy);

        // Activate Subject Under Test
        JobListCriteria criteria = jobFilter.getValue();

        // Verify test
        verifyNoMoreInteractions(jobFilter.sinkList);
        assertThat(criteria, equalTo(new JobListCriteria().where(new ListFilter<>(JobListCriteria.Field.SINK_ID, ListFilter.Op.EQUAL, "345"))));
    }

    @Test
    public void getValue_beforeSinksHaveBeenFetchedAndZeroFilter_emptyCriteria() {
        // Test Preparation
        SinkJobFilter jobFilter = new SinkJobFilter(mockedTexts, mockedResources, "0", mockedFlowStoreProxy);

        // Activate Subject Under Test
        JobListCriteria criteria = jobFilter.getValue();

        // Verify test
        verifyNoMoreInteractions(jobFilter.sinkList);
        assertThat(criteria, equalTo(new JobListCriteria()));
    }

    @Test
    public void getValue_beforeSinksHaveBeenFetchedAndEmptyFilter_emptyCriteria() {
        // Test Preparation
        SinkJobFilter jobFilter = new SinkJobFilter(mockedTexts, mockedResources, "", mockedFlowStoreProxy);

        // Activate Subject Under Test
        JobListCriteria criteria = jobFilter.getValue();

        // Verify test
        verifyNoMoreInteractions(jobFilter.sinkList);
        assertThat(criteria, equalTo(new JobListCriteria()));
    }

    @Test
    public void getValue_nullListEmpty_emptyCriteria() {
        // Test Preparation
        SinkJobFilter jobFilter = new SinkJobFilter(mockedTexts, mockedResources, null, mockedFlowStoreProxy);
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
        SinkJobFilter jobFilter = new SinkJobFilter(mockedTexts, mockedResources, null, mockedFlowStoreProxy);
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
        SinkJobFilter jobFilter = new SinkJobFilter(mockedTexts, mockedResources, null, mockedFlowStoreProxy);
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
        SinkJobFilter jobFilter = new SinkJobFilter(mockedTexts, mockedResources, null, mockedFlowStoreProxy);
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
        SinkJobFilter jobFilter = new SinkJobFilter(mockedTexts, mockedResources, "123", mockedFlowStoreProxy);

        // Activate Subject Under Test
        jobFilter.setParameter("");

        // Verify test
        verify(jobFilter.sinkList).setSelectedValue("");
        verifyNoMoreInteractions(jobFilter.sinkList);
    }

    @Test
    public void setParameterData_zeroParameter_zeroSinkSet() {
        // Test Preparation
        SinkJobFilter jobFilter = new SinkJobFilter(mockedTexts, mockedResources, "123", mockedFlowStoreProxy);

        // Activate Subject Under Test
        jobFilter.setParameter("0");

        // Verify test
        verify(jobFilter.sinkList).setSelectedValue("0");
        verifyNoMoreInteractions(jobFilter.sinkList);
    }

    @Test
    public void setParameterData_nonZeroParameter_validSinkSet() {
        // Test Preparation
        SinkJobFilter jobFilter = new SinkJobFilter(mockedTexts, mockedResources, "123", mockedFlowStoreProxy);

        // Activate Subject Under Test
        jobFilter.setParameter("321");

        // Verify test
        verify(jobFilter.sinkList).setSelectedValue("321");
        verifyNoMoreInteractions(jobFilter.sinkList);
    }

    @Test
    public void getParameterData_validValue_correctValueFetched() {
        // Test Preparation
        SinkJobFilter jobFilter = new SinkJobFilter(mockedTexts, mockedResources, "4321", mockedFlowStoreProxy);

        // Activate Subject Under Test
        String result = jobFilter.getParameter();

        // Verify test
        assertThat(result, is("4321"));
    }

    @Test
    public void addValueChangeHandler_callAddValueChangeHandler_valueChangeHandlerAdded() {
        // Test Preparation
        SinkJobFilter jobFilter = new SinkJobFilter(mockedTexts, mockedResources, "", mockedFlowStoreProxy);
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
            super(texts, resources, parameter, flowStoreProxy);
        }
        FetchSinksCallback fetchSinksCallback = new FetchSinksCallback();
    }

}
