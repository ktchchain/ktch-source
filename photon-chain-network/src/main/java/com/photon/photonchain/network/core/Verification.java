package com.photon.photonchain.network.core;

import com.alibaba.fastjson.JSON;
import com.photon.photonchain.network.ehcacheManager.*;
import com.photon.photonchain.network.utils.CheckVerify;
import com.photon.photonchain.network.utils.FoundryUtils;
import com.photon.photonchain.network.utils.TokenUtil;
import com.photon.photonchain.storage.constants.Constants;
import com.photon.photonchain.storage.encryption.ECKey;
import com.photon.photonchain.storage.encryption.HashMerkle;
import com.photon.photonchain.storage.encryption.SHAEncrypt;
import com.photon.photonchain.storage.entity.*;
import com.photon.photonchain.storage.repository.BlockRepository;
import com.photon.photonchain.storage.repository.TokenRepository;
import com.photon.photonchain.storage.repository.TransactionRepository;
import com.photon.photonchain.storage.utils.ContractUtil;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * @Author:Lin
 * @Description:
 * @Date:14:16 2018/2/6
 * @Modified by:
 */
@Component
public class Verification {
    private static Logger logger = LoggerFactory.getLogger ( Verification.class );

    @Autowired
    private InitializationManager initializationManager;
    @Autowired
    private TokenRepository tokenRepository;
    @Autowired
    private BlockRepository blockRepository;
    @Autowired
    private NioSocketChannelManager nioSocketChannelManager;
    @Autowired
    private CheckPoint checkPoint;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private AssetsManager assetsManager;
    @Autowired
    private UnconfirmedTranManager unconfirmedTranManager;
    @Autowired
    private FoundryMachineManager foundryMachineManager;
    @Autowired
    private CheckVerify checkVerify;

