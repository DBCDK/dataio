package dk.dbc.dataio.gui.client.places;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class SubmittersShowPlace  extends Place {
    private String submittersShowName;

    public SubmittersShowPlace() {
        this.submittersShowName = "";
    }

    public SubmittersShowPlace(String submttersShowName) {
        this.submittersShowName = submttersShowName;
    }

    public String getSubmittersShowName() {
        return submittersShowName;
    }

    @Prefix("ShowSubmitters")
    public static class Tokenizer implements PlaceTokenizer<SubmittersShowPlace> {
        @Override
        public String getToken(SubmittersShowPlace place) {
            return place.getSubmittersShowName();
        }

        @Override
        public SubmittersShowPlace getPlace(String token) {
            return new SubmittersShowPlace(token);
        }
    }

}

