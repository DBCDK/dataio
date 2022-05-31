package dk.dbc.dataio.cli.lhrretriever.config;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class ConfigJson {
    private String vipCoreEndpoint;
    private String ocn2pidServiceTarget;
    private String flowStoreEndpoint;
    private String dbHost, dbName, dbUser, dbPassword;
    private int dbPort;

    public String getVipCoreEndpoint() {
        return vipCoreEndpoint;
    }

    public String getOcn2pidServiceTarget() {
        return ocn2pidServiceTarget;
    }

    public String getFlowStoreEndpoint() {
        return flowStoreEndpoint;
    }

    public String getDbHost() {
        return dbHost;
    }

    public String getDbName() {
        return dbName;
    }

    public String getDbUser() {
        return dbUser;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public int getDbPort() {
        return dbPort;
    }

    public static ConfigJson parseConfig(String configPath) throws ConfigParseException {
        try {
            File file = new File(configPath);
            byte[] bytes = Files.readAllBytes(file.toPath());
            String config = new String(bytes, StandardCharsets.UTF_8);
            JSONBContext jsonbContext = new JSONBContext();
            return jsonbContext.unmarshall(config, ConfigJson.class);
        } catch (IOException | JSONBException e) {
            throw new ConfigParseException(String.format(
                    "Error parsing config %s: %s", configPath, e.toString()), e);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(this.getClass().getSimpleName())
                .append("{vipCoreEndpoint=").append(vipCoreEndpoint)
                .append(", ocn2pidServiceTarget=").append(ocn2pidServiceTarget)
                .append(", flowStoreEndpoint=").append(flowStoreEndpoint)
                .append(", dbHost=").append(dbHost)
                .append(", dbName=").append(dbName)
                .append(", dbUser=").append(dbUser)
                .append(", dbPassword=").append(dbPassword)
                .append(", dbPort}").append(dbPort);
        return sb.toString();
    }
}
