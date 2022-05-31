package dk.dbc.dataio.gui.client.pages.flow.show;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import dk.dbc.dataio.gui.client.places.AbstractBasePlace;
import dk.dbc.dataio.gui.util.ClientFactory;

public class Place extends AbstractBasePlace {
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

    @Override
    public Activity createPresenter(ClientFactory clientFactory) {
        return new PresenterImpl(clientFactory.getPlaceController());
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

