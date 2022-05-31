package dk.dbc.dataio.gui.client.pages.harvester.corepo.modify;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import dk.dbc.dataio.gui.client.places.AbstractBasePlace;
import dk.dbc.dataio.gui.util.ClientFactory;
import dk.dbc.dataio.harvester.types.CoRepoHarvesterConfig;

public class EditPlace extends AbstractBasePlace {
    private Long harvesterId;

    public EditPlace(String url) {
        this.harvesterId = Long.valueOf(url);
    }

    public EditPlace(CoRepoHarvesterConfig config) {
        this.harvesterId = config.getId();
    }

    Long getHarvesterId() {
        return harvesterId;
    }

    @Override
    public Activity createPresenter(ClientFactory clientFactory) {
        return new PresenterEditImpl(this, commonInjector.getMenuTexts().menu_HarvesterEdit());
    }

    @Prefix("EditCorepoHarvester")
    public static class Tokenizer implements PlaceTokenizer<EditPlace> {
        @Override
        public String getToken(EditPlace place) {
            return String.valueOf(place.getHarvesterId());
        }

        @Override
        public EditPlace getPlace(String token) {
            return new EditPlace(token);
        }
    }
}
