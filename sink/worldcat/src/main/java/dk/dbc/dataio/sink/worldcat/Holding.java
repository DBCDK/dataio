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

public class Holding implements Comparable<Holding> {
    public enum Action {
        INSERT("I"),
        DELETE("D");

        private final String wciruValue;

        Action(String wciruValue) {
            this.wciruValue = wciruValue;
        }
        public String getWciruValue() {
            return wciruValue;
        }
    }

    private String symbol;
    private Action action;

    public String getSymbol() {
        return symbol;
    }

    public Holding withSymbol(String symbol) {
        this.symbol = symbol;
        return this;
    }

    public Action getAction() {
        return action;
    }

    public Holding withAction(Action action) {
        this.action = action;
        return this;
    }

    @Override
    public String toString() {
        return "Holding{" +
                "symbol='" + symbol + '\'' +
                ", action=" + action +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Holding holding = (Holding) o;

        if (symbol != null ? !symbol.equals(holding.symbol) : holding.symbol != null) {
            return false;
        }
        return action == holding.action;
    }

    @Override
    public int hashCode() {
        int result = symbol != null ? symbol.hashCode() : 0;
        result = 31 * result + (action != null ? action.hashCode() : 0);
        return result;
    }

    @Override
    public int compareTo(Holding other) {
        return symbol.compareTo(other.symbol);
    }
}
