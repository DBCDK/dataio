function toEmptyString(str) {
    return "";
}

function toUpperCase(str) {
    return str.toUpperCase();
}

function throwException(str) {
    if (str === "throw" || str === "") {
        throw "this is an exception from JavaScript";
    }
    if (str === "illegal-operation-on-control-field") {
        throw "Illegal operation on control field";
    }
    if (str === "fail") {
        Packages.dk.dbc.javascript.recordprocessing.FailRecord.doThrow("errorMessage");
    }
    if (str === "ignore") {
        Packages.dk.dbc.javascript.recordprocessing.IgnoreRecord.doThrow("errorMessage");
    }
    return str;
}

function concat(str, additionalArgs) {
    return additionalArgs.submitter + " " + str + " " + additionalArgs.format;
}
