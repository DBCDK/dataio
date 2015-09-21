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

/** @file Provide a Global symbol */
EXPORTED_SYMBOLS = [ 'Global' ];

// Important note: This module can not use the Log module, as the Log
// module uses this module. And, honestely, what would you log?
// Right. Just don't do it. Now, move along, nothing to see here.

/**
 * Provide consistent access to the Global variable.
 *
 * When used, this module will introduce a global variable called
 * `Global`, that is a reference to the toplevel Global object.
 * 
 * This module does not provide any methods or member, besides the
 * Global variable.
 *
 * @type {object}
 * @example
// This loads the Global module, and creates a variable called Global
use( "Global" );
* @namespace
 * @name Global */
var Global = function( ) {
    /** Get access to the Global variable.
     * 
     * This function returns a reference to the toplevel Global
     * variable. In non-browser environments, this is equal to the
     * this variable at toplevel scope, but requires a bit more to get
     * to, if not a toplevel scope.
     * 
     * @type{function}
     * @private
     * @return {object} A reference to the global object */
    var getGlobal = function( ) {
        return ( function( ) {
                return this;
            } )( );
    };
    // return result of call to getGlobal, i.e. ref to global
    return getGlobal( );
}( );
