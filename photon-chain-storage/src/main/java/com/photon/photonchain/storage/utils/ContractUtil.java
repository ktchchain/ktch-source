package com.photon.photonchain.storage.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.photon.photonchain.storage.constants.Constants;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author: lqh
 * @description: ContractUtil
 * @program: photon-chain
 * @create: 2018-07-12 10:40
 **/
public class ContractUtil {

    /*
* 获取兑换的ptn金额
* */
    public static long getExchangePtn(String bin) {
        Map<String, Object> contractMap = JSON.parseObject(Hex.decode(bin), Map.class);//将字节码转回map
        String parameter = null;
        BigDecimal fee = new BigDecimal(0);
        List<String> list = new ArrayList<>();
        for (String s : contractMap.keySet()) {
            if (StringUtils.startsWith(contractMap.get(s).toString().trim(), "set")) {
                String[] variable = ArrayUtils.removeAllOccurences(contractMap.get(s).toString().split(" "), "");
                if (variable[1].equals("coin")) {
                    list.add(contractMap.get(s).toString());
                }
            }
            if (StringUtils.startsWith(contractMap.get(s).toString().trim(), "event")) {
                if (StringUtils.substringBetween(contractMap.get(s).toString(), "event", "(").trim().equals("business")) {
                    parameter = StringUtils.substringBetween(contractMap.get(s).toString(), "(", ")");
                }
            }
        }
        String[] eventParams = parameter.split(",");//事件里的参数
        for (int i = 0; i < eventParams.length; i++)//去除参数里面空格
            eventParams[i] = eventParams[i].trim();
        for (String s : list) {
            String[] variable = ArrayUtils.removeAllOccurences(s.split(" "), "");
//            String[] params = variable[2].split("=");//获取coin 参数
            //获取手续费
            if (variable[variable.length - 1].toLowerCase().equals(Constants.PTN) && (variable[2].equals(eventParams[0]) || variable[2].equals(eventParams[1]))) {
                fee = new BigDecimal(variable[4]).multiply(new BigDecimal(Constants.MININUMUNIT));
            }
        }
        return fee.longValue();
    }

