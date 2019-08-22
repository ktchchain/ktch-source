package com.photon.photonchain.extend.Universal;

import com.photon.photonchain.extend.compiler.RegexCompile;

import java.util.regex.Pattern;

public enum TypeEnum {
    str,
    num,
    integer,
    coin,
    time,
    arr,
    address;

    public static String matchTypeByRegex(String type) {
        String result = "";
        switch (type) {
            case "str":
//                result = "^\".*?\"$";
                result  = "^\"([^\"]*)\"$";
                break;
            case "num":
                result = "^([0-9]{1,}[.][0-9]*)$";
                break;
            case "coin":
//                result = "^(\\-|\\+)?\\d+(\\.\\d+)?$";
                result = "^\\d+$";
                break;
            case "integer":
                result = "^\\d+$";
                break;
            case "address":
                result = "\\w+";
                break;
            case "time":
//                result = "((((19|20)\\d{2})-(0?[13578]|1[02])-(0?[1-9]|[12]\\d|3[01]))|(((19|20)\\d{2})-(0?[469]|11)-(0?[1-9]|[12]\\d|30))|(((19|20)\\d{2})-0?2-(0?[1-9]|1\\d|2[0-8])))";
//                result = "((([1-9]\\d{3})-(0?[13578]|1[02])-(0?[1-9]|[12]\\d|3[01]))|(((19|20)\\d{2})-(0?[469]|11)-(0?[1-9]|[12]\\d|30))|(([1-9]\\d{3})-0?2-(0?[1-9]|1\\d|2[0-8])))\\s+(20|21|22|23|[0-1]\\d):[0-5]\\d:[0-5]\\d";
                result = "^\"[1-9]\\d{3}-(0[1-9]|1[0-2])-(0[1-9]|[1-2][0-9]|3[0-1])\\s+(20|21|22|23|[0-1]\\d):[0-5]\\d:[0-5]\\d\"$";
                break;
            case "arr":
//                result = "\\[([(?!_)(?!:;.*?_$)[a-zA-Z0-9_\\u4e00-\\u9fa5]+]|,[(?!_)(?!:;.*?_$)[a-zA-Z0-9_\\u4e00-\\u9fa5]+])+\\]";
                result = "\\[(\"([^\"]*)\"|,\"([^\"]*)\")+\\]";
//                result = "\\[((\"([^\"]*)\"|\\d)|,(\"([^\"]*)\")|\\d)+\\]";
//                result = "\\[((\"([^\"]*)\"|\\d||\\d\\.\\d)|,((\"([^\"]*)\")|\\d|\\d\\.\\d))+\\]";
                break;
        }
        return result;
    }

    //正则不包含数字
    public static boolean checkLetterLine(String str) {
        String reg = "([A-Z]|[a-z]|-|_){1,}";
        if (Pattern.matches(reg, str)) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean matchType(String typeStr) {
        for (TypeEnum typeEnum : TypeEnum.values()) {
            if (typeEnum.name().equals(typeStr)) {
                return true;
            }
        }
        return false;
    }

    public static boolean matchTypeVar(String type, String typeStr1, String typeStr2) {
        String reg = matchTypeByRegex(type);
        if (Pattern.matches(reg, typeStr1) && Pattern.matches(reg, typeStr2)) {
            return true;
        } else {
            return false;
        }
    }

    public static void main(String[] args) {
        String s =  "pcompiler version 1.0.0 ; \n" +
                "contract demo{\n" +
                "set str title = \"主题@#$@$@%#%@#，。、，、，。2425435\";\n" +
                "set arr options = [1,2.0,\"\",\"的所得税法，m\\,.87567452452312\",\"!@!$@%^@$^!@#$%^&*()_+\"]; \n" +
                "set time t = \"2019-10-02 00:00:00\";\n" +
                "function jiaoyan () {\n" +
                "if(options[0].count>2) {stop;}\n" +
                "if(options[1].count>=3){stop;}\n" +
                "if( nowTime>t ) {stop;}\n" +
                "}\n" +
                "event vote(jiaoyan,title,options);\n" +
                "} ";
        try {
            RegexCompile.checkRegex(s);
        }catch (Exception e){
            e.printStackTrace();
        }
        System.out.println();
    }
}
