/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dbc.dataio.gui.client.pages.flow.modify;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import dk.dbc.dataio.gui.client.places.AbstractBasePlace;
import dk.dbc.dataio.gui.util.ClientFactory;

/**
 * CreatePlace
 */
public class CreatePlace extends AbstractBasePlace {

    public CreatePlace() {
    }

    @Override
    public Activity createPresenter(ClientFactory clientFactory) {
        return new PresenterCreateImpl(clientFactory.getPlaceController(), commonInjector.getMenuTexts().menu_FlowCreation());
    }

    @Prefix("CreateFlow")
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
