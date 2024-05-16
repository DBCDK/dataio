function process(objstr, additionalArgs) {
    let obj = JSON.parse(objstr);
    return obj.id.toLowerCase() + ":" + additionalArgs.format;
}
