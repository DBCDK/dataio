/** @file Some added functionality to marc classes */
//------------------------------------------------------------------------------------------------------
/*!
    \file marc.js
*/
//------------------------------------------------------------------------------------------------------

//------------------------------------------------------------------------------------------------------
//                      Exported Symbols
//------------------------------------------------------------------------------------------------------

EXPORTED_SYMBOLS = [ 'EachFactory', 'Handler' ];

// Marc depends on MarcClasses
use( "MarcClasses" );
use( "MarcMatchers" );
use( "UnitTest" );

//------------------------------------------------------------------------------------------------------
//            Subfield prototypes
//------------------------------------------------------------------------------------------------------

// See doc in MarcClasses.use.js
Subfield.prototype.clone = function( ) {
    return new Subfield( this.name, this.value );
};

//------------------------------------------------------------------------------------------------------
//            Field prototypes
//------------------------------------------------------------------------------------------------------

// See doc in MarcClasses.use.js
Field.prototype.clone = function( ) {
    var newField = new Field( this.name, this.indicator );

    for ( var i = 0; i < this.size( ); i++ ) {
        newField.append( this.subfield( i ).clone( ) );
    };

    return newField;
};

// See doc in MarcClasses.use.js
Field.prototype.empty = function( ) {
    return this.size( ) === 0;
};

// See doc in MarcClasses.use.js
Field.prototype.size = function( ) {
    return this.count( );
};

// See doc in MarcClasses.use.js
Field.prototype.eachSubField = function( matcher, handler ) {
    return new EachFactory( ).eachSubField( this, getMatchSubField( matcher ), handler );
};

// See doc in MarcClasses.use.js
Field.prototype.firstSubField = function( matcher, handler ) {
    return new EachFactory( ).firstSubField( this, getMatchSubField( matcher ), handler );
};

// See doc in MarcClasses.use.js
Field.prototype.eachSubFieldSequence = function( matchers, handler ) {
    return new EachFactory( ).eachFieldSequence( this, matchers, handler );
};

// See doc in MarcClasses.use.js
Field.prototype.exists = function( matcher ) {
    var result = false;

    this.eachSubField( getMatchSubField( matcher ), function( field, subField ) {
            result = true;
        } );

    return result;
};

// See doc in MarcClasses.use.js
Field.prototype.isUnique = function( matcher ) {
    return this.eachSubField( getMatchSubField( matcher ), function( field, subField ) {
            return 1;
        } ) == 1;
};

// See doc in MarcClasses.use.js
Field.prototype.getFirstValue = function( matcher ) {

    var msf = matcher;
    var result = this.firstSubField( msf, function( field, subField ) {
            if ( "" !== subField.value || subField.value !== undefined ) {
                return subField.value;
            }
        } );
    if ( result != "" ) {
        return result;
    } else {
        return "";
    };
};

// See doc in MarcClasses.use.js
Field.prototype.getValueAsArray = function( matcher ) {

    var matchers;

    if ( matcher instanceof Array ) {
        matchers = matcher;
    } else {
        matchers = [ matcher ];
    };

    for ( var i = 0; i < matchers.length; i++ ) {
        var msf = getMatchSubField( matchers[ i ] );
        var result = [ ];

        this.eachSubField( msf, function( field, subField ) {
                if ( subField.value !== "" && subField.value !== undefined ) {
                    result.push( subField.value );
                }
            } );
        if ( result.length > 0 ) {
            return result;
        };
    };

    return [ ];
};

// See doc in MarcClasses.use.js
Field.prototype.getValue = function( matcher, sep ) {

    var matchers;

    if ( matcher instanceof Array ) {
        matchers = matcher;
    } else {
        matchers = [ matcher ];
    };

    for ( var i = 0; i < matchers.length; i++ ) {
        var msf = getMatchSubField( matchers[ i ] );
        var result = undefined;

        this.eachSubField( msf, function( field, subField ) {
                if ( result === undefined ) {
                    result = subField.value;
                } else {
                    if ( sep !== undefined && result !== "" ) {
                        result += sep;
                    };

                    result += subField.value;
                };
            } );

        if ( result !== undefined ) {
            return result;
        };
    };

    return "";
};

// See doc in MarcClasses.use.js
Field.prototype.matchValue = function( matcher, valueRegExp ) {
    return valueRegExp.test( this.getValue( matcher ) );
};

