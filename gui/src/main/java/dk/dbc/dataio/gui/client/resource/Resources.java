package dk.dbc.dataio.gui.client.resource;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

public interface Resources extends ClientBundle {
    @Source("lamp_gray.png")
    ImageResource gray();

    @Source("lamp_green.png")
    ImageResource green();

    @Source("lamp_red.png")
    ImageResource red();
}
