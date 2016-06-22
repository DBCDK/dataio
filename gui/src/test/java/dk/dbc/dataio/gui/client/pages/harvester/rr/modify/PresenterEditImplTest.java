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

package dk.dbc.dataio.gui.client.pages.harvester.rr.modify;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.gui.client.components.PopupMapEntry;
import dk.dbc.dataio.gui.client.components.PromptedCheckBox;
import dk.dbc.dataio.gui.client.components.PromptedMultiList;
import dk.dbc.dataio.gui.client.components.PromptedPasswordTextBox;
import dk.dbc.dataio.gui.client.components.PromptedTextBox;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import dk.dbc.dataio.harvester.types.OpenAgencyTarget;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(GwtMockitoTestRunner.class)
public class PresenterEditImplTest extends PresenterImplTestBase {
    @Mock private EditPlace mockedPlace;
    @Mock private Texts mockedTexts;
    @Mock private View mockedView;
    @Mock private PromptedTextBox mockedName;
    @Mock private PromptedTextBox mockedResource;
    @Mock private PromptedTextBox mockedTargetUrl;
    @Mock private PromptedTextBox mockedTargetGroup;
    @Mock private PromptedTextBox mockedTargetUser;
    @Mock private PromptedPasswordTextBox mockedTargetPassword;
    @Mock private PromptedTextBox mockedConsumerId;
    @Mock private PromptedTextBox mockedSize;
    @Mock private PromptedMultiList mockedFormatOverrides;
    @Mock private PromptedCheckBox mockedRelations;
    @Mock private PromptedTextBox mockedDestination;
    @Mock private PromptedTextBox mockedFormat;
    @Mock private PromptedTextBox mockedType;
    @Mock private PromptedCheckBox mockedEnabled;
    @Mock private Button mockedUpdateButton;
    @Mock private Label mockedStatus;
    @Mock private PopupMapEntry mockedPopupFormatOverrideEntry;
    @Mock private Widget mockedWidget;
    @Mock private Map mockedMap;
    @Mock private RRHarvesterConfig mockedConfig;
    @Mock private RRHarvesterConfig.Content mockedContent;
    @Mock private OpenAgencyTarget mockedOpenAgencyTarget;


    private PresenterEditImpl presenter;


