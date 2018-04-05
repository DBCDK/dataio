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

package dk.dbc.dataio.commons.types;

import dk.dbc.invariant.InvariantUtil;

/**
 * Objects of this class represent a harvester token with its mandatory parts
 * variant, id and version and an optional remainder, eg. variant:id:version:remainder
 */
public class HarvesterToken {
    public static HarvesterToken of(String tokenValue) {
        return new HarvesterToken(tokenValue);
    }

    public enum HarvesterVariant {
        RAW_REPO("raw-repo"),
        TICKLE_REPO("tickle-repo"),
        USH_SOLR("ush-solr");

        private final String variantName;

        HarvesterVariant(String variantName) {
            this.variantName = variantName;
        }

        @Override
        public String toString() {
            return variantName;
        }

        public static HarvesterVariant of(String variantName) {
            switch (variantName) {
                case "raw-repo": return RAW_REPO;
                case "tickle-repo": return TICKLE_REPO;
                case "ush-solr": return USH_SOLR;
                default: throw new IllegalArgumentException("Unknown variant " + variantName);
            }
        }
    }

    private HarvesterVariant harvesterVariant;
    private long id;
    private long version;
    private String remainder;

    private HarvesterToken(String tokenValue) {
        try {
            final String[] parts = tokenValue.trim().split(":", 4);
            harvesterVariant = HarvesterVariant.of(parts[0]);
            id = Long.parseLong(parts[1]);
            version = Long.parseLong(parts[2]);
            remainder = parts.length == 4 ? parts[3] : null;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid harvester token '" + tokenValue + "'", e);
        }
    }

    public HarvesterToken() {}

    public HarvesterVariant getHarvesterVariant() {
        return harvesterVariant;
    }

    public HarvesterToken withHarvesterVariant(HarvesterVariant harvesterVariant) {
        this.harvesterVariant = InvariantUtil.checkNotNullOrThrow(harvesterVariant, "harvesterVariant");
        return this;
    }

    public long getId() {
        return id;
    }

    public HarvesterToken withId(long id) {
        this.id = id;
        return this;
    }

    public long getVersion() {
        return version;
    }

    public HarvesterToken withVersion(long version) {
        this.version = version;
        return this;
    }

    public String getRemainder() {
        return remainder;
    }

    public HarvesterToken withRemainder(String remainder) {
        this.remainder = remainder;
        return this;
    }

    @Override
    public String toString() {
        return harvesterVariant + ":" + id + ":" + version + (remainder != null ? ":" + remainder : "");
    }
}
