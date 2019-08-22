package com.photon.photonchain.extend.vm;

import com.alibaba.fastjson.JSON;
import com.photon.photonchain.extend.Universal.EventEnum;
import com.photon.photonchain.extend.Universal.OpCodeEnum;
import com.photon.photonchain.extend.Universal.TypeEnum;
import com.photon.photonchain.extend.compiler.Pcompiler;
import com.photon.photonchain.network.ehcacheManager.UnconfirmedTranManager;
import com.photon.photonchain.network.utils.DateUtil;
import com.photon.photonchain.storage.constants.Constants;
import com.photon.photonchain.storage.encryption.ECKey;
import com.photon.photonchain.storage.entity.UnconfirmedTran;
import com.photon.photonchain.storage.repository.TransactionRepository;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author:Lin
 * @Description:
 * @Date:16:50 2018/4/11
 * @Modified by:
 */
@Component
public class Program extends ClassLoader {

    private static Logger logger = LoggerFactory.getLogger(Program.class);

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UnconfirmedTranManager unconfirmedTranManager;


    private Stack stack;
    private Heap heap;
    private List<String> ops;
    private String bin;
    private String version;
    private String contractName;
    private String contractContent;
    private Object result;
    private Map<String, Object> map;

    private boolean verify = false;//ture 为不满足条件



    /**
     * 分析字节码并将方法变量压入堆
     */
    public void analysisAndPushHeap() {
        this.heap = new Heap();
        Map<String, Object> contractMap = JSON.parseObject(Hex.decode(bin), Map.class);//将字节码转回map

        this.version = (String) contractMap.get("version");//版本号
        this.contractName = (String) contractMap.get("contract"); //获取合同名称

        for (int i = 1; i <= contractMap.size() - 2; i++) {
            String content = (String) contractMap.get("content" + i);//获取到每行内容
            logger.info("合约bin解析内容[{}]", content);
            if (content.startsWith("set")) { //set开头为全局变量 set num ab = 10 set num a;

                String[] variable = ArrayUtils.removeAllOccurences(content.split(" "), "");

                String variableName = variable[2];//变量名
                String variableType = variable[1];//分割获取变量类型

                if (variableType.equals(TypeEnum.num.name())) {
                    heap.putItem(variableName.replace("\"", "").trim(), ArrayUtils.contains(variable, "=") ? variable[4] : 0); //是否包含赋值操作
                    heap.putItem(variableName.replace("\"", "").trim()+"_variable_type", variable[1]);//变量类型
                    logger.info("堆KEY[{}]VALUE[{}]", variableName.replace("\"", "").trim(), ArrayUtils.contains(variable, "=") ? variable[4] : 0);
                }
                if (variableType.equals(TypeEnum.integer.name())) {
                    heap.putItem(variableName.replace("\"", "").trim(), ArrayUtils.contains(variable, "=") ? variable[4] : 0); //是否包含赋值操作
                    heap.putItem(variableName.replace("\"", "").trim()+"_variable_type", variable[1]);//变量类型
                    logger.info("堆KEY[{}]VALUE[{}]", variableName.replace("\"", "").trim(), ArrayUtils.contains(variable, "=") ? variable[4] : 0);
                }
                if (variableType.equals(TypeEnum.str.name())) {
                    heap.putItem(variableName.replace("\"", "").trim(), ArrayUtils.contains(variable, "=") ? variable[4] : "");
                    heap.putItem(variableName.replace("\"", "").trim()+"_variable_type", variable[1]);//变量类型
                    logger.info("堆KEY[{}]VALUE[{}]", variableName.replace("\"", "").trim(), ArrayUtils.contains(variable, "=") ? variable[4] : "");
                }
                if (variableType.equals(TypeEnum.address.name())) {
                    heap.putItem(variableName.replace("\"", "").trim(), ArrayUtils.contains(variable, "=") ? variable[4] : "");
                    heap.putItem(variableName.replace("\"", "").trim()+"_variable_type", variable[1]);//变量类型
                    logger.info("堆KEY[{}]VALUE[{}]", variableName.replace("\"", "").trim(), ArrayUtils.contains(variable, "=") ? variable[4] : "");
                }
                if (variableType.equals(TypeEnum.coin.name())) {
                    heap.putItem(variableName.replace("\"", "").trim(), ArrayUtils.contains(variable, "=") ? variable[4] : 0);
                    heap.putItem(variableName.replace("\"", "").trim() + "Type", variable[5]);//币种类型
                    heap.putItem(variableName.replace("\"", "").trim()+"_variable_type", variable[1]);//变量类型
                    logger.info("堆KEY[{}]VALUE[{}]", variableName.replace("\"", "").trim(), ArrayUtils.contains(variable, "=") ? variable[4] : 0);
                    logger.info("堆KEY[{}]VALUE[{}]", variableName.replace("\"", "").trim() + "Type", variable[5]);
                }
                if (variableType.equals(TypeEnum.time.name())) {
                    heap.putItem(variableName.replace("\"", "").trim(), ArrayUtils.contains(variable, "=") ? variable[4] + " " + variable[5] : "");
                    heap.putItem(variableName.replace("\"", "").trim()+"_variable_type", variable[1]);//变量类型
                }
                if (variableType.equals(TypeEnum.arr.name())) {
                    heap.putItem(variableName.replace("\"", "").trim(), ArrayUtils.contains(variable, "=") ? variable[4] : "");
                    heap.putItem(variableName.replace("\"", "").trim()+"_variable_type", variable[1]);//变量类型
                }
            }
            if (content.startsWith("function")) {// 判断方法
                String methodName = StringUtils.substringBetween(content, "function", "(").trim().replace(" ", "");//获取方法名
                heap.putItem(methodName, content);//将整个方法压入堆，key为方法名
                logger.info("堆KEY[{}]VALUE[{}]", methodName, content);
            }
            if (content.startsWith("event")) { //判断事件
                String eventName = StringUtils.substringBetween(content, "event", "(").trim().replace(" ", "");//获取事件名
                String[] evnetPar = StringUtils.substringBetween(content, "(", ")").trim().replace(" ", "").split(",");//获取事件名
                if ("business".equals(eventName)) {
                    heap.putItem("fromCoin", evnetPar[0].replace(" ", "").trim());
                    heap.putItem("toCoin", evnetPar[1].replace(" ", "").trim());
                    logger.info("堆KEY[{}]VALUE[{}]", "fromCoin", evnetPar[0]);
                    logger.info("堆KEY[{}]VALUE[{}]", "toCoin", evnetPar[1]);
                }
                heap.putItem(eventName, content);
            }
        }
    }


