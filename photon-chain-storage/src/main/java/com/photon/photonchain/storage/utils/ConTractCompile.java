package com.photon.photonchain.storage.utils;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ConTractCompile {

    //使用正则获取内容
    public static List<String> getMember(String str, String regex) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(str);
        List<String> result = new ArrayList<>();
        while (m.find()) {
            result.add(m.group());
        }
        return result;
    }

    public static int statistics(String str, String delimiter) {
        int count = 0;
        int index = -1;
        while ((index = str.indexOf(delimiter)) != -1) {
            str = str.substring(index + delimiter.length());
            count++;
        }
        return count;
    }

    //读一行
    public static String readLine(String str) {
        String line = null;
        str = str.trim();
        if (str.contains(";")) {
            line = str.substring(0, str.indexOf(";"));
            if (!line.contains("{") && !line.contains("}")) {
                return line;
            }
        }
        if (str.contains("{")) {
            int begin = 0;
            while (true) {
                line = str.substring(0, str.indexOf("}", begin) + 1);
                if (statistics(line, "{") == statistics(line, "}")) {
                    return line;
                }
                begin = str.indexOf("}", begin + 1);
            }
        }
        return line;
    }


    //获取括号里面内容
    public static String getContent(String str, String head, String end) {
        if (str.indexOf(head) > 0) {
            str = str.substring(str.indexOf(head));
            int he = 0;
            int en = 0;
            int index = 0;
            for (int i = 0; i < str.length(); i++) {
                if (head.equals(String.valueOf(str.charAt(i)))) {
                    he++;
                }
                if (end.equals(String.valueOf(str.charAt(i)))) {
                    en++;
                }
                if (he == en) {
                    index = i;
                    break;
                }
            }
            str = str.substring(1, index);
        }
//		str = str.substring(0,index+1);//移除head和end
        return str;
    }
    public static Map<String,String> readFunctionLine(String functionContent){
        Map<String, String> contentMap = new TreeMap<>();
        int i = 1;
        while (true) {
            String content = readLine(functionContent.trim());
            if (StringUtils.isNotBlank(content)) {
                contentMap.put("content" + i, content);
                i++;
            }
            if (StringUtils.isBlank(content)) {
                content += ";";
            }
            functionContent = functionContent.trim().substring(content.length(), functionContent.trim().length());
            if (functionContent.trim().length() == 0)
                break;
        }
        return contentMap;
    }

    public static boolean checkStr(String str) {
        String regex = "^(?!_)(?!.*?_$)[a-zA-Z0-9_\\u4e00-\\u9fa5]+$";
        return Pattern.matches(regex, str);
    }

    public static boolean checkNum(String str) {
//        String regex = "^(\\-|\\+)?\\d+(\\.\\d+)?$";
        String regex = "^([0-9]{1,}[.][0-9]*)$";
        return Pattern.matches(regex, str);
    }

    public static boolean checkInt(String str) {
//        String regex = "^(\\-|\\+)?\\d+(\\.\\d+)?$";
        String regex = "^\\d+$";
        return Pattern.matches(regex, str);
    }

    public static boolean checkTime(String str) {
        String regex = "((((19|20)\\d{2})-(0?[13578]|1[02])-(0?[1-9]|[12]\\d|3[01]))|(((19|20)\\d{2})-(0?[469]|11)-(0?[1-9]|[12]\\d|30))|(((19|20)\\d{2})-0?2-(0?[1-9]|1\\d|2[0-8])))\\s+(20|21|22|23|[0-1]\\d):[0-5]\\d:[0-5]\\d";
//        String regex = "((((19|20)\\d{2})-(0?[13578]|1[02])-(0?[1-9]|[12]\\d|3[01]))|(((19|20)\\d{2})-(0?[469]|11)-(0?[1-9]|[12]\\d|30))|(((19|20)\\d{2})-0?2-(0?[1-9]|1\\d|2[0-8])))";
        return Pattern.matches(regex, str);
    }

    public static boolean checkArr(String str) {
        String regex = "\\[([(?!_)(?!:;.*?_$)[a-zA-Z0-9_\\u4e00-\\u9fa5]+]|,[(?!_)(?!:;.*?_$)[a-zA-Z0-9_\\u4e00-\\u9fa5]+])+\\]";
        return Pattern.matches(regex, str);
    }

    public static boolean checkContract(String str) {
        String regex = "\\s*pcompiler\\s+version\\s+1\\.0\\.0;\\s*contract\\s+[\\s\\S]*";
        return Pattern.matches(regex, str);
    }

    public static boolean checkOneContract(String str) {
        String regex = "\\s*pcompiler\\s+version\\s+1\\.0\\.0;\\s*contract\\s+[\\s\\S]*\\{}\\s*";
        return Pattern.matches(regex, str);
    }

    public static boolean checkChina(String str) {
        //匹配这些中文标点符号 。 ？ ！ ， 、 ； ： “ ” ‘ ' （ ） 《 》 〈 〉 【 】 『 』 「 」 ﹃ ﹄ 〔 〕 … — ～ ﹏ ￥
//        String regex="\\u3002|\\uff1f|\\uff01|\\uff0c|\\u3001|\\uff1b|\\uff1a|\\u201c|\\u201d|\\u2018|\\u2019|\\uff08|\\uff09|\\u300a|\\u300b|\\u3008|\\u3009|\\u3010|\\u3011|\\u300e|\\u300f|\\u300c|\\u300d|\\ufe43|\\ufe44|\\u3014|\\u3015|\\u2026|\\u2014|\\uff5e|\\ufe4f|\\uffe5";
//        Matcher m = Pattern.compile(regex).matcher(str);
//        if (m.find()) {
//            return false;
//        }
//        return true;
        String regex = "[^。？！，、；：“”‘'（）《》〈〉【】『』「」﹃﹄〔〕…—～﹏￥]+";
        return Pattern.matches(regex, str);
    }

    public static boolean checkSet(String str) {
        String regex = "[\\s\\S]* +set [\\s\\S]*";
        return Pattern.matches(regex, str);
    }

    public static List getSetList(String str) {
        return getMember(str, "(set[\\s]+[^;]*)");
    }

    public static List getFunctionList(String str) {
        return getMember(str, "(function[\\s]+[^{]*)");
    }

    public static List getStopList(String str) {
        return getMember(str, "(stop[\\s]+\\w+[\\s]*[^;]*)");
    }

    public static List getReturnList(String str) {
        return getMember(str, "(return[\\s]+[^;]*)");
    }

    public static List getEventList(String str) {
        return getMember(str, "(event[\\s]+[^;]*)");
    }

    public static String[] splitStr(String str) {
        Pattern p = Pattern.compile("((?<=\\s\").*?(?=\"))|([[\\u4e00-\\u9fa5]|\\w|=|<|>|,]+)");
        Matcher m = p.matcher(str);
        List<String> list = new ArrayList<>();
        while (m.find()) {
            list.add(m.group());
        }
        String[] array = list.toArray(new String[list.size()]);
        return array;
    }
    public static List<String> getIfList(String content){
        List<String> ls=new ArrayList<String>();
        Pattern pattern = Pattern.compile("(?<=if\\()(.+?)(?=\\))");
        Matcher matcher = pattern.matcher(content);
        while(matcher.find())
            ls.add(matcher.group());
        return ls;
    }
    public static void main(String[] args) {
        String s = "2399-10-02 00:00:00";
        String result = "^[1-9]\\d{3}-(0[1-9]|1[0-2])-(0[1-9]|[1-2][0-9]|3[0-1])\\s+(20|21|22|23|[0-1]\\d):[0-5]\\d:[0-5]\\d$";
        System.out.println(checkNum("1230.110"));

    }
}