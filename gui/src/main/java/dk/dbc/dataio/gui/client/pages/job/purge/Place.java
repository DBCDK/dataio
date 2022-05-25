/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dbc.dataio.gui.client.pages.job.purge;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import dk.dbc.dataio.gui.client.places.AbstractBasePlace;
import dk.dbc.dataio.gui.util.ClientFactory;


/**
 * Purge Place
 */
public class Place extends AbstractBasePlace {

    public Place() {
        super();
    }

    /**
     * Constructor taking a Token
     *
     * @param token The token to be used
     */
    public Place(String token) {
        super(token);
    }

    @Override
    public Activity createPresenter(ClientFactory clientFactory) {
        return new PresenterImpl(commonInjector.getMenuTexts().menu_JobPurge());
    }

    @Prefix("JobPurge")
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
