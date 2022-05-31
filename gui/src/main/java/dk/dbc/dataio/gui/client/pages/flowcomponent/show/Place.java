package dk.dbc.dataio.gui.client.pages.flowcomponent.show;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import dk.dbc.dataio.gui.client.places.AbstractBasePlace;
import dk.dbc.dataio.gui.util.ClientFactory;

public class Place extends AbstractBasePlace {
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

    @Override
    public Activity createPresenter(ClientFactory clientFactory) {
        return new PresenterImpl(clientFactory.getPlaceController(), commonInjector.getMenuTexts().menu_FlowComponents());
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

