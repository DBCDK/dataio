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

package dk.dbc.dataio.gui.client.pages.harvester.ticklerepo.modify;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.gui.client.components.prompted.PromptedCheckBox;
import dk.dbc.dataio.gui.client.components.prompted.PromptedList;
import dk.dbc.dataio.gui.client.components.prompted.PromptedTextArea;
import dk.dbc.dataio.gui.client.components.prompted.PromptedTextBox;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import dk.dbc.dataio.harvester.types.HarvesterConfig;
import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(GwtMockitoTestRunner.class)
public class PresenterEditImplTest extends PresenterImplTestBase {
    @Mock private EditPlace mockedPlace;
    @Mock private Texts mockedTexts;
    @Mock private View mockedView;
    @Mock private PromptedTextBox mockedId;
    @Mock private PromptedTextBox mockedName;
    @Mock private PromptedTextArea mockedDescription;
    @Mock private PromptedTextBox mockedDestination;
    @Mock private PromptedTextBox mockedFormat;
    @Mock private PromptedList mockedType;
    @Mock private PromptedCheckBox mockedEnabled;
    @Mock private Label mockedStatus;
    @Mock private Button mockedDeleteButton;
    @Mock private Button mockedTaskRecordHarvestButton;

    @Mock private Widget mockedWidget;


    private PresenterEditImpl presenter;


    /*
     * Test data
     */
    private final TickleRepoHarvesterConfig.Content content =
            new TickleRepoHarvesterConfig.Content().
                    withId("id123").
                    withDatasetName("dataSetName123").
                    withDescription("Description123").
                    withDestination("Destination123").
                    withFormat("Format123").
                    withType(JobSpecification.Type.TEST).
                    withEnabled(true);
    private final TickleRepoHarvesterConfig tickleHarvesterConfig = new TickleRepoHarvesterConfig(123L, 234L, content);


    /**
     * Test Preparation
     */
    @Before
    public void commonTestPreparation() {
        when(mockedPlace.getHarvesterId()).thenReturn(123L);
        presenter = new PresenterEditImpl(mockedPlace, "Header Text");
        when(presenter.viewInjector.getView()).thenReturn(mockedView);
        when(presenter.viewInjector.getTexts()).thenReturn(mockedTexts);
        mockedView.id = mockedId;
        mockedView.name = mockedName;
        mockedView.description = mockedDescription;
        mockedView.destination = mockedDestination;
        mockedView.format = mockedFormat;
        mockedView.type = mockedType;
        mockedView.enabled = mockedEnabled;
        mockedView.status = mockedStatus;
        mockedView.deleteButton = mockedDeleteButton;
        mockedView.taskRecordHarvestButton = mockedTaskRecordHarvestButton;
        when(mockedView.asWidget()).thenReturn(mockedWidget);
        when(presenter.commonInjector.getFlowStoreProxyAsync()).thenReturn(mockedFlowStore);
        when(presenter.commonInjector.getProxyErrorTexts()).thenReturn(mockedProxyErrorTexts);
    }

    @Before
    public void prepareTexts() {
        when(mockedTexts.status_ConfigSuccessfullySaved()).thenReturn("status_ConfigSuccessfullySaved");
    }


    /**
     * Run tests
     */
    @Test
    public void constructor_valid_ok() {
        commonPostVerification();
    }

    @Test
    public void start_valid_ok() {  // Includes call to (and test of) initializeModel()
        // Test
        presenter.start(mockedContainerWidget, mockedEventBus);

        // Test validation
        verifyStart();
        verify(presenter.commonInjector).getFlowStoreProxyAsync();
        verify(mockedFlowStore).getTickleRepoHarvesterConfig(any(Long.class), any(PresenterEditImpl.GetTickleRepoHarvesterConfigAsyncCallback.class));
        commonPostVerification();
    }

