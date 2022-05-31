package dk.dbc.dataio.gui.client.pages.harvester.infomedia.modify;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import dk.dbc.dataio.gui.client.places.AbstractBasePlace;
import dk.dbc.dataio.gui.util.ClientFactory;

public class CreatePlace extends AbstractBasePlace {
    public CreatePlace() {
    }

    @Override
    public Activity createPresenter(ClientFactory clientFactory) {
        return new PresenterCreateImpl(commonInjector.getMenuTexts().menu_HarvesterEdit());
    }

    @Prefix("CreateInfomediaHarvester")
    public static class Tokenizer implements PlaceTokenizer<CreatePlace> {
        @Override
        public String getToken(CreatePlace place) {
            return "";
        }

        @Override
        public CreatePlace getPlace(String token) {
            return new CreatePlace();
        }
    }
}
