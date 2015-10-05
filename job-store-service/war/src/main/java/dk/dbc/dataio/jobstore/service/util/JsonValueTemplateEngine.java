/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.jobstore.service.util;

import com.fasterxml.jackson.databind.JsonNode;
import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * Simple template engine taking a string template containing properties on
 * the form ${json_path} or using the date macro form __DATE__{json_path}
 * where json_path is a used to select property values from a given json document,
 * where each path element is separated by a dot '.'.
 * </p>
 * <p>
 * The json path given to the __DATE__ macro assumes that the selected property
 * value is a numeric value representing the number of milliseconds since
 * January 1, 1970, 00:00:00 GMT.
 * </p>
 */
public class JsonValueTemplateEngine {
    private static final String EMPTY_VALUE = "";

    private final JSONBContext jsonbContext;
    private final Pattern pattern = Pattern.compile("(\\$|__DATE__)\\{(.+?)\\}");

    public JsonValueTemplateEngine() {
        jsonbContext = new JSONBContext();
    }

    public JsonValueTemplateEngine(JSONBContext jsonbContext) throws NullPointerException {
        this.jsonbContext = InvariantUtil.checkNotNullOrThrow(jsonbContext, "jsonbContext");
    }

    /**
     * Applies property substitution to given template
     * @param template template with ${json_path} or __DATE__{json_path} properties
     * @param json json value used to lookup property values
     * @return result of substituting property values in the template
     * @throws NullPointerException if given null-valued argument
     * @throws IllegalArgumentException if given invalid json string
     */
    public String apply(String template, String json) throws NullPointerException, IllegalArgumentException {
        final JsonNode jsonTree;
        try {
            jsonTree = jsonbContext.getJsonTree(json);
        } catch (JSONBException e) {
            throw new IllegalArgumentException(e);
        }

        final Matcher matcher = pattern.matcher(template);
        // StringBuilder cannot be used here because Matcher expects StringBuffer
        final StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            final String[] path = matcher.group(2).split("\\.");
            String replacement = getReplacementValue(path, jsonTree);
            if ("__DATE__".equals(matcher.group(1))) {
                replacement = asDateString(replacement);
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);

        return buffer.toString();
    }

    private String getReplacementValue(String[] pathElements, JsonNode jsonTree) {
        final LinkedList<String> path = new LinkedList<>(Arrays.asList(pathElements));
        if (path.isEmpty()) {
            return EMPTY_VALUE;
        }
        final LinkedList<String> accumulator = new LinkedList<>();
        getReplacementValue(path, jsonTree, accumulator);
        return join(accumulator, "\n");
    }

    private void getReplacementValue(LinkedList<String> path, JsonNode jsonTree, LinkedList<String> accumulator) {
        final JsonNode node;
        if (jsonTree.isArray() || path.isEmpty()) {
            node = jsonTree;
        } else {
            node = jsonTree.path(path.remove());
        }

        switch (node.getNodeType()) {
            case ARRAY:
                for (final JsonNode arrayElement : jsonTree) {
                    // handle array value through recursive call
                    getReplacementValue(new LinkedList<>(path), arrayElement, accumulator);
                }
                break;

            case OBJECT:
                // handle complex object through recursive call
                getReplacementValue(path, node, accumulator);
                break;

            case STRING:
            case NUMBER:
                // Only simple value left ...
                if (path.isEmpty()) {
                    // extract value
                    final String textValue = node.asText();
                    accumulator.add(textValue.equals(Constants.MISSING_FIELD_VALUE) ? EMPTY_VALUE : textValue);
                } else {
                    // but path indicate still further steps
                    accumulator.add(EMPTY_VALUE);
                }
                break;

            case MISSING:
            default:
                break;
        }
    }

    private String asDateString(String longValue) {
        String dateString;
        try {
            final Long time = Long.valueOf(longValue);
            dateString = new Date(time).toString();
        } catch (Exception e) {
            dateString = EMPTY_VALUE;
        }
        return dateString;
    }

    // sure wish we had java 8 by now
    private String join(List<String> strings, String separator) {
        final StringBuilder buffer = new StringBuilder();
        String actualSeparator = "";
        for (String s: strings) {
            buffer.append(actualSeparator).append(s);
            actualSeparator = separator;
        }
        return buffer.toString();
    }
}
