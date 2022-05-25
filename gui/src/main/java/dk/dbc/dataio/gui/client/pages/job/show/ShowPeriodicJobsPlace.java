package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import dk.dbc.dataio.gui.client.places.AbstractBasePlace;
import dk.dbc.dataio.gui.util.ClientFactory;

public class ShowPeriodicJobsPlace extends AbstractBasePlace {

    public ShowPeriodicJobsPlace() {
        super();
    }

    public ShowPeriodicJobsPlace(String token) {
        super(token);
    }

    @Override
    public Activity createPresenter(ClientFactory clientFactory) {
        presenter = new PresenterPeriodicJobsImpl(
                clientFactory.getPlaceController(),
                clientFactory.getGlobalViewsFactory().getPeriodicJobsView(),
                commonInjector.getMenuTexts().menu_Jobs());
        return presenter;
    }

    @Prefix("ShowPeriodicJobs")
    public static class Tokenizer implements PlaceTokenizer<ShowPeriodicJobsPlace> {
        @Override
        public String getToken(ShowPeriodicJobsPlace place) {
            return place.getToken();
        }

        @Override
        public ShowPeriodicJobsPlace getPlace(String token) {
            return new ShowPeriodicJobsPlace(token);
        }
    }
}

