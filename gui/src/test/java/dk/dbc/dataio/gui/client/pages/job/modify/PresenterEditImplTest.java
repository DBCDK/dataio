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

package dk.dbc.dataio.gui.client.pages.job.modify;

import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(GwtMockitoTestRunner.class)
public class PresenterEditImplTest extends PresenterImplTestBase {

    @Mock private Texts mockedTexts;
    @Mock private EditPlace mockedEditPlace;
    @Mock private ViewGinjector mockedViewGinjector;

    private View editView;

    private PresenterEditImpl presenterEditImpl;

    class PresenterEditImplConcrete<Place extends EditPlace> extends PresenterEditImpl {

        public PresenterEditImplConcrete(Place place, String header) {
            super(place, header);
            this.commonInjector = mockedCommonGinjector;
            this.viewInjector = mockedViewGinjector;
        }
    }


    @Before
    public void setup() {

        when(mockedCommonGinjector.getJobStoreProxyAsync()).thenReturn(mockedJobStore);
        when(mockedCommonGinjector.getProxyErrorTexts()).thenReturn(mockedProxyErrorTexts);
//        when(mockedEditPlace.getSinkId()).thenReturn(DEFAULT_SINK_ID);
        when(mockedCommonGinjector.getMenuTexts()).thenReturn(mockedMenuTexts);
//        when(mockedMenuTexts.menu_SinkEdit()).thenReturn("Header Text");
        editView = new View(); // GwtMockito automagically populates mocked versions of all UiFields in the view
        when(mockedViewGinjector.getView()).thenReturn(editView);

    }

    @Test
    public void constructor_instantiate_objectCorrectInitialized() {

        // Subject Under Test
        setupPresenterEditImpl();

        // Verifications
        verify(mockedEditPlace).getJobId();
        // The instantiation of presenterEditImpl instantiates the "Edit version" of the presenter - and the basic test has been done in the test of PresenterImpl
        // Therefore, we only intend to test the Edit specific stuff, which basically is to assert, that the view attribute has been initialized correctly
    }

    @Test
    public void initializeModel_callPresenterStart_listItemsIsInvoked() {

        // Expectations
        setupPresenterEditImpl();

        // Subject Under Test
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);  // Calls initializeModel

        // initializeModel has the responsibility to setup the model in the presenter correctly
        // In this case, we expect the model to be initialized with the submitter values.
        verify(mockedJobStore).listJobs(any(JobListCriteria.class), any(PresenterEditImpl.GetJobModelFilteredAsyncCallback.class));
    }

    @Test
    public void RerunJob_jobModelContentOk_rerunJobCalled() {

        // Expectations
        setupPresenterEditImpl();

        // Subject Under Test
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);
        presenterEditImpl.jobModel = new JobModel();

        presenterEditImpl.packagingChanged("a");                   // Name is ok
        presenterEditImpl.formatChanged("b");
        presenterEditImpl.charsetChanged("c");
        presenterEditImpl.destinationChanged("d");

        presenterEditImpl.doRerunJobInJobStore();

        // Verifications
        verify(mockedJobStore).addJob(eq(presenterEditImpl.jobModel), any(PresenterEditImpl.RerunJobFilteredAsyncCallback.class));
    }

    private void setupPresenterEditImpl() {
        presenterEditImpl = new PresenterEditImpl(mockedEditPlace, header);
        presenterEditImpl.viewInjector = mockedViewGinjector;
        presenterEditImpl.commonInjector = mockedCommonGinjector;
    }
}