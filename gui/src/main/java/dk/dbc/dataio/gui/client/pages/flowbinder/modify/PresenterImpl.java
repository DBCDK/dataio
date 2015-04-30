package dk.dbc.dataio.gui.client.pages.flowbinder.modify;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.exceptions.texts.ProxyErrorTexts;
import dk.dbc.dataio.gui.client.model.FlowBinderModel;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.model.SubmitterModel;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents the create flowbinder activity encompassing saving
 * of flowbinder data in the flow store via RPC proxy
 */
public abstract class PresenterImpl extends AbstractActivity implements Presenter {
    private final static String EMPTY = "";
    protected final Texts texts;
    protected final ProxyErrorTexts proxyErrorTexts;
    protected FlowStoreProxyAsync flowStoreProxy;
    protected View view;

    // Application Models
    protected FlowBinderModel model = new FlowBinderModel();
    protected List<SubmitterModel> availableSubmitters = new ArrayList<SubmitterModel>();
    protected List<FlowModel> availableFlows = new ArrayList<FlowModel>();
    protected List<SinkModel> availableSinks = new ArrayList<SinkModel>();


    /**
     * Constructor
     * Please note, that in the constructor, view has NOT been initialized and can therefore not be used
     * Put code, utilizing view in the start method
     *
     * @param clientFactory, clientFactory
     */
    public PresenterImpl(ClientFactory clientFactory) {
        texts = clientFactory.getFlowBinderModifyTexts();
        proxyErrorTexts = clientFactory.getProxyErrorTexts();
        flowStoreProxy = clientFactory.getFlowStoreProxyAsync();
    }

