package dk.dbc.dataio.gui.client.pages.sink.status;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import dk.dbc.dataio.gui.client.places.AbstractBasePlace;
import dk.dbc.dataio.gui.util.ClientFactory;

public class Place extends AbstractBasePlace {
    public Place() {
    }

    @Override
    public Activity createPresenter(ClientFactory clientFactory) {
        return new PresenterImpl(clientFactory.getPlaceController(), commonInjector.getMenuTexts().menu_SinkStatus());
    }

    @Prefix("SinkStatus")
    public static class Tokenizer implements PlaceTokenizer<Place> {
        @Override
        public String getToken(Place place) {
            return "";
        }

        @Override
        public Place getPlace(String token) {
            return new Place();
        }
    }

}