// See doc in MarcClasses.use.js
Field.prototype.removeWithMatcher = function( matcher ) {
    var matchers;

    if ( matcher instanceof Array ) {
        matchers = matcher;
    } else {
        matchers = [ matcher ];
    };

    for ( var i = 0; i < matchers.length; i++ ) {
        var msf = getMatchSubField( matchers[ i ] );

        var j = 0;
        while ( j < this.size( ) ) {
            if ( msf.matchSubField( this, this.subfield( j ) ) ) {
                this.remove( j );
            } else {
                j++;
            }
        }
    };

    return "";
};

//------------------------------------------------------------------------------------------------------
//            Record prototypes
//------------------------------------------------------------------------------------------------------

// See doc in MarcClasses.use.js
Record.prototype.getFirstFieldAsField = function( regExFieldMatcher ) {
    //if its not a regex obj we should probally throw an error in the future mvs
    //if (!regExFieldMatcher instanceof RegExp  ) {
    //  return ""; 
    //}

    //"this" refers to the record obj
    var size = this.numberOfFields( );

    for ( var i = 0; i < size; i++ ) {
        if ( this.field( i ).name.match( regExFieldMatcher ) ) {
            return this.field( i );
        }
    };
    //we return an empty string if no valid fields are in the records
    return "";
};

// See doc in MarcClasses.use.js
Record.prototype.clone = function( ) {
    var newRecord = new Record;

    newRecord.recordStatus = this.recordStatus;
    newRecord.implementationCodes = this.implementationCodes;

    for ( var i = 0; i < this.size( ); i++ ) {
        newRecord.append( this.field( i ).clone( ) );
    };

    return newRecord;
};

// See doc in MarcClasses.use.js
Record.prototype.empty = function( ) {
    return this.size( ) === 0;
};

// See doc in MarcClasses.use.js
Record.prototype.eachField = function( matcher, arg2, arg3 ) {
    if ( arg3 != undefined ) {
        return new EachFactory( this, arg2 ).eachField( getMatchField( matcher ), arg3 );
    };
    return new EachFactory( this ).eachField( getMatchField( matcher ), arg2 );
};

// See doc in MarcClasses.use.js
Record.prototype.firstField = function( matcher, arg2, arg3 ) {
    if ( arg3 != undefined ) {
        return new EachFactory( this, arg2 ).firstField( getMatchField( matcher ), arg3 );
    };

    return new EachFactory( this ).firstField( getMatchField( matcher ), arg2 );
};

// See doc in MarcClasses.use.js
Record.prototype.eachFieldSequence = function( matchers, arg2, arg3 ) {
    if ( arg3 != undefined ) {
        return new EachFactory( this, arg2 ).eachFieldSequence( matchers, arg3 );
    };

    return new EachFactory( this ).eachFieldSequence( matchers, arg2 );
};

// See doc in MarcClasses.use.js
Record.prototype.selectFields = function( matcher, sorters ) {
    return new EachFactory( this, sorters ).selectFields( getMatchField( matcher ) );
};

// See doc in MarcClasses.use.js
Record.prototype.selectFieldsSequence = function( matcher, sorters ) {
    return new EachFactory( this, sorters ).selectFieldsSequence( matcher );
};

// See doc in MarcClasses.use.js
Record.prototype.size = function( ) {
    return this.numberOfFields( );
};

// See doc in MarcClasses.use.js
Record.prototype.existField = function( matcher ) {
    var result = false;

    this.eachField( getMatchField( matcher ), function( field ) {
            result = true;
        } );

    return result;
};

// See doc in MarcClasses.use.js
Record.prototype.isUnique = function( matcher ) {
    return this.eachField( getMatchField( matcher ), function( field ) {
            return 1;
        } ) == 1;
};

// See doc in MarcClasses.use.js
Record.prototype.getFirstValue = function( fieldMatcher, subFieldMatcher ) {

    var result = this.firstField( getMatchField( fieldMatcher ), function( field ) {
            if ( "" != field.getFirstValue( subFieldMatcher ) ) {
                return field.getFirstValue( subFieldMatcher );
            }
        } );
    if ( "" != result && undefined !== result ) {
        return result;
    } else {
        return "";
    };
};

// See doc in MarcClasses.use.js
Record.prototype.getValue = function( fieldMatcher, subFieldMatcher, sep ) {
    var result = "";

    this.eachField( getMatchField( fieldMatcher ), function( field ) {
            if ( sep !== undefined && result !== "" ) {
                result += sep;
            };

            result += field.getValue( subFieldMatcher, sep );
        } );

    return result;
};