    /**
     * 解析投票合约
     */
    public static Map analisysVotesContract(String bin) {
        Map resultMap = null;
        try {
            resultMap = new HashMap();
            Map<String, Object> contractMap = JSON.parseObject(Hex.decode(bin), Map.class);//将字节码转回map
            StringBuffer contract = new StringBuffer();
            for (int i = 0;i < contractMap.keySet().size();i++) {
                if(contractMap.get("content"+i) != null) contract.append(contractMap.get("content"+i).toString().replaceAll("\n", "")+";");
            }
            String eventVote = getString(contract.toString(), "event(\\s)*vote\\([^\\(\\)]*\\)");
            String[] eventVoteParams = eventVote.substring(eventVote.indexOf("(") + 1, eventVote.lastIndexOf(")")).split(",");
            String function = eventVoteParams[0];
            String title = eventVoteParams[1];
            String options = eventVoteParams[2];
            //topic
            String topic = "";
            //items
            String items = "";
            //jiaoyan
            String jiaoyan = getString(contract.toString(), "function(\\s)*jiaoyan\\(\\)\\{.*\\}");
            List<String> setList = ConTractCompile.getSetList(contract.toString());
            String functionContent = ConTractCompile.getContent(jiaoyan,"{","}");
            List<String> ifList = ConTractCompile.getIfList(functionContent);
            for(String set : setList){
                String[] sets = ConTractCompile.splitStr(set);
                if(sets[1].equals("arr")){
                    sets = set.split(" ");
                }
                if(sets[2].equals(title)){
                    topic = sets[4];
                }
                if(sets[2].equals(options)){
                    items = sets[4];
                }
            }
            StringBuffer sb = new StringBuffer();
            //数组使用
            Integer index = 0;
            for(int i = 0; i <ifList.size() ; i++){
                String[] conditions = ifList.get(i).split(" ");
                if(conditions[0].contains("[")){
                    index = Integer.parseInt(ConTractCompile.getContent(conditions[0],"[","]"));
                    conditions[0] = StringUtils.substringBefore(conditions[0],"[");
                }
                if(conditions[2].contains("[")){
                    index = Integer.parseInt(ConTractCompile.getContent(conditions[2],"[","]"));
                    conditions[2] = StringUtils.substringBefore(conditions[2],"[");
                }
                for(int j = 0;j < conditions.length;j++){
                    boolean flag = false;
                    for(String set : setList){
                        String[] sets = ConTractCompile.splitStr(set);
                        if(sets[1].equals("arr")){
                            sets = set.split(" ");
                        }
                        if(conditions[j].equals(sets[2])){
                            if(sets[4].contains("[")){
                                String[] arrStr = StringUtils.substringsBetween( sets[4],"[","]")[0].split(",");
//                                sb.append(arrStr[index]+" ");
                                sb.append(Constants.serialNumber[index]+" ");
                                flag = true;
                            }else{
                                sb.append(sets[4]+" ");
                                flag = true;
                            }
                        }
                    }
                    if(conditions[j].equals("nowTime")){
                        sb.append("当前时间"+" ");
                        flag = true;
                    }
                    if(!flag){
                        sb.append(conditions[j]+" ");
                    }
                }

                if(i != ifList.size()-1){
                    sb.append("或者"+" ");
                }
            }
            resultMap.put("topic", topic.replace("\"",""));
            resultMap.put("items", items.substring(1,items.length()-1));
            //A>100或者B>=2或者C==1
            resultMap.put("dealLineCondition", sb);
            System.out.println(JSONObject.toJSON(resultMap));
            return resultMap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    /**
     * 获取查询的字符串
     * 将匹配的字符串取出
     */
    private static String getString(String str, String regx) {
        String res = "";
        Pattern pattern = Pattern.compile(regx);
        Matcher matcher = pattern.matcher(str);
        while (matcher.find()) {
            res += matcher.group();
        }
        return res;
    }

    /**
     * 解析代币合约
     *
     * @param bin
     * @return
     */
    public static Map<String,String> analisysTokenContract(String bin) {
        try {
            Map<String,String> resultMap = new LinkedHashMap();
            Map<String, Object> contractMap = JSON.parseObject(Hex.decode(bin), Map.class);//将字节码转回map
            StringBuffer contract = new StringBuffer();
            for (String key : contractMap.keySet()) {
                contract.append(contractMap.get(key).toString().replaceAll("\n", "")).append(";");
            }
            String contractxx = contract.toString();
            String eventToken = getString(contract.toString(), "event(\\s)*token\\([^\\(\\)]*\\)");
            String[] tokenArr = eventToken.substring(eventToken.indexOf("(") + 1, eventToken.lastIndexOf(")")).split(",");
            String tokenDecimal = tokenArr[0]; //tokenDecimal
            String tokenNum = tokenArr[1]; //tokenNum
            String tokenName = tokenArr[2]; //tokenName
            tokenDecimal = getString(contract.toString(), "set(\\s)*integer(\\s)*" + tokenDecimal.trim() + "(\\s)*=[^;]*;");
            tokenDecimal = tokenDecimal.substring(tokenDecimal.indexOf("=") + 1, tokenDecimal.indexOf(";")).trim();
            tokenNum = getString(contract.toString(), "set(\\s)*integer(\\s)*" + tokenNum + "(\\s)*=[^;]*;");
            tokenNum = tokenNum.substring(tokenNum.indexOf("=") + 1, tokenNum.indexOf(";")).trim();
            tokenName = getString(contract.toString(), "set(\\s)*str(\\s)*" + tokenName + "(\\s)*=[^;]*;");
            tokenName = tokenName.substring(tokenName.indexOf("=") + 1, tokenName.indexOf(";")).replace("\"", "").trim();
            resultMap.put("tokenName", tokenName);
            resultMap.put("tokenNum", tokenNum);
            resultMap.put("tokenDecimal", tokenDecimal);
            return resultMap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
