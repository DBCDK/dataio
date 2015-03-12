package dk.dbc.dataio.gui.client.pages.flow.modify;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(GwtMockitoTestRunner.class)
public class PresenterEditImplTest {
    @Mock ClientFactory mockedClientFactory;
    @Mock FlowStoreProxyAsync mockedFlowStoreProxy;
    @Mock Texts mockedTexts;
    @Mock AcceptsOneWidget mockedContainerWidget;
    @Mock EventBus mockedEventBus;
    @Mock EditPlace mockedEditPlace;

    private View view;

    private PresenterEditImpl presenterEditImpl;

    private PresenterEditImplConcrete presenterEditImplConcrete;

    class PresenterEditImplConcrete extends PresenterEditImpl {
        public PresenterEditImplConcrete(Place place, ClientFactory clientFactory, Texts constants) {
            super(place, clientFactory, constants);
        }

        public GetFlowModelAsyncCallback callback = new GetFlowModelAsyncCallback();
    }

    //------------------------------------------------------------------------------------------------------------------

    @Before
    public void setupMockedObjects() {
        when(mockedClientFactory.getFlowStoreProxyAsync()).thenReturn(mockedFlowStoreProxy);
        when(mockedClientFactory.getFlowEditView()).thenReturn(view);
    }

    @Before
    public void setupView() {
        view = new View("Header Text");  // GwtMockito automagically populates mocked versions of all UiFields in the view
    }

    //------------------------------------------------------------------------------------------------------------------

    @Test
    public void constructor_instantiate_objectCorrectInitialized() {
        presenterEditImpl = new PresenterEditImpl(mockedEditPlace, mockedClientFactory, mockedTexts);
        verify(mockedEditPlace).getFlowId();
        verify(mockedClientFactory).getFlowEditView();
    }

    @Test
    public void initializeModel_callPresenterStart_getFlowIsInvoked() {
        presenterEditImpl = new PresenterEditImpl(mockedEditPlace, mockedClientFactory, mockedTexts);
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);  // Calls initializeModel
        verify(mockedFlowStoreProxy).getFlow(any(Long.class), any(PresenterEditImpl.SaveFlowModelAsyncCallback.class));
    }

    @Test
    public void saveModel_flowContentOk_updateFlowCalled() {
        presenterEditImpl = new PresenterEditImpl(mockedEditPlace, mockedClientFactory, mockedTexts);
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);
        presenterEditImpl.model = new FlowModel();

        presenterEditImpl.availableFlowComponentModels = new ArrayList<FlowComponentModel>();
        presenterEditImpl.availableFlowComponentModels.add(getValidFlowComponentModel(1, 2));

        Map<String, String> flowModelMap = new HashMap<String, String>();
        flowModelMap.put(Long.toString(presenterEditImpl.availableFlowComponentModels.get(0).getId()), presenterEditImpl.availableFlowComponentModels.get(0).getName());

        presenterEditImpl.nameChanged("a");                                     // Name is ok
        presenterEditImpl.descriptionChanged("Changed Description");            // Description is ok
        presenterEditImpl.flowComponentsChanged(flowModelMap);                  // FlowComponents are ok

        presenterEditImpl.saveModel();

        verify(mockedFlowStoreProxy).updateFlow(eq(presenterEditImpl.model), any(PresenterImpl.SaveFlowModelAsyncCallback.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getFlowModelFilteredAsyncCallback_successfulCallback_modelIsInitializedCorrectly() {
        presenterEditImplConcrete = new PresenterEditImplConcrete(mockedEditPlace, mockedClientFactory, mockedTexts);
        presenterEditImplConcrete.start(mockedContainerWidget, mockedEventBus);
        FlowModel model = getValidFlowModel(4, 5);
        assertThat(presenterEditImplConcrete.model, is(nullValue()));

        presenterEditImplConcrete.callback.onSuccess(model);  // Emulate a successful callback from flowstore

        // Assert that the sink model has been updated correctly
        assertThat(presenterEditImplConcrete.model, is(notNullValue()));
        assertThat(presenterEditImplConcrete.model.getId(), is(model.getId()));
        assertThat(presenterEditImplConcrete.model.getVersion(), is(model.getVersion()));
        assertThat(presenterEditImplConcrete.model.getFlowName(), is(model.getFlowName()));
        assertThat(presenterEditImplConcrete.model.getDescription(), is(model.getDescription()));

        assertFlowComponentModelsEquals(presenterEditImplConcrete.model.getFlowComponents(), model.getFlowComponents());

        // Assert that the view is displaying the correct values
        verify(view.name).setText(model.getFlowName());
        verify(view.name).setEnabled(true);
        verify(view.description).setText(model.getDescription());
        verify(view.description).setEnabled(true);
        verify(view.flowComponents, times(2)).clear();
        verify(view.flowComponents).addValue("name", "4");
        verify(view.flowComponents).setEnabled(false);
    }

    @Test
    public void getFlowModelFilteredAsyncCallback_unsuccessfulCallback_setErrorTextCalledInView() {
        presenterEditImplConcrete = new PresenterEditImplConcrete(mockedEditPlace, mockedClientFactory, mockedTexts);
        presenterEditImplConcrete.start(mockedContainerWidget, mockedEventBus);

        // Emulate an unsuccessful callback from flowstore
        presenterEditImplConcrete.callback.onFilteredFailure(new Throwable("Fejl"));
        // Expect the error text to be set in View
        verify(mockedTexts).error_CannotFetchFlow();
    }

    private FlowComponentModel getValidFlowComponentModel(long id, long version) {
        return new FlowComponentModel(id, version, "name", "svnProject", "34", "invocationJavaScript", "invocationMethod", Arrays.asList("JavaScriptModuleName"));
    }

    private FlowModel getValidFlowModel(long id, long version) {
        return new FlowModel(id, version, "name", "Description", Arrays.asList(getValidFlowComponentModel(id, version)));
    }

    private void assertFlowComponentModelsEquals(List<FlowComponentModel> flowComponentModelList1, List<FlowComponentModel> flowComponentModelList2) {
        assertThat(flowComponentModelList1.size(), is(flowComponentModelList2.size()));
        for(int i = 0; i < flowComponentModelList1.size(); i ++) {
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
        for(int i = 0; i < javaScriptModules1.size(); i++) {
            assertThat(javaScriptModules1.get(i), is(javaScriptModules2.get(i)));
        }
    }

}
