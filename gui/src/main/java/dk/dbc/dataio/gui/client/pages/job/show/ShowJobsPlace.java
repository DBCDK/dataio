package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import dk.dbc.dataio.gui.client.places.AbstractBasePlace;
import dk.dbc.dataio.gui.util.ClientFactory;

public class ShowJobsPlace extends AbstractBasePlace {

    /**
     * Constructor taking no arguments
     */
    public ShowJobsPlace() {
        super();
    }

    /**
     * Constructor taking a Token
     *
     * @param token The token to be used
     */
    public ShowJobsPlace(String token) {
        super(token);
    }

    @Override
    public Activity createPresenter(ClientFactory clientFactory) {
        presenter = new PresenterJobsImpl(
                clientFactory.getPlaceController(),
                clientFactory.getGlobalViewsFactory().getJobsView(),
                commonInjector.getMenuTexts().menu_Jobs());
        return presenter;
    }

    @Prefix("ShowJobs")
    public static class Tokenizer implements PlaceTokenizer<ShowJobsPlace> {
        @Override
        public String getToken(ShowJobsPlace place) {
            return place.getToken();
        }

        @Override
        public ShowJobsPlace getPlace(String token) {
            return new ShowJobsPlace(token);
        }
    }
}

