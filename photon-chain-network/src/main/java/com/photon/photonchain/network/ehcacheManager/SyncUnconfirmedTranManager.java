package com.photon.photonchain.network.ehcacheManager;

import com.photon.photonchain.network.proto.MessageManager;
import com.photon.photonchain.network.proto.UnconfirmedTranMessage;
import com.photon.photonchain.storage.constants.Constants;
import com.photon.photonchain.storage.entity.UnconfirmedTran;
import net.sf.ehcache.Cache;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @Author:Lin
 * @Description:
 * @Date:16:27 2018/1/19
 * @Modified by:
 */
@Component
public class SyncUnconfirmedTranManager {
    private Cache syncTransactionCache = EhCacheManager.getCache("syncTransactionCache");

    private static final String SYNC_TRANSACTION = "SYNC_TRANSACTION";

    private static final String SYNC_COUNT = "SYNC_COUNT";

    private static final String SYNC_TRANSACTION_QUEUE = "SYNC_TRANSACTION_QUEUE";

    private static final String SYNC_UNCOMFIRMEDUNVARTRAN_QUEUE = "SYNC_UNCOMFIRMEDUNVARTRAN_QUEUE";

    private static final String HAS_NEW_TRANSACTION = "HAS_NEW_TRANSACTION";

    private static final String BLOCK_HEIGHT = "BLOCK_HEIGHT";

    public void setSyncTransaction(boolean syncTransaction) {
        EhCacheManager.put(syncTransactionCache, SYNC_TRANSACTION, syncTransaction);
    }

    public boolean isSyncTransaction() {
        return EhCacheManager.getCacheValue(syncTransactionCache, SYNC_TRANSACTION, boolean.class);
    }

    public void setSyncCount(int syncCount) {
        EhCacheManager.put(syncTransactionCache, SYNC_COUNT, syncCount);
    }

    public int getSyncCount() {
        return EhCacheManager.getCacheValue(syncTransactionCache, SYNC_COUNT, int.class);
    }

    public void setTransactionQueue() {
        Queue<Map> syncTransactionQueue = new LinkedList<Map>();
        EhCacheManager.put(syncTransactionCache, SYNC_TRANSACTION_QUEUE, syncTransactionQueue);
    }

    public Map getTransactionQueue() {
        Queue<Map> transactionQueue = EhCacheManager.getCacheValue(syncTransactionCache, SYNC_TRANSACTION_QUEUE, Queue.class);
        Map queueMap = transactionQueue.poll();
        EhCacheManager.put(syncTransactionCache, SYNC_TRANSACTION_QUEUE, transactionQueue);
        return queueMap;
    }

    public synchronized Queue<Map> addTransactionQueue(List<UnconfirmedTranMessage.UnconfirmedTran> transactionList, long blockHeight) {
        Queue<Map> syncTransactionQueue = EhCacheManager.getCacheValue(syncTransactionCache, SYNC_TRANSACTION_QUEUE, Queue.class);
        if (getBlockHeight() < blockHeight) {
            setBlockHeight(blockHeight);
        }
        List<UnconfirmedTran> syncTransactionList = new ArrayList<>();
        transactionList.forEach(transaction -> {
            syncTransactionList.add(MessageManager.paresUnconfirmedTranMessage(transaction));
        });
        Map queueMap = new HashMap();
        queueMap.put(Constants.SYNC_TRANSACTION_LIST, syncTransactionList);
        queueMap.put(Constants.SYNC_BLOCK_HEIGHT, blockHeight);
        syncTransactionQueue.offer(queueMap);
        EhCacheManager.put(syncTransactionCache, SYNC_TRANSACTION_QUEUE, syncTransactionQueue);
        return syncTransactionQueue;
    }

    public void setHasNewTransaction(boolean hasNewBlock) {
        EhCacheManager.put(syncTransactionCache, HAS_NEW_TRANSACTION, hasNewBlock);
    }

    public boolean getHasNewTransaction() {
        return EhCacheManager.getCacheValue(syncTransactionCache, HAS_NEW_TRANSACTION, boolean.class);
    }

    public void setBlockHeight(long blockHeight) {
        EhCacheManager.put(syncTransactionCache, BLOCK_HEIGHT, blockHeight);
    }

    public long getBlockHeight() {
        return EhCacheManager.getCacheValue(syncTransactionCache, BLOCK_HEIGHT, long.class);
    }

    //添加未验证未确认流水到队列
    public List getUnComfirmedUnVarTranQueue() {
        Queue<List> unComfirmedUnVarTranQueue = EhCacheManager.getCacheValue(syncTransactionCache, SYNC_UNCOMFIRMEDUNVARTRAN_QUEUE, Queue.class);
        List queueList = unComfirmedUnVarTranQueue.poll();
        EhCacheManager.put(syncTransactionCache, SYNC_UNCOMFIRMEDUNVARTRAN_QUEUE, unComfirmedUnVarTranQueue);
        return queueList;
    }

    public void setUnComfirmedUnVarTranQueue() {
        Queue<List> syncUnComfirmedUnVarTranQueue = new LinkedList<List>();
        EhCacheManager.put(syncTransactionCache, SYNC_UNCOMFIRMEDUNVARTRAN_QUEUE, syncUnComfirmedUnVarTranQueue);
    }

    public synchronized Queue<List> addUnComfirmedUnVarTranQueue(List<UnconfirmedTranMessage.UnconfirmedTran> unconfirmedUnVarTranList) {
        Queue<List> syncUnComfirmedUnVarTranQueue = EhCacheManager.getCacheValue(syncTransactionCache, SYNC_UNCOMFIRMEDUNVARTRAN_QUEUE, Queue.class);
        List<UnconfirmedTran> syncUnconfirmedTranList = new ArrayList<>();
        unconfirmedUnVarTranList.forEach(unconfirmedTran -> {
            syncUnconfirmedTranList.add(MessageManager.paresUnconfirmedTranMessage(unconfirmedTran));
        });
        syncUnComfirmedUnVarTranQueue.offer(syncUnconfirmedTranList);
        EhCacheManager.put(syncTransactionCache, SYNC_UNCOMFIRMEDUNVARTRAN_QUEUE, syncUnComfirmedUnVarTranQueue);
        return syncUnComfirmedUnVarTranQueue;
    }

    public Queue<Map> getSyncQueue() {
        return EhCacheManager.getCacheValue(syncTransactionCache, SYNC_TRANSACTION_QUEUE, Queue.class);
    }
}
