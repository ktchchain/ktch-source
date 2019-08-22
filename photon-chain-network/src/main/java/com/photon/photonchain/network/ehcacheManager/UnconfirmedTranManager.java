package com.photon.photonchain.network.ehcacheManager;

import com.alibaba.fastjson.JSON;
import com.photon.photonchain.network.utils.FileUtil;
import com.photon.photonchain.storage.constants.Constants;
import com.photon.photonchain.storage.entity.Transaction;
import com.photon.photonchain.storage.entity.UnconfirmedTran;
import com.photon.photonchain.storage.utils.ContractUtil;
import net.sf.ehcache.Cache;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author:Lin
 * @Description:
 * @Date:20:02 2018/1/18
 * @Modified by:
 */
@Component
public class UnconfirmedTranManager {

    private final static String UNCONFIRMEDTRAN_MAP = "unconfirmedTranMap";

    private final static String NO_DEL_MAP = "NO_DEL_MAP";

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(UnconfirmedTranManager.class);

    private Cache unconfirmedTranCache = EhCacheManager.getCache("unconfirmedTranCache");

    public void putUnconfirmedTran(String key, UnconfirmedTran unconfirmedtran) {
        Map<String, UnconfirmedTran> map = EhCacheManager.getCacheValue(unconfirmedTranCache, UNCONFIRMEDTRAN_MAP, Map.class);
        map.put(key, unconfirmedtran);
    }


    public void setUnconfirmedtranMap() {
        EhCacheManager.put(unconfirmedTranCache, UNCONFIRMEDTRAN_MAP, new ConcurrentHashMap());
    }

    public ConcurrentHashMap<String, UnconfirmedTran> getUnconfirmedTranMap() {
        if (EhCacheManager.existKey(unconfirmedTranCache, UNCONFIRMEDTRAN_MAP)) {
            return EhCacheManager.getCacheValue(unconfirmedTranCache, UNCONFIRMEDTRAN_MAP, ConcurrentHashMap.class);
        } else {
            ConcurrentHashMap concurrentHashMap = new ConcurrentHashMap();
            EhCacheManager.put(unconfirmedTranCache, UNCONFIRMEDTRAN_MAP, concurrentHashMap);
            return concurrentHashMap;
        }
    }

    public ConcurrentHashMap getCloneUnconfirmedTranMap() {
        if (EhCacheManager.existKey(unconfirmedTranCache, UNCONFIRMEDTRAN_MAP)) {
            return FileUtil.clone(EhCacheManager.getCacheValue(unconfirmedTranCache, UNCONFIRMEDTRAN_MAP, ConcurrentHashMap.class));
        } else {
            ConcurrentHashMap concurrentHashMap = new ConcurrentHashMap();
            EhCacheManager.put(unconfirmedTranCache, UNCONFIRMEDTRAN_MAP, concurrentHashMap);
            return FileUtil.clone(concurrentHashMap);
        }
    }

    public void setNoDelUnconfirmedtranMap() {
        EhCacheManager.put(unconfirmedTranCache, NO_DEL_MAP, new ConcurrentHashMap());
    }

    public ConcurrentHashMap<String, Integer> getNoDelUnconfirmedtranMap() {
        if (EhCacheManager.existKey(unconfirmedTranCache, NO_DEL_MAP)) {
            return EhCacheManager.getCacheValue(unconfirmedTranCache, NO_DEL_MAP, ConcurrentHashMap.class);
        } else {
            ConcurrentHashMap concurrentHashMap = new ConcurrentHashMap();
            EhCacheManager.put(unconfirmedTranCache, NO_DEL_MAP, concurrentHashMap);
            return concurrentHashMap;
        }
    }

