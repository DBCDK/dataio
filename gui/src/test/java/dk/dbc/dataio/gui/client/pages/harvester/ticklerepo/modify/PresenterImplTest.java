package dk.dbc.dataio.gui.client.pages.harvester.ticklerepo.modify;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.gui.client.components.prompted.PromptedCheckBox;
import dk.dbc.dataio.gui.client.components.prompted.PromptedList;
import dk.dbc.dataio.gui.client.components.prompted.PromptedTextArea;
import dk.dbc.dataio.gui.client.components.prompted.PromptedTextBox;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
    private PromptedTextBox mockedId;
    @Mock
    private PromptedTextBox mockedName;
    @Mock
    private PromptedTextArea mockedDescription;
    @Mock
    private PromptedTextBox mockedDestination;
    @Mock
    private PromptedTextBox mockedFormat;
    @Mock
    private PromptedList mockedType;
    @Mock
    private PromptedCheckBox mockedEnabled;
    @Mock
    private PromptedCheckBox mockedNotificationsEnabled;
    @Mock
    private Label mockedStatus;
    @Mock
    private Widget mockedWidget;
    @Mock
    private TickleRepoHarvesterConfig mockedConfig;
    @Mock
    private TickleRepoHarvesterConfig.Content mockedContent;

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

        @Override
        public void taskRecordHarvestButtonPressed() {
        }

        @Override
        public void deleteOutdatedRecordsButtonPressed() {
        }

        @Override
        public void deleteOutdatedRecords() {
        }

        @Override
        public void deleteButtonPressed() {
        }

        @Override
        public void setRecordHarvestCount() {
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
        mockedView.id = mockedId;
        mockedView.name = mockedName;
        mockedView.description = mockedDescription;
        mockedView.destination = mockedDestination;
        mockedView.format = mockedFormat;
        mockedView.type = mockedType;
        mockedView.enabled = mockedEnabled;
        mockedView.notificationsEnabled = mockedNotificationsEnabled;
        mockedView.status = mockedStatus;
        when(mockedView.asWidget()).thenReturn(mockedWidget);
        when(mockedConfig.getContent()).thenReturn(mockedContent);
    }

    @Before
    public void prepareTexts() {
        when(mockedTexts.error_InputFieldValidationError()).thenReturn("InputFieldValidationError");
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
    public void idChanged_null_noAction() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setTickleRepoHarvesterConfig(null);

        // Test
        presenter.idChanged("id");

        // Test verification
        verifyStart();
        commonPostVerification();
    }

    @Test
    public void idChanged_validId_idSet() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setTickleRepoHarvesterConfig(mockedConfig);

        // Test
        presenter.idChanged("id");

        // Test verification
        verifyStart();
        verify(mockedConfig).getContent();
        verify(mockedContent).withId("id");
        commonPostVerification();
    }

    @Test
    public void nameChanged_null_noAction() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setTickleRepoHarvesterConfig(null);

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
        presenter.setTickleRepoHarvesterConfig(mockedConfig);

        // Test
        presenter.nameChanged("name");

        // Test verification
        verifyStart();
        verify(mockedConfig).getContent();
        verify(mockedContent).withDatasetName("name");
        commonPostVerification();
    }

    @Test
    public void descriptionChanged_null_noAction() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setTickleRepoHarvesterConfig(null);

        // Test
        presenter.descriptionChanged("description");

        // Test verification
        verifyStart();
        commonPostVerification();
    }

    @Test
    public void descriptionChanged_validDescription_descriptionSet() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setTickleRepoHarvesterConfig(mockedConfig);

        // Test
        presenter.descriptionChanged("description");

        // Test verification
        verifyStart();
        verify(mockedConfig).getContent();
        verify(mockedContent).withDescription("description");
        commonPostVerification();
    }

    @Test
    public void destinationChanged_null_noAction() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setTickleRepoHarvesterConfig(null);

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
        presenter.setTickleRepoHarvesterConfig(mockedConfig);

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
        presenter.setTickleRepoHarvesterConfig(null);

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
        presenter.setTickleRepoHarvesterConfig(mockedConfig);

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
        presenter.setTickleRepoHarvesterConfig(null);

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
        presenter.setTickleRepoHarvesterConfig(mockedConfig);

        // Test
        presenter.typeChanged("INVALID TYPE");
    }

    @Test
    public void typeChanged_validType_typeSet() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setTickleRepoHarvesterConfig(mockedConfig);

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
        presenter.setTickleRepoHarvesterConfig(null);

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
        presenter.setTickleRepoHarvesterConfig(mockedConfig);

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
        presenter.setTickleRepoHarvesterConfig(null);

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
        presenter.setTickleRepoHarvesterConfig(mockedConfig);

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
        presenter.setTickleRepoHarvesterConfig(null);

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
        presenter.setTickleRepoHarvesterConfig(mockedConfig);
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
        presenter.setTickleRepoHarvesterConfig(mockedConfig);
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
        presenter.setTickleRepoHarvesterConfig(mockedConfig);
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
        presenter.setTickleRepoHarvesterConfig(mockedConfig);
        when(mockedContent.getId()).thenReturn("123");

        // Test
        presenter.saveButtonPressed();

        // Test verification
        verifyStart();
        verify(mockedConfig, times(4)).getContent();
        verify(mockedContent, times(2)).getId();
        verify(mockedTexts).error_InputFieldValidationError();
        verify(mockedView).setErrorText("InputFieldValidationError");
        commonPostVerification();
    }

    @Test
    public void saveButtonPressed_configNonEmpty_errorMessageDisplayed() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setTickleRepoHarvesterConfig(mockedConfig);
        when(mockedContent.getId()).thenReturn("Id");
        when(mockedContent.getDatasetName()).thenReturn("Name");
        when(mockedContent.getDescription()).thenReturn("Description");
        when(mockedContent.getDestination()).thenReturn("Destination");
        when(mockedContent.getFormat()).thenReturn("Format");
        when(mockedContent.getType()).thenReturn(JobSpecification.Type.ACCTEST);
        when(mockedContent.isEnabled()).thenReturn(true);

        // Test
        presenter.saveButtonPressed();

        // Test verification
        verifyStart(1, true);
        verify(mockedConfig, times(13)).getContent();
        verify(mockedContent, times(2)).getId();
        verify(mockedContent, times(2)).getDatasetName();
        verify(mockedContent, times(2)).getDescription();
        verify(mockedContent, times(2)).getDestination();
        verify(mockedContent, times(2)).getFormat();
        verify(mockedContent, times(2)).getType();
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
        assertThat(this.saveModelCalled, is(saveModelCalled));
    }

    private void verifyStart() {
        verifyStart(1, false);
    }

    private void verifyInitializeViewMocks() {
        verify(mockedView).setHeader("Header Text");
        verify(mockedView).setPresenter(presenter);
    }

    private void verifyInitializeViewFields(int statusCount) {
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
        verify(mockedNotificationsEnabled).setValue(false);
        verify(mockedNotificationsEnabled).setEnabled(false);
        verify(mockedStatus, times(statusCount)).setText("");
    }

    private void verifyType(PromptedList list, String value, boolean enabled) {
        verify(list).clear();
        verify(list).addAvailableItem("TRANSIENT");
        verify(list).addAvailableItem("PERSISTENT");
        verify(list).addAvailableItem("TEST");
        verify(list).addAvailableItem("ACCTEST");
        verify(list).addAvailableItem("INFOMEDIA");
        verify(list).addAvailableItem("PERIODIC");
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
        verifyNoMoreInteractions(mockedNotificationsEnabled);
        verifyNoMoreInteractions(mockedStatus);
        verifyNoMoreInteractions(mockedFlowStore);
        verifyNoMoreInteractions(mockedProxyErrorTexts);
    }

}