// See doc in MarcClasses.use.js
Record.prototype.matchValue = function( fieldMatcher, subFieldMatcher, valueRegExp ) {
    return valueRegExp.test( this.getValue( fieldMatcher, subFieldMatcher ) );
};

// See doc in MarcClasses.use.js
Record.prototype.removeWithMatcher = function( matcher ) {
    var matchers;

    if ( matcher instanceof Array ) {
        matchers = matcher;
    } else {
        matchers = [ matcher ];
    };

    for ( var i = 0; i < matchers.length; i++ ) {
        var msf = getMatchField( matchers[ i ] );

        var j = 0;
        while ( j < this.size( ) ) {
            if ( msf.matchField( this, this.field( j ) ) ) {
                this.remove( j );
            } else {
                j++;
            }
        }
    };
};

//------------------------------------------------------------------------------------------------------
//                            EachFactory class
//------------------------------------------------------------------------------------------------------

function EachFactory( record, sorter ) {
    this.record = record;
    this.sorter = sorter;
};

EachFactory.prototype.eachField = function( matcher, handler ) {
    var result = "";
    var fields = this.selectFields( matcher );

    if ( this.sorter != undefined ) {
        var sorter = new Sorter( this.sorter );

        fields.sort( function( a, b ) {
                return sorter.sort( a, b );
            } );
    };

    var first = true;
    for ( var i = 0; i < fields.length; i++ ) {
        if ( first ) {
            result = this.callFieldHandler( handler, fields[ i ] );
            first = false;
        } else {
            result += this.callFieldHandler( handler, fields[ i ] );
        };
    };

    return result;
};

EachFactory.prototype.firstField = function( matcher, handler ) {

    var result = "";
    var fields = this.selectFields( matcher );

    if ( this.sorter != undefined ) {
        var sorter = new Sorter( this.sorter );

        fields.sort( function( a, b ) {
                return sorter.sort( a, b );
            } );
    };

    for ( var i = 0; i < fields.length; i++ ) {
        result = this.callFieldHandler( handler, fields[ i ] );
        if ( "" != result && undefined !== result ) {
            return result;
        }
    };
    return result;
};

EachFactory.prototype.eachFieldSequence = function( matchers, handler ) {
    var result = "";
    var max = 0;
    var seqs = new Array;

    for ( var i = 0; i < matchers.length; i++ ) {
        var seq = this.selectFields( matchers[ i ] );

        if ( this.sorter != undefined ) {
            var sorter = new Sorter( this.sorter );

            seq.sort( function( a, b ) {
                    return sorter.sort( a, b );
                } );
        };

        seqs.push( seq );

        if ( max < seq.length ) {
            max = seq.length;
        };
    };

    var first = true;
    for ( var i = 0; i < max; i++ ) {
        for ( var seq = 0; seq < seqs.length; seq++ ) {
            if ( i < seqs[ seq ].length ) {
                if ( first ) {
                    result = this.callFieldHandler( handler, seqs[ seq ][ i ] );
                    first = false;
                } else {
                    result += this.callFieldHandler( handler, seqs[ seq ][ i ] );
                };
            };
        };
    };

    return result;
};

EachFactory.prototype.firstSubField = function( field, matcher, handler ) {

    var result = "";

    for ( var i = 0; i < field.count( ); i++ ) {
        if ( matcher == undefined || matcher.matchSubField( field, field.subfield( i ) ) ) {
            result = this.callSubFieldHandler( handler, field, field.subfield( i ) );
            if ( "" != result ) {
                return result;
            }
        };
    };

    if ( result === undefined ) {} else {

    }
    return result;
};
EachFactory.prototype.eachSubField = function( field, matcher, handler ) {

    var result = "";


    var first = true;
    for ( var i = 0; i < field.count( ); i++ ) {
        if ( matcher == undefined || matcher.matchSubField( field, field.subfield( i ) ) ) {
            if ( first ) {
                result = this.callSubFieldHandler( handler, field, field.subfield( i ) );
                first = false;
            } else {
                result += this.callSubFieldHandler( handler, field, field.subfield( i ) );
            };
        };
    };

    return result;
};

EachFactory.prototype.eachSubFieldSequence = function( field, matchers, handler ) {
    var result = "";

    var first = true;
    for ( var i = 0; i < matchers.length; i++ ) {
        if ( first ) {
            result = this.eachSubField( field, matchers[ i ], handler );
            first = false;
        } else {
            result += this.eachSubField( field, matchers[ i ], handler );
        };
    };

    return result;
};



