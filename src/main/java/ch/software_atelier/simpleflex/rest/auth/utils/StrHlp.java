package ch.software_atelier.simpleflex.rest.auth.utils;

import java.util.*;

public class StrHlp {

    public static long parseLong(String s){
        StringBuilder result = new StringBuilder();
        char[] chars = s.toCharArray();
        
        if (chars.length>0){
            if (chars[0]=='-')
                result.append('-');
        }
        for (char aChar : chars) {
            if ((aChar == '.') | (aChar == ',')) {
                break;
            }
            if (Character.isDigit(aChar))
                result.append(aChar);
        }
        String resStr = result.toString();
        if (resStr.length()==0)
            return 0;
        else
            return Long.parseLong(resStr);
    }

    public static List<String> tokenize(String src, String separator){
        List<String> list = new ArrayList<>();
        StringTokenizer st = new StringTokenizer(src,separator);
        while (st.hasMoreTokens()){
            list.add(st.nextToken());
        }
        return list;
    }

}
