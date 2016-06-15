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

package dk.dbc.dataio.gui.client.pages.harvester.ush.modify;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.components.PromptedCheckBox;
import dk.dbc.dataio.gui.client.components.PromptedTextArea;
import dk.dbc.dataio.gui.client.components.PromptedTextBox;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import dk.dbc.dataio.harvester.types.UshHarvesterProperties;
import dk.dbc.dataio.harvester.types.UshSolrHarvesterConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(GwtMockitoTestRunner.class)
public class PresenterEditImplTest extends PresenterImplTestBase {
    @Mock private EditPlace mockedPlace;
    @Mock private Texts mockedTexts;
    @Mock private View mockedView;
    @Mock private Widget mockedWidget;

    @Mock private PromptedTextBox mockedName;
    @Mock private PromptedTextArea mockedDescription;
    @Mock private PromptedTextBox mockedSubmitter;
    @Mock private PromptedTextBox mockedFormat;
    @Mock private PromptedTextBox mockedDestination;
    @Mock private PromptedCheckBox mockedEnabled;
    @Mock private Button mockedSaveButton;
    @Mock private Label mockedStatus;

    @Mock private UshSolrHarvesterConfig mockedConfig;
    @Mock private UshSolrHarvesterConfig.Content mockedContent;


    private PresenterEditImpl presenter;


    /**
     * Test data
     */

    private final UshSolrHarvesterConfig ushSolrHarvesterConfigWith123Content = new UshSolrHarvesterConfig(123L, 1,
            new UshSolrHarvesterConfig.Content().
                    withName("Name").
                    withDescription("Description").
                    withFormat("Format").
                    withDestination("Destination").
                    withSubmitterNumber(432).
                    withUshHarvesterJobId(999).
                    withUshHarvesterProperties(new UshHarvesterProperties().
                            withUri("Uri").
                            withAmountHarvested(33).
                            withCurrentStatus("Current Status").
                            withEnabled(true).
                            withId(222).
                            withJobClass("JobClass").
                            withLastHarvestFinishedDate(new Date(1000L)).
                            withLastHarvestStartedDate(new Date(2000L)).
                            withLastUpdatedDate(new Date(3000L)).
                            withMessage("Message").
                            withNextHarvestSchedule(new Date(4000L)).
                            withStorageUrl("StorageUrl")
                    )
    );
    private final UshSolrHarvesterConfig ushSolrHarvesterConfigContent1 = new UshSolrHarvesterConfig(1L, 1L, new UshSolrHarvesterConfig.Content().withName("Name1"));
    private final UshSolrHarvesterConfig ushSolrHarvesterConfigContent2 = new UshSolrHarvesterConfig(2L, 1L, new UshSolrHarvesterConfig.Content().withName("Name2"));
    private final List<UshSolrHarvesterConfig> ushSolrHarvesterConfigListWith123Content = Arrays.asList(
            ushSolrHarvesterConfigContent1,
            ushSolrHarvesterConfigWith123Content,
            ushSolrHarvesterConfigContent2
            );
    private final List<UshSolrHarvesterConfig> ushSolrHarvesterConfigListWithout123Content = Arrays.asList(
            ushSolrHarvesterConfigContent1,
            ushSolrHarvesterConfigContent2
            );



    /**
     * Test Preparation
     */
    @Before
    public void commonTestPreparation() {
        when(mockedPlace.getHarvesterId()).thenReturn(123L);
        presenter = new PresenterEditImpl(mockedPlace, "Header Text");
        when(presenter.viewInjector.getView()).thenReturn(mockedView);
        when(presenter.viewInjector.getTexts()).thenReturn(mockedTexts);
        mockedView.name = mockedName;
        mockedView.description = mockedDescription;
        mockedView.submitter = mockedSubmitter;
        mockedView.format = mockedFormat;
        mockedView.destination = mockedDestination;
        mockedView.enabled = mockedEnabled;
        mockedView.saveButton = mockedSaveButton;
        mockedView.status = mockedStatus;
        when(mockedConfig.getContent()).thenReturn(mockedContent);
        when(mockedView.asWidget()).thenReturn(mockedWidget);
        when(presenter.commonInjector.getFlowStoreProxyAsync()).thenReturn(mockedFlowStore);
        when(presenter.commonInjector.getProxyErrorTexts()).thenReturn(mockedProxyErrorTexts);
    }

    @Before
    public void prepareTexts() {
        when(mockedTexts.status_ConfigSuccessfullySaved()).thenReturn("status_ConfigSuccessfullySaved");
        when(mockedTexts.error_HarvesterNotFound()).thenReturn("Harvester not found");
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
        verify(mockedFlowStore).getHarvesterUshConfigs(any(PresenterEditImpl.GetHarvesterUshConfigsAsyncCallback.class));
        commonPostVerification();
    }

