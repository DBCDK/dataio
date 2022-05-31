package dk.dbc.dataio.gui.client.pages.flow.modify;

import com.google.gwt.place.shared.PlaceController;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.modelBuilders.FlowComponentModelBuilder;
import dk.dbc.dataio.gui.client.modelBuilders.FlowModelBuilder;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(GwtMockitoTestRunner.class)
public class PresenterEditImplTest extends PresenterImplTestBase {

    @Mock
    Texts mockedTexts;
    @Mock
    EditPlace mockedEditPlace;
    @Mock
    ViewGinjector mockedViewGinjector;

    private ViewWidget editView;

    private PresenterEditImplConcrete presenterEditImplConcrete;

    class PresenterEditImplConcrete extends PresenterEditImpl {
        public PresenterEditImplConcrete(EditPlace place, PlaceController placeController, String header) {
            super(place, placeController, header);
            this.commonInjector = mockedCommonGinjector;
            this.viewInjector = mockedViewGinjector;
        }

        public GetFlowModelAsyncCallback callback = new GetFlowModelAsyncCallback();

        public ViewWidget getView() {
            return editView;
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    @Before
    public void setupView() {
        when(mockedCommonGinjector.getFlowStoreProxyAsync()).thenReturn(mockedFlowStore);
        when(mockedViewGinjector.getView()).thenReturn(editView);
        when(mockedViewGinjector.getTexts()).thenReturn(mockedTexts);
        when(mockedCommonGinjector.getProxyErrorTexts()).thenReturn(mockedProxyErrorTexts);
        when(mockedCommonGinjector.getMenuTexts()).thenReturn(mockedMenuTexts);
        when(mockedMenuTexts.menu_FlowEdit()).thenReturn("Header Text");
        editView = new ViewWidget();  // GwtMockito automagically populates mocked versions of all UiFields in the view
    }

    //------------------------------------------------------------------------------------------------------------------

    @Test
    public void constructor_instantiate_objectCorrectInitialized() {
        setupPresenterEditImpl();
        verify(mockedEditPlace).getFlowId();
    }


    @Test
    public void initializeModel_callPresenterStart_getFlowIsInvoked() {

        // Setup
        setupPresenterEditImpl();

        // Subject Under Test
        presenterEditImplConcrete.start(mockedContainerWidget, mockedEventBus);  // Calls initializeModel

        // Verifications
        verify(mockedCommonGinjector.getFlowStoreProxyAsync()).getFlow(any(Long.class), any(PresenterEditImpl.GetFlowModelAsyncCallback.class));
    }

    @Test
    public void saveModel_flowContentOk_updateFlowCalled() {

        // Setup
        setupPresenterEditImpl();

        // Subject Under Test
        presenterEditImplConcrete.start(mockedContainerWidget, mockedEventBus);

        // Verifications
        presenterEditImplConcrete.availableFlowComponentModels = new ArrayList<>();
        presenterEditImplConcrete.availableFlowComponentModels.add(new FlowComponentModelBuilder().setId(1).setVersion(2).build());

        Map<String, String> flowModelMap = new HashMap<>();
        flowModelMap.put(Long.toString(presenterEditImplConcrete.availableFlowComponentModels.get(0).getId()), presenterEditImplConcrete.availableFlowComponentModels.get(0).getName());

        presenterEditImplConcrete.nameChanged("a");                                     // Name is ok
        presenterEditImplConcrete.descriptionChanged("Changed Description");            // Description is ok
        presenterEditImplConcrete.flowComponentsChanged(flowModelMap);                  // FlowComponents are ok

        presenterEditImplConcrete.saveModel();

        verify(mockedCommonGinjector.getFlowStoreProxyAsync()).updateFlow(eq(editView.model), any(PresenterImpl.SaveFlowModelAsyncCallback.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getFlowModelFilteredAsyncCallback_successfulCallback_modelIsInitializedCorrectly() {

        // Setup
        setupPresenterEditImpl();

        // Subject Under Test
        presenterEditImplConcrete.start(mockedContainerWidget, mockedEventBus);
        FlowModel model = getValidFlowModel(4, 5);
        assertThat(editView.model, is(notNullValue()));

        presenterEditImplConcrete.callback.onSuccess(model);  // Emulate a successful callback from flowstore

        // Assert that the sink model has been updated correctly
        assertThat(editView.model, is(notNullValue()));
        assertThat(editView.model.getId(), is(model.getId()));
        assertThat(editView.model.getVersion(), is(model.getVersion()));
        assertThat(editView.model.getFlowName(), is(model.getFlowName()));
        assertThat(editView.model.getDescription(), is(model.getDescription()));
        assertThat(editView.showAvailableFlowComponents, is(false));

        assertFlowComponentModelsEquals(editView.model.getFlowComponents(), model.getFlowComponents());

        // Assert that the view is displaying the correct values
        verify(editView.name).setText(model.getFlowName());
        verify(editView.name).setEnabled(true);
        verify(editView.description).setText(model.getDescription());
        verify(editView.description).setEnabled(true);
        verify(editView.flowComponents, times(2)).clear();
        verify(editView.flowComponents).addValue("name", "4");
        verify(editView.flowComponents).setEnabled(false);
    }

    @Test
    public void getFlowModelFilteredAsyncCallback_unsuccessfulCallback_setErrorTextCalledInView() {

        // Setup
        setupPresenterEditImpl();

        // Subject Under Test
        presenterEditImplConcrete.start(mockedContainerWidget, mockedEventBus);

        ProxyException mockedProxyException = mock(ProxyException.class);
        when(mockedProxyException.getErrorCode()).thenReturn(ProxyError.ENTITY_NOT_FOUND);

        // Emulate an unsuccessful callback from flowstore
        presenterEditImplConcrete.callback.onFilteredFailure(mockedProxyException);
        // Expect the error text to be set in View
        verify(mockedProxyException).getErrorCode();
        verify(mockedProxyErrorTexts).flowStoreProxy_notFoundError();
    }

    @Test
    public void deleteFlowModelFilteredAsyncCallback_callback_invoked() {

        // Setup
        setupPresenterEditImpl();

        // Subject Under Test
        presenterEditImplConcrete.start(mockedContainerWidget, mockedEventBus);

        presenterEditImplConcrete.deleteModel();

        // Verify that the proxy call is invoked... Cannot emulate the callback as the return type is Void
        verify(mockedCommonGinjector.getFlowStoreProxyAsync()).deleteFlow(
                eq(editView.model.getId()),
                eq(editView.model.getVersion()),
                any(PresenterEditImpl.DeleteFlowModelFilteredAsyncCallback.class));
    }

    private FlowModel getValidFlowModel(long id, long version) {
        return new FlowModelBuilder().setId(id).setVersion(version)
                .setComponents(Collections.singletonList(new FlowComponentModelBuilder().setId(id).setVersion(version).build()))
                .build();
    }

    private void assertFlowComponentModelsEquals(List<FlowComponentModel> flowComponentModelList1, List<FlowComponentModel> flowComponentModelList2) {
        assertThat(flowComponentModelList1.size(), is(flowComponentModelList2.size()));
        for (int i = 0; i < flowComponentModelList1.size(); i++) {
            assertFlowComponentModelEquals(flowComponentModelList1.get(i), flowComponentModelList2.get(i));
        }
    }

    private void assertFlowComponentModelEquals(FlowComponentModel flowComponentModel1, FlowComponentModel flowComponentModel2) {
        assertThat(flowComponentModel1.getId(), is(flowComponentModel2.getId()));
        assertThat(flowComponentModel1.getVersion(), is(flowComponentModel2.getVersion()));
        assertThat(flowComponentModel1.getName(), is(flowComponentModel2.getName()));
        assertThat(flowComponentModel1.getSvnProject(), is(flowComponentModel2.getSvnProject()));
        assertThat(flowComponentModel1.getSvnRevision(), is(flowComponentModel2.getSvnRevision()));
        assertThat(flowComponentModel1.getInvocationJavascript(), is(flowComponentModel2.getInvocationJavascript()));
        assertThat(flowComponentModel1.getInvocationMethod(), is(flowComponentModel2.getInvocationMethod()));
        assertJavaScriptModulesEquals(flowComponentModel1.getJavascriptModules(), flowComponentModel2.getJavascriptModules());
    }

    private void assertJavaScriptModulesEquals(List<String> javaScriptModules1, List<String> javaScriptModules2) {
        assertThat(javaScriptModules1.size(), is(javaScriptModules2.size()));
        for (int i = 0; i < javaScriptModules1.size(); i++) {
            assertThat(javaScriptModules1.get(i), is(javaScriptModules2.get(i)));
        }
    }

    private void setupPresenterEditImpl() {
        presenterEditImplConcrete = new PresenterEditImplConcrete(mockedEditPlace, mockedPlaceController, header);
    }
}
