package com.photon.photonchain.network.utils;

import com.alibaba.fastjson.JSON;
import com.photon.photonchain.network.ehcacheManager.UnconfirmedTranManager;
import com.photon.photonchain.storage.constants.Constants;
import com.photon.photonchain.storage.encryption.ECKey;
import com.photon.photonchain.storage.entity.UnconfirmedTran;
import com.photon.photonchain.storage.repository.TransactionRepository;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by lqh on 2018/8/9.
 */
@Component
public class CheckVerify {

    private OrtherHeap heap;
    private String bin;
    private boolean verify = false;//ture 为不满足条件

    @Autowired
    TransactionRepository transactionRepository;
    @Autowired
    UnconfirmedTranManager unconfirmedTranManager;

    public void analysisAndPushHeap() {
        verify = false;
        this.heap = new OrtherHeap();
        Map<String, Object> contractMap = JSON.parseObject(Hex.decode(bin), Map.class);//将字节码转回map

        for (int i = 1; i <= contractMap.size() - 2; i++) {
            String content = (String) contractMap.get("content" + i);//获取到每行内容
            if (content.startsWith("set")) { //set开头为全局变量 set num ab = 10 set num a;

                String[] variable = ArrayUtils.removeAllOccurences(content.split(" "), "");

                String variableName = variable[2];//变量名
                String variableType = variable[1];//分割获取变量类型

                if (variableType.equals("num")) {
                    heap.putItem(variableName.replace("\"", "").trim(), ArrayUtils.contains(variable, "=") ? variable[4] : 0); //是否包含赋值操作
                    heap.putItem(variableName.replace("\"", "").trim() + "_variable_type", variable[1]);//变量类型
                }
                if (variableType.equals("integer")) {
                    heap.putItem(variableName.replace("\"", "").trim(), ArrayUtils.contains(variable, "=") ? variable[4] : 0); //是否包含赋值操作
                    heap.putItem(variableName.replace("\"", "").trim() + "_variable_type", variable[1]);//变量类型
                }
                if (variableType.equals("str")) {
                    heap.putItem(variableName.replace("\"", "").trim(), ArrayUtils.contains(variable, "=") ? variable[4] : "");
                    heap.putItem(variableName.replace("\"", "").trim() + "_variable_type", variable[1]);//变量类型
                }
                if (variableType.equals("address")) {
                    heap.putItem(variableName.replace("\"", "").trim(), ArrayUtils.contains(variable, "=") ? variable[4] : "");
                    heap.putItem(variableName.replace("\"", "").trim() + "_variable_type", variable[1]);//变量类型
                }
                if (variableType.equals("coin")) {
                    heap.putItem(variableName.replace("\"", "").trim(), ArrayUtils.contains(variable, "=") ? variable[4] : 0);
                    heap.putItem(variableName.replace("\"", "").trim() + "Type", variable[5]);//币种类型
                    heap.putItem(variableName.replace("\"", "").trim() + "_variable_type", variable[1]);//变量类型
                }
                if (variableType.equals("time")) {
                    heap.putItem(variableName.replace("\"", "").trim(), ArrayUtils.contains(variable, "=") ? variable[4] + " " + variable[5] : "");
                    heap.putItem(variableName.replace("\"", "").trim() + "_variable_type", variable[1]);//变量类型
                }
                if (variableType.equals("arr")) {
                    heap.putItem(variableName.replace("\"", "").trim(), ArrayUtils.contains(variable, "=") ? variable[4] : "");
                    heap.putItem(variableName.replace("\"", "").trim() + "_variable_type", variable[1]);//变量类型
                }
            }
            if (content.startsWith("function")) {// 判断方法
                String methodName = StringUtils.substringBetween(content, "function", "(").trim().replace(" ", "");//获取方法名
                heap.putItem(methodName, content);//将整个方法压入堆，key为方法名
            }
            if (content.startsWith("event")) { //判断事件
                String eventName = StringUtils.substringBetween(content, "event", "(").trim().replace(" ", "");//获取事件名
                String[] evnetPar = StringUtils.substringBetween(content, "(", ")").trim().replace(" ", "").split(",");//获取事件名
                if ("business".equals(eventName)) {
                    heap.putItem("fromCoin", evnetPar[0].replace(" ", "").trim());
                    heap.putItem("toCoin", evnetPar[1].replace(" ", "").trim());
                }
                heap.putItem(eventName, content);
            }
        }
    }


