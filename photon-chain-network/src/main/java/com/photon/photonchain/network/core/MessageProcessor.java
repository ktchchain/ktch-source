package com.photon.photonchain.network.core;

import com.photon.photonchain.network.ehcacheManager.*;
import com.photon.photonchain.network.peer.PeerClient;
import com.photon.photonchain.network.proto.*;
import com.photon.photonchain.network.utils.CheckVerify;
import com.photon.photonchain.network.utils.FoundryUtils;
import com.photon.photonchain.network.utils.NetWorkUtil;
import com.photon.photonchain.storage.constants.Constants;
import com.photon.photonchain.storage.entity.*;
import com.photon.photonchain.storage.repository.BlockRepository;
import com.photon.photonchain.storage.repository.NodeAddressRepository;
import com.photon.photonchain.storage.repository.TokenRepository;
import com.photon.photonchain.storage.repository.TransactionRepository;
import com.photon.photonchain.storage.utils.ContractUtil;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.photon.photonchain.network.proto.EventTypeEnum.EventType.*;
import static com.photon.photonchain.network.proto.MessageTypeEnum.MessageType.RESPONSE;
import static com.photon.photonchain.network.utils.NetWorkUtil.*;

/**
 * @Author:Lin
 * @Description:
 * @Date:15:35 2018/2/6
 * @Modified by:
 */
@Component
public class MessageProcessor {

    private static Logger logger = LoggerFactory.getLogger ( MessageProcessor.class );

    @Autowired
    private InitializationManager initializationManager;
    @Autowired
    private BlockRepository blockRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private TokenRepository tokenRepository;
    @Autowired
    private SyncBlockManager syncBlockManager;
    @Autowired
    private SyncParticipantManager syncParticipantManager;
    @Autowired
    private SyncUnconfirmedTranManager syncUnconfirmedTranManager;
    @Autowired
    private SyncTokenManager syncTokenManager;
    @Autowired
    private SyncBlock syncBlock;
    @Autowired
    private NodeAddressRepository nodeAddressRepository;
    @Autowired
    private PeerClient peerClient;
    @Autowired
    private Verification verification;
    @Autowired
    private NioSocketChannelManager nioSocketChannelManager;
    @Autowired
    private ResetData resetData;
    @Autowired
    private FoundryMachineManager foundryMachineManager;
    @Autowired
    private FoundryUtils foundryUtils;
    @Autowired
    private UnconfirmedTranManager unconfirmedTranManager;
    @Autowired
    private IterationDataManager iterationDataManager;
    @Autowired
    private SyncTimer syncTimer;
    @Autowired
    private CheckVerify checkVerify;

