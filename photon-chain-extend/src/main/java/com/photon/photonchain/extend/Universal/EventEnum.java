package com.photon.photonchain.extend.Universal;

import com.photon.photonchain.exception.BusinessException;
import com.photon.photonchain.exception.ErrorCode;
import com.photon.photonchain.extend.compiler.Pcompiler;
import com.photon.photonchain.extend.compiler.RegexCompile;
import com.sun.org.apache.regexp.internal.RE;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

public enum EventEnum {
    business(2),
    test(0),
    vote(3),
    count(0),
    token(3),;
    private int a;

    EventEnum(int a) {
        this.a = a;
    }

    public static int checkEventName(String eventName) {
        for (EventEnum eventEnum : EventEnum.values()) {
            if (eventEnum.name().equals(eventName)) {
                return eventEnum.a;
            }
        }
        return -1;
    }

    public static boolean checkEventVote(String str) {
        boolean flag = false;
        int i = 0;
        List<String> setList = RegexCompile.getSetList(str);
        List<String> eventList = RegexCompile.getEventList(str);
        List<String> functionList = RegexCompile.getFunctionList(str);
        for (String eventContent : eventList) {
            String eventName = StringUtils.substringBetween(eventContent, "event ", "(");
            if (eventName.equals(vote.name())) {
                flag = true;
            }
        }
        if (flag) {
            for (String eventContent : eventList) {
                //取出方法名
                String eventName = StringUtils.substringBetween(eventContent, "event ", "(");
                if (eventName.equals(vote.name())) {
                    String[] vote = StringUtils.substringBetween(eventContent, "(", ")").replace(" ", "").split(",");
//                    for (String function : functionList) {
//                        if (StringUtils.substringBetween(function, "function ", "(").equals(vote[0])) {
//                            i++;
//                        }
//                    }
                    for (String sets : setList) {
                        String[] set = sets.split(" ");

                        if (set[2].equals(vote[0])) {
                            if (set[1].equals(TypeEnum.time.name())) {
                                i++;
                            }
                        }
                        if (set[2].equals(vote[1])) {
                            if (set[1].equals(TypeEnum.str.name())) {
                                i++;
                            }
                        }
                        if (set[2].equals(vote[2])) {
                            if (set[1].equals(TypeEnum.arr.name())) {
                                String[] arr = RegexCompile.splitCommaStr(set[4]);
                                if (arr.length >= 2 && arr.length <= 10)
                                    i++;
                            }
                        }
                    }
                }
            }
            if (i == vote.a) {
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    public static boolean checkEventBusiness(String str) {
        boolean flag = false;
        int i = 0;
        List<String> setList = RegexCompile.getSetList(str);
        List<String> eventList = RegexCompile.getEventList(str);
        for (String eventContent : eventList) {
            String eventName = StringUtils.substringBetween(eventContent, "event ", "(");
            if (eventName.equals(business.name())) {
                flag = true;
            }
        }
        if (flag) {
            for (String eventContent : eventList) {
                //取出方法名
                String eventName = StringUtils.substringBetween(eventContent, "event ", "(");
                if (eventName.equals(business.name())) {
                    String[] business = StringUtils.substringBetween(eventContent, "(", ")").replace(" ", "").split(",");
                    for (String sets : setList) {
                        String[] set = sets.split(" ");
                        if (set[2].equals(business[0])) {
                            if (set[1].equals(TypeEnum.coin.name())) {
                                i++;
                            }
                        }
                        if (set[2].equals(business[1])) {
                            if (set[1].equals(TypeEnum.coin.name())) {
                                i++;
                            }
                        }
                    }
                }
            }
            if (i == business.a) {
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    public static boolean checkEventToken(String str){
        boolean flag = false;
        //判断是否存在
        int i = 0;
        List<String> setList = RegexCompile.getSetList(str);
        List<String> eventList = RegexCompile.getEventList(str);
        for (String eventContent : eventList) {
            String eventName = StringUtils.substringBetween(eventContent, "event ", "(");
            if (eventName.equals(token.name())) {
                flag = true;
            }
        }
        if (flag) {
            for (String eventContent : eventList) {
                //取出方法名
                String eventName = StringUtils.substringBetween(eventContent, "event ", "(");
                if (eventName.equals(token.name())) {
                    String[] token = StringUtils.substringBetween(eventContent, "(", ")").replace(" ", "").split(",");
                    for (String sets : setList) {
                        String[] set = sets.split(" ");
                        if (set[2].equals(token[0])) {
                            if (set[1].equals(TypeEnum.integer.name())) {
                                i++;
                            }
                        }
                        if (set[2].equals(token[1])) {
                            if (set[1].equals(TypeEnum.integer.name())) {
                                i++;
                            }
                        }
                        if (set[2].equals(token[2])) {
                            if (set[1].equals(TypeEnum.str.name())) {
                                i++;
                            }
                        }
                    }
                }
            }
            if (i == token.a) {
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) {

    }
}
