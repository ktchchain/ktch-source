package com.photon.photonchain.interfaces.utils;

/**
 * @author: lqh
 * @description: 数据校验
 * @program: photon-chain
 * @create: 2018-03-08 13:40
 **/
public class ValidateUtil {
    /**
     * 正则表达式验证密码（6-20 位，字母、数字、字符）
     *
     * @param input
     * @return
     */
    public static boolean rexCheckPassword(String input) {
        // 6-20 位，字母、数字、字符
        //String reg = "^([A-Z]|[a-z]|[0-9]|[`-=[];,./~!@#$%^*()_+}{:?]){6,20}$";
        String regStr = "^([A-Z]|[a-z]|[0-9]|[!@#$%^&*()-=_+,.<>/-]){6,20}$";
        return input.matches(regStr);
    }

    /**
     * 正则表达式验证字母（由26个英文字母组成的字符串 ）
     *
     * @param input
     * @return
     */
    public static boolean checkLetter(String input) {
        String regStr = "^[A-Za-z0-9]+$";
        return input.matches(regStr);
    }

    /**
     * 正整数
     *
     * @param input
     * @return
     */
    public static boolean checkPositiveInteger(String input) {
        String regStr = "^[0-9]*[1-9][0-9]*$";
        return input.matches(regStr);
    }

    // 判断一个字符是否是中文
    public static boolean isChinese(char c) {
        return c >= 0x4E00 && c <= 0x9FA5;// 根据字节码判断
    }

    // 判断一个字符串是否含有中文
    public static boolean isChinese(String str) {
        if (str == null) return false;
        for (char c : str.toCharArray()) {
            if (isChinese(c)) return true;// 有一个中文字符就返回
        }
        return false;
    }

    // 根据UnicodeBlock方法判断中文标点符号
    public static boolean isChinesePunctuation(String str) {
        char[] c = str.toCharArray();
        for (int i = 0; i < c.length; i++) {
            Character.UnicodeBlock ub = Character.UnicodeBlock.of(c[i]);
            if (ub == Character.UnicodeBlock.GENERAL_PUNCTUATION
                    || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                    || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
                    || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_FORMS
                    || ub == Character.UnicodeBlock.VERTICAL_FORMS) {
                return true;
            }
        }
        return false;
    }
}
