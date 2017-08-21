package dk.dbc.dataio.cli.lhrretriever.config;

import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class ConfigJson {
    private String openAgencyTarget;
    private String dbHost, dbName, dbUser, dbPassword;
    private int dbPort;

    public String getOpenAgencyTarget() {
        return openAgencyTarget;
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
        } catch(IOException | JSONBException e) {
            throw new ConfigParseException(String.format(
                "Error parsing config %s: %s", configPath, e.toString()), e);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(this.getClass().getSimpleName())
            .append("{openAgencyTarget=").append(openAgencyTarget)
            .append(", dbHost=").append(dbHost)
            .append(", dbName=").append(dbName)
            .append(", dbUser=").append(dbUser)
            .append(", dbPassword=").append(dbPassword)
            .append(", dbPort}").append(dbPort);
        return sb.toString();
    }
}
