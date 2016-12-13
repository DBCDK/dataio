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

package dk.dbc.dataio.gui.client.pages.harvester.ticklerepo.show;

import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.any;
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
    @Mock View mockedView;
    @Mock Widget mockedViewWidget;
    @Mock ViewGinjector mockedViewGinjector;

    // Setup mocked data
    @Before
    public void setupMockedData() {
        when(mockedCommonGinjector.getFlowStoreProxyAsync()).thenReturn(mockedFlowStore);
        when(mockedViewGinjector.getView()).thenReturn(mockedView);
        when(mockedCommonGinjector.getMenuTexts()).thenReturn(mockedMenuTexts);
        when(mockedView.asWidget()).thenReturn(mockedViewWidget);
        when(mockedCommonGinjector.getProxyErrorTexts()).thenReturn(mockedProxyErrorTexts);
    }

    private List<TickleRepoHarvesterConfig> testHarvesterConfig = new ArrayList<>();


    // Subject Under Test
    private PresenterImplConcrete presenterImpl;


    // Test specialization of Presenter to enable test of callback's
    class PresenterImplConcrete extends PresenterImpl {
        public PresenterImplConcrete() {
            super(mockedPlaceController);
            viewInjector = mockedViewGinjector;
            commonInjector = mockedCommonGinjector;
        }
        public GetTickleRepoHarvestersCallback getHarvestersCallback = new GetTickleRepoHarvestersCallback();
    }


    //------------------------------------------------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    public void start_callStart_ok() {
        presenterImpl = new PresenterImplConcrete();

        // Test Subject Under Test
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Verify Test
        verify(mockedViewGinjector, times(3)).getView();
        verify(mockedView).setPresenter(presenterImpl);
        verify(mockedView).setHeader(any(String.class));
        verify(mockedView).asWidget();
        verify(mockedContainerWidget).setWidget(mockedViewWidget);
        verify(mockedFlowStore).findAllTickleRepoHarvesterConfigs(any(AsyncCallback.class));
        verifyNoMoreInteractions(mockedViewGinjector);
        verifyNoMoreInteractions(mockedView);
        verifyNoMoreInteractions(mockedContainerWidget);
        verifyNoMoreInteractions(mockedFlowStore);
    }


    @Test
    public void getHarvesterRrConfigs_callbackWithError_errorMessageInView() {
        presenterImpl = new PresenterImplConcrete();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getHarvestersCallback.onFilteredFailure(new Exception());

        // Verify Test
        // The following is called from start()
        verify(mockedView).setPresenter(presenterImpl);
        verify(mockedView).setHeader(any(String.class));
        verify(mockedView).asWidget();
        // The following is called from GetHarvestersCallback
        verify(mockedView).setErrorText(any(String.class));
        verifyNoMoreInteractions(mockedView);
    }

    @Test
    public void getHarvesterRrConfigs_callbackWithSuccess_fetchHarvesterConfigs() {
        presenterImpl = new PresenterImplConcrete();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getHarvestersCallback.onSuccess(testHarvesterConfig);

        // Verify Test
        // The following is called from start()
        verify(mockedView).setPresenter(presenterImpl);
        verify(mockedView).setHeader(any(String.class));
        verify(mockedView).asWidget();
        // The following is called from GetHarvestersCallback
        verify(mockedView).setHarvesters(testHarvesterConfig);
        verifyNoMoreInteractions(mockedView);
    }

}
