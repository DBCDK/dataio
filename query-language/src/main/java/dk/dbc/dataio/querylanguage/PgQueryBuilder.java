/*
 * DataIO - Data IO
 *
 * Copyright (C) 2017 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
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

package dk.dbc.dataio.querylanguage;

public class PgQueryBuilder {
    private StringBuilder buffer;

    public PgQueryBuilder init() {
        buffer = new StringBuilder();
        return this;
    }

    public PgQueryBuilder binaryClause(Token ident, Token operator, Token value) {
        final Identifier identifier = Identifier.of(ident);
        if (buffer.length() == 0) {
            beginSelectExpression(identifier);
        }
        buffer.append(identifier.getField())
              .append(' ').append(operator.image).append(' ')
              .append(unQuote(value));
        return this;
    }

    public PgQueryBuilder and() {
        buffer.append(" AND ");
        return this;
    }

    public PgQueryBuilder or() {
        buffer.append(" OR ");
        return this;
    }

    public PgQueryBuilder lparenthesis() {
        buffer.append("(");
        return this;
    }

    public PgQueryBuilder rparenthesis() {
        buffer.append(")");
        return this;
    }

    public String build() {
        return buffer.toString();
    }

    private void beginSelectExpression(Identifier identifier) {
        buffer.append("SELECT * FROM ").append(identifier.getResource()).append(" WHERE ");
    }

    private String unQuote(Token token) {
        if (token.image.charAt(0) == '"') {
            return "'" + token.image.substring(1, token.image.length() - 1) + "'";
        }
        return token.image;
    }
}
