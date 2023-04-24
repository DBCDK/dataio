package dk.dbc.dataio.gui.client.pages.flowbinder.status;


import com.google.gwt.activity.shared.Activity;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import dk.dbc.dataio.gui.client.places.AbstractBasePlace;
import dk.dbc.dataio.gui.util.ClientFactory;

public class Place extends AbstractBasePlace {

    public Place() { super();}
    public Place(String token) {super(token);}

    @Override
    public Activity createPresenter(ClientFactory clientFactory) {
        return new PresenterImpl(clientFactory.getPlaceController(), commonInjector
                .getMenuTexts().menu_FlowBinderStatus());
    }

    @Prefix("Flowbinderstatus")
    public static class Tokenizer implements PlaceTokenizer<Place> {

        @Override
        public String getToken(Place place) {
            return place.getToken();
        }

        @Override
        public Place getPlace(String token) {
            return new Place(token);
        }
    }
}
