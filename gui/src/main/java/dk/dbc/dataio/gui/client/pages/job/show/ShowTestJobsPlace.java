package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import dk.dbc.dataio.gui.client.places.AbstractBasePlace;
import dk.dbc.dataio.gui.util.ClientFactory;

public class ShowTestJobsPlace extends AbstractBasePlace {

    /**
     * Constructor taking no arguments
     */
    public ShowTestJobsPlace() {
        super();
    }

    /**
     * Constructor taking a Token
     *
     * @param token The token to be used
     */
    public ShowTestJobsPlace(String token) {
        super(token);
    }

    @Override
    public Activity createPresenter(ClientFactory clientFactory) {
        presenter = new PresenterTestJobsImpl(
                clientFactory.getPlaceController(),
                clientFactory.getGlobalViewsFactory().getTestJobsView(),
                commonInjector.getMenuTexts().menu_TestJobs());
        return presenter;
    }

    @Prefix("ShowTestJobs")
    public static class Tokenizer implements PlaceTokenizer<ShowTestJobsPlace> {
        @Override
        public String getToken(ShowTestJobsPlace place) {
            return place.getToken();
        }

        @Override
        public ShowTestJobsPlace getPlace(String token) {
            return new ShowTestJobsPlace(token);
        }
    }
}

