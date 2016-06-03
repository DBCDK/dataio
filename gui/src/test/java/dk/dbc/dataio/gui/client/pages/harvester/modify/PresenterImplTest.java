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

package dk.dbc.dataio.gui.client.pages.harvester.modify;

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
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import dk.dbc.dataio.harvester.types.OpenAgencyTarget;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * PresenterImpl unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class PresenterImplTest extends PresenterImplTestBase {

    /*
     * Mocks
     */
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


    /*
     * Subject Under Test
     */
    private PresenterImplConcrete presenter;


    /*
     * Test attributes
     */
    private Boolean initializeModelCalled = false;
    private Boolean saveModelCalled = false;


    /*
     * The class under test: PresenterImpl is an abstract class, and we therefore need to
     * construct a concrete PresenterImpl class, implementing the missing abstract methods
     */
    class PresenterImplConcrete extends PresenterImpl {
        public PresenterImplConcrete(String header) {
            super(header);
        }
        @Override
        void initializeModel() {
            initializeModelCalled = true;
        }
        @Override
        void saveModel() {
            saveModelCalled = true;
        }
        String getHeader() {
            return header;
        }
    }


    /*
     * Test preparation
     */

    @Before
    public void commonTestPreparation() {
        initializeModelCalled = false;
        saveModelCalled = false;
        presenter = new PresenterImplConcrete(header);
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
    }

    @Before
    public void prepareTexts() {
        when(mockedTexts.error_InputFieldValidationError()).thenReturn("InputFieldValidationError");
        when(mockedTexts.error_NumericSubmitterValidationError()).thenReturn("NumericSubmitterValidationError");
    }


    /*
     * Tests start here
     */

    @Test
    public void constructor_default_ok() {
        // Test
        assertThat(presenter.getHeader(), is("Header Text"));

        // Test verification
        commonPostVerification();
    }

    @Test
    public void start_default_ok() {
        // Test preparation

        // Test
        presenter.start(mockedContainerWidget, mockedEventBus);

        // Test verification
        verifyStart();
        commonPostVerification();
    }

    @Test
    public void nameChanged_null_noAction() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(null);

        // Test
        presenter.nameChanged("name");

        // Test verification
        verifyStart();
        commonPostVerification();
    }

    @Test
    public void nameChanged_validName_nameSet() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(mockedConfig);

        // Test
        presenter.nameChanged("name");

        // Test verification
        verifyStart();
        verify(mockedConfig).getContent();
        verify(mockedContent).withId("name");
        commonPostVerification();
    }

    @Test
    public void resourceChanged_null_noAction() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(null);

        // Test
        presenter.resourceChanged("resource");

        // Test verification
        verifyStart();
        commonPostVerification();
    }

    @Test
    public void resourceChanged_validResource_resourceSet() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(mockedConfig);

        // Test
        presenter.resourceChanged("resource");

        // Test verification
        verifyStart();
        verify(mockedConfig).getContent();
        verify(mockedContent).withResource("resource");
        commonPostVerification();
    }

    @Test
    public void targetUrlChanged_null_noAction() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(null);

        // Test
        presenter.targetUrlChanged("targetUrl");

        // Test verification
        verifyStart();
        commonPostVerification();
    }

    @Test
    public void targetUrlChanged_validUrl_targetUrlSet() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(mockedConfig);

        // Test
        presenter.targetUrlChanged("targetUrl");

        // Test verification
        verifyStart();
        verify(mockedConfig).getContent();
        verify(mockedContent).getOpenAgencyTarget();
        verify(mockedOpenAgencyTarget).setUrl("targetUrl");
        commonPostVerification();
    }

    @Test
    public void targetGroupChanged_null_noAction() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(null);

        // Test
        presenter.targetGroupChanged("targetGroup");

        // Test verification
        verifyStart();
        commonPostVerification();
    }

    @Test
    public void targetGroupChanged_validGroup_targetGroupSet() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(mockedConfig);

        // Test
        presenter.targetGroupChanged("targetGroup");

        // Test verification
        verifyStart();
        verify(mockedConfig).getContent();
        verify(mockedContent).getOpenAgencyTarget();
        verify(mockedOpenAgencyTarget).setGroup("targetGroup");
        commonPostVerification();
    }

    @Test
    public void targetUserChanged_null_noAction() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(null);

        // Test
        presenter.targetUserChanged("targetUser");

        // Test verification
        verifyStart();
        commonPostVerification();
    }

    @Test
    public void targetUserChanged_validUser_targetUserSet() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(mockedConfig);

        // Test
        presenter.targetUserChanged("targetUser");

        // Test verification
        verifyStart();
        verify(mockedConfig).getContent();
        verify(mockedContent).getOpenAgencyTarget();
        verify(mockedOpenAgencyTarget).setUser("targetUser");
        commonPostVerification();
    }

    @Test
    public void targetPasswordChanged_null_noAction() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(null);

        // Test
        presenter.targetPasswordChanged("targetPassword");

        // Test verification
        verifyStart();
        commonPostVerification();
    }

    @Test
    public void targetPasswordChanged_validPassword_targetPasswordSet() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(mockedConfig);

        // Test
        presenter.targetPasswordChanged("targetPassword");

        // Test verification
        verifyStart();
        verify(mockedConfig).getContent();
        verify(mockedContent).getOpenAgencyTarget();
        verify(mockedOpenAgencyTarget).setPassword("targetPassword");
        commonPostVerification();
    }

    @Test
    public void consumerIdChanged_null_noAction() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(null);

        // Test
        presenter.consumerIdChanged("consumerId");

        // Test verification
        verifyStart();
        commonPostVerification();
    }

    @Test
    public void consumerIdChanged_validConsumerId_consumerIdSet() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(mockedConfig);

        // Test
        presenter.consumerIdChanged("consumerId");

        // Test verification
        verifyStart();
        verify(mockedConfig).getContent();
        verify(mockedContent).withConsumerId("consumerId");
        commonPostVerification();
    }


    @Test
    public void sizeChanged_null_noAction() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(null);

        // Test
        presenter.sizeChanged("321");

        // Test verification
        verifyStart();
        commonPostVerification();
    }

    @Test
    public void sizeChanged_validSize_sizeSet() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(mockedConfig);

        // Test
        presenter.sizeChanged("321");

        // Test verification
        verifyStart();
        verify(mockedConfig).getContent();
        verify(mockedContent).withBatchSize(321);
        commonPostVerification();
    }

    @Test
    public void formatOverrideAdded_nullConfig_errorMessage() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(null);

        // Test
        String error = presenter.formatOverrideAdded("overrideKey", "overrideValue");

        // Test verification
        verifyStart();
        verify(mockedTexts).error_InputFieldValidationError();
        assertThat(error, is("InputFieldValidationError"));
        commonPostVerification();
    }

    @Test
    public void formatOverrideAdded_nullKeyData_errorMessage() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(mockedConfig);

        // Test
        String error = presenter.formatOverrideAdded(null, "overrideValue");

        // Test verification
        verifyStart();
        verify(mockedTexts).error_InputFieldValidationError();
        assertThat(error, is("InputFieldValidationError"));
        commonPostVerification();
    }

    @Test
    public void formatOverrideAdded_nullValueData_errorMessage() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(mockedConfig);

        // Test
        String error = presenter.formatOverrideAdded("overrideKey", null);

        // Test verification
        verifyStart();
        verify(mockedTexts).error_InputFieldValidationError();
        assertThat(error, is("InputFieldValidationError"));
        commonPostVerification();
    }

    @Test
    public void formatOverrideAdded_emptyKeyData_errorMessage() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(mockedConfig);

        // Test
        String error = presenter.formatOverrideAdded("", "overrideValue");

        // Test verification
        verifyStart();
        verify(mockedTexts).error_InputFieldValidationError();
        assertThat(error, is("InputFieldValidationError"));
        commonPostVerification();
    }

    @Test
    public void formatOverrideAdded_emptyValueData_errorMessage() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(mockedConfig);

        // Test
        String error = presenter.formatOverrideAdded("overrideKey", "");

        // Test verification
        verifyStart();
        verify(mockedTexts).error_InputFieldValidationError();
        assertThat(error, is("InputFieldValidationError"));
        commonPostVerification();
    }

    @Test
    public void formatOverrideAdded_invalidNumber_overridesAdded() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(mockedConfig);

        // Test
        String error = presenter.formatOverrideAdded("2x34", "overrideValue");

        // Test verification
        verifyStart();
        verify(mockedConfig).getContent();
        verify(mockedTexts).error_NumericSubmitterValidationError();
        assertThat(error, is("NumericSubmitterValidationError"));
        commonPostVerification();
    }

    @Test
    public void formatOverrideAdded_validData_overridesAdded() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(mockedConfig);

        // Test
        String error = presenter.formatOverrideAdded("234", "overrideValue");

        // Test verification
        verifyStart();
        verify(mockedConfig).getContent();
        verify(mockedContent).withFormatOverridesEntry(234, "overrideValue");
        assertThat(error, is(nullValue()));
        commonPostVerification();
    }

    @Test
    public void relationsChanged_null_noAction() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(null);

        // Test
        presenter.relationsChanged(true);

        // Test verification
        verifyStart();
        commonPostVerification();
    }

    @Test
    public void relationsChanged_validRelations_relationsSet() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(mockedConfig);

        // Test
        presenter.relationsChanged(true);

        // Test verification
        verifyStart();
        verify(mockedConfig).getContent();
        verify(mockedContent).withIncludeRelations(true);
        commonPostVerification();
    }

    @Test
    public void destinationChanged_null_noAction() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(null);

        // Test
        presenter.destinationChanged("destination");

        // Test verification
        verifyStart();
        commonPostVerification();
    }

    @Test
    public void destinationChanged_validDestination_destinationSet() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(mockedConfig);

        // Test
        presenter.destinationChanged("destination");

        // Test verification
        verifyStart();
        verify(mockedConfig).getContent();
        verify(mockedContent).withDestination("destination");
        commonPostVerification();
    }

    @Test
    public void formatChanged_null_noAction() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(null);

        // Test
        presenter.formatChanged("format");

        // Test verification
        verifyStart();
        commonPostVerification();
    }

    @Test
    public void formatChanged_validFormat_formatSet() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(mockedConfig);

        // Test
        presenter.formatChanged("format");

        // Test verification
        verifyStart();
        verify(mockedConfig).getContent();
        verify(mockedContent).withFormat("format");
        commonPostVerification();
    }

    @Test
    public void typeChanged_null_noAction() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(null);

        // Test
        presenter.typeChanged("type");

        // Test verification
        verifyStart();
        commonPostVerification();
    }

    @Test(expected = IllegalArgumentException.class)
    public void typeChanged_invalidType_exception() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(mockedConfig);

        // Test
        presenter.typeChanged("INVALID TYPE");
    }

    @Test
    public void typeChanged_validFormat_typeSet() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(mockedConfig);

        // Test
        presenter.typeChanged("TEST");

        // Test verification
        verifyStart();
        verify(mockedConfig).getContent();
        verify(mockedContent).withType(JobSpecification.Type.TEST);
        commonPostVerification();
    }

    @Test
    public void enabledChanged_null_noAction() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(null);

        // Test
        presenter.enabledChanged(true);

        // Test verification
        verifyStart();
        commonPostVerification();
    }

    @Test
    public void enabledChanged_validEnabled_enabledSet() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(mockedConfig);

        // Test
        presenter.enabledChanged(true);

        // Test verification
        verifyStart();
        verify(mockedConfig).getContent();
        verify(mockedContent).withEnabled(true);
        commonPostVerification();
    }

    @Test
    public void keyPressed_null_noAction() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(null);

        // Test
        presenter.keyPressed();

        // Test verification
        verifyStart();
        commonPostVerification();
    }

    @Test
    public void keyPressed_valid_StatusTextCleared() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(mockedConfig);

        // Test
        presenter.keyPressed();

        // Test verification
        verifyStart(2, false);  // With status cleared twice (also upon initialization)
        commonPostVerification();
    }

    @Test
    public void saveButtonPressed_configNull_errorMessageDisplayed() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(null);

        // Test
        presenter.saveButtonPressed();

        // Test verification
        verifyStart();
        verify(mockedTexts).error_InputFieldValidationError();
        verify(mockedView).setErrorText("InputFieldValidationError");
        commonPostVerification();
    }

    @Test
    public void saveButtonPressed_configContentNull_errorMessageDisplayed() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(mockedConfig);
        when(mockedConfig.getContent()).thenReturn(null);

        // Test
        presenter.saveButtonPressed();

        // Test verification
        verifyStart();
        verify(mockedConfig).getContent();
        verify(mockedTexts).error_InputFieldValidationError();
        verify(mockedView).setErrorText("InputFieldValidationError");
        commonPostVerification();
    }

    @Test
    public void saveButtonPressed_configIdNull_errorMessageDisplayed() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(mockedConfig);
        when(mockedContent.getId()).thenReturn(null);

        // Test
        presenter.saveButtonPressed();

        // Test verification
        verifyStart();
        verify(mockedConfig, times(2)).getContent();
        verify(mockedContent).getId();
        verify(mockedTexts).error_InputFieldValidationError();
        verify(mockedView).setErrorText("InputFieldValidationError");
        commonPostVerification();
    }

    @Test
    public void saveButtonPressed_configIdEmpty_errorMessageDisplayed() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(mockedConfig);
        when(mockedContent.getId()).thenReturn("");

        // Test
        presenter.saveButtonPressed();

        // Test verification
        verifyStart();
        verify(mockedConfig, times(3)).getContent();
        verify(mockedContent, times(2)).getId();
        verify(mockedTexts).error_InputFieldValidationError();
        verify(mockedView).setErrorText("InputFieldValidationError");
        commonPostVerification();
    }

    @Test
    public void saveButtonPressed_configIdValidAndNonEmpty_errorMessageDisplayed() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(mockedConfig);
        when(mockedContent.getId()).thenReturn("123");

        // Test
        presenter.saveButtonPressed();

        // Test verification
        verifyStart();
        verify(mockedConfig, times(4)).getContent();
        verify(mockedContent, times(2)).getId();
        verify(mockedContent).getResource();
        verify(mockedTexts).error_InputFieldValidationError();
        verify(mockedView).setErrorText("InputFieldValidationError");
        commonPostVerification();
    }

    @Test
    public void saveButtonPressed_configNonEmpty_errorMessageDisplayed() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(mockedConfig);
        when(mockedContent.getId()).thenReturn("Id");
        when(mockedContent.getResource()).thenReturn("Resource");
        when(mockedOpenAgencyTarget.getUrl()).thenReturn("Url");
        when(mockedContent.getConsumerId()).thenReturn("ConsumerId");
        when(mockedContent.getDestination()).thenReturn("Destination");
        when(mockedContent.getFormat()).thenReturn("Format");

        // Test
        presenter.saveButtonPressed();

        // Test verification
        verifyStart(1, true);
        verify(mockedConfig, times(14)).getContent();
        verify(mockedContent, times(2)).getId();
        verify(mockedContent, times(2)).getResource();
        verify(mockedContent, times(3)).getOpenAgencyTarget();
        verify(mockedOpenAgencyTarget, times(2)).getUrl();
        verify(mockedContent, times(2)).getConsumerId();
        verify(mockedContent, times(2)).getDestination();
        verify(mockedContent, times(2)).getFormat();
        commonPostVerification();
    }

    @Test
    public void updateButtonPressed_configNonEmpty_errorMessageDisplayed() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(mockedConfig);
        when(mockedConfig.getId()).thenReturn(11L);
        when(mockedConfig.getVersion()).thenReturn(22L);
        when(mockedContent.getId()).thenReturn("1");
        when(mockedContent.getResource()).thenReturn("Resource");
        when(mockedOpenAgencyTarget.getUrl()).thenReturn("Url");
        when(mockedContent.getConsumerId()).thenReturn("ConsumerId");
        when(mockedContent.getDestination()).thenReturn("Destination");
        when(mockedContent.getFormat()).thenReturn("Format");

        // Test
        presenter.updateButtonPressed();

        // Test verification
        verifyStart(1, true);
        verify(mockedConfig).getId();
        verify(mockedConfig).getVersion();
        verify(mockedConfig, times(1)).getContent();  // Please note, that a new (non-mocked) config is instantiated with the old (mocked) content - therefore only one call is made here...
        verify(mockedContent, times(2)).getId();
        verify(mockedContent, times(2)).getResource();
        verify(mockedContent, times(3)).getOpenAgencyTarget();
        verify(mockedOpenAgencyTarget, times(2)).getUrl();
        verify(mockedContent, times(2)).getConsumerId();
        verify(mockedContent, times(2)).getDestination();
        verify(mockedContent, times(2)).getFormat();
        commonPostVerification();
    }

    @Test
    public void formatOverridesAddButtonPressed_configNull_errorMessageDisplayed() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(null);

        // Test
        presenter.formatOverridesAddButtonPressed();

        // Test verification
        verifyStart();
        commonPostVerification();
    }

    @Test
    public void formatOverridesAddButtonPressed_configNonEmpty_errorMessageDisplayed() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(mockedConfig);

        // Test
        presenter.formatOverridesAddButtonPressed();

        // Test verification
        verifyStart();
        verify(mockedPopupFormatOverrideEntry).show();
        commonPostVerification();
    }

    @Test
    public void formatOverridesRemoveButtonPressed_configNull_errorMessageDisplayed() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(null);

        // Test
        presenter.formatOverridesRemoveButtonPressed("item");

        // Test verification
        verifyStart();
        commonPostVerification();
    }

    @Test(expected = NumberFormatException.class)
    public void formatOverridesRemoveButtonPressed_nullItem_errorMessageDisplayed() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(mockedConfig);

        // Test
        presenter.formatOverridesRemoveButtonPressed(null);
    }

    @Test(expected = NumberFormatException.class)
    public void formatOverridesRemoveButtonPressed_emptyItem_errorMessageDisplayed() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(mockedConfig);

        // Test
        presenter.formatOverridesRemoveButtonPressed("");

        // Test verification
        verifyStart();
        commonPostVerification();
    }

    @Test
    public void formatOverridesRemoveButtonPressed_validItem_errorMessageDisplayed() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(mockedConfig);
        when(mockedContent.getFormatOverrides()).thenReturn(mockedMap);

        // Test
        presenter.formatOverridesRemoveButtonPressed("123");

        // Test verification
        verifyStart();
        verify(mockedConfig).getContent();
        verify(mockedContent).getFormatOverrides();
        verify(mockedMap).remove(123);

        commonPostVerification();
    }


    /*
     * Private methods
     */

    private void verifyStart(int statusCount, Boolean saveModelCalled) {
        verifyInitializeViewMocks();
        verifyInitializeViewFields(statusCount);
        verify(mockedContainerWidget).setWidget(mockedWidget);
        assertThat(initializeModelCalled, is(true));
        assertThat(saveModelCalled, is(saveModelCalled));
    }

    private void verifyStart() {
        verifyStart(1, false);
    }

    private void verifyInitializeViewMocks() {
        verify(mockedView).setHeader("Header Text");
        verify(mockedView).setPresenter(presenter);
    }

    private void verifyInitializeViewFields(int statusCount) {
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
        verify(mockedStatus, times(statusCount)).setText("");
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
    }


}