package dk.dbc.dataio.gui.client.pages.harvester.rr.modify;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.gui.client.components.popup.PopupMapEntry;
import dk.dbc.dataio.gui.client.components.prompted.PromptedCheckBox;
import dk.dbc.dataio.gui.client.components.prompted.PromptedList;
import dk.dbc.dataio.gui.client.components.prompted.PromptedMultiList;
import dk.dbc.dataio.gui.client.components.prompted.PromptedPasswordTextBox;
import dk.dbc.dataio.gui.client.components.prompted.PromptedTextArea;
import dk.dbc.dataio.gui.client.components.prompted.PromptedTextBox;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.HashMap;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * PresenterImpl unit tests
 * <p>
 * The test methods of this class uses the following naming convention:
 * <p>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class PresenterImplTest extends PresenterImplTestBase {

    /*
     * Mocks
     */
    @Mock
    private Texts mockedTexts;
    @Mock
    private View mockedView;
    @Mock
    private PromptedTextBox mockedName;
    @Mock
    private PromptedTextBox mockedDescription;
    @Mock
    private PromptedTextBox mockedResource;
    @Mock
    private PromptedTextBox mockedTargetUrl;
    @Mock
    private PromptedTextBox mockedTargetGroup;
    @Mock
    private PromptedTextBox mockedTargetUser;
    @Mock
    private PromptedPasswordTextBox mockedTargetPassword;
    @Mock
    private PromptedTextBox mockedConsumerId;
    @Mock
    private PromptedTextBox mockedSize;
    @Mock
    private PromptedMultiList mockedFormatOverrides;
    @Mock
    private PromptedCheckBox mockedRelations;
    @Mock
    private PromptedCheckBox mockedExpand;
    @Mock
    private PromptedCheckBox mockedLibraryRules;
    @Mock
    private PromptedList mockedImsHarvester;
    @Mock
    private PromptedTextBox mockedImsHoldingsTarget;
    @Mock
    private PromptedTextBox mockedDestination;
    @Mock
    private PromptedTextBox mockedFormat;
    @Mock
    private PromptedList mockedType;
    @Mock
    private PromptedTextArea mockedNote;
    @Mock
    private PromptedCheckBox mockedEnabled;
    @Mock
    private Button mockedUpdateButton;
    @Mock
    private Label mockedStatus;
    @Mock
    private PopupMapEntry mockedPopupFormatOverrideEntry;
    @Mock
    private Widget mockedWidget;
    private RRHarvesterConfig.Content content;
    private RRHarvesterConfig config;


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

        @Override
        public void deleteButtonPressed() {
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
        mockedView.description = mockedDescription;
        mockedView.resource = mockedResource;
        mockedView.consumerId = mockedConsumerId;
        mockedView.size = mockedSize;
        mockedView.formatOverrides = mockedFormatOverrides;
        mockedView.relations = mockedRelations;
        mockedView.expand = mockedExpand;
        mockedView.libraryRules = mockedLibraryRules;
        mockedView.harvesterType = mockedImsHarvester;
        mockedView.holdingsTarget = mockedImsHoldingsTarget;
        mockedView.destination = mockedDestination;
        mockedView.format = mockedFormat;
        mockedView.type = mockedType;
        mockedView.note = mockedNote;
        mockedView.enabled = mockedEnabled;
        mockedView.updateButton = mockedUpdateButton;
        mockedView.status = mockedStatus;
        mockedView.popupFormatOverrideEntry = mockedPopupFormatOverrideEntry;
        when(mockedView.asWidget()).thenReturn(mockedWidget);
    }

    @Before
    public void prepareTexts() {
        when(mockedTexts.error_InputFieldValidationError()).thenReturn("InputFieldValidationError");
        when(mockedTexts.error_NumericSubmitterValidationError()).thenReturn("NumericSubmitterValidationError");
    }

    @Before
    public void setUpConfig() {
        content = new RRHarvesterConfig.Content()
                .withId("id")
                .withDescription("description")
                .withResource("resource")
                .withConsumerId("consumerId")
                .withIncludeRelations(false)
                .withIncludeLibraryRules(false)
                .withDestination("destination")
                .withFormat("format")
                .withFormatOverrides(new HashMap<>());

        config = new RRHarvesterConfig(1, 1, content);
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
    }

    @Test
    public void nameChanged_null_noAction() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(null);

        // Test
        try {
            presenter.nameChanged("name");
        } catch (NullPointerException e) {
            fail("Exception attempting to set values on null valued model");
        }
    }

    @Test
    public void nameChanged_validName_nameSet() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(config);

        // Test
        presenter.nameChanged("name");

        // Test verification
        assertThat(content.getId(), is("name"));
    }

    @Test
    public void descriptionChanged_null_noAction() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(null);

        // Test
        try {
            presenter.descriptionChanged("description");
        } catch (NullPointerException e) {
            fail("Exception attempting to set values on null valued model");
        }
    }

    @Test
    public void descriptionChanged_validDescription_descriptionSet() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(config);

        // Test
        presenter.descriptionChanged("description");

        // Test verification
        assertThat(content.getDescription(), is("description"));
    }

    @Test
    public void resourceChanged_null_noAction() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(null);

        // Test
        try {
            presenter.resourceChanged("resource");
        } catch (NullPointerException e) {
            fail("Exception attempting to set values on null valued model");
        }
    }

    @Test
    public void resourceChanged_validResource_resourceSet() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(config);

        // Test
        presenter.resourceChanged("resource");

        // Test verification
        assertThat(content.getResource(), is("resource"));
    }

    @Test
    public void consumerIdChanged_null_noAction() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(null);

        // Test
        try {
            presenter.consumerIdChanged("consumerId");
        } catch (NullPointerException e) {
            fail("Exception attempting to set values on null valued model");
        }
    }

    @Test
    public void consumerIdChanged_validConsumerId_consumerIdSet() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(config);

        // Test
        presenter.consumerIdChanged("consumerId");

        // Test verification
        assertThat(content.getConsumerId(), is("consumerId"));
    }


    @Test
    public void sizeChanged_null_noAction() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(null);

        // Test
        try {
            presenter.sizeChanged("321");
        } catch (NullPointerException e) {
            fail("Exception attempting to set values on null valued model");
        }
    }

    @Test
    public void sizeChanged_validSize_sizeSet() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(config);

        // Test
        presenter.sizeChanged("321");

        // Test verification
        assertThat(content.getBatchSize(), is(321));
    }

    @Test
    public void formatOverrideAdded_nullConfig_errorMessage() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(null);

        // Test
        String error = presenter.formatOverrideAdded("overrideKey", "overrideValue");

        // Test verification
        verify(mockedTexts).error_InputFieldValidationError();
        assertThat(error, is("InputFieldValidationError"));
    }

    @Test
    public void formatOverrideAdded_nullKeyData_errorMessage() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(config);

        // Test
        String error = presenter.formatOverrideAdded(null, "overrideValue");

        // Test verification
        verify(mockedTexts).error_InputFieldValidationError();
        assertThat(error, is("InputFieldValidationError"));
    }

    @Test
    public void formatOverrideAdded_nullValueData_errorMessage() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(config);

        // Test
        String error = presenter.formatOverrideAdded("overrideKey", null);

        // Test verification
        verify(mockedTexts).error_InputFieldValidationError();
        assertThat(error, is("InputFieldValidationError"));
    }

    @Test
    public void formatOverrideAdded_emptyKeyData_errorMessage() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(config);

        // Test
        String error = presenter.formatOverrideAdded("", "overrideValue");

        // Test verification
        verify(mockedTexts).error_InputFieldValidationError();
        assertThat(error, is("InputFieldValidationError"));
    }

    @Test
    public void formatOverrideAdded_emptyValueData_errorMessage() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(config);

        // Test
        String error = presenter.formatOverrideAdded("overrideKey", "");

        // Test verification
        verify(mockedTexts).error_InputFieldValidationError();
        assertThat(error, is("InputFieldValidationError"));
    }

    @Test
    public void formatOverrideAdded_invalidNumber_overridesAdded() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(config);

        // Test
        String error = presenter.formatOverrideAdded("2x34", "overrideValue");

        // Test verification
        verify(mockedTexts).error_NumericSubmitterValidationError();
        assertThat(error, is("NumericSubmitterValidationError"));
    }

    @Test
    public void formatOverrideAdded_validData_overridesAdded() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(config);

        // Test
        String error = presenter.formatOverrideAdded("234", "overrideValue");

        // Test verification
        assertThat(content.getFormatOverrides().size(), is(1));
        assertThat(content.getFormatOverrides().get(234), is("overrideValue"));
        assertThat(error, is(nullValue()));
    }

    @Test
    public void relationsChanged_null_noAction() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(null);

        // Test
        try {
            presenter.relationsChanged(true);
        } catch (NullPointerException e) {
            fail("Exception attempting to set values on null valued model");
        }
    }

    @Test
    public void relationsChanged_validRelations_relationsSet() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(config);

        // Test
        presenter.relationsChanged(true);

        // Test verification
        assertThat(content.hasIncludeRelations(), is(true));
    }

    @Test
    public void libraryRulesChanged_null_noAction() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(null);

        // Test
        try {
            presenter.libraryRulesChanged(true);
        } catch (NullPointerException e) {
            fail("Exception attempting to set values on null valued model");
        }
    }

    @Test
    public void libraryRulesChanged_validLibraryRules_libraryRulesSet() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(config);

        // Test
        presenter.libraryRulesChanged(true);

        // Test verification
        assertThat(content.hasIncludeLibraryRules(), is(true));
    }

    @Test
    public void imsHarvesterChanged_null_noAction() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(null);

        // Test
        try {
            presenter.harvesterTypeChanged(RRHarvesterConfig.HarvesterType.STANDARD.toString());
        } catch (NullPointerException e) {
            fail("Exception attempting to set values on null valued model");
        }
    }

    @Test
    public void imsHarvesterChanged_validHarvester_imsHarvesterSet() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(config);

        // Test
        presenter.harvesterTypeChanged(RRHarvesterConfig.HarvesterType.IMS.toString());

        // Test verification
        assertThat(content.getHarvesterType(), is(RRHarvesterConfig.HarvesterType.IMS));
    }

    @Test
    public void imsHoldingsTargetChanged_null_noAction() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(null);

        // Test
        try {
            presenter.holdingsTargetChanged("holdingsTarget");
        } catch (NullPointerException e) {
            fail("Exception attempting to set values on null valued model");
        }
    }

    @Test
    public void imsHoldingsTargetChanged_validDestination_destinationSet() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(config);

        // Test
        presenter.holdingsTargetChanged("holdingsTarget");

        // Test verification
        assertThat(content.getImsHoldingsTarget(), is("holdingsTarget"));
    }

    @Test
    public void destinationChanged_null_noAction() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(null);

        // Test
        try {
            presenter.destinationChanged("destination");
        } catch (NullPointerException e) {
            fail("Exception attempting to set values on null valued model");
        }
    }

    @Test
    public void destinationChanged_validDestination_destinationSet() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(config);

        // Test
        presenter.destinationChanged("destination");

        // Test verification
        assertThat(content.getDestination(), is("destination"));
    }

    @Test
    public void formatChanged_null_noAction() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(null);

        // Test
        try {
            presenter.formatChanged("format");
        } catch (NullPointerException e) {
            fail("Exception attempting to set values on null valued model");
        }
    }

    @Test
    public void formatChanged_validFormat_formatSet() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(config);

        // Test
        presenter.formatChanged("format");

        // Test verification
        assertThat(presenter.model.getContent().getFormat(), is("format"));
    }

    @Test
    public void typeChanged_null_noAction() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(null);

        // Test
        try {
            presenter.typeChanged("type");
        } catch (NullPointerException e) {
            fail("Exception attempting to set values on null valued model");
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void typeChanged_invalidType_exception() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(config);

        // Test
        presenter.typeChanged("INVALID TYPE");
    }

    @Test
    public void typeChanged_validType_typeSet() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(config);

        // Test
        presenter.typeChanged("TEST");

        // Test verification
        assertThat(content.getType(), is(JobSpecification.Type.TEST));
    }

    @Test
    public void noteChanged_null_noAction() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(null);

        // Test
        try {
            presenter.noteChanged("note");
        } catch (NullPointerException e) {
            fail("Exception attempting to set values on null valued model");
        }
    }

    @Test
    public void noteChanged_validNote_noteSet() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(config);

        // Test
        presenter.noteChanged("note");

        // Test verification
        assertThat(content.getNote(), is("note"));
    }

    @Test
    public void enabledChanged_null_noAction() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(null);

        // Test
        try {
            presenter.enabledChanged(true);
        } catch (NullPointerException e) {
            fail("Exception attempting to set values on null valued model");
        }
    }

    @Test
    public void enabledChanged_validEnabled_enabledSet() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(config);

        // Test
        presenter.enabledChanged(true);

        // Test verification
        assertThat(content.isEnabled(), is(true));
    }

    @Test
    public void keyPressed_null_noAction() {
        // Test preparation
        presenter.setRRHarvesterConfig(null);

        // Test
        presenter.keyPressed();

        // Test verification
        verifyNoInteractions(mockedView.status);
    }

    @Test
    public void keyPressed_valid_StatusTextCleared() {
        // Test preparation
        presenter.setRRHarvesterConfig(config);

        // Test
        presenter.keyPressed();

        // Test verification
        verify(mockedView.status, times(1)).setText("");
    }

    @Test
    public void saveButtonPressed_configNull_errorMessageDisplayed() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(null);

        // Test
        presenter.saveButtonPressed();

        // Test verification
        verify(mockedTexts).error_InputFieldValidationError();
        verify(mockedView).setErrorText("InputFieldValidationError");
    }

    @Test
    public void saveButtonPressed_configIdNull_errorMessageDisplayed() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        content.withId(null);
        presenter.setRRHarvesterConfig(config);

        // Test
        presenter.saveButtonPressed();

        // Test verification
        verify(mockedTexts).error_InputFieldValidationError();
        verify(mockedView).setErrorText("InputFieldValidationError");
    }

    @Test
    public void saveButtonPressed_configIdEmpty_errorMessageDisplayed() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        content.withId("");
        presenter.setRRHarvesterConfig(config);

        // Test
        presenter.saveButtonPressed();

        // Test verification
        verify(mockedTexts).error_InputFieldValidationError();
        verify(mockedView).setErrorText("InputFieldValidationError");
    }

    @Test
    public void saveButtonPressed_configNonEmpty_ok() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(config);

        // Test
        presenter.saveButtonPressed();

        // Test verification
        assertThat(this.saveModelCalled, is(true));
        verify(mockedView, times(0)).setErrorText(mockedTexts.error_InputFieldValidationError());
    }

    @Test
    public void updateButtonPressed_configNonEmpty_ok() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(config);

        // Test
        presenter.updateButtonPressed();

        verifyStart();
        verify(mockedView, times(0)).setErrorText(mockedTexts.error_InputFieldValidationError());
    }

    @Test
    public void formatOverridesAddButtonPressed_configNull_ok() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(null);

        // Test
        presenter.formatOverridesAddButtonPressed();

        // Test verification
        verify(mockedView.popupFormatOverrideEntry, times(0)).show();
    }

    @Test
    public void formatOverridesAddButtonPressed_configNonEmpty_ok() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(config);

        // Test
        presenter.formatOverridesAddButtonPressed();

        // Test verification
        verify(mockedPopupFormatOverrideEntry).show();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void formatOverridesRemoveButtonPressed_configNull_ok() {
        // Test preparation
        final HashMap mockedFormatOverrides = mock(HashMap.class);
        content.withFormatOverrides(mockedFormatOverrides);
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(null);

        // Test
        presenter.formatOverridesRemoveButtonPressed("123");

        // Test verification
        verify(mockedFormatOverrides, times(0)).remove(Integer.valueOf("123"));
    }

    @Test(expected = NumberFormatException.class)
    public void formatOverridesRemoveButtonPressed_nullItem_errorMessageDisplayed() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(config);

        // Test
        presenter.formatOverridesRemoveButtonPressed(null);
    }

    @Test(expected = NumberFormatException.class)
    public void formatOverridesRemoveButtonPressed_emptyItem_errorMessageDisplayed() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setRRHarvesterConfig(config);

        // Test
        presenter.formatOverridesRemoveButtonPressed("");
    }

    @Test
    public void formatOverridesRemoveButtonPressed_validItem_errorMessageDisplayed() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        final HashMap<Integer, String> formatOverrides = new HashMap<>();
        formatOverrides.put(123, "format123");
        formatOverrides.put(234, "format234");
        content.withFormatOverrides(formatOverrides);
        presenter.setRRHarvesterConfig(config);

        // Test
        presenter.formatOverridesRemoveButtonPressed("123");

        // Test verification
        assertThat(content.getFormatOverrides().size(), is(1));
        assertThat(content.getFormatOverrides().containsKey(123), is(false));
    }


    /*
     * Private methods
     */

    private void verifyStart() {
        verifyInitializeViewMocks();
        verifyInitializeViewFields();
        verify(mockedContainerWidget).setWidget(mockedWidget);
        assertThat(initializeModelCalled, is(true));
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
        verify(mockedResource).setText("");
        verify(mockedResource).setEnabled(false);
        verify(mockedConsumerId).setText("");
        verify(mockedConsumerId).setEnabled(false);
        verify(mockedSize).setText("");
        verify(mockedSize).setEnabled(false);
        verify(mockedView).setFormatOverrides(new HashMap<>());
        verify(mockedFormatOverrides).setEnabled(false);
        verify(mockedRelations).setValue(false);
        verify(mockedRelations).setEnabled(false);
        verify(mockedLibraryRules).setValue(false);
        verify(mockedLibraryRules).setEnabled(false);
        verify(mockedDestination).setText("");
        verify(mockedDestination).setEnabled(false);
        verify(mockedFormat).setText("");
        verify(mockedFormat).setEnabled(false);
        verifyType(mockedType, "");
        verify(mockedType).setEnabled(false);
        verify(mockedNote).setText("");
        verify(mockedNote).setEnabled(false);
        verify(mockedEnabled).setValue(false);
        verify(mockedEnabled).setEnabled(false);
        verify(mockedStatus, times(1)).setText("");
        verify(mockedUpdateButton).setVisible(false);
        verify(mockedPopupFormatOverrideEntry).hide();
    }

    private void verifyType(PromptedList list, String value) {
        verify(list).clear();
        verify(list).addAvailableItem("TRANSIENT");
        verify(list).addAvailableItem("PERSISTENT");
        verify(list).addAvailableItem("TEST");
        verify(list).addAvailableItem("ACCTEST");
        verify(list).addAvailableItem("INFOMEDIA");
        verify(list).addAvailableItem("PERIODIC");
        verify(list).setSelectedValue(value);
        verify(list).setEnabled(false);
    }

    private void commonPostVerification() {
        verifyNoMoreInteractions(mockedTexts);
        verifyNoMoreInteractions(presenter.commonInjector);
        verifyNoMoreInteractions(mockedContainerWidget);
        verifyNoMoreInteractions(mockedEventBus);
        verifyNoMoreInteractions(mockedName);
        verifyNoMoreInteractions(mockedDescription);
        verifyNoMoreInteractions(mockedResource);
        verifyNoMoreInteractions(mockedTargetUrl);
        verifyNoMoreInteractions(mockedTargetGroup);
        verifyNoMoreInteractions(mockedTargetUser);
        verifyNoMoreInteractions(mockedTargetPassword);
        verifyNoMoreInteractions(mockedConsumerId);
        verifyNoMoreInteractions(mockedSize);
        verifyNoMoreInteractions(mockedFormatOverrides);
        verifyNoMoreInteractions(mockedRelations);
        verifyNoMoreInteractions(mockedLibraryRules);
        verifyNoMoreInteractions(mockedDestination);
        verifyNoMoreInteractions(mockedFormat);
        verifyNoMoreInteractions(mockedType);
        verifyNoMoreInteractions(mockedNote);
        verifyNoMoreInteractions(mockedEnabled);
        verifyNoMoreInteractions(mockedUpdateButton);
        verifyNoMoreInteractions(mockedStatus);
        verifyNoMoreInteractions(mockedPopupFormatOverrideEntry);
    }

}
