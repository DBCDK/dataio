/** @file Module to do unit tests and report them. */

use( "Util" );

EXPORTED_SYMBOLS = [ 'Assert', 'UnitTest' ];

////////////////////////////////////////////////////////////////////////////////
// Global Assert object. As this module can only be use'd once pr. program, this is safe:
/**
 * Assert object that provides unit test methods.
 * 
 * An instance of this object reflects a test fixture/group, and you can
 * create your individual testcases by using the functions on this object. 
 *
 * @see UnitTest
 * @name Assert
 * @namespace 
 */
var Assert = undefined;

////////////////////////////////////////////////////////////////////////////////
// The main namespace to control test fixtures.
/**
 * Controls the overall UnitTest system.
 * 
 * This object is used to control the use of the UnitTest system, that is
 * adding and managing test fixtures. Actual tests are done by the Assert
 * object. The main method to use is the addFixture method. (See the example).
 *
 * When using the UnitTest module like in the example, you may run the unittests for a module by "loading" the file, usually by specifying it to the environment as the file you wish to execute. The tests will be run, and a report emitted using the Print module. When the module is use'd the unit tests will not be performed, nor reported.
 *
 * If the Log module is loaded, some high level logging will take place, when the UnitTest are run. This can help debugging tests that fails, or during implementatio.
 * 
 * @example
// This is an example of the preffered way to use the UnitTest module:
use( "UnitTest" ); 
// Add a fixture for the module.
UnitTest.addFixture( "Test Module modulename", function() { 
    // Add all the tests
    Assert.exception( "readFile on non existant file", 'readFile("nothere")' ); 
    Assert.that( "comment", 'expression evaluating to true' );
    Assert.not( "This should always be false", 'expression evaluating to false' );
    Assert.equal( "test0, output", 'test0()', "UserParam was 0" );
    Assert.equal( "test1, array output", 'test1().sort()', [1,2,3,4] );   
});
 * 
 *
 * @name UnitTest
 * @namespace
 * @see Assert
 */
