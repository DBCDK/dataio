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

import dk.dbc.invariant.InvariantUtil;

import java.nio.charset.Charset;

/**
 * This class represents a java mail attachment
 */
public class Attachment {
    private final static String EXTENSION_ISO2709 = "iso2709";
    private final static String EXTENSION_LINE_FORMAT = "lin";
    private final static String EXTENSION_XML = "xml";
    private final static String EXTENSION_TEXT = "txt";

    private final byte[] content;
    private final String contentType;
    private final String fileName;

    public Attachment(byte[] content, String filename, Charset charset) {
        this.content = InvariantUtil.checkNotNullOrThrow(content, "content");
        this.contentType = String.format("application/octet-stream; charset=%s",
                InvariantUtil.checkNotNullOrThrow(charset, "charset").name());
        this.fileName = InvariantUtil.checkNotNullNotEmptyOrThrow(filename, "filename");
    }

    public byte[] getContent() {
        return content;
    }

    public String getContentType() {
        return contentType;
    }

    public String getFileName() {
        return fileName;
    }

    /**
     * Returns character set implementation for given name
     * @param charsetName name of character set
     * @return Charset object
     * @throws NullPointerException if given null-valued charsetName
     * @throws IllegalArgumentException if given empty-valued charsetName, if charsetName is illegal,
     * or if support for the named charset is not available in this instance of the Java virtual machine
     */
    public static Charset decipherCharset(String charsetName) throws NullPointerException, IllegalArgumentException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(charsetName, "charsetName");
        return Charset.forName(charsetName);
    }

    /**
     * Returns filename extension for given job packaging.
     * Since windows translates .iso to an ISO image, an 'iso'
     * packaging is translated to filename extension 'iso2709'.
     * @param packaging job packaging
     * @return file name extension, defaults to .txt
     * @throws NullPointerException if given null-valued packaging
     * @throws IllegalArgumentException if given empty-value packaging
     */
    public static String decipherFileNameExtensionFromPackaging(String packaging)
            throws NullPointerException, IllegalArgumentException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(packaging, "packaging");
        packaging = packaging.toLowerCase();
        packaging = packaging.replace("addi-", "");
        switch (packaging) {
            case "iso": return EXTENSION_ISO2709;
            case "lin": return EXTENSION_LINE_FORMAT;
            case "xml": return EXTENSION_XML;
            default: return EXTENSION_TEXT;
        }
    }
}
