package dk.dbc.dataio.commons.utils.test.model;

import dk.dbc.dataio.commons.types.OpenUpdateSinkConfig;

public class OpenUpdateSinkConfigBuilder {
    private String userId = "userId";
    private String password = "password";
    private String endpoint = "endpoint";

    public OpenUpdateSinkConfigBuilder setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public OpenUpdateSinkConfigBuilder setPassword(String password) {
        this.password = password;
        return this;
    }

    public OpenUpdateSinkConfigBuilder setEndpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public OpenUpdateSinkConfig build() {
        return new OpenUpdateSinkConfig(userId, password, endpoint);
    }
}