    /**
     * Test data
     */
    private final OpenAgencyTarget openAgencyTarget123 = new OpenAgencyTarget();
    private final HashMap<Integer, String> formatOverrides123 = new HashMap<>();
    private final RRHarvesterConfig.Content content1 = new RRHarvesterConfig.Content().withId("id1");
    private final RRHarvesterConfig.Content content2 = new RRHarvesterConfig.Content().withId("id2");
    private final RRHarvesterConfig.Content content123 =
            new RRHarvesterConfig.Content().
                    withId("id123").
                    withResource("resource123").
                    withOpenAgencyTarget(openAgencyTarget123).
                    withConsumerId("ConsumerId123").
                    withBatchSize(1).
                    withFormatOverrides(formatOverrides123).
                    withIncludeRelations(true).
                    withDestination("Destination123").
                    withFormat("Format123").
                    withType(JobSpecification.Type.TEST).
                    withEnabled(true);
    private final RRHarvesterConfig rrHarvesterConfig1 = new RRHarvesterConfig(111L, 222L, content1);
    private final RRHarvesterConfig rrHarvesterConfig2 = new RRHarvesterConfig(112L, 223L, content2);
    private final RRHarvesterConfig rrHarvesterConfig123 = new RRHarvesterConfig(123L, 234L, content123);
    private final List<RRHarvesterConfig> harvesterListWithout123Content = new ArrayList<>();
    private final List<RRHarvesterConfig> harvesterListWith123Content = new ArrayList<>();
    @Before
    public void prepareTestData() {
        openAgencyTarget123.setUrl("Url123");
        openAgencyTarget123.setGroup("Group123");
        openAgencyTarget123.setUser("User123");
        openAgencyTarget123.setPassword("Password123");
        formatOverrides123.put(123, "value123");
        formatOverrides123.put(1234, "value1234");
        harvesterListWith123Content.add(rrHarvesterConfig1);
        harvesterListWith123Content.add(rrHarvesterConfig123);
        harvesterListWith123Content.add(rrHarvesterConfig2);
        harvesterListWithout123Content.add(rrHarvesterConfig1);
        harvesterListWithout123Content.add(rrHarvesterConfig2);
    }


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
        mockedView.resource = mockedResource;
        mockedView.targetUrl = mockedTargetUrl;
        mockedView.targetGroup = mockedTargetGroup;
        mockedView.targetUser = mockedTargetUser;
        mockedView.targetPassword = mockedTargetPassword;
        mockedView.consumerId = mockedConsumerId;
        mockedView.size = mockedSize;
        mockedView.formatOverrides = mockedFormatOverrides;
        mockedView.relations = mockedRelations;
        mockedView.destination = mockedDestination;
        mockedView.format = mockedFormat;
        mockedView.type = mockedType;
        mockedView.enabled = mockedEnabled;
        mockedView.updateButton = mockedUpdateButton;
        mockedView.status = mockedStatus;
        mockedView.popupFormatOverrideEntry = mockedPopupFormatOverrideEntry;
        when(mockedView.asWidget()).thenReturn(mockedWidget);
        when(mockedConfig.getContent()).thenReturn(mockedContent);
        when(mockedContent.getOpenAgencyTarget()).thenReturn(mockedOpenAgencyTarget);
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
        verify(mockedFlowStore).findAllHarvesterRrConfigs(any(PresenterEditImpl.GetHarvesterRrConfigsAsyncCallback.class));
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
        verify(mockedFlowStore).findAllHarvesterRrConfigs(any(PresenterEditImpl.GetHarvesterRrConfigsAsyncCallback.class));
        verify(mockedFlowStore).updateHarvesterConfig(eq(presenter.config), any(PresenterEditImpl.UpdateHarvesterConfigAsyncCallback.class));
        commonPostVerification();
    }

    @Test
    public void GetHarvesterRrConfigsAsyncCallback_onFailure_errorMessage() {
        // Test preparation
        PresenterEditImpl.GetHarvesterRrConfigsAsyncCallback callback = presenter.new GetHarvesterRrConfigsAsyncCallback();
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
    public void GetHarvesterRrConfigsAsyncCallback_onSuccessNotFound_errorMessage() {
        // Test preparation
        PresenterEditImpl.GetHarvesterRrConfigsAsyncCallback callback = presenter.new GetHarvesterRrConfigsAsyncCallback();

        // Test
        callback.onSuccess(harvesterListWithout123Content);

        // Test validation
        verify(mockedTexts).error_HarvesterNotFound();
        commonPostVerification();
    }

    @Test
    public void GetHarvesterRrConfigsAsyncCallback_onSuccessFound_ok() {
        // Test preparation
        PresenterEditImpl.GetHarvesterRrConfigsAsyncCallback callback = presenter.new GetHarvesterRrConfigsAsyncCallback();

        // Test
        callback.onSuccess(harvesterListWith123Content);

        // Test validation
        verify(mockedName).setText("id123");
        verify(mockedName).setEnabled(true);
        verify(mockedResource).setText("resource123");
        verify(mockedResource).setEnabled(true);
        verify(mockedTargetUrl).setText("Url123");
        verify(mockedTargetUrl).setEnabled(true);
        verify(mockedTargetGroup).setText("Group123");
        verify(mockedTargetGroup).setEnabled(true);
        verify(mockedTargetUser).setText("User123");
        verify(mockedTargetUser).setEnabled(true);
        verify(mockedTargetPassword).setText("Password123");
        verify(mockedTargetPassword).setEnabled(true);
        verify(mockedConsumerId).setText("ConsumerId123");
        verify(mockedConsumerId).setEnabled(true);
        verify(mockedSize).setText("1");
        verify(mockedSize).setEnabled(true);
        verify(mockedView).setFormatOverrides(new HashMap<>());
        verify(mockedFormatOverrides).setEnabled(true);
        verify(mockedRelations).setValue(true);
        verify(mockedRelations).setEnabled(true);
        verify(mockedDestination).setText("Destination123");
        verify(mockedDestination).setEnabled(true);
        verify(mockedFormat).setText("Format123");
        verify(mockedFormat).setEnabled(true);
        verify(mockedType).setText("TEST");
        verify(mockedType).setEnabled(true);
        verify(mockedEnabled).setValue(true);
        verify(mockedEnabled).setEnabled(true);
        verify(mockedStatus, times(1)).setText("");
        verify(mockedUpdateButton).setVisible(false);
        verify(mockedPopupFormatOverrideEntry).hide();
        commonPostVerification();
    }

    @Test
    public void UpdateHarvesterRrConfigAsyncCallback_onFailure_errorMessage() {
        // Test preparation
        PresenterEditImpl.UpdateHarvesterConfigAsyncCallback callback = presenter.new UpdateHarvesterConfigAsyncCallback();
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
    public void UpdateHarvesterRrConfigAsyncCallback_onSuccessNotFound_errorMessage() {
        // Test preparation
        PresenterEditImpl.UpdateHarvesterConfigAsyncCallback callback = presenter.new UpdateHarvesterConfigAsyncCallback();

        // Test
        callback.onSuccess(rrHarvesterConfig1);

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
        verify(mockedName).setText("");
        verify(mockedName).setEnabled(false);
        verify(mockedResource).setText("");
        verify(mockedResource).setEnabled(false);
        verify(mockedTargetUrl).setText("");
        verify(mockedTargetUrl).setEnabled(false);
        verify(mockedTargetGroup).setText("");
        verify(mockedTargetGroup).setEnabled(false);
        verify(mockedTargetUser).setText("");
        verify(mockedTargetUser).setEnabled(false);
        verify(mockedTargetPassword).setText("");
        verify(mockedTargetPassword).setEnabled(false);
        verify(mockedConsumerId).setText("");
        verify(mockedConsumerId).setEnabled(false);
        verify(mockedSize).setText("");
        verify(mockedSize).setEnabled(false);
        verify(mockedView).setFormatOverrides(new HashMap<>());
        verify(mockedFormatOverrides).setEnabled(false);
        verify(mockedRelations).setValue(false);
        verify(mockedRelations).setEnabled(false);
        verify(mockedDestination).setText("");
        verify(mockedDestination).setEnabled(false);
        verify(mockedFormat).setText("");
        verify(mockedFormat).setEnabled(false);
        verify(mockedType).setText("");
        verify(mockedType).setEnabled(false);
        verify(mockedEnabled).setValue(false);
        verify(mockedEnabled).setEnabled(false);
        verify(mockedStatus).setText("");
        verify(mockedUpdateButton).setVisible(false);
        verify(mockedPopupFormatOverrideEntry).hide();
    }

    private void commonPostVerification() {
        verifyNoMoreInteractions(mockedTexts);
        verifyNoMoreInteractions(presenter.commonInjector);
        verifyNoMoreInteractions(mockedContainerWidget);
        verifyNoMoreInteractions(mockedEventBus);
        verifyNoMoreInteractions(mockedName);
        verifyNoMoreInteractions(mockedResource);
        verifyNoMoreInteractions(mockedTargetUrl);
        verifyNoMoreInteractions(mockedTargetGroup);
        verifyNoMoreInteractions(mockedTargetUser);
        verifyNoMoreInteractions(mockedTargetPassword);
        verifyNoMoreInteractions(mockedConsumerId);
        verifyNoMoreInteractions(mockedSize);
        verifyNoMoreInteractions(mockedFormatOverrides);
        verifyNoMoreInteractions(mockedRelations);
        verifyNoMoreInteractions(mockedDestination);
        verifyNoMoreInteractions(mockedFormat);
        verifyNoMoreInteractions(mockedType);
        verifyNoMoreInteractions(mockedEnabled);
        verifyNoMoreInteractions(mockedUpdateButton);
        verifyNoMoreInteractions(mockedStatus);
        verifyNoMoreInteractions(mockedPopupFormatOverrideEntry);
        verifyNoMoreInteractions(mockedMap);
        verifyNoMoreInteractions(mockedConfig);
        verifyNoMoreInteractions(mockedContent);
        verifyNoMoreInteractions(mockedOpenAgencyTarget);
        verifyNoMoreInteractions(mockedFlowStore);
        verifyNoMoreInteractions(mockedProxyErrorTexts);
    }

}
