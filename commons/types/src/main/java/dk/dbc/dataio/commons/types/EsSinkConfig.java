package dk.dbc.dataio.commons.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import dk.dbc.invariant.InvariantUtil;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class EsSinkConfig implements SinkConfig, Serializable {

    private static final long serialVersionUID = 3028558873084758575L;
    private Integer userId;
    private String databaseName;
    private String esAction;

    public EsSinkConfig() {
        this.esAction = "INSERT";
    }

    public Integer getUserId() {
        return userId;
    }

    public EsSinkConfig withUserId(Integer userId) throws IllegalArgumentException {
        this.userId = InvariantUtil.checkNotNullOrThrow(userId, "userId");
        return this;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public EsSinkConfig withDatabaseName(String databaseName) throws IllegalArgumentException {
        this.databaseName = InvariantUtil.checkNotNullNotEmptyOrThrow(databaseName, "databaseName");
        return this;
    }

    public String getEsAction() {
        return esAction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EsSinkConfig)) return false;

        EsSinkConfig that = (EsSinkConfig) o;

        if (userId != null ? !userId.equals(that.userId) : that.userId != null) return false;
        if (databaseName != null ? !databaseName.equals(that.databaseName) : that.databaseName != null) return false;
        return esAction != null ? esAction.equals(that.esAction) : that.esAction == null;

    }

    @Override
    public int hashCode() {
        int result = userId != null ? userId.hashCode() : 0;
        result = 31 * result + (databaseName != null ? databaseName.hashCode() : 0);
        result = 31 * result + (esAction != null ? esAction.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "EsSinkConfig{" +
                "userId=" + userId +
                ", databaseName='" + databaseName + '\'' +
                ", esAction='" + esAction + '\'' +
                '}';
    }
}
