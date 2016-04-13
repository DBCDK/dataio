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

package dk.dbc.dataio.gui.client.pages.iotraffic;

import com.google.gwt.user.client.ui.Widget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.commons.types.GatekeeperDestination;
import dk.dbc.dataio.commons.utils.test.model.GatekeeperDestinationBuilder;
import dk.dbc.dataio.gui.client.components.EnterButton;
import dk.dbc.dataio.gui.client.components.PromptedCheckBox;
import dk.dbc.dataio.gui.client.components.PromptedTextBox;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
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

    @Mock View mockedView;
    @Mock Texts mockedTexts;
    @Mock Widget mockedViewWidget;
    @Mock PromptedTextBox mockedSubmitter;
    @Mock PromptedTextBox mockedPackaging;
    @Mock PromptedTextBox mockedFormat;
    @Mock PromptedTextBox mockedDestination;
    @Mock PromptedCheckBox mockedCopy;
    @Mock PromptedCheckBox mockedNotify;
    @Mock EnterButton mockedAddButton;
    @Mock GatekeepersTable mockedGatekeepersTable;
    @Mock Throwable mockedThrowable;


    // Setup data
    @Before
    public void setupData() {
        when(mockedView.asWidget()).thenReturn(mockedViewWidget);
        mockedView.submitter = mockedSubmitter;
        mockedView.packaging = mockedPackaging;
        mockedView.format = mockedFormat;
        mockedView.destination = mockedDestination;
        mockedView.copy = mockedCopy;
        mockedView.notify = mockedNotify;
        mockedView.addButton = mockedAddButton;
        mockedView.gatekeepersTable = mockedGatekeepersTable;
    }


    // Subject Under Test
    private PresenterImpl presenterImpl;


    @Test
    public void start_callStart_verifyAllCalls() {
        // Prepare test
        presenterImpl = setupPresenter();

        // Subject Under Test
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Verify Test
        verify(mockedContainerWidget).setWidget(mockedViewWidget);
        verify(mockedView).asWidget();
        verify(mockedView).setHeader(header);
        verify(mockedView).setPresenter(presenterImpl);
        verifyNoMoreInteractions(mockedContainerWidget);
        verifyNoMoreInteractions(mockedView);
        verifyInitializeDataMethodCall();
    }

    @Test
    public void copyChanged_copyChangedToFalse_notifyIsSetToFalse() {
        // Prepare Test
        presenterImpl = setupPresenter();

        // Subject under test
        presenterImpl.copyChanged(false);

        // Verify Test
        assertThat(presenterImpl.copy, is(false));
        assertThat(presenterImpl.notify, is(false));
    }

    @Test(expected = NullPointerException.class)
    public void addButtonPressed_submitterNull_exception() {
        // Prepare Test
        presenterImpl = setupPresenter();
        presenterImpl.submitter = null;

        // Subject Under Test
        presenterImpl.addButtonPressed();
    }

    @Test
    public void addButtonPressed_submitterEmpty_displayWarning() {
        // Prepare Test
        presenterImpl = setupPresenter();
        presenterImpl.submitter = "";

        // Subject Under Test
        presenterImpl.addButtonPressed();

        // Verify Test
        verify(mockedTexts).error_InputFieldValidationError();
        verify(mockedView).displayWarning(any(String.class));
        verifyNoMoreInteractions(mockedTexts);
        verifyNoMoreInteractions(mockedView);
    }

    @Test(expected = NullPointerException.class)
    public void addButtonPressed_packagingNull_exception() {
        // Prepare Test
        presenterImpl = setupPresenter();
        presenterImpl.packaging = null;

        // Subject Under Test
        presenterImpl.addButtonPressed();
    }

    @Test
    public void addButtonPressed_packagingEmpty_displayWarning() {
        // Prepare Test
        presenterImpl = setupPresenter();
        presenterImpl.packaging = "";

        // Subject Under Test
        presenterImpl.addButtonPressed();

        // Verify Test
        verify(mockedTexts).error_InputFieldValidationError();
        verify(mockedView).displayWarning(any(String.class));
        verifyNoMoreInteractions(mockedTexts);
        verifyNoMoreInteractions(mockedView);
    }

    @Test(expected = NullPointerException.class)
    public void addButtonPressed_formatNull_exception() {
        // Prepare Test
        presenterImpl = setupPresenter();
        presenterImpl.format = null;

        // Subject Under Test
        presenterImpl.addButtonPressed();
    }

    @Test
    public void addButtonPressed_formatEmpty_displayWarning() {
        // Prepare Test
        presenterImpl = setupPresenter();
        presenterImpl.format = "";

        // Subject Under Test
        presenterImpl.addButtonPressed();

        // Verify Test
        verify(mockedTexts).error_InputFieldValidationError();
        verify(mockedView).displayWarning(any(String.class));
        verifyNoMoreInteractions(mockedTexts);
        verifyNoMoreInteractions(mockedView);
    }

    @Test(expected = NullPointerException.class)
    public void addButtonPressed_destinationNull_exception() {
        // Prepare Test
        presenterImpl = setupPresenter();
        presenterImpl.destination = null;

        // Subject Under Test
        presenterImpl.addButtonPressed();
    }

    @Test
    public void addButtonPressed_destinationEmpty_displayWarning() {
        // Prepare Test
        presenterImpl = setupPresenter();
        presenterImpl.destination = "";

        // Subject Under Test
        presenterImpl.addButtonPressed();

        // Verify Test
        verify(mockedTexts).error_InputFieldValidationError();
        verify(mockedView).displayWarning(any(String.class));
        verifyNoMoreInteractions(mockedTexts);
        verifyNoMoreInteractions(mockedView);
    }

    @Test
    public void addButtonPressed_noEmptyData_callFlowStore() {
        // Prepare Test
        presenterImpl = setupPresenter();

        // Subject Under Test
        presenterImpl.addButtonPressed();

        // Verify Test
        verify(presenterImpl.flowStoreProxy).createGatekeeperDestination(
                any(GatekeeperDestination.class),
                any(PresenterImpl.CreateGatekeeperDestinationCallback.class)
        );
        verifyNoMoreInteractions(presenterImpl.flowStoreProxy);
        verifyNoMoreInteractions(mockedTexts);
        verifyNoMoreInteractions(mockedView);
    }

    @Test
    public void createGatekeeperDestinationCallback_callOnFailure_displayWarning() {
        // Prepare Test
        presenterImpl = new PresenterImpl("Header Text");
        PresenterImpl.CreateGatekeeperDestinationCallback callback = presenterImpl.new CreateGatekeeperDestinationCallback();
        when(presenterImpl.viewInjector.getView()).thenReturn(mockedView);
        when(presenterImpl.viewInjector.getTexts()).thenReturn(mockedTexts);

        // Subject Under Test
        callback.onFailure(mockedThrowable);

        // Verify Test
        verify(mockedTexts).error_CannotCreateGatekeeperDestination();
        verify(mockedView).displayWarning(any(String.class));
        verifyNoMoreInteractions(mockedTexts);
        verifyNoMoreInteractions(mockedView);
    }

    @Test
    public void createGatekeeperDestinationCallback_callOnSucces_initializeData() {
        // Prepare Test
        presenterImpl = new PresenterImpl("Header Text");
        PresenterImpl.CreateGatekeeperDestinationCallback callback = presenterImpl.new CreateGatekeeperDestinationCallback();
        when(presenterImpl.viewInjector.getView()).thenReturn(mockedView);
        when(presenterImpl.viewInjector.getTexts()).thenReturn(mockedTexts);

        // Subject Under Test
        callback.onSuccess(new GatekeeperDestinationBuilder().build());

        // Verify Test
        verifyInitializeDataMethodCall();
    }

    @Test
    public void findAllGateKeeperDestinationsCallback_callOnFailure_displayWarning() {
        // Prepare Test
        presenterImpl = new PresenterImpl("Header Text");
        PresenterImpl.FindAllGateKeeperDestinationsCallback callback = presenterImpl.new FindAllGateKeeperDestinationsCallback();
        when(presenterImpl.viewInjector.getView()).thenReturn(mockedView);
        when(presenterImpl.viewInjector.getTexts()).thenReturn(mockedTexts);

        // Subject Under Test
        callback.onFailure(mockedThrowable);

        // Verify Test
        verify(mockedTexts).error_CannotFetchGatekeeperDestinations();
        verify(mockedView).displayWarning(any(String.class));
        verifyNoMoreInteractions(mockedTexts);
        verifyNoMoreInteractions(mockedView);
    }

    @Test
    public void findAllGateKeeperDestinationsCallback_callOnSucces_initializeData() {
        // Prepare Test
        presenterImpl = new PresenterImpl("Header Text");
        PresenterImpl.FindAllGateKeeperDestinationsCallback callback = presenterImpl.new FindAllGateKeeperDestinationsCallback();
        when(presenterImpl.viewInjector.getView()).thenReturn(mockedView);
        when(presenterImpl.viewInjector.getTexts()).thenReturn(mockedTexts);
        List<GatekeeperDestination> gatekeeperDestinations = Collections.singletonList(new GatekeeperDestinationBuilder().build());

        // Subject Under Test
        callback.onSuccess(gatekeeperDestinations);

        // Verify Test
        verify(mockedView).setGatekeepers(gatekeeperDestinations);
        verifyNoMoreInteractions(mockedTexts);
        verifyNoMoreInteractions(mockedView);
    }


    /*
     * Private methods
     */
    private PresenterImpl setupPresenter() {
        PresenterImpl presenter = new PresenterImpl("Header Text");
        when(presenter.viewInjector.getView()).thenReturn(mockedView);
        when(presenter.viewInjector.getTexts()).thenReturn(mockedTexts);
        presenter.submitter = "submitter";
        presenter.packaging = "packaging";
        presenter.format = "format";
        presenter.destination = "destination";
        presenter.copy = true;
        presenter.notify = true;
        return presenter;
    }

    private void verifyInitializeDataMethodCall() {
        verify(mockedSubmitter).clearText();
        verify(mockedPackaging).clearText();
        verify(mockedFormat).clearText();
        verify(mockedDestination).clearText();
        verify(mockedCopy).setValue(false);
        verify(mockedNotify).setValue(false);
        verify(mockedNotify).setVisible(false);
        verify(presenterImpl.flowStoreProxy).findAllGatekeeperDestinations(any(PresenterImpl.FindAllGateKeeperDestinationsCallback.class));
        verifyNoMoreInteractions(mockedSubmitter);
        verifyNoMoreInteractions(mockedPackaging);
        verifyNoMoreInteractions(mockedFormat);
        verifyNoMoreInteractions(mockedDestination);
        verifyNoMoreInteractions(mockedCopy);
        verifyNoMoreInteractions(presenterImpl.flowStoreProxy);
    }
}
