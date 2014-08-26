package dk.dbc.dataio.gui.client.pages.submittermodify;

import com.google.gwt.place.shared.Place;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.gui.client.pages.sinkcreateedit.SinkEditPlace;
import dk.dbc.dataio.gui.util.ClientFactory;

/**
 * Concrete Presenter Implementation Class for Submitter Edit
 */
public class PresenterEditImpl extends PresenterImpl {
//    private Long id;

    /**
     * Constructor
     * @param clientFactory
     * @param constants
     */
    public PresenterEditImpl(Place place, ClientFactory clientFactory, SubmitterModifyConstants constants) {
        super(clientFactory, constants);
//        view = clientFactory.getSubmitterCreateView();
//        SinkEditPlace sinkEditPlace = (SinkEditPlace) place;
//        id = sinkEditPlace.getSinkId();
        initializeModel();
    }


    /**
     * getModel - initializes the model
     * The method fetches the stored Submitter, as given in the Place (referenced by this.id)
     */
    public void initializeModel(){
//        SubmitterContent content = new SubmitterContent(234L, "Submitter Name...", "Description...");
//        Submitter submitter = new Submitter(22L, 123L, content);
//        model = ModelMapper.toModel(submitter);
    }


    /**
     * saveModel
     * Saves the embedded model as a new Submitter in the database
     */
    @Override
    void saveModel() {
//        final SubmitterContent submitterContent = new SubmitterContent(Long.parseLong(model.getNumber()), model.getName(), model.getDescription());
//        flowStoreProxy.createSubmitter(submitterContent, new FilteredAsyncCallback<Submitter>() {
//            @Override
//            public void onFilteredFailure(Throwable e) {
//                view.setErrorText(getErrorText(e));
//            }
//
//            @Override
//            public void onSuccess(Submitter submitter) {
//                view.setStatusText(constants.status_SubmitterSuccessfullySaved());
//            }
//        });
    }

}
