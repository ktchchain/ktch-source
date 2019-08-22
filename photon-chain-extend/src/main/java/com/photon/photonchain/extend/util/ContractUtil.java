package com.photon.photonchain.extend.util;

import com.alibaba.fastjson.JSON;
import com.photon.photonchain.extend.compiler.Pcompiler;
import com.photon.photonchain.extend.compiler.RegexCompile;
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
            for (String key : contractMap.keySet()) {
                contract.append(contractMap.get(key).toString().replaceAll("\n", ""));
            }
            String eventVote = getString(contract.toString(), "event(\\s)*vote\\([^\\(\\)]*\\)");
            String[] eventVoteParams = eventVote.substring(eventVote.indexOf("(") + 1, eventVote.lastIndexOf(")")).split(",");
            String function = eventVoteParams[0];
            String title = eventVoteParams[1];
            String options = eventVoteParams[2];
            //topic
            String topic = getString(contract.toString(), "set(\\s)*str(\\s)*" + title + "(\\s)*=(\\s)*\".*\"(\\s)*");
            topic = topic.substring(topic.indexOf("\"") + 1, topic.lastIndexOf("\""));
            //items
            String items = getString(contract.toString(), "set(\\s)*arr(\\s)*" + options + "(\\s)*=(\\s)*\\[.*\\](\\s)*");
            items = items.substring(items.indexOf("[") + 1, items.lastIndexOf("]")).replace("", "").replaceAll("\"", "");
            //jiaoyan
            String jiaoyan = getString(contract.toString(), "function(\\s)*" + function + "\\(\\)\\{.*\\}");
           List<String> setList = RegexCompile.getSetList(contract.toString());
            for(String set : setList){
                String[] sets = set.split(" ");//sets[2] name sets[4] value
            }
            Map<String,String> contentMap = Pcompiler.getLine(jiaoyan);
            for (int function_i = 1; function_i <= contentMap.keySet().size(); function_i++) {

            }
//            String eventVote = getString(contract.toString(), "event(\\s)*vote\\([^\\(\\)]*\\)");
//            String[] eventVoteParams = eventVote.substring(eventVote.indexOf("(") + 1, eventVote.lastIndexOf(")")).split(",");
//            String function = eventVoteParams[0];
//            String title = eventVoteParams[1];
//            String options = eventVoteParams[2];
//            //topic
//            String topic = getString(contract.toString(), "set(\\s)*str(\\s)*" + title + "(\\s)*=(\\s)*\".*\"(\\s)*");
//            topic = topic.substring(topic.indexOf("\"") + 1, topic.lastIndexOf("\""));
//            //items
//            String items = getString(contract.toString(), "set(\\s)*arr(\\s)*" + options + "(\\s)*=(\\s)*\\[.*\\](\\s)*");
//            items = items.substring(items.indexOf("[") + 1, items.lastIndexOf("]")).replace("", "").replaceAll("\"", "");
//
//            //jiaoyan
//            String jiaoyan = getString(contract.toString(), "function(\\s)*" + function + "\\(\\)\\{.*\\}");
//            String ifs = getString(jiaoyan, "if\\(.*\\)\\{.*stop;\\}");
//            System.out.println(ifs);
//            String[] ifArr = ifs.split("if");
//            String dealLineCondition = "";
//            for (int i = 0; i < ifArr.length; i++) {
//                if (ifArr[i].equals("")) {
//                    continue;
//                }
//                if (ifArr[i].contains(options)) {
//                    Integer optionsIndex = Integer.parseInt(ifArr[i].substring(ifArr[i].indexOf(options + "[") + (options + "[").length(), ifArr[i].indexOf("].count")));
//                    String opSymbol = getString(ifArr[i], "[>|<|>=|<=|==]");
//                    String number = getString(ifArr[i], "(\\s)*" + opSymbol + "(\\s)*[\\d]*(\\s)*\\)").replace(opSymbol, "").replace(")", "").replaceAll(" ", "");
//                    System.out.println("opSymbol:" + opSymbol);
//                    dealLineCondition += Constants.serialNumber[optionsIndex] + opSymbol + number;
//                }
//                if (ifArr[i].toLowerCase().contains("nowtime")) {
//                    String opSymbol = getString(ifArr[i], "[>|<|>=|<=|==]");
//                    String t = getString(ifArr[i], opSymbol + ".*\\)").replace(opSymbol, "").replace(")", "");
//                    String tVal = "";
//                    tVal = getString(contract.toString(), "set(\\s)*time(\\s)*" + t + "(\\s)*=(\\s)*\"[\\d|-]*\"");
//                    tVal = tVal.substring(tVal.indexOf("\"") + 1, tVal.lastIndexOf("\""));
//                    dealLineCondition += "时间" + opSymbol + tVal;
//                }
//                if (i != (ifArr.length - 1)) {
//                    dealLineCondition += "或者";
//                }
//            }
            resultMap.put("topic", topic);
            resultMap.put("items", items);
//            resultMap.put("dealLineCondition", dealLineCondition);
            System.out.println(resultMap);
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

    public static void main(String[] args) {
        String bin ="7b22636f6e74656e7438223a227365742073747220746f6b656e4e616d65203d205c2274657374345c22222c22636f6e74656e7437223a2273657420696e746567657220746f6b656e4e756d32203d20313030303032222c22636f6e74656e7436223a2273657420696e746567657220746f6b656e4e756d31203d20313030303031222c22636f6e74656e7435223a2273657420696e746567657220746f6b656e4e756d203d20313030303030222c22636f6e74656e7434223a2273657420696e746567657220746f6b656e42697431203d2038222c22636f6e74656e7433223a2273657420696e746567657220746f6b656e42697432203d2037222c22636f6e74656e7432223a2273657420696e746567657220746f6b656e426974203d2036222c22636f6e7472616374223a226365736869222c22636f6e74656e7431223a22736574206164647265737320616464203d2030343366383833663862373032656339346463633536323230393961633733313538323230306334393666313130396664613339643364613064383065656231323361353333396533363039353362656362646464363061613434623831333233313530303731626634386132316538653239356630396338663363313434383038222c2276657273696f6e223a2270636f6d70696c65722076657273696f6e20312e302e30222c22636f6e74656e7439223a226576656e7420746f6b656e28746f6b656e42697432202c746f6b656e4e756d32202c746f6b656e4e616d6529227d";
        ContractUtil.analisysTokenContract(bin);
    }
}
