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
    private StringBuilder buffer = new StringBuilder();
    private String resource;
    private boolean countQuery = false;
    private boolean leadingNot = false;
    private boolean firstOrderBy = true;

    public PgQueryBuilder init() {
        buffer = new StringBuilder();
        countQuery = false;
        return this;
    }

    public PgQueryBuilder setCountQuery() {
        countQuery = true;
        return this;
    }

    public PgQueryBuilder binaryClause(Token ident, Token operator, Token value) throws ParseException {
        final Identifier identifier = Identifier.of(ident);
        if (buffer.length() == 0) {
            beginSelectExpression(identifier);
        } else {
            assertResource(identifier);
        }
        buffer.append(getFieldPath(identifier.getField()))
              .append(' ').append(operator.image).append(' ')
              .append(value.image);
        return this;
    }

    public PgQueryBuilder unaryClause(Token ident, Token operator) throws ParseException {
        final Identifier identifier = Identifier.of(ident);
        if (buffer.length() == 0) {
            beginSelectExpression(identifier);
        } else {
            assertResource(identifier);
        }
        if ("WITH".equals(operator.image)) {
            buffer.append(getFieldPath(identifier.getField())).append(" IS NOT NULL");
        }
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

    public PgQueryBuilder not() {
        if (buffer.length() != 0) {
            buffer.append("NOT ");
        } else {
            leadingNot = true;
        }
        return this;
    }

    public PgQueryBuilder lparenthesis() {
        buffer.append("(");
        return this;
    }

    public PgQueryBuilder orderBy(Token ident, Token sort, Token sortcase) {
        final Identifier identifier = Identifier.of(ident);
        if (firstOrderBy) {
            buffer.append(" ORDER BY ");
            firstOrderBy = false;
        } else {
            buffer.append(", ");
        }
        if (sortcase != null) {
            buffer.append(sortcase.image.toLowerCase())
                    .append("(").append(getFieldPath(identifier.getField())).append(")")
                    .append(' ').append(sort.image);
        } else {
            buffer.append(getFieldPath(identifier.getField())).append(' ').append(sort.image);
        }
        return this;
    }

    public PgQueryBuilder rparenthesis() {
        buffer.append(")");
        return this;
    }

    public PgQueryBuilder limit(Token limit) {
        buffer.append(limit.image);
        return this;
    }

    public PgQueryBuilder offset(Token offset) {
        buffer.append(offset.image);
        return this;
    }

    public String build() {
        return buffer.toString();
    }

    private void beginSelectExpression(Identifier identifier) {
        resource = identifier.getResource();
        if (countQuery) {
            buffer.append("SELECT COUNT(*) FROM ");
        } else {
            buffer.append("SELECT * FROM ");
        }
        buffer.append(identifier.getResource()).append(" WHERE ");
        if (leadingNot) {
            buffer.append("NOT ");
        }
    }

    private void assertResource(Identifier identifier) throws ParseException {
        if (!resource.equals(identifier.getResource())) {
            throw new ParseException("Multiple resources in query: {"
                    + resource + ", " + identifier.getResource() + "}");
        }
    }

    private String getFieldPath(String field) {
        if (field.contains(".")) {
            return getJsonFieldPath(field);
        }
        return field;
    }

    private String getJsonFieldPath(String field) {
        final StringBuilder buffer = new StringBuilder();
        final String[] keys = field.split("\\.");
        buffer.append(keys[0]);
        if (keys.length > 2) {
            for (int i = 1; i < keys.length - 1; i++) {
                buffer.append("->").append("'").append(keys[i]).append("'");
            }
        }
        buffer.append("->>").append("'").append(keys[keys.length-1]).append("'");
        return buffer.toString();
    }
}
