package dk.dbc.dataio.gui.client.pages.flowbinder.show;

import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class Place extends com.google.gwt.place.shared.Place {
    private String flowBindersShowName;

    public Place() {
        this.flowBindersShowName = "";
    }

    public Place(String flowBindersShowName) {
        this.flowBindersShowName = flowBindersShowName;
    }

    public String getFlowBindersShowName() {
        return flowBindersShowName;
    }

    @Prefix("ShowFlowBinders")
    public static class Tokenizer implements PlaceTokenizer<Place> {
        @Override
        public String getToken(Place place) {
            return place.getFlowBindersShowName();
        }

        @Override
        public Place getPlace(String token) {
            return new Place(token);
        }
    }

}
