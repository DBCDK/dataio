package dk.dbc.dataio.gui.client.pages.submitter.show;

import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class Place extends com.google.gwt.place.shared.Place {
    private String submittersShowName;

    public Place() {
        this.submittersShowName = "";
    }

    public Place(String submttersShowName) {
        this.submittersShowName = submttersShowName;
    }

    public String getSubmittersShowName() {
        return submittersShowName;
    }

    @Prefix("ShowSubmitters")
    public static class Tokenizer implements PlaceTokenizer<Place> {
        @Override
        public String getToken(Place place) {
            return place.getSubmittersShowName();
        }

        @Override
        public Place getPlace(String token) {
            return new Place(token);
        }
    }

}

