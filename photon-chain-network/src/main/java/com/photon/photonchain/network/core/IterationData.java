package com.photon.photonchain.network.core;

import com.photon.photonchain.network.ehcacheManager.*;
import com.photon.photonchain.network.excutor.IterationDataExcutor;
import com.photon.photonchain.storage.constants.Constants;
import com.photon.photonchain.storage.entity.Transaction;
import com.photon.photonchain.storage.entity.UnconfirmedTran;
import com.photon.photonchain.storage.repository.TransactionRepository;
import com.photon.photonchain.storage.utils.ContractUtil;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author:Lin
 * @Description:
 * @Date:10:45 2018/6/19
 * @Modified by:
 */
@Component
public class IterationData {
    @Autowired
    private IterationDataManager iterationDataManager;
    @Autowired
    private IterationDataExcutor iterationDataExcutor;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private Verification verification;
    @Autowired
    private InitializationManager initializationManager;
    @Autowired
    private UnconfirmedTranManager unconfirmedTranManager;
    @Autowired
    private SyncBlockManager syncBlockManager;
    @Autowired
    private SyncUnconfirmedTranManager syncUnconfirmedTranManager;
    @Autowired
    private SyncTokenManager syncTokenManager;
    @Autowired
    private SyncParticipantManager syncParticipantManager;

    private boolean start = false;

    public synchronized boolean putUnconfirmedTran(UnconfirmedTran unconfirmedTran) {
        ConcurrentHashMap<String, UnconfirmedTran> unconfirmedTranMap = iterationDataManager.getUnconfirmedTranMap();
        String key = unconfirmedTran.getTimeStamp() + Hex.toHexString(unconfirmedTran.getTransSignature());
        if (!unconfirmedTranMap.containsKey(key)) {
            unconfirmedTranMap.put(key, unconfirmedTran);
            if (!start) {
                iterationDataExcutor.execute(() -> {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    saveUnconfirmedTran(unconfirmedTranMap);
                });
            }
            return true;
        }
        return false;
    }

    public synchronized void saveUnconfirmedTran(ConcurrentHashMap<String, UnconfirmedTran> unconfirmedTranMap) {
        start = true;
        while (true) {
            if (!syncBlockManager.isSyncBlock() && !syncUnconfirmedTranManager.isSyncTransaction() && !syncTokenManager.isSyncToken()&&!syncParticipantManager.isSyncParticipant()) {
                unconfirmedTranMap.forEach((s, unconfirmedTran) -> {
                    boolean save = true;
                    Transaction transaction = transactionRepository.findByTransSignature(unconfirmedTran.getTransSignature());
                    if (transaction != null) {
                        save = false;
                    } else {
                        if (verification.verificationUnconfirmedTran(unconfirmedTran)) {
                            if (unconfirmedTran.getTransType() == 5 || unconfirmedTran.getTransType() == 6) {
                                Transaction contractTrans = transactionRepository.findByContract(unconfirmedTran.getContractAddress(), 3);
                                if (contractTrans == null) {
                                    save = false;
                                }
                                if (unconfirmedTran.getTransType() == 5 && contractTrans.getContractState() == 2) {
                                    save = false;
                                }
                                if (unconfirmedTran.getTransType() == 6 && contractTrans.getContractState() == 1) {
                                    save = false;
                                }
                            }
                        } else {
                            save = false;
                        }
                    }
                    if (save) {
                        String key = Hex.toHexString(unconfirmedTran.getTransSignature());
                        if (unconfirmedTran.getTransType() == 5 || unconfirmedTran.getTransType() == 6) {
                            //TODO hwh 并发兑换或取消
                            key = unconfirmedTran.getContractAddress();
                            UnconfirmedTran unconfirmedTranVerification = unconfirmedTranManager.getUnconfirmedTranMap().get(key);
                            if (unconfirmedTranVerification != null) {
                                String from = unconfirmedTranVerification.getTransFrom();
                                if (unconfirmedTran.getTransTo().equals(from)) {
                                    key = key + "_" + Constants.EXCHANGE;
                                }
                            }
                        } else if (unconfirmedTran.getTransType() == 3 && unconfirmedTran.getContractType() == 3) {
                            key = unconfirmedTran.getTokenName();
                        } else if (unconfirmedTran.getTransType() == 4 && unconfirmedTran.getContractType() == 3) {
                            Map<String, String> binMap = ContractUtil.analisysTokenContract(unconfirmedTran.getContractBin());
                            if (binMap != null) {
                                key = binMap.get("tokenName") + "_4";
                            }
                        } else if (unconfirmedTran.getTransType() == 4 && unconfirmedTran.getContractType() == 2 && !unconfirmedTran.getRemark().equals("")) {
                            key = unconfirmedTran.getContractAddress() + "_4" + "_" + unconfirmedTran.getTransFrom();
                        } else if (unconfirmedTran.getTransType() == 4) {
                            key = unconfirmedTran.getContractAddress() + "_4";
                        }
                        unconfirmedTranManager.putUnconfirmedTran (key, unconfirmedTran );
                    }
                    if (unconfirmedTran.getTransType() == 5) {
                        String contractAddress = unconfirmedTran.getUniqueAddress().substring(0, unconfirmedTran.getUniqueAddress().indexOf(","));
                        if (unconfirmedTranManager.queryUnconfirmedTran(contractAddress, 5, null, null, -1,"").size() == 2) {
                            Transaction transaction1 = new Transaction();
                            transaction1.setContractAddress(contractAddress);
                            transaction1.setContractState(1);
                            transaction1.setTransType(3);
                            transactionRepository.updateTransactionState(transaction1);
                            initializationManager.removeContract(contractAddress);
                        }
                    }
                    if (unconfirmedTran.getTransType() == 6) {
                        Transaction transaction1 = new Transaction();
                        transaction1.setContractAddress(unconfirmedTran.getContractAddress());
                        transaction1.setTransType(3);
                        transaction1.setContractState(2);
                        transactionRepository.updateTransactionState(transaction1);
                        initializationManager.removeCancelContract(unconfirmedTran.getContractAddress());
                    }
                    Set<String> pubkeySet = new HashSet<>();
                    pubkeySet.add(unconfirmedTran.getTransFrom());
                    pubkeySet.add(unconfirmedTran.getTransTo());
                    initializationManager.saveAddressAndPubKey(pubkeySet);
                    unconfirmedTranMap.remove(s);
                });
                break;
            } else {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        start = false;
    }
}
