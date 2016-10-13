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


package dk.dbc.dataio.gui.client.pages.sink.status;


import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.mockito.Matchers.anyList;
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
    @Mock PlaceController mockedPlaceController;
    @Mock Texts mockedTexts;
    @Mock View mockedView;
    @Mock Widget mockedViewWidget;
    @Mock ViewGinjector mockedViewGinjector;

    // Setup mocked data
    @Before
    public void setupMockedData() {
        when(mockedViewGinjector.getView()).thenReturn(mockedView);
        when(mockedViewGinjector.getTexts()).thenReturn(mockedTexts);
        when(mockedView.asWidget()).thenReturn(mockedViewWidget);
        when(mockedCommonGinjector.getProxyErrorTexts()).thenReturn(mockedProxyErrorTexts);
    }

    @Before
    public void setupTexts() {
        when(mockedMenuTexts.menu_SinkStatus()).thenReturn("UshHarvestersMenu");
    }


    // Subject Under Test
    private PresenterImpl presenterImpl;



    @Test
    @SuppressWarnings("unchecked")
    public void start_callStart_ok() {
        setupPresenterImpl();

        // Test Subject Under Test
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Verify Test
        verify(mockedViewGinjector, times(4)).getView();
        verify(mockedView).setHeader("Sink Status");
        verify(mockedView).setPresenter(presenterImpl);
        verify(mockedView).asWidget();
        verify(mockedContainerWidget).setWidget(mockedViewWidget);
        verify(mockedView).setSinkStatus(anyList());
        verifyNoMoreInteractionsOnMocks();
    }



    // Private methods

    private void verifyNoMoreInteractionsOnMocks() {
        verifyNoMoreInteractions(mockedViewGinjector);
        verifyNoMoreInteractions(mockedView);
        verifyNoMoreInteractions(mockedContainerWidget);
        verifyNoMoreInteractions(mockedFlowStore);
        verifyNoMoreInteractions(mockedMenuTexts);
        verifyNoMoreInteractions(mockedTexts);
    }

    private void setupPresenterImpl() {
        presenterImpl = new PresenterImpl("Sink Status");
        presenterImpl.viewInjector = mockedViewGinjector;
    }
}
