package dk.dbc.dataio.gui.client.pages.gatekeeper.ftp.show;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import dk.dbc.dataio.gui.client.places.AbstractBasePlace;
import dk.dbc.dataio.gui.util.ClientFactory;

public class Place extends AbstractBasePlace {
    private String ftpShowName;
    public Place() { this.ftpShowName = "";}
    public Place(String ftpShowName) { this.ftpShowName = ftpShowName;}

    public String getFtpShowName() { return ftpShowName;}

    @Override
    public Activity createPresenter(ClientFactory clientFactory) {
        return new PresenterImpl(clientFactory.getPlaceController(), commonInjector
                .getMenuTexts().menu_Ftp());
    }

    @Prefix("ShowFtp")
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
