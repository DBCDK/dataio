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

package dk.dbc.dataio.sink.worldcat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

public class Checksum {
    private Checksum() {}

    /**
     * Checksum generator taking chunk item data, holdings and LHR flag into account
     * @param chunkItemWithWorldCatAttributes chunk item with WorldCat attributes
     * @return checksum
     */
    public static String of(ChunkItemWithWorldCatAttributes chunkItemWithWorldCatAttributes) {
        // MessageDigest algorithm implementations are not thread safe
        final SHA1 sha1 = new SHA1();
        sha1.add(chunkItemWithWorldCatAttributes.getData());
        chunkItemWithWorldCatAttributes.getWorldCatAttributes().getHoldings().stream()
                .sorted()
                .map(holding -> holding.toString().getBytes(StandardCharsets.UTF_8))
                .forEachOrdered(sha1::add);
        sha1.add(chunkItemWithWorldCatAttributes.getWorldCatAttributes().hasLhr()
                ? new byte[] { (byte)1 }
                : new byte[] { (byte)0 });
        return sha1.compute();
    }

    private static class SHA1 {
        final MessageDigest md;

        SHA1() {
            try {
                md = MessageDigest.getInstance("SHA1");
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
        }

        void add(byte[] bytes) {
            md.update(bytes);
        }

        String compute() {
            return toString(md.digest());
        }

        private String toString(byte[] digest) {
            final StringBuilder hexString = new StringBuilder(digest.length * 2);
            final Formatter hexStringFormatter = new Formatter(hexString);
            for (byte b : digest) {
                // each java byte is represented as a 2-digit hex string
                hexStringFormatter.format("%02x", b);
            }
            return hexString.toString();
        }
    }
}
