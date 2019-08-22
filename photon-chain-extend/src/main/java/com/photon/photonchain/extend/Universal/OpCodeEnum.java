package com.photon.photonchain.extend.Universal;

import com.photon.photonchain.exception.BusinessException;
import com.photon.photonchain.exception.ErrorCode;
import com.photon.photonchain.extend.compiler.Pcompiler;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @Author:
 * @Description:
 * @Date:16:46 2018/4/
 * @Modified by:
 */
public enum OpCodeEnum {
    ADD("ADD", "+"),
    SUB("SUB", "-"),
    MUL("MUL", "*"),
    DIV("DIV", "/"),
    SEQ("SEQ","="),
    INC("INC", "++"),
    DEC("DEC", "--"),
    LT("LT", "<"),
    LTEQ("LT", "<="),
    GT("GT", ">"),
    GTEQ("GT", ">="),
    NOT("NOT", "<>"),
    EQ("EQ", "=="),
    ISZERO("ISZERO", "0"),
    NULL("NULL", ""),
    RETURN("RETURN", "RETURN"),
    STOP("STOP", "STOP"),
    MOD("MOD", "%"),
    AND("AND", "&&"),
    OR("OR", "||"),
    IF("IF", "if"),
    nowTime("nowTime","nowTime"),
    TRANS("TRANS", "TRANS"),
    VOTE("VOTE", "VOTE"),
    TOKEN("TOKEN", "TOKEN");

    public String msg;
    public String key;
    OpCodeEnum(){

    }
    OpCodeEnum(String msg, String key) {
        this.msg = msg;
        this.key = key;
    }

    //匹配操作码
    public static OpCodeEnum matchOpCode(String opCodeStr) {
        for (OpCodeEnum opCode : OpCodeEnum.values()) {
            if (opCode.name().equalsIgnoreCase(opCodeStr)) {
                return opCode;
            }
        }
        return OpCodeEnum.NULL;
    }

    //匹配IF操作码
    public static boolean matchStartsWithIFCode(String s) {
        return StringUtils.startsWith(s, IF.key)
                && s.substring(2, 3).equalsIgnoreCase("(")
                && StringUtils.containsAny(s, new char[]{'(', ')', '{', '}'});
    }

    //验证编译参数命名是否与操作码相同
    public static boolean matchCompilerVarOpCode(String opCodeStr) {
        for (OpCodeEnum opCode : OpCodeEnum.values()) {
            if (opCode.name().equalsIgnoreCase(opCodeStr) && !opCodeStr.equalsIgnoreCase("add")) {
                return false;
            }
        }
        return true;
    }
    //验证运算符
    public static boolean matchOperator(String str){
        if (StringUtils.containsNone(str,new char[]{OpCodeEnum.ADD.key.toCharArray()[0],OpCodeEnum.DIV.key.toCharArray()[0]
                ,OpCodeEnum.SUB.key.toCharArray()[0],OpCodeEnum.MUL.key.toCharArray()[0]}))
            return true;
        else
            return false;
    }
    //验证编译操作符
    public static String matchConditionOpCode(String str) {
        String[] var = Pcompiler.getContent(str,"(",")").split(" ");
        ArrayList<String> list = new ArrayList();
        if (var[1].equals( EQ.key)) {//等等与
            list.add("==");
        } else if (var[1].equals( LTEQ.key)) {//小于或等于
            list.add("<=");
        } else if (var[1].equals( GTEQ.key)) {//大于或等于
            list.add(">=");
        } else if (var[1].equals( NOT.key)) {
            list.add("<>");
        }else if (var[1].equals( GT.key)) {//大于
            list.add(">");
        } else if (var[1].equals( LT.key)) {//小于
            list.add("<");
        } else return "";
        if (list.size() > 1) return "";
        for (OpCodeEnum opCode : OpCodeEnum.values()) {
            if (opCode.key.equalsIgnoreCase(list.get(0))) {
                return opCode.key;
            }
        }
        return "";
    }
}
