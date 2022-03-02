package dk.dbc.dataio.commons.utils.test.model;

import dk.dbc.dataio.commons.types.GatekeeperDestination;

public class GatekeeperDestinationBuilder {

    private long id = 42L;
    private String submitterNumber = "123456";
    private String destination = "destination";
    private String packaging = "packaging";
    private String format = "format";

    public GatekeeperDestinationBuilder setId(long id) {
        this.id = id;
        return this;
    }

    public GatekeeperDestinationBuilder setSubmitterNumber(String submitterNumber) {
        this.submitterNumber = submitterNumber;
        return this;
    }

    public GatekeeperDestinationBuilder setDestination(String destination) {
        this.destination = destination;
        return this;
    }

    public GatekeeperDestinationBuilder setPackaging(String packaging) {
        this.packaging = packaging;
        return this;
    }

    public GatekeeperDestinationBuilder setFormat(String format) {
        this.format = format;
        return this;
    }

    public GatekeeperDestination build() {
        return new GatekeeperDestination(id, submitterNumber, destination, packaging, format);
    }
}
