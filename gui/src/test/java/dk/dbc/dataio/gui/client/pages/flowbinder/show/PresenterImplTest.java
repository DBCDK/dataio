package dk.dbc.dataio.gui.client.pages.flowbinder.show;


import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.model.FlowBinderModel;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.modelBuilders.FlowBinderModelBuilder;
import dk.dbc.dataio.gui.client.modelBuilders.FlowComponentModelBuilder;
import dk.dbc.dataio.gui.client.modelBuilders.FlowModelBuilder;
import dk.dbc.dataio.gui.client.modelBuilders.SinkModelBuilder;
import dk.dbc.dataio.gui.client.modelBuilders.SubmitterModelBuilder;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import dk.dbc.dataio.gui.client.pages.flowbinder.modify.CreatePlace;
import dk.dbc.dataio.gui.client.pages.flowbinder.modify.EditPlace;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * PresenterImpl unit tests
 * <p>
 * The test methods of this class uses the following naming convention:
 * <p>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class PresenterImplTest extends PresenterImplTestBase {

    @Mock
    View mockedView;
    @Mock
    FlowBindersTable mockedFlowBindersTable;
    @Mock
    Widget mockedViewWidget;
    @Mock
    SingleSelectionModel<FlowBinderModel> mockedSelectionModel;
    @Mock
    ListDataProvider<FlowBinderModel> mockedDataProvider;
    @Mock
    ViewGinjector mockedViewGinjector;

    // Setup mocked data
    @Before
    public void setupMockedData() {
        when(mockedCommonGinjector.getFlowStoreProxyAsync()).thenReturn(mockedFlowStore);
        when(mockedViewGinjector.getView()).thenReturn(mockedView);
        when(mockedView.asWidget()).thenReturn(mockedViewWidget);
        when(mockedCommonGinjector.getProxyErrorTexts()).thenReturn(mockedProxyErrorTexts);
        mockedView.flowBindersTable = mockedFlowBindersTable;
        mockedView.flowBindersTable.setSelectionModel(mockedSelectionModel);
        when(mockedView.flowBindersTable.getSelectionModel()).thenReturn(mockedSelectionModel);
        mockedView.flowBindersTable.dataProvider = mockedDataProvider;
    }

    // Subject Under Test
    private PresenterImplConcrete presenterImpl;


    // Test specialization of Presenter to enable test of callback's
    class PresenterImplConcrete extends PresenterImpl {
        public PresenterImplConcrete(PlaceController placeController, String header) {
            super(placeController, new View(), header);
            viewInjector = mockedViewGinjector;
            commonInjector = mockedCommonGinjector;
        }

        public FetchFlowBindersCallback fetchFlowBindersCallback = new FetchFlowBindersCallback();
    }

    // Test Data
    private FlowComponentModel flowComponentModel1 = new FlowComponentModelBuilder().build();
    private FlowModel flowModel1 = new FlowModelBuilder().setComponents(Collections.singletonList(flowComponentModel1)).build();
    private SinkModel sinkModel1 = new SinkModelBuilder().setName("SInam1").build();

    private FlowBinderModel flowBinderModel1 = new FlowBinderModelBuilder()
            .setName("FBnam1")
            .setFlowModel(flowModel1)
            .setSubmitterModels(Collections.singletonList(new SubmitterModelBuilder().build()))
            .setSinkModel(sinkModel1)
            .build();

    private FlowBinderModel flowBinderModel2 = new FlowBinderModelBuilder()
            .setName("FBnam2")
            .setFlowModel(flowModel1)
            .setSubmitterModels(Collections.singletonList(new SubmitterModelBuilder().build()))
            .setSinkModel(sinkModel1)
            .build();

    private List<FlowBinderModel> flowBinderModels = Arrays.asList(flowBinderModel1, flowBinderModel2);


    @Test
    public void constructor_instantiate_objectCorrectInitialized() {
        // Test Subject Under Test
        setupPresenterImplConcrete();

    }

    @Test
    public void editFlowBinder_call_gotoEditPlace() {

        // Setup
        setupPresenterImplConcrete();

        // Subject Under Test
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.editFlowBinder(flowBinderModel1);

        // Verify Test
        verify(mockedPlaceController).goTo(any(EditPlace.class));
    }

    @Test
    public void createFlowBinder_call_gotoCreatePlace() {

        // Setup
        setupPresenterImplConcrete();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Subject Under Test
        presenterImpl.createFlowBinder();

        // Verify Test
        verify(mockedPlaceController).goTo(any(CreatePlace.class));
    }

    @Test
    public void fetchFlowBinders_callbackWithSuccess_flowBindersAreFetchedInitialCallback() {

        // Setup
        setupPresenterImplConcrete();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.fetchFlowBindersCallback.onSuccess(flowBinderModels);

        // Verify Test
        assertThat(presenterImpl.view.flowBindersTable.dataProvider.getList(), is(flowBinderModels));
    }

    @Test
    public void fetchFlowBinders_callbackWithSuccess_flowBindersAreFetchedNoChanges() {

        // Setup
        setupPresenterImplConcrete();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        when(mockedDataProvider.getList()).thenReturn(flowBinderModels);

        // Test Subject Under Test
        presenterImpl.fetchFlowBindersCallback.onSuccess(flowBinderModels);

        // Verify Test
        verifyNoInteractions(mockedSelectionModel);
        assertThat(presenterImpl.view.flowBindersTable.dataProvider.getList(), is(flowBinderModels));
    }

    private void setupPresenterImplConcrete() {
        presenterImpl = new PresenterImplConcrete(mockedPlaceController, header);
    }
}
