package com.photon.photonchain.network.core;

import com.photon.photonchain.network.ehcacheManager.*;
import com.photon.photonchain.network.excutor.TotalTranExcutor;
import com.photon.photonchain.network.proto.InesvMessage;
import com.photon.photonchain.network.proto.MessageManager;
import com.photon.photonchain.network.proto.ParticipantMessage;
import com.photon.photonchain.network.utils.DateUtil;
import com.photon.photonchain.network.utils.FileUtil;
import com.photon.photonchain.network.utils.FoundryUtils;
import com.photon.photonchain.network.utils.NetWorkUtil;
import com.photon.photonchain.storage.constants.Constants;
import com.photon.photonchain.storage.entity.Block;
import com.photon.photonchain.storage.entity.Token;
import com.photon.photonchain.storage.entity.Transaction;
import com.photon.photonchain.storage.entity.UnconfirmedTran;
import com.photon.photonchain.storage.repository.BlockRepository;
import com.photon.photonchain.storage.repository.TokenRepository;
import com.photon.photonchain.storage.repository.TransactionRepository;
import com.photon.photonchain.storage.utils.ContractUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;

import static com.photon.photonchain.network.proto.EventTypeEnum.EventType.ADD_PARTICIPANT;
import static com.photon.photonchain.network.proto.MessageTypeEnum.MessageType.RESPONSE;

/**
 * @Author:Lin
 * @Description:
 * @Date:17:20 2018/6/19
 * @Modified by:
 */
@Component
public class SyncTimer {
    @Autowired
    private SyncBlockManager syncBlockManager;
    @Autowired
    private NioSocketChannelManager nioSocketChannelManager;
    @Autowired
    private BlockRepository blockRepository;
    @Autowired
    private InitializationManager initializationManager;
    @Autowired
    private Verification verification;
    @Autowired
    private SyncBlock syncBlock;
    @Autowired
    private SyncUnconfirmedTran syncUnconfirmedTran;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private SyncUnconfirmedTranManager syncUnconfirmedTranManager;
    @Autowired
    private UnconfirmedTranManager unconfirmedTranManager;
    @Autowired
    private SyncToken syncToken;
    @Autowired
    private SyncParticipant syncParticipant;
    @Autowired
    private SyncTokenManager syncTokenManager;
    @Autowired
    private TokenRepository tokenRepository;
    @Autowired
    private FoundryMachineManager foundryMachineManager;
    @Autowired
    private SyncParticipantManager syncParticipantManager;
    @Autowired
    private ResetData resetData;
    @Autowired
    private TotalTranExcutor totalTranExcutor;
    @Autowired
    private StatisticalAssetsManager statisticalAssetsManager;

    @Autowired
    private FoundryMachine foundryMachine;


    private Logger logger = LoggerFactory.getLogger ( SyncTimer.class );

    public boolean syncBlockTimer = false;

    public boolean syncTransactionTimer = false;

    public boolean syncTokenTimer = false;

    public boolean syncParticipantTimer = false;

    public boolean startSyncBlock = false;

    public boolean startSyncTransaction = false;

    public boolean startSyncToken = false;

    public boolean startSyncParticipant = false;

