package dk.dbc.dataio.gui.client.pages.flowcomponent.show;

import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class Place extends com.google.gwt.place.shared.Place {
    private String flowComponentsShowName;

    public Place() {
        this.flowComponentsShowName = "";
    }

    public Place(String flowComponentsShowName) {
        this.flowComponentsShowName = flowComponentsShowName;
    }

    public String getFlowComponentsShowName() {
        return flowComponentsShowName;
    }

    @Prefix("ShowFlowComponents")
    public static class Tokenizer implements PlaceTokenizer<Place> {
        @Override
        public String getToken(Place place) {
            return place.getFlowComponentsShowName();
        }

        @Override
        public Place getPlace(String token) {
            return new Place(token);
        }
    }
    
}

