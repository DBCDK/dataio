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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwtmockito.GwtMockitoTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.AbstractMap;
import java.util.Map;

import static org.mockito.Matchers.anyString;
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

    @Mock Presenter mockedPresenter;
    @Mock ValueChangeEvent mockedValueChangeEvent;
    @Mock ClickEvent mockedClickEvent;
    @Mock Map mockedMap;


    // Subject Under Test
    private View view;


    @Before
    public void setupMocks() {
        view = new View();
        view.setPresenter(mockedPresenter);
        when(view.name.getText()).thenReturn("-name-");
        when(view.resource.getText()).thenReturn("-resource-");
        when(view.targetUrl.getText()).thenReturn("-targetUrl-");
        when(view.targetGroup.getText()).thenReturn("-targetGroup-");
        when(view.targetUser.getText()).thenReturn("-targetUser-");
        when(view.targetPassword.getText()).thenReturn("-targetPassword-");
        when(view.consumerId.getText()).thenReturn("-consumerId-");
        when(view.size.getText()).thenReturn("-size-");
        when(view.relations.getValue()).thenReturn(false);
        when(view.libraryRules.getValue()).thenReturn(false);
        when(view.destination.getText()).thenReturn("-destination-");
        when(view.format.getText()).thenReturn("-format-");
        when(view.type.getText()).thenReturn("-type-");
        when(view.enabled.getValue()).thenReturn(true);
    }

    @After
    public void verifyNoMoreMockCalls() {
        verifyNoMoreInteractions(mockedPresenter);
        verifyNoMoreInteractions(mockedValueChangeEvent);
        verifyNoMoreInteractions(mockedClickEvent);
        verifyNoMoreInteractions(mockedMap);
        verifyNoMoreInteractions(view.name);
        verifyNoMoreInteractions(view.resource);
        verifyNoMoreInteractions(view.targetUrl);
        verifyNoMoreInteractions(view.targetGroup);
        verifyNoMoreInteractions(view.targetUser);
        verifyNoMoreInteractions(view.targetPassword);
        verifyNoMoreInteractions(view.consumerId);
        verifyNoMoreInteractions(view.size);
        verifyNoMoreInteractions(view.formatOverrides);
        verifyNoMoreInteractions(view.relations);
        verifyNoMoreInteractions(view.libraryRules);
        verifyNoMoreInteractions(view.destination);
        verifyNoMoreInteractions(view.format);
        verifyNoMoreInteractions(view.type);
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
    public void resourceChanged_call_resourceChanged() {
        // Subject Under Test
        view.resourceChanged(mockedValueChangeEvent);

        // Test verification
        verify(view.resource).getText();
        verify(mockedPresenter).resourceChanged("-resource-");
        verify(mockedPresenter).keyPressed();
    }

    @Test
    public void targetUrlChanged_call_targetUrlChanged() {
        // Subject Under Test
        view.targetUrlChanged(mockedValueChangeEvent);

        // Test verification
        verify(view.targetUrl).getText();
        verify(mockedPresenter).targetUrlChanged("-targetUrl-");
        verify(mockedPresenter).keyPressed();
    }

    @Test
    public void targetGroupChanged_call_targetGroupChanged() {
        // Subject Under Test
        view.targetGroupChanged(mockedValueChangeEvent);

        // Test verification
        verify(view.targetGroup).getText();
        verify(mockedPresenter).targetGroupChanged("-targetGroup-");
        verify(mockedPresenter).keyPressed();
    }

    @Test
    public void targetUserChanged_call_targetUserChanged() {
        // Subject Under Test
        view.targetUserChanged(mockedValueChangeEvent);

        // Test verification
        verify(view.targetUser).getText();
        verify(mockedPresenter).targetUserChanged("-targetUser-");
        verify(mockedPresenter).keyPressed();
    }

    @Test
    public void targetPasswordChanged_call_targetPasswordChanged() {
        // Subject Under Test
        view.targetPasswordChanged(mockedValueChangeEvent);

        // Test verification
        verify(view.targetPassword).getText();
        verify(mockedPresenter).targetPasswordChanged("-targetPassword-");
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
        verify(view.type).getText();
        verify(mockedPresenter).typeChanged("-type-");
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
    public void popupTextBoxChanged_noError_popupTextBoxChanged() {
        // Test preparation
        Map.Entry<String, String> entry = new AbstractMap.SimpleEntry<>("*key", "*value");
        when(mockedValueChangeEvent.getValue()).thenReturn(entry);
        when(mockedPresenter.formatOverrideAdded(anyString(), anyString())).thenReturn(null);  // Signalling no error

        // Subject Under Test
        view.popupTextBoxChanged(mockedValueChangeEvent);

        // Test verification
        verify(mockedValueChangeEvent, times(2)).getValue();
        verify(mockedPresenter).formatOverrideAdded("*key", "*value");
        verify(mockedPresenter).keyPressed();
        verify(view.formatOverrides).addValue("*key - *value", "*key");
    }

    @Test
    public void popupTextBoxChanged_error_popupTextBoxChanged() {
        // Test preparation
        Map.Entry<String, String> entry = new AbstractMap.SimpleEntry<>("*key", "*value");
        when(mockedValueChangeEvent.getValue()).thenReturn(entry);
        when(mockedPresenter.formatOverrideAdded(anyString(), anyString())).thenReturn("There was an error");  // Signalling error

        // Subject Under Test
        view.popupTextBoxChanged(mockedValueChangeEvent);

        // Test verification
        verify(mockedValueChangeEvent, times(2)).getValue();
        verify(mockedPresenter).formatOverrideAdded("*key", "*value");
        verify(mockedPresenter).keyPressed();
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
