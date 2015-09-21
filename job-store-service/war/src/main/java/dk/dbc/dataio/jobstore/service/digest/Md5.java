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

package dk.dbc.dataio.jobstore.service.digest;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

public class Md5 {
    private final static String MD5_ALGORITHM = "MD5";

    /* Private constructor in order to keep class static */
    private Md5() {}

    /**
     * Calculates the MD5 digest and returns the value as a 32 character hex string.
     * @param data data to digest
     * @return MD5 digest as a hex string
     * @throws NullPointerException when given null valued data argument
     * @throws IllegalStateException when unable to utilize md5 algorithm
     */
    public static String asHex(byte[] data) throws NullPointerException, IllegalStateException {
        // MessageDigest algorithm implementations are not tread safe, so we
        // create a new instance each time
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(MD5_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            // transform NoSuchAlgorithmException into run-time exception
            throw new IllegalStateException(e);
        }
        md.update(data);
        return hashAsString(md.digest());
    }

    private static String hashAsString(byte[] hashBytes) {
        StringBuilder hexString = new StringBuilder(hashBytes.length*2);
        Formatter hexStringFormatter = new Formatter(hexString);
        for(byte b : hashBytes) {
            // each java byte is represented as a 2-digit hex string
            hexStringFormatter.format("%02x", b);
        }
        return hexString.toString();
    }
}
