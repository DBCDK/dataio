/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dbc.dataio.gui.client.pages.job.modify;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.places.AbstractBasePlace;
import dk.dbc.dataio.gui.util.ClientFactory;


/**
 * EditPlace
 */
public class EditPlace extends AbstractBasePlace {

    static final String JOB_ID = "jobId";
    static final String FAILED_ITEMS_ONLY = "failedItemsOnly";

    public EditPlace() {
        super();
    }

    /**
     * Constructor taking a Token
     *
     * @param token The token to be used
     */
    private EditPlace(String token) {
        super(token);
    }

    public EditPlace(JobModel model, Boolean failedItemsOnly) {
        addParameter(JOB_ID, model.getJobId());
        addParameter(FAILED_ITEMS_ONLY, String.valueOf(failedItemsOnly));
    }

    @Override
    public Activity createPresenter(ClientFactory clientFactory) {
        return new PresenterEditImpl(this, commonInjector.getMenuTexts().menu_JobEdit());
    }

    @Prefix("RerunJob")
    public static class Tokenizer implements PlaceTokenizer<EditPlace> {
        @Override
        public String getToken(EditPlace place) {
            return place.getToken();
        }

        @Override
        public EditPlace getPlace(String token) {
            return new EditPlace(token);
        }
    }

}
