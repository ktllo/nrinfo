package org.leolo.web.nrinfo.util;

import scala.Tuple3;

public class IrcUtil {

    public static Tuple3<String, String, String> splitHostmask(String hostmask) {
        if (hostmask == null || hostmask.strip().length() == 0) {
            return null;
        }
        int delim1 = hostmask.indexOf("!");
        int delim2 = hostmask.indexOf("@");
        if (delim2 <= 0 || delim1 <= 0 || delim1 > delim2){
            throw new RuntimeException("Incorrect format");
        }
        String nick = hostmask.substring(0, delim1);
        String ident = hostmask.substring(delim1+1, delim2);
        String host = hostmask.substring(delim2+1);
        return new Tuple3<>(nick, ident, host);
    }
}
