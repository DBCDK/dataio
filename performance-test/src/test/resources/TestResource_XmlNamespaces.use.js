/** @file XML namespace wrapper */
EXPORTED_SYMBOLS = [ 'XmlNamespaces' ];

use( "Log" );
use( "UnitTest" );
use( "XmlUtil" );

/**
 * Module that holds a number of namespace declarations..
 * 
 * This namespace holds a number of namespace declarations that are used
 * as constants in parts of the code. 
 * 
 * @type {namespace}
 * @namespace 
 * @name XmlNamespaces */
var XmlNamespaces = function( ) {

    var that = {};
    /** ubf namespace.
     *
     * The prefix for this namespace is `ubf`. The uri for this namespace is `http://www.dbc.dk/ubf`
     *
     * @type {Namespace}
     * @name XmlNamespaces.ubf */
    that.ubf = new Namespace( "ubf", "http://www.dbc.dk/ubf" );
    /** ors namespace.
     *
     * The prefix for this namespace is `ors`. The uri for this namespace is `http://oss.dbc.dk/ns/openresourcesharing`.
     *
     * @type {Namespace}
     * @name XmlNamespaces.ors */
    that.ors = new Namespace( "ors", "http://oss.dbc.dk/ns/openresourcesharing" );
    /** holdings namespace.
     *
     * The prefix for this namespace is `hs`. The uri for this namespace is `http://oss.dbc.dk/ns/holdings`.
     * @type {Namespace}
     * @name XmlNamespaces.holdings */
    that.holdings = new Namespace( "hs", "http://oss.dbc.dk/ns/holdings" );
    /** ill5 namespace.
     *
     * The prefix for this namespace is `ill5`. The uri for this namespace is `
     * @type {Namespace}
     * @name XmlNamespaces.ill5 */
    that.ill5 = new Namespace( "ill5", "http://www.loc.gov/z3950/agency/defns/ill5" );
    /** zigholdings namespace.
     *
     * The prefix for this namespace is `n`. The uri for this namespace is `http://www.loc.gov/z3950/agency/defns/HoldingsSchema8`.
     * @type {Namespace}
     * @name XmlNamespaces.zigholdings */
    that.zigholdings = new Namespace( "n", "http://www.loc.gov/z3950/agency/defns/HoldingsSchema8" );
    /** openagency namespace.
     *
     * The prefix for this namespace is `oa`. The uri for this namespace is `http://oss.dbc.dk/ns/openagency`.
     * @type {Namespace}
     * @name XmlNamespaces.openagency */
    that.openagency = new Namespace( "oa", "http://oss.dbc.dk/ns/openagency" );
    /** ting namespace.
     *
     * The prefix for this namespace is `ting`. The uri for this namespace is `http://www.dbc.dk/ting`.
     * @type {Namespace}
     * @name XmlNamespaces.ting */
    that.ting = new Namespace( "ting", "http://www.dbc.dk/ting" );
    /** es namespace.
     *
     * The prefix for this namespace is `es`. The uri for this namespace is `http://oss.dbc.dk/ns/es`.
     * @type {Namespace}
     * @name XmlNamespaces.es */
    that.es = new Namespace( "es", "http://oss.dbc.dk/ns/es" );
    /** dkabm namespace.
     *
     * The prefix for this namespace is `dkabm`. The uri for this namespace is `http://biblstandard.dk/abm/namespace/dkabm/`.
     * @type {Namespace}
     * @name XmlNamespaces.dkabm */
    that.dkabm = new Namespace( "dkabm", "http://biblstandard.dk/abm/namespace/dkabm/" );
    /** ac namespace.
     *
     * The prefix for this namespace is `ac`. The uri for this namespace is `http://biblstandard.dk/ac/namespace/`.
     * @type {Namespace}
     * @name XmlNamespaces.ac */
    that.ac = new Namespace( "ac", "http://biblstandard.dk/ac/namespace/" );
    /** dkdcplus namespace.
     *
     * The prefix for this namespace is `dkdcplus`. The uri for this namespace is `http://biblstandard.dk/abm/namespace/dkdcplus/`.
     * @type {Namespace}
     * @name XmlNamespaces.dkdcplus */
    that.dkdcplus = new Namespace( "dkdcplus", "http://biblstandard.dk/abm/namespace/dkdcplus/" );
    /** oss namespace.
     *
     * The prefix for this namespace is `oss`. The uri for this namespace is `http://oss.dbc.dk/ns/osstypes`.
     * @type {Namespace}
     * @name XmlNamespaces.oss */
    that.oss = new Namespace( "oss", "http://oss.dbc.dk/ns/osstypes" );
    /** dc namespace.
     *
     * The prefix for this namespace is `dc`. The uri for this namespace is `http://purl.org/dc/elements/1.1/`.
     * @type {Namespace}
     * @name XmlNamespaces.dc */
    that.dc = new Namespace( "dc", "http://purl.org/dc/elements/1.1/" );
    /** oai_dc namespace.
     *
     * The prefix for this namespace is `oai_dc`. The uri for this namespace is `http://www.openarchives.org/OAI/2.0/oai_dc/`.
     * @type {Namespace}
     * @name XmlNamespaces.oai_dc */
    that.oai_dc = new Namespace( "oai_dc", "http://www.openarchives.org/OAI/2.0/oai_dc/" );
    /** dcterms namespace.
     *
     * The prefix for this namespace is `dcterms`. The uri for this namespace is `http://purl.org/dc/terms/`.
     * @type {Namespace}
     * @name XmlNamespaces.dcterms */
    that.dcterms = new Namespace( "dcterms", "http://purl.org/dc/terms/" );
    /** xsi namespace.
     *
     * The prefix for this namespace is `xsi`. The uri for this namespace is `http://www.w3.org/2001/XMLSchema-instance`.
     * @type {Namespace}
     * @name XmlNamespaces.xsi */
    that.xsi = new Namespace( "xsi", "http://www.w3.org/2001/XMLSchema-instance" );
    /** marcx namespace.
     *
     * The prefix for this namespace is `marcx`. The uri for this namespace is `info:lc/xmlns/marcxchange-v1`.
     * @type {Namespace}
     * @name XmlNamespaces.marcx */
    that.marcx = new Namespace( "marcx", "info:lc/xmlns/marcxchange-v1" );
    /** mx namespace.
     *
     * The prefix for this namespace is `mx`. The uri for this namespace is `http://www.loc.gov/MARC21/slim`.
     * @type {Namespace}
     * @name XmlNamespaces.mx */
    that.mx = new Namespace( "mx", "http://www.loc.gov/MARC21/slim" );
    /** docbook namespace.
     *
     * The prefix for this namespace is `docbook`. The uri for this namespace is `http://docbook.org/ns/docbook`.
     * @type {Namespace}
     * @name XmlNamespaces.docbook */
    that.docbook = new Namespace( "docbook", "http://docbook.org/ns/docbook" );
    /** oso namespace.
     *
 The prefix for this namespace is `oso`.     * The uri for this namespace is `http://oss.dbc.dk/ns/opensearchobjects`.
     * @type {Namespace}
     * @name XmlNamespaces.oso */
    that.oso = new Namespace( "oso", "http://oss.dbc.dk/ns/opensearchobjects" );
    /** xsd namespace.
     *
     * The prefix for this namespace is `xsd`. The uri for this namespace is `http://www.w3.org/2001/XMLSchema#`.
     * @type {Namespace}
     * @name XmlNamespaces.xsd */
    that.xsd = new Namespace( "xsd", "http://www.w3.org/2001/XMLSchema#" );
    /** xml namespace.
     *
     * The prefix for this namespace is `xml`. The uri for this namespace is `http://www.w3.org/XML/1998/namespace`.
     * @type {Namespace}
     * @name XmlNamespaces.xml */
    that.xml = new Namespace( "xml", "http://www.w3.org/XML/1998/namespace" );
    /** rdf namespace.
     *
     * The prefix for this namespace is `rdf`. The uri for this namespace is `http://www.w3.org/1999/02/22-rdf-syntax-ns#`.
     * @type {Namespace}
     * @name XmlNamespaces.rdf */
    that.rdf = new Namespace( "rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#" );
    /** skos namespace.
     *
     * The prefix for this namespace is `skos`. The uri for this namespace is `http://www.w3.org/2008/05/skos#`.
     * @type {Namespace}
     * @name XmlNamespaces.skos */
    that.skos = new Namespace( "skos", "http://www.w3.org/2008/05/skos#" );
    /** foaf namespace.
     *
     * The prefix for this namespace is `foaf`. The uri for this namespace is `http://xmlns.com/foaf/0.1`.
     * @type {Namespace}
     * @name XmlNamespaces.foaf */
    that.foaf = new Namespace( "foaf", "http://xmlns.com/foaf/0.1" );
    /** of namespace.
     * The prefix for this namespace is `of`.     
     * The uri for this namespace is `http://oss.dbc.dk/ns/openformat`.
     * @type {Namespace}
     * @name XmlNamespaces.of */
    that.of = new Namespace( "of", "http://oss.dbc.dk/ns/openformat" );
    /** ofo namespace.
     *
     * The prefix for this namespace is `ofo`. The uri for this namespace is `http://oss.dbc.dk/ns/openformatoutput`.
     * @type {Namespace}
     * @name XmlNamespaces.ofo */
    that.ofo = new Namespace( "ofo", "http://oss.dbc.dk/ns/openformatoutput" );
    /** bibdk namespace.
     *
     * The prefix for this namespace is `bibdk`. The uri for this namespace is `http://oss.dbc.dk/ns/openformatoutput/bibliotekdkdisplay`.
     * @type {Namespace}
     * @name XmlNamespaces.bibdk */
    that.bibdk = new Namespace( "bibdk", "http://oss.dbc.dk/ns/openformatoutput/bibliotekdkdisplay" );
    /** os namespace.
     *
     * The prefix for this namespace is `os`. The uri for this namespace is `http://oss.dbc.dk/ns/opensearch`.
     * @type {Namespace}
     * @name XmlNamespaces.os */
    that.os = new Namespace( "os", "http://oss.dbc.dk/ns/opensearch" );

    const const_that = that;
    return const_that;
}( );


UnitTest.addFixture( "util.XmlNamespaces module", function( ) {
        // let's at least just do something...
        var nstest = function( ) {
            var hs = new Namespace( "hs", 'http://oss.dbc.dk/ns/holdings' );
            var holdingsRequest1 = XmlUtil.fromString( "<holdingsRequest/>" );
            var holdingsRequest2 = XmlUtil.fromString( "<holdingsRequest/>" );
            holdingsRequest1.setNamespace( hs );
            holdingsRequest2.setNamespace( XmlNamespaces.holdings );
            Assert.that( 'noget', holdingsRequest1 == holdingsRequest2 );
        };
        nstest( );

    } );
