package dk.dbc.dataio.gui.client.pages.flowbinder.modify;

import com.google.gwt.place.shared.Place;
import dk.dbc.dataio.gui.client.pages.flow.modify.FlowModel;
import dk.dbc.dataio.gui.client.pages.sink.modify.SinkModel;
import dk.dbc.dataio.gui.client.pages.submitter.modify.SubmitterModel;
import dk.dbc.dataio.gui.util.ClientFactory;

import java.util.Arrays;

/**
 * Concrete Presenter Implementation Class for Flow Binder Edit
 */
public class PresenterEditImpl extends PresenterImpl {
    private long id;

    /**
     * Constructor
     * @param clientFactory the clientFactory
     * @param texts The text String used by flow binder
     */
    public PresenterEditImpl(Place place, ClientFactory clientFactory, Texts texts) {
        super(clientFactory, texts);
        view = clientFactory.getFlowBinderEditView();
        EditPlace editPlace = (EditPlace) place;
        id = editPlace.getFlowId();
    }

    @Override
    void initializeModel() {
        getFlowBinder(id);
    }

    @Override
    void saveModel() {}

    // Private methods

    // TODO - dummy method just to see if it works
    private void getFlowBinder(final long flowBinderId) {
        model = new FlowBinderModel(
                flowBinderId,
                1,
                "Name",
                "Description",
                "Packaging",
                "Format",
                "Charset",
                "Destination",
                "RecordSplitter",
                new FlowModel(),
                Arrays.asList(new SubmitterModel()),
                new SinkModel());

        updateAllFieldsAccordingToCurrentState();
    }

}
