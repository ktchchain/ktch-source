package com.photon.photonchain.extend.compiler;

import com.alibaba.fastjson.JSON;
import com.photon.photonchain.extend.Universal.EventEnum;
import com.photon.photonchain.extend.Universal.OpCodeEnum;
import com.sun.org.apache.bcel.internal.generic.RETURN;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.photon.photonchain.extend.compiler.MemberType.event;
import static com.photon.photonchain.extend.compiler.MemberType.variable;

public class Pcompiler {
    //编译合约
    public static String compilerContract(String contractStr) {
        contractStr = unescapeJava(contractStr.trim());
        contractStr = complementSpace(contractStr);//补=两边空格
        contractStr = replaceSpace(contractStr);
        Map<String, Object> contractMap = new HashMap<>();
        Map<String, String> eventMap = new HashMap<>();
        int i = 1;
        while (true) {
            if (contractStr.trim().contains("version")) {
                String version = readLine(contractStr.trim());
                contractMap.put("version", version);
                contractStr = contractStr.trim().substring(version.length() + 1, contractStr.trim().length());
            }
            if (contractStr.contains("contract")) {
                String contract = readLine(contractStr.trim());
                contractMap.put("contract", getMemberName(contract, MemberType.contract));
                contractStr = getContent(contractStr.trim(), "{", "}");
            }
            String content = readLine(contractStr.trim());
            if (StringUtils.isNotBlank(content)) {
                contractMap.put("content" + i, content);
                i++;
            }
            if (StringUtils.isBlank(content)) {
                content += ";";
            }
            contractStr = contractStr.trim().substring(content.length(), contractStr.trim().length());
            if (contractStr.trim().length() == 0)
                break;
        }
        for (String s : contractMap.keySet()) {
            if (contractMap.get(s).toString().contains("event")) {
                int index = contractMap.get(s).toString().indexOf("event");
                String st = contractMap.get(s).toString().substring(index);
                eventMap.put(StringUtils.substringBetween(st, " ", "("), StringUtils.substringBetween(st, "(", ")"));
            }
        }
        for (String eventName : eventMap.keySet()) {
            String[] eventst = eventMap.get(eventName).split(",");
            for (int z = 0; z < eventst.length; z++)//去除参数里面空格
                eventst[z] = eventst[z].trim();
            if (eventName.trim().equals(EventEnum.business.name())) {
                String selling = eventst[0];
                String purchase = eventst[1];
                String init = "function cancel(address contractAddress){trans(contractAddress,add," + selling + ");}";
                String function = "function exchange(address fromAddress,address contractAddress){trans(contractAddress,fromAddress," + selling + ");trans(fromAddress,add," + purchase + ");}";
                contractMap.put("content" + i++, init);
                contractMap.put("content" + i++, function);
            }
            if (eventName.trim().equals(EventEnum.vote.name())) {
                String functionName = eventst[0];
                String title = eventst[1];
                String options = eventst[2];
                String function = "function vote(" + "jiaoyan" + "," + title + "," + options + ",chose){" +
                        "function." + "jiaoyan" + "();" +
                        "make chose;" +
                        "}";
                String jiaoyan = "function jiaoyan(){" +
                        "if( nowTime > "+eventst[0]+" ) {stop;}" +
                        "}";
                contractMap.put("content" + i++, jiaoyan);
                contractMap.put("content" + i++, function);
            }
        }
        for (String s : contractMap.keySet()) {
            System.out.println(s + "---" + contractMap.get(s));
        }
        String s = Hex.toHexString(JSON.toJSONBytes(contractMap));
        System.out.println(s);
        return s;
    }

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
    //使用正则获取内容
    public static List<String> getStrMember(String str) {
        String p = "\"([^\"]*)\"" ;
//        Pattern pattern = Pattern.compile("(?<=\").*?(?=\")");
        Pattern pattern = Pattern.compile(p);
        Matcher m = pattern.matcher(str);
        List<String> result = new ArrayList<>();
        while (m.find()) {
            result.add(m.group());
        }
        return result;
    }

