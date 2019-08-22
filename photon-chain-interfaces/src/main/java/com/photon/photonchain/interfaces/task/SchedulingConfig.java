package com.photon.photonchain.interfaces.task;

import com.photon.photonchain.interfaces.controller.WalletController;
import com.photon.photonchain.interfaces.utils.DateUtil;
import com.photon.photonchain.interfaces.utils.Res;
import com.photon.photonchain.network.ehcacheManager.*;
import com.photon.photonchain.network.utils.CheckVerify;
import com.photon.photonchain.storage.constants.Constants;
import com.photon.photonchain.storage.encryption.ECKey;
import com.photon.photonchain.storage.entity.Transaction;
import com.photon.photonchain.storage.entity.UnconfirmedTran;
import com.photon.photonchain.storage.repository.TransactionRepository;
import com.photon.photonchain.storage.utils.ContractUtil;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.CollectionUtils;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@EnableScheduling
public class SchedulingConfig {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private WalletController walletController;
    @Autowired
    private UnconfirmedTranManager unconfirmedTranManager;
    @Autowired
    private SyncBlockManager syncBlockManager;
    @Autowired
    private SyncParticipantManager syncParticipantManager;
    @Autowired
    private SyncUnconfirmedTranManager syncUnconfirmedTranManager;
    @Autowired
    private SyncTokenManager syncTokenManager;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private CheckVerify checkVerify;
    @Autowired
    private AssetsManager assetsManager;


    //@Scheduled(cron = "0/1 * * * * ?") // 每10s执行一次
    public void testTransferAccounts() {
        if (!syncBlockManager.isSyncBlock() && !syncUnconfirmedTranManager.isSyncTransaction() && !syncTokenManager.isSyncToken() && !syncParticipantManager.isSyncParticipant()) {
//        logger.info("定时任务启动"+unconfirmedTranRepository.countTran());
//        //transferAccounts(String transFrom, String transTo, String transValue, String fee, String passWord, String remark, String transToPubkey, String tokenName)
            String transFrom = "pxa2a680a6aed4a84313005009fca42d3f235cc11e";
            String transTo = "px87b0157c0bf8566b378515e9f3669380c66e2784";
            String transValue = new BigDecimal(RandomUtils.nextDouble(0.01, 0.09)).setScale(3, BigDecimal.ROUND_HALF_UP).toString();
            String fee = "0.000001";
            String passWord = "845820";
            String remark = "";
            String transToPubkey = "0466f082205f126e61575fc423e235dd821617c4a1e96439391d80225c64446eace09d1daea14f03fba8593ece8078ce8517a28b208344821d8f1fe72b0d4ec124";
            String tokenName = "ptn";
            Res res = walletController.transferAccounts(transFrom, transTo, transValue, fee, passWord, remark, transToPubkey, tokenName);
            if (res.getCode() == 200)
                logger.info(transFrom + ":" + transTo + ":" + transValue + ":" + fee);
            else
                logger.error(res.getCode() + "");
            logger.info("定时任务结束");
        }
    }

