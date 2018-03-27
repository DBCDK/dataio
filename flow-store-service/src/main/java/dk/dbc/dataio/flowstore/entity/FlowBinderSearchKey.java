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

package dk.dbc.dataio.flowstore.entity;

import dk.dbc.invariant.InvariantUtil;

/**
 * Representation of FlowBinderSearchIndexEntry composite key (packaging, format, charset, destination, submitter)
 */
public final class FlowBinderSearchKey {
    private final String packaging;
    private final String format;
    private final String charset;
    private final String destination;
    private final Long submitter;

    /**
     * Class constructor
     *
     * @throws NullPointerException if given null-valued argument
     * @throws IllegalArgumentException if empty valued String argument
     */

    /**
     * Class constructor
     *
     * @param packaging packaging
     * @param format format
     * @param charset charset
     * @param destination destination
     * @param submitter submitter
     *
     * @throws NullPointerException if given null-valued argument
     * @throws IllegalArgumentException if empty valued String argument
     */
    public FlowBinderSearchKey(String packaging, String format, String charset, String destination, Long submitter) {
        this.packaging = InvariantUtil.checkNotNullNotEmptyOrThrow(packaging, "packaging");
        this.format = InvariantUtil.checkNotNullNotEmptyOrThrow(format, "format");
        this.charset = InvariantUtil.checkNotNullNotEmptyOrThrow(charset, "charset");
        this.destination = InvariantUtil.checkNotNullNotEmptyOrThrow(destination, "destination");
        this.submitter = InvariantUtil.checkNotNullOrThrow(submitter, "submitter");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final FlowBinderSearchKey that = (FlowBinderSearchKey) o;

        if (!charset.equals(that.charset)) {
            return false;
        }
        if (!destination.equals(that.destination)) {
            return false;
        }
        if (!format.equals(that.format)) {
            return false;
        }
        if (!packaging.equals(that.packaging)) {
            return false;
        }
        if (!submitter.equals(that.submitter)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = packaging.hashCode();
        result = 31 * result + format.hashCode();
        result = 31 * result + charset.hashCode();
        result = 31 * result + destination.hashCode();
        result = 31 * result + submitter.hashCode();
        return result;
    }
}
