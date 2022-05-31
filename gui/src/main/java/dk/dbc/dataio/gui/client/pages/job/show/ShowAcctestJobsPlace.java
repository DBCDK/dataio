package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import dk.dbc.dataio.gui.client.places.AbstractBasePlace;
import dk.dbc.dataio.gui.util.ClientFactory;

public class ShowAcctestJobsPlace extends AbstractBasePlace {

    /**
     * Constructor taking no arguments
     */
    public ShowAcctestJobsPlace() {
        super();
    }

    /**
     * Constructor taking a Token
     *
     * @param token The token to be used
     */
    public ShowAcctestJobsPlace(String token) {
        super(token);
    }

    @Override
    public Activity createPresenter(ClientFactory clientFactory) {
        presenter = new PresenterAcctestJobsImpl(
                clientFactory.getPlaceController(),
                clientFactory.getGlobalViewsFactory().getAcctestJobsView(),
                commonInjector.getMenuTexts().menu_AcctestJobs());
        return presenter;
    }

    @Prefix("ShowAcctestJobs")
    public static class Tokenizer implements PlaceTokenizer<ShowAcctestJobsPlace> {
        @Override
        public String getToken(ShowAcctestJobsPlace place) {
            return place.getToken();
        }

        @Override
        public ShowAcctestJobsPlace getPlace(String token) {
            return new ShowAcctestJobsPlace(token);
        }
    }
}

