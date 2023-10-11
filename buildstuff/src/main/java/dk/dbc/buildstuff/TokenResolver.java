package dk.dbc.buildstuff;

import java.util.Map;

public interface TokenResolver {
    boolean resolve(Map<String, ValueResolver> scope);
}
