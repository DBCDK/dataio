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

// PrintCore modules - implements a Print function

/* Java deps: nothing, this is very basic, prints to System.out */

// We export this symbol, which we expect to be used from a Print module.
EXPORTED_SYMBOLS = [ '__PrintCore_print' ];

// Actual log function
var __PrintCore_print = function( ) {
    var res = "";
    for ( var i = 0; i < arguments.length; i++ ) {
        if ( null === arguments[ i ] ) {
            res += "null";
        } else {
            if ( undefined === arguments[ i ] ) {
                res += "undefined";
            } else {
                res += arguments[ i ].toString( );
            }
        }
    }
    Packages.java.lang.System.out.print( res );
};