    public void deleteUnconfirmedTrans(List<UnconfirmedTran> unconfirmedTrans) throws Exception {
        Map<String, UnconfirmedTran> map = getUnconfirmedTranMap();
        logger.error(map.size() + "删除未确认流水前");
        Map<String, Integer> delMap = getNoDelUnconfirmedtranMap();
        for (UnconfirmedTran unconfirmedTran : unconfirmedTrans) {
            logger.warn("删除成功！：" + Hex.toHexString(unconfirmedTran.getTransSignature()));
            if (unconfirmedTran.getTransType() == 5 || unconfirmedTran.getTransType() == 6) {
                String keyOne = unconfirmedTran.getContractAddress();
                UnconfirmedTran delUnconfirmedTran_one = map.remove(keyOne);
                if (delUnconfirmedTran_one == null) {
                    delMap.put(keyOne, 0);
                }
                String keyTwo = unconfirmedTran.getContractAddress() + "_" + Constants.EXCHANGE;
                UnconfirmedTran delUnconfirmedTran_two = map.remove(keyTwo);
                if (delUnconfirmedTran_two == null) {
                    delMap.put(keyTwo, 0);
                }
            } else {
                String key = Hex.toHexString(unconfirmedTran.getTransSignature());
                UnconfirmedTran delUnconfirmedTran = map.remove(key);
                if (delUnconfirmedTran == null) {
                    delMap.put(key, 0);
                }
            }
        }
        logger.error(map.size() + "删除未确认流水后");
    }

    public void deleteTransactions(List<Transaction> transactions) throws Exception {
        Map<String, UnconfirmedTran> map = getUnconfirmedTranMap();
        logger.error(map.size() + "删除确认流水前");
        Map<String, Integer> delMap = getNoDelUnconfirmedtranMap();
        for (Transaction transaction : transactions) {
            String key = "";
            logger.warn("删除成功！：" + Hex.toHexString(transaction.getTransSignature()));
            if (transaction.getTransType() == 5 || transaction.getTransType() == 6) {
                String keyOne = transaction.getContractAddress();
                UnconfirmedTran delUnconfirmedTran_one = map.remove(keyOne);
                if (delUnconfirmedTran_one == null) {
                    delMap.put(keyOne, 0);
                }
                String keyTwo = transaction.getContractAddress() + "_" + Constants.EXCHANGE;
                UnconfirmedTran delUnconfirmedTran_two = map.remove(keyTwo);
                if (delUnconfirmedTran_two == null) {
                    delMap.put(keyTwo, 0);
                }
            } else if (transaction.getTransType() == 3 && transaction.getContractType() == 3) {
                key = transaction.getTokenName();
            } else if (transaction.getTransType() == 4 && transaction.getContractType() == 3) {
                Map<String, String> binMap = ContractUtil.analisysTokenContract(transaction.getContractBin());
                if (binMap != null) {
                    key = binMap.get("tokenName") + "_4";
                }
            } else if (transaction.getTransType() == 4 && transaction.getContractType() == 2 && !transaction.getRemark().equals("")) {
                key = transaction.getContractAddress() + "_4" + "_" + transaction.getTransFrom();
            } else if (transaction.getTransType() == 4) {
                key = transaction.getContractAddress() + "_4";
            } else {
                key = Hex.toHexString(transaction.getTransSignature());
            }
            UnconfirmedTran delUnconfirmedTran = map.remove(key);
            if (delUnconfirmedTran == null) {
                delMap.put(key, 0);
            }
        }
        logger.error(map.size() + "删除确认流水后");
    }

    public void removeTheUnconfirmedTran() {
        ConcurrentHashMap<String, Integer> delMap = getNoDelUnconfirmedtranMap();//获取到未被删除的未确认流水的KEY
        logger.info("未被删除的未确认流水", JSON.toJSONString(delMap));
        Map<String, UnconfirmedTran> unMap = getUnconfirmedTranMap();
        for (String key : delMap.keySet()) {
            UnconfirmedTran tran = unMap.remove(key);
            if (tran == null) {
                int value = delMap.get(key);
                if (value >= 100) {
                    logger.info("超过100次移除");
                    delMap.remove(key);
                } else {
                    delMap.put(key, value++);
                }
            } else {
                delMap.remove(key);
            }
        }
    }