    @Test
    public void saveModel_ok_modelSaved() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setUshSolrHarvesterConfig(ushSolrHarvesterConfigWith123Content);
        // Test
        presenter.saveModel();

        // Test validation
        verifyStart();
        verify(presenter.commonInjector).getFlowStoreProxyAsync();
        commonPostVerification();
    }

    @Test
    public void GetHarvesterUshConfigsAsyncCallback_onFailure_errorMessage() {
        // Test preparation
        PresenterEditImpl.GetHarvesterUshConfigsAsyncCallback callback = presenter.new GetHarvesterUshConfigsAsyncCallback();
        when(mockedProxyException.getErrorCode()).thenReturn(ProxyError.PRECONDITION_FAILED);
        when(presenter.commonInjector.getProxyErrorTexts()).thenReturn(mockedProxyErrorTexts);
        when(mockedProxyErrorTexts.flowStoreProxy_preconditionFailedError()).thenReturn("PreconditionFailed");

        // Test
        callback.onFailure(mockedProxyException);

        // Test validation
        verify(mockedView).setErrorText("PreconditionFailed {UshSolrHarvesterConfig.id: 123}.");
        verify(presenter.commonInjector).getProxyErrorTexts();
        verify(mockedProxyException).getErrorCode();
        verify(mockedProxyErrorTexts).flowStoreProxy_preconditionFailedError();
        commonPostVerification();
    }

    @Test
    public void GetHarvesterUshConfigsAsyncCallback_onSuccessNotFound_errorMessage() {
        // Test preparation
        PresenterEditImpl.GetHarvesterUshConfigsAsyncCallback callback = presenter.new GetHarvesterUshConfigsAsyncCallback();

        // Test
        callback.onSuccess(ushSolrHarvesterConfigListWithout123Content);

        // Test validation
        verify(mockedTexts).error_HarvesterNotFound();
        verify(mockedView).setErrorText("Harvester not found");
        commonPostVerification();
    }

    @Test
    public void GetHarvesterUshConfigsAsyncCallback_onSuccessFound_ok() {
        // Test preparation
        PresenterEditImpl.GetHarvesterUshConfigsAsyncCallback callback = presenter.new GetHarvesterUshConfigsAsyncCallback();

        // Test
        callback.onSuccess(ushSolrHarvesterConfigListWith123Content);

        // Test validation
        verify(mockedName).setText("Name");
        verify(mockedName).setEnabled(false);
        verify(mockedDescription).setText("Description");
        verify(mockedDescription).setEnabled(true);
        verify(mockedSubmitter).setText("432");
        verify(mockedSubmitter).setEnabled(true);
        verify(mockedFormat).setText("Format");
        verify(mockedFormat).setEnabled(true);
        verify(mockedDestination).setText("Destination");
        verify(mockedDestination).setEnabled(true);
        verify(mockedEnabled).setValue(false);
        verify(mockedEnabled).setEnabled(true);
        verify(mockedStatus).setText("");
        commonPostVerification();
    }


    /*
     * Private methods
     */

    private void verifyStart() {
        verifyInitializeViewMocks();
        verifyInitializeViewFields();
        verify(mockedView).asWidget();
        verify(mockedContainerWidget).setWidget(mockedWidget);
    }

    private void verifyInitializeViewMocks() {
        verify(mockedView).setHeader("Header Text");
        verify(mockedView).setPresenter(presenter);
    }

    private void verifyInitializeViewFields() {
        verify(mockedName).setText("");
        verify(mockedName).setEnabled(false);
        verify(mockedDescription).setText("");
        verify(mockedDescription).setEnabled(false);
        verify(mockedSubmitter).setText("");
        verify(mockedSubmitter).setEnabled(false);
        verify(mockedFormat).setText("");
        verify(mockedFormat).setEnabled(false);
        verify(mockedDestination).setText("");
        verify(mockedDestination).setEnabled(false);
        verify(mockedEnabled).setValue(false);
        verify(mockedEnabled).setEnabled(false);
        verify(mockedStatus).setText("");
    }

    private void commonPostVerification() {
        verifyNoMoreInteractions(mockedTexts);
        verifyNoMoreInteractions(mockedView);
        verifyNoMoreInteractions(presenter.commonInjector);
        verifyNoMoreInteractions(mockedContainerWidget);
        verifyNoMoreInteractions(mockedEventBus);
        verifyNoMoreInteractions(mockedName);
        verifyNoMoreInteractions(mockedDescription);
        verifyNoMoreInteractions(mockedSubmitter);
        verifyNoMoreInteractions(mockedFormat);
        verifyNoMoreInteractions(mockedDestination);
        verifyNoMoreInteractions(mockedEnabled);
        verifyNoMoreInteractions(mockedSaveButton);
        verifyNoMoreInteractions(mockedStatus);
        verifyNoMoreInteractions(mockedConfig);
    }

}
