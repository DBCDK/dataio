function process(objstr, additionalArgs) {
    let obj = JSON.parse(objstr);
    return obj.id.toUpperCase() + ":" + additionalArgs.format;
}