    public synchronized void SyncBlockTimer() {
        if ( !syncBlockTimer ) {
            syncBlockTimer = true;
            Timer timer = new Timer ( );
            timer.schedule ( new TimerTask ( ) {
                long startTime = System.currentTimeMillis ( );

                @Override
                public void run() {
                    int syncBlockResponseCount = syncBlockManager.getSyncQueue ( ).size ( );
                    long waitTime = System.currentTimeMillis ( ) - startTime;
                    if ( syncBlockResponseCount >= Constants.SYNC_WAIT_COUNT || syncBlockManager.getSyncCount ( ) == syncBlockResponseCount || syncBlockResponseCount == nioSocketChannelManager.getActiveNioSocketChannelCount ( ) || waitTime > Constants.SYNC_WAIT_TIME ) {
                        startSyncBlock = true;
                        timer.cancel ( );
                        boolean coincident = syncBlockManager.isCoincident ( );
                        if ( coincident ) {
                            for (int i = 0; i < syncBlockResponseCount; i++) {
                                Map queueMap = syncBlockManager.getSyncBlockQueue ( );
                                if ( queueMap == null ) {
                                    continue;
                                }
                                List<Block> syncBlockList = (List<Block>) queueMap.get ( Constants.SYNC_BLOCK_LIST );
                                if ( !syncBlockList.isEmpty ( ) ) {
                                    boolean verifyPrevHash = Arrays.equals ( syncBlockList.get ( 0 ).getBlockHead ( ).getHashPrevBlock ( ), Hex.decode ( initializationManager.getLastBlock ( ).getBlockHash ( ) ) );
                                    if ( verifyPrevHash ) {
                                        saveBlocks ( syncBlockList );
                                        blockRepository.save ( syncBlockList );
                                        initializationManager.setLastBlock ( syncBlockList.get ( syncBlockList.size ( ) - 1 ) );
                                    } else {
                                        syncBlockManager.setSyncBlock ( true );
                                        resetData.backBlock ( 100 );
                                        logger.info ( "可能处于分叉" );
                                    }
                                    break;
                                }
                            }
                        } else {
                            for (int i = 0; i < syncBlockResponseCount; i++) {
                                long blockHeight = initializationManager.getBlockHeight ( );
                                Map queueMap = syncBlockManager.getSyncBlockQueue ( );
                                if ( queueMap == null ) {
                                    continue;
                                }
                                long syncBlockHieght = (long) queueMap.get ( Constants.SYNC_BLOCK_HEIGHT );
                                List<Block> syncBlockList = (List<Block>) queueMap.get ( Constants.SYNC_BLOCK_LIST );
                                if ( blockHeight < syncBlockHieght ) {
                                    String macAddress = (String) queueMap.get ( Constants.SYNC_MAC_ADDRESS );
                                    String localMacAddress = NetWorkUtil.getMACAddress ( );
                                    if ( !localMacAddress.equals ( macAddress ) ) {
                                        if ( syncBlockList.size ( ) == 0 ) {
                                            nioSocketChannelManager.removeTheMac ( macAddress );
                                            break;
                                        }
                                    }
                                    List<Block> saveBlock = verification.verificationSyncBlockList ( syncBlockList, macAddress );
                                    if ( saveBlock.size ( ) > 0 ) {
                                        saveBlocks ( saveBlock );
                                        blockRepository.save ( saveBlock );
                                        initializationManager.setLastBlock ( saveBlock.get ( saveBlock.size ( ) - 1 ) );
                                    }
                                }
                            }
                        }
                        if ( syncBlockManager.needSyncBlockHeight ( ) > initializationManager.getBlockHeight ( ) ) {
                            syncBlock.init ( );
                        } else {
                            syncBlockManager.setSyncBlock ( false );
                            syncUnconfirmedTran.init ( );
                        }
                    }
                }
            }, 0, 1000 );

        }
    }

