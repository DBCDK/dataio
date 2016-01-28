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