    public void analysisAndGetOpcode(Map<String, Object> parmarMap) throws Exception {
        Object methodNameOne = parmarMap.get("function");//事件
        if (parmarMap.get("function") != null) {
            Object content = heap.getItem(methodNameOne.toString());//获取到要操作的整个方法 vote
            String opCode = analysis(content.toString(), parmarMap);
        }
    }

    //解析
    private String analysis(String content, Map<String, Object> parmarMap) throws Exception {
        String methodBody = getContent(content.toString(), "{", "}");//获取到方法体
        String newBody;//每一行
        while (true) {
            if (methodBody.startsWith(";")) {
                methodBody = methodBody.substring(1, methodBody.length());
            }
            newBody = StringUtils.trimToEmpty(readLine(methodBody));
            if (newBody.startsWith("function")) {
                String otherFunctionName = StringUtils.substringAfter(newBody, ".");
                Object ortherFunction = heap.getItem(StringUtils.substringBeforeLast(otherFunctionName, "("));
                if (ortherFunction == null)
                    throw new Exception("unknowFunctionException,please check you function params name!");
                String opCode = analysis(ortherFunction.toString(), parmarMap);
                if (opCode != null && !"".equals(opCode)) {
                    return opCode;
                } else {
                    String str = analysis(methodBody.split(";")[1] + ";", parmarMap);
                    return str;
                }
            }
            if (newBody.startsWith("stop")) {
                heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                verify = true;
            }
            if (newBody.startsWith("if")) {
                String[] conditionArr = StringUtils.substringBetween(newBody, "(", ")").split(" ");//获取到条件
                conditionArr = ArrayUtils.removeAllOccurences(conditionArr, "");
                String conditionOp = "";//判断条件

                if ("==".equals(conditionArr[1])) {//等等与
                    conditionOp = "==";
                } else if (">".equals(conditionArr[1])) {//大于
                    conditionOp = ">";
                } else if ("<".equals(conditionArr[1])) {//小于
                    conditionOp = "<";
                } else if ("<=".equals(conditionArr[1])) {//小于或等于
                    conditionOp = "<=";
                } else if (">=".equals(conditionArr[1])) {//大于或等于
                    conditionOp = ">=";
                } else if ("!=".equals(conditionArr[1])) {//不等于
                    conditionOp = "!=";
                }

                String[] bodyCodeArr = null;//条件里面的代码块

                if (StringUtils.contains(conditionArr[0], "count")) {//判断左边的条件是否是统计数据
                    String option = StringUtils.substringBefore(conditionArr[0], "[");//获取变量名
                    int index = Integer.valueOf(StringUtils.substringBetween(conditionArr[0], "[", "]"));//获取下标
                    Object options = heap.getItem(option);
                    if (options == null) throw new Exception("the params is not exist in the heap!");
                    //String OneOption = StringUtils.substringBetween(options.toString(), "[", "]").split(",")[index];//得到其中的选项
                    String OneOption = Constants.serialNumber[index]+"."+splitStrs(options.toString())[index];
                    int choseTime = transactionRepository.findVodesCount(parmarMap.get("contractAddress").toString(), 7, 2, OneOption);
                    List<UnconfirmedTran> tranList = unconfirmedTranManager.queryUnconfirmedTran(parmarMap.get("contractAddress").toString(), 7, "", "", -1, OneOption);
                    if (tranList.size() > 0) {
                        choseTime = choseTime + tranList.size();
                    }

                    Integer comparison = null;//对比参数
                    String regex = "^\\d+$";
                    if (!Pattern.matches(regex, conditionArr[2])) {//如果不是数字则是定义变量变量
                        comparison = Integer.valueOf(heap.getItem(conditionArr[2]).toString());
                    } else {
                        comparison = Integer.valueOf(conditionArr[2]);//对比参数
                    }
                    if ("==".equals(conditionOp)) {
                        if (choseTime == comparison) {
                            heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            verify = true;
                        }
                    }
                    if ("<".equals(conditionOp)) {
                        if (choseTime < comparison) {
                            heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            verify = true;
                        }
                    }
                    if (">".equals(conditionOp)) {
                        if (choseTime > comparison) {
                            heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            verify = true;
                        }
                    }
                    if ("<=".equals(conditionOp)) {
                        if (choseTime <= comparison) {
                            heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            verify = true;
                        }
                    }
                    if (">=".equals(conditionOp)) {
                        if (choseTime >= comparison) {
                            heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            verify = true;
                        }
                    }
                    if ("!=".equals(conditionOp)) {
                        if (choseTime != comparison) {
                            heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            verify = true;
                        }
                    }
                } else if (StringUtils.contains(conditionArr[2], "count")) {
                    String option = StringUtils.substringBefore(conditionArr[2], "[");//获取变量名
                    int index = Integer.valueOf(StringUtils.substringBetween(conditionArr[2], "[", "]"));//获取下标
                    Object options = heap.getItem(option);
                    if (options == null) throw new Exception("the params is not exist in the heap!");
                    //String OneOption = StringUtils.substringBetween(options.toString(), "[", "]").split(",")[index].replace("\"", "");//得到其中的选项
                    String OneOption = Constants.serialNumber[index]+"."+splitStrs(options.toString())[index];
                    int choseTime = transactionRepository.findVodesCount(ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()), 7, 2, OneOption);

                    Integer comparison = null;//对比参数
                    String regex = "^\\d+$";
                    if (!Pattern.matches(regex, conditionArr[0])) {//如果不是数字则是定义变量变量
                        comparison = Integer.valueOf(heap.getItem(conditionArr[0]).toString());
                    } else {
                        comparison = Integer.valueOf(conditionArr[0]);//对比参数
                    }

                    if ("==".equals(conditionOp)) {
                        if (comparison == choseTime) {
                            heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            verify = true;
                        }
                    }
                    if ("<".equals(conditionOp)) {
                        if (comparison < choseTime) {
                            heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            verify = true;
                        }
                    }
                    if (">".equals(conditionOp)) {
                        if (comparison > choseTime) {
                            heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            verify = true;
                        }
                    }
                    if ("<=".equals(conditionOp)) {
                        if (comparison <= choseTime) {
                            heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            verify = true;
                        }
                    }
                    if (">=".equals(conditionOp)) {
                        if (comparison >= choseTime) {
                            heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            verify = true;
                        }
                    }
                    if ("!=".equals(conditionOp)) {
                        if (comparison != choseTime) {
                            heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            verify = true;
                        }
                    }
                } else if (StringUtils.contains(conditionArr[0], "nowTime")) {//判断是否对比时间
                    Object endTime = null;
                    if (conditionArr[2].startsWith("\"")) {//有引号则为写死参数
                        endTime = conditionArr[2].replace("\"", "");
                    } else {//去获取变量值
                        endTime = heap.getItem(conditionArr[2]);
                    }
                    if (endTime == null) throw new Exception("error parmas don't extis in heap");
                    if (">".equals(conditionOp)) {
                        if (DateUtil.getWebTime() > DateUtil.strToDate(endTime.toString().replace("\"", "")).getTime()) {
                            heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            verify = true;
                        }
                    }
                    if ("<".equals(conditionOp)) {
                        if (DateUtil.getWebTime() < DateUtil.strToDate(endTime.toString().replace("\"", "")).getTime()) {
                            heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            verify = true;
                        }
                    }
                    if (">=".equals(conditionOp)) {
                        if (DateUtil.getWebTime() >= DateUtil.strToDate(endTime.toString().replace("\"", "")).getTime()) {
                            heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            verify = true;
                        }
                    }
                    if ("<=".equals(conditionOp)) {
                        if (DateUtil.getWebTime() <= DateUtil.strToDate(endTime.toString().replace("\"", "")).getTime()) {
                            heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            verify = true;
                        }
                    }
                    if ("!=".equals(conditionOp)) {
                        if (DateUtil.getWebTime() != DateUtil.strToDate(endTime.toString().replace("\"", "")).getTime()) {
                            heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            verify = true;
                        }
                    }
                } else if (StringUtils.contains(conditionArr[2], "nowTime")) {
                    Object endTime = null;
                    if (conditionArr[0].startsWith("\"")) {//有引号则为写死参数
                        endTime = conditionArr[0].replace("\"", "");
                    } else {//去获取变量值
                        endTime = heap.getItem(conditionArr[0]);
                    }
                    if (endTime == null) throw new Exception("error parmas don't extis in heap");
                    if (">".equals(conditionOp)) {
                        if (DateUtil.strToDate(endTime.toString().replace("\"", "")).getTime() > DateUtil.getWebTime()) {
                            heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            verify = true;
                        }
                    }
                    if ("<".equals(conditionOp)) {
                        if (DateUtil.strToDate(endTime.toString().replace("\"", "")).getTime() < DateUtil.getWebTime()) {
                            heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            verify = true;
                        }
                    }
                    if (">=".equals(conditionOp)) {
                        if (DateUtil.strToDate(endTime.toString().replace("\"", "")).getTime() >= DateUtil.getWebTime()) {
                            heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            verify = true;
                        }
                    }
                    if ("<=".equals(conditionOp)) {
                        if (DateUtil.strToDate(endTime.toString().replace("\"", "")).getTime() <= DateUtil.getWebTime()) {
                            heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            verify = true;
                        }
                    }
                    if ("!=".equals(conditionOp)) {
                        if (DateUtil.strToDate(endTime.toString().replace("\"", "")).getTime() != DateUtil.getWebTime()) {
                            heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            verify = true;
                        }
                    }
                } else {
                    String regex = "^\\d+$";
                    if ("==".equals(conditionOp)) {
                        if (!Pattern.matches(regex, conditionArr[0]) && !Pattern.matches(regex, conditionArr[2])) {//两个字符串
                            if (conditionArr[0].startsWith("\"") && conditionArr[2].startsWith("\"")) {//判断是否有双引号，有的话即为写死字符串; 两个都是写死
                                if (conditionArr[0].equals(conditionArr[2])) {
                                    heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                                    bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                                    verify = true;
                                }
                            } else if (conditionArr[0].startsWith("\"") && !conditionArr[2].startsWith("\"")) {//第一个是写死，第二个是变量
                                String conditionArr_two = heap.getItem(conditionArr[2]).toString();
                                if (conditionArr_two.equals(conditionArr[0])) {
                                    heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                                    bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                                    verify = true;
                                }
                            } else if (!conditionArr[0].startsWith("\"") && conditionArr[2].startsWith("\"")) {//第一个是变量 第二个是写死
                                String conditionArr_one = heap.getItem(conditionArr[0]).toString();
                                if (conditionArr_one.equals(conditionArr[2])) {
                                    heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                                    bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                                    verify = true;
                                }
                            } else if (!conditionArr[0].startsWith("\"") && !conditionArr[2].startsWith("\"")) {//两个都是变量
                                String conditionArr_one = heap.getItem(conditionArr[0]).toString();
                                String conditionArr_two = heap.getItem(conditionArr[2]).toString();
                                if (conditionArr_one.equals(conditionArr_two)) {
                                    heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                                    bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                                    verify = true;
                                }
                            }
                        } else if (Pattern.matches(regex, conditionArr[0]) && !Pattern.matches(regex, conditionArr[2])) { //第一个是数字 第二个是变量
                            String conditionArr_two = heap.getItem(conditionArr[2].replace("\"", "")).toString();
                            if (Integer.valueOf(conditionArr[0]) == Integer.valueOf(conditionArr_two)) {
                                heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                                bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                                verify = true;
                            }
                        } else if (!Pattern.matches(regex, conditionArr[0]) && Pattern.matches(regex, conditionArr[2])) {//第一个是变量，第二个是数字
                            String conditionArr_one = heap.getItem(conditionArr[0].replace("\"", "")).toString();
                            if (Integer.valueOf(conditionArr[2]) == Integer.valueOf(conditionArr_one)) {
                                heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                                bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                                verify = true;
                            }
                        } else { //两个都是数字
                            if (Integer.valueOf(conditionArr[0]) == Integer.valueOf(conditionArr[2])) {
                                heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                                bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                                verify = true;
                            }
                        }
                    }
                    if ("!=".equals(conditionOp)) {
                        if (!Pattern.matches(regex, conditionArr[0]) && !Pattern.matches(regex, conditionArr[2])) {//两个字符串
                            if (conditionArr[0].startsWith("\"") && conditionArr[2].startsWith("\"")) {//判断是否有双引号，有的话即为写死字符串; 两个都是写死
                                if (!conditionArr[0].equals(conditionArr[2])) {
                                    heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                                    bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                                    verify = true;
                                }
                            } else if (conditionArr[0].startsWith("\"") && !conditionArr[2].startsWith("\"")) {//第一个是写死，第二个是变量
                                String conditionArr_two = heap.getItem(conditionArr[2]).toString();
                                if (!conditionArr[0].equals(conditionArr_two)) {
                                    heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                                    bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                                    verify = true;
                                }
                            } else if (!conditionArr[0].startsWith("\"") && conditionArr[2].startsWith("\"")) {//第一个是变量 第二个是写死
                                String conditionArr_one = heap.getItem(conditionArr[0]).toString();
                                if (!conditionArr[2].equals(conditionArr_one)) {
                                    heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                                    bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                                    verify = true;
                                }
                            } else if (!conditionArr[0].startsWith("\"") && !conditionArr[2].startsWith("\"")) {//两个都是变量
                                String conditionArr_one = heap.getItem(conditionArr[0]).toString();
                                String conditionArr_two = heap.getItem(conditionArr[2]).toString();
                                if (!conditionArr_one.equals(conditionArr_two)) {
                                    heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                                    bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                                    verify = true;
                                }
                            }
                        } else if (Pattern.matches(regex, conditionArr[0]) && !Pattern.matches(regex, conditionArr[2])) { //第一个是数字 第二个是变量
                            String conditionArr_two = heap.getItem(conditionArr[2].replace("\"", "")).toString();
                            if (Integer.valueOf(conditionArr[0]) != Integer.valueOf(conditionArr_two)) {
                                heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                                bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                                verify = true;
                            }
                        } else if (!Pattern.matches(regex, conditionArr[0]) && Pattern.matches(regex, conditionArr[2])) {//第一个是变量，第二个是数字
                            String conditionArr_one = heap.getItem(conditionArr[0].replace("\"", "")).toString();
                            if (Integer.valueOf(conditionArr[2]) != Integer.valueOf(conditionArr_one)) {
                                heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                                bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                                verify = true;
                            }
                        } else {
                            if (Integer.valueOf(conditionArr[0]) != Integer.valueOf(conditionArr[2])) {//两个都是数字
                                heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                                bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                                verify = true;
                            }
                        }
                    }
                    String conditionArr_one = "";
                    String conditionArr_two = "";
                    if (!Pattern.matches(regex, conditionArr[0]) && !Pattern.matches(regex, conditionArr[2])) {//两边都是变量的
                        conditionArr_one = heap.getItem(conditionArr[0]).toString();
                        conditionArr_two = heap.getItem(conditionArr[2]).toString();
                    } else if (Pattern.matches(regex, conditionArr[0]) && !Pattern.matches(regex, conditionArr[2])) {//左边是写死，右边是变量的
                        conditionArr_one = conditionArr[0].toString();
                        conditionArr_two = heap.getItem(conditionArr[2]).toString();
                    } else if (!Pattern.matches(regex, conditionArr[0]) && Pattern.matches(regex, conditionArr[2])) { //左边是变量，右边是写死的
                        conditionArr_one = heap.getItem(conditionArr[0]).toString();
                        conditionArr_two = conditionArr[2].toString();
                    } else {//两边都是写死的
                        conditionArr_one = conditionArr[0].toString();
                        conditionArr_two = conditionArr[2].toString();
                    }
                    if ("<".equals(conditionOp)) {
                        if (Integer.valueOf(conditionArr_one) < Integer.valueOf(conditionArr_two)) {
                            heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            verify = true;
                        }
                    }
                    if (">".equals(conditionOp)) {
                        if (Integer.valueOf(conditionArr_one) > Integer.valueOf(conditionArr_two)) {
                            heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            verify = true;
                        }
                    }
                    if (">=".equals(conditionOp)) {
                        if (Integer.valueOf(conditionArr_one) >= Integer.valueOf(conditionArr_two)) {
                            heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            verify = true;
                        }
                    }
                    if ("<=".equals(conditionOp)) {
                        if (Integer.valueOf(conditionArr_one) <= Integer.valueOf(conditionArr_two)) {
                            heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            verify = true;
                        }
                    }
                }
            }
            methodBody = methodBody.substring(newBody.length(), methodBody.length());
            if (methodBody.length() == 0)
                break;
        }
        return "";
    }


    //读行
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

    public static String[] splitStrs(String str) {
//        [0,","];
        Pattern p = Pattern.compile("(\"(?<=\").*?(?=\")\")");
        Matcher m = p.matcher(str);
        List<String> list = new ArrayList<>();
        while (m.find()) {
            list.add(m.group());
        }
        for(int i=0;i< list.size();i++){
            str = str.replace(list.get(i),"!_"+i);
        }
        String arrays[] = str.split(",");
        for(String s :arrays){
            if(!s.contains("!_")) list.add(s);
        }
        String[] array = list.toArray(new String[list.size()]);
        return array;
    }


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

    public static int statistics(String str, String delimiter) {
        int count = 0;
        int index = -1;
        while ((index = str.indexOf(delimiter)) != -1) {
            str = str.substring(index + delimiter.length());
            count++;
        }
        return count;
    }

    //-------------------------get set---------------------------------


    public boolean getVerify() {
        return verify;
    }

    public void setVerify(boolean verify) {
        this.verify = verify;
    }

    public OrtherHeap getHeap() {
        return heap;
    }

    public void setHeap(OrtherHeap heap) {
        this.heap = heap;
    }

    public String getBin() {
        return bin;
    }

    public void setBin(String bin) {
        this.bin = bin;
    }


}