    @Test
    public void saveModel_ok_modelSaved() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);

        // Test
        presenter.saveModel();

        // Test validation
        verifyStart();
        verify(presenter.commonInjector, times(2)).getFlowStoreProxyAsync();
        verify(mockedFlowStore).getTickleRepoHarvesterConfig(any(Long.class), any(PresenterEditImpl.GetTickleRepoHarvesterConfigAsyncCallback.class));
        verify(mockedFlowStore).updateHarvesterConfig(any(HarvesterConfig.class), any(PresenterEditImpl.GetTickleRepoHarvesterConfigAsyncCallback.class));
        commonPostVerification();
    }

    @Test
    public void GetTickleRepoHarvesterConfigsAsyncCallback_onFailure_errorMessage() {
        // Test preparation
        PresenterEditImpl.GetTickleRepoHarvesterConfigAsyncCallback callback = presenter.new GetTickleRepoHarvesterConfigAsyncCallback();
        when(mockedProxyException.getErrorCode()).thenReturn(ProxyError.PRECONDITION_FAILED);

        // Test
        callback.onFailure(mockedProxyException);

        // Test validation
        verify(presenter.commonInjector).getProxyErrorTexts();
        verify(mockedProxyException).getErrorCode();
        verify(mockedProxyErrorTexts).flowStoreProxy_preconditionFailedError();
        commonPostVerification();
    }

    @Test
    public void GetTickleRepoHarvesterConfigsAsyncCallback_onSuccessNotFound_errorMessage() {
        // Test preparation
        PresenterEditImpl.GetTickleRepoHarvesterConfigAsyncCallback callback = presenter.new GetTickleRepoHarvesterConfigAsyncCallback();

        // Test
        callback.onSuccess(null);

        // Test validation
        verify(mockedTexts).error_HarvesterNotFound();
        commonPostVerification();
    }

    @Test
    public void GetTickleRepoHarvesterConfigsAsyncCallback_onSuccessFound_ok() {
        // Test preparation
        PresenterEditImpl.GetTickleRepoHarvesterConfigAsyncCallback callback = presenter.new GetTickleRepoHarvesterConfigAsyncCallback();

        // Test
        callback.onSuccess(tickleHarvesterConfig);

        // Test validation
        verify(mockedId).setText("id123");
        verify(mockedId).setEnabled(true);
        verify(mockedName).setText("dataSetName123");
        verify(mockedName).setEnabled(true);
        verify(mockedDescription).setText("Description123");
        verify(mockedDescription).setEnabled(true);
        verify(mockedDestination).setText("Destination123");
        verify(mockedDestination).setEnabled(true);
        verify(mockedFormat).setText("Format123");
        verify(mockedFormat).setEnabled(true);
        verifyType(mockedType, "TEST", true);
        verify(mockedEnabled).setValue(true);
        verify(mockedEnabled).setEnabled(true);
        verify(mockedStatus, times(1)).setText("");
        commonPostVerification();
    }

    @Test
    public void UpdateTickleRepoHarvesterConfigAsyncCallback_onFailure_errorMessage() {
        // Test preparation
        PresenterEditImpl.UpdateTickleRepoHarvesterConfigAsyncCallback callback = presenter.new UpdateTickleRepoHarvesterConfigAsyncCallback();
        when(mockedProxyException.getErrorCode()).thenReturn(ProxyError.PRECONDITION_FAILED);

        // Test
        callback.onFailure(mockedProxyException);

        // Test validation
        verify(presenter.commonInjector).getProxyErrorTexts();
        verify(mockedProxyException).getErrorCode();
        verify(mockedProxyErrorTexts).flowStoreProxy_preconditionFailedError();
        commonPostVerification();
    }

    @Test
    public void UpdateTickleRepoHarvesterConfigAsyncCallback_onSuccess_errorMessage() {
        // Test preparation
        PresenterEditImpl.UpdateTickleRepoHarvesterConfigAsyncCallback callback = presenter.new UpdateTickleRepoHarvesterConfigAsyncCallback();

        // Test
        callback.onSuccess(tickleHarvesterConfig);

        // Test validation
        verify(mockedTexts).status_ConfigSuccessfullySaved();
        verify(mockedStatus).setText("status_ConfigSuccessfullySaved");
        commonPostVerification();
    }


    /*
     * Private methods
     */

    private void verifyStart() {
        verifyInitializeViewMocks();
        verifyInitializeViewFields();
        verify(mockedContainerWidget).setWidget(mockedWidget);
    }

    private void verifyInitializeViewMocks() {
        verify(mockedView).setHeader("Header Text");
        verify(mockedView).setPresenter(presenter);
    }

    private void verifyInitializeViewFields() {
        verify(mockedId).setText("");
        verify(mockedId).setEnabled(false);
        verify(mockedName).setText("");
        verify(mockedName).setEnabled(false);
        verify(mockedDescription).setText("");
        verify(mockedDescription).setEnabled(false);
        verify(mockedDestination).setText("");
        verify(mockedDestination).setEnabled(false);
        verify(mockedFormat).setText("");
        verify(mockedFormat).setEnabled(false);
        verifyType(mockedType, "", false);
        verify(mockedType).setEnabled(false);
        verify(mockedEnabled).setValue(false);
        verify(mockedEnabled).setEnabled(false);
        verify(mockedStatus).setText("");
    }

    private void verifyType(PromptedList list, String value, boolean enabled) {
        verify(list).clear();
        verify(list).addAvailableItem("TRANSIENT");
        verify(list).addAvailableItem("PERSISTENT");
        verify(list).addAvailableItem("TEST");
        verify(list).addAvailableItem("ACCTEST");
        verify(list).setSelectedValue(value);
        verify(list).setEnabled(enabled);
    }

    private void commonPostVerification() {
        verifyNoMoreInteractions(mockedTexts);
        verifyNoMoreInteractions(presenter.commonInjector);
        verifyNoMoreInteractions(mockedContainerWidget);
        verifyNoMoreInteractions(mockedEventBus);
        verifyNoMoreInteractions(mockedId);
        verifyNoMoreInteractions(mockedName);
        verifyNoMoreInteractions(mockedDescription);
        verifyNoMoreInteractions(mockedDestination);
        verifyNoMoreInteractions(mockedFormat);
        verifyNoMoreInteractions(mockedType);
        verifyNoMoreInteractions(mockedEnabled);
        verifyNoMoreInteractions(mockedStatus);
        verifyNoMoreInteractions(mockedFlowStore);
        verifyNoMoreInteractions(mockedProxyErrorTexts);
    }

}
