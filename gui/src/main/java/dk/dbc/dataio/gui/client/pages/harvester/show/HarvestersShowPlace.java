package dk.dbc.dataio.gui.client.pages.harvester.show;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

/**
 * Created by sma on 25/04/14.
 */
public class HarvestersShowPlace extends Place {
    private String harvestersShowName;

    public HarvestersShowPlace() {
        this.harvestersShowName = "";
    }

    public HarvestersShowPlace(String harvestersShowName) {
        this.harvestersShowName = harvestersShowName;
    }

    public String getHarvestersShowName() {
        return harvestersShowName;
    }

    @Prefix("ShowHarvesters")
    public static class Tokenizer implements PlaceTokenizer<HarvestersShowPlace> {
        @Override
        public String getToken(HarvestersShowPlace place) {
            return place.getHarvestersShowName();
        }

        @Override
        public HarvestersShowPlace getPlace(String token) {
            return new HarvestersShowPlace(token);
        }
    }
}
