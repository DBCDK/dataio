/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dbc.dataio.gui.client.pages.flow.oldmodify;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

/**
 * CreatePlace
 */
public class CreatePlace extends Place {

    public CreatePlace() {}

    @Prefix("OldCreateFlow")
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
