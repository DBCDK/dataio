package dk.dbc.dataio.commons.types;

import java.util.List;
import java.util.Objects;

public class ParameterSuggestion {
    String name;
    List<String> values;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }
    
    public ParameterSuggestion withName(String name) {
        this.name = name;
        return this;
    }
    public ParameterSuggestion withValues(List<String> values) {
        this.values = values;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParameterSuggestion that = (ParameterSuggestion) o;
        return Objects.equals(name, that.name) && Objects.equals(values, that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, values);
    }

    @Override
    public String toString() {
        return "ParameterSuggestion{" +
                "name='" + name + '\'' +
                ", values=" + values +
                '}';
    }
}
