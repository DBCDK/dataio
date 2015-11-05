package dk.dbc.dataio.commons.utils.test.json;

import dk.dbc.dataio.commons.types.OpenUpdateSinkConfig;

public class OpenUpdateSinkConfigJsonBuilder extends JsonBuilder {
    private String userId = "defaultUserId";
    private String password = "defaultPassword";
    private String endpoint = "defaultEndpoint";

    public OpenUpdateSinkConfigJsonBuilder setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public OpenUpdateSinkConfigJsonBuilder setPassword(String password) {
        this.password = password;
        return this;
    }

    public OpenUpdateSinkConfigJsonBuilder setEndpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public String build() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(START_OBJECT);
        stringBuilder.append(asTextMember("@class", OpenUpdateSinkConfig.class.getTypeName()));
        stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("userId", userId));
        stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("password", password));
        stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("endpoint", endpoint));
        stringBuilder.append(END_OBJECT);
        return stringBuilder.toString();
    }

}