    //contractAddress transType transFrom transTo timeStamp
    public List<UnconfirmedTran> queryUnconfirmedTran(String contractAddress, int transType, String transFrom, String transTo, long timeStamp,String remark) {
        UnconfirmedTran unconfirmedTran = new UnconfirmedTran();
        Map<String, UnconfirmedTran> map = getCloneUnconfirmedTranMap();
        List<UnconfirmedTran> list = new ArrayList<>();
        List<String> li = new ArrayList<>();
        if (StringUtils.isNotBlank(contractAddress)) {
            unconfirmedTran.setContractAddress(contractAddress);
            li.add("ContractAddress");
        }
        if (-1 != transType) {
            unconfirmedTran.setTransType(transType);
            li.add("TransType");
        }
        if (StringUtils.isNotBlank(transFrom)) {
            unconfirmedTran.setTransFrom(transFrom);
            li.add("TransFrom");
        }
        if (StringUtils.isNotBlank(transTo)) {
            unconfirmedTran.setTransTo(transTo);
            li.add("TransTo");
        }
        if (0 < timeStamp) {
            unconfirmedTran.setTimeStamp(timeStamp);
            li.add("TimeStamp");
        }
        if(StringUtils.isNotBlank(remark)){
            unconfirmedTran.setRemark(remark);
            li.add("remark");
        }
        for (String key : map.keySet()) {
            if (li.size() == 0) {
                list.add(map.get(key));
            } else {
                if (classCompareValue(unconfirmedTran, map.get(key), li)) {
                    list.add(map.get(key));
                }
            }
        }
        return list;
    }

    public List<UnconfirmedTran> queryUnconfirmedTranByContractType(String tokenName, int contractType) {
        UnconfirmedTran unconfirmedTran = new UnconfirmedTran();
        Map<String, UnconfirmedTran> map = getCloneUnconfirmedTranMap();
        List<UnconfirmedTran> list = new ArrayList<>();
        List<String> li = new ArrayList<>();
        if (StringUtils.isNotBlank(tokenName)) {
            unconfirmedTran.setTokenName(tokenName);
            li.add("TokenName");
        }
        if (-1 != contractType) {
            unconfirmedTran.setContractType(contractType);
            li.add("ContractType");
        }
        for (String key : map.keySet()) {
            if (li.size() == 0) {
                list.add(map.get(key));
            } else {
                if (classCompareValue(unconfirmedTran, map.get(key), li)) {
                    list.add(map.get(key));
                }
            }
        }
        return list;
    }

    /**
     * 根据字段比较
     *
     * @param obj1
     * @param obj2
     * @param fieldName
     * @return
     */
    public boolean classCompareValue(Object obj1, Object obj2, List<String> fieldName) {
        if (fieldName.size() == 0) {
            return false;
        }
        int flag = 0;//判断条件使用
        if (obj1 == null) {
            return false;
        }
        try {
            Class beanClass = obj1.getClass();
            Method[] ms = beanClass.getMethods();
            Class beanClass1 = obj2.getClass();
            Method[] ms2 = beanClass1.getMethods();
            for (int i = 0; i < ms.length; i++) {
                // 非get方法不取
                if (!ms[i].getName().startsWith("get")) {
                    continue;
                }
                Object objValue = null;
                objValue = ms[i].invoke(obj1, new Object[]{});
                if (objValue == null) {
                    continue;
                }
                for (String s : fieldName) {
//                    System.out.println(ms[i].getName());
//                    System.out.println(ms[i].getName().substring(3).equalsIgnoreCase(s));
//                    System.out.println(ms2[i].invoke(obj2, new Object[]{}));
                    if (ms[i].getName().equalsIgnoreCase(s) || ms[i].getName().substring(3).equalsIgnoreCase(s)) {
                        if (s.equalsIgnoreCase("TimeStamp")) {
                            if (Long.parseLong(objValue.toString()) > Long.parseLong(ms2[i].invoke(obj2, new Object[]{}).toString())) {
                                flag++;
                            }
                        } else {
                            if (objValue.equals(ms2[i].invoke(obj2, new Object[]{}))) {
                                flag++;
                            }
                        }
                    }
                }
            }
            if (flag == fieldName.size()) {
                return true;
            }
        } catch (Exception e) {
            logger.info("取方法出错！" + e.toString());
        }
        return false;
    }

    /*
    * 判断是否存在token
    * **/
    public boolean isExistKeyEqualsIgnoreCase(String key){
        boolean res = false;
        Map<String, UnconfirmedTran> map = getUnconfirmedTranMap();
        for(String keys: map.keySet()){
            if(keys.equalsIgnoreCase(key)){
                  return true;
            }
        }
        return res;
     }
}
