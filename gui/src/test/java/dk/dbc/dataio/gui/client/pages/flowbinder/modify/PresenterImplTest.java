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

package dk.dbc.dataio.gui.client.pages.flowbinder.modify;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.model.FlowBinderModel;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.model.SubmitterModel;
import dk.dbc.dataio.gui.client.modelBuilders.FlowBinderModelBuilder;
import dk.dbc.dataio.gui.client.modelBuilders.FlowModelBuilder;
import dk.dbc.dataio.gui.client.modelBuilders.SinkModelBuilder;
import dk.dbc.dataio.gui.client.modelBuilders.SubmitterModelBuilder;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * PresenterImpl unit tests
 * <p/>
 * The test methods of this class uses the following naming convention:
 * <p/>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class PresenterImplTest extends PresenterImplTestBase {

    @Mock private Texts mockedTexts;
    @Mock private ViewGinjector mockedViewInjector;

    private static boolean initializeModelHasBeenCalled;
    private static boolean saveModelHasBeenCalled;
    List<SubmitterModel> availableSubmitterModelList;

    private View view;
    private PresenterImplConcrete presenterImpl;

    private final FlowModel flowModel1 = new FlowModelBuilder().setId(101).build();
    private final FlowModel flowModel2 = new FlowModelBuilder().setId(102).build();
    private final FlowModel flowModel3 = new FlowModelBuilder().setId(103).build();
    private final SubmitterModel submitterModel1 = new SubmitterModelBuilder().setId(201L).setNumber("2201").setName("SName 1").build();
    private final SubmitterModel submitterModel2 = new SubmitterModelBuilder().setId(202L).setNumber("2202").setName("SName 2").build();
    private final SubmitterModel submitterModel3 = new SubmitterModelBuilder().setId(203L).setNumber("2203").setName("SName 3").build();
    private final SubmitterModel submitterModel4 = new SubmitterModelBuilder().setId(204L).setNumber("2204").setName("SName 4").build();
    private final SinkModel sinkModel1 = new SinkModelBuilder().setId(301L).setName("Snm1").build();
    private final SinkModel sinkModel2 = new SinkModelBuilder().setId(302L).setName("Snm2").build();
    private final SinkModel sinkModel3 = new SinkModelBuilder().setId(303L).setName("Snm3").build();

    class PresenterImplConcrete extends PresenterImpl {
        public PresenterImplConcrete(String header) {
            super(header);
            model = new FlowBinderModelBuilder().setFlowModel(flowModel1).setSubmitterModels(Collections.singletonList(submitterModel1)).setSinkModel(sinkModel1).build();
            setAvailableSubmitters(availableSubmitterModelList);
            setAvailableFlows(Arrays.asList(flowModel1, flowModel2, flowModel3));
            setAvailableSinks(Arrays.asList(sinkModel1, sinkModel2, sinkModel3));
            setAvailableRecordSplitters();
            initializeModelHasBeenCalled = false;
            saveModelHasBeenCalled = false;
        }

        @Override
        void initializeModel() {
            initializeModelHasBeenCalled = true;
        }

        @Override
        void saveModel() {
            saveModelHasBeenCalled = true;
        }

        @Override
        View getView() {
            return view;
        }

        // The following instances of the callback classes allows a unit test for each of them
        public FetchAvailableSubmittersCallback fetchAvailableSubmittersCallback = new FetchAvailableSubmittersCallback();
        public FetchAvailableFlowsCallback fetchAvailableFlowsCallback = new FetchAvailableFlowsCallback();
        public FetchAvailableSinksCallback fetchAvailableSinksCallback = new FetchAvailableSinksCallback();
        public SaveFlowBinderModelFilteredAsyncCallback saveFlowBinderCallback = new SaveFlowBinderModelFilteredAsyncCallback();

        // Test methods implemented for the test only

        @Override
        public void deleteButtonPressed() {
            deleteButtonPressed();
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    @Before
    public void setupMockedObjects() {
        view = new View();  // GwtMockito automagically populates mocked versions of all UiFields in the view
        availableSubmitterModelList = new ArrayList<>(4);
        availableSubmitterModelList.add(submitterModel1);
        availableSubmitterModelList.add(submitterModel2);
        availableSubmitterModelList.add(submitterModel3);
        availableSubmitterModelList.add(submitterModel4);
        when(mockedCommonGinjector.getFlowStoreProxyAsync()).thenReturn(mockedFlowStore);
        when(mockedViewInjector.getTexts()).thenReturn(mockedTexts);
        when(mockedViewInjector.getView()).thenReturn(view);
        when(mockedCommonGinjector.getProxyErrorTexts()).thenReturn(mockedProxyErrorTexts);
    }

    //------------------------------------------------------------------------------------------------------------------


    @Test
    public void constructor_instantiate_objectCorrectInitialized() {
        setupPresenterImpl();
        assertThat(presenterImpl.getTexts(), is(mockedTexts));
    }


    @Test
    public void start_instantiateAndCallStart_objectCorrectInitializedAndViewAndModelInitializedCorrectly() {
        setupPresenterImpl();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        verify(mockedContainerWidget).setWidget(any(IsWidget.class));
        verify(mockedCommonGinjector.getFlowStoreProxyAsync()).findAllSubmitters(any(FilteredAsyncCallback.class));
        verify(mockedCommonGinjector.getFlowStoreProxyAsync()).findAllFlows(any(FilteredAsyncCallback.class));
        verify(mockedCommonGinjector.getFlowStoreProxyAsync()).findAllSinks(any(FilteredAsyncCallback.class));
        assertThat(initializeModelHasBeenCalled, is(true));
        FlowBinderModel model = presenterImpl.model;
        Map<String, String> selectedSubmitterModel = new HashMap<>();
        SubmitterModel sModel = model.getSubmitterModels().get(0);
        selectedSubmitterModel.put(String.valueOf(sModel.getId()), sModel.getNumber() + " (" + sModel.getName() + ")");
    }

    @Test
    public void nameChanged_callNameChanged_nameIsChangedAccordingly() {
        final String CHANGED_NAME = "UpdatedName";

        initializeAndStartPresenter();

        presenterImpl.nameChanged(CHANGED_NAME);

        assertThat(presenterImpl.model.getName(), is(CHANGED_NAME));
    }

    @Test
    public void descriptionChanged_callDescriptionChanged_descriptionIsChangedAccordingly() {
        final String CHANGED_DESCRIPTION = "UpdatedDescription";

        initializeAndStartPresenter();

        presenterImpl.descriptionChanged(CHANGED_DESCRIPTION);

        assertThat(presenterImpl.model.getDescription(), is(CHANGED_DESCRIPTION));
    }

    @Test
    public void packagingChanged_callPackagingChanged_packagingIsChangedAccordingly() {
        final String CHANGED_PACKAGING = "UpdatedPackaging";

        initializeAndStartPresenter();

        presenterImpl.frameChanged(CHANGED_PACKAGING);

        assertThat(presenterImpl.model.getPackaging(), is(CHANGED_PACKAGING));
    }

    @Test
    public void formatChanged_callFormatChanged_formatIsChangedAccordingly() {
        final String CHANGED_FORMAT = "UpdatedFormat";

        initializeAndStartPresenter();

        presenterImpl.formatChanged(CHANGED_FORMAT);

        assertThat(presenterImpl.model.getFormat(), is(CHANGED_FORMAT));
    }

    @Test
    public void charsetChanged_callCharsetChanged_charsetIsChangedAccordingly() {
        final String CHANGED_CHARSET = "UpdatedCharset";

        initializeAndStartPresenter();

        presenterImpl.charsetChanged(CHANGED_CHARSET);

        assertThat(presenterImpl.model.getCharset(), is(CHANGED_CHARSET));
    }

    @Test
    public void destinationChanged_callDestinationChanged_destinationIsChangedAccordingly() {
        final String CHANGED_DESTINATION = "UpdatedDestination";

        initializeAndStartPresenter();

        presenterImpl.destinationChanged(CHANGED_DESTINATION);

        assertThat(presenterImpl.model.getDestination(), is(CHANGED_DESTINATION));
    }

    @Test
    public void recordSplitterChanged_callRecordSplitterChanged_recordSplitterIsChangedAccordingly() {
        final String CHANGED_RECORDSPLITTER = "UpdatedRecordSplitter";

        initializeAndStartPresenter();

        presenterImpl.recordSplitterChanged(CHANGED_RECORDSPLITTER);

        assertThat(presenterImpl.model.getRecordSplitter(), is(CHANGED_RECORDSPLITTER));
    }

    @Test
    public void sequenceAnalysisChanged_callSequenceAnalysisChanged_sequenceAnalysisIsChangedAccordingly() {
        initializeAndStartPresenter();

        presenterImpl.sequenceAnalysisChanged(false);

        assertThat(presenterImpl.model.getSequenceAnalysis(), is(false));
    }

    @Test
    public void selectedSubmittersChanged_callSelectedSubmittersChangedWithKnownSubmitter_selectedSubmittersIsChangedAccordingly() {
        initializeAndStartPresenter();

        Map<String, String> newSelectedSubmitters = new HashMap<>();
        newSelectedSubmitters.put("202", "85 (knallert)");
        presenterImpl.submittersChanged(newSelectedSubmitters);

        assertThat(presenterImpl.model.getSubmitterModels().size(), is(1));
        assertThat(presenterImpl.model.getSubmitterModels().get(0).getId(), is(202L));
        assertThat(presenterImpl.model.getSubmitterModels().get(0).getNumber(), is("2202"));
        assertThat(presenterImpl.model.getSubmitterModels().get(0).getName(), is("SName 2"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void selectedSubmittersChanged_callSelectedSubmittersChangedWithUnknownSubmitter_throws() {
        initializeAndStartPresenter();

        Map<String, String> newSelectedSubmitters = new HashMap<>();
        newSelectedSubmitters.put("210", "85 (knallert)");
        presenterImpl.submittersChanged(newSelectedSubmitters);
    }

    @Test
    public void selectedFlowChanged_callSelectedFlowChangedWithKnownSubmitter_selectedSubmittersIsChangedAccordingly() {
        initializeAndStartPresenter();

        presenterImpl.flowChanged("103");

        assertThat(presenterImpl.model.getFlowModel().getId(), is(flowModel3.getId()));
        assertThat(presenterImpl.model.getFlowModel().getFlowName(), is(flowModel3.getFlowName()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void selectedFlowChanged_callSelectedFlowChangedWithUnknownSubmitter_throws() {
        initializeAndStartPresenter();

        presenterImpl.flowChanged("145");
    }

    @Test
    public void selectedSinkChanged_callSelectedSinkChangedWithKnownSubmitter_selectedSubmittersIsChangedAccordingly() {
        initializeAndStartPresenter();

        presenterImpl.sinkChanged("303");

        assertThat(presenterImpl.model.getSinkModel().getId(), is(sinkModel3.getId()));
        assertThat(presenterImpl.model.getSinkModel().getSinkName(), is(sinkModel3.getSinkName()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void selectedSinkChanged_callSelectedSinkChangedWithUnknownSubmitter_throws() {
        initializeAndStartPresenter();

        presenterImpl.sinkChanged("645");
    }

    @Test
    public void keyPressed_callKeyPressed_statusTextEmptyed() {
        initializeAndStartPresenter();

        presenterImpl.keyPressed();
        verify(view.status, times(2)).setText("");
    }

    @Test
    public void saveButtonPressed_callSaveButtonPressedNameEmpty_errorMessageDisplayed() {
        initializeAndStartPresenter();
        presenterImpl.model.setName("");  // We do only test an empty name, since all other empty cases are tested in the model
        assertThat(saveModelHasBeenCalled, is(false));

        presenterImpl.saveButtonPressed();

        assertThat(saveModelHasBeenCalled, is(false));
        verify(mockedTexts).error_InputFieldValidationError();  // We cannot verify a call to view.setErrorText since it is not mocked, but it has a call to texts.error_InputFieldValidationError() - so we will verify this instead
    }

    @Test
    public void saveButtonPressed_callSaveButtonPressedWithInvalidCharactersInNameField_ErrorTextIsDisplayed() {
        setupPresenterImpl();
        presenterImpl.model.setName("*(Flow binder name)*_%€");

        presenterImpl.saveButtonPressed();

        verify(mockedTexts).error_NameFormatValidationError();
    }

    @Test
    public void fetchAvailableSubmittersCallback_unsuccessfulCallback_errorMessageDisplayed() {
        initializeAndStartPresenter();
        ProxyException mockedProxyException = mock(ProxyException.class);
        when(mockedProxyException.getErrorCode()).thenReturn(ProxyError.SERVICE_NOT_FOUND);

        presenterImpl.fetchAvailableSubmittersCallback.onFilteredFailure(mockedProxyException);

        verify(mockedProxyException).getErrorCode();
        verify(mockedProxyErrorTexts).flowStoreProxy_serviceError();
    }

    @Test
    public void fetchAvailableSubmittersCallback_successfullCallback_statusMessageDisplayed() {
        initializeAndStartPresenter();

        presenterImpl.fetchAvailableSubmittersCallback.onSuccess(Collections.singletonList(submitterModel3));

        assertThat(presenterImpl.availableSubmitters.size(), is(1));
        assertThat(presenterImpl.availableSubmitters.get(0).getId(), is(submitterModel3.getId()));
    }

    @Test
    public void fetchAvailableFlowsCallback_unsuccessfulCallback_errorMessageDisplayed() {
        initializeAndStartPresenter();

        presenterImpl.fetchAvailableFlowsCallback.onFilteredFailure(mockedException);

        verify(mockedException).getMessage();  // Is called before calling view.setErrorText, which we cannot verify, since view is not mocked
    }

    @Test
    public void fetchAvailableFlowsCallback_successfullCallback_statusMessageDisplayed() {
        initializeAndStartPresenter();

        presenterImpl.fetchAvailableFlowsCallback.onSuccess(Collections.singletonList(flowModel2));

        assertThat(presenterImpl.availableFlows.size(), is(1));
        assertThat(presenterImpl.availableFlows.get(0).getId(), is(flowModel2.getId()));
    }

    @Test
    public void fetchAvailableSinksCallback_unsuccessfulCallback_errorMessageDisplayed() {
        initializeAndStartPresenter();
        ProxyException mockedProxyException = mock(ProxyException.class);
        when(mockedProxyException.getErrorCode()).thenReturn(ProxyError.BAD_REQUEST);

        presenterImpl.fetchAvailableSinksCallback.onFilteredFailure(mockedProxyException);

        verify(mockedProxyException).getErrorCode();
        verify(mockedProxyErrorTexts).flowStoreProxy_dataValidationError();
    }

    @Test
    public void fetchAvailableSinksCallback_successfullCallback_statusMessageDisplayed() {
        initializeAndStartPresenter();

        presenterImpl.fetchAvailableSinksCallback.onSuccess(Collections.singletonList(sinkModel2));

        assertThat(presenterImpl.availableSinks.size(), is(1));
        assertThat(presenterImpl.availableSinks.get(0).getId(), is(sinkModel2.getId()));
    }


    @Test
    public void saveFlowBinderModelCallback_unsuccessfulCallback_errorMessageDisplayed() {
        initializeAndStartPresenter();
        ProxyException mockedProxyException = mock(ProxyException.class);
        when(mockedProxyException.getErrorCode()).thenReturn(ProxyError.CONFLICT_ERROR);
        presenterImpl.saveFlowBinderCallback.onFilteredFailure(mockedProxyException);

        verify(mockedProxyException).getErrorCode();
        verify(mockedProxyErrorTexts).flowStoreProxy_conflictError();
    }

    @Test
    public void saveFlowBinderModelCallback_successfullCallback_statusMessageDisplayed() {
        final String SUCCESS_TEXT = "Check!";
        FlowBinderModel flowBinderModel = new FlowBinderModelBuilder().setFlowModel(flowModel3).setSubmitterModels(Collections.singletonList(submitterModel3)).setSinkModel(sinkModel3).build();
        when(mockedTexts.status_SaveSuccess()).thenReturn(SUCCESS_TEXT);
        initializeAndStartPresenter();

        presenterImpl.saveFlowBinderCallback.onSuccess(flowBinderModel);

        verify(view.status).setText(SUCCESS_TEXT);
        assertThat(presenterImpl.model, is(flowBinderModel));
    }


    //Private methods
    private void initializeAndStartPresenter() {
        setupPresenterImpl();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
    }
    private void setupPresenterImpl() {
        presenterImpl = new PresenterImplConcrete(header);
        presenterImpl.commonInjector = mockedCommonGinjector;
        presenterImpl.viewInjector = mockedViewInjector;
    }
}