    public synchronized void SyncTransactionTimer() {
        if ( !syncTransactionTimer ) {
            syncTransactionTimer = true;
            Timer timer = new Timer ( );
            timer.schedule ( new TimerTask ( ) {
                long startTime = System.currentTimeMillis ( );

                @Override
                public void run() {
                    long waitTime = System.currentTimeMillis ( ) - startTime;
                    int syncTransactionResponseCount = syncUnconfirmedTranManager.getSyncQueue ( ).size ( );
                    if ( syncTransactionResponseCount >= Constants.SYNC_WAIT_COUNT || syncUnconfirmedTranManager.getSyncCount ( ) == syncTransactionResponseCount || syncTransactionResponseCount == nioSocketChannelManager.getActiveNioSocketChannelCount ( ) || waitTime > Constants.SYNC_WAIT_TIME ) {
                        startSyncTransaction = true;
                        timer.cancel ( );
                        for (int i = 0; i < syncTransactionResponseCount; i++) {
                            Map queueMap = syncUnconfirmedTranManager.getTransactionQueue ( );
                            if ( queueMap == null ) {
                                continue;
                            }
                            List<UnconfirmedTran> syncTransactionList = (List<UnconfirmedTran>) queueMap.get ( Constants.SYNC_TRANSACTION_LIST );
                            long blockHeight = (long) queueMap.get ( Constants.SYNC_BLOCK_HEIGHT );
                            if ( initializationManager.getBlockHeight ( ) == blockHeight ) {
                                List<UnconfirmedTran> saveTransaction = verification.verificationSyncUnconfirmedTranList ( syncTransactionList );
                                if ( saveTransaction.size ( ) > 0 ) {
                                    for (UnconfirmedTran unconfirmedTran : saveTransaction) {
                                        String key = Hex.toHexString ( unconfirmedTran.getTransSignature ( ) );
                                        if ( unconfirmedTran.getTransType ( ) == 5 || unconfirmedTran.getTransType ( ) == 6 ) {
                                            Transaction contractTrans = transactionRepository.findByContract ( unconfirmedTran.getContractAddress ( ), 3 );
                                            if ( contractTrans == null ) {
                                                continue;
                                            }
                                            if ( unconfirmedTran.getTransType ( ) == 5 && contractTrans.getContractState ( ) == 2 ) {
                                                continue;
                                            }
                                            if ( unconfirmedTran.getTransType ( ) == 6 && contractTrans.getContractState ( ) == 1 ) {
                                                continue;
                                            }
                                            //TODO hwh 并发兑换或取消
                                            key = unconfirmedTran.getContractAddress ( );
                                            UnconfirmedTran unconfirmedTranVerification = unconfirmedTranManager.getUnconfirmedTranMap ( ).get ( key );
                                            if ( unconfirmedTranVerification != null ) {
                                                String from = unconfirmedTranVerification.getTransFrom ( );
                                                if ( unconfirmedTran.getTransTo ( ).equals ( from ) ) {
                                                    key = key + "_" + Constants.EXCHANGE;
                                                } else {
                                                    continue;
                                                }
                                            }
                                        } else if ( unconfirmedTran.getTransType ( ) == 3 && unconfirmedTran.getContractType ( ) == 3 ) {
                                            key = unconfirmedTran.getTokenName ( );
                                        } else if ( unconfirmedTran.getTransType ( ) == 4 && unconfirmedTran.getContractType ( ) == 3 ) {
                                            Map<String, String> binMap = ContractUtil.analisysTokenContract ( unconfirmedTran.getContractBin ( ) );
                                            if ( binMap != null ) {
                                                key = binMap.get ( "tokenName" ) + "_4";
                                            }
                                        } else if ( unconfirmedTran.getTransType ( ) == 4 && unconfirmedTran.getContractType ( ) == 2 && !unconfirmedTran.getRemark ( ).equals ( "" ) ) {
                                            key = unconfirmedTran.getContractAddress ( ) + "_4" + "_" + unconfirmedTran.getTransFrom ( );
                                        } else if ( unconfirmedTran.getTransType ( ) == 4 ) {
                                            key = unconfirmedTran.getContractAddress ( ) + "_4";
                                        }
                                        unconfirmedTranManager.putUnconfirmedTran ( key, unconfirmedTran );
                                    }
                                    Set<String> pubkeySet = new HashSet<> ( );
                                    saveTransaction.forEach ( transaction -> {
                                        pubkeySet.add ( transaction.getTransFrom ( ) );
                                        pubkeySet.add ( transaction.getTransTo ( ) );
                                    } );
                                    initializationManager.saveAddressAndPubKey ( pubkeySet );
                                }
                                break;
                            }
                        }
                        syncUnconfirmedTranManager.setSyncTransaction ( false );
                        syncTokenManager.setSyncToken ( false );
                        syncParticipant.init ( );
                    }
                }
            }, 0, 1000 );
        }
    }

    public synchronized void syncTokenTimer() {
        if ( !syncTokenTimer ) {
            syncTokenTimer = true;
            Timer timer = new Timer ( );
            timer.schedule ( new TimerTask ( ) {
                long startTime = System.currentTimeMillis ( );

                @Override
                public void run() {
                    int syncTokenResponseCount = syncTokenManager.getSyncQueue ( ).size ( );
                    long waitTime = System.currentTimeMillis ( ) - startTime;
                    if ( syncTokenResponseCount >= Constants.SYNC_WAIT_COUNT || syncTokenManager.getSyncCount ( ) == syncTokenResponseCount || syncTokenResponseCount == nioSocketChannelManager.getActiveNioSocketChannelCount ( ) || waitTime > Constants.SYNC_WAIT_TIME ) {
                        startSyncToken = true;
                        timer.cancel ( );
                        for (int i = 0; i < syncTokenResponseCount; i++) {
                            List syncTokenList = syncTokenManager.getSyncTokenQueue ( );
                            if ( syncTokenList == null ) {
                                continue;
                            }
                            List<Token> saveTokenList = verification.verificationSyncTokenList ( syncTokenList );
                            if ( saveTokenList.size ( ) > 0 ) {
                                tokenRepository.save ( saveTokenList );
                                for (Token token : saveTokenList) {
                                    initializationManager.addTokenDecimal ( token.getName ( ), token.getDecimals ( ) );
                                }
                            }
                        }
                        syncTokenManager.setSyncToken ( false );
                        syncParticipant.init ( );
                    }
                }
            }, 0, 1000 );
        }
    }

