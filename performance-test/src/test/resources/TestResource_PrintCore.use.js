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