EachFactory.prototype.selectFields = function( matcher ) {
    var list = new Array;

    if ( matcher === undefined ) {
        matcher.matchCounter = new Object;
    };

    for ( var i = 0; i < this.record.size( ); i++ ) {
        if ( matcher === undefined || matcher.matchField( this.record, this.record.field( i ) ) ) {
            list.push( this.record.field( i ) );
        };
    };

    return list;
};

EachFactory.prototype.selectFieldsSequence = function( matchers ) {
    var list = new Array;

    for ( var i = 0; i < matchers.length; i++ ) {
        list = list.concat( this.selectFields( matchers[ i ] ) );
    };

    return list;
};

EachFactory.prototype.callFieldHandler = function( handler, record, field ) {
    if ( handler instanceof Handler ) {
        return handler.func.call( handler.thisObj, record, field );
    };

    return handler( record, field );
};

EachFactory.prototype.callSubFieldHandler = function( handler, field, subField ) {
    if ( handler instanceof Handler ) {
        return handler.func.call( handler.thisObj, field, subField );
    };

    return handler( field, subField );
};

//------------------------------------------------------------------------------------------------------
//                            Handler class
//------------------------------------------------------------------------------------------------------

/** Defines a handler object then we iterate over fields or sub fields with Record.eachField, 
 * Field.eachSubFields, etc.
 *
 * @param {Object} [thisObj] This is the object that will be passed as the "this" object, then the function 
 * 		  is invoked. This logic is implementated in the EachFactory type.
 * @param {Function} [func] The function that will be invoked for each Field or Subfield in a record.
 * @constructor
 * @see MarcClasses
 * @see Record
 * @see Field 
 * @name Handler */
function Handler( thisObj, func ) {
    this.thisObj = thisObj;
    this.func = func;
};