var UnitTest = function( ) {
    var that = {};

    //////////////////////////////////////////////////////////////////////
    // Helper function to log something, if we can log

    function utLog( string ) {
        if ( ( typeof Log === "object" ) && Log.trace && ( typeof Log.trace === "function" ) ) {
            Log.trace( "UnitTest: " + string );
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Create an Assert object, eg. every time we create a Fixture, an Assert object is created.

    function CreateAssertObject( fixtureName ) {
        var that = {};

        // Store results of testcases in here.
        var m_reports = [ ];

        // Timestamp for last time a test ended - or initial start of the assert.
        var m_lastEnd;

        that.ActivateAssertObject = function( ) {
            m_lastEnd = Date.now( );
        }
        that.ActivateAssertObject( );

        // A single testcase object

        function CreateTestcaseObject( object ) {
            // Object must contain:
            // { number: testnum, passed: passed, fixture:this.name, description:description, report: printbuffer, expression:expr_str, expected:expected, typeofexpected:typeofexpected, result:res, typeofresult:typeofres } );
            var that = object;

            // Adjust time

            // Take two strings, and return a string describing their difference
            var compareStrings = function( expected, actual, leadin, maxwidth ) {
                // Find first difference
                var max_length = Math.max( expected.length, actual.length );
                var i = 0;
                while ( i < max_length && actual.charAt( i ) == expected.charAt( i ) ) {
                    ++i;
                }
                // Adjust to "window"
                var begin = ( i > leadin ) ? ( i - leadin ) : 0;
                var prefix = begin > 0 ? "..." : "";
                var suffixe = ( begin + maxwidth < expected.length ) ? "..." : "";
                var suffixa = ( begin + maxwidth < actual.length ) ? "..." : "";
                var res = "Expected and actual differs at position " + i + ":\n";
                res += "Expected: " + prefix + expected.slice( begin, begin + maxwidth ).replace( "\n", "\\n" ).replace( "\t", "\\t" ) + suffixe + "\n";
                res += "Actual  : " + prefix + actual.slice( begin, begin + maxwidth ).replace( "\n", "\\n" ).replace( "\t", "\\t" ) + suffixa + "\n";
                res += ( "          " + prefix + actual.slice( begin, i ).replace( "\n", "\\n" ).replace( "\t", "\\t" ) ).replace( /[\s\S]/g, " " )
                res += "^";
                return res;
            }

            that.toString = function( ) {
                var res = ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> equal <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n";
                res += "Test number : " + ( this.number ) + "\n";
                res += "Description : " + ( this.description ) + "\n";
                res += "Evaluated   : " + ( this.expression ) + "\n";
                res += "Time        : " + ( ( this.time / 1000.0 ).toFixed( 3 ) ) + "\n";
                res += "Expected    : " + ( this.expected ) + " (" + this.typeofexpected + ")\n";
                res += "Actual      : " + ( this.result ) + " (" + this.typeofresult + ")\n";
                if ( this.stack != "" ) {
                    res += "Stacktrace  : " + this.stack;
                }
                if ( "string" == this.typeofexpected && "string" == this.typeofresult ) {
                    res += "String diff : " + compareStrings( this.expected, this.result, 20, 80 ) + "\n";
                }
                // If xml, compare. Both expected and result should be toSource now.
                if ( "xml" == this.typeofexpected && "xml" == this.typeofresult ) {
                    res += "XML diff    : " +
                        compareStrings( this.expected, this.result, 20, 80 ) + "\n";
                }


                res += "Result      : " + ( function( ) {
                        if ( this.passed ) {
                            return "PASSED"
                        } else {
                            return "FAILED"
                        }
                    } )( ) + "\n";
                res += ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n";
                return res;
            }
            that.escapeXml = function( arg ) {
                return arg.replace( "&", "&amp;", "g" ).replace( "<", "&lt;", "g" ).replace( ">", "&gt;", "g" ).replace( "'", "&apos;", "g" )
                    .replace( '"', "&quot;", "g" );
            }
            that.toJUnitXml = function( ) {
                var res = "";
                res += "<testcase classname='" + this.escapeXml( this.fixture ) + "' time='" + ( ( this.time / 1000.0 ).toFixed( 3 ) ) + "' name='" + this.escapeXml( this.description ) + "'";
                if ( this.passed ) {
                    res += "/>\n";
                } else {
                    // Dropped indent of failure because it gets to be part of the text
                    res += ">\n<failure type='CompareError'>\n" +
                        this.escapeXml( "Fixture     : " + this.fixture + "\n" +
                        "Description : " + this.description + "\n" +
                        "Evaluated   : " + this.expression + "\n" +
                        "Expected    : " + this.expected + " (" + this.typeofexpected + ")\n" +
                        "Actual      : " + this.result + " (" + this.typeofresult + ")\n" );
                    if ( this.stack != "" ) {
                        res += "Stacktrace  : " + this.escapeXml( this.stack );
                    }
                    if ( "string" == this.typeofexpected && "string" == this.typeofresult ) {
                        res += this.escapeXml( "String diff : " + compareStrings( this.expected, this.result, 40, 120 ) + "\n" );
                    }
                    // If xml, compare. Both expected and result should be toSource now.
                    if ( "xml" == this.typeofexpected && "xml" == this.typeofresult ) {
                        res += this.escapeXml( "XML diff    : " +
                            compareStrings( this.expected, this.result, 20, 80 ) + "\n" );
                    }
                    res += "</failure>\n";
                    res += "</testcase>\n";
                }
                return res;
            }
            return that;
        }

        var m_totalPassed = 0;
        var m_totalFailed = 0;

        /**
         * The number of passed tests in this fixture.
         * 
         * 
         * @syntax Assert.totalPassed()
         * @return {number} The number of passed tests in this fixture
         * @name Assert.totalPassed
         * @method
         */
        that.totalPassed = function( ) {
            return m_totalPassed;
        };

        /**
         * Returns the number of currently failed tests.
         * 
         * This is mostly used by scripts that defines a number of modules to
         * unittest. If you end such a script with a call to this function, it
         * will return that value to the calling shell, and it can be used to
         * detect any failures. 
         * 
         * @type {method}
         * @syntax UnitTest.totalFailed()
         * @return The number of failed test so far.
         * @name UnitTest.totalFailed
         * @sourceName that.totalFailed
         * @wronglyPlacedComment maybe
         * @method
         */
        /**
         * The number of failed tests in this fixture.
         * 
         * 
         * @type {property}
         * @syntax Assert.totalFailed()
         * @return {number} The number of failed tests in this fixture
         * @name Assert.totalFailed
         * @sourceName that.totalFailed
         * @wronglyPlacedComment maybe
         * @method
         */
        that.totalFailed = function( ) {
            return m_totalFailed;
        };

        /**
         * The name of this fixture.
         * 
         * 
         * @type {property}
         * @name Assert.name
         * @method
         */
        that.name = new String( fixtureName );

        /**
         * Build a report of all test fixtures and all test cases in either string or Xml format.
         * 
         * This builds a report of all executed test fixtures, with all test
         * cases. The report is formatted in Xml format or string format,
         * depending on the value of the UnitTest.outputXml variable 
         * 
         * @type {method}
         * @syntax UnitTest.report()
         * @return A string with a test report, suitable for printing
         * @name UnitTest.report
         * @sourceName that.report
         * @wronglyPlacedComment maybe
         * @method
         */
        /**
         * Return a report for the fixture.
         * 
         * This method returns a string, that contains a list with details about
         * each testcase. By default only failed testcases are reported, but if
         * verbose is true, passed testcases are included too. 
         * 
         * @type {method}
         * @syntax Assert.report( verbose );
         * @param verbose If true, include passed tests in report, otherwise only failed tests
         * @name Assert.report
         * @sourceName that.report
         * @wronglyPlacedComment maybe
         * @method
         */
        that.report = function( verbose ) {
            var res = "";
            for ( var reporti in m_reports ) {
                var report = m_reports[ reporti ];
                if ( verbose || report.passed === false ) {
                    res += report.toString( );
                }
            }
            if ( res !== "" ) {
                res = ">>>>>>>>>>>>>> " + this.name + "\n" + res;
            }
            return res;
        };

        /**
         * Build a report of all test fixtures and all test cases in JUnit Xml format.
         * 
         * This builds a report of all executed test fixtures, with all test
         * cases. The report is formatted in Xml format, compatible with JUnit 
         * 
         * @type {method}
         * @syntax UnitTest.report()
         * @return A string with a test report, suitable for printing
         * @name UnitTest.reportJUnitXml
         * @method
         */
        that.reportJUnitXml = function( ) {
            var res = "";
            for ( var reporti in m_reports ) {
                res += m_reports[ reporti ].toJUnitXml( );
            }
            return res;
        }

        /**
         * Return an array of numbers for the failed testcases.
         * 
         * This method returns an array containing numbers of the failed testcases 
         * 
         * @type {method}
         * @syntax Assert.failed()
         * @return Array with numbers of failed testcases
         * @name Assert.failed
         * @method
         */
        that.failed = function( ) {
            var res = [ ];
            for ( var reporti in m_reports ) {
                var report = m_reports[ reporti ];
                if ( report.passed === false ) {
                    res.push( report.number );
                }
            }
            return res;
        };

        ////////////////////////////////////////////////////////////////////////
        // Public interface of Assert object


        // Assertfunction for values rather than eval'ed strings.
        /**
         * Test that two values are the same.
         * 
         * Similar to Assert.equal, except that the parameter is a value, and not
         * a string to be evaluated. 
         * 
         * @syntax Assert.equalValue( description, value, expected );
         * @param description Used for feedback in case of errors
         * @param value The value to compare.
         * @param expected The expected result.
         * @return true if the test passed, false otherwise
         * @example Assert.equalValue( "2 + 2 is 5", 2+2, 5)
         * @name Assert.equalValue
         * @method
         */
        that.equalValue = function( desc, a, b ) {
            if ( uneval( a ) === uneval( b ) ) {
                return that.equal( desc, true, true );
            } else {
                return that.equal( desc, uneval( a ), b );
            }
        }

        /**
         * Tests if two values are different..
         * 
         * Similar to Assert.equal, but tests for not equal. 
         * 
         * @syntax Assert.notEqual( description, value, expected );
         * @param description Used for feedback in case of errors
         * @param value The value to compare.
         * @param expected The expected result.
         * @return true if the test passed, false otherwise
         * @name Assert.notEqual
         * @method
         */
        that.notEqual = function( desc, result, expected ) {
            if ( arguments.length !== 3 ) {
                throw "notEqual needs description, result, and expected value";
            }
            // The current implementation is just a quick hack, 
            // as it will be reimplemted when the
            // unit test system is due for some refactoring,
            // see bug 10323.
            resultString = uneval( Util.keySortedObject( result ) );
            expectedString = uneval( Util.keySortedObject( expected ) );
            if ( resultString !== expectedString ) {
                return that.equal( desc, 1, 1 );
            } else {
                return that.equal( desc, uneval( resultString ), "not " + expectedString );
            }
        }



        ////////////////////////////////////////////////////////////////////////
        // Use with a string including an expression, and the expected result
        // Note, the result of expr may be printed, so it only works with printable results
        /**
         * Test that two expressions are equal.
         * 
         * This method is the core of the testing system. It takes an expression
         * that it evaluates, and compares the result to the expected value. If
         * the results are equal, a success is reported. If the results are not
         * equal, a failure is recorded. The optional description parameter is
         * used in error reports, and in general it is recommended that you
         * provide a description to facilitate debugging when tests fails. You can
         * pass arrays to this method; they will be compared elementwise. Elements
         * need to occur in the same order, so [1,2] !== [2,1]. Use sort if you
         * need set-like comparision. You can also pass objects to this method.
         * Objects are compared as arrays, except for the length property. For
         * both, only ownProperties are compared. 
         * 
         * @syntax Assert.equal( [description, ], expression, expected );
         * @param description Optional parameter, used for feedback in case of errors
         * @param expression The expression to evaluate. This must be a string that can be evaluated or the method will not work as expected.
         * @param expected The expected value from the evaluated expression. Note, that this is not evaluated.
         * @return true if the test passed, false otherwise
         * @note The last parameter to this function is not evaluated. This means that e.g. Assert.equal( 'true', 'true' ) will fail, as the last argument is a string, which is different from the result of eval( 'true' ).
         * @name Assert.equal
         * @method
         */
        that.equal = function( description, expr, expected ) {
            utLog( "Assert entering equal, desc: " + description );

            var uncaught = false;
            var errorstring;
            // Massage args
            if ( arguments.length < 3 ) {
                expected = expr;
                expr = description;
                description = "<no description of testcase>";
            }
            var res; // "evaluated" expr
            var expr_str = 'UNAVAILABLE (not a string)';
            var stack = "";
            if ( typeof( expr ) === 'string' ) {
                // Eval the expression.
                try {
                    res = eval( expr );
                    expr_str = expr;
                } catch ( error ) {
                    res = "Received exception: " + error.toSource( );
                    // If it has a stack, and it is in SpiderMonkey format, try making it more readable.
                    if ( error.stack && ( typeof error.stack == "string" ) ) {
                        stack = error.stack.replace( /(@[^:\[]*:)/g, "\n$1" );
                    }
                    // Maybe there will sometimes be a Rhino stack there here. We hope...
                    // It needs to be change from a java.lang.String into a JavaScript string.
                    if ( error.rhinoException ) {
                        stack = String( error.rhinoException.getScriptStackTrace( ) );
                    }
                    errorstring = error;
                    uncaught = true;
                }
            } else {
                // just "keep" the expresson
                res = expr;
            }
            var time = Date.now( ) - m_lastEnd;

            // var printbuffer = "<testcase class=\"" + this.name + "\" name=\"" + description + "\">\n";
            var printbuffer = "";

            function println( arg ) {
                printbuffer += arg + "\n";
            }

            var typeofres = Util.getType( res );
            var typeofexpected = Util.getType( expected );
            var passed;

            try {
                passed = ( res === expected ) || ( uneval( Util.keySortedObject( res ) ) === uneval( Util.keySortedObject( expected ) ) );
            } catch ( e ) {
                passed = false;
                uncaught = true;
                errorstring = "Got exception while trying to compare result with expected - this test may or may not be passed. This is likely due to a getter throwing an exception. The exception was:" + e;
            }

            var testnum = m_totalPassed + m_totalFailed;
            // Create a report
            // println( ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> equal <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<" );
            // var singleLineError = "unknown:0:Error: JSUT; '" + description + "' Evaluating; '" + expr_str + "' ";
            if ( uncaught ) {
                // println( "WARNING!!!!!!! UNCAUGHT EXCEPTION: '" + errorstring + "'" );
                // singleLineError += "UNCAUGHT EXCEPTION; '" + errorstring + "'";
            }
            // println( "Test number  : "  + ( testnum ) );
            // println( "Description  : '" + description + "'" );
            // println( "Evaluating   : '" + expr_str + "'" );
            if ( expected != undefined ) {
                // Have to wrap this in try-catch, as Rhino barfs on toSource from ValueCheck
                try {
                    expected = expected.toSource( );
                } catch ( e ) {
                    expected = expected.toString( );
                }
            }
            if ( res != undefined ) {
                // Have to wrap this in try-catch, as Rhino barfs on toSource from ValueCheck
                try {
                    res = res.toSource( );
                } catch ( e ) {
                    res = res.toString( );
                }
            }
            //println( "Expecting    : '" + expected + "' (" + typeofexpected + ")" );
            // singleLineError += "Expecting; '" + expected + "' (" + typeofexpected + ") ";
            // println( "Evaluated to : '" + res + "' (" + typeofres + ")" );
            // singleLineError += "Evaluated to; '" + res + "' (" + typeofres + ") ";

            if ( passed ) {
                m_totalPassed++;
                // println( "Result       : PASSED" ); 
            } else {
                if ( uncaught ) {
                    println( "WARNING!!!!!!! UNCAUGHT EXCEPTION: '" + errorstring + "'" );
                }
                println( "Expecting    : '" + expected + "' (" + typeofexpected + ")" );
                println( "Evaluated to : '" + res + "' (" + typeofres + ")" );
                // println( "Result       : FAILED" );
                // println( singleLineError ); 
                m_totalFailed++;
            }
            // println( "</testcase>\n" );
            // println( ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<" );
            // Add it to the list of reports
            m_reports.push( CreateTestcaseObject( {
                        number: testnum,
                        passed: passed,
                        fixture: this.name,
                        description: description,
                        report: printbuffer,
                        expression: expr_str,
                        expected: expected,
                        typeofexpected: typeofexpected,
                        result: res,
                        typeofresult: typeofres,
                        stack: stack,
                        time: time
                    } ) );
            this.ActivateAssertObject( );
            utLog( "Assert leaving equal, desc: " + description );
            return passed;
        }; // equal function.

        /**
         * Test that an expression evalutes to true.
         * 
         * As equal, but assumes that the result must evaluate to true 
         * 
         * @syntax Assert.that( [description, ], expression );
         * @param description Optional parameter, used for feedback in case of errors
         * @param expression The expression to evaluate. This must be a string that can be eval, or the feedback will be less useful
         * @return true if the test passed, false otherwise
         * @name Assert.that
         * @method
         */
        that.that = function( description, expr ) {
            if ( arguments.length < 2 ) {
                return this.equal( description, true );
            } else {
                return this.equal( description, expr, true );
            }
        };

        /**
         * Test that an expression evalutes to false.
         * 
         * As equal, but assumes that the result must evaluate to false 
         * 
         * @syntax Assert.not( [description, ], expression );
         * @param description Optional parameter, used for feedback in case of errors
         * @param expression The expression to evaluate. This must be a string that can be eval, or the feedback will be less useful
         * @return true if the test passed, false otherwise
         * @name Assert.not
         * @method
         */
        that.not = function( description, expr ) {
            if ( arguments.length < 2 ) {
                return this.equal( description, false );
            } else {
                return this.equal( description, expr, false );
            }
        };



        // Function that expects an exception
        // If exception is expected, call with 'true' for last argument, 'false' else
        /**
         * Test that an expression throws an exception when evaluated.
         * 
         * This test checks that evaluating the expression raises an exception. 
         * 
         * @syntax Assert.exception( [description, ], expression );
         * @param description Optional parameter, used for feedback in case of errors.
         * @param expression The expression to evaluate. This must be a string that can be eval, or the feedback will be less useful
         * @return true if the test passed, false otherwise
         * @name Assert.exception
         * @method
         */
        that.exception = function( description, expr ) {
            if ( arguments.length < 2 ) {
                return this.that( '\
                    var caught = false;\
                    try {\
                        ' + description + ';\
                    } catch ( e ) {\
                        caught = true;\
                    }\
                    caught' );
            } else {
                return this.that( description, '\
                    var caught = false;\
                    try {\
                        ' + expr + ';\
                    } catch ( e ) {\
                        caught = true;\
                    }\
                    caught' );
            }
        };

        // Return created object
        return that;
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Back to UnitTest function

    // Should user call report?
    var isLoadingAsModule = function( ) {
        return __ModulesInfo.isLoadingModule( );
    };

    /**
     * Should clients actually perform tests.
     * 
     * Clients can call this method prior to performing tests, although if
     * using addFixture, this is handled automatically. 
     * 
     * @type {method}
     * @syntax UnitTest.doTests()
     * @return true if clients should perform tests, false otherwise.
     * @name UnitTest.doTests
     * @method
     */
    that.doTests = function( ) {
        return !isLoadingAsModule( );
    };

    /**
     * Should clients emit a report.
     * 
     * Clients can call this method prior to emitting a test report. This is
     * handled automatically if using addFixture. 
     * 
     * @type {method}
     * @syntax UnitTest.emitReport()
     * @return true if clients should emit a report, false otherwise
     * @name UnitTest.emitReport
     * @method
     */
    that.emitReport = function( ) {
        return !isLoadingAsModule( );
    };

    that.verbose = false; // Set to true for verbose, false only output on error

    that.outputXml = false; // Set to true to get output in xml for JUnit

    // Store all the Assert objects in here. The default one is index 0
    var m_fixtures = [ ];

    // Stack to hold references to existing sessions
    var m_fixtureStack = [ ];

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Begin a new test fixture.
     * 
     * Begin a new text fixture, that is, a set of testcases that logically
     * belongs together. Note, that you can "nest" fixtures. If you have not
     * explicitely created a test fixture, you will be using the default
     * "global" test fixture. It is recommended to use addFixture to manage
     * your test fixtures. 
     * 
     * @type {method}
     * @syntax UnitTest.beginFixture( name )
     * @param name The name of the test fixture
     * @name UnitTest.beginFixture
     * @method
     */
    that.beginFixture = function( name ) {
        utLog( "beginFixture: " + name );
        // No checking
        if ( Assert !== undefined ) {
            m_fixtureStack.push( Assert );
        }
        Assert = CreateAssertObject( name );
        m_fixtures.push( Assert );
    };

    /**
     * End the current test fixture.
     * 
     * Ends the current test fixture, and returns to the previously active
     * test fixture. You do not need to call this method, if using addFixture. 
     * 
     * @type {method}
     * @syntax UnitTest.endFixture()
     * @name UnitTest.endFixture
     * @method
     */
    that.endFixture = function( ) {
        utLog( "endFixture" );
        Assert = m_fixtureStack.pop( );
        if ( Assert !== undefined ) {
            Assert.ActivateAssertObject( )
        }
    };

    /**
     * Create a new fixture and optionally run it.
     * 
     * This is the main method to use to create a test fixture and have it
     * automatically run and report as needed. 
     * 
     * @type {method}
     * @syntax UnitTest.addFixture( name, func )
     * @param name The name of the testfixture. Use something that identifies the module or set of tests
     * @param func A function that has the actual tests (Asserts) in it
     * @return The number of failed tests, if any was run, 0 otherwise (also 0 if no tests failed).
     * @example Use it like this:
     *   UnitTest.addFixture( "Test Module modulename", function() { 
     * 
     *     Assert.exception( "readFile on non existant file", 'readFile("nothere")' ); 
     *     Assert.that( "comment", 'expression evaluating to true' );
     *     Assert.not( "This should always be false", 'expression evaluating to false' );
     *     Assert.equal( "test0, output", 'test0()', "UserParam was 0" );
     *     Assert.equal( "test1, array output", 'test1().sort()', [1,2,3,4] );
     *     
     *   });
     * @name UnitTest.addFixture
     * @method
     */
    that.addFixture = function( name, func ) {
        var res = 0;
        if ( this.doTests( ) ) {
            this.beginFixture( name );
            try {
                func( );
            } catch ( e ) {
                // Ups, leaked expection, stuff it into the unittest system
                __UnitTest__ut_e = e;
                Assert.equal( "Fixture does not leak exceptions", "throw __UnitTest__ut_e;", "No exceptions" );
                delete __UnitTest__ut_e;
            }
            res = Assert.totalFailed( );
            this.endFixture( );
            // Only auto-emit report, if we are not nested "beyond" the Global default fixture.
            if ( this.emitReport( ) && m_fixtureStack.length <= 1 ) {
                use( "Print" );
                // print ( this.reportJUnitXml() + "\n" );
                print( this.report( ) + "\n" );
            }
        }
        return res;
    };


    var m_printbuffer = "";

    function println( arg ) {
        m_printbuffer += arg + "\n";
    }


    /**
     * Build a report of all test fixtures and all test cases..
     * 
     * This builds a report of all executed test fixtures, with all test
     * cases. The report is a string, suitable for outputting to a console or
     * similar. By default, you get a summary and a report for each failed
     * test. You can control the level of verbosity with the variable verbose;
     * if set to true, also non-failed tests are output. 
     * 
     * @type {method}
     * @syntax UnitTest.report()
     * @return A string with a test report, suitable for printing
     * @name UnitTest.reportString
     * @method
     */
    that.reportString = function( ) {
        var totalPassed = 0;
        var totalFailed = 0;
        m_printbuffer = "";

        // Report is splitted in two, first the overview, then the summary
        var details = "";
        for ( var x in m_fixtures ) {
            details += m_fixtures[ x ].report( this.verbose );
        }
        if ( details !== "" ) {
            println( "=============================== DETAILS ===============================" );
            println( details );
        }

        // println( "=============================== SUMMARY ===============================" );
        var count_fixtures = 0;
        for ( x in m_fixtures ) {
            var ses = m_fixtures[ x ];

            // Skip default global fixture, if no tests was actually done under this fixture (bug 8969)
            if ( x === "0" && ses.totalPassed( ) === 0 && ses.totalFailed( ) === 0 ) {
                continue;
            }
            count_fixtures += 1;

            totalPassed += ses.totalPassed( );
            totalFailed += ses.totalFailed( );

            if ( this.verbose || ses.totalFailed( ) > 0 ) {
                println( "-----------------------------------------------------------------------" );
                println( "Fixture: " + ses.name );
                var fixtotal = ses.totalPassed( ) + ses.totalFailed( );
                println( "Result : " + ses.totalPassed( ) + " / " + fixtotal + " passed (" +
                    ses.totalPassed( ) / fixtotal * 100 + " %)." );
                if ( ses.totalFailed( ) > 0 ) {
                    println( "Failed#: " + ses.failed( ).join( ", " ) );
                }
            }
        }

        // Print some info about totals
        if ( count_fixtures > 0 ) {
            println( "=======================================================================" );
            println( "Totals for all " + count_fixtures + " fixtures in test" );
            var sestotal = totalPassed + totalFailed;
            println( "Result : " + totalPassed + " / " + sestotal + " passed (" +
                totalPassed / sestotal * 100 + " %)." );
            println( "=======================================================================" );
        }
        if ( totalFailed !== 0 ) {
            println( "!!!!!!!!!! FAILURES WERE PRESENT !!!!!!!!!!" );
        } else {
            if ( totalPassed > 0 ) {
                println( "All tests passed" );
            }
        }
        return m_printbuffer;
    };

    that.reportJUnitXml = function( ) {
        var res = "<testsuite tests=\"" + m_fixtures.length + "\">\n";
        for ( var x in m_fixtures ) {
            res += m_fixtures[ x ].reportJUnitXml( );
        }
        res += "</testsuite>\n";
        return res;
    };

    that.report = function( ) {
        if ( this.outputXml ) {
            return this.reportJUnitXml( );
        } else {
            return this.reportString( );
        }
    }



    that.totalFailed = function( ) {
        var res = 0;
        for ( var x in m_fixtures ) {
            res += m_fixtures[ x ].totalFailed( );
        }
        return res;
    };

    return that;
}( );

// Add a default global Assert object
UnitTest.beginFixture( "Default global fixture" );
