/*
 *
 *  * DataIO - Data IO
 *  * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 *  * Denmark. CVR: 15149043
 *  *
 *  * This file is part of DataIO.
 *  *
 *  * DataIO is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * DataIO is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package dk.dbc.dataio.gui.client.pages.harvester.holdingsitem.modify;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwtmockito.GwtMockitoTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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

    // Subject Under Test
    private View view;


    @Before
    public void setupMocks() {
        view = new View();
        view.setPresenter(mockedPresenter);
        when(view.name.getText()).thenReturn("-name-");
        when(view.description.getText()).thenReturn("-description-");
        when(view.resource.getText()).thenReturn("-resource-");
        when(view.enabled.getValue()).thenReturn(true);
    }

    @After
    public void verifyNoMoreMockCalls() {
        verifyNoMoreInteractions(mockedPresenter);
        verifyNoMoreInteractions(mockedValueChangeEvent);
        verifyNoMoreInteractions(mockedClickEvent);
        verifyNoMoreInteractions(view.name);
        verifyNoMoreInteractions(view.description);
        verifyNoMoreInteractions(view.resource);
        verifyNoMoreInteractions(view.rrHarvesters);
        verifyNoMoreInteractions(view.enabled);
        verifyNoMoreInteractions(view.status);
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
    public void rrHarvesterChanged_call_rrHarvesterChanged() {
        // Test Preparation
        Map<String, String> value = new HashMap<>();
        value.put("-key", "-value");
        when(view.rrHarvesters.getValue()).thenReturn(value);

        // Subject Under Test
        view.rrHarvestersChanged(mockedValueChangeEvent);

        // Test verification
        verify(view.rrHarvesters).getValue();
        verify(mockedPresenter).rrHarvestersChanged(Arrays.asList("-key"));
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
    public void saveButtonPressed_call_presenterSignalled() {
        // Subject Under Test
        view.saveButtonPressed(mockedClickEvent);

        // Test verification
        verify(mockedPresenter).saveButtonPressed();
    }

}