    /**
     * 根据参数调用方法获取操作码
     */
    public void analysisAndGetOpcode(Map<String, Object> parmarMap) throws Exception {
        this.ops = new LinkedList<>();
        Object methodName = parmarMap.get("function");
        Object eventName = parmarMap.get("event");//事件
        if (eventName != null) {
            if (EventEnum.business.name().equals(eventName.toString())) {
                methodName = "exchange";
            }
            if ("cancel".equals(eventName.toString())) {
                methodName = "cancel";
            }
        }
        if (eventName == null) {
            if (methodName == null) throw new Exception("error parmas about function");
        }

        if (methodName != null) {
            for (String methodNameOne : methodName.toString().split(",")) {//多个方法,如init,guadan
                Object content = heap.getItem(methodNameOne);//获取到要操作的整个方法
                if ("".equals(content) || content == null)
                    throw new Exception("unknowFunctionException,please check you function params name!");
                String opCode = analysis(content.toString(), parmarMap);
                String[] opsList = opCode.split(",");
                for (String op : opsList)
                    ops.add(op);
                break;
            }
        } else if (eventName != null) {
            for (String eventNameOne : eventName.toString().split(",")) {
                Object content = heap.getItem(eventNameOne.toLowerCase());//获取到要操作的事件
                String eventParmas = StringUtils.substringBetween(content.toString(), "(", ")").trim().replace(" ", "");//获取事件参数
                pushVariableInTheHeap(parmarMap, eventParmas);
                String opCode = getOpCode(("tokenEvent," + eventParmas).split(","));//获取到操作码
                String[] opsList = opCode.split(",");
                for (String op : opsList)
                    ops.add(op);
                break;
            }
        }
    }