    public synchronized void syncParticipantTimer() {
        if ( !syncParticipantTimer ) {
            syncParticipantTimer = true;
            Timer timer = new Timer ( );
            timer.schedule ( new TimerTask ( ) {
                long startTime = System.currentTimeMillis ( );

                @Override
                public void run() {
                    int syncParticipantResponseCount = syncParticipantManager.getSyncQueue ( ).size ( );
                    long waitTime = System.currentTimeMillis ( ) - startTime;
                    if ( syncParticipantResponseCount >= Constants.SYNC_WAIT_COUNT || syncParticipantManager.getSyncCount ( ) == syncParticipantResponseCount || syncParticipantResponseCount == nioSocketChannelManager.getActiveNioSocketChannelCount ( ) || waitTime > Constants.SYNC_WAIT_TIME ) {
                        startSyncParticipant = true;
                        timer.cancel ( );
                        boolean finish = false;
                        for (int i = 0; i < syncParticipantResponseCount; i++) {
                            Map mapQueue = syncParticipantManager.getParticipantQueue ( );
                            if ( mapQueue == null ) {
                                continue;
                            }
                            if ( initializationManager.getBlockHeight ( ) == (Long) mapQueue.get ( Constants.SYNC_BLOCK_HEIGHT ) ) {
                                List<ParticipantMessage.Participant> participants = (List<ParticipantMessage.Participant>) mapQueue.get ( Constants.SYNC_PARTICIPANT_LIST );
                                participants.forEach ( participant -> {
                                    syncParticipantManager.addParticipant ( participant.getParticipant ( ), participant.getCount ( ) );
                                } );
                                foundryMachineManager.setWaitFoundryMachineCount ( 1 );
                                logger.info ( "同步完成铸造机列表：" + FoundryUtils.getSortingMap ( syncParticipantManager.getParticipantList ( ) ) );
                                finish = true;
                                break;
                            }
                        }
                        if ( !finish ) {
                            syncBlockManager.setSyncBlock ( true );
                            resetData.backBlock ( 2 );
                            syncBlock.init ( );
                            return;
                        }
                        if ( syncUnconfirmedTranManager.getHasNewTransaction ( ) || syncBlockManager.getHasNewBlock ( ) || syncTokenManager.getHasNewToken ( ) ) {
                            syncBlockManager.setSyncBlock ( true );
                            resetData.backBlock ( 2 );
                            syncBlock.init ( );
                        } else {
                            syncParticipantManager.setSyncParticipant ( false );
                            logger.info ( "同步完成总资产：" + initializationManager.getTokenAssets ( Constants.PTN, false ) );
                            syncBlockManager.setSyncBlock ( false );
                            syncUnconfirmedTranManager.setSyncTransaction ( false );
                            syncTokenManager.setSyncToken ( false );
                            syncParticipantManager.setSyncParticipant ( false );

                        }
                    }
                }
            }, 0, 1000 );
        }
    }


    private void saveBlocks(List<Block> saveBlock) {
        Set<String> pubkeySet = new HashSet<> ( );
        saveBlock.forEach ( block -> {
            try {
                transactionRepository.save ( block.getBlockTransactions ( ) );
                block.getBlockTransactions ( ).forEach ( transaction -> {
                    //TODO:addressAndPubkey
                    if ( transaction.getTransType ( ) != 2 ) {
                        pubkeySet.add ( transaction.getTransFrom ( ) );
                        pubkeySet.add ( transaction.getTransTo ( ) );
                    }
                    if ( transaction.getTransType ( ) == 1 ) { //TODO:token
                        Map<String, String> binMap = ContractUtil.analisysTokenContract ( transaction.getContractBin ( ) );
                        if ( binMap != null ) {
                            Token token = new Token ( "", binMap.get ( "tokenName" ), "", Integer.valueOf ( binMap.get ( "tokenDecimal" ) ) );
                            tokenRepository.save ( token );
                            //token cache
                            initializationManager.addTokenDecimal ( token.getName ( ), token.getDecimals ( ) );
                        }
                    }
                } );
            } catch (Exception e) {
                e.printStackTrace ( );
            }
        } );
        initializationManager.saveAddressAndPubKey ( pubkeySet );
    }

}
