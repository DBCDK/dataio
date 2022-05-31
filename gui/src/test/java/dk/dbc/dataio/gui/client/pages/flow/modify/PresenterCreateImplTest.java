package dk.dbc.dataio.gui.client.pages.flow.modify;

import com.google.gwt.place.shared.PlaceController;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.modelBuilders.FlowComponentModelBuilder;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * PresenterCreateImpl unit tests
 * <p>
 * The test methods of this class uses the following naming convention:
 * <p>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class PresenterCreateImplTest extends PresenterImplTestBase {

    @Mock
    Texts mockedTexts;
    @Mock
    ViewGinjector mockedViewGinjector;

    private ViewWidget createView;

    private PresenterCreateImplConcrete presenterCreateImpl;

    private final static String INPUT_FIELD_VALIDATION_ERROR = "InputFieldValidationError";

    class PresenterCreateImplConcrete extends PresenterCreateImpl {
        public PresenterCreateImplConcrete(PlaceController placeController, String header) {
            super(placeController, header);
        }

        public ViewWidget getView() {
            return createView;
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    @Before
    public void setup() {
        when(mockedCommonGinjector.getFlowStoreProxyAsync()).thenReturn(mockedFlowStore);
        when(mockedCommonGinjector.getMenuTexts()).thenReturn(mockedMenuTexts);
        when(mockedViewGinjector.getView()).thenReturn(createView);
        createView = new ViewWidget();  // GwtMockito automagically populates mocked versions of all UiFields in the view
    }

    //------------------------------------------------------------------------------------------------------------------


    @Test
    public void constructor_instantiate_objectCorrectInitialized() {
        presenterCreateImpl = new PresenterCreateImplConcrete(mockedPlaceController, header);
        // The instanitation of presenterCreateImpl instantiates the "Create version" of the presenter - and the basic test has been done in the test of PresenterImpl
        // Therefore, we only intend to test the Create specific stuff, which basically is to assert, that the view attribute has been initialized correctly
    }

    @Test
    public void initializeModel_callPresenterStart_modelIsInitializedCorrectly() {
        setupPresenterAndStart();

        // Verify, that the model is cleared and updated accordingly
        assertThat(createView.model, is(notNullValue()));
        assertThat(createView.model.getFlowName(), is(""));
        assertThat(createView.model.getDescription(), is(""));
        assertThat(createView.model.getFlowComponents().size(), is(0));

        // Verify, that the view is updated accordingly
        verify(createView.name).setText("");
        verify(createView.name).setEnabled(true);
        verify(createView.description).setText("");
        verify(createView.description).setEnabled(true);
        verify(createView.flowComponents, times(2)).clear();
        verify(createView.flowComponents).setEnabled(false);
        assertThat(createView.showAvailableFlowComponents, is(false));
        verifyNoMoreInteractions(createView.flowComponents);  // To make sure, that there are no addValue() calls
    }


    @Test
    public void saveModel_flowContentOk_createFlowCalled() {
        final String FLOW_NAME = "flow name";
        final String DESCRIPTION = "description";
        final long FLOW_COMPONENT_ID = 534;
        final String FLOW_COMPONENT_NAME = "flow component name";

        setupPresenterAndStart();

        presenterCreateImpl.nameChanged(FLOW_NAME);
        presenterCreateImpl.descriptionChanged(DESCRIPTION);
        Map<String, String> flowComponents = new HashMap<>();
        flowComponents.put(String.valueOf(FLOW_COMPONENT_ID), FLOW_COMPONENT_NAME);
        presenterCreateImpl.availableFlowComponentModels = Collections.singletonList(new FlowComponentModelBuilder().setId(FLOW_COMPONENT_ID).setName(FLOW_COMPONENT_NAME).build());
        presenterCreateImpl.flowComponentsChanged(flowComponents);

        presenterCreateImpl.saveModel();

        verify(mockedCommonGinjector.getFlowStoreProxyAsync()).createFlow(eq(createView.model), any(PresenterImpl.SaveFlowModelAsyncCallback.class));
    }

    private void setupPresenterAndStart() {
        presenterCreateImpl = new PresenterCreateImplConcrete(mockedPlaceController, header);
        presenterCreateImpl.start(mockedContainerWidget, mockedEventBus);  // Calls initializeModel
        presenterCreateImpl.viewInjector = mockedViewGinjector;
        presenterCreateImpl.commonInjector = mockedCommonGinjector;
    }
}
