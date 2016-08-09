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

package dk.dbc.dataio.gui.client.pages.sink.modify;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.events.DialogEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;


/**
 * PresenterImpl unit tests
 * <p/>
 * The test methods of this class uses the following naming convention:
 * <p/>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class ViewTest {

    @Mock ViewGinjector mockedViewInjector;
    @Mock Presenter mockedPresenter;
    @Mock ValueChangeEvent mockedValueChangeEvent;
    @Mock ClickEvent mockedClickEvent;
    @Mock DialogEvent mockedDialogEvent;


    // Subject Under Test
    private View view;


    // Testing starts here...
    @Test
    public void constructor_instantiate_objectCorrectInitialized() {
        // Subject Under Test
        view = new View();

        // Test Verification
        verifyNoMoreInteractions(view.sinkTypeSelection);
        verifyNoMoreInteractions(view.name);
        verifyNoMoreInteractions(view.resource);
        verifyNoMoreInteractions(view.description);
        verifyNoMoreInteractions(view.updateSinkSection);
        verifyNoMoreInteractions(view.url);
        verifyNoMoreInteractions(view.userid);
        verifyNoMoreInteractions(view.password);
        verifyNoMoreInteractions(view.queueProviders);
//        verifyNoMoreInteractions(view.sequenceAnalysisOptionAllButton);
//        verifyNoMoreInteractions(view.sequenceAnalysisOptionIdOnlyButton);
        verifyNoMoreInteractions(view.deleteButton);
        verifyNoMoreInteractions(view.status);
        verifyNoMoreInteractions(view.popupTextBox);
    }

    @Test(expected = IllegalArgumentException.class)
    public void sinkTypeSelectionChanged_unknownSinkType_exception() {
        // Test preparation
        setupView();
        when(view.sinkTypeSelection.getSelectedKey()).thenReturn("UNKNOWN TYPE");

        // Subject Under Test
        view.sinkTypeSelectionChanged(mockedValueChangeEvent);
    }

    @Test
    public void sinkTypeSelectionChanged_dummySinkType_setUpdateSinkSectionInvisible() {
        // Test preparation
        setupView();
        when(view.sinkTypeSelection.getSelectedKey()).thenReturn("DUMMY");

        // Subject Under Test
        view.sinkTypeSelectionChanged(mockedValueChangeEvent);

        // Test Verification
        verify(view.updateSinkSection).setVisible(false);
        verifyNoMoreInteractions(view.updateSinkSection);
        verify(mockedPresenter).sinkTypeChanged("DUMMY");
        verify(mockedPresenter).keyPressed();
        verifyNoMoreInteractions(mockedPresenter);
    }

    @Test
    public void sinkTypeSelectionChanged_openupdateSinkType_setUpdateSinkSectionVisible() {
        // Test preparation
        setupView();
        when(view.sinkTypeSelection.getSelectedKey()).thenReturn("OPENUPDATE");

        // Subject Under Test
        view.sinkTypeSelectionChanged(mockedValueChangeEvent);

        // Test Verification
        verify(view.updateSinkSection).setVisible(true);
        verifyNoMoreInteractions(view.updateSinkSection);
        verify(mockedPresenter).sinkTypeChanged("OPENUPDATE");
        verify(mockedPresenter).keyPressed();
        verifyNoMoreInteractions(mockedPresenter);
    }

    @Test
    public void nameChanged_called_presenterNotified() {
        // Test preparation
        setupView();
        when(view.name.getText()).thenReturn("-name-");

        // Subject Under Test
        view.nameChanged(mockedValueChangeEvent);

        // Test Verification
        verify(mockedPresenter).nameChanged("-name-");
        verify(mockedPresenter).keyPressed();
        verifyNoMoreInteractions(mockedPresenter);
    }

    @Test
    public void resourceChanged_called_presenterNotified() {
        // Test preparation
        setupView();
        when(view.resource.getText()).thenReturn("-resource-");

        // Subject Under Test
        view.resourceChanged(mockedValueChangeEvent);

        // Test Verification
        verify(mockedPresenter).resourceChanged("-resource-");
        verify(mockedPresenter).keyPressed();
        verifyNoMoreInteractions(mockedPresenter);
    }

    @Test
    public void descriptionChanged_called_presenterNotified() {
        // Test preparation
        setupView();
        when(view.description.getText()).thenReturn("-description-");

        // Subject Under Test
        view.descriptionChanged(mockedValueChangeEvent);

        // Test Verification
        verify(mockedPresenter).descriptionChanged("-description-");
        verify(mockedPresenter).keyPressed();
        verifyNoMoreInteractions(mockedPresenter);
    }

    @Test
    public void urlChanged_called_presenterNotified() {
        // Test preparation
        setupView();
        when(view.url.getText()).thenReturn("-url-");

        // Subject Under Test
        view.urlChanged(mockedValueChangeEvent);

        // Test Verification
        verify(mockedPresenter).endpointChanged("-url-");
        verify(mockedPresenter).keyPressed();
        verifyNoMoreInteractions(mockedPresenter);
    }

    @Test
    public void userIdChanged_called_presenterNotified() {
        // Test preparation
        setupView();
        when(view.userid.getText()).thenReturn("-userid-");

        // Subject Under Test
        view.useridChanged(mockedValueChangeEvent);

        // Test Verification
        verify(mockedPresenter).userIdChanged("-userid-");
        verify(mockedPresenter).keyPressed();
        verifyNoMoreInteractions(mockedPresenter);
    }

    @Test
    public void passwordChanged_called_presenterNotified() {
        // Test preparation
        setupView();
        when(view.password.getText()).thenReturn("-password-");

        // Subject Under Test
        view.passwordChanged(mockedValueChangeEvent);

        // Test Verification
        verify(mockedPresenter).passwordChanged("-password-");
        verify(mockedPresenter).keyPressed();
        verifyNoMoreInteractions(mockedPresenter);
    }

    @Test
    public void queueProvidersChanged_called_presenterNotified() {
        // Test preparation
        setupView();
        Map<String, String> queueProviders = new HashMap<>();
        queueProviders.put("key1", "value1");
        queueProviders.put("key2", "value2");
        when(view.queueProviders.getValue()).thenReturn(queueProviders);

        // Subject Under Test
        view.availableQueueProvidersChanged(mockedValueChangeEvent);

        // Test Verification
        verify(mockedPresenter).queueProvidersChanged(Arrays.asList("value1", "value2"));
        verify(mockedPresenter).keyPressed();
        verifyNoMoreInteractions(mockedPresenter);
    }

    @Test
    public void saveButtonPressed_called_presenterNotified() {
        // Test preparation
        setupView();

        // Subject Under Test
        view.saveButtonPressed(mockedClickEvent);

        // Test Verification
        verify(mockedPresenter).saveButtonPressed();
        verifyNoMoreInteractions(mockedPresenter);
    }

    @Test
    public void deleteButtonPressed_called_presenterNotified() {
        // Test preparation
        setupView();

        // Subject Under Test
        view.deleteButtonPressed(mockedClickEvent);

        // Test Verification
        verify(view.confirmation).show();
        verifyNoMoreInteractions(mockedPresenter);
    }

    @Test
    public void availableQueueProvidersButtonClicked_isAddEvent_presenterNotified() {
        // Test preparation
        setupView();
        when(view.queueProviders.isAddEvent(any(ClickEvent.class))).thenReturn(true);

        // Subject Under Test
        view.availableQueueProvidersButtonClicked(mockedClickEvent);

        // Test Verification
        verify(mockedPresenter).queueProvidersAddButtonPressed();
        verifyNoMoreInteractions(mockedPresenter);
    }

    @Test
    public void availableQueueProvidersButtonClicked_isNotAddEvent_presenterNotNotified() {
        // Test preparation
        setupView();
        when(view.queueProviders.isAddEvent(any(ClickEvent.class))).thenReturn(false);

        // Subject Under Test
        view.availableQueueProvidersButtonClicked(mockedClickEvent);

        // Test Verification
        verifyNoMoreInteractions(mockedPresenter);
    }

    @Test
    public void sequenceAnalysisSelectionChanged_all_presenterNotified() {
        // Test preparation
        setupView();
        when(mockedValueChangeEvent.getValue()).thenReturn("ALL");

        // Subject Under Test
        view.sequenceAnalysisSelectionChanged(mockedValueChangeEvent);

        // Test Verification
        verify(mockedPresenter).sequenceAnalysisSelectionChanged("ALL");
        verifyNoMoreInteractions(mockedPresenter);
    }

    @Test
    public void sequenceAnalysisSelectionChanged_idOnly_presenterNotified() {
        // Test preparation
        setupView();
        when(mockedValueChangeEvent.getValue()).thenReturn("ID_ONLY");

        // Subject Under Test
        view.sequenceAnalysisSelectionChanged(mockedValueChangeEvent);

        // Test Verification
        verify(mockedPresenter).sequenceAnalysisSelectionChanged("ID_ONLY");
        verifyNoMoreInteractions(mockedPresenter);
    }

    @Test(expected = NullPointerException.class)
    public void popupTextBoxChanged_nullEvent_exception() {
        // Test preparation
        setupView();

        // Subject Under Test
        view.popupTextBoxChanged(null);
    }

    @Test
    public void popupTextBoxChanged_validEvent_setPopupContent() {
        // Test preparation
        setupView();
        Map<String, String> qProviders = new HashMap<>();
        qProviders.put("key1", "value1");
        when(view.queueProviders.getValue()).thenReturn(qProviders);
        when(mockedValueChangeEvent.getValue()).thenReturn("provider2");

        // Subject Under Test
        view.popupTextBoxChanged(mockedValueChangeEvent);

        // Test Verification
        verify(mockedValueChangeEvent, times(2)).getValue();
        verifyNoMoreInteractions(mockedValueChangeEvent);
        verify(view.queueProviders).getValue();
        Map<String, String> expectedResult = new HashMap<>();
        expectedResult.put("key1", "value1");
        expectedResult.put("provider2", "provider2");
        verify(view.queueProviders).setValue(expectedResult, true);
        verifyNoMoreInteractions(view.queueProviders);
    }

    @Test
    public void confirmationButtonClicked_okButton_presenterCalled() {
        // Test preparation
        setupView();
        when(mockedDialogEvent.getDialogButton()).thenReturn(DialogEvent.DialogButton.OK_BUTTON);

        // Subject Under Test
        view.confirmationButtonClicked(mockedDialogEvent);

        // Test Verification
        verify(mockedPresenter).deleteButtonPressed();
        verifyNoMoreInteractions(mockedPresenter);
    }

    @Test
    public void confirmationButtonClicked_cancelButton_presenterNotCalled() {
        // Test preparation
        setupView();
        when(mockedDialogEvent.getDialogButton()).thenReturn(DialogEvent.DialogButton.CANCEL_BUTTON);

        // Subject Under Test
        view.confirmationButtonClicked(mockedDialogEvent);

        // Test Verification
        verifyNoMoreInteractions(mockedPresenter);
    }


    /**
     * Private methods
     */
    private void setupView() {
        view = new View();
        view.setPresenter(mockedPresenter);
    }

}
