//-----------------------------------------------------------------------------
EXPORTED_SYMBOLS = [ 'Engine' ];

//-----------------------------------------------------------------------------
/**
 *
 *
 * @constructor
 */
var Engine = function() {
    var SPIDERMONKEY = "spidermonkey";
    var RHINO = "rhino";
    var NASHORN = "nashorn";

    function isEngine( name ) {
        switch( name ) {
            case RHINO: {
                return ( typeof Packages === "object" ) && ( typeof Packages.java === "object" );
            }

            case NASHORN: {
                return typeof( __environment ) === "object";
            }

            default: {
                if( name !== SPIDERMONKEY ) {
                    return false;
                }

                return !isEngine( RHINO ) && !isEngine( NASHORN );
            }
        }
    }

    return {
        'SPIDERMONKEY': SPIDERMONKEY,
        'RHINO': RHINO,
        'NASHORN': NASHORN,
        'isEngine': isEngine
    }
}();
