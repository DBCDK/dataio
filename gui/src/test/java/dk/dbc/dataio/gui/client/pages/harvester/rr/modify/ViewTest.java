package dk.dbc.dataio.gui.client.pages.harvester.rr.modify;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.events.DialogEvent;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.AbstractMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
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

    @Mock
    Presenter mockedPresenter;
    @Mock
    ValueChangeEvent mockedValueChangeEvent;
    @Mock
    DialogEvent mockedDialogEvent;
    @Mock
    ClickEvent mockedClickEvent;
    @Mock
    Map mockedMap;


    // Subject Under Test
    private View view;


    @Before
    public void setupMocks() {
        view = new View();
        view.setPresenter(mockedPresenter);
        when(view.name.getText()).thenReturn("-name-");
        when(view.description.getText()).thenReturn("-description-");
        when(view.resource.getText()).thenReturn("-resource-");
        when(view.consumerId.getText()).thenReturn("-consumerId-");
        when(view.size.getText()).thenReturn("-size-");
        when(view.relations.getValue()).thenReturn(false);
        when(view.libraryRules.getValue()).thenReturn(false);
        when(view.harvesterType.getValue()).thenReturn(RRHarvesterConfig.HarvesterType.STANDARD.toString());
        when(view.holdingsTarget.getText()).thenReturn("-holdingsTarget-");
        when(view.destination.getText()).thenReturn("-destination-");
        when(view.format.getText()).thenReturn("-format-");
        when(view.type.getSelectedKey()).thenReturn("-type-");
        when(view.note.getText()).thenReturn("-note-");
        when(view.enabled.getValue()).thenReturn(true);
    }

    @After
    public void verifyNoMoreMockCalls() {
        verifyNoMoreInteractions(mockedPresenter);
        verifyNoMoreInteractions(mockedValueChangeEvent);
        verifyNoMoreInteractions(mockedClickEvent);
        verifyNoMoreInteractions(mockedMap);
        verifyNoMoreInteractions(view.name);
        verifyNoMoreInteractions(view.description);
        verifyNoMoreInteractions(view.resource);
        verifyNoMoreInteractions(view.consumerId);
        verifyNoMoreInteractions(view.size);
        verifyNoMoreInteractions(view.formatOverrides);
        verifyNoMoreInteractions(view.relations);
        verifyNoMoreInteractions(view.libraryRules);
        verifyNoMoreInteractions(view.harvesterType);
        verifyNoMoreInteractions(view.holdingsTarget);
        verifyNoMoreInteractions(view.destination);
        verifyNoMoreInteractions(view.format);
        verifyNoMoreInteractions(view.type);
        verifyNoMoreInteractions(view.note);
        verifyNoMoreInteractions(view.enabled);
        verifyNoMoreInteractions(view.status);
        verifyNoMoreInteractions(view.popupFormatOverrideEntry);
        verifyNoMoreInteractions(view.updateButton);
    }


    /*
     * Testing starts here...
     */

    @Test
    public void constructor_instantiate_objectCorrectInitialized() {
        // Subject Under Test
        view = new View();
    }

    @Test
    public void nameChanged_call_nameChanged() {
        // Subject Under Test
        view.nameChanged(mockedValueChangeEvent);

        // Test verification
        verify(view.name).getText();
        verify(mockedPresenter).nameChanged("-name-");
        verify(mockedPresenter).keyPressed();
    }

    @Test
    public void descriptionChanged_call_descriptionChanged() {
        // Subject Under Test
        view.descriptionChanged(mockedValueChangeEvent);

        // Test verification
        verify(view.description).getText();
        verify(mockedPresenter).descriptionChanged("-description-");
        verify(mockedPresenter).keyPressed();
    }

    @Test
    public void resourceChanged_call_resourceChanged() {
        // Subject Under Test
        view.resourceChanged(mockedValueChangeEvent);

        // Test verification
        verify(view.resource).getText();
        verify(mockedPresenter).resourceChanged("-resource-");
        verify(mockedPresenter).keyPressed();
    }

    @Test
    public void consumerIdChanged_call_consumerIdChanged() {
        // Subject Under Test
        view.consumerIdChanged(mockedValueChangeEvent);

        // Test verification
        verify(view.consumerId).getText();
        verify(mockedPresenter).consumerIdChanged("-consumerId-");
        verify(mockedPresenter).keyPressed();
    }

    @Test
    public void sizeChanged_call_sizeChanged() {
        // Subject Under Test
        view.sizeChanged(mockedValueChangeEvent);

        // Test verification
        verify(view.size).getText();
        verify(mockedPresenter).sizeChanged("-size-");
        verify(mockedPresenter).keyPressed();
    }

    @Test
    public void relationsChanged_call_relationsChanged() {
        // Subject Under Test
        view.relationsChanged(mockedValueChangeEvent);

        // Test verification
        verify(view.relations).getValue();
        verify(mockedPresenter).relationsChanged(false);
        verify(mockedPresenter).keyPressed();
    }

    @Test
    public void libraryRulesChanged_call_libraryRulesChanged() {
        // Subject Under Test
        view.libraryRulesChanged(mockedValueChangeEvent);

        // Test verification
        verify(view.libraryRules).getValue();
        verify(mockedPresenter).libraryRulesChanged(false);
        verify(mockedPresenter).keyPressed();
    }

    @Test
    public void imsHarvesterChanged_call_imsHarvesterChanged() {
        // Subject Under Test
        view.imsHarvesterChanged(mockedValueChangeEvent);

        // Test verification
        verify(view.harvesterType).getValue();
        verify(mockedPresenter).harvesterTypeChanged(RRHarvesterConfig.HarvesterType.STANDARD.toString());
        verify(mockedPresenter).keyPressed();
    }

    @Test
    public void imsHoldingsTargetChanged_call_imsHoldingsTargetChanged() {
        // Subject Under Test
        view.holdingsTargetChanged(mockedValueChangeEvent);

        // Test verification
        verify(view.holdingsTarget).getText();
        verify(mockedPresenter).holdingsTargetChanged("-holdingsTarget-");
        verify(mockedPresenter).keyPressed();
    }


    @Test
    public void destinationChanged_call_destinationChanged() {
        // Subject Under Test
        view.destinationChanged(mockedValueChangeEvent);

        // Test verification
        verify(view.destination).getText();
        verify(mockedPresenter).destinationChanged("-destination-");
        verify(mockedPresenter).keyPressed();
    }

    @Test
    public void formatChanged_call_formatChanged() {
        // Subject Under Test
        view.formatChanged(mockedValueChangeEvent);

        // Test verification
        verify(view.format).getText();
        verify(mockedPresenter).formatChanged("-format-");
        verify(mockedPresenter).keyPressed();
    }

    @Test
    public void typeChanged_call_typeChanged() {
        // Subject Under Test
        view.typeChanged(mockedValueChangeEvent);

        // Test verification
        verify(view.type).getSelectedKey();
        verify(mockedPresenter).typeChanged("-type-");
        verify(mockedPresenter).keyPressed();
    }

    @Test
    public void noteChanged_call_noteChanged() {
        // Subject Under Test
        view.noteChanged(mockedValueChangeEvent);

        // Test verification
        verify(view.note).getText();
        verify(mockedPresenter).noteChanged("-note-");
        verify(mockedPresenter).keyPressed();
    }

    @Test
    public void enabledChanged_call_enabledChanged() {
        // Subject Under Test
        view.enabledChanged(mockedValueChangeEvent);

        // Test verification
        verify(view.enabled).getValue();
        verify(mockedPresenter).enabledChanged(true);
        verify(mockedPresenter).keyPressed();
    }

    @Test
    public void popupFormatOverrideChanged_call_keyPressed() {
        // Subject Under Test
        view.popupFormatOverrideChanged(mockedValueChangeEvent);

        // Test verification
        verify(mockedPresenter).keyPressed();
    }

    @Test
    public void popupFormatOverrideOkButton_noError_popupTextBoxChanged() {
        // Test preparation
        when(mockedPresenter.formatOverrideAdded(anyString(), anyString())).thenReturn(null);  // Signalling no error
        Map.Entry<String, String> entry = new AbstractMap.SimpleEntry<>("*key", "*value");
        when(view.popupFormatOverrideEntry.getValue()).thenReturn(entry);
        when(mockedDialogEvent.getDialogButton()).thenReturn(DialogEvent.DialogButton.OK_BUTTON);

        // Subject Under Test
        view.popupFormatOverrideOkButton(mockedDialogEvent);

        // Test verification
        verify(view.popupFormatOverrideEntry, times(2)).getValue();
        verify(mockedPresenter).formatOverrideAdded("*key", "*value");
        verify(view.formatOverrides).addValue("*key - *value", "*key");
    }

    @Test
    public void popupFormatOverrideOkButton_error_popupTextBoxChanged() {
        // Test preparation
        when(mockedPresenter.formatOverrideAdded(anyString(), anyString())).thenReturn("There was an error");  // Signalling error
        Map.Entry<String, String> entry = new AbstractMap.SimpleEntry<>("*key", "*value");
        when(view.popupFormatOverrideEntry.getValue()).thenReturn(entry);
        when(mockedDialogEvent.getDialogButton()).thenReturn(DialogEvent.DialogButton.OK_BUTTON);

        // Subject Under Test
        view.popupFormatOverrideOkButton(mockedDialogEvent);

        // Test verification
        verify(view.popupFormatOverrideEntry, times(2)).getValue();
        verify(mockedPresenter).formatOverrideAdded("*key", "*value");
        // A call to setErrorText is made, which cannot be seen here
    }


    @Test
    public void formatOverridesButtonPressed_nonExistingEvent_onlyKeyPressedAction() {
        // Test preparation
        when(view.formatOverrides.isAddEvent(mockedClickEvent)).thenReturn(false);
        when(view.formatOverrides.isRemoveEvent(mockedClickEvent)).thenReturn(false);

        // Subject Under Test
        view.formatOverridesButtonPressed(mockedClickEvent);

        // Test verification
        verify(view.formatOverrides).isAddEvent(mockedClickEvent);
        verify(view.formatOverrides).isRemoveEvent(mockedClickEvent);
        verify(mockedPresenter).keyPressed();
    }

    @Test
    public void formatOverridesButtonPressed_addEvent_addAction() {
        // Test preparation
        when(view.formatOverrides.isAddEvent(mockedClickEvent)).thenReturn(true);

        // Subject Under Test
        view.formatOverridesButtonPressed(mockedClickEvent);

        // Test verification
        verify(view.formatOverrides).isAddEvent(mockedClickEvent);
        verify(mockedPresenter).formatOverridesAddButtonPressed();
        verify(mockedPresenter).keyPressed();
    }

    @Test
    public void formatOverridesButtonPressed_removeEvent_addAction() {
        // Test preparation
        when(view.formatOverrides.isAddEvent(mockedClickEvent)).thenReturn(false);
        when(view.formatOverrides.isRemoveEvent(mockedClickEvent)).thenReturn(true);
        when(view.formatOverrides.getSelectedItem()).thenReturn("1324");
        when(view.formatOverrides.getValue()).thenReturn(mockedMap);

        // Subject Under Test
        view.formatOverridesButtonPressed(mockedClickEvent);

        // Test verification
        verify(view.formatOverrides).isAddEvent(mockedClickEvent);
        verify(view.formatOverrides).isRemoveEvent(mockedClickEvent);
        verify(view.formatOverrides).getSelectedItem();
        verify(view.formatOverrides).getValue();
        verify(view.formatOverrides).setValue(mockedMap);
        verify(mockedPresenter).formatOverridesRemoveButtonPressed("1324");
        verify(mockedPresenter).keyPressed();
        verify(mockedMap).remove("1324");
    }

    @Test
    public void saveButtonPressed_call_presenterSignalled() {
        // Subject Under Test
        view.saveButtonPressed(mockedClickEvent);

        // Test verification
        verify(mockedPresenter).saveButtonPressed();
    }

    @Test
    public void updateButtonPressed_call_presenterSignalled() {
        // Subject Under Test
        view.updateButtonPressed(mockedClickEvent);

        // Test verification
        verify(mockedPresenter).updateButtonPressed();
    }

}
