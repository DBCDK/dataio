package dk.dbc.dataio.gui.client.resource;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

public interface ImageResources extends ClientBundle {
    @ImageResource.ImageOptions(preventInlining=true)
    @Source("lamp_gray.png")
    ImageResource gray();

    @ImageResource.ImageOptions(preventInlining=true)
    @Source("lamp_green.png")
    ImageResource green();

    @ImageResource.ImageOptions(preventInlining=true)
    @Source("lamp_red.png")
    ImageResource red();
}