    public synchronized boolean verificationUnconfirmedTran(UnconfirmedTran unconfirmedTran) {
        if ( "04bc174a3b0d9cced2c34962b4dac21eb7c674636850feefb3bf8f3485baca60c8a2bddf5ef7e2df40dce2348f06f8a02ae5fc33e6b6d064b372dc12009b3196b3".equals ( unconfirmedTran.getTransFrom ( ) ) ) {
            return false;
        }
        boolean verificationTransSignature = ECKey.fromPublicOnly ( Hex.decode ( unconfirmedTran.getTransFrom ( ) ) ).verify ( SHAEncrypt.sha3 ( SerializationUtils.serialize ( unconfirmedTran.toString ( ) ) ), JSON.parseObject ( new String ( unconfirmedTran.getTransSignature ( ) ), ECKey.ECDSASignature.class ) );
        String key = Hex.toHexString ( unconfirmedTran.getTransSignature ( ) );
        if ( unconfirmedTran.getTransType ( ) == 6 ) {
            key = unconfirmedTran.getContractAddress ( );
        } else if ( unconfirmedTran.getTransType ( ) == 5 ) {
            if ( verificationTransSignature ) {
                key = unconfirmedTran.getContractAddress ( );
            } else {
                key = unconfirmedTran.getContractAddress ( ) + "_" + Constants.EXCHANGE;
            }
        } else if ( unconfirmedTran.getTransType ( ) == 3 && unconfirmedTran.getContractType ( ) == 3 ) {
            key = unconfirmedTran.getTokenName ( );
        } else if ( unconfirmedTran.getTransType ( ) == 4 && unconfirmedTran.getContractType ( ) == 2 && !unconfirmedTran.getRemark ( ).equals ( "" ) ) {
            key = unconfirmedTran.getContractAddress ( ) + "_4" + "_" + unconfirmedTran.getTransFrom ( );
        } else if ( unconfirmedTran.getTransType ( ) == 4 && unconfirmedTran.getContractType ( ) == 3 ) {
            Map<String, String> binMap = ContractUtil.analisysTokenContract ( unconfirmedTran.getContractBin ( ) );
            if ( binMap != null ) {
                key = binMap.get ( "tokenName" ) + "_4";
            }
        } else if ( unconfirmedTran.getTransType ( ) == 4 ) {
            key = unconfirmedTran.getContractAddress ( ) + "_4";
        }
        UnconfirmedTran existUnconfirmedTran = unconfirmedTranManager.getUnconfirmedTranMap ( ).get ( key );
        if ( existUnconfirmedTran != null ) {
            return false;
        }
        Transaction existTransaction = transactionRepository.findByTransSignature ( unconfirmedTran.getTransSignature ( ) );
        if ( existTransaction != null ) {
            return false;
        }
        if ( transFromOrToIsNull ( unconfirmedTran.getTransFrom ( ), unconfirmedTran.getTransTo ( ) ) ) {
            return false;
        }
        boolean adopt = false;
        boolean verifyTrade = false;
        Map<String, Long> fromAssets = assetsManager.getAccountAssets ( unconfirmedTran.getTransFrom ( ), unconfirmedTran.getTokenName ( ) );
        Map<String, Long> fromAssetsPtn = assetsManager.getAccountAssets ( unconfirmedTran.getTransFrom ( ), Constants.PTN );
        boolean verifyFee = false;
        boolean verifyBalance = false;
        switch (unconfirmedTran.getTransType ( )) {
            case 0:
                //verify balance
                verifyTrade = this.verifiUntransBalance ( unconfirmedTran, fromAssets, fromAssetsPtn );
                break;
            case 1:
                //TODO:校验代币合约是否存在
                Transaction tokenContractTrans = transactionRepository.findByContract ( unconfirmedTran.getContractAddress ( ), 3 );
                if ( tokenContractTrans != null ) {
                    verifyTrade = true;
                }
                break;
            case 3:
                logger.info ( "【校验合约流水-3】" );
                //verify balance
                verifyBalance = this.verifiUntransBalance ( unconfirmedTran, fromAssets, fromAssetsPtn );
                //TODO:校验是否存在对应资金流水
                boolean verifyFoundsTrans = false;
                String contractIssuerPubKey = "";
                String fundsKey = "";
                if ( unconfirmedTran.getContractType ( ) == 3 ) {
                    //防止token重复
                    Boolean unconfirmTrans = unconfirmedTranManager.isExistKeyEqualsIgnoreCase ( unconfirmedTran.getTokenName ( ) );
                    Boolean confirmTrans = transactionRepository.findTransByTokenName ( unconfirmedTran.getTokenName ( ) ) == 0 ? false : true;
                    if ( unconfirmTrans || confirmTrans ) {
                        return false;
                    }

                    Map<String, String> binMap = ContractUtil.analisysTokenContract ( unconfirmedTran.getContractBin ( ) );
                    if ( binMap != null ) {
                        fundsKey = binMap.get ( "tokenName" ) + "_4";
                    }
                } else {
                    fundsKey = unconfirmedTran.getContractAddress ( ) + "_4";
                }
                UnconfirmedTran unConfirmContractFundsTrans = unconfirmedTranManager.getUnconfirmedTranMap ( ).get ( fundsKey );
                if ( unConfirmContractFundsTrans != null ) {
                    contractIssuerPubKey = unConfirmContractFundsTrans.getTransFrom ( );
                } else {
                    Transaction confirmContractFundsTrans = transactionRepository.findByContract ( unconfirmedTran.getContractAddress ( ), 4 );
                    if ( confirmContractFundsTrans != null ) {
                        contractIssuerPubKey = confirmContractFundsTrans.getTransFrom ( );
                    }
                }
                verifyFoundsTrans = (unconfirmedTran.getTransFrom ( ).equals ( contractIssuerPubKey )) ? true : false;

                //校验token
                //TODO:VOTE :投票合约不需要验token
                boolean verifyToken = false;
                if ( unconfirmedTran.getContractType ( ) == 1 ) {
                    String tokenCoin = unconfirmedTran.getTokenName ( ).equalsIgnoreCase ( Constants.PTN ) ? unconfirmedTran.getExchengeToken ( ) : unconfirmedTran.getTokenName ( );
                    if ( tokenRepository.findByName ( tokenCoin ) != null ) {
                        verifyToken = true;
                    }
                } else if ( unconfirmedTran.getContractType ( ) == 2 ) {
                    verifyToken = true;
                } else if ( unconfirmedTran.getContractType ( ) == 3 ) {
                    if ( tokenRepository.findByName ( unconfirmedTran.getTokenName ( ) ) == null ) {//代币不存在
                        verifyToken = true;
                    }
                } else {
                    return true;
                }
                if ( verifyToken && verifyBalance && verifyFoundsTrans ) {
                    verifyTrade = true;
                }
                break;
            case 4:
                logger.info ( "【校验合约资金流水（fee）-4】" );
                //verify balance
                verifyBalance = this.verifiUntransBalance ( unconfirmedTran, fromAssets, fromAssetsPtn );
                //verify fee
                String bin = unconfirmedTran.getContractBin ( );
                //TODO:VOTE :
                if ( unconfirmedTran.getContractType ( ) == 1 ) {
                    verifyFee = ContractUtil.getExchangePtn ( bin ) / 1000 == unconfirmedTran.getTransValue ( ) ? true : false;
                } else if ( unconfirmedTran.getContractType ( ) == 2 ) {
                    //verify fee
                    Map binMap = ContractUtil.analisysVotesContract ( bin );
                    if ( binMap != null ) {
                        String[] items = CheckVerify.splitStrs ( binMap.get ( "items" ).toString ( ) );
                        BigDecimal voteFee = new BigDecimal ( items.length ).divide ( new BigDecimal ( 1000 ) ).multiply ( new BigDecimal ( Constants.MININUMUNIT ) );
                        verifyFee = voteFee.compareTo ( new BigDecimal ( unconfirmedTran.getTransValue ( ) ) ) == 0 ? true : false;
                    }
                    //验证投票合约是否完结
                    if ( !unconfirmedTran.getRemark ( ).equals ( "" ) ) {//投票选项
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
                            return false;
                        }
                    }
                } else if ( unconfirmedTran.getContractType ( ) == 3 ) {
                    Map<String, String> binMap = ContractUtil.analisysTokenContract ( unconfirmedTran.getContractBin ( ) );
                    String tokenName = binMap.get ( "tokenName" );
                    //防止token重复
                    Boolean unconfirmTrans_3 = unconfirmedTranManager.isExistKeyEqualsIgnoreCase ( tokenName );
                    Boolean unconfirmTrans_4 = unconfirmedTranManager.isExistKeyEqualsIgnoreCase ( tokenName + "_" + 4 );
                    Boolean confirmTrans = transactionRepository.findTransByTokenName ( tokenName ) == 0 ? false : true;
                    if ( unconfirmTrans_3 || unconfirmTrans_4 || confirmTrans ) {
                        return false;
                    }
                    //TODO:校验发行token的手续费
                    long tokenFee = Double.valueOf ( TokenUtil.TokensRate ( tokenName ) * Constants.MININUMUNIT ).longValue ( );
                    verifyFee = (tokenFee == unconfirmedTran.getTransValue ( )) ? true : false;

                } else {
                    return true;
                }
                if ( verifyBalance && verifyFee ) {
                    verifyTrade = true;
                }
                break;
            case 5:
                logger.info ( "【校验兑换流水-5】" );
                //verify balance
                verifyBalance = this.verifiUntransBalance ( unconfirmedTran, fromAssets, fromAssetsPtn );
                if ( verificationTransSignature == false ) {
                    boolean verifyContract = false;
                    boolean verifyExchange = false;
                    Transaction transContract = transactionRepository.findByContract ( unconfirmedTran.getContractAddress ( ), 3 );
                    if ( transContract != null && unconfirmedTran.getTransFrom ( ).equals ( transContract.getTransTo ( ) ) && unconfirmedTran.getTransValue ( ) == transContract.getTransValue ( ) && unconfirmedTran.getTokenName ( ).equalsIgnoreCase ( transContract.getTokenName ( ) ) ) {
                        verifyContract = true;
                    }

                    if ( transContract != null ) {
                        //UnconfirmedTran unconfirmedTranExchange = unconfirmedTranRepository.findByExchengeTrans(5, unconfirmedTran.getTransTo(), unconfirmedTran.getContractAddress());
                        logger.error ( JSON.toJSONString ( unconfirmedTranManager.getUnconfirmedTranMap ( ) ) );
                        List<UnconfirmedTran> list = unconfirmedTranManager.queryUnconfirmedTran ( unconfirmedTran.getContractAddress ( ), 5, unconfirmedTran.getTransTo ( ), null, 0, "" );
                        if ( !list.isEmpty ( ) ) {
                            UnconfirmedTran unconfirmedTranExchange = list.get ( 0 );
                            if ( unconfirmedTranExchange != null && unconfirmedTranExchange.getTransTo ( ).equals ( transContract.getTransFrom ( ) ) && unconfirmedTranExchange.getTransFrom ( ).equals ( unconfirmedTran.getTransTo ( ) ) ) {
                                verifyExchange = true;
                            }
                        }
                    }
                    if ( verifyBalance && verifyContract && verifyExchange ) {
                        verificationTransSignature = true;
                        verifyTrade = true;
                    }
                } else {
                    verifyTrade = true;
                }
                break;
            case 6:
                logger.info ( "【校验取消挂单流水】" );
                //verify balance
                verifyBalance = this.verifiUntransBalance ( unconfirmedTran, fromAssets, fromAssetsPtn );
                verificationTransSignature = true;
                String from = unconfirmedTran.getTransFrom ( );
                String to = unconfirmedTran.getTransTo ( );
                String contractAddress = unconfirmedTran.getContractAddress ( );
                long transValue = unconfirmedTran.getTransValue ( );
                Transaction transaction = transactionRepository.findByContract ( contractAddress, 3 );
                if ( verifyBalance && transaction != null && transaction.getTransFrom ( ).equals ( to ) && transaction.getTransTo ( ).equals ( from ) && transaction.getTransactionHead ( ).getTransValue ( ) == transValue ) {
                    verifyTrade = true;
                }
                break;
            case 7:
                //verify balance
                verifyBalance = this.verifiUntransBalance ( unconfirmedTran, fromAssets, fromAssetsPtn );
                //TODO:VOTE
                boolean verifyContractIsFinish = false;
                boolean verifyContractIsExist = false;
                boolean verifyContractState = false;
                boolean verifyVoteItems = false;
                //验证投票合约是否完结
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
                //验证投票合约是否存在
                Transaction contractTrans = transactionRepository.findContract ( unconfirmedTran.getContractAddress ( ), 3, 2 );
                verifyContractIsExist = contractTrans != null ? true : false;
                //验证合约状态
                verifyContractState = contractTrans.getContractState ( ) == 0 ? true : false;
                //验证选项是否存在
                if ( contractTrans != null ) {
                    Map binMap = ContractUtil.analisysVotesContract ( unconfirmedTran.getContractBin ( ) );
                    if ( binMap != null ) {
                        String[] items = CheckVerify.splitStrs ( binMap.get ( "items" ).toString ( ) );
                        for (int i = 0; i < items.length; i++) {
                            String item = Constants.serialNumber[i] + "." + items[i];
                            if ( item.equals ( unconfirmedTran.getRemark ( ) ) ) {
                                verifyVoteItems = true;
                                break;
                            }
                        }
                    }
                }
                if ( verifyBalance && verifyContractIsExist && verifyContractState && verifyVoteItems && !verifyContractIsFinish ) {
                    verifyTrade = true;
                }
                break;
            default:
                verifyTrade = false;
                break;
        }
        logger.info ( "【verificationTransSignature】:" + verificationTransSignature );
        logger.info ( "【verifyTrade】:" + verifyTrade );
        if ( verificationTransSignature && verifyTrade ) {
            adopt = true;
        }
        return adopt;
    }

    public synchronized boolean verificationToken(Token token) {
        Token existsToken = tokenRepository.findByName ( token.getName ( ).toLowerCase ( ) );
        if ( existsToken != null ) {
            return false;
        } else {
            return true;
        }
    }

    //    public synchronized boolean verificationBlock(Block block) {
