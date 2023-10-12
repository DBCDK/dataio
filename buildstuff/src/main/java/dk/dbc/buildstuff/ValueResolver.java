package dk.dbc.buildstuff;

import dk.dbc.buildstuff.model.Namespace;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ValueResolver implements TokenResolver {
    private static final Pattern TOKEN = Pattern.compile("\\$\\{(?!\\$\\{)([^}]+)}");

    private String value;

    public ValueResolver(String value) {
        this.value = value;
    }

    public String getValue() {
        return value.trim();
    }

    public boolean hasTokens() {
        Matcher matcher = TOKEN.matcher(value);
        return matcher.find();
    }

    public boolean hasTokens(String deploy, String key, Namespace namespace) {
        if(value == null) throw new IllegalStateException("In " + deploy + " the token " + key + " has no value for " + namespace.getNamespace());
        return hasTokens();
    }

    public boolean resolve(Map<String, ValueResolver> scope) {
        Matcher matcher = TOKEN.matcher(value);
        if(matcher.find()) {
            String token = matcher.group(1);
            if(token.startsWith("file://")) {
                try {
                    value = insertValue(matcher, Files.readString(Main.getBasePath().resolve(token.substring(7))).trim());
                    return !hasTokens();
                } catch (IOException e) {
                    throw new IllegalArgumentException("Unable to read file reference " + token, e);
                }
            }
            ValueResolver resolver = scope.get(token);
            if(resolver != null) value = insertValue(matcher, resolver.getValue());
        }
        return !hasTokens();
    }

    private String insertValue(Matcher matcher, String insert) {
        if(insert == null) return value;
        String start = value.substring(0, matcher.start());
        String end = value.substring(matcher.end());
        return start + insert + end;
    }
}