    public static String getMemberName(String member, MemberType memberType) {
        String memberName = null;
        switch (memberType) {
            case contract:
                memberName = member.substring("contract ".length(), member.indexOf("{"));
                break;
            case event:
                memberName = member.substring("event ".length(), member.indexOf("("));
                break;
            case variable:
                memberName = member.substring(member.lastIndexOf(" "), member.indexOf("="));
                break;
            default:
                break;
        }
        return memberName.trim();
    }

    public static List<Map<String, String>> variableHandle(List<String> variableList) {
        List<Map<String, String>> result = new LinkedList<>();
        variableList.forEach(var -> {
            Map<String, String> map = new HashMap();
            var.trim();
            String name = getMemberName(var, variable);
            String type = var.substring("set ".length(), var.indexOf(" " + name)).trim();
            String value = var.substring(var.indexOf("=") + 1).trim();
            result.add(map);
        });
        return result;
    }

    public static List<Map<String, String>> eventHandle(List<String> eventList) {
        List<Map<String, String>> result = new LinkedList<>();
        eventList.forEach(eve -> {
            Map<String, String> map = new HashMap();
            eve.trim();
            String name = getMemberName(eve, event);
            String[] parameter = eve.substring(eve.indexOf("(") + 1, eve.indexOf(")")).split(",");
            result.add(map);
        });
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

    //获取每一行 只限合约
    public static Map<String, String> getLine(String contractStr) {
        Map<String, String> contractMap = new HashMap<>();
        int i = 1;
        while (true) {
            if (contractStr.trim().contains("version")) {
                String version = Pcompiler.readLine(contractStr.trim());
                contractMap.put("version", version);
                contractStr = contractStr.trim().substring(version.length() + 1, contractStr.trim().length());
            }
            if (contractStr.contains("contract")) {
                String contract = Pcompiler.readLine(contractStr.trim());
                contractMap.put("contract", Pcompiler.getMemberName(contract, MemberType.contract));
                contractStr = Pcompiler.getContent(contractStr.trim(), "{", "}");
            }
            String content = Pcompiler.readLine(contractStr.trim());
            if (StringUtils.isNotBlank(content)) {
                contractMap.put("content" + i, content);
                i++;
            }
            if (StringUtils.isBlank(content)) {
                content += ";";
            }
            contractStr = contractStr.trim().substring(content.length(), contractStr.trim().length());
            if (contractStr.trim().length() == 0)
                break;
        }
        return contractMap;
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

    //去除转义符
    public static String unescapeJava(String str) {
        str = StringEscapeUtils.unescapeHtml4(str);
        str = str.replaceAll("\\u00A0", " ");
        str = str.replaceAll("\\n", "");
        str = StringEscapeUtils.unescapeJava(str);
        return str;
    }

    //补齐=左右两边空格
    public static String complementSpace(String contract) {
        List<String> arrList = Pcompiler.getMember(contract, "(=[\\s]*\\[[^;]*)");
//        List<String> timeList = Pcompiler.getMember(contract, "(set[\\s]*time\\s*[^;]*)");
        //去除数组
        for (int i = 0; i < arrList.size(); i++) {
            contract = contract.replace(arrList.get(i), arrList.get(i).replaceAll("\\s+", ""));
        }
        //补齐中排除字符串
        List<String> strList = Pcompiler.getStrMember(contract);//取出字符串
        for(int i = 0;i<strList.size();i++){
            contract = contract.replace(strList.get(i),"!_"+i);
        }
        //排除set time类型
//        for (int i = 0; i < timeList.size(); i++) {
//            contract = contract.replace(timeList.get(i), "_hq_str_contract_" + i);
//        }
        contract = contract.replaceAll("\\s*\\;\\s*", "\\;");
        contract = contract.replaceAll("\\s*\\{\\s*", "\\{");
        contract = contract.replaceAll("\\s*\\}\\s*", "\\}");
        contract = contract.replaceAll("\\s*\\(\\s*", "\\(");
        contract = contract.replaceAll("\\s*\\)\\s*", "\\)");
        contract = contract.replaceAll("[" + OpCodeEnum.SEQ.key + "]", " " + OpCodeEnum.SEQ.key + " ");
        contract = contract.replaceAll("[" + OpCodeEnum.ADD.key + "]", " " + OpCodeEnum.ADD.key + " ");
        contract = contract.replaceAll("[" + OpCodeEnum.SUB.key + "]", " " + OpCodeEnum.SUB.key + " ");
        contract = contract.replaceAll("[" + OpCodeEnum.MUL.key + "]", " " + OpCodeEnum.MUL.key + " ");
        contract = contract.replaceAll("[" + OpCodeEnum.DIV.key + "]", " " + OpCodeEnum.DIV.key + " ");
        contract = contract.replaceAll("[" + OpCodeEnum.GT.key + "]", " " + OpCodeEnum.GT.key + " ");
        contract = contract.replaceAll("[" + OpCodeEnum.LT.key + "]", " " + OpCodeEnum.LT.key + " ");
        contract = contract.replaceAll("[" + OpCodeEnum.SEQ.key + "]\\s+[" + OpCodeEnum.SEQ.key + "]", " " + OpCodeEnum.SEQ.key + OpCodeEnum.SEQ.key + " ");
        contract = contract.replaceAll("[" + OpCodeEnum.GT.key + "]\\s+[" + OpCodeEnum.SEQ.key + "]", " " + OpCodeEnum.GT.key + OpCodeEnum.SEQ.key + " ");
        contract = contract.replaceAll("[" + OpCodeEnum.LT.key + "]\\s+[" + OpCodeEnum.SEQ.key + "]", " " + OpCodeEnum.LT.key + OpCodeEnum.SEQ.key + " ");
        contract = contract.replaceAll("[" + OpCodeEnum.LT.key + "]\\s+[" + OpCodeEnum.GT.key + "]", " " + OpCodeEnum.LT.key + OpCodeEnum.GT.key + " ");
        for(int i = strList.size()-1;i>=0;i--){
            contract = contract.replace("!_"+i,strList.get(i));
        }
//        for (int i = 0; i < timeList.size(); i++) {
//            contract = contract.replace("_hq_str_contract_" + i,timeList.get(i));
//        }
        return contract;
    }

    //去空格
    public static String replaceSpace(String s) {
        return s.replaceAll(" {2,}", " ");
    }

    public static void main(String[] args) {
//        1005
        String s = "7b22636f6e74656e7437223a2266756e6374696f6e20766f7465286a69616f79616e2c7469746c652c6f7074696f6e732c63686f7365297b66756e6374696f6e2e6a69616f79616e28293b6d616b652063686f73653b7d222c22636f6e74656e7436223a226576656e7420766f7465286a69616f79616e2c7469746c652c6f7074696f6e7329222c22636f6e74656e7435223a2266756e6374696f6e206a69616f79616e28297b6966286f7074696f6e735b305d2e636f756e74203d3d2031297b73746f703b7d6966286f7074696f6e735b315d2e636f756e74203e3d2033297b73746f703b7d6966286f7074696f6e735b325d2e636f756e74203d3d2034297b73746f703b7d6966286e6f7754696d65203e2074297b73746f703b7d7d222c22636f6e74656e7434223a227365742074696d652074203d205c22323031392d31302d30322030303a30303a30305c22222c22636f6e74656e7433223a2273657420617272206f7074696f6e73203d205b5c22e98089e9a1b9315c222c5c22e98089e9a1b9325c222c5c22e98089e9a1b9335c225d222c22636f6e74656e7432223a2273657420737472207469746c65203d205c22e6b58be8af95e6b58be8af955c22222c22636f6e7472616374223a2264656d6f222c22636f6e74656e7431223a22736574206164647265737320616464203d2030346134616339316438626634313732386462323166303732303961663163373031343339303431336239646265643631626632623736643333623032386336316635306134333866353032393233343232393433373438373434663530653930393439326138323434633737336530613565393963346633383062613032386434222c2276657273696f6e223a2270636f6d70696c65722076657273696f6e20312e302e30227d";
        Map<String, Object> contractMap = JSON.parseObject(Hex.decode(s), Map.class);//将字节码转回map
//        String s =
//                "              set  coin    sell = 200&nbsp;&nbsp;&nbsp;&nbsp;ptn" +
//                "        ";
//        s = unescapeJava(s);
//        String[] variable = ArrayUtils.removeAllOccurences(s.split(" "), "");
//        System.out.println(variable);
        System.out.println(contractMap);
    }
}