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
        buffer.append("->>").append("'").append(keys[keys.length - 1]).append("'");
        return buffer.toString();
    }
}