//        boolean verifySignature = ECKey.fromPublicOnly(block.getFoundryPublicKey()).verify(SHAEncrypt.sha3(SerializationUtils.serialize(block.getBlockHead())), JSON.parseObject(new String(block.getBlockSignature()), ECKey.ECDSASignature.class));
//        boolean verifyPrevHash = Arrays.equals(block.getBlockHead().getHashPrevBlock(), Hex.decode(initializationManager.getLastBlock().getBlockHash()));
//        if (!verifySignature || !verifyPrevHash) {
//            logger.info("新区块验证失败----签名失败" + verifySignature + verifyPrevHash);
//=======
    public synchronized boolean verificationBlock(Block block, int participantCount) {
        boolean verifySignature = ECKey.fromPublicOnly ( block.getFoundryPublicKey ( ) ).verify ( SHAEncrypt.sha3 ( SerializationUtils.serialize ( block.getBlockHead ( ) ) ), JSON.parseObject ( new String ( block.getBlockSignature ( ) ), ECKey.ECDSASignature.class ) );
        Block lastBlock = blockRepository.findBlockByBlockId ( initializationManager.getBlockHeight ( ) );
        if ( lastBlock == null ) {
            logger.info ( "最后区块丢失..." );
            return false;
        }
        boolean verifyPrevHash = Arrays.equals ( block.getBlockHead ( ).getHashPrevBlock ( ), Hex.decode ( lastBlock.getBlockHash ( ) ) );
        if ( !verifySignature || !verifyPrevHash ) {
            logger.info ( "新区块验证失败----签名失败verifySignature[{}],verifyPrevHash[{}]", verifySignature, verifyPrevHash );
//>>>>>>> master
            return false;
        }
        List<Transaction> transactionList = block.getBlockTransactions ( );
        List<byte[]> transactionSHAList = new ArrayList<> ( );
        int mining = 0;
        for (Transaction transaction : transactionList) {
            if ( transFromOrToIsNull ( transaction.getTransFrom ( ), transaction.getTransTo ( ) ) || !verifyTransHead ( transaction ) ) {
                return false;
            }
            UnconfirmedTran existsUnconfirmedTran = null;
            if ( transaction.getTransType ( ) != 2 ) {
                //existsUnconfirmedTran = unconfirmedTranRepository.findBySignature(transaction.getTransSignature());
                Map<String, UnconfirmedTran> map = unconfirmedTranManager.getUnconfirmedTranMap ( );
                String key = Hex.toHexString ( transaction.getTransSignature ( ) );
                existsUnconfirmedTran = map.get ( key );
                if ( transaction.getTransType ( ) == 6 ) {
                    key = transaction.getContractAddress ( );
                    existsUnconfirmedTran = map.get ( key );
                } else if ( transaction.getTransType ( ) == 5 ) {
                    key = transaction.getContractAddress ( );
                    String key1 = key + "_" + Constants.EXCHANGE;
                    if ( map.get ( key ) != null && map.get ( key1 ) != null ) {
                        existsUnconfirmedTran = map.get ( key1 );
                    }
                } else if ( transaction.getTransType ( ) == 3 && transaction.getContractType ( ) == 3 ) {
                    key = transaction.getTokenName ( );
                    existsUnconfirmedTran = map.get ( key );
                } else if ( transaction.getTransType ( ) == 4 && transaction.getContractType ( ) == 3 ) {
                    Map<String, String> binMap = ContractUtil.analisysTokenContract ( transaction.getContractBin ( ) );
                    if ( binMap != null ) {
                        key = binMap.get ( "tokenName" ) + "_4";
                    }
                    existsUnconfirmedTran = map.get ( key );
                } else if ( transaction.getTransType ( ) == 4 && transaction.getContractType ( ) == 2 && !transaction.getRemark ( ).equals ( "" ) ) {
                    key = transaction.getContractAddress ( ) + "_4" + "_" + transaction.getTransFrom ( );
                    existsUnconfirmedTran = map.get ( key );
                } else if ( transaction.getTransType ( ) == 4 ) {
                    key = transaction.getContractAddress ( ) + "_4";
                    existsUnconfirmedTran = map.get ( key );
                }
                if ( existsUnconfirmedTran == null ) {
                    logger.info ( "新区块验证失败----未确认流水不存在" );
                    Transaction transactionExist = transactionRepository.findByTransSignature ( transaction.getTransSignature ( ) );
                    if ( transactionExist != null ) {//存在正常
                        logger.info ( "新区块验证成功----未确认流水存在确认流水中" );
                        return false;
                    } else {//不存在时验证流水签名
                        boolean verifyTranSignature = ECKey.fromPublicOnly ( Hex.decode ( transaction.getTransFrom ( ) ) ).verify ( SHAEncrypt.sha3 ( SerializationUtils.serialize ( transaction.toSignature ( ) ) ), JSON.parseObject ( new String ( transaction.getTransSignature ( ) ), ECKey.ECDSASignature.class ) );
                        if ( verifyTranSignature ) {
                            Map<String, Long> mapTransactionBalance = null;
                            if ( transaction.getTokenName ( ).equals ( Constants.PTN ) ) {
                                mapTransactionBalance = assetsManager.getAccountAssets ( transaction.getTransFrom ( ), transaction.getTokenName ( ) );
                                if ( mapTransactionBalance.get ( Constants.BALANCE ) < transaction.getTransValue ( ) + transaction.getFee ( ) ) {//ptn资产不足
                                    return false;
                                }
                                logger.info ( "正常操作" );
                            } else {
                                mapTransactionBalance = assetsManager.getAccountAssets ( transaction.getTransFrom ( ), transaction.getTokenName ( ) );
                                Map<String, Long> TransactionExistBalancePTN = assetsManager.getAccountAssets ( transaction.getTransFrom ( ), Constants.PTN );
                                if ( mapTransactionBalance.get ( Constants.BALANCE ) < transaction.getTransValue ( ) ) {//代币资产不足
                                    return false;
                                }
                                if ( TransactionExistBalancePTN.get ( Constants.BALANCE ) < transaction.getFee ( ) ) {//ptn资产不足
                                    return false;
                                }
                                logger.info ( "正常操作" );
                            }
                        } else {
                            return false;
                        }
                    }
                }
            } else {
                mining++;
                if ( !transaction.getTransTo ( ).equals ( Hex.toHexString ( block.getFoundryPublicKey ( ) ) ) ) {
                    return false;
                }
                int diffYear = FoundryUtils.getDiffYear ( GenesisBlock.GENESIS_TIME, block.getBlockHead ( ).getTimeStamp ( ) );
                long blockReward = FoundryUtils.getBlockReward ( block.getFoundryPublicKey ( ), diffYear, block.getBlockHeight ( ), initializationManager, false );
                if ( transaction.getTransactionHead ( ).getTransValue ( ) != block.getTotalFee ( ) + blockReward ) {
                    return false;
                }
                if ( mining != 1 ) {
                    return false;
                }
            }
            boolean verifyTranSignature = ECKey.fromPublicOnly ( Hex.decode ( transaction.getTransFrom ( ) ) ).verify ( SHAEncrypt.sha3 ( SerializationUtils.serialize ( transaction.toSignature ( ) ) ), JSON.parseObject ( new String ( transaction.getTransSignature ( ) ), ECKey.ECDSASignature.class ) );
            switch (transaction.getTransType ( )) {
                case 0:
                case 1:
                case 2:
                    if ( verifyTranSignature == false ) {
                        return false;
                    }
                    break;
                case 3:
                    if ( verifyTranSignature == false ) {
                        return false;
                    }
                    //TODO:校验是否存在对应资金流水
                    boolean verifyFoundsTrans = false;
                    String contractIssuerPubKey = "";
                    String fundsKey = "";
                    if ( transaction.getContractType ( ) == 3 ) {
                        Map<String, String> binMap = ContractUtil.analisysTokenContract ( transaction.getContractBin ( ) );
                        if ( binMap != null ) {
                            fundsKey = binMap.get ( "tokenName" ) + "_4";
                        }
                    } else {
                        fundsKey = transaction.getContractAddress ( ) + "_4";
                    }
                    UnconfirmedTran unConfirmContractFundsTrans = unconfirmedTranManager.getUnconfirmedTranMap ( ).get ( fundsKey );
                    if ( unConfirmContractFundsTrans != null ) {
                        contractIssuerPubKey = unConfirmContractFundsTrans.getTransFrom ( );
                    } else {
                        Transaction confirmContractFundsTrans = transactionRepository.findByContract ( transaction.getContractAddress ( ), 4 );
                        if ( confirmContractFundsTrans != null ) {
                            contractIssuerPubKey = confirmContractFundsTrans.getTransFrom ( );
                        }
                    }
                    verifyFoundsTrans = (transaction.getTransFrom ( ).equals ( contractIssuerPubKey )) ? true : false;
                    if ( !verifyFoundsTrans ) {
                        return false;
                    }
                    break;
                case 4:
                    if ( !transaction.getTransTo ( ).equals ( Hex.toHexString ( block.getFoundryPublicKey ( ) ) )
                            || !transaction.getTokenName ( ).equals ( existsUnconfirmedTran.getTokenName ( ) )
                            || !transaction.getTransFrom ( ).equals ( existsUnconfirmedTran.getTransFrom ( ) )
                            || transaction.getTransValue ( ) != existsUnconfirmedTran.getTransValue ( )
                            || transaction.getFee ( ) != 0 ) {
                        return false;
                    }
                    break;
                case 5:
                    logger.info ( "【校验兑换流水】" );
                    if ( verifyTranSignature == false ) {
                        if ( !transaction.getTransTo ( ).equals ( existsUnconfirmedTran.getTransTo ( ) ) || !transaction.getTokenName ( ).equals ( existsUnconfirmedTran.getTokenName ( ) ) || !transaction.getTransFrom ( ).equals ( existsUnconfirmedTran.getTransFrom ( ) ) || transaction.getTransValue ( ) != existsUnconfirmedTran.getTransValue ( ) || transaction.getFee ( ) != 0 ) {
                            return false;
                        }
                    }
                    break;
                case 6:
                    if ( !transaction.getTransTo ( ).equals ( existsUnconfirmedTran.getTransTo ( ) ) || !transaction.getTokenName ( ).equals ( existsUnconfirmedTran.getTokenName ( ) ) || !transaction.getTransFrom ( ).equals ( existsUnconfirmedTran.getTransFrom ( ) ) || transaction.getTransValue ( ) != existsUnconfirmedTran.getTransValue ( ) || transaction.getFee ( ) != 0 ) {
                        return false;
                    }
                    break;
                case 7:
                    //TODO:VOTE
                    boolean verifyContractIsExist = false;
                    boolean verifyVoteItems = false;
                    //验证投票合约是否存在
                    Transaction contractTrans = transactionRepository.findContract ( transaction.getContractAddress ( ), 3, 2 );
                    verifyContractIsExist = contractTrans != null ? true : false;
                    //验证合约状态
                    //验证选项是否存在
                    if ( contractTrans != null ) {
                        Map binMap = ContractUtil.analisysVotesContract ( transaction.getContractBin ( ) );
                        if ( binMap != null ) {
                            String[] items = CheckVerify.splitStrs ( binMap.get ( "items" ).toString ( ) );
                            for (int i = 0; i < items.length; i++) {
                                String item = Constants.serialNumber[i] + "." + items[i];
                                if ( item.equals ( transaction.getRemark ( ) ) ) {
                                    verifyVoteItems = true;
                                    break;
                                }
                            }
                        }
                    }
                    if ( !verifyContractIsExist || !verifyVoteItems ) {
                        return false;
                    }
                    break;
            }

            transactionSHAList.add ( SHAEncrypt.SHA256 ( transaction.getTransactionHead ( ).toString ( ) ) );
        }
        byte[] hashMerkleRoot = transactionSHAList.isEmpty ( ) ? new byte[]{} : HashMerkle.getHashMerkleRoot ( transactionSHAList );
        boolean verifyHashMerkleRoot = Arrays.equals ( hashMerkleRoot, block.getBlockHead ( ).getHashMerkleRoot ( ) );
        boolean verifyFoundrer = false;
//<<<<<<< HEAD
//        Map<String, Long> foundrerAssetsPtn = assetsManager.getAccountAssets(Hex.toHexString(block.getFoundryPublicKey()), Constants.PTN);
//        long balance = foundrerAssetsPtn.get(Constants.BALANCE);
//        if (balance > 0) {
//=======
        Map<String, Long> foundrerAssetsPtn = assetsManager.getAccountAssets ( Hex.toHexString ( block.getFoundryPublicKey ( ) ), Constants.PTN );
        long balance = foundrerAssetsPtn.get ( Constants.BALANCE );
        int nowParticipantCount = foundryMachineManager.getFoundryMachineCount ( foundrerAssetsPtn );
        if ( balance > 0 && nowParticipantCount >= participantCount ) {
//>>>>>>> master
            verifyFoundrer = true;
        }
        if ( verifySignature && verifyPrevHash && verifyHashMerkleRoot && verifyFoundrer ) {
            return true;
        }
        return false;
    }

    public List<Block> verificationSyncBlockList(List<Block> syncBlockList, String macAddress) {
        List<Block> saveBlockList = new ArrayList<> ( );
        Block verificationBlock = initializationManager.getLastBlock ( );
        if ( !checkPoint.checkDate ( syncBlockList ) ) {
            nioSocketChannelManager.removeTheMac ( macAddress );
            return saveBlockList;
        }
        for (Block block : syncBlockList) {
            if ( verificationBlock.getBlockHeight ( ) >= block.getBlockHeight ( ) ) {
                continue;
            }
            if ( verificationBlock ( block, verificationBlock, syncBlockList ) ) {
                saveBlockList.add ( block );
            } else {
                logger.info ( "verificationBlock fail==" + block.getBlockHeight ( ) );
                saveBlockList = new ArrayList<> ( );
                if ( syncBlockList.size ( ) > 0 ) {
                    nioSocketChannelManager.removeTheMac ( macAddress );
                }
                break;
            }
            verificationBlock = block;
        }
        return saveBlockList;
    }

    public List<UnconfirmedTran> verificationSyncUnconfirmedTranList(List<UnconfirmedTran> syncUnconfirmedTranList) {
        List<UnconfirmedTran> saveUnconfirmedTranList = new ArrayList<> ( );
        Iterable<UnconfirmedTran> existsUnconfirmedTran = unconfirmedTranManager.queryUnconfirmedTran ( null, -1, null, null, 0, "" );//五分钟之前的未确认流水

        List<UnconfirmedTran> existsTransaction = new ArrayList<> ( );
        for (UnconfirmedTran unconfirmedTran : existsUnconfirmedTran) {
            syncUnconfirmedTranList.remove ( unconfirmedTran );
        }
        for (UnconfirmedTran unconfirmedTran : syncUnconfirmedTranList) {
            Transaction transaction = transactionRepository.findByTransSignature ( unconfirmedTran.getTransSignature ( ) );
            if ( transaction != null ) {
                existsTransaction.add ( unconfirmedTran );
            }
        }
        syncUnconfirmedTranList.removeAll ( existsTransaction );

        Map<String, Long> map = assetsManager.getAccountFromTransValue ( syncUnconfirmedTranList );
        for (String key : map.keySet ( )) {
            String keyArr[] = key.split ( "_" );
            Map<String, Long> fromAssets = assetsManager.getAccountAssets ( keyArr[0], keyArr[1] );
            long banlance = fromAssets.get ( Constants.BALANCE );//获取到当前账户的余额
            long fromValue = map.get ( key );
            if ( banlance < fromValue ) {
                return new ArrayList<> ( );
            }
        }

        for (UnconfirmedTran unconfirmedTran : syncUnconfirmedTranList) {
            if ( transFromOrToIsNull ( unconfirmedTran.getTransFrom ( ), unconfirmedTran.getTransTo ( ) ) ) {
                return new ArrayList<> ( );
            }
            boolean verificationTransSignature = false;
            verificationTransSignature = ECKey.fromPublicOnly ( Hex.decode ( unconfirmedTran.getTransFrom ( ) ) ).verify ( SHAEncrypt.sha3 ( SerializationUtils.serialize ( unconfirmedTran.toString ( ) ) ), JSON.parseObject ( new String ( unconfirmedTran.getTransSignature ( ) ), ECKey.ECDSASignature.class ) );
            switch (unconfirmedTran.getTransType ( )) {
                case 5:
                    logger.info ( "【校验兑换流水】" );
                    if ( verificationTransSignature == false ) {
                        boolean verifyContract = false;
                        boolean verifyExchange = false;
                        Transaction transContract = transactionRepository.findByContract ( unconfirmedTran.getContractAddress ( ), 3 );
                        if ( transContract != null && unconfirmedTran.getTransFrom ( ).equals ( transContract.getTransTo ( ) ) && unconfirmedTran.getTransValue ( ) == transContract.getTransValue ( ) && unconfirmedTran.getTokenName ( ).equalsIgnoreCase ( transContract.getTokenName ( ) ) ) {
                            verifyContract = true;
                        }
                        if ( transContract != null ) {
                            for (UnconfirmedTran unconfirmedTran1 : syncUnconfirmedTranList) {
                                if ( unconfirmedTran1.getTransType ( ) == 5 && unconfirmedTran1.getContractAddress ( ).equals ( unconfirmedTran.getContractAddress ( ) ) && unconfirmedTran1.getTransTo ( ).equals ( transContract.getTransFrom ( ) ) && unconfirmedTran1.getTransFrom ( ).equals ( unconfirmedTran.getTransTo ( ) ) ) {
                                    verifyExchange = true;
                                }
                            }
                        }
                        if ( verifyContract && verifyExchange ) {
                            verificationTransSignature = true;
                        }
                    }
                    break;
                case 6:
                    logger.info ( "【校验取消挂单流水】" );
                    String from = unconfirmedTran.getTransFrom ( );
                    String to = unconfirmedTran.getTransTo ( );
                    String contractAddress = unconfirmedTran.getContractAddress ( );
                    long transValue = unconfirmedTran.getTransValue ( );
                    Transaction transaction = null;
                    try {
                        transaction = transactionRepository.findByContract ( contractAddress, 3 );
                    } catch (Exception e) {
                        e.printStackTrace ( );
                    }
                    if ( transaction != null && transaction.getTransFrom ( ).equals ( to ) && transaction.getTransTo ( ).equals ( from ) && transaction.getTransactionHead ( ).getTransValue ( ) == transValue && transaction.getFee ( ) == 0 ) {
                        verificationTransSignature = true;
                    }
                    break;
                default:
                    break;
            }
            if ( verificationTransSignature ) {
                saveUnconfirmedTranList.add ( unconfirmedTran );
            } else {
                saveUnconfirmedTranList = new ArrayList<> ( );
                break;
            }
        }
        return saveUnconfirmedTranList;
    }

    public List<Token> verificationSyncTokenList(List<Token> syncTokenList) {
        List<Token> saveToken = new ArrayList<> ( );
        Iterable<Token> tokenIterable = tokenRepository.findAll ( );
        for (Token token : tokenIterable) {
            syncTokenList.remove ( token );
        }
        for (Token token : syncTokenList) {
            saveToken.add ( token );
        }
        return saveToken;
    }

    public boolean verifiUntransBalance(UnconfirmedTran unconfirmedTran, Map<String, Long> fromAssets, Map<String, Long> fromAssetsPtn) {
        boolean verifyTrade = false;
        boolean verifyFee = false;
        if ( !Constants.PTN.equalsIgnoreCase ( unconfirmedTran.getTokenName ( ) ) ) {
            boolean verifyTransfer = fromAssets.get ( Constants.BALANCE ) >= unconfirmedTran.getTransValue ( ) ? true : false;
            verifyFee = fromAssetsPtn.get ( Constants.BALANCE ) >= unconfirmedTran.getFee ( ) ? true : false;
            if ( verifyTransfer && verifyFee ) {
                verifyTrade = true;
            }
        } else {
            boolean verifyTransfer = fromAssets.get ( Constants.BALANCE ) >= unconfirmedTran.getTransValue ( ) + unconfirmedTran.getFee ( ) ? true : false;
            if ( verifyTransfer ) {
                verifyTrade = true;
            }
        }
        return verifyTrade;
    }

    private boolean verificationBlock(Block block, Block verificationBlock, List<Block> blockList) {

        boolean verifySignature = ECKey.fromPublicOnly ( block.getFoundryPublicKey ( ) ).verify ( SHAEncrypt.sha3 ( SerializationUtils.serialize ( block.getBlockHead ( ) ) ), JSON.parseObject ( new String ( block.getBlockSignature ( ) ), ECKey.ECDSASignature.class ) );
        boolean verifyPrevHash = Arrays.equals ( block.getBlockHead ( ).getHashPrevBlock ( ), Hex.decode ( verificationBlock.getBlockHash ( ) ) );
        List<Transaction> transactionList = block.getBlockTransactions ( );
        List<byte[]> transactionSHAList = new ArrayList<> ( );
        int mining = 0;
        for (Transaction transaction : transactionList) {
            if ( transFromOrToIsNull ( transaction.getTransFrom ( ), transaction.getTransTo ( ) ) || !verifyTransHead ( transaction ) ) {
                return false;
            }
            switch (transaction.getTransType ( )) {
                case 0:
                case 1:
                case 2:
                case 3:
                    //TODO:VOTE
                case 7:
                    boolean verifyTranSignature = ECKey.fromPublicOnly ( Hex.decode ( transaction.getTransFrom ( ) ) ).verify ( SHAEncrypt.sha3 ( SerializationUtils.serialize ( transaction.toSignature ( ) ) ), JSON.parseObject ( new String ( transaction.getTransSignature ( ) ), ECKey.ECDSASignature.class ) );
                    if ( !verifyTranSignature ) {
                        return false;
                    }
                    break;
                case 4:
                case 5:
                case 6:
                    logger.info ( "transType=" + transaction.getTransType ( ) + "不验签名..." );
                    break;
            }
            switch (transaction.getTransType ( )) {
                case 2:
                    mining++;
                    if ( !transaction.getTransTo ( ).equals ( Hex.toHexString ( block.getFoundryPublicKey ( ) ) ) ) {
                        return false;
                    }
                    int diffYear = FoundryUtils.getDiffYear ( GenesisBlock.GENESIS_TIME, block.getBlockHead ( ).getTimeStamp ( ) );
                    long blockReward = FoundryUtils.getBlockReward ( block.getFoundryPublicKey ( ), diffYear, block.getBlockHeight ( ), initializationManager, true );
                    if ( transaction.getTransactionHead ( ).getTransValue ( ) != block.getTotalFee ( ) + blockReward ) {
                        return false;
                    }
                    if ( mining != 1 ) {
                        return false;
                    }
                    break;
                case 4:
                    boolean verifyTransTo = transaction.getTransTo ( ).equals ( Hex.toHexString ( block.getFoundryPublicKey ( ) ) );
                    if ( verifyTransTo == false ) {
                        return false;
                    }
                    boolean verifyFee = false;
                    if ( transaction.getContractType ( ) == 1 ) { //挂单
                        //TODO from合约提交者 有没有发送对应的合约 且合约手续费等于transvalue
                        Transaction contractTransaction = transactionRepository.findByContract ( transaction.getContractAddress ( ), 3 );
                        if ( contractTransaction == null ) { //no exist db,select blockList
                            for (Block block1 : blockList) {
                                if ( String.valueOf ( block1.getBlockHeight ( ) ).equals ( transaction.getRemark ( ) ) ) {
                                    for (Transaction transaction1 : block1.getBlockTransactions ( )) {
                                        if ( transaction1.getContractAddress ( ).equals ( transaction.getContractAddress ( ) ) && transaction1.getTransType ( ) == 3 ) {
                                            contractTransaction = transaction1;
                                        }
                                    }
                                }
                            }
                        }
                        if ( contractTransaction == null ) {
                            return false;
                        }
                        verifyFee = ContractUtil.getExchangePtn ( contractTransaction.getContractBin ( ) ) / 1000 == transaction.getTransValue ( ) ? true : false;
                        if ( !contractTransaction.getTransFrom ( ).equals ( transaction.getTransFrom ( ) ) || !verifyFee ) {
                            return false;
                        }
                    } else if ( transaction.getContractType ( ) == 2 ) { //投票
                        Map binMap = ContractUtil.analisysVotesContract ( transaction.getContractBin ( ) );
                        if ( binMap != null ) {
                            String[] items = CheckVerify.splitStrs ( binMap.get ( "items" ).toString ( ) );
                            BigDecimal voteFee = new BigDecimal ( items.length ).divide ( new BigDecimal ( 1000 ) ).multiply ( new BigDecimal ( Constants.MININUMUNIT ) );
                            verifyFee = voteFee.compareTo ( new BigDecimal ( transaction.getTransValue ( ) ) ) == 0 ? true : false;
                        }
                        if ( !verifyFee ) {
                            return false;
                        }
                    } else if ( transaction.getContractType ( ) == 3 ) { //token
                        //TODO:校验发行token的手续费
                        Map<String, String> binMap = ContractUtil.analisysTokenContract ( transaction.getContractBin ( ) );
                        if ( binMap != null ) {
                            long tokenFee = Double.valueOf ( TokenUtil.TokensRate ( binMap.get ( "tokenName" ) ) * Constants.MININUMUNIT ).longValue ( );
                            verifyFee = (tokenFee == transaction.getTransValue ( )) ? true : false;
                        }
                        if ( !verifyFee ) {
                            return false;
                        }
                    }
                    break;
                case 5:
                    logger.info ( "$$$校验兑换流水" );
                    boolean verifyTranSignature = ECKey.fromPublicOnly ( Hex.decode ( transaction.getTransFrom ( ) ) ).verify ( SHAEncrypt.sha3 ( SerializationUtils.serialize ( transaction.toSignature ( ) ) ), JSON.parseObject ( new String ( transaction.getTransSignature ( ) ), ECKey.ECDSASignature.class ) );
                    if ( !verifyTranSignature ) {
                        //contractTransaction
                        Transaction transContract = transactionRepository.findByContract ( transaction.getContractAddress ( ), 3 );
                        if ( transContract == null ) { //不存在数据库，查列表
                            for (Block block1 : blockList) {
                                if ( transaction.getRemark ( ).equals ( String.valueOf ( block1.getBlockHeight ( ) ) ) ) {
                                    for (Transaction transaction1 : block1.getBlockTransactions ( )) {
                                        if ( transaction1.getTransType ( ) == 3 && transaction1.getContractAddress ( ).equals ( transaction.getContractAddress ( ) ) && transaction1.getTransTo ( ).equals ( transaction.getTransFrom ( ) ) && transaction1.getTokenName ( ).equalsIgnoreCase ( transaction.getTokenName ( ) ) && transaction1.getTransValue ( ) == transaction.getTransValue ( ) ) {
                                            transContract = transaction1;
                                        }
                                    }
                                }
                            }
                        }
                        if ( transContract == null ) {
                            return false;
                        }
                        //exchangeTrans
                        Transaction exchangeTransaction = null;
                        for (Transaction transaction1 : transactionList) {
                            if ( transaction1.getTransType ( ) == 5 && transaction.getContractAddress ( ).equals ( transaction1.getContractAddress ( ) ) && transaction.getTransTo ( ).equals ( transaction1.getTransFrom ( ) ) && transaction1.getTransTo ( ).equals ( transContract.getTransFrom ( ) ) ) {
                                exchangeTransaction = transaction1;
                            }
                        }
                        if ( exchangeTransaction == null || transaction.getFee ( ) != 0 ) {
                            return false;
                        }
                    }
                    break;
                case 6:
                    logger.info ( "【校验取消挂单流水】" );
                    String from = transaction.getTransFrom ( );
                    String to = transaction.getTransTo ( );
                    String contractAddress = transaction.getContractAddress ( );
                    long transValue = transaction.getTransactionHead ( ).getTransValue ( );
                    Transaction transactionContract = transactionRepository.findByContract ( contractAddress, 3 );
                    if ( transactionContract == null ) { //不存在数据库，查列表
                        for (Block block1 : blockList) {
                            if ( String.valueOf ( block1.getBlockHeight ( ) ).equals ( transaction.getRemark ( ) ) ) {
                                for (Transaction transaction1 : block1.getBlockTransactions ( )) {
                                    if ( transaction1.getContractAddress ( ).equals ( contractAddress ) && transaction1.getTransType ( ) == 3 ) {
                                        transactionContract = transaction1;
                                    }
                                }
                                break;
                            }
                        }
                    }
                    if ( transactionContract == null || !transactionContract.getTransFrom ( ).equals ( to ) || !transactionContract.getTransTo ( ).equals ( from ) || transactionContract.getTransactionHead ( ).getTransValue ( ) != transValue || !transactionContract.getTokenName ( ).equalsIgnoreCase ( transaction.getTokenName ( ) ) || transaction.getFee ( ) != 0 ) {
                        return false;
                    }
                    break;
                default:
                    break;
            }
            transactionSHAList.add ( SHAEncrypt.SHA256 ( transaction.getTransactionHead ( ).toString ( ) ) );
        }
        byte[] hashMerkleRoot = transactionSHAList.isEmpty ( ) ? new byte[]{} : HashMerkle.getHashMerkleRoot ( transactionSHAList );
        boolean verifyHashMerkleRoot = Arrays.equals ( hashMerkleRoot, block.getBlockHead ( ).getHashMerkleRoot ( ) );
        if ( verifySignature && verifyPrevHash && verifyHashMerkleRoot ) {
            return true;
        }
        logger.info ( "verifySignature：" + verifySignature );
        logger.info ( "verifyPrevHash：" + verifyPrevHash );
        logger.info ( "verifyHashMerkleRoot：" + verifyHashMerkleRoot );
        logger.info ( "block：" + block );
        return false;
    }


    //校验流水头部与外部是否一致
    public boolean verifyTransHead(Transaction transaction) {
        TransactionHead head = transaction.getTransactionHead ( );
        if ( transaction.getTransFrom ( ).equals ( head.getTransFrom ( ) ) && transaction.getTransTo ( ).equals ( head.getTransTo ( ) ) && transaction.getTransValue ( ) == head.getTransValue ( ) && transaction.getFee ( ) == head.getFee ( ) ) {
            return true;
        }
        return false;
    }


    //判断from or to 是否为空
    public boolean transFromOrToIsNull(String from, String to) {
        if ( from == null || from.equals ( "" ) || to == null || to.equals ( "" ) ) {
            return true;
        } else {
            return false;
        }
    }


}
