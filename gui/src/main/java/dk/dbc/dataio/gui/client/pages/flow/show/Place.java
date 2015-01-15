package dk.dbc.dataio.gui.client.pages.flow.show;

import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class Place extends com.google.gwt.place.shared.Place {
    private String flowsShowName;

    public Place() {
        this.flowsShowName = "";
    }

    public Place(String flowsShowName) {
        this.flowsShowName = flowsShowName;
    }

    public String getFlowsShowName() {
        return flowsShowName;
    }

    @Prefix("ShowFlows")
    public static class Tokenizer implements PlaceTokenizer<Place> {
        @Override
        public String getToken(Place place) {
            return place.getFlowsShowName();
        }

        @Override
        public Place getPlace(String token) {
            return new Place(token);
        }
    }

}

