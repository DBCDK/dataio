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

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.jobstore.types.InvalidDataException;

import java.nio.charset.StandardCharsets;

/**
 * This class represents a java mail attachment
 */
public class Attachment {

    private final static String ISO2709 = "iso2709";
    private final static String LINE_FORMAT = "lin";

    private final byte[] content;
    private final String fileNameExtension;
    private final String contentType;
    private final String fileName;

    public Attachment(byte[] content, String packaging) {
        this.content = InvariantUtil.checkNotNullOrThrow(content, "content");
        this.fileNameExtension = decipherFileNameExtension(packaging);
        this.contentType = String.format("application/octet-stream; charset=%s", decipherEncoding());
        this.fileName = String.format("fejl_i_poststruktur.%s", fileNameExtension);
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

    /*
     * Private methods
     */

    /**
     * This method uses the file name extension to chose the encoding.
     * The deduction will only work as long as we receive iso as latin1
     * @return encoding
     */
    private String decipherEncoding() {
        switch (fileNameExtension) {
            case LINE_FORMAT:
                return StandardCharsets.UTF_8.name();
            case ISO2709:
                return StandardCharsets.ISO_8859_1.name();
            default:
                throw new InvalidDataException(String.format("Unknown file name extension: %s", fileNameExtension));
        }
    }

    /**
     * Since windows translates .iso to an ISO Image, the given packaging (if iso) is translated to file
     * name extension: iso2709.
     * @param packaging (iso or lin)
     * @return file name extension (lin or iso2709)
     */
    private String decipherFileNameExtension(String packaging) {
        InvariantUtil.checkNotNullNotEmptyOrThrow(packaging, "packaging");
        return packaging.equals("iso") ? ISO2709 : packaging;
    }
}
