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
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import dk.dbc.dataio.harvester.types.UshSolrHarvesterConfig;
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
        when(mockedView.asWidget()).thenReturn(mockedWidget);
        mockedView.name = mockedName;
        mockedView.description = mockedDescription;
        mockedView.submitter = mockedSubmitter;
        mockedView.format = mockedFormat;
        mockedView.destination = mockedDestination;
        mockedView.enabled = mockedEnabled;
        mockedView.saveButton = mockedSaveButton;
        mockedView.status = mockedStatus;
        when(mockedConfig.getContent()).thenReturn(mockedContent);
    }

    @Before
    public void prepareTexts() {
        when(mockedTexts.error_InputFieldValidationError()).thenReturn("InputFieldValidationError");
        when(mockedTexts.error_SubmitterNumberValidationError()).thenReturn("SubmitterNumberValidationError");
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
        presenter.setUshSolrHarvesterConfig(null);

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
        presenter.setUshSolrHarvesterConfig(mockedConfig);

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
        presenter.setUshSolrHarvesterConfig(null);

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
        presenter.setUshSolrHarvesterConfig(mockedConfig);

        // Test
        presenter.descriptionChanged("description");

        // Test verification
        verifyStart();
        verify(mockedConfig).getContent();
        verify(mockedContent).withDescription("description");
        commonPostVerification();
    }

    @Test
    public void submitterChanged_null_noAction() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setUshSolrHarvesterConfig(null);

        // Test
        presenter.submitterChanged("submitter");

        // Test verification
        verifyStart();
        commonPostVerification();
    }

    @Test
    public void submitterChanged_invalidSubmitter_submitterSetToZeroAndError() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setUshSolrHarvesterConfig(mockedConfig);

        // Test
        presenter.submitterChanged("xyz");

        // Test verification
        verifyStart();
        verify(mockedTexts).error_SubmitterNumberValidationError();
        verify(mockedView).setErrorText("SubmitterNumberValidationError");
        verify(mockedConfig, times(2)).getContent();
        commonPostVerification();
    }

    @Test
    public void submitterChanged_validSubmitter_submitterSet() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setUshSolrHarvesterConfig(mockedConfig);

        // Test
        presenter.submitterChanged("123");

        // Test verification
        verifyStart();
        verify(mockedConfig).getContent();
        verify(mockedContent).withSubmitterNumber(123);
        commonPostVerification();
    }

    @Test
    public void formatChanged_null_noAction() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setUshSolrHarvesterConfig(null);

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
        presenter.setUshSolrHarvesterConfig(mockedConfig);

        // Test
        presenter.formatChanged("format");

        // Test verification
        verifyStart();
        verify(mockedConfig).getContent();
        verify(mockedContent).withFormat("format");
        commonPostVerification();
    }

    @Test
    public void destinationChanged_null_noAction() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setUshSolrHarvesterConfig(null);

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
        presenter.setUshSolrHarvesterConfig(mockedConfig);

        // Test
        presenter.destinationChanged("destination");

        // Test verification
        verifyStart();
        verify(mockedConfig).getContent();
        verify(mockedContent).withDestination("destination");
        commonPostVerification();
    }

    @Test
    public void enabledChanged_null_noAction() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setUshSolrHarvesterConfig(null);

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
        presenter.setUshSolrHarvesterConfig(mockedConfig);

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
        presenter.setUshSolrHarvesterConfig(null);

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
        presenter.setUshSolrHarvesterConfig(mockedConfig);

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
        presenter.setUshSolrHarvesterConfig(null);

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
        presenter.setUshSolrHarvesterConfig(mockedConfig);
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
    public void saveButtonPressed_configNameNull_errorMessageDisplayed() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setUshSolrHarvesterConfig(mockedConfig);
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
        presenter.setUshSolrHarvesterConfig(mockedConfig);
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
    public void saveButtonPressed_configNameValidAndNonEmpty_errorMessageDisplayed() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setUshSolrHarvesterConfig(mockedConfig);
        when(mockedContent.getName()).thenReturn("name");

        // Test
        presenter.saveButtonPressed();

        // Test verification
        verifyStart();
        verify(mockedConfig, times(4)).getContent();
        verify(mockedContent, times(2)).getName();
        verify(mockedContent).getDescription();
        verify(mockedTexts).error_InputFieldValidationError();
        verify(mockedView).setErrorText("InputFieldValidationError");
        commonPostVerification();
    }

    @Test
    public void saveButtonPressed_configNonEmpty_errorMessageDisplayed() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);
        presenter.setUshSolrHarvesterConfig(mockedConfig);
        when(mockedContent.getName()).thenReturn("Name");
        when(mockedContent.getDescription()).thenReturn("Description");
        when(mockedContent.getSubmitterNumber()).thenReturn(234);
        when(mockedContent.getFormat()).thenReturn("Format");
        when(mockedContent.getDestination()).thenReturn("Destination");

        // Test
        presenter.saveButtonPressed();

        // Test verification
        verifyStart(1, true);
        verify(mockedConfig, times(11)).getContent();
        verify(mockedContent, times(2)).getName();
        verify(mockedContent, times(2)).getDescription();
        verify(mockedContent, times(2)).getSubmitterNumber();
        verify(mockedContent, times(2)).getFormat();
        verify(mockedContent, times(2)).getDestination();
        commonPostVerification();
    }



    /*
     * Private methods
     */

    private void verifyStart(int statusCount, Boolean saveModelCalled) {
        verifyInitializeViewMocks();
        verifyInitializeViewFields(statusCount);
        verify(mockedView).asWidget();
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
        verify(mockedStatus, times(statusCount)).setText("");
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
