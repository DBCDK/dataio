package dk.dbc.dataio.harvester.types;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;

/*
    This stinks beyond reason - GWT serialization won't work with
    an interface or an abstract class, so this needs to be
    actual instantiable type.
*/

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "type")
public class Pickup implements Serializable {
    private String overrideFilename;
    private String contentHeader;
    private String contentFooter;

    Pickup() {
    }

    public String getOverrideFilename() {
        return overrideFilename;
    }

    public Pickup withOverrideFilename(String overrideFilename) throws UnsupportedOperationException {
        this.overrideFilename = overrideFilename;
        return this;
    }

    public String getContentHeader() {
        return contentHeader;
    }

    public String getContentFooter() {
        return contentFooter;
    }

    public Pickup withContentHeader(String contentHeader) throws UnsupportedOperationException {
        this.contentHeader = contentHeader;
        return this;
    }

    public Pickup withContentFooter(String contentFooter) throws UnsupportedOperationException {
        this.contentFooter = contentFooter;
        return this;
    }
}