    /**
     * 处理未确认流水
     * 5分钟执行一次：0 5 0 * * ?
     * 5秒执行一次：0/5 * * * * ?
     */
    @Scheduled(cron = "0 0/5 * * * ?")
    @Transactional
    public void deleteOneTypeFive() {
        try {
            long webTime = DateUtil.getWebTime();
            if (webTime != Constants.GENESIS_TIME) {
                //long time = webTime - 300000;//五分钟之前的时间
                long time = webTime - 60 * 1000;
                Map mapxx = unconfirmedTranManager.getUnconfirmedTranMap();
                List<UnconfirmedTran> allList = unconfirmedTranManager.queryUnconfirmedTran(null, -1, null, null, time, "");//五分钟之前的未确认流水
                List<String> delteList = new ArrayList<>();//KEY
                if (!allList.isEmpty() && allList.size() > 0) {
                    for (UnconfirmedTran unconfirmedTran : allList) {
                        switch (unconfirmedTran.getTransType()) {
                            case 1:
                                //代币
                                int result = transactionRepository.findTransByTokenNameAndType(unconfirmedTran.getTokenName(), 1);
                                if (result > 0) {
                                    delteList.add(Hex.toHexString(unconfirmedTran.getTransSignature()));
                                    delteList.add(unconfirmedTran.getTokenName());
                                }
                                break;
                            case 3:
                                //合约资金
                                List<UnconfirmedTran> fundsListTypeThree = unconfirmedTranManager.queryUnconfirmedTran(unconfirmedTran.getContractAddress(), 4, null, null, -1, "");
                                switch (unconfirmedTran.getContractType()) {
                                    case 1://挂单合约
                                        Transaction transaction_one = transactionRepository.findContract(unconfirmedTran.getContractAddress(), 4, 1);//挂单
                                        if (transaction_one == null && fundsListTypeThree.isEmpty()) {
                                            delteList.add(Hex.toHexString(unconfirmedTran.getTransSignature()));
                                            break;
                                        }
                                        break;
                                    case 2://投票合约
                                        Transaction transaction_two = transactionRepository.findContract(unconfirmedTran.getContractAddress(), 4, 2);//投票
                                        if (transaction_two == null && fundsListTypeThree.isEmpty()) {
                                            delteList.add(Hex.toHexString(unconfirmedTran.getTransSignature()));
                                            break;
                                        }
                                        break;
                                    case 3://代币合约
                                        Transaction transaction_three = transactionRepository.findContract(unconfirmedTran.getContractAddress(), 4, 3);//代币
                                        if (transaction_three == null && fundsListTypeThree.isEmpty()) {
                                            delteList.add(unconfirmedTran.getTokenName());
                                            break;
                                        }
                                        break;
                                }
                                break;
                            case 4:
                                String key = Hex.toHexString(unconfirmedTran.getTransSignature());
                                if (unconfirmedTran.getContractType() == 3) {
                                    Map<String, String> binMap = ContractUtil.analisysTokenContract(unconfirmedTran.getContractBin());
                                    if (binMap != null) {
                                        key = binMap.get("tokenName") + "_4";
                                    }
                                } else if (unconfirmedTran.getContractType() == 2 && !unconfirmedTran.getRemark().equals("")) {
                                    key = unconfirmedTran.getContractAddress() + "_4" + "_" + unconfirmedTran.getTransFrom();
                                } else {
                                    key = unconfirmedTran.getContractAddress() + "_4";
                                }
                                if (unconfirmedTran.getContractType() == 2) {
                                    System.out.println("xxxxx");
                                }
                                //校验是否存在对应的合约
                                List<UnconfirmedTran> fundsListTypeFour = unconfirmedTranManager.queryUnconfirmedTran(unconfirmedTran.getContractAddress(), 3, null, null, -1, "");
                                switch (unconfirmedTran.getContractType()) {
                                    case 1://挂单合约
                                        Transaction transaction_one = transactionRepository.findContract(unconfirmedTran.getContractAddress(), 3, 1);//挂单
                                        if (transaction_one == null && fundsListTypeFour.isEmpty()) {
                                            delteList.add(key);
                                        }
                                        break;
                                    case 2://投票合约
                                        if ("".equals(unconfirmedTran.getRemark())) {//投票合约资金流水
                                            Transaction transaction_two = transactionRepository.findContract(unconfirmedTran.getContractAddress(), 3, 2);//投票
                                            if (transaction_two == null && fundsListTypeFour.isEmpty()) {
                                                delteList.add(key);
                                            }
                                        } else {//投票选项的资金流水
                                            //判断选项是否存在
                                            int choseTime = transactionRepository.findVode(unconfirmedTran.getContractAddress(), unconfirmedTran.getTransFrom(), 7, 2, unconfirmedTran.getRemark());
                                            List<UnconfirmedTran> fundsList = unconfirmedTranManager.queryUnconfirmedTran(unconfirmedTran.getContractAddress(), 7, unconfirmedTran.getTransFrom(), null, -1, unconfirmedTran.getRemark());
                                            if (fundsList.isEmpty() && choseTime == 0) {
                                                delteList.add(key);
                                            }
                                        }
                                        break;
                                    case 3://代币合约
                                        Transaction transaction_three = transactionRepository.findContract(unconfirmedTran.getContractAddress(), 3, 3);//代币
                                        if (transaction_three == null && fundsListTypeFour.isEmpty()) {
                                            delteList.add(key);
                                        }
                                        break;
                                }
                                break;
                            case 7:
                                //校验是否有对应资金流水
                                List<UnconfirmedTran> fundsListTypeSeven = unconfirmedTranManager.queryUnconfirmedTran(unconfirmedTran.getContractAddress(), 4, unconfirmedTran.getTransFrom(), null, -1, unconfirmedTran.getRemark());
                                Integer transaction_seven = transactionRepository.findVodes(unconfirmedTran.getContractAddress(), unconfirmedTran.getTransFrom(), 4, 2, unconfirmedTran.getRemark());//投票
                                if (transaction_seven == 0 && fundsListTypeSeven.isEmpty()) {
                                    delteList.add(Hex.toHexString(unconfirmedTran.getTransSignature()));
                                }
                                //校验截止条件
                                checkVerify.setBin(unconfirmedTran.getContractBin());
                                checkVerify.analysisAndPushHeap();//解析bin
                                Map<String, Object> map = new HashMap<>();
                                map.put("function", "vote");
                                map.put("contractAddress", unconfirmedTran.getTransTo());
                                map.put("bin", unconfirmedTran.getContractBin());//合约bin
                                checkVerify.analysisAndGetOpcode(map);
                                if (checkVerify.getVerify()) {//ture 满足截止条件
                                    delteList.add(Hex.toHexString(unconfirmedTran.getTransSignature()));
                                }
                                break;
                        }
                    }
                }
                ConcurrentHashMap<String, UnconfirmedTran> map = unconfirmedTranManager.getUnconfirmedTranMap();
                for (String key : delteList) {
                    map.remove(key);
                }
                List<UnconfirmedTran> list = unconfirmedTranManager.queryUnconfirmedTran(null, 5, null, null, time, "");//五分钟之前的未确认流水
                for (UnconfirmedTran unconfirmedTran : list) {
                    List<UnconfirmedTran> list2 = unconfirmedTranManager.queryUnconfirmedTran(unconfirmedTran.getContractAddress(), 5, null, null, 0, "");//查找类型为5的相同合约地址未确认流水
                    if (list2.size() == 1) {
                        unconfirmedTranManager.deleteUnconfirmedTrans(list2);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public boolean verifiUntransBalance(UnconfirmedTran unconfirmedTran, Map<String, Long> fromAssets, Map<String, Long> fromAssetsPtn) {
        boolean verifyTrade = false;
        boolean verifyFee = false;
        if (!Constants.PTN.equalsIgnoreCase(unconfirmedTran.getTokenName())) {
            boolean verifyTransfer = fromAssets.get(Constants.BALANCE) >= unconfirmedTran.getTransValue() ? true : false;
            verifyFee = fromAssetsPtn.get(Constants.BALANCE) >= unconfirmedTran.getFee() ? true : false;
            if (verifyTransfer && verifyFee) {
                verifyTrade = true;
            }
        } else {
            boolean verifyTransfer = fromAssets.get(Constants.BALANCE) >= unconfirmedTran.getTransValue() + unconfirmedTran.getFee() ? true : false;
            if (verifyTransfer) {
                verifyTrade = true;
            }
        }
        return verifyTrade;
    }
}
