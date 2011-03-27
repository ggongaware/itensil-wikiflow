package itensil.util;

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

public class StringHelper {
	
	
    public static Set<String> setFromString(String dlmStr) {
        Set<String> sSet = new HashSet<String>();
        if (!Check.isEmpty(dlmStr)) {
            StringTokenizer tok = new StringTokenizer(dlmStr);
            while (tok.hasMoreTokens()) {
                sSet.add(tok.nextToken());
            }
        }
        return sSet;
    }


    public static String stringFromSet(Set<String> sSet) {
    	if (sSet == null) return "";
        if (sSet.size() == 1) {
            return sSet.iterator().next();
        } else if (sSet.size() > 1) {
            StringBuffer buf = new StringBuffer();
            for (String str : sSet) {
                buf.append(str);
                buf.append(' ');
            }
            return buf.substring(0, buf.length() - 1);
        } else {
            return "";
        }
    }
}
