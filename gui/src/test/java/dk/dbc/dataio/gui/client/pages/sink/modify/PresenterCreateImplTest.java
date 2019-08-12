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

import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.commons.types.EsSinkConfig;
import dk.dbc.dataio.commons.types.OpenUpdateSinkConfig;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.WorldCatSinkConfig;
import dk.dbc.dataio.gui.client.modelBuilders.SinkModelBuilder;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
public class PresenterCreateImplTest extends PresenterImplTestBase {

    @Mock private Texts mockedTexts;
    @Mock private ViewGinjector mockedViewGinjector;

    private View createView;
    private PresenterCreateImpl presenterCreateImpl;

    //------------------------------------------------------------------------------------------------------------------

    @Before
    public void setupView() {
        when(mockedCommonGinjector.getFlowStoreProxyAsync()).thenReturn(mockedFlowStore);
        when(mockedCommonGinjector.getMenuTexts()).thenReturn(mockedMenuTexts);
        when(mockedMenuTexts.menu_SinkCreation()).thenReturn(header);
        createView = new View(); // GwtMockito automagically populates mocked versions of all UiFields in the view
        when(mockedViewGinjector.getView()).thenReturn(createView);
    }

    //------------------------------------------------------------------------------------------------------------------


    @Test
    public void constructor_instantiate_objectCorrectInitialized() {

        // Subject Under Test
        presenterCreateImpl = new PresenterCreateImpl(header);
        // The instanitation of presenterCreateImpl instantiates the "Create version" of the presenter - and the basic test has been done in the test of PresenterImpl
        // Therefore, we only intend to test the Create specific stuff, which basically is to assert, that the view attribute has been initialized correctly
    }

    @Test
    public void initializeModel_callPresenterStart_modelIsInitializedCorrectly() {

        // Setup expectations
        setupPresenterCreateImpl();

        // Subject Under Test
        assertThat(presenterCreateImpl.model, is(notNullValue()));
        presenterCreateImpl.start(mockedContainerWidget, mockedEventBus);  // Calls initializeModel

        // Verifications
        assertThat(presenterCreateImpl.model.getSinkName(), is(""));
        assertThat(presenterCreateImpl.model.getResourceName(), is(""));
        assertThat(presenterCreateImpl.model.getDescription(), is(""));
    }

    @Test
    public void saveModel_sinkContentOk_createSinkCalled() {

        // Setup expectations
        setupPresenterCreateImpl();

        // Subject Under Test
        presenterCreateImpl.start(mockedContainerWidget, mockedEventBus);
        presenterCreateImpl.model = new SinkModelBuilder().build();
        presenterCreateImpl.saveModel();

        // Verifications
        verify(mockedFlowStore).createSink(eq(presenterCreateImpl.model), any(PresenterImpl.SaveSinkModelFilteredAsyncCallback.class));
    }

    @Test
    public void handleSinkConfig_sinkTypeDummy_sinkConfigIsNull() {
        // Setup expectations
        setupPresenterCreateImpl();

        // Subject Under Test
        presenterCreateImpl.handleSinkConfig(SinkContent.SinkType.DUMMY);

        // Verifications
        assertThat(presenterCreateImpl.model.getSinkConfig(), is(nullValue()));
    }

    @Test
    public void handleSinkConfig_sinkTypeEs_EsSinkConfigAdded() {
        // Setup expectations
        setupPresenterCreateImpl();

        // Subject Under Test
        presenterCreateImpl.handleSinkConfig(SinkContent.SinkType.ES);

        // Verifications
        assertThat(presenterCreateImpl.model.getSinkConfig(), is(new EsSinkConfig()));
    }

    @Test
    public void handleSinkConfig_sinkTypeOpenUpdate_OpenUpdateSinkConfigAdded() {
        // Setup expectations
        setupPresenterCreateImpl();

        // Subject Under Test
        presenterCreateImpl.handleSinkConfig(SinkContent.SinkType.OPENUPDATE);

        // Verifications
        assertThat(presenterCreateImpl.model.getSinkConfig(), is(new OpenUpdateSinkConfig()));
    }

    @Test
    public void handleSinkConfig_sinkTypeWorldCat_WorldCatSinkConfigAdded() {
        // Setup expectations
        setupPresenterCreateImpl();

        // Subject Under Test
        presenterCreateImpl.handleSinkConfig(SinkContent.SinkType.WORLDCAT);

        // Verifications
        assertThat(presenterCreateImpl.model.getSinkConfig(), is(new WorldCatSinkConfig()));
    }

    private void setupPresenterCreateImpl() {
        presenterCreateImpl = new PresenterCreateImpl(header);
        presenterCreateImpl.viewInjector = mockedViewGinjector;
        presenterCreateImpl.commonInjector = mockedCommonGinjector;
    }
}