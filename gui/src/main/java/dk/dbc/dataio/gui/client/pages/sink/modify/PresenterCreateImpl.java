package dk.dbc.dataio.gui.client.pages.sink.modify;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.DpfSinkConfig;
import dk.dbc.dataio.commons.types.EsSinkConfig;
import dk.dbc.dataio.commons.types.ImsSinkConfig;
import dk.dbc.dataio.commons.types.OpenUpdateSinkConfig;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.VipSinkConfig;
import dk.dbc.dataio.commons.types.WorldCatSinkConfig;
import dk.dbc.dataio.gui.client.model.SinkModel;

/**
 * Concrete Presenter Implementation Class for Sink Create
 */
public class PresenterCreateImpl extends PresenterImpl {

    /**
     * Constructor
     *
     * @param header header
     */
    public PresenterCreateImpl(String header) {
        super(header);
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
        super.start(containerWidget, eventBus);
        getView().deleteButton.setVisible(false);
    }

    /**
     * getModel - initializes the model
     * When starting the form, the fields should be empty, therefore an empty Model is instantiated
     */
    @Override
    public void initializeModel() {
        model = new SinkModel();
        View view = getView();
        view.sinkTypeSelection.fireChangeEvent();
        updateAllFieldsAccordingToCurrentState();
    }

    @Override
    void handleSinkConfig(SinkContent.SinkType sinkType) {
        View view = getView();
        view.sinkTypeSelection.setEnabled(true);
        model.setSinkType(sinkType);
        switch (sinkType) {
            case DPF:
                model.setSinkConfig(new DpfSinkConfig());
                view.dpfSinkSection.setVisible(true);
                break;
            case OPENUPDATE:
                model.setSinkConfig(new OpenUpdateSinkConfig());
                view.updateSinkSection.setVisible(true);
                break;
            case ES:
                model.setSinkConfig(new EsSinkConfig());
                view.esSinkSection.setVisible(true);
                break;
            case IMS:
                model.setSinkConfig(new ImsSinkConfig());
                view.imsSinkSection.setVisible(true);
                break;
            case WORLDCAT:
                model.setSinkConfig(new WorldCatSinkConfig());
                view.worldCatSinkSection.setVisible(true);
                break;
            case TICKLE:
                model.setSinkConfig(null);
                view.sequenceAnalysisSection.setVisible(false);
                break;
            case VIP:
                model.setSinkConfig(new VipSinkConfig());
                view.vipSinkSection.setVisible(true);
                view.sequenceAnalysisSection.setVisible(false);
                break;
            case DMAT:
            default:
                model.setSinkConfig(null);
        }
    }

    /**
     * saveModel
     * Saves the embedded model as a new Sink in the database
     */
    @Override
    void saveModel() {
        commonInjector.getFlowStoreProxyAsync().createSink(model, new SaveSinkModelFilteredAsyncCallback());
    }

    /**
     * This has no implementation because "Create" does not have a delete button!
     */
    public void deleteButtonPressed() {
    }
}
