use("Engine");

if ( ! Engine.isEngine( Engine.SPIDERMONKEY ) ) {
    EXPORTED_SYMBOLS = [];
} else {
    use("ES5_spidermonkey");
}
