package com.photon.photonchain.network.core;


import com.alibaba.fastjson.JSON;
import com.photon.photonchain.network.ehcacheManager.*;
import com.photon.photonchain.network.excutor.FoundryMachineExcutor;
import com.photon.photonchain.network.proto.InesvMessage;
import com.photon.photonchain.network.proto.MessageManager;
import com.photon.photonchain.network.utils.DateUtil;
import com.photon.photonchain.network.utils.FoundryUtils;
import com.photon.photonchain.storage.constants.Constants;
import com.photon.photonchain.storage.encryption.ECKey;
import com.photon.photonchain.storage.encryption.HashMerkle;
import com.photon.photonchain.storage.encryption.SHAEncrypt;
import com.photon.photonchain.storage.entity.*;
import com.photon.photonchain.storage.repository.TransactionRepository;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.jcajce.provider.symmetric.Blowfish;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.*;

import static com.photon.photonchain.network.proto.EventTypeEnum.EventType.DEL_PARTICIPANT;
import static com.photon.photonchain.network.proto.EventTypeEnum.EventType.NEW_BLOCK;
import static com.photon.photonchain.network.proto.MessageTypeEnum.MessageType.RESPONSE;

/**
 * @Author:Lin
 * @Description:
 * @Date:9:48 2017/12/28
 * @Modified by:
 */
@Component
public class FoundryMachine {
    private static Logger logger = LoggerFactory.getLogger(FoundryMachine.class);
    @Autowired
    private FoundryMachineExcutor foundryMachineExcutor;
    @Autowired
    private NioSocketChannelManager nioSocketChannelManager;
    @Autowired
    private InitializationManager initializationManager;
    @Autowired
    private SyncUnconfirmedTranManager syncUnconfirmedTranManager;
    @Autowired
    private SyncBlockManager syncBlockManager;
    @Autowired
    private SyncTokenManager syncTokenManager;
    @Autowired
    private FoundryMachineManager foundryMachineManager;
    @Autowired
    private FoundryUtils foundryUtils;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private UnconfirmedTranManager unconfirmedTranManager;
    @Autowired
    private SyncParticipantManager syncParticipantManager;