    @Transactional(rollbackFor = Exception.class)
    public void requestProcessor(ChannelHandlerContext ctx, InesvMessage.Message msg) {
        switch (msg.getEventType ( )) {
            case SYNC_BLOCK:
                long syncBlockBlockHeight = 0;
                long requestBlockHeight = msg.getBlockHeight ( );
                List<BlockMessage.Block> blockList = new ArrayList<> ( );
                if ( !syncBlockManager.isSyncBlock ( ) && !syncUnconfirmedTranManager.isSyncTransaction ( ) && !syncTokenManager.isSyncToken ( ) && !syncParticipantManager.isSyncParticipant ( ) ) {
                    syncBlockBlockHeight = initializationManager.getBlockHeight ( );
                    if ( syncBlockBlockHeight > requestBlockHeight ) {
                        Iterable<Block> blockIterable = blockRepository.findByBlockHeight ( requestBlockHeight, requestBlockHeight + Constants.SYNC_SIZE );
                        blockIterable.forEach ( block -> {
                            blockList.add ( MessageManager.createBlockMessage ( block ) );
                        } );
                    }
                }
                InesvMessage.Message.Builder syncBlockBuilder = MessageManager.createMessageBuilder ( RESPONSE, SYNC_BLOCK );
                syncBlockBuilder.setBlockHeight ( syncBlockBlockHeight );
                syncBlockBuilder.addAllBlockList ( blockList );
                syncBlockBuilder.setMac ( NetWorkUtil.getMACAddress ( ) );
                ctx.writeAndFlush ( syncBlockBuilder.build ( ) );
                break;
            case SYNC_TRANSACTION:
                List<UnconfirmedTranMessage.UnconfirmedTran> unconfirmedTranList = new ArrayList<> ( );
                List<UnconfirmedTranMessage.UnconfirmedTran> unconfirmedUnVarTranList = new ArrayList<> ( );
                long syncTransActionBlockHeight = 0;
                if ( !syncBlockManager.isSyncBlock ( ) && !syncUnconfirmedTranManager.isSyncTransaction ( ) && !syncTokenManager.isSyncToken ( ) && !syncParticipantManager.isSyncParticipant ( ) ) {
                    syncTransActionBlockHeight = initializationManager.getBlockHeight ( );
                    ConcurrentHashMap unconfirmedTranIterable = unconfirmedTranManager.getCloneUnconfirmedTranMap ( );
                    unconfirmedTranIterable.forEach ( (k, v) -> {
                        unconfirmedTranList.add ( MessageManager.createUnconfirmedTranMessage ( (UnconfirmedTran) v ) );
                    } );
                    ConcurrentHashMap unconfirmedUnVarTranIterable = iterationDataManager.getCloneUnconfirmedTranMap ( );
                    unconfirmedUnVarTranIterable.forEach ( (k, v) -> {
                        unconfirmedUnVarTranList.add ( MessageManager.createUnconfirmedTranMessage ( (UnconfirmedTran) v ) );
                    } );
                }
                InesvMessage.Message.Builder syncTransactionBuilder = MessageManager.createMessageBuilder ( RESPONSE, SYNC_TRANSACTION );
                syncTransactionBuilder.addAllUnconfirmedTranList ( unconfirmedTranList );
                syncTransactionBuilder.addAllUnconfirmedUnVarTranList ( unconfirmedUnVarTranList );
                syncTransactionBuilder.setBlockHeight ( syncTransActionBlockHeight );
                ctx.writeAndFlush ( syncTransactionBuilder.build ( ) );
                break;
            case SYNC_TOKEN:
                List<TokenMessage.Token> tokenList = new ArrayList<> ( );
                long syncTokenBlockHeight = 0;
                if ( !syncBlockManager.isSyncBlock ( ) && !syncUnconfirmedTranManager.isSyncTransaction ( ) && !syncTokenManager.isSyncToken ( ) && !syncParticipantManager.isSyncParticipant ( ) ) {
                    syncTokenBlockHeight = initializationManager.getBlockHeight ( );
                    Iterable<Token> tokenIterable = tokenRepository.findAll ( );
                    tokenIterable.forEach ( token -> {
                        tokenList.add ( MessageManager.createTokenMessage ( token ) );
                    } );
                }
                InesvMessage.Message.Builder syncTokenBuilder = MessageManager.createMessageBuilder ( RESPONSE, SYNC_TOKEN );
                syncTokenBuilder.addAllTokenList ( tokenList );
                syncTokenBuilder.setBlockHeight ( syncTokenBlockHeight );
                ctx.writeAndFlush ( syncTokenBuilder.build ( ) );
                break;
            case SYNC_PARTICIPANT:
                List<ParticipantMessage.Participant> participantList = new ArrayList<> ( );
                long syncParticipantBlockHeight = 0;
                if ( !syncBlockManager.isSyncBlock ( ) && !syncUnconfirmedTranManager.isSyncTransaction ( ) && !syncTokenManager.isSyncToken ( ) && !syncParticipantManager.isSyncParticipant ( ) ) {
                    syncParticipantBlockHeight = initializationManager.getBlockHeight ( );
                    Map<String, Integer> participantMap = syncParticipantManager.getParticipantList ( );
                    for (String key : participantMap.keySet ( )) {
                        ParticipantMessage.Participant.Builder participant = ParticipantMessage.Participant.newBuilder ( );
                        participant.setParticipant ( key );
                        participant.setCount ( participantMap.get ( key ) );
                        participantList.add ( participant.build ( ) );
                    }
                }
                InesvMessage.Message.Builder participantBuilder = MessageManager.createMessageBuilder ( RESPONSE, SYNC_PARTICIPANT );
                participantBuilder.addAllParticipantList ( participantList );
                participantBuilder.setBlockHeight ( syncParticipantBlockHeight );
                ctx.writeAndFlush ( participantBuilder.build ( ) );
                break;
            default:
                break;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void responseProcessor(ChannelHandlerContext ctx, InesvMessage.Message msg) {
        L:
        switch (msg.getEventType ( )) {
            case NEW_BLOCK:
                logger.info ( "【NEW_BLOCK,height={}】", initializationManager.getLastBlock ( ).getBlockHeight ( ) );
                logger.info ( "【unconfirmTrans】" + unconfirmedTranManager.getUnconfirmedTranMap ( ).size ( ) );
                if ( !syncBlockManager.isSyncBlock ( ) && !syncUnconfirmedTranManager.isSyncTransaction ( ) && !syncTokenManager.isSyncToken ( ) && !syncParticipantManager.isSyncParticipant ( ) ) {
                    Block newBlock = MessageManager.parseBlockMessage ( msg.getBlock ( ) );
                    if ( initializationManager.getBlockHeight ( ) - newBlock.getBlockHeight ( ) > 3 ) {
                        ctx.close ( );
                        break L;
                    } else if ( newBlock.getBlockHeight ( ) - initializationManager.getBlockHeight ( ) > 3 ) {
                        syncBlockManager.setSyncBlock ( true );
                        resetData.backBlock ( 2 );
                        syncBlock.init ( );
                        break L;
                    }
                    if ( verification.verificationBlock ( newBlock, msg.getParticipantCount ( ) ) ) {
                        logger.info ( "区块验证通过" );
                        List<Transaction> transactionList = newBlock.getBlockTransactions ( );
                        for (Transaction transaction : transactionList) {
                            try {
                                transactionRepository.saveTransaction ( transaction );
                                if ( transaction.getTransType ( ) == 6 ) {
//                                    unconfirmedTranRepository.deleteByTypeAndAddress(transaction.getContractAddress(), 5);//删除这些未确认流水
                                    List<UnconfirmedTran> li = unconfirmedTranManager.queryUnconfirmedTran ( transaction.getContractAddress ( ), 5, null, null, -1, "" );
                                    unconfirmedTranManager.deleteUnconfirmedTrans ( li );
                                }
                                if ( transaction.getTransType ( ) == 1 ) { //TODO:token
                                    Map<String, String> binMap = ContractUtil.analisysTokenContract ( transaction.getContractBin ( ) );
                                    if ( binMap != null ) {
                                        Token token = new Token ( "", binMap.get ( "tokenName" ), "", Integer.valueOf ( binMap.get ( "tokenDecimal" ) ) );
                                        tokenRepository.save ( token );
                                        //token cache
                                        initializationManager.addTokenDecimal ( token.getName ( ), token.getDecimals ( ) );
                                        //update state
                                        Transaction transaction1 = new Transaction ( );
                                        transaction1.setContractAddress ( transaction.getContractAddress ( ) );
                                        transaction1.setTransType ( 3 );
                                        transaction1.setContractState ( 1 );
                                        transactionRepository.updateTransactionState ( transaction1 );
                                    }
                                }
                            } catch (Exception e) {
                                break L;
                            }
                        }
                        initializationManager.setLastTransaction ( transactionList.get ( transactionList.size ( ) - 1 ) );
//                        unconfirmedTranRepository.deleteByTransSignatureList(transSignatureList);
                        try {
                            unconfirmedTranManager.removeTheUnconfirmedTran ( );
                            unconfirmedTranManager.deleteTransactions ( transactionList );
                            int count = msg.getParticipantCount ( ) - 1;
                            syncParticipantManager.setParticipant ( Hex.toHexString ( newBlock.getFoundryPublicKey ( ) ), count );
                        } catch (Exception e) {
                            e.printStackTrace ( );
                        }
                        foundryMachineManager.setWaitFoundryMachineCount ( 1 );
                        blockRepository.save ( newBlock );
                        initializationManager.setLastBlock ( blockRepository.findBlockByBlockId ( newBlock.getBlockHeight ( ) ) );
                        //relayMessage ( msg );
                        //logger.info ( "新区块总资产：" + initializationManager.getTokenAssets ( Constants.PTN, false ) );
                        logger.info ( "新铸造机列表：" + FoundryUtils.getSortingMap ( syncParticipantManager.getParticipantList ( ) ) );
                    }
                } else {
                    syncBlockManager.setHasNewBlock ( true );
                }
                break;
            case NEW_TRANSACTION:
                logger.info ( "【NEW_TRANSACTION】" );
                if ( !syncBlockManager.isSyncBlock ( ) && !syncUnconfirmedTranManager.isSyncTransaction ( ) && !syncTokenManager.isSyncToken ( ) && !syncParticipantManager.isSyncParticipant ( ) ) {
                    UnconfirmedTran unconfirmedTran = MessageManager.paresUnconfirmedTranMessage ( msg.getUnconfirmedTran ( ) );
                    if ( verification.verificationUnconfirmedTran ( unconfirmedTran ) ) {
                        logger.info ( "流水验证通过" );
                        try {
                            String key = Hex.toHexString ( unconfirmedTran.getTransSignature ( ) );
                            if ( unconfirmedTran.getTransType ( ) == 5 || unconfirmedTran.getTransType ( ) == 6 ) {
                                Transaction contractTrans = transactionRepository.findByContract ( unconfirmedTran.getContractAddress ( ), 3 );
                                if ( contractTrans == null ) {
                                    break L;
                                }
                                if ( unconfirmedTran.getTransType ( ) == 5 && contractTrans.getContractState ( ) == 2 ) {
                                    break L;
                                }
                                if ( unconfirmedTran.getTransType ( ) == 6 && contractTrans.getContractState ( ) == 1 ) {
                                    break L;
                                }
                                //TODO hwh 并发兑换或取消
                                key = unconfirmedTran.getContractAddress ( );
                                UnconfirmedTran unconfirmedTranVerification = unconfirmedTranManager.getUnconfirmedTranMap ( ).get ( key );
                                if ( unconfirmedTranVerification != null ) {
                                    String from = unconfirmedTranVerification.getTransFrom ( );
                                    if ( unconfirmedTran.getTransTo ( ).equals ( from ) ) {
                                        key = key + "_" + Constants.EXCHANGE;
                                    } else {
                                        break L;
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
                            if ( unconfirmedTran.getTransType ( ) == 5 ) {
                                String contractAddress = unconfirmedTran.getUniqueAddress ( ).substring ( 0, unconfirmedTran.getUniqueAddress ( ).indexOf ( "," ) );
                                if ( unconfirmedTranManager.queryUnconfirmedTran ( contractAddress, 5, null, null, -1, "" ).size ( ) == 2 ) {
                                    Transaction transaction1 = new Transaction ( );
                                    transaction1.setContractAddress ( contractAddress );
                                    transaction1.setContractState ( 1 );
                                    transaction1.setTransType ( 3 );
                                    transactionRepository.updateTransactionState ( transaction1 );
                                    initializationManager.removeContract ( contractAddress );
                                }
                            }
                            if ( unconfirmedTran.getTransType ( ) == 6 ) {
                                Transaction transaction1 = new Transaction ( );
                                transaction1.setContractAddress ( unconfirmedTran.getContractAddress ( ) );
                                transaction1.setTransType ( 3 );
                                transaction1.setContractState ( 2 );
                                transactionRepository.updateTransactionState ( transaction1 );
                                initializationManager.removeCancelContract ( unconfirmedTran.getContractAddress ( ) );
                            }
                            if ( unconfirmedTran.getTransType ( ) == 7 || (unconfirmedTran.getTransType ( ) == 3 && unconfirmedTran.getContractType ( ) == 2) ) {
                                //修改投票状态
                                boolean verifyContractIsFinish = false;
                                checkVerify.setBin ( unconfirmedTran.getContractBin ( ) );
                                checkVerify.analysisAndPushHeap ( );//解析bin
                                Map<String, Object> map = new HashMap<> ( );
                                map.put ( "function", "vote" );
                                map.put ( "contractAddress", unconfirmedTran.getContractAddress ( ) );
                                map.put ( "bin", unconfirmedTran.getContractBin ( ) );//合约bin
                                try {
                                    checkVerify.analysisAndGetOpcode ( map );
                                    verifyContractIsFinish = checkVerify.getVerify ( );
                                } catch (Exception e) {
                                    e.printStackTrace ( );
                                }
                                if ( verifyContractIsFinish ) {
                                    Transaction transaction1 = new Transaction ( );
                                    transaction1.setContractAddress ( unconfirmedTran.getContractAddress ( ) );
                                    transaction1.setTransType ( 3 );
                                    transaction1.setContractState ( 1 );
                                    transactionRepository.updateTransactionState ( transaction1 );
                                }
                            }
                            Set<String> pubkeySet = new HashSet<> ( );
                            pubkeySet.add ( unconfirmedTran.getTransFrom ( ) );
                            pubkeySet.add ( unconfirmedTran.getTransTo ( ) );
                            initializationManager.saveAddressAndPubKey ( pubkeySet );
                        } catch (Exception e) {
                            logger.info ( e.getMessage ( ) );
                            break L;
                        }
                        relayMessage ( msg );
                        logger.error ( unconfirmedTranManager.getUnconfirmedTranMap ( ).size ( ) + "新流水总资产：" + initializationManager.getTokenAssets ( Constants.PTN, false ) );
                    }
                } else {
                    syncUnconfirmedTranManager.setHasNewTransaction ( true );
                }
                break;
            case NEW_TOKEN:
                if ( !syncBlockManager.isSyncBlock ( ) && !syncUnconfirmedTranManager.isSyncTransaction ( ) && !syncTokenManager.isSyncToken ( ) && !syncParticipantManager.isSyncParticipant ( ) ) {
                    Token token = MessageManager.paresTokenMessage ( msg.getToken ( ) );
                    UnconfirmedTran unconfirmedToeknTran = MessageManager.paresUnconfirmedTranMessage ( msg.getUnconfirmedTran ( ) );
                    if ( verification.verificationToken ( token ) && verification.verificationUnconfirmedTran ( unconfirmedToeknTran ) ) {
                        try {
//                            unconfirmedTranRepository.saveUnconfirmedTran(unconfirmedToeknTran);
                            unconfirmedTranManager.putUnconfirmedTran ( Hex.toHexString ( unconfirmedToeknTran.getTransSignature ( ) ), unconfirmedToeknTran );
                        } catch (Exception e) {
                            break L;
                        }
                        tokenRepository.save ( token );
                        //token cache
                        initializationManager.addTokenDecimal ( token.getName ( ), token.getDecimals ( ) );
                        relayMessage ( msg );
                        logger.info ( "新代币总资产：" + initializationManager.getTokenAssets ( Constants.PTN, false ) );
                    }
                } else {
                    syncTokenManager.setHasNewToken ( true );
                }
                break;
            case SYNC_BLOCK:
                if ( syncBlockManager.isSyncBlock ( ) && !syncTimer.startSyncBlock && msg.getBlockHeight ( ) != 0 ) {
                    String activeMac = msg.getMac ( );//获取到节点的mac地址
                    boolean isExist = nioSocketChannelManager.getChannelHostList ( ).contains ( activeMac );//判断当前节点的MAC地址纯在激活地址
                    if ( isExist ) {
                        syncBlockManager.addSyncBlockQueue ( msg.getBlockListList ( ), msg.getBlockHeight ( ), activeMac );
                    } else {
                        ctx.close ( );
                    }
                }
                syncTimer.SyncBlockTimer ( );
                break;
            case SYNC_TRANSACTION:
                if ( syncUnconfirmedTranManager.isSyncTransaction ( ) && !syncTimer.startSyncTransaction && msg.getBlockHeight ( ) != 0 ) {
                    logger.info ( "【SYNC_TRANSACTION】--- UnconfirmedTranList" + msg.getUnconfirmedTranListList ( ).size ( ) );
                    logger.info ( "【SYNC_TRANSACTION】------ UnconfirmedUnVarTranList" + msg.getUnconfirmedUnVarTranListList ( ).size ( ) );
                    syncUnconfirmedTranManager.addTransactionQueue ( msg.getUnconfirmedTranListList ( ), msg.getBlockHeight ( ) );
                    syncUnconfirmedTranManager.addUnComfirmedUnVarTranQueue ( msg.getUnconfirmedUnVarTranListList ( ) );
                }
                syncTimer.SyncTransactionTimer ( );
                break;
            case SYNC_TOKEN:
                if ( syncTokenManager.isSyncToken ( ) && !syncTimer.startSyncToken && msg.getBlockHeight ( ) != 0 ) {
                    syncTokenManager.addSyncTokenQueue ( msg.getTokenListList ( ) );
                }
                syncTimer.syncTokenTimer ( );
                break;
            case NODE_ADDRESS:
                List<String> nodeList = new ArrayList<> ( );
                msg.getNodeAddressListList ( ).forEach ( nodeList::add );
                nodeList.removeAll ( initializationManager.getNodeList ( ) );
                List<String> list = new ArrayList<> ( );
                nodeList.forEach ( node -> {
                    if ( !ping ( node ) ) {
                        list.add ( node );
                        logger.error ( "ping不通节点：" + stringToIp ( node ) );
                    }
                } );
                nodeList.removeAll ( list );
                if ( !nodeList.isEmpty ( ) ) {
                    List<NodeAddress> saveNodeList = new ArrayList<> ( );
                    initializationManager.getNodeList ( ).addAll ( nodeList );
                    nodeList.forEach ( node -> {
                        peerClient.poolsConnect ( bytesToInt ( Hex.decode ( node ) ) );
                        saveNodeList.add ( new NodeAddress ( node, 0 ) );
                    } );
                    nodeAddressRepository.save ( saveNodeList );
                }
                break;
            case PUSH_MAC:
                String mac = msg.getMac ( );
                String localhostMac = NetWorkUtil.getMACAddress ( );
                if ( mac.equals ( localhostMac ) ) {
                    ctx.close ( );
                } else {
                    nioSocketChannelManager.addNioSocketChannel ( mac, ctx );
                }
                break;
            case ADD_PARTICIPANT:
                String addParticipant = msg.getParticipant ( );
                syncParticipantManager.addParticipant ( addParticipant, 0 );
                relayMessage ( msg );
                logger.info ( "添加铸造机列表：" + FoundryUtils.getSortingMap ( syncParticipantManager.getParticipantList ( ) ) );
                break;
            case DEL_PARTICIPANT:
                String delParticipant = msg.getParticipant ( );
                syncParticipantManager.delParticipant ( delParticipant );
                foundryMachineManager.setWaitfoundryMachine ( null );
                foundryMachineManager.setWaitFoundryMachineCount ( 1 );
                //foundryMachineManager.removeFoundryMachine ( delParticipant );
                relayMessage ( msg );
                logger.info ( "移除铸造机列表：" + FoundryUtils.getSortingMap ( syncParticipantManager.getParticipantList ( ) ) );
                if ( foundryMachineManager.foundryMachineIsStart ( delParticipant ) ) {
                    InesvMessage.Message.Builder builder = MessageManager.createMessageBuilder ( RESPONSE, ADD_PARTICIPANT );
                    builder.setParticipant ( delParticipant );
                    List<String> hostList = nioSocketChannelManager.getChannelHostList ( );
                    builder.addAllNodeAddressList ( hostList );
                    nioSocketChannelManager.write ( builder.build ( ) );
                }
                break;
            case SYNC_PARTICIPANT:
                if ( syncParticipantManager.isSyncParticipant ( ) && !syncTimer.startSyncParticipant && msg.getBlockHeight ( ) != 0 ) {
                    syncParticipantManager.addParticipantQueueQueue ( msg.getParticipantListList ( ), msg.getBlockHeight ( ) );
                }
                syncTimer.syncParticipantTimer ( );
                break;
            case SET_ZERO_PARTICIPANT:
                foundryUtils.resetParticipant ( );
                if ( foundryUtils.resetParticipant ) {
                    relayMessage ( msg );
                    logger.info ( "重置铸造机列表：" + FoundryUtils.getSortingMap ( syncParticipantManager.getParticipantList ( ) ) );
                }
                foundryUtils.resetParticipant = false;
                break;
            case NEW_CONTRACT:
                String contractAddress = msg.getContractAddresss ( );
                initializationManager.setContract ( contractAddress );
                break;
            case IS_CANCEL:
                String cancelContractAddress = msg.getContractAddresss ( );
                initializationManager.setCancelContract ( cancelContractAddress );
                break;
            case CANCEL_CONTRACT:
                String address = msg.getContractAddresss ( );
                Transaction transaction = transactionRepository.findByContract ( address, 3 );
                transaction.setContractState ( 2 );
                transactionRepository.updateContranctState ( transaction );//更新合同状态
                relayMessage ( msg );
                break;
            case DeadLine_vote:
                String voteAddress = msg.getContractAddresss ( );
                Transaction voteTransaction = transactionRepository.findByContract ( voteAddress, 3 );
                voteTransaction.setContractState ( 1 );
                transactionRepository.updateContranctState ( voteTransaction );//更新合同状态
                relayMessage ( msg );
                break;
            default:
                break;
        }

    }

    public void relayMessage(InesvMessage.Message msg) {
        List<String> notifiedHostList = new ArrayList<> ( );
        List<String> ignoreList = new ArrayList<> ( );
        msg.getNodeAddressListList ( ).forEach ( host -> {
            notifiedHostList.add ( host );
            ignoreList.add ( host );
        } );
        InesvMessage.Message.Builder builder = MessageManager.createMessageBuilder ( RESPONSE, msg.getEventType ( ) );
        switch (msg.getEventType ( )) {
            case NEW_TRANSACTION:
                builder.setUnconfirmedTran ( msg.getUnconfirmedTran ( ) );
                break;
            case NEW_BLOCK:
                builder.setBlock ( msg.getBlock ( ) );
                builder.setParticipantCount ( msg.getParticipantCount ( ) );
                break;
            case NEW_TOKEN:
                builder.setUnconfirmedTran ( msg.getUnconfirmedTran ( ) );
                builder.setToken ( msg.getToken ( ) );
                break;
            case ADD_PARTICIPANT:
                builder.setParticipant ( msg.getParticipant ( ) );
                break;
            case DEL_PARTICIPANT:
                builder.setParticipant ( msg.getParticipant ( ) );
                break;
            case CANCEL_CONTRACT:
                builder.setContractAddresss ( msg.getContractAddresss ( ) );
                break;
            case SET_ZERO_PARTICIPANT:
                break;
            case DeadLine_vote:
                builder.setContractAddresss ( msg.getContractAddresss ( ) );
                break;
            default:
                break;
        }
        List<String> newHostList = nioSocketChannelManager.getChannelHostList ( );
        notifiedHostList.removeAll ( newHostList );
        newHostList.addAll ( notifiedHostList );
        builder.addAllNodeAddressList ( newHostList );
        nioSocketChannelManager.writeWithOutCtxList ( builder.build ( ), ignoreList );
    }
}
