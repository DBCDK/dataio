package dk.dbc.dataio.gui.client.pages.flowbinder.modify;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.commons.types.RecordSplitterConstants;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.model.FlowBinderModel;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.model.SubmitterModel;
import dk.dbc.dataio.gui.client.util.CommonGinjector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class represents the create and edit flowbinder activity encompassing saving
 * of flowbinder data in the flow store via RPC proxy
 */
public abstract class PresenterImpl extends AbstractActivity implements Presenter {

    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);
    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);

    private final static String EMPTY = "";

    // Application Models
    protected FlowBinderModel model = new FlowBinderModel();
    protected List<RecordSplitterConstants.RecordSplitter> availableRecordSplitters = new ArrayList<>();
    protected List<SubmitterModel> availableSubmitters = new ArrayList<>();
    protected List<FlowModel> availableFlows = new ArrayList<>();
    protected List<SinkModel> availableSinks = new ArrayList<>();
    protected String header;

    public PresenterImpl(String header) {
        super();
        this.header = header;
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
        getView().setPresenter(this);
        getView().setHeader(this.header);
        initializeViewFields();
        containerWidget.setWidget(getView().asWidget());
        fetchAvailableSubmitters();
        fetchAvailableFlows();
        fetchAvailableSinks();
        initializeModel();
        setAvailableRecordSplitters();
        setAvailablePriorities();
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
     * A signal to the presenter, saying that the priority field has been changed
     *
     * @param priority, the new value
     */
    @Override
    public void priorityChanged(String priority) {
        model.setPriority(Integer.valueOf(priority));
    }

    /**
     * A signal to the presenter, saying that the record splitter field has been changed
     *
     * @param recordSplitter, the selected record splitter
     */
    @Override
    public void recordSplitterChanged(String recordSplitter) {
        if (recordSplitter != null) {
            model.setRecordSplitter(recordSplitter);
        }
    }

    /**
     * A signal to the presenter, saying that the submitters field has been changed
     *
     * @param submitters, a map of the selected submitters
     */
    @Override
    public void submittersChanged(Map<String, String> submitters) {
        List<SubmitterModel> submitterModels = new ArrayList<>();
        for (String id : submitters.keySet()) {
            SubmitterModel sModel = getSubmitterModel(Long.parseLong(id));
            if (sModel != null) {
                submitterModels.add(sModel);
            }
        }
        model.setSubmitterModels(submitterModels);
    }

    /**
     * Adds a map of submitters
     *
     * @param submitterIds Map of submitters to add
     */
    @Override
    public void addSubmitters(Map<String, String> submitterIds) {
        Set<SubmitterModel> submitterModels = new LinkedHashSet<>();  // Preserve order in the set
        submitterModels.addAll(model.getSubmitterModels());  // Put in a set to avoid duplicates
        for (String id : submitterIds.keySet()) {
            submitterModels.add(getSubmitterModel(Long.parseLong(id)));
        }
        final List<SubmitterModel> result = new ArrayList<>();
        result.addAll(submitterModels);
        model.setSubmitterModels(result);
        updateAllFieldsAccordingToCurrentState();
    }

    /**
     * Removes a submitter from the list of submitters
     *
     * @param value The id for the submitter to remove from the submitters list
     */
    @Override
    public void removeSubmitter(String value) {
        List<SubmitterModel> submitterModels = model.getSubmitterModels();
        SubmitterModel submitterModel = getSubmitterModel(Long.parseLong(value));
        int index = submitterModels.indexOf(submitterModel);
        submitterModels.remove(index);
        updateAllFieldsAccordingToCurrentState();
    }

    /**
     * A signal to the presenter, saying that the flow field has been changed
     *
     * @param flowId, the id for the selected flow
     */
    @Override
    public void flowChanged(String flowId) {
        if (flowId != null && !flowId.isEmpty()) {
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
        if (sinkId != null && !sinkId.isEmpty()) {
            SinkModel sinkModel = getSinkModel(Long.parseLong(sinkId));
            model.setSinkModel(sinkModel);
            updateAllFieldsAccordingToCurrentState();
        }
    }

    /**
     * A signal to the presenter, saying that the queue providers field has been changed
     *
     * @param queueProvider queue providers
     */
    @Override
    public void queueProviderChanged(String queueProvider) {
        if (queueProvider != null) {
            model.setQueueProvider(queueProvider);
        }
    }

    /**
     * A signal to the presenter, saying that a key has been pressed in either of the fields
     */
    @Override
    public void keyPressed() {
        getView().status.setText(EMPTY);
    }

    /**
     * A signal to the presenter, saying that the save button has been pressed
     */
    @Override
    public void saveButtonPressed() {
        if (model.isInputFieldsEmpty()) {
            getView().setErrorText(getTexts().error_InputFieldValidationError());
        } else if (!model.getDataioPatternMatches().isEmpty()) {
            getView().setErrorText(getTexts().error_NameFormatValidationError());
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

    private String formatPriority(Priority priority) {
        switch (priority) {
            case LOW:
                return getTexts().selection_Low();
            case NORMAL:
                return getTexts().selection_Normal();
            case HIGH:
                return getTexts().selection_High();
            default:
                return "<unknown priority>";
        }
    }

    protected void updateAllFieldsAccordingToCurrentState() {
        View view = getView();
        view.name.setText(model.getName());
        view.name.setEnabled(true);
        view.name.setFocus(true);
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
        view.priority.setSelectedValue(String.valueOf(model.getPriority()));
        view.priority.setEnabled(true);
        view.recordSplitter.setSelectedText(model.getRecordSplitter());
        view.recordSplitter.setEnabled(true);
        setAvailableSubmittersToView(getAvailableSubmitters(model));
        view.submitters.setValue(getSelectedSubmitters(model));
        view.submitters.setEnabled(true);
        view.flow.setSelectedText(model.getFlowModel().getFlowName());
        view.flow.setEnabled(true);
        view.sink.setSelectedText(model.getSinkModel().getSinkName());
        view.sink.setEnabled(true);
        configureUpdateSinkSection();
    }

    private void configureUpdateSinkSection() {
        View view = getView();
        if (model.requiresQueueProvider()) {
            setAvailableQueueProvidersToView();
            setQueryProviderIfNoneSelected();
            view.updateSinkSection.setVisible(true);
            view.queueProvider.setValue(model.getQueueProvider());
            view.queueProvider.setEnabled(true);
        } else {
            model.setQueueProvider(null);
            view.updateSinkSection.setVisible(false);
            view.queueProvider.setEnabled(false);
        }
    }

    private void setAvailableQueueProvidersToView() {
        View view = getView();
        view.queueProvider.clear();
        for (String queueProvider : model.getAvailableQueueProviders()) {
            view.queueProvider.addAvailableItem(queueProvider);
        }
    }

    private void setQueryProviderIfNoneSelected() {
        if (model.getQueueProvider() == null) {
            model.setQueueProvider(model.getAvailableQueueProviders().get(0));
        }
    }

    private Map<String, String> getAvailableSubmitters(FlowBinderModel model) {
        Map<String, String> availableSubmitterMap = new LinkedHashMap<>();
        for (SubmitterModel submitterModel : this.availableSubmitters) {
            if (!isSubmitterSelected(submitterModel.getId(), model.getSubmitterModels())) {
                availableSubmitterMap.put(String.valueOf(submitterModel.getId()), formatSubmitterName(submitterModel));
            }
        }
        return availableSubmitterMap;
    }

    private void setAvailableSubmittersToView(Map<String, String> submitters) {
        getView().popupListBox.clear();
        for (Map.Entry<String, String> entry : submitters.entrySet()) {
            getView().popupListBox.addItem(entry.getValue(), entry.getKey());
        }
    }

    private boolean isSubmitterSelected(long id, List<SubmitterModel> submitterModels) {
        for (SubmitterModel model : submitterModels) {
            if (model.getId() == id) {
                return true;
            }
        }
        return false;
    }

    private Map<String, String> getSelectedSubmitters(FlowBinderModel model) {
        Map<String, String> selectedSubmitterMap = new LinkedHashMap<>();
        for (SubmitterModel submitterModel : model.getSubmitterModels()) {
            selectedSubmitterMap.put(String.valueOf(submitterModel.getId()), formatSubmitterName(submitterModel));
        }
        return selectedSubmitterMap;
    }

    protected void setAvailableSubmitters(List<SubmitterModel> models) {
        this.availableSubmitters = models;
        Map<String, String> submitters = new LinkedHashMap<>(models.size());
        for (SubmitterModel model : models) {
            submitters.put(String.valueOf(model.getId()), formatSubmitterName(model));
        }
        setAvailableSubmittersToView(submitters);
        getView().submitters.setEnabled(true);
    }

    protected void setAvailableFlows(List<FlowModel> models) {
        final View view = getView();
        this.availableFlows = models;
        view.flow.clear();
        for (FlowModel model : models) {
            getView().flow.addAvailableItem(model.getFlowName(), Long.toString(model.getId()));
        }
        if (!model.getFlowModel().getFlowName().isEmpty()) {
            view.flow.setSelectedText(model.getFlowModel().getFlowName());
        } else {
            view.flow.setSelectedText(models.get(0).getFlowName());
            model.setFlowModel(models.get(0));
        }
    }

    protected void setAvailableSinks(List<SinkModel> models) {
        final View view = getView();
        this.availableSinks = models;
        view.sink.clear();
        for (SinkModel model : models) {
            getView().sink.addAvailableItem(model.getSinkName(), Long.toString(model.getId()));
        }
        if (!model.getSinkModel().getSinkName().isEmpty()) {
            view.sink.setSelectedText(model.getSinkModel().getSinkName());
        } else {
            view.sink.setSelectedText(models.get(0).getSinkName());
            model.setSinkModel(models.get(0));
        }
    }

    protected void setAvailableRecordSplitters() {
        availableRecordSplitters = RecordSplitterConstants.getRecordSplitters();
        getView().recordSplitter.clear();
        for (RecordSplitterConstants.RecordSplitter recordSplitter : availableRecordSplitters) {
            getView().recordSplitter.addAvailableItem(recordSplitter.name());
        }
        getView().recordSplitter.setEnabled(true);
        if (model.getRecordSplitter().isEmpty()) {
            model.setRecordSplitter(availableRecordSplitters.get(0).name());
        }
    }

    protected void setAvailablePriorities() {
        Arrays.stream(Priority.values()).forEach(
                priority -> {
                    getView().priority.addAvailableItem(formatPriority(priority), String.valueOf(priority.getValue()));
                }
        );
    }

    View getView() {
        return viewInjector.getView();
    }

    Texts getTexts() {
        return viewInjector.getTexts();
    }

    private void initializeViewFields() {
        View view = getView();
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
        view.priority.clear();
        view.priority.setEnabled(false);
        view.recordSplitter.clear();
        view.recordSplitter.setEnabled(false);
        view.submitters.clear();
        view.submitters.setEnabled(false);
        view.flow.clear();
        view.flow.setEnabled(false);
        view.sink.clear();
        view.sink.setEnabled(false);
        view.status.setText("");
    }

    private void fetchAvailableSubmitters() {
        commonInjector.getFlowStoreProxyAsync().findAllSubmitters(new FetchAvailableSubmittersCallback());
    }

    private void fetchAvailableFlows() {
        commonInjector.getFlowStoreProxyAsync().findAllFlows(new FetchAvailableFlowsCallback());
    }

    private void fetchAvailableSinks() {
        commonInjector.getFlowStoreProxyAsync().findAllSinks(new FetchAvailableSinksCallback());
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
     * Local classes
     */


    /**
     * Local call back class to be instantiated in the call to findAllSubmitters in flowstore proxy
     */
    class FetchAvailableSubmittersCallback extends FilteredAsyncCallback<List<SubmitterModel>> {
        @Override
        public void onFilteredFailure(Throwable e) {
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, commonInjector.getProxyErrorTexts(), this.getClass().getCanonicalName()));
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
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, commonInjector.getProxyErrorTexts(), this.getClass().getCanonicalName()));
        }

        @Override
        public void onSuccess(List<FlowModel> flows) {
            setAvailableFlows(flows);
        }
    }

    /**
     * Local call back class to be instantiated in the call to findAllSinks in flowstore proxy
     */
    class FetchAvailableSinksCallback extends FilteredAsyncCallback<List<SinkModel>> {
        @Override
        public void onFilteredFailure(Throwable e) {
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, commonInjector.getProxyErrorTexts(), this.getClass().getCanonicalName()));
        }

        @Override
        public void onSuccess(List<SinkModel> sinks) {
            setAvailableSinks(sinks);
        }
    }

    /**
     * Local call back class to be instantiated in the call to createFlowBinder or updateFlowBinder in flowstore proxy
     */
    class SaveFlowBinderModelFilteredAsyncCallback extends FilteredAsyncCallback<FlowBinderModel> {
        @Override
        public void onFilteredFailure(Throwable e) {
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, commonInjector.getProxyErrorTexts(), null));
        }

        @Override
        public void onSuccess(FlowBinderModel model) {
            getView().status.setText(getTexts().status_SaveSuccess());
            History.back();
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
