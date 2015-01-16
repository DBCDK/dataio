package dk.dbc.dataio.gui.client.pages.sink.oldshow;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class SinksShowPlace  extends Place {
    private String sinksShowName;

    public SinksShowPlace() {
        this.sinksShowName = "";
    }

    public SinksShowPlace(String sinksShowName) {
        this.sinksShowName = sinksShowName;
    }

    public String getSinksShowName() {
        return sinksShowName;
    }

    @Prefix("ShowSinks")
    public static class Tokenizer implements PlaceTokenizer<SinksShowPlace> {
        @Override
        public String getToken(SinksShowPlace place) {
            return place.getSinksShowName();
        }

        @Override
        public SinksShowPlace getPlace(String token) {
            return new SinksShowPlace(token);
        }
    }

}

