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

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.modelBuilders.SinkModelBuilder;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PresenterCreateImpl unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class PresenterCreateImplTest {
    @Mock private ClientFactory mockedClientFactory;
    @Mock private FlowStoreProxyAsync mockedFlowStoreProxy;
    @Mock private Texts mockedTexts;
    @Mock private AcceptsOneWidget mockedContainerWidget;
    @Mock private EventBus mockedEventBus;
    @Mock private dk.dbc.dataio.gui.client.pages.navigation.Texts mockedMenuTexts;
    @Mock private ViewGinjector mockedViewGinjector;
    private final String header = "Header Text";

    private View createView;
    private PresenterCreateImpl presenterCreateImpl;

    //------------------------------------------------------------------------------------------------------------------

    @Before
    public void setupView() {
        when(mockedClientFactory.getFlowStoreProxyAsync()).thenReturn(mockedFlowStoreProxy);
        when(mockedClientFactory.getSinkModifyTexts()).thenReturn(mockedTexts);
        when(mockedClientFactory.getMenuTexts()).thenReturn(mockedMenuTexts);
        when(mockedMenuTexts.menu_SinkCreation()).thenReturn(header);
        createView = new View(); // GwtMockito automagically populates mocked versions of all UiFields in the view
        when(mockedViewGinjector.getView()).thenReturn(createView);
    }

    //------------------------------------------------------------------------------------------------------------------


    @Test
    public void constructor_instantiate_objectCorrectInitialized() {
        presenterCreateImpl = new PresenterCreateImpl(mockedClientFactory, header);
        // The instanitation of presenterCreateImpl instantiates the "Create version" of the presenter - and the basic test has been done in the test of PresenterImpl
        // Therefore, we only intend to test the Create specific stuff, which basically is to assert, that the view attribute has been initialized correctly
    }

    @Test
    public void initializeModel_callPresenterStart_modelIsInitializedCorrectly() {

        // Setup expectations
        presenterCreateImpl = new PresenterCreateImpl(mockedClientFactory, header);
        presenterCreateImpl.injector = mockedViewGinjector;

        // Subject Under Test
        assertThat(presenterCreateImpl.model, is(notNullValue()));
        presenterCreateImpl.start(mockedContainerWidget, mockedEventBus);  // Calls initializeModel

        assertThat(presenterCreateImpl.model.getSinkName(), is(""));
        assertThat(presenterCreateImpl.model.getResourceName(), is(""));
        assertThat(presenterCreateImpl.model.getDescription(), is(""));
    }

    @Test
    public void saveModel_sinkContentOk_createSinkCalled() {

        // Setup expectations
        presenterCreateImpl = new PresenterCreateImpl(mockedClientFactory, header);
        presenterCreateImpl.injector = mockedViewGinjector;

        // Subject Under Test
        presenterCreateImpl.start(mockedContainerWidget, mockedEventBus);
        presenterCreateImpl.model = new SinkModelBuilder().build();
        presenterCreateImpl.saveModel();
        verify(mockedFlowStoreProxy).createSink(eq(presenterCreateImpl.model), any(PresenterImpl.SaveSinkModelFilteredAsyncCallback.class));
    }

}