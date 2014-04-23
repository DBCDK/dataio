function invocationFunction(record, supplementaryData) {

    var logger = Packages.org.slf4j.LoggerFactory.getLogger("Help");

    var md5 = Packages.java.security.MessageDigest.getInstance("md5");
    md5.update((new Packages.java.lang.String(record)).getBytes("UTF-8"));
    var res = md5.digest();
    var resStr = String();
    for(var i=0; i<res.length; i++) {
	resStr += (res[i] & 0xFF).toString(16);
    }

    return resStr;

}
