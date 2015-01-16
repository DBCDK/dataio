package dk.dbc.dataio.gui.client.pages.sink.show;

import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class Place extends com.google.gwt.place.shared.Place {
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

