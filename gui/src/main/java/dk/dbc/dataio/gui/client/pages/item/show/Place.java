/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dbc.dataio.gui.client.pages.item.show;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import dk.dbc.dataio.gui.client.places.AbstractBasePlace;
import dk.dbc.dataio.gui.util.ClientFactory;

/**
 * Place
 */
public class Place extends AbstractBasePlace {
    static final String RECORD_ID = "recordId";
    static final String JOB_ID = "jobId";
    public static final String TOKEN = "ShowItems";

    public Place() {
        super();
    }

    /**
     * Constructor taking a Token
     *
     * @param token The token to be used
     */
    private Place(String token) {
        super(token);
    }

    public Place(String jobId, String recordId) {
        addParameter(JOB_ID, jobId);
        if (recordId != null) {
            addParameter(RECORD_ID, recordId);
        }
    }

    @Override
    public Activity createPresenter(ClientFactory clientFactory) {
        return new PresenterImpl(
                this,
                clientFactory.getPlaceController(),
                clientFactory.getGlobalViewsFactory().getItemsView(),
                commonInjector.getMenuTexts().menu_Items());
    }

    @Prefix(TOKEN)
    public static class Tokenizer implements PlaceTokenizer<Place> {
        @Override
        public String getToken(Place place) {
            return place.getToken();
        }

        @Override
        public Place getPlace(String token) {
            return new Place(token);
        }
    }
}
