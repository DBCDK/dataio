package dk.dbc.dataio.commons.utils.test.json;

import java.util.List;

/**
 * Abstract base class for JSON content builders
 */
public abstract class JsonBuilder {
    protected static final String MEMBER_DELIMITER = ", ";
    protected static final String NULL_VALUE = "null";
    protected static final String START_ARRAY = "[";
    protected static final String END_ARRAY = "]";
    protected static final String START_OBJECT = "{";
    protected static final String END_OBJECT = "}";

    protected String asTextMember(String memberName, String memberValue) {
        if (memberValue == null) {
            return String.format("\"%s\": null", memberName);
        }
        return String.format("\"%s\": \"%s\"", memberName, memberValue);
    }

    protected String asLongMember(String memberName, Long memberValue) {
        final String memberValueAsString = (memberValue == null) ? NULL_VALUE
                : Long.toString(memberValue);
        return String.format("\"%s\": %s", memberName, memberValueAsString);
    }

    protected String asObjectMember(String memberName, String memberValue) {
        final String memberValueAsString = (memberValue == null) ? NULL_VALUE
                : memberValue;
        return String.format("\"%s\": %s", memberName, memberValueAsString);
    }

    protected String asBooleanMember(String memberName, boolean memberValue) {
        final String memberValueAsString = memberValue ? "true"
                : "false";
        return String.format("\"%s\": %s", memberName, memberValueAsString);
    }

    protected String asTextArray(String memberName, List<String> memberValues) {
        final String memberValuesAsString = (memberValues == null) ? NULL_VALUE
                : String.format("%s%s%s", START_ARRAY, joinTextValues(",", memberValues), END_ARRAY);
        return String.format("\"%s\": %s", memberName, memberValuesAsString);
    }

    protected String asObjectArray(String memberName, List<String> memberValues) {
        final String memberValuesAsString = (memberValues == null) ? NULL_VALUE
                : String.format("%s%s%s", START_ARRAY, joinNonTextValues(",", memberValues), END_ARRAY);
        return String.format("\"%s\": %s", memberName, memberValuesAsString);
    }

    protected String asLongArray(String memberName, List<Long> memberValues) {
        final String memberValuesAsString = (memberValues == null) ? NULL_VALUE
                : String.format("%s%s%s", START_ARRAY, joinLongs(",", memberValues), END_ARRAY);
        return String.format("\"%s\": %s", memberName, memberValuesAsString);
    }

    protected String joinTextValues(String delimiter, List<String> memberValues) {
        final StringBuilder stringbuilder = new StringBuilder();
        for (String memberValue : memberValues) {
            final String value = (memberValue != null) ? "\"" + memberValue + "\"" : NULL_VALUE;
            stringbuilder.append(value).append(delimiter);
        }
        return stringbuilder.toString().replaceFirst(String.format("%s$", delimiter), "");
    }

    protected String joinNonTextValues(String delimiter, List<String> memberValues) {
        final StringBuilder stringbuilder = new StringBuilder();
        for (String memberValue : memberValues) {
            final String value = (memberValue != null) ? memberValue : NULL_VALUE;
            stringbuilder.append(value).append(delimiter);
        }
        return stringbuilder.toString().replaceFirst(String.format("%s$", delimiter), "");
    }

    protected String joinLongs(String delimiter, List<Long> ids) {
        final StringBuilder stringbuilder = new StringBuilder();
        for (Long id : ids) {
            final String idAsString = (id != null) ? Long.toString(id) : NULL_VALUE;
            stringbuilder.append(idAsString).append(delimiter);
        }
        return stringbuilder.toString().replaceFirst(String.format("%s$", delimiter), "");
    }
}
