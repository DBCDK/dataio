package dk.dbc.dataio.gui.client.pages.harvester.corepo.modify;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.components.prompted.PromptedCheckBox;
import dk.dbc.dataio.gui.client.components.prompted.PromptedList;
import dk.dbc.dataio.gui.client.components.prompted.PromptedTextArea;
import dk.dbc.dataio.gui.client.components.prompted.PromptedTextBox;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import dk.dbc.dataio.harvester.types.CoRepoHarvesterConfig;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;

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
    private PromptedTextBox mockedName;
    @Mock
    private PromptedTextArea mockedDescription;
    @Mock
    private PromptedTextBox mockedResource;
    @Mock
    private PromptedList mockedRrHarvester;
    @Mock
    private PromptedCheckBox mockedEnabled;
    @Mock
    private Label mockedStatus;
    @Mock
    private Widget mockedWidget;
    @Mock
    private CoRepoHarvesterConfig mockedConfig;
    @Mock
    private CoRepoHarvesterConfig.Content mockedContent;

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
        public void deleteButtonPressed() {
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
        mockedView.description = mockedDescription;
        mockedView.resource = mockedResource;
        mockedView.rrHarvester = mockedRrHarvester;
        mockedView.enabled = mockedEnabled;
        mockedView.status = mockedStatus;
        when(mockedView.asWidget()).thenReturn(mockedWidget);
        when(mockedConfig.getContent()).thenReturn(mockedContent);
        List<RRHarvesterConfig> rrConfigs = new ArrayList<>();
        rrConfigs.add(new RRHarvesterConfig(11L, 1L, new RRHarvesterConfig.Content().withId("RR1")));
        rrConfigs.add(new RRHarvesterConfig(22L, 2L, new RRHarvesterConfig.Content().withId("RR2")));
        rrConfigs.add(new RRHarvesterConfig(33L, 3L, new RRHarvesterConfig.Content().withId("RR3")));
        presenter.setAvailableRrHarvesterConfigs(rrConfigs);
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
    public void nameChanged_null_noAction() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setCoRepoHarvesterConfig(null);

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
        presenter.setCoRepoHarvesterConfig(mockedConfig);

        // Test
        presenter.nameChanged("name");

        // Test verification
        verifyStart();
        verify(mockedConfig).getContent();
        verify(mockedContent).withName("name");
        commonPostVerification();
    }

    @Test
    public void descriptionChanged_null_noAction() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setCoRepoHarvesterConfig(null);

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
        presenter.setCoRepoHarvesterConfig(mockedConfig);

        // Test
        presenter.descriptionChanged("description");

        // Test verification
        verifyStart();
        verify(mockedConfig).getContent();
        verify(mockedContent).withDescription("description");
        commonPostVerification();
    }

    @Test
    public void resourceChanged_null_noAction() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setCoRepoHarvesterConfig(null);

        // Test
        presenter.resourceChanged("resource");

        // Test verification
        verifyStart();
        commonPostVerification();
    }

    @Test
    public void resourceChanged_validDestination_resourceSet() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setCoRepoHarvesterConfig(mockedConfig);

        // Test
        presenter.resourceChanged("resource");

        // Test verification
        verifyStart();
        verify(mockedConfig).getContent();
        verify(mockedContent).withResource("resource");
        commonPostVerification();
    }

    @Test
    public void rrHarvesterChanged_null_noAction() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setCoRepoHarvesterConfig(null);

        // Test
        presenter.rrHarvesterChanged("rrHarvester");

        // Test verification
        verifyStart();
        commonPostVerification();
    }

    @Test
    public void rrHarvesterChanged_validRrHarvester_rrHarvesterSet() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setCoRepoHarvesterConfig(mockedConfig);

        // Test
        presenter.rrHarvesterChanged("123");

        // Test verification
        verifyStart();
        verify(mockedConfig).getContent();
        verify(mockedContent).withRrHarvester(123);
        commonPostVerification();
    }

    @Test
    public void enabledChanged_null_noAction() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setCoRepoHarvesterConfig(null);

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
        presenter.setCoRepoHarvesterConfig(mockedConfig);

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
        presenter.setCoRepoHarvesterConfig(null);

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
        presenter.setCoRepoHarvesterConfig(mockedConfig);

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
        presenter.setCoRepoHarvesterConfig(null);

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
        presenter.setCoRepoHarvesterConfig(mockedConfig);
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
        presenter.setCoRepoHarvesterConfig(mockedConfig);
        when(mockedContent.getName()).thenReturn(null);

        // Test
        presenter.saveButtonPressed();

        // Test verification
        verifyStart();
        verify(mockedConfig, times(2)).getContent();
        verify(mockedContent).getName();
        verify(mockedTexts).error_InputFieldValidationError();
        verify(mockedView).setErrorText("InputFieldValidationError");
        commonPostVerification();
    }

    @Test
    public void saveButtonPressed_configIdEmpty_errorMessageDisplayed() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setCoRepoHarvesterConfig(mockedConfig);
        when(mockedContent.getName()).thenReturn("");

        // Test
        presenter.saveButtonPressed();

        // Test verification
        verifyStart();
        verify(mockedConfig, times(3)).getContent();
        verify(mockedContent, times(2)).getName();
        verify(mockedTexts).error_InputFieldValidationError();
        verify(mockedView).setErrorText("InputFieldValidationError");
        commonPostVerification();
    }

    @Test
    public void saveButtonPressed_configIdValidAndNonEmpty_errorMessageDisplayed() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setCoRepoHarvesterConfig(mockedConfig);
        when(mockedContent.getName()).thenReturn("123");

        // Test
        presenter.saveButtonPressed();

        // Test verification
        verifyStart();
        verify(mockedConfig, times(4)).getContent();
        verify(mockedContent, times(2)).getName();
        verify(mockedTexts).error_InputFieldValidationError();
        verify(mockedView).setErrorText("InputFieldValidationError");
        commonPostVerification();
    }

    @Test
    public void saveButtonPressed_configNonEmpty_errorMessageDisplayed() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setCoRepoHarvesterConfig(mockedConfig);
        when(mockedContent.getName()).thenReturn("Name");
        when(mockedContent.getDescription()).thenReturn("Description");
        when(mockedContent.getResource()).thenReturn("Resource");
        when(mockedContent.getRrHarvester()).thenReturn(234L);
        when(mockedContent.isEnabled()).thenReturn(true);

        // Test
        presenter.saveButtonPressed();

        // Test verification
        verifyStart(1, true);
        verify(mockedConfig, times(7)).getContent();
        verify(mockedContent, times(2)).getName();
        verify(mockedContent, times(2)).getDescription();
        verify(mockedContent, times(2)).getResource();
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
        verify(presenter.commonInjector).getFlowStoreProxyAsync();
        verify(mockedName).setText("");
        verify(mockedName).setEnabled(false);
        verify(mockedDescription).setText("");
        verify(mockedDescription).setEnabled(false);
        verify(mockedResource).setText("");
        verify(mockedResource).setEnabled(false);
        verifyRrHarvesters(mockedRrHarvester, "", false);
        verify(mockedRrHarvester).setEnabled(false);
        verify(mockedEnabled).setValue(false);
        verify(mockedEnabled).setEnabled(false);
        verify(mockedStatus, times(statusCount)).setText("");
    }

    private void verifyRrHarvesters(PromptedList list, String value, boolean enabled) {
        verify(list).clear();
        verify(list).addAvailableItem("RR1", "11");
        verify(list).addAvailableItem("RR2", "22");
        verify(list).addAvailableItem("RR3", "33");
        verify(list).setSelectedValue(value);
        verify(list).setEnabled(enabled);
    }

    private void commonPostVerification() {
        verifyNoMoreInteractions(mockedTexts);
        verifyNoMoreInteractions(presenter.commonInjector);
        verifyNoMoreInteractions(mockedContainerWidget);
        verifyNoMoreInteractions(mockedEventBus);
        verifyNoMoreInteractions(mockedName);
        verifyNoMoreInteractions(mockedDescription);
        verifyNoMoreInteractions(mockedResource);
        verifyNoMoreInteractions(mockedRrHarvester);
        verifyNoMoreInteractions(mockedEnabled);
        verifyNoMoreInteractions(mockedStatus);
        verifyNoMoreInteractions(mockedFlowStore);
        verifyNoMoreInteractions(mockedProxyErrorTexts);
    }

}