UnitTest.addFixture( "marc.Marc module", function( ) {

        __ut = {};

        ////////////////////////////////////////////////////////////

        var createIsoPost = function( ) {

            //this might not fly, should probally be somehting along the lines of marcClassesCore.Record();
            // nope, it works, but why ? shouldnt Record be part of the MarcClassesCore "namespace"" =?? ? ?

            // Work with a "real record"

            var rec = new Record( );
            rec.implementationCodes = 'sse ';
            rec.recordStatus = 'n';
            var f;
            f = new Field( '001', '00' );
            f.append( 'a', '2 548 263 8' );
            f.append( 'b', '870970' );
            f.append( 'c', '20041117025343' );
            f.append( 'd', '20041101' );
            f.append( 'f', 'a' );
            f.append( 't', 'FAUST' );
            rec.append( f );
            f = new Field( '004', '00' );
            f.append( 'r', 'n' );
            f.append( 'a', 'e' );
            rec.append( f );
            f = new Field( '005', '00' );
            f.append( 'h', 'e' );
            rec.append( f );
            f = new Field( '008', '00' );
            f.append( 't', 's' );
            f.append( 'u', 'f' );
            f.append( 'a', '2004' );
            f.append( 'b', 'gb' );
            f.append( 'l', 'eng' );
            f.append( 'v', '0' );
            rec.append( f );
            f = new Field( '009', '00' );
            f.append( 'a', 's' );
            f.append( 'g', 'xc' );
            rec.append( f );
            f = new Field( '010', '00' );
            f.append( 'a', 'D947837018' );
            rec.append( f );
            f = new Field( '021', '00' );
            f.append( 'd', 'kr. 159,00' );
            rec.append( f );
            f = new Field( '023', '00' );
            f.append( 'a', '0028947564089' );
            rec.append( f );
            f = new Field( '032', '00' );
            f.append( 'x', 'SFG200447' );
            f.append( 'x', 'SFGO200447' );
            rec.append( f );
            f = new Field( '039', '00' );
            f.append( 'a', 'bef' );
            rec.append( f );
            f = new Field( '100', '00' );
            f.append( '0', '' );
            f.append( 'a', 'Richard' );
            f.append( 'h', 'Cliff' );
            rec.append( f );
            f = new Field( '245', '00' );
            f.append( 'a', 'Something\'s goin\' on' );
            f.append( 'e', 'Cliff Richard with Steve Mandile, Tommy Sims, JT Corenflos, Michael Spriggs, Greg Morrow, Nashville String Machine, Tony Harrell, Barry Gibb ... [et al.]' );
            rec.append( f );
            f = new Field( '260', '00' );
            f.append( 'a', 'London' );
            f.append( 'b', 'Decca Music Group' );
            f.append( 'c', 'p 2004' );
            rec.append( f );
            f = new Field( '300', '00' );
            f.append( 'n', '1 cd' );
            rec.append( f );
            f = new Field( '512', '00' );
            f.append( 'a', 'Tekster p\xE5 omslag' );
            rec.append( f );
            f = new Field( '531', '00' );
            f.append( 'a', 'Indhold:' );
            rec.append( f );
            f = new Field( '538', '00' );
            f.append( 'f', 'Decca' );
            f.append( 'g', '4756408' );
            rec.append( f );
            f = new Field( '652', '00' );
            f.append( 'm', '78.794' );
            f.append( 'v', '5' );
            rec.append( f );
            f = new Field( '666', '00' );
            f.append( 'm', 'pop' );
            f.append( 'm', 'rock' );
            f.append( 'n', 'vokal' );
            f.append( 'p', '2000-2009' );
            f.append( 'l', 'England' );
            rec.append( f );
            f = new Field( '700', '00' );
            f.append( '0', '' );
            f.append( 'a', 'Corenflos' );
            f.append( 'h', 'J. T.' );
            rec.append( f );
            f = new Field( '700', '00' );
            f.append( '0', '' );
            f.append( 'a', 'Gibb' );
            f.append( 'h', 'Barry' );
            rec.append( f );
            f = new Field( '700', '00' );
            f.append( '0', '' );
            f.append( 'a', 'Harrell' );
            f.append( 'h', 'Tony' );
            rec.append( f );
            f = new Field( '700', '00' );
            f.append( '0', '' );
            f.append( 'a', 'Lewis' );
            f.append( 'h', 'Ken' );
            rec.append( f );
            f = new Field( '700', '00' );
            f.append( '0', '' );
            f.append( 'a', 'Mandile' );
            f.append( 'h', 'Steve' );
            rec.append( f );
            f = new Field( '700', '00' );
            f.append( '0', '' );
            f.append( 'a', 'Morrow' );
            f.append( 'h', 'Greg' );
            rec.append( f );
            f = new Field( '700', '00' );
            f.append( '0', '' );
            f.append( 'a', 'Sims' );
            f.append( 'h', 'Tommy' );
            rec.append( f );
            f = new Field( '700', '00' );
            f.append( '0', '' );
            f.append( 'a', 'Spriggs' );
            f.append( 'h', 'Michael' );
            rec.append( f );
            f = new Field( '710', '00' );
            f.append( '0', '' );
            f.append( 'a', 'Nashville String Machine' );
            rec.append( f );
            f = new Field( '795', '00' );
            f.append( '\xE5', '11' );
            f.append( '0', '' );
            f.append( 'a', 'Thousand miles to go' );
            f.append( 'a', 'Somethin\' is goin\' on' );
            f.append( 'a', 'I will not be a mistake' );
            f.append( 'a', 'Simplicity' );
            f.append( 'a', 'Sometimes love' );
            f.append( 'a', 'I cannot give you my love' );
            f.append( 'a', 'The \xA4day that I stop loving you' );
            f.append( 'a', 'What car' );
            f.append( 'a', 'How did she get here' );
            f.append( 'a', 'Field of love' );
            f.append( 'a', 'For life' );
            f.append( 'a', 'I don\'t wanna lose you' );
            f.append( 'a', 'Faithful one' );
            rec.append( f );
            f = new Field( '795', '00' );
            f.append( '\xE5', '99' );
            f.append( '0', '' );
            f.append( 'y', '0' );
            f.append( 'a', '1000 miles to go' );
            rec.append( f );

            return rec;
        };



        __ut.isopost = createIsoPost( );

        Assert.equal( "RI: toString test", "__ut.isopost.toString()",
            "001 00 *a 2 548 263 8 *b 870970 *c 20041117025343 *d 20041101 *f a *t FAUST \n" +
            "004 00 *r n *a e \n" +
            "005 00 *h e \n" +
            "008 00 *t s *u f *a 2004 *b gb *l eng *v 0 \n" +
            "009 00 *a s *g xc \n" +
            "010 00 *a D947837018 \n" +
            "021 00 *d kr. 159,00 \n" +
            "023 00 *a 0028947564089 \n" +
            "032 00 *x SFG200447 *x SFGO200447 \n" +
            "039 00 *a bef \n" +
            "100 00 *0 *a Richard *h Cliff \n" +
            "245 00 *a Something's goin' on *e Cliff Richard with Steve Mandile, Tommy Sims, JT Corenflos, Michael Spriggs, Greg Morrow, Nashville String Machine, Tony Harrell, Barry Gibb ... [et al.] \n" +
            "260 00 *a London *b Decca Music Group *c p 2004 \n" +
            "300 00 *n 1 cd \n" +
            "512 00 *a Tekster p\xE5 omslag \n" +
            "531 00 *a Indhold: \n" +
            "538 00 *f Decca *g 4756408 \n" +
            "652 00 *m 78.794 *v 5 \n" +
            "666 00 *m pop *m rock *n vokal *p 2000-2009 *l England \n" +
            "700 00 *0 *a Corenflos *h J. T. \n" +
            "700 00 *0 *a Gibb *h Barry \n" +
            "700 00 *0 *a Harrell *h Tony \n" +
            "700 00 *0 *a Lewis *h Ken \n" +
            "700 00 *0 *a Mandile *h Steve \n" +
            "700 00 *0 *a Morrow *h Greg \n" +
            "700 00 *0 *a Sims *h Tommy \n" +
            "700 00 *0 *a Spriggs *h Michael \n" +
            "710 00 *0 *a Nashville String Machine \n" +
            "795 00 *\xE5 11 *0 *a Thousand miles to go *a Somethin' is goin' on *a I will not be a mistake *a Simplicity *a Sometimes love *a I cannot give you my love *a The \xA4day that I stop loving you *a What car *a How did she get here *a Field of love *a For life *a I don't wanna lose you *a Faithful one \n" +
            "795 00 *\xE5 99 *0 *y 0 *a 1000 miles to go \n" );


        ////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////
        // 
        // Testing record functions.
        // 
        ////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////

        __ut.isopostClone = __ut.isopost.clone( )
        Assert.equal( "RI: equality test after clone", "__ut.isopostClone", __ut.isopost );

        Assert.equal( "testing record.empty with a valid record",
            '__ut.isopost.empty()', false );

        __ut.isopostEmpty = new Record( );
        Assert.equal( "testing record.empty with an empty record",
            '__ut.isopostEmpty.empty()', true );

        Assert.equal( "RI: size test on our test post ", "__ut.isopost.size()", 30 );

        Assert.equal( "RI: size test on the empty record", "__ut.isopostEmpty.size()", 0 );

        // mbd 2012-11-20 - This loops for ever in dbc-jsshell when loaded as part of our unittests - good test, bad code?
        // __ut.isopostClone.removeWithMatcher(/700/);
        //Assert.equal ( "RI: size test on the clone with field 700 removed", "__ut.isopostClone.size()",30) ;

        Assert.equal( "RI: test of remove with matcher remove all fields", "__ut.isopostClone.removeWithMatcher(/./)", undefined );

        Assert.equal( "RI: size test after deleting every field on the clone", "__ut.isopostClone.size()", 0 );


        ////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////
        // 
        // Testing field functions.
        // 
        ////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////

        //getFirstValue tests

        Assert.equal( "testing getFirstValue with one valid field and one valid value",
            '__ut.isopost.getFirstValue (/245/, /a/ )', "Something's goin' on" );

        Assert.equal( "testing getFirstValue with multiple valid fields and multiple valid values",
            '__ut.isopost.getFirstValue (/700/, /a/ )', "Corenflos" );

        Assert.equal( "testing getFirstValue with non valid field ",
            '__ut.isopost.getFirstValue (/003/, /a/ )', "" );


        //eachField tests 
        Assert.equal( "testing eachField with non valid field ",
            '__ut.isopost.eachField(/003/, /a/ )', "" );

        Assert.equal( "testing eachField with a valid field ",
            ' (__ut.isopost.eachField(/039/, /a/)) instanceof Array ', true );


        //firstField tests
        Assert.equal( "testing firstField with a valid field ",
            ' (__ut.isopost.getFirstFieldAsField(/039/)) instanceof Object ', true );

        Assert.equal( "testing firstField with no valid fields ",
            ' (__ut.isopost.getFirstFieldAsField(/600/)).toString()', "" );

        Assert.equal( "testing firstField with multiple valid fields ",
            ' (__ut.isopost.getFirstFieldAsField(/700/)).toString()', "700 00 *0 *a Corenflos *h J. T. " );



        //i need someone to continue here, that someone being Sune.

        delete __ut;
    } );
