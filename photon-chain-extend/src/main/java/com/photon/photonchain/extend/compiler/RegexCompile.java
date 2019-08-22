package com.photon.photonchain.extend.compiler;

import com.photon.photonchain.exception.BusinessException;
import com.photon.photonchain.exception.ErrorCode;
import com.photon.photonchain.extend.Universal.EventEnum;
import com.photon.photonchain.extend.Universal.OpCodeEnum;
import com.photon.photonchain.extend.Universal.TypeEnum;
import com.photon.photonchain.storage.constants.Constants;
import io.netty.util.internal.SocketUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexCompile {

    final static Logger logger = LoggerFactory.getLogger(RegexCompile.class);

    //校验合约
    public static void checkRegex(String str) throws Exception {
        boolean flag = false;
        str = Pcompiler.unescapeJava(str);
        str = Pcompiler.complementSpace(str);
        str = Pcompiler.replaceSpace(str);
        String contractStr = str, tempContractStr = str, eventStr = str;
        if (!EventEnum.checkEventVote(eventStr))
            throw new BusinessException("error,undefined event vote", ErrorCode._10001);
        if (!EventEnum.checkEventBusiness(eventStr))
            throw new BusinessException("error,undefined  event business", ErrorCode._10001);
        if (!EventEnum.checkEventToken(eventStr))
            throw new BusinessException("error,undefined  event token", ErrorCode._10001);
        List<String> list = getStopList(eventStr);
        for (String stop : list) {
            if (!stop.trim().equals("stop"))
                throw new BusinessException("error,stop错误", ErrorCode._10045);
        }
        //补齐中排除字符串
        List<String> strList = Pcompiler.getStrMember(contractStr);//取出字符串
        for (int i = 0; i < strList.size(); i++) {
            contractStr = contractStr.replace(strList.get(i), "!_" + i);
        }
        List<String> strList1 = Pcompiler.getStrMember(str);//取出字符串
        for (int i = 0; i < strList1.size(); i++) {
            str = str.replace(strList1.get(i), "!_" + i);
        }
        List<String> strList2 = Pcompiler.getStrMember(eventStr);//取出字符串
        for (int i = 0; i < strList2.size(); i++) {
            eventStr = eventStr.replace(strList2.get(i), "!_" + i);
        }
        if (!checkChina(eventStr)) {
            throw new BusinessException("error,不能使用中文字符", ErrorCode._10001);
        }
        //补齐字符串
        for (int i = 0; i < strList2.size(); i++) {
            eventStr = eventStr.replace("!_" + i, strList.get(i));
        }
//        if(strContract.contains("\""))  throw new BusinessException("error,undefined contract", ErrorCode._10001);
        String tempStr = str.replace(Pcompiler.getContent(str, "{", "}"), "");
        List<String> setList = RegexCompile.getSetList(str);
        //必须按合约规范编写
        if (!checkContract(str))
            throw new BusinessException("error,undefined contract", ErrorCode._10001);
        if (!checkOneContract(tempStr))
            throw new BusinessException("error,undefined contract", ErrorCode._10001);
        if (Collections.frequency(Arrays.asList(splitStr(tempStr)), "pcompiler") > 1)
            throw new BusinessException("error,undefined contract", ErrorCode._10001);
        int end = str.length();
        //验证括号是否符合校验
        int a1 = 0, a2 = 0, b1 = 0, b2 = 0, c0 = 0;
        for (int i = 0; i < end; i++) {
            char[] c = str.toCharArray();
            if (c[i] == '{') {
                a1++;
            }
            if (c[i] == '}') {
                a2++;
            }
            if (c[i] == '(') {
                b1++;
            }
            if (c[i] == ')') {
                b2++;
            }
            if (c[i] == '\"') {
                c0++;
            }
        }
        //校验"成对存在
        if (c0 % 2 != 0) {
            throw new BusinessException("error,non standard contract writing", ErrorCode._10002);
        }
        if (a1 != a2) {
            throw new BusinessException("error,non standard contract writing", ErrorCode._10002);
        } else if (b1 != b2) {
            throw new BusinessException("error,non standard contract writing", ErrorCode._10002);
        } else {
            flag = true;
        }
        //排除括号{前字符问题
        char[] chars = str.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            int j = 0;
            if (i == chars.length) {
                if (!String.valueOf(chars[i]).equals("}")) {
                    throw new BusinessException("error,} end Match no characters", ErrorCode._10003);
                }
            }
            if (chars[i] == '{') {//验证{前面的字符是否是字母数字下划线
                j = i - 1;
                if (j >= 1) {
                    while (String.valueOf(chars[j]).equals(" ")) {
                        j--;
                    }
                    if (j >= 0) {
                        if (!RegexCompile.braRegex(String.valueOf(chars[j]))) {
                            throw new BusinessException("error,{ before Match no characters", ErrorCode._10004);
                        }
                    }
                }
            }
            if (chars[i] == '(') {//验证(前面的字符是否是字母数字下划线
                j = i - 1;
                if (j >= 1) {
                    while (String.valueOf(chars[j]).equals(" ")) {
                        j--;
                    }
                    if (j >= 0) {
                        if (!RegexCompile.braRegex(String.valueOf(chars[j]))) {
                            throw new BusinessException("error,( before Match no characters", ErrorCode._10005);
                        }
                    }
                }
            }
            if (chars[i] == ')') {//验证)后面的字符是否是字母数字下划线
                j = i + 1;
                if (j < chars.length) {
                    while (String.valueOf(chars[j]).equals(" ")) {
                        j++;
                    }
                    if (j < chars.length) {
                        if (Pattern.matches("\\w+", String.valueOf(chars[j]))) {
                            throw new BusinessException("error,) before Match no characters", ErrorCode._10006);
                        }
                    }
                }
            }
        }//验证类是否符合规范
        //补齐字符串
        for (int i = strList.size()-1; i >= 0; i--) {
            contractStr = contractStr.replace("!_" + i, strList.get(i));
        }
        if (flag) {
            int i = 1;
            Map<String, String> contractMap = new HashMap<>();
            while (true) {
                if (contractStr.trim().contains("version")) {
                    String version = Pcompiler.readLine(contractStr.trim());
                    contractMap.put("version", version);
                    contractStr = contractStr.trim().substring(version.length() + 1, contractStr.trim().length());
                }
                if (contractStr.contains("contract")) {
                    String contract = Pcompiler.readLine(contractStr.trim());
                    String s = Pcompiler.getMemberName(contract, MemberType.contract);
                    if (splitStr(s).length > 1)
                        throw new BusinessException("error,undefined contract with name", ErrorCode._10007);
                    contractMap.put("contract", RegexCompile.varRegex(s));
                    contractStr = Pcompiler.getContent(contractStr.trim(), "{", "}");
                }
                String content = Pcompiler.readLine(contractStr.trim());
                if (StringUtils.isNotBlank(content)) {
                    contractMap.put("content" + i, content);
                    i++;
                }
                if (content == null) {
                    throw new BusinessException("error,undefined contract", ErrorCode._10001);
                }
                if (content.equals("")) {
                    content += ";";
                }
                contractStr = contractStr.trim().substring(content.length(), contractStr.trim().length());
                if (contractStr.trim().length() == 0)
                    break;
            }
            List<String[]> ls = new ArrayList<>();//获取set属性类型 名 代币 值
            List<String> lsName = new ArrayList<>();//获取set属性类型 名
            Map<String, String[]> eventMap = new HashMap<>(); //事件名 事件中的参数
            Map<String, String[]> functionMap = new HashMap<>();//方法名 方法中的参数
            Map<String, String[]> eventVarMap = new HashMap<>();
            List<String> varLists = new ArrayList<>();//参数类型值
            for (String s : contractMap.keySet()) {
                if (checkSet(contractMap.get(s))) {//判断全局属性
                    if (StringUtils.containsAny(contractMap.get(s), new char[]{'{', '}', '(', ')'}))
                        throw new BusinessException("error ,variable writing does not start with set", ErrorCode._10008);
                }
                if (!(s.equals("contract") || s.equals("version"))) {
                    if (!(
                            StringUtils.startsWith(contractMap.get(s).trim(), "set ")
                                    || StringUtils.startsWith(contractMap.get(s).trim(), "function ")//目前不给以function开头
                                    || StringUtils.startsWith(contractMap.get(s).trim(), "event "))) {
                        throw new BusinessException("error,undefined contract", ErrorCode._10001);
                    }
                    //验证事件
                    if (StringUtils.startsWith(contractMap.get(s).trim(), "event ")) {
                        int index = contractMap.get(s).indexOf("event");
                        String st = contractMap.get(s).substring(index);
                        String eventName = StringUtils.substringBetween(st, " ", "(").trim();//获取事件名

                        String[] eventParam = ArrayUtils.removeAllOccurences(StringUtils.substringBetween(st, "(", ")").split(","), "");//获取事件方法参数
                        for (int z = 0; z < eventParam.length; z++)//去除参数里面空格
                            eventParam[z] = eventParam[z].trim();
                        //判断事件名字是否重复
                        if (eventMap.get(eventName) != null)
                            throw new BusinessException("error,eventName repetition", ErrorCode._10012);
                        int eventCount = EventEnum.checkEventName(eventName);//判断事件名称是否存在eventEnum
                        if (eventCount == -1)
                            throw new BusinessException("error,eventName repetition", ErrorCode._10012);
                        int eventNum = StringUtils.countMatches(st, ",");//获取逗号数
                        //判断事件中的逗号数是否对应事件的参数
                        if (eventCount == 0 && eventNum > 0)
                            throw new BusinessException("error,event , too much", ErrorCode._10013);
                        if (eventNum > 0 && eventCount - 1 != eventNum) {
                            throw new BusinessException("error,event , too much", ErrorCode._10013);
                        }
                        if (eventParam != null && eventParam.length != eventCount)//判断参数个数
                            throw new BusinessException("error,eventName repetition", ErrorCode._10013);
                        eventMap.put(eventName, eventParam);//存事件 根据名字 里面的内容
                        //判断事件中的参数是否重复
                        List<String> strsToList = Arrays.asList(eventParam);
                        for (String param : strsToList) {
                            int paramNum = Collections.frequency(strsToList, param);
                            if (paramNum > 1)
                                throw new BusinessException("error,event param name repetition", ErrorCode._10014);
                        }
                        eventVarMap.put(s, eventParam);
                    }
                    if (StringUtils.containsAny(contractMap.get(s), new char[]{'{', '}'})) {
                        if (!contractMap.get(s).trim().contains("function ")) {
                            throw new BusinessException("error", ErrorCode._10001);
                        }
                    }
                    //验证方法
                    if (StringUtils.startsWith(contractMap.get(s).trim(), "function ")) {
                        int index = contractMap.get(s).indexOf("function");
                        String st = contractMap.get(s).substring(index);
                        String functionName = StringUtils.substringBetween(st, " ", "(").trim();//获取方法名
                        String functionParamVar = StringUtils.substringBetween(st, "(", ")");
                        String[] functionParam = null;
                        if (StringUtils.isNotBlank(functionParamVar)) {
                            functionParam = StringUtils.substringBetween(st, "(", ")").split(",");//获取方法参数
                        }
                        if(functionName.trim().equalsIgnoreCase("jiaoyan"))
                            throw new BusinessException("error,functionName repetition", ErrorCode._10015);
                        //判断事件名字是否重复
                        if (functionMap.get(functionName) != null || !Pattern.matches("^[a-zA-Z][a-zA-Z0-9_]*$+", functionName))
                            throw new BusinessException("error,functionName repetition", ErrorCode._10015);
                        functionMap.put(functionName, functionParam);
                        if (!(contractMap.get(s).trim().toLowerCase().contains(OpCodeEnum.RETURN.name().toLowerCase()) || contractMap.get(s).trim().toLowerCase().contains(OpCodeEnum.STOP.name().toLowerCase())))
                            throw new BusinessException("error", ErrorCode._10001);
                        functionRegex(contractMap.get(s).trim(), tempContractStr, functionParam);//验证方法内容
                    }
                    //排除方法事件 验证属性
                    if (StringUtils.startsWith(contractMap.get(s).trim(), "set ")) {
                        //判断是否还存在"
                        String contentStr = contractMap.get(s);
                        List<String> setStr = Pcompiler.getStrMember(contentStr);//取出字符串
                        for (int setStr_i = 0; setStr_i < setStr.size(); setStr_i++) {
                            contentStr = contentStr.replace(setStr.get(setStr_i), "!_" + i);
                        }
                        if (StringUtils.containsAny(contentStr, new char[]{'{', '}', '(', ')'}))
                            throw new BusinessException("error ,variable writing does not start with set", ErrorCode._10008);
                        String[] aa = splitStr(contractMap.get(s));
                        //排除引号内容
                        String[] variable = splitStr(contractMap.get(s));
                        //数组时间特殊处理
                        if (variable[1].equals(TypeEnum.arr.name())) {
                            variable = ArrayUtils.removeAllOccurences(contractMap.get(s).split(" "), "");
                        }
                        if (!variable[1].equals(TypeEnum.coin.name()) && variable.length >= 6)
                            throw new BusinessException("error ,variable writing is not standard", ErrorCode._10009);
                        if (variable[1].equals(TypeEnum.coin.name()) && variable.length != 6)
                            throw new BusinessException("error ,variable coin writing is not standard", ErrorCode._10009);
                        if (!TypeEnum.matchType(variable[1]))
                            throw new BusinessException("error ,variable type writing is undefined", ErrorCode._10010);//验证类型
                        if (StringUtils.countMatches(s, "=") > 1)
                            throw new BusinessException("error ,variable writing is = not standard", ErrorCode._10010);//验证多少个=
                        varRegex(variable[2]);//检验=左边 验证属性参数名字
                        String temp = variableRegex(variable[1], variable[4]);//检验=右边
                        //属性类型 属性名
                        if (variable[1].equals("coin")) {
                            String[] var = {variable[1], variable[2], variable[5], variable[4]};
                            ls.add(var);//添加属性命名
                            lsName.add(var[1]);
                        } else {
                            String[] var = {variable[1], variable[2]};
                            ls.add(var);//添加属性命名
                            lsName.add(var[1]);
                        }

                    }
                }
            }
            //验证事件参数顺序问题
            for (String eventVars : eventVarMap.keySet()) {
                int num = Integer.parseInt(StringUtils.substringAfter(eventVars, "content"));//事件参数所在行数
                String[] vars = eventVarMap.get(eventVars);
                //获取事件名字
                String eventName = StringUtils.substringBetween(contractMap.get(eventVars), "event ", "(");
                for (String var : vars) {
                    for (String s : contractMap.keySet()) {
                        if (StringUtils.startsWith(contractMap.get(s).trim(), "set ")) {
                            if (contractMap.get(s).contains(var)) {
                                int compare = Integer.parseInt(StringUtils.substringAfter(s, "content"));//要比较的行数
                                if (num < compare)
                                    throw new BusinessException("error ,The parameter sequence in the event is not found", ErrorCode._10024);//事件中的参数顺序查找不到
                            }
                        }
                    }
                }
            }

            for (String[] s : ls) {//验证属性名字是否重复
                int index = Collections.frequency(lsName, s[1]);
                if (index > 1) throw new BusinessException("error,param name repetition", ErrorCode._10016);
            }
            for (String eventName : eventMap.keySet()) {
                String[] eventParam = eventMap.get(eventName);
                List listB = Arrays.asList(eventParam);
                if (!lsName.containsAll(listB))
                    throw new BusinessException("error,event param name repetition", ErrorCode._10013);
                int ptnNum = 0;
                if (eventName.equalsIgnoreCase(EventEnum.business.name())) {
                    for (String[] s : ls) {
                        if (s[1].equals(eventParam[0]) || s[1].equals(eventParam[1])) {
                            if (s[2].toLowerCase().equals(Constants.PTN)) ptnNum++;//目前挂单 参数有一个必须是ptn
                        }
                    }
                    if (ptnNum != 1) throw new BusinessException("error,event param erroneous ptn", ErrorCode._10018);
                }
            }
            //验证方法参数
            for (String functionParams : functionMap.keySet()) {
                List<String> functionVar = new ArrayList<>();
                String[] param = functionMap.get(functionParams);//参数
                if (param != null) {
                    for (String s : param) {
                        String[] var = splitStr(s);
                        if (var.length != 2) throw new BusinessException("error,function param Unlawful");
                        functionVar.add(var[1]);
                        varRegex(var[1]);//验证方法参数名字
                        if (!TypeEnum.matchType(var[0]))
                            throw new BusinessException("error ,variable type writing is undefined", ErrorCode._10010);//验证类型
                    }
                } else {
                    System.out.println("方法参数为空时处理");//TODO
                }
                for (String s : functionVar) {//验证命名是否重复
                    int index = Collections.frequency(functionVar, s);
                    if (index > 1)
                        throw new BusinessException("error,function param name repetition", ErrorCode._10015);
                }
            }
        }
        StringBuilder contractSb = new StringBuilder(eventStr);
        String indexBefore = StringUtils.substringBefore(contractSb.toString(), "event business");
        int temp1 = indexBefore.indexOf("{") + 1;
        contractSb = contractSb.insert(temp1, "set address add=04d487b5f73ba38d7a411862d5cbc0b82939321cd5af0ebb84293906f56e2d34f9b2f766055fd55a0111e5b55a90f7348cb4637e580926d61cd33eaad3a2b88ee2;");
        contractSb = new StringBuilder(Pcompiler.complementSpace(contractSb.toString()));//为参数=补空格
        String bin = Pcompiler.compilerContract(contractSb.toString());
    }

    //比对(){}等 是否对等
    public static int[] repContent(String str, String head, String end) throws BusinessException {
        int ind = str.indexOf(head);
        str = str.substring(ind);
        int he = 0;
        int en = 0;
        String index = null;
        for (int i = 0; i < str.length(); i++) {
            if (head.equals(String.valueOf(str.charAt(i)))) {
                he++;
            }
            if (end.equals(String.valueOf(str.charAt(i)))) {
                en++;
            }
            if (he == en) {
                index = String.valueOf(i);
                break;
            }
        }
        if (StringUtils.isBlank(index)) {
            throw new BusinessException("error,Unlawful contract", ErrorCode._10001);
        }
        int[] is = new int[2];
        is[0] = ind;
        is[1] = ind + Integer.parseInt(index);
        return is;
    }

    //根据类型返回正则
    public static String variableRegex(String typeRegex, String s) throws BusinessException {
        if (StringUtils.isBlank(s))
            throw new BusinessException("error ,variable example can only be underlined by alphanumeric", ErrorCode._10020);
        String regex = TypeEnum.matchTypeByRegex(typeRegex);
        if (Pattern.matches(regex, s)) {
            return s;
        } else {
            throw new BusinessException("error ,var type mismatch", ErrorCode._10021);
        }
    }

    //验证参数名字是否错误
    public static String varRegex(String s) throws BusinessException {
        if (Pattern.matches("^[a-zA-Z][a-zA-Z0-9_]*$+", s) && OpCodeEnum.matchCompilerVarOpCode(s) && !TypeEnum.matchType(s)) {
            return s;
        } else {
            throw new BusinessException("error ,variable example can only be underlined by alphanumeric", ErrorCode._10020);
        }
    }

    //验证参数名字是否错误
    public static String varReturnRegex(String s) throws BusinessException {
//        s = s.replace("\"", "");
        if (Pattern.matches("\\w+", s) && OpCodeEnum.matchCompilerVarOpCode(s) && !TypeEnum.matchType(s)) {
            return s;
        } else {
            throw new BusinessException("error ,variable example can only be underlined by alphanumeric", ErrorCode._10020);
        }
    }

    //验证{前字符是否错误
    public static boolean braRegex(String s) {
        return s.equals(")") || Pattern.matches("\\w+", s);
    }

    //方法校验
    public static void functionRegex(String functionContent, String context, String[] functionParam) throws Exception {
        functionContent = Pcompiler.getContent(functionContent, "{", "}");
        List<String> list = getSetList(context);
        List<String> events = getEventList(context);
        List<String> voteName = new ArrayList<>();
        List<String> eventVars = new ArrayList<>();
        for (String event : events) {
            if (StringUtils.substringBetween(event, "event", "(").trim().equals(EventEnum.vote.name())) {
                List<String> strings = Arrays.asList(StringUtils.substringBetween(event, "(", ")").split(","));
                for (String s : strings) {
                    eventVars.add(s.trim());
                }
                StringTokenizer stringTokenizer = new StringTokenizer(functionContent, " {};");
                while (stringTokenizer.hasMoreElements()) {
                    voteName.add(stringTokenizer.nextElement().toString());
                }
            }
        }
        Map<String, String> contentMap = new TreeMap<>();
        int i = 1;
        while (true) {
            String content = Pcompiler.readLine(functionContent.trim());
            if (StringUtils.isNotBlank(content)) {
                contentMap.put("content" + i, content);
                i++;
            }
            if (content == null) {
                throw new BusinessException("error,undefined contract", ErrorCode._10001);
            }
            if (content.equals("")) {
                content += ";";
            }
            functionContent = functionContent.trim().substring(content.length(), functionContent.trim().length());
            if (functionContent.trim().length() == 0)
                break;
        }
        for (int function_i = 1; function_i <= contentMap.keySet().size(); function_i++) {
            if (StringUtils.containsAny(contentMap.get("content" + function_i), new char[]{'{', '}'})) {
                if (OpCodeEnum.matchStartsWithIFCode(contentMap.get("content" + function_i))) {
                    ///取出空格里面的条件
                    String[] s = ArrayUtils.removeAllOccurences(Pcompiler.getContent(contentMap.get("content" + function_i), "(", ")").split(" "), "");
                    if (s.length > 3) {
                        throw new BusinessException("error,if条件参数有误", ErrorCode._10040);
                    }
                    //start 验证OpCodeEnum.if括号内的参数是否存在 判断条件是否合法
                    String functionCondition = StringUtils.substringBefore(contentMap.get("content" + function_i), "{");
                    String condition = OpCodeEnum.matchConditionOpCode(functionCondition);
                    if (StringUtils.isBlank(condition)) throw new Exception("error,if content condition too much");
                    String[] conditionVar = StringUtils.substringBetween(contentMap.get("content" + function_i), "(", ")").replace(" ", "").split(condition);
                    //获取类型
                    String varType = "";
                    //先往上找
                    for (String setList : list) {//获取类型
                        String[] sets = splitStr(setList);
                        if (sets[2].equals(conditionVar[0]) || sets[2].equals(conditionVar[1])) {
                            varType = sets[1];
                        }
                    }
                    if (StringUtils.isBlank(varType)) {
                        if (checkNum(conditionVar[0]) || checkNum(conditionVar[1])) {
                            varType = TypeEnum.num.name();
                        } else if (checkInt(conditionVar[0]) || checkInt(conditionVar[1])) {
                            varType = TypeEnum.integer.name();
                        } else if (checkTime(conditionVar[0]) || checkTime(conditionVar[1])) {
                            varType = TypeEnum.time.name();
                        } else if (conditionVar[0].equals(OpCodeEnum.nowTime.name()) || conditionVar[1].equals(OpCodeEnum.nowTime.name())) {
                            varType = TypeEnum.time.name();
                        } else if ((conditionVar[0] instanceof String && !StringUtils.containsAny(conditionVar[0], new char[]{'[', ']'})) || (conditionVar[1] instanceof String && !StringUtils.containsAny(conditionVar[1], new char[]{'[', ']'}))) {
                            varType = TypeEnum.str.name();
                        }
                    }
                    //验证左右两边参数
                    for (String var : conditionVar) {
                        String varType2 = null;
                        for (String setList : list) {
                            String[] sets = setList.split(" ");
                            if (var.equals(sets[2])) {
                                varType2 = sets[1];
                            } else {//数组单独处理
                                String eventArr = StringUtils.substringBefore(var, "[").trim();
                                String optionSize = StringUtils.substringBetween(var, "[", "]");
                                if (eventArr.equals(sets[2])) {
                                    if (Collections.frequency(eventVars, eventArr) != 1) {
                                        throw new BusinessException("error ,方法中的参数不存在事件中", ErrorCode._10034);
                                    }
                                    String[] setArrs = ArrayUtils.removeAllOccurences(Pcompiler.getContent(sets[4], "[", "]").split(","), "");
                                    if (Integer.parseInt(optionSize) >= setArrs.length) {
                                        throw new BusinessException("error ,数组参数不规范", ErrorCode._10039);
                                    }
                                    varType2 = TypeEnum.arr.name();
                                    if (var.replace(eventArr, "").replace("[" + optionSize + "]", "").equals(".count")) {
                                        varType2 = TypeEnum.integer.name();
                                    }
                                }
                            }
                        }
                        if (StringUtils.isBlank(varType2)) {
                            if (checkNum(var)) {
                                varType2 = TypeEnum.num.name();
                            } else if (checkInt(var)) {
                                varType2 = TypeEnum.integer.name();
                            } else if (checkTime(var)) {
                                varType2 = TypeEnum.time.name();
                            } else if (var.equals(OpCodeEnum.nowTime.name())) {
                                varType2 = TypeEnum.time.name();

                            } else if (var.contains("\"")) {
                                varType2 = TypeEnum.str.name();
                            }
                        }
                        if (!varType.equals(varType2)) {
                            throw new BusinessException("error ,类型不匹配", ErrorCode._10030);
                        }
                        //TODO
                        if (varType.equals(TypeEnum.str.name())) {
                            if (!(condition.equals(OpCodeEnum.EQ.key) || condition.equals(OpCodeEnum.NOT.key))) {
                                throw new BusinessException("error ,字符串比较只能等于和不等于 ", ErrorCode._10041);
                            }
                        }
                    }
                    functionContent(contentMap.get("content" + function_i));
                } else {
                    logger.info(contentMap.get("content" + function_i));
                    throw new BusinessException("error ,function if after condition is undefined", ErrorCode._10019);
                }
            } else if (StringUtils.startsWith(contentMap.get("content" + function_i).toLowerCase().trim(), OpCodeEnum.RETURN.name().toLowerCase())) {
                String[] returns = splitStr(contentMap.get("content" + function_i).trim());
                if (returns[0].equalsIgnoreCase(OpCodeEnum.RETURN.name())) {//验证是否以return结束
                    if (function_i != contentMap.keySet().size())
                        throw new BusinessException("error ,function end undefined", ErrorCode._10026);
                }
                if (returns.length != 2)//验证return行中有多少个参数
                    throw new BusinessException("error ,function end undefined", ErrorCode._10026);
                varReturnRegex(returns[1]);//验证return行中参数是否合法
                //验证return返回值是否是变量还是常量
                if (returns[1] instanceof String) {
                    System.out.println("return:字符" + returns[1]);
                } else if (Pattern.matches(TypeEnum.matchTypeByRegex("num"), returns[1])) {
                    System.out.println("return:数字" + returns[1]);
                } else {
                    checkFunctionBefore(context, contentMap.get("content" + function_i), functionParam, returns[1]);
                }
            } else if (StringUtils.startsWith(contentMap.get("content" + function_i).toLowerCase().trim(), OpCodeEnum.STOP.name().toLowerCase())) {
                //验证是否以stop结束
                if (function_i != contentMap.keySet().size()) {
                    throw new BusinessException("error ,function end undefined", ErrorCode._10026);
                }
                logger.error("直接停止");
            } else if (StringUtils.startsWith(contentMap.get("content" + function_i).trim(), "set ")) {
                if (StringUtils.containsAny(contentMap.get("content" + function_i), new char[]{'{', '}', '(', ')'}))
                    throw new BusinessException("error ,variable writing does not start with set", ErrorCode._10008);
                String[] aa = splitStr(contentMap.get("content" + function_i));
                String[] variable = splitStr(contentMap.get("content" + function_i));
                if (!variable[1].equals("coin") && variable.length >= 6)
                    throw new BusinessException("error ,variable writing is not standard", ErrorCode._10009);
                if (variable[1].equals("coin") && variable.length != 6)
                    throw new BusinessException("error ,variable coin writing is not standard", ErrorCode._10009);
                if (!TypeEnum.matchType(variable[1]))
                    throw new BusinessException("error ,variable type writing is undefined", ErrorCode._10010);//验证类型
                if (StringUtils.countMatches(contentMap.get("content" + function_i), "=") > 1)
                    throw new BusinessException("error ,variable writing is = not standard", ErrorCode._10010);//验证多少个=
                varRegex(variable[2]);//检验=左边 验证属性参数名字
                String temp = variableRegex(variable[1], variable[4]);//检验=右边
                eventVars.add(variable[2]);//添加方法里面存在的数组
                //验证局部变量是否存在在全局变量
                List<String> setList = getSetList(context);
                List<String> varList = new ArrayList<>();
                for (String var : setList) {
                    String[] vars = splitStr(var);
                    varList.add(vars[2]);
                }
                if (Collections.frequency(varList, variable[2]) != 1) {
                    throw new BusinessException("error ,写入重复", ErrorCode._10012);
                }
            } else {//不存在操作符时候
                String content = functionAssignment(context, contentMap.get("content" + function_i), functionParam);//验证赋值等操作一行
            }
        }
        //验证投票事件出现return报错
        if (Collections.frequency(voteName, OpCodeEnum.RETURN.name().toLowerCase()) > 0)
            throw new BusinessException("error,vote no return", ErrorCode._10032);
    }

    public static String getVarTypeList(String context, String var) {
        List<String> conditionList = new ArrayList<>();
        String varType = "";
        StringTokenizer stringTokenizer = new StringTokenizer(context, ", ;(){}");
        while (stringTokenizer.hasMoreElements()) {
            conditionList.add(stringTokenizer.nextElement().toString());
        }
        for (int condition_i = 0; condition_i < conditionList.size(); condition_i++) {
            if (conditionList.get(condition_i).equals(var)) {
                String s = conditionList.get(condition_i - 1);
                if (TypeEnum.matchType(s)) {
                    varType = s;
                }
            }
        }
        return varType;
    }

    /**
     * @param context       全文
     * @param content       方法的那一行
     * @param functionParam 方法参数
     * @param var           参数名
     */
    public static void checkFunctionBefore(String context, String content, String[] functionParam, String var) {
        String functionBefore = StringUtils.substringBeforeLast(context, content);//截取return 参数上
        //TODO 往上查找是否出现变量 截止到当前return 单词；
        List<String> functionRegexVars = getSetList(functionBefore);
        List<String> functionVarList = new ArrayList<>();
        for (String functionRegexVar : functionRegexVars) {
            functionVarList.add(splitStr(functionRegexVar)[2]);//将变量名存入list
        }
        int index = Collections.frequency(functionVarList, var);
        if (index < 1) {
            List<String> functionRegexVarLists = new ArrayList<>();
            for (String s : functionParam) {
                for (String par : splitStr(s)) {
                    functionRegexVarLists.add(par);
                }
            }
            if (Collections.frequency(functionRegexVarLists, var) < 1) {
                throw new BusinessException("error,function end undefind", ErrorCode._10026);
            }
        }
    }

    //获取到一行，判断是否存在赋值等操作
    public static String functionAssignment(String context, String content, String[] functionParam) throws BusinessException {
        if (StringUtils.containsAny(content, new char[]{OpCodeEnum.SUB.key.toCharArray()[0], OpCodeEnum.DIV.key.toCharArray()[0]
                , OpCodeEnum.SEQ.key.toCharArray()[0], OpCodeEnum.MUL.key.toCharArray()[0]})) {
            System.out.println("-------赋值等操作-----");
            List<String> list = getSetList(context);
            String[] vals = ArrayUtils.removeAllOccurences(splitStr(content), "");
            //第一个一定是参数
            if (!TypeEnum.checkLetterLine(vals[0]))
                throw new BusinessException("Assignment operation parameter type error", ErrorCode._10028);
            if (!vals[1].equalsIgnoreCase("="))
                throw new BusinessException("Assignment operation parameter type error", ErrorCode._10028);
            String varType = getVarTypeList(context, vals[0]);//获取第一个参数的类型
            if (StringUtils.isBlank(varType))
                throw new BusinessException("Assignment operation parameter type error", ErrorCode._10028);
            String reg = TypeEnum.matchTypeByRegex(varType);
            List<String> assignmentList = new ArrayList<>();
            StringTokenizer stAssignment = new StringTokenizer(content, "+=-*/");
            while (stAssignment.hasMoreElements()) {
                assignmentList.add(stAssignment.nextElement().toString().trim());
            }
            Map<String, Integer> map = new HashMap<>();
            map.put(TypeEnum.str.name(), 0);
            map.put(TypeEnum.num.name(), 0);
            for (String var : assignmentList) {
//                if (var.contains("\"")) {
                if (var instanceof String) {
                    map.put(TypeEnum.str.name(), map.get(TypeEnum.str.name()) + 1);
                } else if (checkNum(var)) {
                    map.put(TypeEnum.num.name(), map.get(TypeEnum.num.name()) + 1);
                } else {
                    for (String setVar : list) {
                        String[] setVars = splitStr(setVar);
                        if (var.equals(setVars[2])) {
                            map.put(setVars[1], map.get(setVars[1]) + 1);
                        }
                    }
                }
            }
            if (!(map.get(TypeEnum.str.name()) == assignmentList.size() || map.get(TypeEnum.num.name()) == assignmentList.size())) {
                throw new BusinessException("error,赋值类型不匹配", ErrorCode._10033);
            }
        } else {
            throw new BusinessException("error ,function operation undefined", ErrorCode._10027);
        }
        return content;
    }

    //校验方法{}里内容遇return返回
    public static void functionContent(String functionContent) throws BusinessException {
        functionContent = Pcompiler.getContent(functionContent, "{", "}");
        Map<String, String> contentMap = new TreeMap<>();
        int i = 1;
        while (true) {
            String content = Pcompiler.readLine(functionContent.trim());
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
        //校验方法返回
        for (int function_i = 1; function_i <= contentMap.keySet().size(); function_i++) {
            if (StringUtils.startsWith(contentMap.get("content" + function_i).trim(), OpCodeEnum.RETURN.name().toLowerCase())) {
                if (function_i != contentMap.keySet().size())
                    throw new BusinessException("error ,function return undefined", ErrorCode._10020);
                String[] returns = splitStr(contentMap.get("content" + function_i).trim());
                if (returns[0].equalsIgnoreCase(OpCodeEnum.RETURN.name())) {//验证是否以return结束
                    if (function_i != contentMap.keySet().size())
                        throw new BusinessException("error ,function end undefined", ErrorCode._10026);
                }
                if (returns.length != 2)//验证return行中有多少个参数
                    throw new BusinessException("error ,function end undefined", ErrorCode._10026);
                varReturnRegex(returns[1]);//验证return行中参数是否合法
            } else if (StringUtils.startsWith(contentMap.get("content" + function_i).trim(), OpCodeEnum.STOP.name().toLowerCase())) {
                if (function_i != contentMap.keySet().size())
                    throw new BusinessException("error ,function end undefined", ErrorCode._10020);
            } else {
                logger.error("条件内容返回错误");
                throw new BusinessException("error ,function end undefined", ErrorCode._10031);
            }
        }
    }

    public static boolean checkStr(String str) {
//        String regex = "^(?!_)(?!.*?_$)[a-zA-Z0-9_\\u4e00-\\u9fa5]+$";
        String regex = "^\"([^\"]*)\"$";
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
//        String regex = "\\[([(?!_)(?!:;.*?_$)[a-zA-Z0-9_\\u4e00-\\u9fa5]+]|,[(?!_)(?!:;.*?_$)[a-zA-Z0-9_\\u4e00-\\u9fa5]+])+\\]";
        String regex = "\\[(\"([^\"]*)\"|,\"([^\"]*)\")+\\]";
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
        return Pcompiler.getMember(str, "(set[\\s]+[^;]*)");
    }

    public static List getFunctionList(String str) {
        return Pcompiler.getMember(str, "(function[\\s]+[^{]*)");
    }

    public static List getStopList(String str) {
        return Pcompiler.getMember(str, "(stop[\\s]*[^;]*)");
    }

    public static List getReturnList(String str) {
        return Pcompiler.getMember(str, "(return[\\s]+[^;]*)");
    }

    public static List getEventList(String str) {
        return Pcompiler.getMember(str, "(event[\\s]+[^;]*)");
    }

    public static String[] splitStr(String str) {
        Pattern p = Pattern.compile("(\"(?<=\\s\").*?(?=\")\")|([[\\u4e00-\\u9fa5]|\\w|=|<|>|,]+)");
        Matcher m = p.matcher(str);
        List<String> list = new ArrayList<>();
        while (m.find()) {
            list.add(m.group());
        }
        String[] array = list.toArray(new String[list.size()]);
        return array;
    }
    public static String[] splitCommaStr(String str) {
        Pattern p = Pattern.compile("(\"(?<=\").*?(?=\")\")");
        Matcher m = p.matcher(str);
        List<String> list = new ArrayList<>();
        while (m.find()) {
            list.add(m.group());
        }
        for(int i = 0 ;i<list.size();i++){
            str = str.replace(list.get(i),"!_"+i);
        }
        String[] arrays = str.split(",");
        for(String s : arrays){
            if(!s.contains("!_")) list.add(s);
        }
        String[] array = list.toArray(new String[list.size()]);
        return array;
    }

    public static List<String> getIfList(String content) {
        List<String> ls = new ArrayList<String>();
        Pattern pattern = Pattern.compile("(?<=if\\()(.+?)(?=\\))");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find())
            ls.add(matcher.group());
        return ls;
    }

    public static void main(String[] args) {
        String s =  "pcompiler version 1.0.0 ; \n" +
                "contract demo{\n" +
                "set str title = \"主题@#$@$@%#%@#，。、，、，。2425435\";\n" +
                "set arr options = [1,\"选 $@%#%@#，。  项2\",\",,,,,,,,,,,,\",\"\"]; \n" +
                "set time t = \"2019-10-02 00:00:00\";\n" +
                "function jiaoyan () {\n" +
                "if(options[0].count>2) {stop;}\n" +
                "if(options[1].count>=3){stop;}\n" +
                "if(options[2].count==4){stop;}\n" +
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