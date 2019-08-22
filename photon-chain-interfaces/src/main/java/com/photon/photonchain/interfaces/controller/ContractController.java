package com.photon.photonchain.interfaces.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.photon.photonchain.exception.BusinessException;
import com.photon.photonchain.extend.Universal.EventEnum;
import com.photon.photonchain.extend.compiler.MyClassLoader;
import com.photon.photonchain.extend.compiler.Pcompiler;
import com.photon.photonchain.extend.compiler.RegexCompile;
import com.photon.photonchain.extend.vm.Heap;
import com.photon.photonchain.extend.vm.PVM;
import com.photon.photonchain.extend.vm.Program;
import com.photon.photonchain.interfaces.utils.*;
import com.photon.photonchain.network.ehcacheManager.AssetsManager;
import com.photon.photonchain.network.ehcacheManager.InitializationManager;
import com.photon.photonchain.network.ehcacheManager.NioSocketChannelManager;
import com.photon.photonchain.network.ehcacheManager.UnconfirmedTranManager;
import com.photon.photonchain.network.proto.InesvMessage;
import com.photon.photonchain.network.proto.MessageManager;
import com.photon.photonchain.network.utils.CheckVerify;
import com.photon.photonchain.network.utils.TokenUtil;
import com.photon.photonchain.storage.constants.Constants;
import com.photon.photonchain.storage.encryption.ECKey;
import com.photon.photonchain.storage.encryption.SHAEncrypt;
import com.photon.photonchain.storage.entity.Token;
import com.photon.photonchain.storage.entity.Transaction;
import com.photon.photonchain.storage.entity.UnconfirmedTran;
import com.photon.photonchain.storage.repository.BlockRepository;
import com.photon.photonchain.storage.repository.TokenRepository;
import com.photon.photonchain.storage.repository.TransactionRepository;
import com.photon.photonchain.storage.utils.ContractUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.photon.photonchain.network.proto.EventTypeEnum.EventType.*;
import static com.photon.photonchain.network.proto.MessageTypeEnum.MessageType.RESPONSE;


/**
 * Created by Administrator on 2018/4/25.
 */
@RestController
@RequestMapping("contract")
public class ContractController {

    private static Logger logger = LoggerFactory.getLogger(ContractController.class);

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private InitializationManager initializationManager;

    @Autowired
    NioSocketChannelManager nioSocketChannelManager;


    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private PVM pvm;

    @Autowired
    private Program program;

    @Autowired
    private BlockRepository blockRepository;

    @Autowired
    private AssetsManager assetsManager;

    @Autowired
    private UnconfirmedTranManager unconfirmedTranManager;

