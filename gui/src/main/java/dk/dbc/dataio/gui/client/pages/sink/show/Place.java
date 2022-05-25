package dk.dbc.dataio.gui.client.pages.sink.show;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import dk.dbc.dataio.gui.client.places.AbstractBasePlace;
import dk.dbc.dataio.gui.util.ClientFactory;

public class Place extends AbstractBasePlace {

    private String sinksShowName;

    public Place() {
        this.sinksShowName = "";
    }

    public Place(String sinksShowName) {
        this.sinksShowName = sinksShowName;
    }

    public String getSinksShowName() {
        return sinksShowName;
    }

    @Override
    public Activity createPresenter(ClientFactory clientFactory) {
        return new PresenterImpl(clientFactory.getPlaceController(), commonInjector.getMenuTexts().menu_Sinks());
    }

    @Prefix("ShowSinks")
    public static class Tokenizer implements PlaceTokenizer<Place> {
        @Override
        public String getToken(Place place) {
            return place.getSinksShowName();
        }

        @Override
        public Place getPlace(String token) {
            return new Place(token);
        }
    }

}

