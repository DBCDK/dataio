package dk.dbc.dataio.gui.client.pages.job.modify;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.components.log.LogPanel;
import dk.dbc.dataio.gui.client.exceptions.texts.LogMessageTexts;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import dk.dbc.dataio.gui.client.views.ContentPanel;
import dk.dbc.dataio.gui.server.jobrerun.JobRerunScheme;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dk.dbc.dataio.gui.client.views.ContentPanel.GUIID_CONTENT_PANEL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(GwtMockitoTestRunner.class)
public class PresenterEditImplTest extends PresenterImplTestBase {

    @Mock
    private EditPlace mockedEditPlace;
    @Mock
    private ViewGinjector mockedViewGinjector;
    @Mock
    private ContentPanel mockedContentPanel;
    @Mock
    private Element mockedElement;
    @Mock
    private LogMessageTexts mockedLogMessageTexts;
    @Mock
    private LogPanel mockedLogPanel;

    private PresenterEditImpl presenterEditImpl;
    private final static String MOCKED_LOG_ALL_ITEMS = "mocked log_allItems()";

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
        when(mockedCommonGinjector.getMenuTexts()).thenReturn(mockedMenuTexts);
        when(mockedViewGinjector.getLogMessageTexts()).thenReturn(mockedLogMessageTexts);
        View editView = new View();
        when(mockedViewGinjector.getView()).thenReturn(editView);
        when(mockedEditPlace.getParameter(EditPlace.JOB_ID)).thenReturn("42");
        when(mockedEditPlace.getParameter(EditPlace.FAILED_ITEMS_ONLY)).thenReturn("false");
        when(Document.get().getElementById(eq(GUIID_CONTENT_PANEL))).thenReturn(mockedElement);
        when(mockedElement.getPropertyObject(eq(GUIID_CONTENT_PANEL))).thenReturn(mockedContentPanel);
        when(mockedContentPanel.getLogPanel()).thenReturn(mockedLogPanel);
        when(mockedLogMessageTexts.log_allItems()).thenReturn(MOCKED_LOG_ALL_ITEMS);
    }

    @Test
    public void constructor_instantiate_objectCorrectInitialized() {

        // Subject Under Test
        setupPresenterEditImpl();

        // Verifications
        verify(mockedEditPlace).getParameter(EditPlace.JOB_ID);
        verify(mockedEditPlace).getParameter(EditPlace.FAILED_ITEMS_ONLY);
        // The instantiation of presenterEditImpl instantiates the "Edit version" of the presenter - and the basic test has been done in the test of PresenterImpl
        // Therefore, we only intend to test the Edit specific stuff, which basically is to assert, that the view attribute has been initialized correctly
    }

    @Test
    @SuppressWarnings("unchecked")
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
    @SuppressWarnings("unchecked")
    public void doReSubmitJobInJobStore_allItems_createJobRerunCalled() {

        // Expectations
        when(mockedEditPlace.getParameter(EditPlace.FAILED_ITEMS_ONLY)).thenReturn("false");
        setupPresenterEditImpl();
        presenterEditImpl.jobRerunScheme = new JobRerunScheme()
                .withType(JobRerunScheme.Type.ORIGINAL_FILE)
                .withActions(Stream.of(JobRerunScheme.Action.RERUN_ALL).collect(Collectors.toCollection(HashSet::new)));

        // Subject Under Test
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);
        presenterEditImpl.jobModel = new JobModel();

        presenterEditImpl.doReSubmitJobInJobStore();

        // Verifications
        verify(mockedJobStore).createJobRerun(anyInt(), eq(false), any(PresenterEditImpl.CreateJobRerunAsyncCallback.class));
    }


    @Test
    @SuppressWarnings("unchecked")
    public void doReSubmitJobInJobStore_failedOnly_createJobRerun() {

        // Expectations
        when(mockedEditPlace.getParameter(EditPlace.FAILED_ITEMS_ONLY)).thenReturn("true");
        setupPresenterEditImpl();
        presenterEditImpl.jobRerunScheme = new JobRerunScheme()
                .withType(JobRerunScheme.Type.ORIGINAL_FILE)
                .withActions(Stream.of(JobRerunScheme.Action.RERUN_ALL, JobRerunScheme.Action.RERUN_FAILED).collect(Collectors.toCollection(HashSet::new)));


        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);
        presenterEditImpl.jobModel = new JobModel();

        // Subject under test
        presenterEditImpl.doReSubmitJobInJobStore();

        // Verifications
        verify(mockedJobStore).createJobRerun(anyInt(), eq(true), any(PresenterEditImpl.CreateJobRerunAsyncCallback.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void doReSubmitJobInJobStore_fatalError_reSubmitJob() {

        // Expectations
        setupPresenterEditImpl();
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);
        presenterEditImpl.jobModel.withDiagnosticFatal(true);
        presenterEditImpl.jobRerunScheme = new JobRerunScheme()
                .withType(JobRerunScheme.Type.ORIGINAL_FILE)
                .withActions(Stream.of(JobRerunScheme.Action.COPY).collect(Collectors.toCollection(HashSet::new)));

        // Subject under test
        presenterEditImpl.doReSubmitJobInJobStore();

        // Verifications
        verify(mockedJobStore).reSubmitJob(eq(presenterEditImpl.jobModel), any(PresenterEditImpl.ReSubmitJobFilteredAsyncCallback.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void doReSubmitJobInJobStore_preview_reSubmitJob() {

        // Expectations
        setupPresenterEditImpl();
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);
        presenterEditImpl.jobModel.withNumberOfChunks(0);
        presenterEditImpl.jobModel.withNumberOfItems(1);
        presenterEditImpl.jobRerunScheme = new JobRerunScheme()
                .withType(JobRerunScheme.Type.ORIGINAL_FILE)
                .withActions(Stream.of(JobRerunScheme.Action.COPY).collect(Collectors.toCollection(HashSet::new)));

        // Subject under test
        presenterEditImpl.doReSubmitJobInJobStore();

        // Verifications
        verify(mockedJobStore).reSubmitJob(eq(presenterEditImpl.jobModel), any(PresenterEditImpl.ReSubmitJobFilteredAsyncCallback.class));
    }

    private void setupPresenterEditImpl() {
        presenterEditImpl = new PresenterEditImpl(mockedEditPlace, header);
        presenterEditImpl.viewInjector = mockedViewGinjector;
        presenterEditImpl.commonInjector = mockedCommonGinjector;
    }
}
