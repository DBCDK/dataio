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

package dk.dbc.dataio.commons.utils.lang;

import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;

import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * This class handles pretty printing of various formats.
 */
public abstract class PrettyPrint {

    private static final JSONBContext jsonbContext = new JSONBContext();

    private static final String LINEBREAK = System.lineSeparator();
    private static final String EMPTY = "";
    private static final String TAB = "\t";


    /**
     *  Determines if data given as input is xml alike and should be pretty printed as xml:
     *  if TRUE : returns pretty printed xml String
     *  if FALSE: Returns data as String
     * @param bytes containing data
     * @param encoding of data
     * @return data as String
     */
    public static String asXml(byte[] bytes, Charset encoding) {
        String data = StringUtil.asString(bytes, encoding);
        // Might be xml alike and should be displayed as xml even though the xml string starts with a number...
        // check for the xmlns attribute.
        if (data.startsWith("<")) {
            return asXml(data);
        } else {
            return data;
        }
    }

    /**
     * Adds tabs and new lines to a json string
     * @param bytes containing data
     * @param encoding of data
     * @return he string pretty printed as json
     * @throws JSONBException on failure to to unmarshall
     */
    public static String asJson(byte[] bytes, Charset encoding) throws JSONBException {
        String json = StringUtil.asString(bytes, encoding);
        return jsonbContext.prettyPrint(json).replaceAll(" {2}", TAB);
    }

    /**
     * Combines string elements to print
     * @param elements to print
     * @return the elements combined as one String
     */
    public static String combinePrintElements(String... elements) {
        StringBuilder stringBuilder = new StringBuilder();
        for(String element : elements) {
            if(stringBuilder.length() == 0) {
                stringBuilder.append(element);
            } else {
                stringBuilder.append(LINEBREAK).append(LINEBREAK).append(element);
            }
        }
        return stringBuilder.toString();
    }

    /*
     * private methods
     */

    /**
     * Adds tabs and new lines to a xml string
     * @param xml the string to pretty print as xml
     * @return the string pretty printed as xml
     */
    private static String asXml(String xml) {
        final StringBuffer prettyPrintXml = new StringBuffer();

        /* Group the xml tags */
        Pattern pattern = Pattern.compile("(<[^/][^>]+>)?([^<]*)(</[^>]+>)?(<[^/][^>]+/>)?");
        Matcher matcher = pattern.matcher(xml);
        int tabCount = 0;
        while (matcher.find()) {
            String str1 = null == matcher.group(1) || "null".equals(matcher.group()) ? EMPTY : matcher.group(1);
            String str2 = null == matcher.group(2) || "null".equals(matcher.group()) ? EMPTY : matcher.group(2);
            String str3 = null == matcher.group(3) || "null".equals(matcher.group()) ? EMPTY : matcher.group(3);
            String str4 = null == matcher.group(4) || "null".equals(matcher.group()) ? EMPTY : matcher.group(4);

            if (matcher.group() != null && !matcher.group().trim().equals(EMPTY)) {
                printTabs(tabCount, prettyPrintXml);
                if (!str1.equals(EMPTY) && str3.equals(EMPTY)) {
                    ++tabCount;
                }
                if (str1.equals(EMPTY) && !str3.equals(EMPTY)) {
                    --tabCount;
                    prettyPrintXml.deleteCharAt(prettyPrintXml.length() - 1);
                }
                prettyPrintXml.append(str1);
                prettyPrintXml.append(str2);
                prettyPrintXml.append(str3);
                if (!str4.equals(EMPTY)) {
                    prettyPrintXml.append(LINEBREAK);
                    printTabs(tabCount, prettyPrintXml);
                    prettyPrintXml.append(str4);
                }
                if (!str2.equals(LINEBREAK)){
                    prettyPrintXml.append(LINEBREAK);
                }
            }
        }
        return prettyPrintXml.toString();
    }

    private static void printTabs(int count, StringBuffer stringBuffer) {
        for (int i = 0; i < count; i++) {
            stringBuffer.append(TAB);
        }
    }
}
