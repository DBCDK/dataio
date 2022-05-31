package dk.dbc.dataio.gui.client.pages.flowbinder.modify;

import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.commons.types.RecordSplitterConstants;
import dk.dbc.dataio.gui.client.model.FlowBinderModel;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
    private Texts mockedTexts;
    @Mock
    dk.dbc.dataio.gui.client.pages.navigation.Texts mockedMenuTexts;
    @Mock
    ViewGinjector mockedViewInjector;

    private View createView;
    private PresenterCreateImpl presenterCreateImpl;
    private final static String INPUT_FIELD_VALIDATION_ERROR = "InputFieldValidationError";

    //------------------------------------------------------------------------------------------------------------------

    @Before
    public void setupMockedObjects() {
        when(mockedCommonGinjector.getFlowStoreProxyAsync()).thenReturn(mockedFlowStore);
        when(mockedViewInjector.getView()).thenReturn(createView);
        when(mockedViewInjector.getTexts()).thenReturn(mockedTexts);
        when(mockedTexts.error_InputFieldValidationError()).thenReturn(INPUT_FIELD_VALIDATION_ERROR);
    }

    @Before
    public void setupView() {
        when(mockedCommonGinjector.getMenuTexts()).thenReturn(mockedMenuTexts);
        when(mockedMenuTexts.menu_FlowBinderCreation()).thenReturn("Header Text");
        createView = new View();  // GwtMockito automagically populates mocked versions of all UiFields in th view
    }

    //------------------------------------------------------------------------------------------------------------------


    @Test
    public void constructor_instantiate_objectCorrectInitialized() {
        setupPresenterImpl();
        // The instanitation of presenterCreateImpl instantiates the "Create version" of the presenter - and the basic test has been done in the test of PresenterImpl
        // Therefore, we only intend to test the Create specific stuff, which basically is to assert, that the view attribute has been initialized correctly

    }

    @Test
    public void initializeModel_callPresenterStart_modelIsInitializedCorrectly() {
        setupPresenterImpl();
        assertThat(presenterCreateImpl.model, is(notNullValue()));
        assertThat(presenterCreateImpl.model.getName(), is(""));
        presenterCreateImpl.start(mockedContainerWidget, mockedEventBus);  // Calls initializeModel

        assertThat(presenterCreateImpl.model, is(notNullValue()));
        assertThat(presenterCreateImpl.model.getName(), is(""));
        assertThat(presenterCreateImpl.model.getDescription(), is(""));
        assertThat(presenterCreateImpl.model.getPackaging(), is(""));
        assertThat(presenterCreateImpl.model.getFormat(), is(""));
        assertThat(presenterCreateImpl.model.getCharset(), is(""));
        assertThat(presenterCreateImpl.model.getDestination(), is(""));
        assertThat(presenterCreateImpl.model.getPriority(), is(Priority.NORMAL.getValue()));
        assertThat(presenterCreateImpl.model.getRecordSplitter(), is(RecordSplitterConstants.getRecordSplitters().get(0).name()));
        assertThat(presenterCreateImpl.model.getFlowModel(), is(notNullValue()));
        assertThat(presenterCreateImpl.model.getFlowModel().getFlowName(), is(""));
        assertThat(presenterCreateImpl.model.getSubmitterModels(), is(notNullValue()));
        assertThat(presenterCreateImpl.model.getSubmitterModels().size(), is(0));
        assertThat(presenterCreateImpl.model.getSinkModel(), is(notNullValue()));
        assertThat(presenterCreateImpl.model.getSinkModel().getSinkName(), is(""));
    }

    @Test
    public void saveModel_flowBinderOk_createFlowBinderCalled() {
        setupPresenterImpl();
        presenterCreateImpl.start(mockedContainerWidget, mockedEventBus);
        presenterCreateImpl.model = new FlowBinderModel();
        presenterCreateImpl.saveModel();

        verify(mockedFlowStore).createFlowBinder(eq(presenterCreateImpl.model), any(PresenterImpl.SaveFlowBinderModelFilteredAsyncCallback.class));
    }

    private void setupPresenterImpl() {
        presenterCreateImpl = new PresenterCreateImpl(header);
        presenterCreateImpl.commonInjector = mockedCommonGinjector;
        presenterCreateImpl.viewInjector = mockedViewInjector;
    }
}