    /**
     * start method
     * Is called by PlaceManager, whenever the PlaceCreate or PlaceEdit are being invoked
     * This method is the start signal for the presenter
     *
     * @param containerWidget the widget to use
     * @param eventBus        the eventBus to use
     */
    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        initializeViewFields();
        view.setPresenter(this);
        containerWidget.setWidget(view.asWidget());
        fetchAvailableSubmitters();
        fetchAvailableFlows();
        fetchAvailableSinks();
        initializeModel();
    }

    private void initializeViewFields() {
        view.name.clearText();
        view.name.setEnabled(false);
        view.description.clearText();
        view.description.setEnabled(false);
        view.frame.clearText();
        view.frame.setEnabled(false);
        view.format.clearText();
        view.format.setEnabled(false);
        view.charset.clearText();
        view.charset.setEnabled(false);
        view.destination.clearText();
        view.destination.setEnabled(false);
        view.recordSplitter.clearText();
        view.recordSplitter.setEnabled(false);
        view.sequenceAnalysis.setEnabled(true);
        view.recordSplitter.setEnabled(false);
        view.submitters.clear();
        view.submitters.setEnabled(false);
        view.flow.clear();
        view.flow.setEnabled(false);
        view.sink.clear();
        view.sink.setEnabled(false);
        view.status.setText("");
    }

    /**
     * A signal to the presenter, saying that the name field has been changed
     *
     * @param name, the new value
     */
    @Override
    public void nameChanged(String name) {
        model.setName(name);
    }

    /**
     * A signal to the presenter, saying that the description field has been changed
     *
     * @param description, the new value
     */
    @Override
    public void descriptionChanged(String description) {
        model.setDescription(description);
    }

    /**
     * A signal to the presenter, saying that the packaging field has been changed
     *
     * @param packaging, the new value
     */
    @Override
    public void frameChanged(String packaging) {
        model.setPackaging(packaging);
    }

    /**
     * A signal to the presenter, saying that the format field has been changed
     *
     * @param format, the new value
     */
    @Override
    public void formatChanged(String format) {
        model.setFormat(format);
    }

    /**
     * A signal to the presenter, saying that the charset field has been changed
     *
     * @param charset, the new value
     */
    @Override
    public void charsetChanged(String charset) {
        model.setCharset(charset);
    }

    /**
     * A signal to the presenter, saying that the destination field has been changed
     *
     * @param destination, the new value
     */
    @Override
    public void destinationChanged(String destination) {
        model.setDestination(destination);
    }

    /**
     * A signal to the presenter, saying that the recordSplitter field has been changed
     *
     * @param recordSplitter, the new value
     */
    @Override
    public void recordSplitterChanged(String recordSplitter) {
        model.setRecordSplitter(recordSplitter);
    }

    /**
     * A signal to the presenter, saying that the sequenceAnalysis field has been changed
     *
     * @param sequenceAnalysis, the new value
     */
    @Override
    public void sequenceAnalysisChanged(boolean sequenceAnalysis) {
        model.setSequenceAnalysis(sequenceAnalysis);
    }

    /**
     * A signal to the presenter, saying that the submitters field has been changed
     *
     * @param submitters, a map of the selected submitters
     */
    @Override
    public void submittersChanged(Map<String, String> submitters) {
        List<SubmitterModel> submitterModels = new ArrayList<SubmitterModel>();
        for (String id : submitters.keySet()) {
            SubmitterModel sModel = getSubmitterModel(Long.parseLong(id));
            if (sModel != null) {
                submitterModels.add(sModel);
            }
        }
        model.setSubmitterModels(submitterModels);
    }

    /**
     * A signal to the presenter, saying that the flow field has been changed
     *
     * @param flowId, the id for the selected flow
     */
    @Override
    public void flowChanged(String flowId) {
        if (flowId != null) {
            model.setFlowModel(getFlowModel(Long.parseLong(flowId)));
        }
    }

    /**
     * A signal to the presenter, saying that the sink field has been changed
     *
     * @param sinkId, the id for the selected sink
     */
    @Override
    public void sinkChanged(String sinkId) {
        if (sinkId != null) {
            model.setSinkModel(getSinkModel(Long.parseLong(sinkId)));
        }
    }

    /**
     * A signal to the presenter, saying that a key has been pressed in either of the fields
     */
    @Override
    public void keyPressed() {
        view.status.setText(EMPTY);
    }

    /**
     * A signal to the presenter, saying that the save button has been pressed
     */
    @Override
    public void saveButtonPressed() {
        if (model.isInputFieldsEmpty()) {
            view.setErrorText(texts.error_InputFieldValidationError());
        } else if (!model.getDataioPatternMatches().isEmpty()) {
            view.setErrorText(texts.error_NameFormatValidationError());
        } else {
            saveModel();
        }
    }


    /*
     * Private methods
     */
    private String formatSubmitterName(SubmitterModel model) {
        return model.getNumber() + " (" + model.getName() + ")";
    }

    protected void updateAllFieldsAccordingToCurrentState() {
        model.setRecordSplitter(texts.label_DefaultRecordSplitter());
        view.name.setText(model.getName());
        view.name.setEnabled(true);
        view.description.setText(model.getDescription());
        view.description.setEnabled(true);
        view.frame.setText(model.getPackaging());
        view.frame.setEnabled(true);
        view.format.setText(model.getFormat());
        view.format.setEnabled(true);
        view.charset.setText(model.getCharset());
        view.charset.setEnabled(true);
        view.destination.setText(model.getDestination());
        view.destination.setEnabled(true);
        view.recordSplitter.setText(model.getRecordSplitter());
        view.recordSplitter.setEnabled(false);
        view.sequenceAnalysis.setValue(model.getSequenceAnalysis());
        view.sequenceAnalysis.setEnabled(true);
        view.submitters.setAvailableItems(getAvailableSubmitters(model));
        view.submitters.setSelectedItems(getSelectedSubmitters(model));
        view.submitters.setEnabled(true);
        view.flow.setSelectedItem(model.getFlowModel().getFlowName());
        view.flow.setEnabled(true);
        view.sink.setSelectedItem(model.getSinkModel().getSinkName());
        view.sink.setEnabled(true);
    }

    private Map<String, String> getAvailableSubmitters(FlowBinderModel model) {
        Map<String, String> availableSubmitterMap = new HashMap<String, String>();
        for (SubmitterModel submitterModel: this.availableSubmitters) {
            if (!isSubmitterSelected(submitterModel.getId(), model.getSubmitterModels())) {
                availableSubmitterMap.put(String.valueOf(submitterModel.getId()), formatSubmitterName(submitterModel));
            }
        }
        return availableSubmitterMap;
    }

    private boolean isSubmitterSelected(long id, List<SubmitterModel> submitterModels) {
        for (SubmitterModel model: submitterModels) {
            if (model.getId() == id) {
                return true;
            }
        }
        return false;
    }

    private Map<String, String> getSelectedSubmitters(FlowBinderModel model) {
        Map<String, String> selectedSubmitterMap = new HashMap<String, String>();
        for (SubmitterModel submitterModel: model.getSubmitterModels()) {
            selectedSubmitterMap.put(String.valueOf(submitterModel.getId()), formatSubmitterName(submitterModel));
        }
        return selectedSubmitterMap;
    }

    protected void setAvailableSubmitters(List<SubmitterModel> models) {
        this.availableSubmitters = models;
        Map<String, String> submitters = new HashMap<String, String>(models.size());
        for (SubmitterModel model : models) {
            submitters.put(String.valueOf(model.getId()), formatSubmitterName(model));
        }
        view.submitters.setAvailableItems(submitters);
        view.submitters.setEnabled(true);
    }

    protected void setAvailableFlows(List<FlowModel> models) {
        this.availableFlows = models;
        view.flow.clear();
        for (FlowModel model : models) {
            view.flow.addAvailableItem(model.getFlowName(), Long.toString(model.getId()));
        }
        view.flow.setEnabled(true);
    }

    protected void setAvailableSinks(List<SinkModel> models) {
        this.availableSinks = models;
        view.sink.clear();
        for (SinkModel model : models) {
            view.sink.addAvailableItem(model.getSinkName(), Long.toString(model.getId()));
        }
        view.sink.setEnabled(true);
    }

    private void fetchAvailableSubmitters() {
        flowStoreProxy.findAllSubmitters(new FetchAvailableSubmittersCallback());
    }

    private void fetchAvailableFlows() {
        flowStoreProxy.findAllFlows(new FetchAvailableFlowsCallback());
    }

    private void fetchAvailableSinks() {
        flowStoreProxy.findAllSinks(new FetchAvailableSinksCallback());
    }

    private SubmitterModel getSubmitterModel(long submitterId) {
        for (SubmitterModel model : availableSubmitters) {
            if (model.getId() == submitterId) {
                return model;
            }
        }
        throw new IllegalArgumentException("Submitter not found");
    }

    private FlowModel getFlowModel(long flowId) {
        for (FlowModel model : availableFlows) {
            if (model.getId() == flowId) {
                return model;
            }
        }
        throw new IllegalArgumentException("Flow not found");
    }

    private SinkModel getSinkModel(long sinkId) {
        for (SinkModel model : availableSinks) {
            if (model.getId() == sinkId) {
                return model;
            }
        }
        throw new IllegalArgumentException("Sink not found");
    }

    protected void setFlowBinderModel(FlowBinderModel model) {
        this.model = model;
    }

    /*
     * Local class
     */


    /**
     * Local call back class to be instantiated in the call to findAllSubmitters in flowstore proxy
     */
    class FetchAvailableSubmittersCallback extends FilteredAsyncCallback<List<SubmitterModel>> {
        @Override
        public void onFilteredFailure(Throwable e) {
            view.setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, proxyErrorTexts));
        }
        @Override
        public void onSuccess(List<SubmitterModel> submitters) {
            setAvailableSubmitters(submitters);
            updateAllFieldsAccordingToCurrentState();
        }
    }

    /**
     * Local call back class to be instantiated in the call to findAllFlows in flowstore proxy
     */
    class FetchAvailableFlowsCallback extends FilteredAsyncCallback<List<FlowModel>> {
        @Override
        public void onFilteredFailure(Throwable e) {
            view.setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, proxyErrorTexts));
        }
        @Override
        public void onSuccess(List<FlowModel> flows) {
            setAvailableFlows(flows);
            updateAllFieldsAccordingToCurrentState();
        }
    }

    /**
     * Local call back class to be instantiated in the call to findAllSinks in flowstore proxy
     */
    class FetchAvailableSinksCallback extends FilteredAsyncCallback<List<SinkModel>> {
        @Override
        public void onFilteredFailure(Throwable e) {
            view.setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, proxyErrorTexts));
        }
        @Override
        public void onSuccess(List<SinkModel> sinks) {
            setAvailableSinks(sinks);
            updateAllFieldsAccordingToCurrentState();
        }
    }

    /**
     * Local call back class to be instantiated in the call to createFlowBinder or updateFlowBinder in flowstore proxy
     */
    class SaveFlowBinderModelFilteredAsyncCallback extends FilteredAsyncCallback<FlowBinderModel> {
        @Override
        public void onFilteredFailure(Throwable e) {
            view.setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, proxyErrorTexts));
        }
        @Override
        public void onSuccess(FlowBinderModel model) {
            view.status.setText(texts.status_SaveSuccess());
            setFlowBinderModel(model);
            updateAllFieldsAccordingToCurrentState();
        }

    }


    /*
     * Abstract methods
     */

    /**
     * getModel
     */
    abstract void initializeModel();

    /**
     * saveModel
     */
    abstract void saveModel();

}