    /**
     * 调用交易合同
     */
    @PostMapping("/transaction")
    public Res transaction(String contractAddress, String fromAddress, String pwd) {
        try {
            logger.info("合同地址{[]},钱包地址{[]}", contractAddress, fromAddress);
            //查询缓存里面是否有合约地址保存在缓存
            if (initializationManager.getContract(contractAddress) != null) return new Res(Res.CODE_132, null);
            if (initializationManager.getCancelContract(contractAddress) != null)
                return new Res(Res.CODE_141, null);

            if ("".equals(contractAddress) || contractAddress == null) return new Res(Res.CODE_133, null);
            if ("".equals(fromAddress) || fromAddress == null) return new Res(Res.CODE_134, null);
            if ("".equals(pwd) || pwd == null) return new Res(Res.CODE_135, null);

            Transaction transaction = transactionRepository.findByContractAddress(contractAddress, 0, 3);
            if (transaction == null) return new Res(Res.CODE_136, null);

            String bin = transaction.getContractBin();
            program.setBin(bin);
            program.analysisAndPushHeap();//解析bin

            Map<String, String> localAccount = initializationManager.getAccountListByAddress(fromAddress);
            String pwdFrom = localAccount.get(Constants.PWD);
            String pubkeyFrom = localAccount.get(Constants.PUBKEY);

            if (pubkeyFrom == null || "".equals(pubkeyFrom)) return new Res(Res.CODE_137, null);

            if (!pwd.equals(pwdFrom)) {
                return new Res(Res.CODE_301, null);
            }

            Map<String, Object> map = new HashMap<>();
            map.put("fromAddress", pubkeyFrom);//将要交易的钱包地址放入map
            map.put("event", "business");
            map.put("contractAddress", transaction.getTransTo());

            program.analysisAndGetOpcode(map);
            pvm.step(program);
            if ((int) program.getResult() == 3) {
                return new Res(Res.CODE_106, null);
            } else if ((int) program.getResult() == 1) {

                //将将要被兑换的地址广播出去
                InesvMessage.Message.Builder builder = MessageManager.createMessageBuilder(RESPONSE, NEW_CONTRACT);
                builder.setContractAddresss(contractAddress);
                List<String> hostList = nioSocketChannelManager.getChannelHostList();
                builder.addAllNodeAddressList(hostList);
                nioSocketChannelManager.write(builder.build());

                //现在本地保存，防止多次点击
                initializationManager.setContract(contractAddress);
                return new Res(Res.CODE_139, null);
            } else {
                return new Res(Res.CODE_500, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Res(Res.CODE_500, null);
        }
    }


    /**
     * 取消合约
     */
    @PostMapping("/cancelContract")
    public Res cancelContract(String contractAddress, String password) {
        try {
            if (initializationManager.getContract(contractAddress) != null) return new Res(Res.CODE_142, null);
            if (initializationManager.getCancelContract(contractAddress) != null)
                return new Res(Res.CODE_141, null);

            logger.info("合同地址{[]}", contractAddress);
            if ("".equals(contractAddress) || contractAddress == null) return new Res(Res.CODE_133, null);
            if ("".equals(password) || password == null) return new Res(Res.CODE_135, null);

            List<String> hostList = nioSocketChannelManager.getChannelHostList();

            //将将要取消的合约广播出去
            InesvMessage.Message.Builder cancelBuilder = MessageManager.createMessageBuilder(RESPONSE, IS_CANCEL);
            cancelBuilder.setContractAddresss(contractAddress);
            cancelBuilder.addAllNodeAddressList(hostList);
            nioSocketChannelManager.write(cancelBuilder.build());

            Transaction transaction = transactionRepository.findByContractAddress(contractAddress, 0, 3);
            if (transaction == null) return new Res(Res.CODE_136, null);

            Map<String, String> localAccount = initializationManager.getAccountListByAddress(ECKey.pubkeyToAddress(transaction.getTransFrom()));
            String fromPassword = localAccount.get(Constants.PWD);//获取到账号交易密码

            if (!fromPassword.equals(password)) return new Res(Res.CODE_301, null);

            String bin = transaction.getContractBin();
            program.setBin(bin);
            program.analysisAndPushHeap();//解析bin

            Map<String, Object> map = new HashMap<>();
            map.put("event", "cancel");
            map.put("contractAddress", transaction.getTransTo());

            program.analysisAndGetOpcode(map);
            pvm.step(program);

            //现在本地保存，防止多次点击
            initializationManager.setCancelContract(contractAddress);

            return new Res(Res.CODE_138, null);
        } catch (Exception e) {
            e.printStackTrace();
            return new Res(Res.CODE_500, null);
        }
    }


    /**
     * 投票合约
     */
    @PostMapping("/vote")
    public Res vote(String contractAddress, String fromAddress, String password, String chose) {
        try {
            if (initializationManager.getVoteContract(contractAddress, fromAddress) != null)
                return new Res(Res.CODE_147, null);

            logger.info("合同地址{[]}", contractAddress);
            if ("".equals(contractAddress) || contractAddress == null) return new Res(Res.CODE_133, null);
            if ("".equals(password) || password == null) return new Res(Res.CODE_135, null);
            if ("".equals(chose) || chose == null) return new Res(Res.CODE_144, null);
            if ("".equals(fromAddress) || fromAddress == null) return new Res(Res.CODE_134, null);

            Transaction transaction = transactionRepository.findByContractAddress(contractAddress, 0, 3);
            if (transaction == null) return new Res(Res.CODE_146, null);

            Map<String, String> localAccount = initializationManager.getAccountListByAddress(fromAddress);
            String pwdFrom = localAccount.get(Constants.PWD);
            String pubkeyFrom = localAccount.get(Constants.PUBKEY);

            if (pubkeyFrom == null || "".equals(pubkeyFrom)) return new Res(Res.CODE_137, null);

            if (!password.equals(pwdFrom)) {
                return new Res(Res.CODE_301, null);
            }

            //判断是否已经投过票
            int hasTran = transactionRepository.findVoteContract(contractAddress, pubkeyFrom, 7);
            if (hasTran > 0) return new Res(Res.CODE_147, null);

            List<UnconfirmedTran> tranList = unconfirmedTranManager.queryUnconfirmedTran(contractAddress, -1, fromAddress, transaction.getTransTo(), -1, chose);
            if (tranList.size() > 0) return new Res(Res.CODE_147, null);

            String bin = transaction.getContractBin();
            program.setBin(bin);
            program.analysisAndPushHeap();//解析bin

            Map<String, Object> map = new HashMap<>();
            map.put("fromAddress", pubkeyFrom);//将要交易的钱包地址放入map
            map.put("function", "vote");
            map.put("contractAddress", transaction.getTransTo());
            //map.put("chose", StringUtils.substring(chose, 2, chose.length()));
            map.put("chose", chose.trim());
            map.put("bin", transaction.getContractBin());//合约bin
            program.analysisAndGetOpcode(map);

            pvm.step(program);
            if ((int) program.getResult() == 3) {
                return new Res(Res.CODE_106, null);
            } else if ((int) program.getResult() == 1) {
                //现在本地保存，防止多次点击
                initializationManager.setVoteContract(contractAddress, fromAddress);
                return new Res(Res.CODE_145, null);
            } else if ((int) program.getResult() == 2) {
                return new Res(Res.CODE_146, null);
            } else {
                return new Res(Res.CODE_148, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Res(Res.CODE_500, null);
        }
    }


    /**
     * 代币合约
     */
    @PostMapping("/createToken")
    public Res createToken(String contractAddress, String password) {
        try {
            if (initializationManager.getContract(contractAddress) != null) return new Res(Res.CODE_149, null);

            if ("".equals(contractAddress) || contractAddress == null) return new Res(Res.CODE_133, null);
            if ("".equals(password) || password == null) return new Res(Res.CODE_135, null);

            Transaction transaction = transactionRepository.findByContractAddress(contractAddress, 0, 3);
            if (transaction == null) return new Res(Res.CODE_136, null);

            Map<String, String> localAccount = initializationManager.getAccountListByAddress(ECKey.pubkeyToAddress(transaction.getTransFrom()));
            String pwdFrom = localAccount.get(Constants.PWD);
            String pubkeyFrom = localAccount.get(Constants.PUBKEY);

            if (pubkeyFrom == null || "".equals(pubkeyFrom)) return new Res(Res.CODE_137, null);

            if (!password.equals(pwdFrom)) {
                return new Res(Res.CODE_301, null);
            }

            String bin = transaction.getContractBin();
            program.setBin(bin);
            program.analysisAndPushHeap();//解析bin

            Map<String, Object> map = new HashMap<>();
            map.put("fromAddress", pubkeyFrom);//将要交易的钱包地址放入map
            map.put("contractAddress", contractAddress);//合约地址
            map.put("bin", bin);//bin
            map.put("event", "TOKEN");
            program.analysisAndGetOpcode(map);

            pvm.step(program);
            if ((int) program.getResult() == 2) {
                return new Res(Res.CODE_123, null);
            } else {
                initializationManager.setContract(contractAddress);
                return new Res(Res.CODE_150, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Res(Res.CODE_500, null);
        }
    }


    /**
     * 其他合约列表及其方法参数获取
     */
    @PostMapping("/getOrtherContract")
    public Res getOrtherContractMethodAndField(PageObject pageObject) {
        try {
            Res res = new Res();
            int count = transactionRepository.findContractCount(3, 4, 0);//Java合约总数
            int start = (count - pageObject.getPageNumber() * pageObject.getPageSize()) < 0 ? 0 : count - pageObject.getPageNumber() * pageObject.getPageSize();

            pageObject.setSumRecord(count);
            List<Transaction> transactions = transactionRepository.findContract(3, 4, 0, start, pageObject.getPageSize());

            List<Object> dataList = new ArrayList<>();

            Map resultMap = new LinkedHashMap();
            for (Transaction transaction : transactions) {
                Map<String, Object> map = new HashMap<>();
                map.put("contractAddress", transaction.getContractAddress());
                map.put("issuer", ECKey.pubkeyToAddress(transaction.getTransFrom()));
                MyClassLoader myClassLoader = new MyClassLoader();
                List<Class> list = myClassLoader.binToClass(transaction.getContractBin());
                String regex = "[^$]+";//获取主类
                for (Class clazz : list) {
                    if (clazz.getName().matches(regex)) {
                        for (Class c : list) {
                            if (!c.getName().matches(regex)) {
                                clazz.getClassLoader().loadClass(c.getName());
                            }
                        }
                        map.put("className", StringUtils.substringAfterLast(clazz.getName(), ".") != "" ? StringUtils.substringAfterLast(clazz.getName(), ".") : clazz.getName());//类名

                        Method[] methods = clazz.getDeclaredMethods();//获取到类里面的方法

                        int i = 1;
                        List<Object> list1 = new ArrayList<>();
                        for (Method method : methods) {
                            List<Object> keyList = new ArrayList<>();
                            Map<String, Object> methodMap = new HashMap<>();

                            String methodName = method.getName();
                            Parameter[] parameter = method.getParameters();//获取到方法参数
                            //String[] parmasArr = StringUtils.substringBetween(method.toString(), "(", ")").split(",");
                            //获取本方法所有参数类型
                            //Class<?>[] getTypeParameters = method.getParameterTypes();
                            if (parameter.length == 0) {//没有参数
                                map.put(methodName, null);
                            }
                            if (map.get(methodName) != null) {//说明已有同名方法
                                for (Parameter p : parameter) {
                                    Map<String, Object> parmasMap = new HashMap<>();
                                    parmasMap.put("keyname", p.getName());//变量名
                                    String str = StringUtils.substringBeforeLast(p.toString(), " ").replace("java.util.", "").replace("java.lang.", "");
                                    parmasMap.put("keytype", str);//参数类型

                                    //数字类型
                                    if (str.equals("byte") || str.equals("short") || str.equals("int") || str.equals("long") ||
                                            str.equals("Byte") || str.equals("Short") || str.equals("Integer") || str.equals("Long")) {
                                        parmasMap.put("keyval", 1);
                                    }
                                    //浮点型
                                    if (str.equals("float") || str.equals("Float")) {
                                        parmasMap.put("keyval", 3.14f);
                                    }
                                    if (str.equals("double") || str.equals("Double")) {
                                        parmasMap.put("keyval", 3.14);
                                    }
                                    //布尔型
                                    if (str.equals("boolean") || str.equals("Boolean")) {
                                        parmasMap.put("keyval", true);
                                    }
                                    //字符型
                                    if (str.equals("String") || str.equals("StringBuffer") || str.equals("StringBuilder")) {
                                        parmasMap.put("keyval", "hello world");
                                    }
                                    //数组
                                    if (str.equals("byte[]") || str.equals("short[]") || str.equals("int[]") || str.equals("long[]") ||
                                            str.equals("Byte[]") || str.equals("Short[]") || str.equals("Integer[]") || str.equals("Long[]")) {
                                        parmasMap.put("keyval", "[1,2,3]");
                                    }
                                    if (str.equals("double[]") || str.equals("Double[]")) {
                                        parmasMap.put("keyval", "[3.14,1.23]");
                                    }
                                    if (str.equals("float[]") || str.equals("Float[]")) {
                                        parmasMap.put("keyval", "[3.14f,1.23f]");
                                    }
                                    if (str.equals("boolean[]") || str.equals("Boolean[]")) {
                                        parmasMap.put("keyval", "[true,false]");
                                    }
                                    if (str.equals("String[]") || str.equals("StringBuffer[]") || str.equals("StringBuilder[]")) {
                                        parmasMap.put("keyval", "[\"hello world\",\"hello java\"]");
                                    }
                                    //list
                                    if (str.equals("List") || str.equals("Set") || str.startsWith("ArrayList")
                                            || str.startsWith("LinkedHashSet") || str.equals("LinkedHashSet")
                                            || str.startsWith("LinkedList") || str.startsWith("HashSet")) {
                                        parmasMap.put("keyval", "[AAA,BBB]");
                                    }
                                    //Map
                                    if (str.equals("Map<String, Object>")) {
                                        parmasMap.put("keyval", "{\"key\":\"value\",\"key2\":\"value2\"}");
                                    }
                                    if (str.equals("Map<String, Map<String, Object>>")) {
                                        parmasMap.put("keyval", "{\"key\": \"value\",\"key2\": {\"key3 \": \"value3\"}}");
                                    }
                                    if(str.startsWith("HashMap") || str.startsWith("Hashtable")
                                            || str.startsWith("LinkedHashMap")){
                                        parmasMap.put("keyval", "{\"key\":\"value\",\"key2\":\"value2\"}");
                                    }
                                    keyList.add(parmasMap);
                                }
                                methodMap.put("name", methodName + i);//函数名
                                i++;
                                continue;
                            }
                            for (Parameter p : parameter) {
                                Map<String, Object> parmasMap = new HashMap<>();
                                parmasMap.put("keyname", p.getName());//变量名
                                String str = StringUtils.substringBeforeLast(p.toString(), " ").replace("java.util.", "").replace("java.lang.", "");
                                parmasMap.put("keytype", str);//参数类型
                                //数字类型
                                if (str.equals("byte") || str.equals("short") || str.equals("int") || str.equals("long") ||
                                        str.equals("Byte") || str.equals("Short") || str.equals("Integer") || str.equals("Long")) {
                                    parmasMap.put("keyval", 1);
                                }
                                //浮点型
                                if (str.equals("float") || str.equals("Float")) {
                                    parmasMap.put("keyval", 3.14f);
                                }
                                if (str.equals("double") || str.equals("Double")) {
                                    parmasMap.put("keyval", 3.14);
                                }
                                //布尔型
                                if (str.equals("boolean") || str.equals("Boolean")) {
                                    parmasMap.put("keyval", true);
                                }
                                //字符型
                                if (str.equals("String") || str.equals("StringBuffer") || str.equals("StringBuilder")) {
                                    parmasMap.put("keyval", "hello world");
                                }
                                //数组
                                if (str.equals("byte[]") || str.equals("short[]") || str.equals("int[]") || str.equals("long[]") ||
                                        str.equals("Byte[]") || str.equals("Short[]") || str.equals("Integer[]") || str.equals("Long[]")) {
                                    parmasMap.put("keyval", "[1,2,3]");
                                }
                                if (str.equals("double[]") || str.equals("Double[]")) {
                                    parmasMap.put("keyval", "[3.14,1.23]");
                                }
                                if (str.equals("float[]") || str.equals("Float[]")) {
                                    parmasMap.put("keyval", "[3.14f,1.23f]");
                                }
                                if (str.equals("String[]") || str.equals("StringBuffer[]") || str.equals("StringBuilder[]")) {
                                    parmasMap.put("keyval", "[\"hello world\",\"hello java\"]");
                                }
                                if (str.equals("boolean[]") || str.equals("Boolean[]")) {
                                    parmasMap.put("keyval", "[true,false]");
                                }
                                //list
                                if (str.equals("List") || str.equals("Set") || str.startsWith("ArrayList")
                                        || str.startsWith("LinkedHashSet") || str.equals("LinkedHashSet")
                                        || str.startsWith("LinkedList") || str.startsWith("HashSet")) {
                                    parmasMap.put("keyval", "[AAA,BBB]");
                                }
                                //Map
                                if (str.equals("Map<String, Object>")) {
                                    parmasMap.put("keyval", "{\"key\":\"value\",\"key2\":\"value2\"}");
                                }
                                if (str.equals("Map<String, Map<String, Object>>")) {
                                    parmasMap.put("keyval", "{\"key\": \"value\",\"key2\": {\"key3 \": \"value3\"}}");
                                }
                                if(str.startsWith("HashMap") || str.startsWith("Hashtable")
                                        || str.startsWith("LinkedHashMap")){
                                    parmasMap.put("keyval", "{\"key\":\"value\",\"key2\":\"value2\"}");
                                }
                                keyList.add(parmasMap);
                            }
                            methodMap.put("name", methodName);//函数名
                            if (StringUtils.contains(method.getGenericReturnType().getTypeName(), ".")) {
                                methodMap.put("returnType", method.getGenericReturnType().getTypeName().replace("java.util.", "").replace("java.lang.", ""));
                            } else {
                                methodMap.put("returnType", method.getGenericReturnType().getTypeName());
                            }
                            methodMap.put("keys", keyList);
                            list1.add(methodMap);
                        }
                        map.put("func", list1);
                        dataList.add(map);
                    }
                }
            }
            resultMap.put("funcData", dataList);
            resultMap.put("pageNumber", pageObject.getPageNumber());
            resultMap.put("count", count);
            res.setCode(Res.CODE_100);
            res.setData(resultMap);
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return new Res(Res.CODE_500, null);
        }
    }


    /**
     * 执行代码
     */
    @PostMapping("invoke")
    public Res invoke(String contractAddress, String methodName, String typeArr, String valueArr) {
        try {
            if ("".equals(contractAddress) || contractAddress == null) return new Res(Res.CODE_133, null);
            if ("".equals(methodName) || methodName == null) return new Res(Res.CODE_133, null);//TODO

            JSONArray typeJsonArray = JSON.parseArray(typeArr);
            JSONArray valueJsonArray = JSON.parseArray(valueArr);

            if (typeJsonArray.size() != valueJsonArray.size()) return new Res(Res.CODE_154, "传递参数不对");

            Transaction transaction = transactionRepository.findByContractAddress(contractAddress, 0, 3);
            if (transaction == null) return new Res(Res.CODE_136, null);

            MyClassLoader myClassLoader = new MyClassLoader();
            List<Class> list = myClassLoader.binToClass(transaction.getContractBin());

            String regex = "[^$]+";//获取主类
            for (Class clazz : list) {
                if (clazz.getName().matches(regex)) {
                    for (Class c : list) {
                        if (!c.getName().matches(regex)) {
                            clazz.getClassLoader().loadClass(c.getName());
                        }
                    }
                    Method[] methods = clazz.getDeclaredMethods();//获取到类里面的方法
                    for (Method method : methods) {
                        String getMethodName = method.getName();
                        Class<?>[] getTypeParameters = method.getParameterTypes();
                        if (methodName.equals(getMethodName) && getTypeParameters.length == typeJsonArray.size() && getTypeParameters.length > 0) {//判断调用方法
                            int i = 0;
                            boolean isMethod = true;
                            Parameter[] parmasArr = method.getParameters();//获取到方法参数
                            for (Parameter p : parmasArr) {
                                String str = StringUtils.substringBeforeLast(p.toString(), " ").replace("java.util.", "").replace("java.lang.", "");
                                if (!typeJsonArray.get(i).toString().replace("\"", "").equals(str)) {
                                    isMethod = false;
                                    continue;
                                }
                                i++;
                            }
                            if (isMethod) {
                                Object[] objects = new Object[typeJsonArray.size()];
                                Method invokeMethod = clazz.getMethod(getMethodName, getTypeParameters);//要执行的方法
                                for (int j = 0; j < typeJsonArray.size(); j++) {
                                    String type = typeJsonArray.get(j).toString().replace("\"", "");
                                    if (type.equals("byte") || type.equals("Byte")) {
                                        Byte value = new Byte(valueJsonArray.get(j).toString());
                                        objects[j] = value;
                                    }
                                    if (type.equals("short") || type.equals("Short")) {
                                        Short value = new Short(valueJsonArray.get(j).toString());
                                        objects[j] = value;
                                    }
                                    if (type.equals("int") || type.equals("Integer")) {
                                        Integer value = new Integer(valueJsonArray.get(j).toString());
                                        objects[j] = value;
                                    }
                                    if (type.equals("long") || type.equals("Long")) {
                                        Long value = new Long(valueJsonArray.get(j).toString());
                                        objects[j] = value;
                                    }
                                    if (type.equals("float") || type.equals("Float")) {
                                        Float value = new Float(valueJsonArray.get(j).toString());
                                        objects[j] = value;
                                    }
                                    if (type.equals("double") || type.equals("Double")) {
                                        Double value = new Double(valueJsonArray.get(j).toString());
                                        objects[j] = value;
                                    }
                                    if (type.equals("boolean") || type.equals("Boolean")) {
                                        Boolean value = new Boolean(valueJsonArray.get(j).toString());
                                        objects[j] = value;
                                    }
                                    //TODO char
                                    if (type.equals("String")) {
                                        String value = new String(valueJsonArray.get(j).toString());
                                        objects[j] = value;
                                    }
                                    if (type.equals("byte[]") || type.equals("Byte[]")) {
                                        String[] strArr = valueJsonArray.get(j).toString().split(",");
                                        Byte[] value = new Byte[strArr.length];
                                        for (int k = 0; k < strArr.length; k++) {
                                            Byte b = new Byte(strArr[k]);
                                            value[k] = b;
                                        }
                                        objects[j] = value;
                                    }
                                    if (type.equals("short[]") || type.equals("Short[]")) {
                                        String[] strArr = valueJsonArray.get(j).toString().split(",");
                                        Short[] value = new Short[strArr.length];
                                        for (int k = 0; k < strArr.length; k++) {
                                            Short b = new Short(strArr[k]);
                                            value[k] = b;
                                        }
                                        objects[j] = value;
                                    }
                                    if (type.equals("int[]") || type.equals("Integer[]")) {
                                        JSONArray strArr = JSON.parseArray(valueJsonArray.get(j).toString());
                                        Integer[] value = new Integer[strArr.size()];
                                        for (int k = 0; k < strArr.size(); k++) {
                                            Integer b = new Integer(strArr.get(k).toString());
                                            value[k] = b;
                                        }
                                        objects[j] = value;
                                    }
                                    if (type.equals("long[]") || type.equals("Long[]")) {
                                        JSONArray strArr = JSON.parseArray(valueJsonArray.get(j).toString());
                                        Long[] value = new Long[strArr.size()];
                                        for (int k = 0; k < strArr.size(); k++) {
                                            Long b = new Long(strArr.get(k).toString());
                                            value[k] = b;
                                        }
                                        objects[j] = value;
                                    }
                                    if (type.equals("float[]") || type.equals("Float[]")) {
                                        JSONArray strArr = JSON.parseArray(valueJsonArray.get(j).toString());
                                        Float[] value = new Float[strArr.size()];
                                        for (int k = 0; k < strArr.size(); k++) {
                                            Float b = new Float(strArr.get(k).toString());
                                            value[k] = b;
                                        }
                                        objects[j] = value;
                                    }
                                    if (type.equals("double[]") || type.equals("Double[]")) {
                                        JSONArray strArr = JSON.parseArray(valueJsonArray.get(j).toString());
                                        Double[] value = new Double[strArr.size()];
                                        for (int k = 0; k < strArr.size(); k++) {
                                            Double b = new Double(strArr.get(k).toString());
                                            value[k] = b;
                                        }
                                        objects[j] = value;
                                    }
                                    if (type.equals("boolean[]") || type.equals("Boolean[]")) {
                                        JSONArray strArr = JSON.parseArray(valueJsonArray.get(j).toString());
                                        Boolean[] value = new Boolean[strArr.size()];
                                        for (int k = 0; k < strArr.size(); k++) {
                                            Boolean b = new Boolean(strArr.get(k).toString());
                                            value[k] = b;
                                        }
                                        objects[j] = value;
                                    }
                                    //TODO char
                                    if (type.equals("String[]")) {
                                        JSONArray strArr = JSON.parseArray(valueJsonArray.get(j).toString());
                                        String[] value = new String[strArr.size()];
                                        for (int k = 0; k < strArr.size(); k++) {
                                            String b = new String(strArr.get(k).toString());
                                            value[k] = b;
                                        }
                                        objects[j] = value;
                                    }
                                    if (type.equals("List")) {
                                        JSONArray listValue = JSON.parseArray(valueJsonArray.get(j).toString());
                                        List<Object> value = new ArrayList<>();
                                        for (Object o : listValue) {
                                            value.add(o);
                                        }
                                        objects[j] = value;
                                    }
                                    if (type.equals("Set")) {
                                        JSONArray listValue = JSON.parseArray(valueJsonArray.get(j).toString());
                                        Set<Object> value = new HashSet<>();
                                        for (Object o : listValue) {
                                            value.add(o);
                                        }
                                    }
                                    if(type.equals("ArrayList")){
                                        JSONArray listValue = JSON.parseArray(valueJsonArray.get(j).toString());
                                        ArrayList<Object> value = new ArrayList<>();
                                        for (Object o : listValue) {
                                            value.add(o);
                                        }
                                    }
                                    if(type.equals("LinkedHashSet")){
                                        JSONArray listValue = JSON.parseArray(valueJsonArray.get(j).toString());
                                        LinkedHashSet<Object> value = new LinkedHashSet<>();
                                        for (Object o : listValue) {
                                            value.add(o);
                                        }
                                    }
                                    if(type.equals("LinkedList")){
                                        JSONArray listValue = JSON.parseArray(valueJsonArray.get(j).toString());
                                        LinkedList<Object> value = new LinkedList<>();
                                        for (Object o : listValue) {
                                            value.add(o);
                                        }
                                    }
                                    if(type.equals("HashSet")){
                                        JSONArray listValue = JSON.parseArray(valueJsonArray.get(j).toString());
                                        HashSet<Object> value = new HashSet<>();
                                        for (Object o : listValue) {
                                            value.add(o);
                                        }
                                    }
                                    if (type.startsWith("Map")) {
                                        Map value = (Map) JSON.parse(valueJsonArray.get(j).toString());
                                        objects[j] = value;
                                    }
                                    if(type.startsWith("HashMap") || type.equals("Hashtable") || type.equals("LinkedHashMap")){
                                        HashMap value = (HashMap) JSON.parse(valueJsonArray.get(j).toString());
                                        objects[j] = value;
                                    }
                                }
                                Object o = clazz.newInstance();
                                Object returnObject = invokeMethod.invoke(o, objects);
                                return new Res(Res.CODE_151, returnObject);
                            }
                        } else if (methodName.equals(getMethodName) && getTypeParameters.length == 0) {//没有参数，直接执行方法
                            Method invokeMethod = clazz.getMethod(getMethodName, getTypeParameters);//要执行的方法
                            Object o = clazz.newInstance();
                            Object returnObject = invokeMethod.invoke(o);
                            return new Res(Res.CODE_151, returnObject);
                        }
                    }
                }
            }
            return new Res(Res.CODE_152, null);
        } catch (Exception e) {
            e.printStackTrace();
            return new Res(Res.CODE_153, e.toString());
        }
    }


    /**
     * 合约详情
     */
    @GetMapping("/getContractDetails")
    public Res getContractDetails(String contractAddress) {
        try {
            if ("".equals(contractAddress) || contractAddress == null) return new Res(Res.CODE_140, null);

            Transaction transaction = transactionRepository.findByContract(contractAddress, 3);//查出流水
            if (transaction == null) return new Res(Res.CODE_136, null);

            String bin = transaction.getContractBin();
            program.setBin(bin);
            program.analysisAndPushHeap();//解析bin
            Heap heap = program.getHeap();
            BigDecimal fromCoin = new BigDecimal(heap.getItem(heap.getItem("fromCoin").toString()).toString());//a
            BigDecimal toCoin = new BigDecimal(heap.getItem(heap.getItem("toCoin").toString()).toString());//b
            String fromType = heap.getItem(heap.getItem("fromCoin").toString() + "Type").toString();
            String toType = heap.getItem(heap.getItem("toCoin").toString() + "Type").toString();

            if (!fromType.toLowerCase().equals(Constants.PTN))
                fromType = tokenRepository.findByName(fromType).getName();

            if (!toType.toLowerCase().equals(Constants.PTN))
                toType = tokenRepository.findByName(toType).getName();

            Map<String, Object> map = new HashMap<>();

            map.put("contractState", transaction.getContractState());//合同状态
            map.put("transFrom", ECKey.pubkeyToAddress(transaction.getTransFrom()));//from
            map.put("transTo", ECKey.pubkeyToAddress(transaction.getTransTo()));//to
            map.put("fromCoin", fromCoin);
            map.put("toCoin", toCoin);
            map.put("fromType", fromType);
            map.put("toType", toType);

            if (transaction.getContractState() == 1) {//已完成交易
                Transaction transac = transactionRepository.findByTransFromAndType(transaction.getTransFrom(), 5, contractAddress);//查出合同转给的类型为5的流水
                if (transac != null) {
                    map.put("toAddress", ECKey.pubkeyToAddress(transac.getTransTo()));
                }
            }
            return new Res(Res.CODE_100, map);
        } catch (Exception e) {
            e.printStackTrace();
            return new Res(Res.CODE_500, null);
        }
    }

    /*
    * 我的合约
    * **/
    @PostMapping("myContract")
    @ResponseBody
    public Res myContract(PageObject pageObject, @RequestParam("contractType") Integer contractType) {
        Res res = new Res();
        Map resultMap = new LinkedHashMap();
        List<Map> transMap = new ArrayList<>();
        Map<String, String> localAccount = initializationManager.getAccountList();
        List<String> localAccountList = new ArrayList<>();
        for (Map.Entry<String, String> entry : localAccount.entrySet()) {
            String address = entry.getKey();
            Map<String, String> accountInfo = initializationManager.getAccountListByAddress(address);
            localAccountList.add(accountInfo.get(Constants.PUBKEY));
        }
        int count = transactionRepository.findTransCount(contractType, 3, localAccountList);
        int start = (count - pageObject.getPageNumber() * pageObject.getPageSize()) < 0 ? 0 : (count - pageObject.getPageNumber() * pageObject.getPageSize());
        pageObject.setSumRecord(count);
        List<Transaction> transactions = transactionRepository.findContractTrans(contractType, 3, localAccountList, start, pageObject.getPageSize());
        Collections.sort(transactions);
        if (contractType == 1) { //兑换
            for (Transaction transaction : transactions) {
                Map exchangeInfoMap = this.getExchangeInfoByBin(transaction.getContractBin());
                Map map = new LinkedHashMap();
                map.put("address", ECKey.pubkeyToAddress(transaction.getTransFrom()));
                map.put("contractAddress", transaction.getContractAddress());
                map.put("contractState", transaction.getContractState());
                map.put("date", DateUtil.stampToDate(transaction.getTransactionHead().getTimeStamp()));
                map.put("fromCoinName", exchangeInfoMap.get("fromCoinName"));
                map.put("fromVal", exchangeInfoMap.get("fromVal"));
                map.put("toCoinName", exchangeInfoMap.get("toCoinName"));
                map.put("toVal", exchangeInfoMap.get("toVal"));
                transMap.add(map);
            }
        } else if (contractType == 2) { //投票
            for (Transaction transaction : transactions) {
                Map map = new LinkedHashMap();
                Map binMap = ContractUtil.analisysVotesContract(transaction.getContractBin()); //解析bin
                if (binMap == null) {
                    continue;
                }
                LinkedHashMap items = new LinkedHashMap();
                String[] itemsArr = CheckVerify.splitStrs(binMap.get("items").toString());
                Integer paticipateNumber = transactionRepository.findVodesCount(transaction.getContractAddress(), 7, 2);
                for (int i = 0; i < itemsArr.length; i++) {
                    String item = Constants.serialNumber[i] + "." + itemsArr[i];
                    Integer votesCount = transactionRepository.findVodesCount(transaction.getContractAddress(), 7, 2, item); //票数
                    BigDecimal votesProportion = (paticipateNumber == 0 ? BigDecimal.ZERO : new BigDecimal(votesCount).divide(new BigDecimal(paticipateNumber), 2, BigDecimal.ROUND_HALF_UP)); //票数占比
                    Map votes = new HashMap();
                    votes.put("votesCount", votesCount);
                    votes.put("votesProportion", votesProportion);
                    items.put(item, votes);
                }
                map.put("sponsor", ECKey.pubkeyToAddress(transaction.getTransFrom())); //投票发起人
                map.put("contractAddress", transaction.getContractAddress()); //合约地址
                map.put("contractState", transaction.getContractState());  //投票状态
                map.put("deadLine", binMap.get("dealLineCondition")); //截止条件
                map.put("topic", binMap.get("topic")); //主题
                map.put("items", items); //选项
                map.put("paticipateNumber", paticipateNumber);//参与人数
                transMap.add(map);
            }
        } else if (contractType == 3) { //代币
            for (Transaction transaction : transactions) {
                Map binMap = ContractUtil.analisysTokenContract(transaction.getContractBin());
                Map map = new LinkedHashMap();
                if (binMap != null) {
                    map.put("tokenIssuer", ECKey.pubkeyToAddress(transaction.getTransFrom()));
                    map.put("contractAddress", transaction.getContractAddress());
                    map.put("tokenName", binMap.get("tokenName"));
                    map.put("tokenNum", binMap.get("tokenNum"));
                    map.put("tokenDecimal", binMap.get("tokenDecimal"));
                    map.put("data", DateUtil.stampToDate(transaction.getTransactionHead().getTimeStamp()));
                    map.put("contractState", transaction.getContractState());
                }
                transMap.add(map);
            }
        }

        resultMap.put("transMap", transMap);
        resultMap.put("pageNumber", pageObject.getPageNumber());
        resultMap.put("count", count);
        resultMap.put("contractType", contractType);
        res.setCode(Res.CODE_100);
        res.setData(resultMap);
        return res;
    }

    /*
    * 投票列表
    * **/
    @PostMapping("votesList")
    public Res votesList(PageObject pageObject, @RequestParam("contractAddress") String contractAddress, @RequestParam("items") String items) {
        Res res = new Res();
        Map resultMap = new HashMap();
        List addressList = new ArrayList();
        try {
            Integer count = transactionRepository.findVodesCount(contractAddress, 7, 2, items);
            int start = (count - pageObject.getPageNumber() * pageObject.getPageSize()) < 0 ? 0 : count - pageObject.getPageNumber() * pageObject.getPageSize();
            pageObject.setSumRecord(count);
            List<Transaction> transactions = transactionRepository.findVodes(contractAddress, 7, 2, items, start, pageObject.getPageSize());
            for (Transaction transaction : transactions) {
                addressList.add(ECKey.pubkeyToAddress(transaction.getTransFrom()));
            }
            resultMap.put("addressList", addressList);
            res.setData(resultMap);
            res.setCode(Res.CODE_100);
        } catch (Exception e) {
            res.setCode(Res.CODE_101);
        }
        return res;
    }

    /*
     * 交易市场
     * **/
    @PostMapping("marketPlace")
    @ResponseBody
    public Res marketPlace(PageObject pageObject, @RequestParam int contractState, @RequestParam String sell, @RequestParam String buy, @RequestParam("contractType") Integer contractType) {
        Res res = new Res();
        Map resultMap = new LinkedHashMap();
        List<Map> transMap = new ArrayList<>();
        int count = 0;
        if (contractType == 1) {  //兑换
            Map<String, String> localAccount = initializationManager.getAccountList();
            List<String> localAccountList = new ArrayList<>();

            for (Map.Entry<String, String> entry : localAccount.entrySet()) {
                String address = entry.getKey();
                Map<String, String> accountInfo = initializationManager.getAccountListByAddress(address);
                localAccountList.add(accountInfo.get(Constants.PUBKEY));
            }

            count = transactionRepository.findContractTransCount(1, 3, localAccountList, contractState, sell.toLowerCase(), buy.toLowerCase());
            int start = (count - pageObject.getPageNumber() * pageObject.getPageSize()) < 0 ? 0 : count - pageObject.getPageNumber() * pageObject.getPageSize();
            pageObject.setSumRecord(count);
            List<Transaction> transactions = transactionRepository.findContractTrans(1, 3, localAccountList, contractState, sell.toLowerCase(), buy.toLowerCase(), start, pageObject.getPageSize());
            Collections.reverse(transactions);
            for (Transaction transaction : transactions) {
                long time = transaction.getTransactionHead().getTimeStamp();
                Map exchangeInfoMap = this.getExchangeInfoByBin(transaction.getContractBin());
                Map map = new LinkedHashMap();
                map.put("address", ECKey.pubkeyToAddress(transaction.getTransFrom()));
                map.put("contractAddress", transaction.getContractAddress());
                map.put("contractState", transaction.getContractState());
                map.put("date", DateUtil.stampToDate(transaction.getTransactionHead().getTimeStamp()));
                map.put("fromCoinName", exchangeInfoMap.get("fromCoinName"));
                map.put("fromVal", exchangeInfoMap.get("fromVal"));
                map.put("toCoinName", exchangeInfoMap.get("toCoinName"));
                map.put("toVal", exchangeInfoMap.get("toVal"));
                map.put("time", time);
                transMap.add(map);
            }
        } else if (contractType == 2) { //投票
            count = transactionRepository.findContractCount(3, 2, contractState);
            int start = (count - pageObject.getPageNumber() * pageObject.getPageSize()) < 0 ? 0 : count - pageObject.getPageNumber() * pageObject.getPageSize();
            pageObject.setSumRecord(count);
            List<Transaction> transactions = transactionRepository.findContract(3, 2, contractState, start, pageObject.getPageSize());
            Collections.reverse(transactions);
            for (Transaction transaction : transactions) {
                if (contractState == 0) { //未完成
                    Map map = new LinkedHashMap();
                    Map binMap = ContractUtil.analisysVotesContract(transaction.getContractBin()); //解析bin
                    if (binMap == null) {
                        continue;
                    }
                    List<String> items = new ArrayList<>();
                    String[] itemsArr = CheckVerify.splitStrs(binMap.get("items").toString());
                    for (int i = 0; i < itemsArr.length; i++) {
                        items.add(Constants.serialNumber[i] + "." + itemsArr[i]);
                    }
                    BigDecimal fee = (items.size() == 0 ? BigDecimal.ZERO : new BigDecimal(items.size()).divide(new BigDecimal(1000), 8, BigDecimal.ROUND_HALF_UP));
                    map.put("contractAddress", transaction.getContractAddress()); //合约地址
                    map.put("contractState", transaction.getContractState());  //投票状态
                    map.put("topic", binMap.get("topic")); //主题
                    map.put("items", items); //选项
                    map.put("fee", fee); //投票费用
                    transMap.add(map);
                } else if (contractState == 1) { //已完成
                    Map map = new LinkedHashMap();
                    Map binMap = ContractUtil.analisysVotesContract(transaction.getContractBin()); //解析bin
                    if (binMap == null) {
                        continue;
                    }
                    LinkedHashMap items = new LinkedHashMap();
                    String[] itemsArr = CheckVerify.splitStrs(binMap.get("items").toString());
                    Integer paticipateNumber = transactionRepository.findVodesCount(transaction.getContractAddress(), 7, 2);
                    for (int i = 0; i < itemsArr.length; i++) {
                        String item = Constants.serialNumber[i] + "." + itemsArr[i];
                        Integer votesCount = transactionRepository.findVodesCount(transaction.getContractAddress(), 7, 2, item); //票数
                        BigDecimal votesProportion = (paticipateNumber == 0 ? BigDecimal.ZERO : new BigDecimal(votesCount).divide(new BigDecimal(paticipateNumber), 2, BigDecimal.ROUND_HALF_UP)); //票数占比
                        Map votes = new HashMap();
                        votes.put("votesCount", votesCount);
                        votes.put("votesProportion", votesProportion);
                        items.put(item, votes);
                    }
                    map.put("sponsor", ECKey.pubkeyToAddress(transaction.getTransFrom())); //投票发起人
                    map.put("contractAddress", transaction.getContractAddress()); //合约地址
                    map.put("contractState", transaction.getContractState());  //投票状态
                    map.put("deadLine", binMap.get("dealLineCondition")); //截止条件
                    map.put("topic", binMap.get("topic")); //主题
                    map.put("items", items); //选项
                    map.put("paticipateNumber", paticipateNumber);//参与人数
                    transMap.add(map);
                }
            }
        }
        resultMap.put("transMap", transMap);
        resultMap.put("pageNumber", pageObject.getPageNumber());
        resultMap.put("count", count);
        resultMap.put("contractType", contractType);
        resultMap.put("contractState", contractState);
        res.setCode(Res.CODE_100);
        res.setData(resultMap);
        return res;
    }

    private Map<Object, Object> getExchangeInfoByBin(String bin) {
        Map<Object, Object> resultMap = new HashMap<Object, Object>();
        Program program = new Program();
        program.setBin(bin);
        program.analysisAndPushHeap();
        Heap heap = program.getHeap();
        Object a = heap.getItem("fromCoin") == null ? "a" : heap.getItem("fromCoin");
        Object b = heap.getItem("toCoin") == null ? "b" : heap.getItem("toCoin");
        Long fromVal = Long.parseLong(heap.getItem(a.toString()).toString());
        String fromCoinName = heap.getItem(a + "Type").toString();
        Long toVal = Long.parseLong(heap.getItem(b.toString()).toString());
        String toCoinName = heap.getItem(b + "Type").toString();
        try {
            fromCoinName = fromCoinName.equalsIgnoreCase(Constants.PTN) ? fromCoinName : tokenRepository.findByName(fromCoinName).getName();
            toCoinName = toCoinName.equalsIgnoreCase(Constants.PTN) ? toCoinName : tokenRepository.findByName(toCoinName).getName();
        } catch (Exception e) {
            logger.info(e.getMessage());
        }
        resultMap.put("fromCoinName", fromCoinName);
        resultMap.put("fromVal", fromVal);
        resultMap.put("toCoinName", toCoinName);
        resultMap.put("toVal", toVal);
        logger.info("getExchangeInfoByBin:" + JSON.toJSONString(resultMap));
        return resultMap;
    }

    /**
     * 预编译合约
     *
     * @param contractStr
     * @return
     */
    @PostMapping("createContract")
    @ResponseBody
    public Res createContract(String contractStr, @RequestParam(defaultValue = "0") Integer eventType) {
        Res res = new Res();
        BigDecimal fee = null;
        Map resultMap = new HashMap();
        if (eventType == 4) {//合约为其他单独处理
            String bin = MyClassLoader.compilerContract(contractStr);
            if (StringUtils.isBlank(bin)) {
                res.setCode(Res.CODE_601);
                return res;
            }
            int count = count(contractStr, ';');
            resultMap.put("fee", new BigDecimal(count).divide(new BigDecimal(1000)));
            res.setCode(Res.CODE_202);
            res.setData(resultMap);
            return res;
        } else {
            try {
                contractStr = Pcompiler.unescapeJava(contractStr);//去转义
                contractStr = Pcompiler.complementSpace(contractStr);//补齐等号空格
                contractStr = Pcompiler.replaceSpace(contractStr);//去多个空格
                RegexCompile.checkRegex(contractStr);//验证
                List<String> list = RegexCompile.getSetList(contractStr);
                String[] indexAfters = StringUtils.substringsBetween(contractStr, "event", ";");
                if (indexAfters != null) {
                    for (String indexAfter : indexAfters) {
                        String eventName = StringUtils.substringBefore(indexAfter, "(").trim();
                        eventName = eventName.replace(" ", "");
                        int eventCount = EventEnum.checkEventName(eventName);//判断事件名称是否存在eventEnum
                        if (eventCount != -1) {
                            String parameter = StringUtils.substringBetween(indexAfter, "(", ")");
                            String[] eventParams = parameter.split(",");//事件里的参数
                            for (int i = 0; i < eventParams.length; i++)//去除参数里面空格
                                eventParams[i] = eventParams[i].trim();
                            if (eventParams.length == eventCount) {//判断参数个数
                                if (eventName.equals(EventEnum.business.name())) {
                                    for (String s : list) {
                                        String[] variable = ArrayUtils.removeAllOccurences(s.split(" "), "");
                                        if (variable[1].equals("coin")) {
                                            if (new BigDecimal(variable[4]).compareTo(new BigDecimal(0)) != 1) {
                                                res.setCode(Res.CODE_126);
                                                res.setData(resultMap);
                                                return res;
                                            }
                                            //获取手续费
                                            if (variable[variable.length - 1].toLowerCase().equals(Constants.PTN) && (variable[2].equals(eventParams[0]) || variable[2].equals(eventParams[1]))) {
                                                fee = new BigDecimal(variable[4]).divide(new BigDecimal(1000));
                                            }
                                        }
                                    }
                            /*if (!eventParams[1].equals(Constants.PTN) && variable[2].equals(eventParams[1])) {//获取第二个参数的代币
                                String exchengeTokenName = variable[variable.length - 1];
                                Token token = tokenRepository.findByName(exchengeTokenName);
                                if (token == null) {
                                    res.setCode(Res.CODE_131);
                                    res.setMsg("代币不存在");
                                    res.setData(resultMap);
                                    return res;
                                }
                            }*/
                                    if (list.size() == 0) {
                                        res.setCode(Res.CODE_101);
                                        return res;
                                    }
                                    if (fee == null) {
                                        res.setCode(Res.CODE_131);
                                        return res;
                                    }
                                    resultMap.put("fee", fee);
                                }
                                if (eventName.equals(EventEnum.vote.name())) {
                                    fee = new BigDecimal(1).divide(new BigDecimal(1000));
                                    for (String set : list) {//根据投票个数统计手续费
                                        String[] sets = set.split(" ");
                                        if (eventParams[2].equals(sets[2])) {
                                            String[] arr = RegexCompile.splitCommaStr(set);
                                            fee = fee.multiply(new BigDecimal(arr.length));
                                        }
                                    }
                                    resultMap.put("fee", fee);
                                }
                                if (eventName.equalsIgnoreCase(EventEnum.token.name())) {
                                    int decimalsInt = 0;
                                    Long tokenAmountLong = 0l;
                                    String tokenName = null;
                                    for (String sets : list) {
                                        String[] set = sets.split(" ");
                                        if (set[2].equals(eventParams[0])) {
//                              System.out.println(set[4]);
                                            BigInteger bi = new BigInteger(set[4]);
//                                        bi.compareTo(new BigInteger(6 + "")) == -1
//                                        bi.compareTo(new BigInteger(8 + "")) == 1
//                                        if (6 > Integer.valueOf(set[4]) || Integer.valueOf(set[4]) > 8) {
                                            if (bi.compareTo(new BigInteger(6 + "")) == -1 || bi.compareTo(new BigInteger(8 + "")) == 1) {
//                                        throw new BusinessException("error,token 代币精度只能为6-8", ErrorCode._10036);
                                                res.setCode(Res.CODE_125);
                                                return res;
                                            }
                                            decimalsInt = Integer.valueOf(set[4]);
                                        }
                                        if (set[2].equals(eventParams[1])) {
                                            BigInteger bi = new BigInteger(set[4]);
                                            if (bi.compareTo(new BigInteger(1000l + "")) == -1 || bi.compareTo(new BigInteger(100000000000l + "")) != -1) {
//                                        throw new BusinessException("error,token 代币发行量只能为1000-100000000000的整数", ErrorCode._10037);
                                                res.setCode(Res.CODE_124);
                                                return res;
                                            }
                                            tokenAmountLong = Long.parseLong(set[4]);
                                        }
                                        if (set[2].equals(eventParams[2])) {
//                              system.out.println(set[4]);
                                            set[4] = set[4].replace("\"", "");
                                            if (set[4].length() < 3 || set[4].length() > 20 || !ValidateUtil.checkLetter(set[4])) {
                                                res.setCode(Res.CODE_127);
                                                return res;
                                            }
                                            tokenName = set[4];
                                        }
                                    }
                                    if (tokenAmountLong >= 10000000000l && decimalsInt > 7) {
//                                throw new BusinessException("error,token 当发行量大于或等于10000000000,单位不得超过7",ErrorCode._10038);
                                        res.setCode(Res.CODE_143);
                                        return res;
                                    }//展示手续费不需要乘以最小单位
                               /* new BigDecimal(fee).divide(new BigDecimal(Constants.MININUMUNIT)).toPlainString()
                                double feeDouble = Double.valueOf(TokenUtil.TokensRate(tokenName));
                                if (feeDouble == 0) feeDouble = 1;
                                */
                                    long feeDouble = Double.valueOf(TokenUtil.TokensRate(tokenName) * Constants.MININUMUNIT).longValue();
                                    if (feeDouble == 0) feeDouble = 1;
                                    fee = new BigDecimal(feeDouble);
                                    resultMap.put("fee", fee.divide(new BigDecimal(Constants.MININUMUNIT)).setScale(6, BigDecimal.ROUND_HALF_UP).toPlainString());
                                }
                            }
                        }
                    }
                }
                res.setCode(Res.CODE_202);
                res.setData(resultMap);
            } catch (BusinessException e) {
                e.printStackTrace();
                res.setCode(e.getErrorCode().getCode());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return res;
    }

    /**
     * 编译合约
     *
     * @param contractStr 合约
     * @param address     发送方的地址
     * @return
     */
    @PostMapping("compileContract")
    @ResponseBody
    public Res compileContract(String contractStr, String address, @RequestParam(defaultValue = "0") Integer eventType, String password) {
        Res res = new Res();
        long webTime = DateUtil.getWebTime();
        if (webTime == Constants.GENESIS_TIME) {
            res.setCode(Res.CODE_500);
            return res;
        }
        //合约类型为其他时候单独处理 start
        if (eventType == 4) {
            InesvMessage.Message.Builder builder = MessageManager.createMessageBuilder(RESPONSE, NEW_TRANSACTION);
            InesvMessage.Message.Builder builderFee = MessageManager.createMessageBuilder(RESPONSE, NEW_TRANSACTION);
            ECKey ecKeyFee = new ECKey(new SecureRandom());//生成手续费存放账户
            //验证账户是否有效
            Map<String, String> localAccount = initializationManager.getAccountListByAddress(address);
            String prikeyFrom = localAccount.get(Constants.PRIKEY);
            String pubKey = localAccount.get(Constants.PUBKEY);
            String savePwd = localAccount.get(Constants.PWD);
            if (!savePwd.equals(password)) {
                res.setCode(Res.CODE_301);
                return res;
            }
            if (pubKey.equals("")) {
                res.setCode(Res.CODE_102);
                return res;
            }//end
            String bin = MyClassLoader.compilerContract(contractStr);
            if (StringUtils.isBlank(bin)) {
                res.setCode(Res.CODE_601);
                return res;
            }
            //创建合约地址 start
            ECKey ecKey = new ECKey(new SecureRandom());
            String contractPrivateKey = Hex.toHexString(ecKey.getPrivKeyBytes());
            String contractPublicKey = Hex.toHexString(ecKey.getPubKey());
            String contractAddress = Constants.ADDRESS_PREFIX + Hex.toHexString(ecKey.getAddress());
            //统计手续费
            int count = count(contractStr, ';');
            Long feeDouble = new BigDecimal(count).divide(new BigDecimal(1000)).multiply(new BigDecimal(Constants.MININUMUNIT)).longValue();

            UnconfirmedTran unconfirmedTran = new UnconfirmedTran(pubKey, contractPublicKey, "", Constants.PTN
                    , 0, 0, webTime, 3, contractAddress, bin, 4, 0, Constants.PTN);
            byte[] transSignature = JSON.toJSONString(ECKey.fromPrivate(Hex.decode(prikeyFrom)).sign(SHAEncrypt.sha3(SerializationUtils.serialize(unconfirmedTran.toString())))).getBytes();
            unconfirmedTran.setTransSignature(transSignature);
            logger.warn("其他流水：" + unconfirmedTran.toString());
            builder.setUnconfirmedTran(MessageManager.createUnconfirmedTranMessage(unconfirmedTran));
            //手续费流水
            UnconfirmedTran unconfirmedTranFee = new UnconfirmedTran(pubKey, Hex.toHexString(ecKeyFee.getPubKey()), "", Constants.PTN
                    , feeDouble, 0, webTime, 4, contractAddress, bin, 4, 0, Constants.PTN);
            byte[] transSignatureFee = JSON.toJSONString(ECKey.fromPrivate(Hex.decode(prikeyFrom)).sign(SHAEncrypt.sha3(SerializationUtils.serialize(unconfirmedTranFee.toString())))).getBytes();
            unconfirmedTranFee.setTransSignature(transSignatureFee);
            logger.warn("其他手续费流水：" + unconfirmedTranFee.toString());
            builderFee.setUnconfirmedTran(MessageManager.createUnconfirmedTranMessage(unconfirmedTranFee));

            List<String> hostList = nioSocketChannelManager.getChannelHostList();
            builder.addAllNodeAddressList(hostList);
            builderFee.addAllNodeAddressList(hostList);

            nioSocketChannelManager.write(builderFee.build());
            nioSocketChannelManager.write(builder.build());
            return res;
            //合约类型为其他时候单独处理 end
        } else {
            try {
                RegexCompile.checkRegex(contractStr);
                Map<String, Object> resultMap = new HashMap();
                Map<String, String> localAccount = initializationManager.getAccountListByAddress(address);
                String prikeyFrom = localAccount.get(Constants.PRIKEY);
                String pubKey = localAccount.get(Constants.PUBKEY);
                String savePwd = localAccount.get(Constants.PWD);
                if (!savePwd.equals(password)) {
                    res.setCode(Res.CODE_301);
                    return res;
                }
                if (pubKey.equals("")) {
                    res.setCode(Res.CODE_102);
                    res.setData(resultMap);
                    return res;
                }
                contractStr = Pcompiler.unescapeJava(contractStr);//去掉转义符
                contractStr = Pcompiler.replaceSpace(contractStr);
                StringBuilder contractSb = new StringBuilder(contractStr);
                //为第一行添加address start
                int temp1 = contractStr.indexOf("{") + 1;
                contractSb = contractSb.insert(temp1, "set address add=" + pubKey + ";");
                contractSb = new StringBuilder(Pcompiler.complementSpace(contractSb.toString()));//为参数=补空格
                //为第一行添加address end
                List<String> list = RegexCompile.getSetList(contractSb.toString());
                String[] events = StringUtils.substringsBetween(contractSb.toString(), "event", ";");
                //校验只允许一个事件 start
                boolean eventflag = true;
                if (eventType == 1) {//挂单
                    for (String e : events) {
                        String eventName = StringUtils.substringBefore(e, "(").trim();
                        if (!eventName.equals(EventEnum.business.name())) {
                            res.setCode(Res.CODE_600);
                            res.setData(resultMap);
                            return res;
                        }
                    }
                } else if (eventType == 2) {//投票
                    for (String e : events) {
                        String eventName = StringUtils.substringBefore(e, "(").trim();
                        if (!eventName.equals(EventEnum.vote.name())) {
                            res.setCode(Res.CODE_600);
                            res.setData(resultMap);
                            return res;
                        }
                    }
                } else if (eventType == 3) {//发行代币
                    for (String e : events) {
                        String eventName = StringUtils.substringBefore(e, "(").trim();
                        if (!eventName.equals(EventEnum.token.name())) {
                            res.setCode(Res.CODE_600);
                            res.setData(resultMap);
                            return res;
                        }
                    }
                }
                //校验只允许一个事件 end
                String tokenName = null;
                String exchengeTokenName = null;
                BigDecimal transValue = new BigDecimal(0);
                BigDecimal fee = new BigDecimal(0);
                List<String> eventNames = new ArrayList<>();
                //创建合约地址 start
                ECKey ecKey = new ECKey(new SecureRandom());
                String contractPrivateKey = Hex.toHexString(ecKey.getPrivKeyBytes());
                String contractPublicKey = Hex.toHexString(ecKey.getPubKey());
                String contractAddress = Constants.ADDRESS_PREFIX + Hex.toHexString(ecKey.getAddress());
                String bin = Pcompiler.compilerContract(contractSb.toString());
                //创建合约地址 end
                //事件个数
                for (String eventVar : events) {
                    String eventName = StringUtils.substringBefore(eventVar, "(").trim();
                    if (eventName.equals(EventEnum.business.name())) {
                        InesvMessage.Message.Builder builder = MessageManager.createMessageBuilder(RESPONSE, NEW_TRANSACTION);
                        InesvMessage.Message.Builder builderFee = MessageManager.createMessageBuilder(RESPONSE, NEW_TRANSACTION);
                        ECKey ecKeyFee = new ECKey(new SecureRandom());//生成手续费存放账户
                        eventNames.add(eventName);
                        String parameter = StringUtils.substringBetween(eventVar, "(", ")");
                        String[] eventParams = parameter.split(",");//事件里的参数
                        //去除参数里面空格
                        for (int i = 0; i < eventParams.length; i++) eventParams[i] = eventParams[i].trim();
                        for (String s : list) {
                            String[] variable = ArrayUtils.removeAllOccurences(s.split(" "), "");
                            if (variable[1].equals("coin")) {
                                if (new BigDecimal(variable[4]).compareTo(new BigDecimal(0)) != 1) {
                                    res.setCode(Res.CODE_126);
                                    res.setData(resultMap);
                                    return res;
                                }
                                //获取手续费
                                if (variable[variable.length - 1].equals(Constants.PTN) && (variable[2].equals(eventParams[0]) || variable[2].equals(eventParams[1]))) {
                                    initializationManager.unitConvert(new BigInteger(variable[4]), Constants.PTN, Constants.MINI_UNIT);
                                    fee = new BigDecimal(variable[4]).multiply(new BigDecimal(Constants.MININUMUNIT)).divide(new BigDecimal(1000));
                                }
                                if (variable[2].equals(eventParams[0])) {//获取第一个参数得到 sell 类型
                                    if (!variable[variable.length - 1].toLowerCase().equals(Constants.PTN)) {
                                        Token token = tokenRepository.findByName(variable[variable.length - 1].toLowerCase());
                                        if (token == null) {
                                            res.setCode(Res.CODE_131);
                                            res.setData(resultMap);
                                            return res;
                                        }
                                    }
                                    Map<String, Long> map = assetsManager.getAccountAssets(pubKey, variable[variable.length - 1]);
                                    long balance = map.get(Constants.BALANCE);
                                    if (balance < initializationManager.unitConvert(new BigInteger(variable[4]), variable[variable.length - 1], Constants.MINI_UNIT).longValue()) {
                                        res.setCode(Res.CODE_106);
                                        res.setData(resultMap);
                                        return res;
                                    }
                                    //代币名
                                    tokenName = variable[variable.length - 1];
                                    //交易金额
                                    transValue = initializationManager.unitConvert(new BigInteger(variable[4]), variable[variable.length - 1], Constants.MINI_UNIT);
                                }
                                if (variable[2].equals(eventParams[1])) {//获取第二个参数的代币
                                    exchengeTokenName = variable[variable.length - 1].toLowerCase();
                                    if (!exchengeTokenName.equals(Constants.PTN)) {
                                        Token token = tokenRepository.findByName(exchengeTokenName);
                                        if (token == null) {
                                            res.setCode(Res.CODE_131);
                                            res.setData(resultMap);
                                            return res;
                                        }
                                    }
                                }
                            }
                        }
                        if (fee.compareTo(new BigDecimal(0)) == 0) {
                            res.setCode(Res.CODE_500);
                            res.setData(resultMap);
                            return res;
                        }
                        //单独验证手续费
                        Map<String, Long> assetsPtn = assetsManager.getAccountAssets(pubKey, Constants.PTN);
                        long effectiveIncome = assetsPtn.get(Constants.TOTAL_EFFECTIVE_INCOME);
                        long expenditure = assetsPtn.get(Constants.TOTAL_EXPENDITURE);
                        long balance = assetsPtn.get(Constants.BALANCE);
                        long Transfee;
                        if (tokenName.toLowerCase().equals(Constants.PTN)) {
                            Transfee = transValue.add(fee).longValue();
                        } else {
                            Transfee = fee.longValue();
                        }
                        if (balance < Transfee) {
                            res.setCode(Res.CODE_106);
                            res.setData(resultMap);
                            return res;
                        }
                        //转成数据库存在的token 挂单事件
                        if (!tokenName.toLowerCase().equals(Constants.PTN)) {
                            tokenName = tokenRepository.findByName(tokenName).getName();
                        }
                        UnconfirmedTran unconfirmedTran = new UnconfirmedTran(pubKey, contractPublicKey, "", tokenName
                                , transValue.longValue(), 0, webTime, 3, contractAddress, bin, 1, 0, exchengeTokenName);
                        byte[] transSignature = JSON.toJSONString(ECKey.fromPrivate(Hex.decode(prikeyFrom)).sign(SHAEncrypt.sha3(SerializationUtils.serialize(unconfirmedTran.toString())))).getBytes();
                        unconfirmedTran.setTransSignature(transSignature);
                        logger.warn("挂单流水：" + unconfirmedTran.toString());
                        builder.setUnconfirmedTran(MessageManager.createUnconfirmedTranMessage(unconfirmedTran));
                        //手续费流水
                        UnconfirmedTran unconfirmedTranFee = new UnconfirmedTran(pubKey, Hex.toHexString(ecKeyFee.getPubKey()), "", Constants.PTN
                                , fee.longValue(), 0, webTime, 4, contractAddress, bin, 1, 0, exchengeTokenName);
                        byte[] transSignatureFee = JSON.toJSONString(ECKey.fromPrivate(Hex.decode(prikeyFrom)).sign(SHAEncrypt.sha3(SerializationUtils.serialize(unconfirmedTranFee.toString())))).getBytes();
                        unconfirmedTranFee.setTransSignature(transSignatureFee);
                        logger.warn("挂单手续费流水：" + unconfirmedTranFee.toString());
                        builderFee.setUnconfirmedTran(MessageManager.createUnconfirmedTranMessage(unconfirmedTranFee));

                        List<String> hostList = nioSocketChannelManager.getChannelHostList();
                        builder.addAllNodeAddressList(hostList);
                        builderFee.addAllNodeAddressList(hostList);

                        nioSocketChannelManager.write(builderFee.build());
                        nioSocketChannelManager.write(builder.build());
                    }
                    if (eventName.equals(EventEnum.vote.name())) {
                        InesvMessage.Message.Builder builder = MessageManager.createMessageBuilder(RESPONSE, NEW_TRANSACTION);
                        InesvMessage.Message.Builder builderFee = MessageManager.createMessageBuilder(RESPONSE, NEW_TRANSACTION);
                        ECKey ecKeyFee = new ECKey(new SecureRandom());//生成手续费存放账户
                        eventNames.add(eventName);
                        tokenName = Constants.PTN;
                        fee = new BigDecimal(1).multiply(new BigDecimal(Constants.MININUMUNIT)).divide(new BigDecimal(1000));
                        String parameter = StringUtils.substringBetween(eventVar, "(", ")");
                        String[] eventParams = parameter.split(",");//事件里的参数
                        //去除参数里面空格
                        for (int i = 0; i < eventParams.length; i++) eventParams[i] = eventParams[i].trim();
                        for (String set : list) {//根据投票个数统计手续费
                            String[] sets = set.split(" ");
                            if (eventParams[2].equals(sets[2])) {
                                String[] arr = RegexCompile.splitCommaStr(set);
                                fee = fee.multiply(new BigDecimal(arr.length));
                            }
                        }
                        //验证手续费
                        Map<String, Long> assetsPtn = assetsManager.getAccountAssets(pubKey, Constants.PTN);
                        long effectiveIncome = assetsPtn.get(Constants.TOTAL_EFFECTIVE_INCOME);
                        long expenditure = assetsPtn.get(Constants.TOTAL_EXPENDITURE);
                        long balance = assetsPtn.get(Constants.BALANCE);
                        long Transfee;
                        if (tokenName.toLowerCase().equals(Constants.PTN)) {
                            Transfee = transValue.add(fee).longValue();
                        } else {
                            Transfee = fee.longValue();
                        }
                        if (balance < Transfee) {
                            res.setCode(Res.CODE_106);
                            res.setData(resultMap);
                            return res;
                        }
                        //转成数据库存在的token 投票事件
                        if (!tokenName.toLowerCase().equals(Constants.PTN)) {
                            tokenName = tokenRepository.findByName(tokenName).getName();
                        }
                        UnconfirmedTran unconfirmedTran = new UnconfirmedTran(pubKey, contractPublicKey, "", tokenName
                                , 0, 0, webTime, 3, contractAddress, bin, 2, 0, exchengeTokenName);
                        byte[] transSignature = JSON.toJSONString(ECKey.fromPrivate(Hex.decode(prikeyFrom)).sign(SHAEncrypt.sha3(SerializationUtils.serialize(unconfirmedTran.toString())))).getBytes();
                        unconfirmedTran.setTransSignature(transSignature);
                        logger.warn("投票流水：" + unconfirmedTran.toString());
                        builder.setUnconfirmedTran(MessageManager.createUnconfirmedTranMessage(unconfirmedTran));
                        UnconfirmedTran unconfirmedTranFee = new UnconfirmedTran(pubKey, Hex.toHexString(ecKeyFee.getPubKey()), "", Constants.PTN
                                , fee.longValue(), 0, webTime, 4, contractAddress, bin, 2, 0, exchengeTokenName);
                        byte[] transSignatureFee = JSON.toJSONString(ECKey.fromPrivate(Hex.decode(prikeyFrom)).sign(SHAEncrypt.sha3(SerializationUtils.serialize(unconfirmedTranFee.toString())))).getBytes();
                        unconfirmedTranFee.setTransSignature(transSignatureFee);
                        logger.warn("投票手续费流水：" + unconfirmedTranFee.toString());
                        builderFee.setUnconfirmedTran(MessageManager.createUnconfirmedTranMessage(unconfirmedTranFee));

                        List<String> hostList = nioSocketChannelManager.getChannelHostList();
                        builder.addAllNodeAddressList(hostList);
                        builderFee.addAllNodeAddressList(hostList);

                        nioSocketChannelManager.write(builderFee.build());
                        nioSocketChannelManager.write(builder.build());
                    }
                    if (eventName.equals(EventEnum.token.name())) {
                        InesvMessage.Message.Builder builder = MessageManager.createMessageBuilder(RESPONSE, NEW_TRANSACTION);
                        InesvMessage.Message.Builder builderFee = MessageManager.createMessageBuilder(RESPONSE, NEW_TRANSACTION);
                        ECKey ecKeyFee = new ECKey(new SecureRandom());//生成手续费存放账户
                        String parameter = StringUtils.substringBetween(eventVar, "(", ")");
                        String[] eventParams = parameter.split(",");//事件里的参数
                        //去除参数里面空格
                        for (int i = 0; i < eventParams.length; i++) eventParams[i] = eventParams[i].trim();
                        int decimalsInt = 0;
                        Long tokenAmountLong = 0l;
                        //统计手续费
                        for (String sets : list) {
                            String[] set = ArrayUtils.removeAllOccurences(sets.split(" "), "");
                            if (set[2].equals(eventParams[0])) {
                                if (6 > Integer.valueOf(set[4]) || Integer.valueOf(set[4]) > 8) {
                                    res.setCode(Res.CODE_125);
                                    return res;
                                }
                                decimalsInt = Integer.valueOf(set[4]);
                            }
                            if (set[2].equals(eventParams[1])) {
                                if (Long.parseLong(set[4]) < 1000l || Long.parseLong(set[4]) >= 100000000000l) {
                                    res.setCode(Res.CODE_124);
                                    return res;
                                }
                                tokenAmountLong = Long.parseLong(set[4]);
                            }
                            if (set[2].equals(eventParams[2])) {
                                set[4] = set[4].replace("\"", "");
                                if (set[4].length() < 3 || set[4].length() > 20 || !ValidateUtil.checkLetter(set[4])) {
                                    res.setCode(Res.CODE_127);
                                    return res;
                                }
                                tokenName = set[4];
                            }
                        }
                        //校验tokenname是否存在 start
                        boolean isExistUnconfirmedTrans = unconfirmedTranManager.isExistKeyEqualsIgnoreCase(tokenName);
                        Integer confirmTransaction = transactionRepository.findTransByTokenName(tokenName);
                        if (isExistUnconfirmedTrans || confirmTransaction > 0) {
                            res.setCode(Res.CODE_123);
                            return res;
                        }
                        //校验tokenname是否存在 end
                        if (tokenAmountLong >= 10000000000l && decimalsInt > 7) {
                            res.setCode(Res.CODE_143);
                            return res;
                        }
                        //计算手续费 并比较账户资产
                        double feeDouble = Double.valueOf(TokenUtil.TokensRate(tokenName) * Constants.MININUMUNIT).longValue();
                        if (feeDouble == 0) feeDouble = 1;
                        fee = new BigDecimal(feeDouble);
                        Map<String, Long> assets = assetsManager.getAccountAssets(pubKey, Constants.PTN);
                        long balance = assets.get(Constants.BALANCE);
                        if (balance < fee.longValue()) {
                            res.setCode(Res.CODE_106);
                            return res;
                        }
                        //TODO 暂存 未确定流水
                        UnconfirmedTran unconfirmedTran = new UnconfirmedTran(pubKey, contractPublicKey, "", tokenName
                                , transValue.longValue(), 0, webTime, 3, contractAddress, bin, 3, 0, exchengeTokenName);
                        byte[] transSignature = JSON.toJSONString(ECKey.fromPrivate(Hex.decode(prikeyFrom)).sign(SHAEncrypt.sha3(SerializationUtils.serialize(unconfirmedTran.toString())))).getBytes();
                        unconfirmedTran.setTransSignature(transSignature);
                        logger.warn("代币流水：" + unconfirmedTran.toString());
                        builder.setUnconfirmedTran(MessageManager.createUnconfirmedTranMessage(unconfirmedTran));
                        //手续费流水
                        UnconfirmedTran unconfirmedTranFee = new UnconfirmedTran(pubKey, Hex.toHexString(ecKeyFee.getPubKey()), "", Constants.PTN
                                , fee.longValue(), 0, webTime, 4, contractAddress, bin, 3, 0, exchengeTokenName);
                        byte[] transSignatureFee = JSON.toJSONString(ECKey.fromPrivate(Hex.decode(prikeyFrom)).sign(SHAEncrypt.sha3(SerializationUtils.serialize(unconfirmedTranFee.toString())))).getBytes();
                        unconfirmedTranFee.setTransSignature(transSignatureFee);
                        logger.warn("代币手续费流水：" + unconfirmedTranFee.toString());
                        builderFee.setUnconfirmedTran(MessageManager.createUnconfirmedTranMessage(unconfirmedTranFee));

                        List<String> hostList = nioSocketChannelManager.getChannelHostList();
                        builder.addAllNodeAddressList(hostList);
                        builderFee.addAllNodeAddressList(hostList);

                        nioSocketChannelManager.write(builderFee.build());
                        nioSocketChannelManager.write(builder.build());
                    }
                }

                resultMap.put("contractAddress", contractAddress);
                resultMap.put("bin", bin);

                res.code = Res.CODE_100;
                res.data = resultMap;
                return res;
            } catch (BusinessException e) {
                res.setCode(e.getErrorCode().getCode());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return res;
    }

    @PostMapping("regexContract")
    @ResponseBody
    public Res regexContract(@RequestBody String contract) {
        Res res = new Res();
        try {
//            res.setCode(unconfirmedTranManager.getUnconfirmedTranMap().size());
//            res.setData(JSONObject.toJSON(unconfirmedTranManager.getUnconfirmedTranMap()));
            String bin = MyClassLoader.compilerContract(contract);
            res.setData(bin);
//            File file = new File("e:/count.txt");

//            if(!file.exists()){
//                file.createNewFile();
//                List<Transaction> list = transactionRepository.findAllASC();
//                Long temp = list.get(0).getTransactionHead().getTimeStamp();
//                Map<String,Object> map = new TreeMap<>();
//                List<Transaction> li = new ArrayList<>();
//                for(int i = 0;i<list.size();i++){
//                    if(temp <= list.get(i).getTransactionHead().getTimeStamp() && list.get(i).getTransactionHead().getTimeStamp() <= (temp+(1000L*60L*60L)) ){
//                        li.add(list.get(i));
//                    }else{
////                        String s = DateUtil.stampToDate(temp) + "-" + DateUtil.stampToDate( new Long(temp+(1000L*60L*60L)));
//                        String s = DateUtil.dateToStamp(DateUtil.stampToDateHours( new Long(temp+(1000L*60L*60L))));
//                        if(map.get(s) != null){
//                            map.put(s,Long.parseLong(map.get(s).toString())+li.size());
//                        }else{
//                            map.put(s,li.size());
//                        }
//                        li.clear();
//                        temp = list.get(i).getTransactionHead().getTimeStamp();
//                    }
//                    if(i == list.size()-1){
//                        String s = DateUtil.dateToStamp(DateUtil.stampToDateHours( new Long(temp+(1000L*60L*60L))));
//                        map.put(s,li.size());
//                    }
//                }
//                List<String[]> list1 = new ArrayList<>();
//                Long a = 0L;
//                for(String s:map.keySet()){
//                    a = a + Long.parseLong(map.get(s).toString());
//                    String[] st = {s,map.get(s).toString()};
//                    list1.add(st);
//                }
//                res.setData(a);
//                datas.setDatas(list1);
//                FileUtil.writeFileContent("e:/count.txt", JSONObject.toJSON(datas).toString());
//                FileUtil.writeFileContent("e:/count.txt",list.get(list.size()-1).getBlockHeight()+"");
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    @GetMapping("getUnconfirmedTran")
    public Res getUnconfirmedTran() {
        Res res = new Res();
        res.setCode(unconfirmedTranManager.getUnconfirmedTranMap().size());
        ConcurrentHashMap<String, UnconfirmedTran> concurrentHashMap = unconfirmedTranManager.getUnconfirmedTranMap();
        res.setData(JSONObject.toJSON(concurrentHashMap));
        return res;
    }

    private static BigDecimal getAddTokenFee(String name, String tokenAmount, int decimal) {
        if (StringUtils.isBlank(name)) {
            return null;
        }
        Map resultMap = new HashMap();
        long tokenAmountLongMiniUnit = 1;
        for (int i = 0; i < decimal; i++) {
            tokenAmountLongMiniUnit *= 10;
        }
        long tokenAmountLong = Long.parseLong(tokenAmount);
        long fee = Double.valueOf(TokenUtil.TokensRate(name) * Constants.MININUMUNIT).longValue();
        if (fee == 0) fee = 1;
        return new BigDecimal(fee).divide(new BigDecimal(Constants.MININUMUNIT)).setScale(6, BigDecimal.ROUND_HALF_UP);
    }


    /**
     * 制作数据
     */
    @PostMapping("/createData")
    public void createData() {
        Map<String, String> localAccount = initializationManager.getAccountListByAddress("px0e4543cc71dd4b90002aec6b06f810b41878e2bb");
        String pwdFrom = localAccount.get(Constants.PWD);
        String pubkeyFrom = localAccount.get(Constants.PUBKEY);
        String prikeyFrom = localAccount.get(Constants.PRIKEY);
        UnconfirmedTran unconfirmedTran = new UnconfirmedTran(pubkeyFrom,
                "043f883f8b702ec94dcc5622099ac731582200c496f1109fda39d3da0d80eeb123a5339e360953becbddd60aa44b81323150071bf48a21e8e295f09c8f3c144808",
                "", Constants.PTN, 2000000000000000l, 0, Constants.GENESIS_TIME, 0);
        byte[] transSignature = JSON.toJSONString(ECKey.fromPrivate(Hex.decode(prikeyFrom)).sign(SHAEncrypt.sha3(SerializationUtils.serialize(unconfirmedTran.toString())))).getBytes();
        unconfirmedTran.setTransSignature(transSignature);
        unconfirmedTranManager.putUnconfirmedTran(Hex.toHexString(transSignature), unconfirmedTran);
    }

    private static int count(String str, Character c) {
        return StringUtils.countMatches(str, c);
    }


}