    /**
     * 解析
     */
    private String analysis(String content, Map<String, Object> parmarMap) throws Exception {
        String methodBody = Pcompiler.getContent(content.toString(), "{", "}");//获取到方法体
        String methodParmas = StringUtils.substringBetween(content.toString(), "(", ")");//获取方法参数
        String newBody;//每一行
        while (true) {
            if (methodBody.startsWith(";")) {
                methodBody = methodBody.substring(1, methodBody.length());
            }
            newBody = StringUtils.trimToEmpty(Pcompiler.readLine(methodBody));
            if (newBody.startsWith("function")) {
                String otherFunctionName = StringUtils.substringAfter(newBody, ".");
                Object ortherFunction = heap.getItem(StringUtils.substringBeforeLast(otherFunctionName, "("));
                if (ortherFunction == null)
                    throw new Exception("unknowFunctionException,please check you function params name!");
                String opCode = analysis(ortherFunction.toString(), parmarMap);
                if (opCode != null && !"".equals(opCode)) {
                    return opCode;
                } else {
                    pushVariableInTheHeap(parmarMap, methodParmas);
                    String str = analysis(methodBody.split(";")[1] + ";", parmarMap);
                    return str;
                }
            }
            if (newBody.startsWith("stop")) {
                heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                String opCode = getOpCode(newBody.split(";"));
                return opCode;
            }
            if (newBody.startsWith("if")) {
                String[] conditionArr = StringUtils.substringBetween(newBody, "(", ")").split(" ");//获取到条件
                conditionArr = ArrayUtils.removeAllOccurences(conditionArr, "");
                String conditionOp = "";//判断条件

                if (OpCodeEnum.EQ.key.equals(conditionArr[1])) {//等等与
                    conditionOp = "==";
                } else if (OpCodeEnum.GT.key.equals(conditionArr[1])) {//大于
                    conditionOp = ">";
                } else if (OpCodeEnum.LT.key.equals(conditionArr[1])) {//小于
                    conditionOp = "<";
                } else if (OpCodeEnum.LTEQ.key.equals(conditionArr[1])) {//小于或等于
                    conditionOp = "<=";
                } else if (OpCodeEnum.GTEQ.key.equals(conditionArr[1])) {//大于或等于
                    conditionOp = ">=";
                } else if (OpCodeEnum.NOT.key.equals(conditionArr[1])) {//不等于
                    conditionOp = "!=";
                }

                String[] bodyCodeArr = null;//条件里面的代码块

                if (StringUtils.contains(conditionArr[0], "count")) {//判断左边的条件是否是统计数据
                    String option = StringUtils.substringBefore(conditionArr[0], "[");//获取变量名
                    int index = Integer.valueOf(StringUtils.substringBetween(conditionArr[0], "[", "]"));//获取下标
                    Object options = heap.getItem(option);

                    if (options == null) throw new Exception("the params is not exist in the heap!");
                    String OneOption = Constants.serialNumber[index]+"."+splitStrs(options.toString())[index];
                    int choseTime = transactionRepository.findVodesCount(ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()), 7, 2, OneOption);

                    Integer comparison = null;//对比参数
                    String regex = "^\\d+$";
                    if (!Pattern.matches(regex, conditionArr[2])) {//如果不是数字则是定义变量变量
                        comparison = Integer.valueOf(heap.getItem(conditionArr[2]).toString());
                    } else {
                        comparison = Integer.valueOf(conditionArr[2]);//对比参数
                    }
                    if ("==".equals(conditionOp)) {
                        if (choseTime == comparison) {
                            if(comparison == 0){
                                heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            }
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            String opCode = getOpCode(bodyCodeArr);//获取到操作码
                            verify = true;
                            return opCode;
                        }
                    }
                    if ("<".equals(conditionOp)) {
                        if (choseTime < comparison) {
                            if(comparison == 0){
                                heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            }
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            String opCode = getOpCode(bodyCodeArr);//获取到操作码
                            verify = true;
                            return opCode;
                        }
                    }
                    if (">".equals(conditionOp)) {
                        if (choseTime > comparison) {
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            String opCode = getOpCode(bodyCodeArr);//获取到操作码
                            verify = true;
                            return opCode;
                        }
                    }
                    if ("<=".equals(conditionOp)) {
                        if (choseTime <= comparison) {
                            if(comparison == 0){
                                heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            }
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            String opCode = getOpCode(bodyCodeArr);//获取到操作码
                            verify = true;
                            return opCode;
                        }
                    }
                    if (">=".equals(conditionOp)) {
                        if (choseTime >= comparison) {
                            if(comparison == 0){
                                heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            }
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            String opCode = getOpCode(bodyCodeArr);//获取到操作码
                            verify = true;
                            return opCode;
                        }
                    }
                    if ("!=".equals(conditionOp)) {
                        if (choseTime != comparison) {
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            String opCode = getOpCode(bodyCodeArr);//获取到操作码
                            verify = true;
                            return opCode;
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
                            if(comparison == 0){
                                heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            }
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            String opCode = getOpCode(bodyCodeArr);//获取到操作码
                            verify = true;
                            return opCode;
                        }
                    }
                    if ("<".equals(conditionOp)) {
                        if (comparison < choseTime) {
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            String opCode = getOpCode(bodyCodeArr);//获取到操作码
                            verify = true;
                            return opCode;
                        }
                    }
                    if (">".equals(conditionOp)) {
                        if (comparison > choseTime) {
                            if(comparison == 0){
                                heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            }
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            String opCode = getOpCode(bodyCodeArr);//获取到操作码
                            verify = true;
                            return opCode;
                        }
                    }
                    if ("<=".equals(conditionOp)) {
                        if (comparison <= choseTime) {
                            if(comparison == 0){
                                heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            }
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            String opCode = getOpCode(bodyCodeArr);//获取到操作码
                            verify = true;
                            return opCode;
                        }
                    }
                    if (">=".equals(conditionOp)) {
                        if (comparison >= choseTime) {
                            if(comparison == 0){
                                heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            }
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            String opCode = getOpCode(bodyCodeArr);//获取到操作码
                            verify = true;
                            return opCode;
                        }
                    }
                    if ("!=".equals(conditionOp)) {
                        if (comparison != choseTime) {
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            String opCode = getOpCode(bodyCodeArr);//获取到操作码
                            verify = true;
                            return opCode;
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
                            String opCode = getOpCode(bodyCodeArr);//获取到操作码
                            verify = true;
                            return opCode;
                        }
                    }
                    if ("<".equals(conditionOp)) {
                        if (DateUtil.getWebTime() < DateUtil.strToDate(endTime.toString().replace("\"", "")).getTime()) {
                            heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            String opCode = getOpCode(bodyCodeArr);//获取到操作码
                            verify = true;
                            return opCode;
                        }
                    }
                    if (">=".equals(conditionOp)) {
                        if (DateUtil.getWebTime() >= DateUtil.strToDate(endTime.toString().replace("\"", "")).getTime()) {
                            heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            String opCode = getOpCode(bodyCodeArr);//获取到操作码
                            verify = true;
                            return opCode;
                        }
                    }
                    if ("<=".equals(conditionOp)) {
                        if (DateUtil.getWebTime() <= DateUtil.strToDate(endTime.toString().replace("\"", "")).getTime()) {
                            heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            String opCode = getOpCode(bodyCodeArr);//获取到操作码
                            verify = true;
                            return opCode;
                        }
                    }
                    if ("!=".equals(conditionOp)) {
                        if (DateUtil.getWebTime() != DateUtil.strToDate(endTime.toString().replace("\"", "")).getTime()) {
                            heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            String opCode = getOpCode(bodyCodeArr);//获取到操作码
                            verify = true;
                            return opCode;
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
                            String opCode = getOpCode(bodyCodeArr);//获取到操作码
                            verify = true;
                            return opCode;
                        }
                    }
                    if ("<".equals(conditionOp)) {
                        if (DateUtil.strToDate(endTime.toString().replace("\"", "")).getTime() < DateUtil.getWebTime()) {
                            heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            String opCode = getOpCode(bodyCodeArr);//获取到操作码
                            verify = true;
                            return opCode;
                        }
                    }
                    if (">=".equals(conditionOp)) {
                        if (DateUtil.strToDate(endTime.toString().replace("\"", "")).getTime() >= DateUtil.getWebTime()) {
                            heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            String opCode = getOpCode(bodyCodeArr);//获取到操作码
                            verify = true;
                            return opCode;
                        }
                    }
                    if ("<=".equals(conditionOp)) {
                        if (DateUtil.strToDate(endTime.toString().replace("\"", "")).getTime() <= DateUtil.getWebTime()) {
                            heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            String opCode = getOpCode(bodyCodeArr);//获取到操作码
                            verify = true;
                            return opCode;
                        }
                    }
                    if ("!=".equals(conditionOp)) {
                        if (DateUtil.strToDate(endTime.toString().replace("\"", "")).getTime() != DateUtil.getWebTime()) {
                            heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            String opCode = getOpCode(bodyCodeArr);//获取到操作码
                            verify = true;
                            return opCode;
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
                                    String opCode = getOpCode(bodyCodeArr);//获取到操作码
                                    verify = true;
                                    return opCode;
                                }
                            } else if (conditionArr[0].startsWith("\"") && !conditionArr[2].startsWith("\"")) {//第一个是写死，第二个是变量
                                String conditionArr_two = heap.getItem(conditionArr[2]).toString();
                                if (conditionArr_two.equals(conditionArr[0])) {
                                    heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                                    bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                                    String opCode = getOpCode(bodyCodeArr);//获取到操作码
                                    verify = true;
                                    return opCode;
                                }
                            } else if (!conditionArr[0].startsWith("\"") && conditionArr[2].startsWith("\"")) {//第一个是变量 第二个是写死
                                String conditionArr_one = heap.getItem(conditionArr[0]).toString();
                                if (conditionArr_one.equals(conditionArr[2])) {
                                    heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                                    bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                                    String opCode = getOpCode(bodyCodeArr);//获取到操作码
                                    verify = true;
                                    return opCode;
                                }
                            } else if (!conditionArr[0].startsWith("\"") && !conditionArr[2].startsWith("\"")) {//两个都是变量
                                String conditionArr_one = heap.getItem(conditionArr[0]).toString();
                                String conditionArr_two = heap.getItem(conditionArr[2]).toString();
                                if (conditionArr_one.equals(conditionArr_two)) {
                                    heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                                    bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                                    String opCode = getOpCode(bodyCodeArr);//获取到操作码
                                    verify = true;
                                    return opCode;
                                }
                            }
                        } else if (Pattern.matches(regex, conditionArr[0]) && !Pattern.matches(regex, conditionArr[2])) { //第一个是数字 第二个是变量
                            String conditionArr_two = heap.getItem(conditionArr[2].replace("\"", "")).toString();
                            if (Integer.valueOf(conditionArr[0]) == Integer.valueOf(conditionArr_two)) {
                                heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                                bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                                String opCode = getOpCode(bodyCodeArr);//获取到操作码
                                verify = true;
                                return opCode;
                            }
                        } else if (!Pattern.matches(regex, conditionArr[0]) && Pattern.matches(regex, conditionArr[2])) {//第一个是变量，第二个是数字
                            String conditionArr_one = heap.getItem(conditionArr[0].replace("\"", "")).toString();
                            if (Integer.valueOf(conditionArr[2]) == Integer.valueOf(conditionArr_one)) {
                                heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                                bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                                String opCode = getOpCode(bodyCodeArr);//获取到操作码
                                verify = true;
                                return opCode;
                            }
                        } else { //两个都是数字
                            if (Integer.valueOf(conditionArr[0]) == Integer.valueOf(conditionArr[2])) {
                                heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                                bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                                String opCode = getOpCode(bodyCodeArr);//获取到操作码
                                verify = true;
                                return opCode;
                            }
                        }
                    }
                    if ("!=".equals(conditionOp)) {
                        if (!Pattern.matches(regex, conditionArr[0]) && !Pattern.matches(regex, conditionArr[2])) {//两个字符串
                            if (conditionArr[0].startsWith("\"") && conditionArr[2].startsWith("\"")) {//判断是否有双引号，有的话即为写死字符串; 两个都是写死
                                if (!conditionArr[0].equals(conditionArr[2])) {
                                    heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                                    bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                                    String opCode = getOpCode(bodyCodeArr);//获取到操作码
                                    verify = true;
                                    return opCode;
                                }
                            } else if (conditionArr[0].startsWith("\"") && !conditionArr[2].startsWith("\"")) {//第一个是写死，第二个是变量
                                String conditionArr_two = heap.getItem(conditionArr[2]).toString();
                                if (!conditionArr[0].equals(conditionArr_two)) {
                                    heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                                    bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                                    String opCode = getOpCode(bodyCodeArr);//获取到操作码
                                    verify = true;
                                    return opCode;
                                }
                            } else if (!conditionArr[0].startsWith("\"") && conditionArr[2].startsWith("\"")) {//第一个是变量 第二个是写死
                                String conditionArr_one = heap.getItem(conditionArr[0]).toString();
                                if (!conditionArr[2].equals(conditionArr_one)) {
                                    heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                                    bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                                    String opCode = getOpCode(bodyCodeArr);//获取到操作码
                                    verify = true;
                                    return opCode;
                                }
                            } else if (!conditionArr[0].startsWith("\"") && !conditionArr[2].startsWith("\"")) {//两个都是变量
                                String conditionArr_one = heap.getItem(conditionArr[0]).toString();
                                String conditionArr_two = heap.getItem(conditionArr[2]).toString();
                                if (!conditionArr_one.equals(conditionArr_two)) {
                                    heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                                    bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                                    String opCode = getOpCode(bodyCodeArr);//获取到操作码
                                    verify = true;
                                    return opCode;
                                }
                            }
                        } else if (Pattern.matches(regex, conditionArr[0]) && !Pattern.matches(regex, conditionArr[2])) { //第一个是数字 第二个是变量
                            String conditionArr_two = heap.getItem(conditionArr[2].replace("\"", "")).toString();
                            if (Integer.valueOf(conditionArr[0]) != Integer.valueOf(conditionArr_two)) {
                                heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                                bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                                String opCode = getOpCode(bodyCodeArr);//获取到操作码
                                verify = true;
                                return opCode;
                            }
                        } else if (!Pattern.matches(regex, conditionArr[0]) && Pattern.matches(regex, conditionArr[2])) {//第一个是变量，第二个是数字
                            String conditionArr_one = heap.getItem(conditionArr[0].replace("\"", "")).toString();
                            if (Integer.valueOf(conditionArr[2]) != Integer.valueOf(conditionArr_one)) {
                                heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                                bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                                String opCode = getOpCode(bodyCodeArr);//获取到操作码
                                verify = true;
                                return opCode;
                            }
                        } else {
                            if (Integer.valueOf(conditionArr[0]) != Integer.valueOf(conditionArr[2])) {//两个都是数字
                                heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                                bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                                String opCode = getOpCode(bodyCodeArr);//获取到操作码
                                verify = true;
                                return opCode;
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
                            String opCode = getOpCode(bodyCodeArr);//获取到操作码
                            verify = true;
                            return opCode;
                        }
                    }
                    if (">".equals(conditionOp)) {
                        if (Integer.valueOf(conditionArr_one) > Integer.valueOf(conditionArr_two)) {
                            heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            String opCode = getOpCode(bodyCodeArr);//获取到操作码
                            verify = true;
                            return opCode;
                        }
                    }
                    if (">=".equals(conditionOp)) {
                        if (Integer.valueOf(conditionArr_one) >= Integer.valueOf(conditionArr_two)) {
                            heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            String opCode = getOpCode(bodyCodeArr);//获取到操作码
                            verify = true;
                            return opCode;
                        }
                    }
                    if ("<=".equals(conditionOp)) {
                        if (Integer.valueOf(conditionArr_one) <= Integer.valueOf(conditionArr_two)) {
                            heap.putItem("contractAddress", ECKey.pubkeyToAddress(parmarMap.get("contractAddress").toString()));
                            bodyCodeArr = StringUtils.substringBetween(newBody, "{", "}").split(";");//获取方法里面的代码块
                            String opCode = getOpCode(bodyCodeArr);//获取到操作码
                            verify = true;
                            return opCode;
                        }
                    }
                }
            } else if (newBody.startsWith("set")) {//是否还有其他变量
                heap.putItem(newBody.split(" ")[2].replace("\"", "").trim(), newBody.split(" ")[4]);
            } else if (newBody.startsWith("make")) {
                String opCode = getOpCode(methodBody.split(";"));//获取到操作码
                return opCode;
            } else {//没有任何开头，直接执行方法体
                pushVariableInTheHeap(parmarMap, methodParmas);
                String opCode = getOpCode(methodBody.split(";"));//获取到操作码
                return opCode;
            }
            methodBody = methodBody.substring(newBody.length(), methodBody.length());
            if (methodBody.length() == 0)
                break;
        }
        return "";
    }


    /**
     * 将变量参数压入堆
     */

    public void pushVariableInTheHeap(Map<String, Object> parmarMap, String methodParmas) throws Exception {
        String[] methodParmasArr = methodParmas.split(",");
        for (int i = 0; i < methodParmasArr.length; i++) { // {num a,num b,str operator}
            String[] oneParmas = ArrayUtils.removeAllOccurences(methodParmasArr[i].split(" "), "");
            Object parmas = null;
            if (oneParmas.length >= 2) {
                parmas = heap.getItem(oneParmas[1]);
            } else {
                parmas = heap.getItem(oneParmas[0]);
            }
            if (parmas == null) {
                Object paramas = null;
                if (oneParmas.length >= 2) {
                    paramas = parmarMap.get(oneParmas[1]);//变量名 a
                } else {
                    paramas = parmarMap.get(oneParmas[0]);
                }
                if (paramas == null) throw new Exception("you params is un incomplete,check you params");
                if (oneParmas.length >= 2) {
                    if ("str".equals(oneParmas[0])) {
                        heap.putItem(oneParmas[1], paramas.toString());
                    }
                    if ("address".equals(oneParmas[0])) {
                        heap.putItem(oneParmas[1], paramas.toString());
                    }
                    if ("num".equals(oneParmas[0])) {
                        heap.putItem(oneParmas[1], new BigDecimal(paramas.toString()));
                    }
                    if ("coin".equals(oneParmas[0])) {
                        heap.putItem(oneParmas[1], new BigDecimal(paramas.toString()));
                    }
                } else {
                    heap.putItem(oneParmas[0], paramas.toString());
                }
            } else {
                if (parmas instanceof String) {
                    if ("".equals(parmas)) {
                        Object paramas = parmarMap.get(oneParmas[1]);//变量名 a
                        if (paramas == null) throw new Exception("you params is un incomplete,check you params");
                        if ("str".equals(oneParmas[0])) {
                            heap.putItem(oneParmas[1], paramas.toString());
                        }
                        if ("address".equals(oneParmas[0])) {
                            heap.putItem(oneParmas[1], paramas.toString());
                        }
                    }
                }
                if (parmas instanceof BigDecimal) {
                    Object paramas = parmarMap.get(oneParmas[1]);//变量名 a
                    if (paramas == null) throw new Exception("you params is un incomplete,check you params");
                    if ("num".equals(oneParmas[0])) {
                        heap.putItem(oneParmas[1], new BigDecimal(paramas.toString()));
                    }
                    if ("coin".equals(oneParmas[0])) {
                        heap.putItem(oneParmas[1], new BigDecimal(paramas.toString()));
                    }
                }
            }
        }

        //将传入未定义参数存入堆
        for (String key : parmarMap.keySet()) {
            Object Exparmas = heap.getItem(key);//判断传入参数是否是合约定义参数
            if (Exparmas == null) {
                //存入堆
                heap.putItem(key, parmarMap.get(key));
            }
        }
    }


    /**
     * 获取操作码
     *
     * @return {o:push;v:1;o:push;v:2;o:add}
     * @parma:{bodyCode:"方法体每行代码"}
     */
    public String getOpCode(String[] bodyCodeArr) throws Exception {
        this.stack = new Stack();
        StringBuffer opCode = new StringBuffer();
        StringBuffer ops = new StringBuffer();
        if (bodyCodeArr != null && !bodyCodeArr[0].startsWith("tokenEvent")) {
            for (int i = 0; i < bodyCodeArr.length; i++) {
                if (bodyCodeArr[i].startsWith("trans", 0)) {//判断是不是交易事件
                    int k = 1;
                    String[] eventParmasArr = StringUtils.substringBetween(bodyCodeArr[i], "(", ")").split(",");//获取事件参数
                    for (String eventParmas : eventParmasArr) {
                        Object code = heap.getItem(eventParmas.trim());
                        if (code == null) throw new Exception("the variable does not exist in memory");
                        stack.push(code);
                        setStack(stack);
                        if (k == 3) {
                            stack.push(eventParmas.trim());//a
                            setStack(stack);
                        }
                        k++;
                    }
                    opCode.append("trans" + ",");
                } else if (bodyCodeArr[i].startsWith("make", 0)) {//判断是不是投票方法
                    if (heap.getItem("chose") != null) { //做出的选择
                        stack.push(heap.getItem("chose").toString());
                        stack.push(heap.getItem("fromAddress"));
                        String function = heap.getItem("vote").toString();
                        String parmas = StringUtils.substringBetween(function,"(",")");
                        String options = parmas.split(",")[2].replace(" ","");
                        stack.push(splitStrs(heap.getItem(options).toString()).length);
                        stack.push(heap.getItem("bin").toString());
                    }
                    opCode.append("VOTE" + ",");
                } else if (bodyCodeArr[i].startsWith("stop", 0)) {
                    stack.push(heap.getItem("contractAddress"));
                    opCode.append("STOP" + ",");
                } else {
                    String doWhat = "";//return后面的操作
                    String returnStr = "";
                    if (bodyCodeArr[i].startsWith("return", 0)) {
                        String[] resultArr = bodyCodeArr[i].split(" ");
                        ops.append("return");
                        returnStr = resultArr[0];
                        doWhat = resultArr[1];
                    }
                    String[] lineCodeArr = null;
                    if (!"".equals(doWhat)) {
                        lineCodeArr = doWhat.split("");
                    } else {
                        lineCodeArr = bodyCodeArr[i].split("");//return a+b;
                    }
                    for (String str : lineCodeArr) {
                        Object code = heap.getItem(str.trim());
                        if (code == null) {
                            if (!"+".equals(str) && !"-".equals(str) && !"*".equals(str) && !"/".equals(str) && !"return".equals(str)) {//判断是不是运算符
                                throw new Exception("the variable does not exist in memory");
                            } else {
                                switch (str) {
                                    case "+":
                                        opCode.append(OpCodeEnum.ADD + ",");
                                        break;
                                    case "-":
                                        opCode.append(OpCodeEnum.SUB + ",");
                                        break;
                                    case "*":
                                        opCode.append(OpCodeEnum.MUL + ",");
                                        break;
                                    case "/":
                                        opCode.append(OpCodeEnum.DIV + ",");
                                        break;
                                }
                            }
                        } else {
                            stack.push(code);
                            setStack(stack);
                        }
                    }
                    opCode.append(ops.toString());
                    return opCode.toString();
                }
            }
        } else {
            //判断是不是投票事件 或者是代币事件
            Object event = heap.getItem("event");
            if (event != null) {
                //Object eventName = heap.getItem(event.toString());
                if (event.toString().equals(OpCodeEnum.VOTE.toString())) {
                    opCode.append(OpCodeEnum.VOTE);
                    stack.push(heap.getItem("chose").toString().replace("\"",""));//做的选择
                    stack.push(heap.getItem("fromAddress").toString());
                }
                if (event.toString().equals(OpCodeEnum.TOKEN.toString())) {
                    opCode.append(OpCodeEnum.TOKEN);
                    stack.push(heap.getItem("bin").toString());//合约bin
                    stack.push(heap.getItem("fromAddress").toString());//公钥
                    stack.push(heap.getItem(bodyCodeArr[1]).toString());//位数
                    stack.push(heap.getItem(bodyCodeArr[2]).toString());//发行数量
                    stack.push(heap.getItem(bodyCodeArr[3]).toString().replace("\"", ""));//代币名称
                }
            }
        }
        if (heap.getItem("contractAddress") != null) {
            stack.push(heap.getItem("contractAddress").toString());
        }
        return opCode.toString();
    }



    public static String[] splitStrs(String str) {
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


    /**
     * 其他合约编译
     */
    public Class loadClass(String bin) {
        byte[] code = Hex.decode(bin);
        Class c1 = defineClass(code, 0, code.length);
        return c1;
    }


    /*-------------------------get set---------------------------*/

    public Program() {
    }

    public Program(String bin) {
        this.bin = bin;
    }

    public Stack getStack() {
        return stack;
    }

    public Heap getHeap() {
        return heap;
    }

    public String getBin() {
        return bin;
    }

    public String getVersion() {
        return version;
    }

    public String getContractContent() {
        return contractContent;
    }

    public void setStack(Stack stack) {
        this.stack = stack;
    }

    public void setHeap(Heap heap) {
        this.heap = heap;
    }

    public void setBin(String bin) {
        this.bin = bin;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setContractContent(String contractContent) {
        this.contractContent = contractContent;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public List<String> getOps() {
        return ops;
    }

    public void setOps(List<String> ops) {
        this.ops = ops;
    }

    public Map<String, Object> getMap() {
        return map;
    }

    public void setMap(Map<String, Object> map) {
        this.map = map;
    }

    public boolean isVerify() {
        return verify;
    }

    public void setVerify(boolean verify) {
        this.verify = verify;
    }
}