    private void blockFoundryMachine(byte[] foundryPublicKey, byte[] foundryPrivateKey) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                long webTime = DateUtil.getWebTime();
                if (webTime == Constants.GENESIS_TIME) {
                    return;
                }
                if (!foundryMachineManager.foundryMachineIsStart(Hex.toHexString(foundryPublicKey)) || nioSocketChannelManager.getActiveNioSocketChannelCount() < Constants.FORGABLE_NODES || syncBlockManager.isSyncBlock() || syncUnconfirmedTranManager.isSyncTransaction() || syncTokenManager.isSyncToken()) {
                    InesvMessage.Message.Builder builder = MessageManager.createMessageBuilder(RESPONSE, DEL_PARTICIPANT);
                    builder.setParticipant(Hex.toHexString(foundryPublicKey));
                    List<String> hostList = nioSocketChannelManager.getChannelHostList();
                    builder.addAllNodeAddressList(hostList);
                    nioSocketChannelManager.write(builder.build());
                    initializationManager.setFoundryMachineState(false);
                    foundryMachineManager.setFoundryMachine(Hex.toHexString(foundryPublicKey), false);
                    logger.info("锻造时节点数：" + nioSocketChannelManager.getActiveNioSocketChannelCount());
                    timer.cancel();
                    return;
                } else {
                    String foundryMachiner = foundryUtils.getFoundryMachiner();
                    logger.info("挖矿者：" + foundryMachiner);
                    if (foundryMachiner == null) {
                        return;
                    }
                    if (foundryMachiner.equals(foundryMachineManager.getWaitfoundryMachine())) {
                        foundryMachineManager.setWaitFoundryMachineCount(foundryMachineManager.getWaitFoundryMachineCount() + 1);
                    } else {
                        foundryMachineManager.setWaitfoundryMachine(foundryMachiner);
                        foundryMachineManager.setWaitFoundryMachineCount(1);
                    }
                    if (!Hex.toHexString(foundryPublicKey).equals(foundryMachiner) && foundryMachineManager.getWaitFoundryMachineCount() > 8) {
                        foundryMachineManager.setWaitfoundryMachine(null);
                        InesvMessage.Message.Builder builder = MessageManager.createMessageBuilder(RESPONSE, DEL_PARTICIPANT);
                        builder.setParticipant(foundryMachiner);
                        List<String> hostList = nioSocketChannelManager.getChannelHostList();
                        builder.addAllNodeAddressList(hostList);
                        nioSocketChannelManager.write(builder.build());
                        return;
                    }
                    long interval = webTime - initializationManager.getLastBlock().getBlockHead().getTimeStamp();
                    if (interval > Constants.BLOCK_INTERVAL && Hex.toHexString(foundryPublicKey).equals(foundryMachiner)) {
                        unconfirmedTranManager.removeTheUnconfirmedTran();
                        Block lastBlock = initializationManager.getLastBlock();
                        int version = Constants.BLOCK_VERSION;
                        long blockHeight = initializationManager.getBlockHeight() + 1;
                        List<Transaction> blockTransactions = new ArrayList<>();

                        //TODO:<合约资金流水金额大于CONTRACT_FUNDS_MIN> 或者 <数量达到1000>才能转为确认流水。
                        long transValueSum = 0;
                        List<UnconfirmedTran> unconfirmedTrans = unconfirmedTranManager.queryUnconfirmedTran(null, 4, null, null, webTime - 60 * 1000l, "");
                        for (UnconfirmedTran unconfirmedTran : unconfirmedTrans) {
                            transValueSum += unconfirmedTran.getTransValue();
                        }
                        logger.info("<<<<<<<< 资金流水sum:{},count:{} >>>>>>>>", new BigDecimal(transValueSum).divide(new BigDecimal(Constants.MININUMUNIT)), unconfirmedTrans.size());
                        if (transValueSum >= Constants.CONTRACT_FUNDS_SUM || unconfirmedTrans.size() > Constants.CONTRACT_FUNDS_COUNT) {
                            logger.warn("统计前：" + System.currentTimeMillis());
                            List<String> li = new ArrayList<>();
                            for (UnconfirmedTran unconfirmedTran : unconfirmedTrans) {
                                li.add(unconfirmedTran.getContractAddress());
                            }
                            logger.error("查询前：" + System.currentTimeMillis());
                            List<Transaction> contractTransactions = transactionRepository.findByContracts(li, 3);
                            logger.error("查询后：" + System.currentTimeMillis());
                            for (UnconfirmedTran unconfirmedTran : unconfirmedTrans) {
                                for (Transaction contractTransaction : contractTransactions) {
                                    if (contractTransaction.getContractAddress().equals(unconfirmedTran.getContractAddress())) {
                                        TransactionHead transactionHead = new TransactionHead(unconfirmedTran.getTransFrom(), Hex.toHexString(foundryPublicKey), unconfirmedTran.getTransValue(), unconfirmedTran.getFee(), unconfirmedTran.getTimeStamp());
                                        String remark = unconfirmedTran.getRemark();
                                        if (unconfirmedTran.getContractType() == 1) {
                                            remark = String.valueOf(contractTransaction.getBlockHeight());
                                        }
                                        Transaction transaction = new Transaction(unconfirmedTran.getTransSignature(), transactionHead, blockHeight, webTime - unconfirmedTran.getTimeStamp(), unconfirmedTran.getTransFrom(), Hex.toHexString(foundryPublicKey), remark, unconfirmedTran.getTokenName(), unconfirmedTran.getTransType(), unconfirmedTran.getContractAddress(), unconfirmedTran.getContractBin(), unconfirmedTran.getContractType(), unconfirmedTran.getContractState(), unconfirmedTran.getExchengeToken(), unconfirmedTran.getTransValue(), unconfirmedTran.getFee());
                                        blockTransactions.add(transaction);
                                        break;
                                    }
                                }
                            }
                            logger.warn("统计后：" + System.currentTimeMillis());
                        }
                        //TODO:unconfirm
                        List<UnconfirmedTran> unconfirmedTranList = unconfirmedTranManager.queryUnconfirmedTran(null, -1, null, null, webTime - 8000l, "");
                        List<UnconfirmedTran> li = new ArrayList<>();
                        li.addAll(unconfirmedTranList);
                        for (UnconfirmedTran unconfirmedTran : unconfirmedTranList) {
                            if (unconfirmedTran.getTransType() == 4) {
                                li.remove(unconfirmedTran);
                            }
                        }
                        unconfirmedTranList = li;

                        Collections.sort(unconfirmedTranList);
                        Collections.reverse(unconfirmedTranList);
                        int count = unconfirmedTranList.size() > Constants.BLOCK_TRANSACTION_SIZE ? Constants.BLOCK_TRANSACTION_SIZE : unconfirmedTranList.size();
                        unconfirmedTranList = unconfirmedTranList.subList(0, count);

                        long totalAmount = 0;
                        long totalFee = 0;

                        List<UnconfirmedTran> contraUnconfirmedTranList = new ArrayList<>();
                        List<String> contraUnconfirmedTranListTypeSix = new ArrayList<>();

                        Map<String, UnconfirmedTran> map = new HashMap<>();

                        for (UnconfirmedTran unconfirmedTran : unconfirmedTranList) {
                            if (unconfirmedTran.getTransType() == 5) {
                                contraUnconfirmedTranList.add(unconfirmedTran);
                                continue;
                            }
                            totalFee = totalFee + unconfirmedTran.getFee();
                            TransactionHead transactionHead = new TransactionHead(unconfirmedTran.getTransFrom(), unconfirmedTran.getTransTo(), unconfirmedTran.getTransValue(), unconfirmedTran.getFee(), unconfirmedTran.getTimeStamp());
                            Transaction transaction = new Transaction(unconfirmedTran.getTransSignature(), transactionHead, blockHeight, webTime - unconfirmedTran.getTimeStamp(), unconfirmedTran.getTransFrom(), unconfirmedTran.getTransTo(), unconfirmedTran.getRemark(), unconfirmedTran.getTokenName(), unconfirmedTran.getTransType(), unconfirmedTran.getContractAddress(), unconfirmedTran.getContractBin(), unconfirmedTran.getContractType(), unconfirmedTran.getContractState(), unconfirmedTran.getExchengeToken(), unconfirmedTran.getTransValue(), unconfirmedTran.getFee());
                            blockTransactions.add(transaction);

                            if (unconfirmedTran.getTransType() == 6) { //当有取消的合约地址时
                                contraUnconfirmedTranListTypeSix.add(unconfirmedTran.getContractAddress());
                            }
                        }

                        List<UnconfirmedTran> contraUnconfirmedTranListTure = new ArrayList<>();//真正可以确认的流水
                        for (UnconfirmedTran unconfirmedTran : contraUnconfirmedTranList) {
                            String contractAddress = unconfirmedTran.getContractAddress();//获取到合约地址
                            if (!contraUnconfirmedTranListTypeSix.contains(contractAddress)) {//判断6里面是否有5的合约流水
                                contraUnconfirmedTranListTure.add(unconfirmedTran);
                            }
                        }

                        for (UnconfirmedTran unconfirmedTran : contraUnconfirmedTranListTure) { //全都是5的
                            //TODO:unconfirm
                            List<UnconfirmedTran> unconfirmedTranList1 = unconfirmedTranManager.queryUnconfirmedTran(unconfirmedTran.getContractAddress(), 6, null, null, -1, "");
                            if (unconfirmedTranList1.size() == 0) {
                                if (map.get(unconfirmedTran.getContractAddress() + "5") != null) {//不等于空说明有两5的
                                    totalFee = totalFee + unconfirmedTran.getFee();
                                    TransactionHead transactionHead = new TransactionHead(unconfirmedTran.getTransFrom(), unconfirmedTran.getTransTo(), unconfirmedTran.getTransValue(), unconfirmedTran.getFee(), unconfirmedTran.getTimeStamp());
                                    Transaction transaction = new Transaction(unconfirmedTran.getTransSignature(), transactionHead, blockHeight, webTime - unconfirmedTran.getTimeStamp(), unconfirmedTran.getTransFrom(), unconfirmedTran.getTransTo(), unconfirmedTran.getRemark(), unconfirmedTran.getTokenName(), unconfirmedTran.getTransType(), unconfirmedTran.getContractAddress(), unconfirmedTran.getContractBin(), unconfirmedTran.getContractType(), unconfirmedTran.getContractState(), unconfirmedTran.getExchengeToken(), unconfirmedTran.getTransValue(), unconfirmedTran.getFee());
                                    blockTransactions.add(transaction);

                                    //同时处理同类型同地址流水
                                    UnconfirmedTran u = map.get(unconfirmedTran.getContractAddress() + "5");
                                    totalFee = totalFee + u.getFee();
                                    TransactionHead transactionHeadA = new TransactionHead(u.getTransFrom(), u.getTransTo(), u.getTransValue(), u.getFee(), u.getTimeStamp());
                                    Transaction transactionA = new Transaction(u.getTransSignature(), transactionHeadA, blockHeight, webTime - u.getTimeStamp(), u.getTransFrom(), u.getTransTo(), u.getRemark(), u.getTokenName(), u.getTransType(), u.getContractAddress(), u.getContractBin(), u.getContractType(), u.getContractState(), u.getExchengeToken(), u.getTransValue(), u.getFee());
                                    blockTransactions.add(transactionA);
                                } else {
                                    map.put(unconfirmedTran.getContractAddress() + "5", unconfirmedTran);
                                }
                            }
                        }


                        //TODO: 判断最新区块是否在新区块时间偏移值内
                        //TODO: 计算当前产生区块的时间离创世块时间间隔几年
                        int diffYear = FoundryUtils.getDiffYear(GenesisBlock.GENESIS_TIME, webTime);
                        //TODO: 根据产生者ECKEY和区块间隔年限得到的挖矿数量（第一年奖励范围在14个，第二年在8个，第三年在4个，之后维持，直到挖完）（用挖矿者的ECKEY公钥取后两位前后相乘保留1位，范围在0,1,2,用最佳奖励值减去偏移值，得到最终挖矿奖励）
                        long blockReward = FoundryUtils.getBlockReward(foundryPublicKey, diffYear, blockHeight, initializationManager, false);
                        ECKey ecKey = new ECKey(new SecureRandom());
                        TransactionHead miningTransactionHead = new TransactionHead(Hex.toHexString(ecKey.getPubKey()), Hex.toHexString(foundryPublicKey), totalFee + blockReward, totalFee, webTime);
                        Transaction miningTransaction = new Transaction(null, miningTransactionHead, blockHeight, 0, Hex.toHexString(ecKey.getPubKey()), Hex.toHexString(foundryPublicKey), "mining", Constants.PTN, 2, blockReward + totalFee, totalFee);
                        byte[] transSignature = JSON.toJSONString(ECKey.fromPrivate(ecKey.getPrivKeyBytes()).sign(SHAEncrypt.sha3(SerializationUtils.serialize(miningTransaction.toSignature())))).getBytes();
                        miningTransaction.setTransSignature(transSignature);
                        blockTransactions.add(miningTransaction);
                        byte[] hashPrevBlock = Hex.decode(lastBlock.getBlockHash());
                        List<byte[]> transactionSHAList = new ArrayList<>();
                        long blockSize = 0;
                        for (Transaction transaction : blockTransactions) {
                            totalAmount = totalAmount + transaction.getTransactionHead().getTransValue();
                            TransactionHead transactionHead = transaction.getTransactionHead();
                            transactionSHAList.add(SHAEncrypt.SHA256(transactionHead.toString()));
                            blockSize = blockSize + SerializationUtils.serialize(transactionHead).length;
                        }
                        byte[] hashMerkleRoot = transactionSHAList.isEmpty() ? new byte[]{} : HashMerkle.getHashMerkleRoot(transactionSHAList);
                        BlockHead blockHead = new BlockHead(version, webTime, Constants.CUMULATIVE_DIFFICULTY, hashPrevBlock, hashMerkleRoot);
                        byte[] blockSignature = JSON.toJSONString(ECKey.fromPrivate(foundryPrivateKey).sign(SHAEncrypt.sha3(SerializationUtils.serialize(blockHead)))).getBytes();
                        Block block = new Block(blockHeight, blockSize, totalAmount, totalFee, blockSignature, foundryPublicKey, blockHead, blockTransactions);
                        block.setBlockHash(Hex.toHexString(SHAEncrypt.SHA256(block.getBlockHead())));
                        InesvMessage.Message.Builder builder = MessageManager.createMessageBuilder(RESPONSE, NEW_BLOCK);
                        builder.setBlock(MessageManager.createBlockMessage(block));
                        builder.setParticipantCount(syncParticipantManager.getParticipantCount(Hex.toHexString(foundryPublicKey)));
                        List<String> hostList = nioSocketChannelManager.getChannelHostList();
                        builder.addAllNodeAddressList(hostList);
                        nioSocketChannelManager.write(builder.build());
                    }

                }
            }
        }, 30000, 30000);
    }

    public void init(String pubKey, String priKey) {
        foundryMachineExcutor.execute(() -> {
            blockFoundryMachine(
                    Hex.decode(pubKey),
                    Hex.decode(priKey));
        });
    }
}
