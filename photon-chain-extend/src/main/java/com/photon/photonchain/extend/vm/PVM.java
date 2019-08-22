package com.photon.photonchain.extend.vm;

import com.alibaba.fastjson.JSON;
import com.photon.photonchain.extend.Universal.OpCodeEnum;
import com.photon.photonchain.network.ehcacheManager.AssetsManager;
import com.photon.photonchain.network.ehcacheManager.InitializationManager;
import com.photon.photonchain.network.ehcacheManager.NioSocketChannelManager;
import com.photon.photonchain.network.proto.InesvMessage;
import com.photon.photonchain.network.proto.MessageManager;
import com.photon.photonchain.network.utils.DateUtil;
import com.photon.photonchain.storage.constants.Constants;
import com.photon.photonchain.storage.encryption.ECKey;
import com.photon.photonchain.storage.encryption.SHAEncrypt;
import com.photon.photonchain.storage.entity.Token;
import com.photon.photonchain.storage.entity.Transaction;
import com.photon.photonchain.storage.entity.UnconfirmedTran;
import com.photon.photonchain.storage.repository.TokenRepository;
import com.photon.photonchain.storage.repository.TransactionRepository;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

import static com.photon.photonchain.network.proto.EventTypeEnum.EventType.DeadLine_vote;
import static com.photon.photonchain.network.proto.EventTypeEnum.EventType.NEW_TRANSACTION;
import static com.photon.photonchain.network.proto.MessageTypeEnum.MessageType.RESPONSE;


/**
 * @Author:Lin
 * @Description:
 * @Date:16:04 2018/4/11
 * @Modified by:
 */
@Component
public class PVM {

    private static Logger logger = LoggerFactory.getLogger(PVM.class);


    @Autowired
    private NioSocketChannelManager nioSocketChannelManager;

    @Autowired
    private InitializationManager initializationManager;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private AssetsManager assetsManager;


    public void step(Program program) {
        int count = 1;
        int addressCount = 1;
        String contractAddress = "";
        List<String> ops = program.getOps();
        String error = "param type error...";
        for (String op : ops) {
            switch (OpCodeEnum.matchOpCode(op)) {
                case TRANS:
                    //生成流水
                    if (addressCount == 1) {
                        Object address = program.getStack().pop();//合同地址
                        contractAddress = ECKey.pubkeyToAddress(address.toString());
                        addressCount++;
                        logger.info("合同地址[{}]", contractAddress);
                    }

                    Object coinType = program.getStack().pop();//币种类型
                    Object changCoin = program.getStack().pop();//要兑换多少币
                    Object toAddress = program.getStack().pop();//合同方的钱包地址
                    Object fromAddress = program.getStack().pop();//我的钱包地址

                    long effectiveIncome = 0;
                    long expenditure = 0;

                    String tokenName = program.getHeap().getItem(coinType.toString() + "Type").toString();

                    logger.info("币种变量[{}]币种类型[{}]合同方的钱包地址[{}]我的钱包地址[{}]", coinType, toAddress, changCoin, toAddress, fromAddress);

                    long changCoinLong = initializationManager.unitConvert(changCoin, tokenName, Constants.MINI_UNIT).longValue();

                    if (ops.size() >= 2) {
                        if (count == 1) {
                            String address = ECKey.pubkeyToAddress(fromAddress.toString());
                            Map<String, Long> assetsFrom = assetsManager.getAccountAssets(fromAddress.toString(), tokenName);
                            if (assetsFrom == null) {
                                program.setResult(3);
                                return;
                            } else {
                                effectiveIncome = assetsFrom.get(Constants.TOTAL_EFFECTIVE_INCOME);
                                expenditure = assetsFrom.get(Constants.TOTAL_EXPENDITURE);
                                long balance = effectiveIncome - expenditure;
                                if (balance < changCoinLong) {
                                    program.setResult(3);
                                    return;
                                }
                            }
                        }
                        count++;
                    }
                    creatrTransaction(fromAddress.toString(), toAddress.toString(), "",
                            tokenName, changCoinLong, contractAddress, 1, 5, "");
                    program.setResult(1);
                    break;
                case VOTE:
                    Object contractAddressVote = program.getStack().pop();//合约地址
                    Object bin = program.getStack().pop();//合约bin
                    Object options = program.getStack().pop();//几个选项
                    Object fromAddressVote = program.getStack().pop();//投票钱包地址
                    Object chose = program.getStack().pop();//选择

                    contractAddress = ECKey.pubkeyToAddress(contractAddressVote.toString());

                    //合约资金
                    ECKey ecKey = new ECKey(new SecureRandom());
                    String randomContractAddress = Constants.ADDRESS_PREFIX + Hex.toHexString(ecKey.getAddress());
                    String toPubkey = Hex.toHexString(ecKey.getPubKey());

                    //交易金额为选项的千分之一
                    double value = Double.valueOf(options.toString()) / 1000;
                    long tranValue = initializationManager.unitConvert(value, Constants.PTN, Constants.MINI_UNIT).longValue();

                    Map<String, Long> assetsFrom = assetsManager.getAccountAssets(fromAddressVote.toString(), Constants.PTN);
                    if (assetsFrom == null) {
                        program.setResult(3);
                        break;
                    } else {
                        effectiveIncome = assetsFrom.get(Constants.TOTAL_EFFECTIVE_INCOME);
                        expenditure = assetsFrom.get(Constants.TOTAL_EXPENDITURE);
                        long balance = effectiveIncome - expenditure;
                        if (balance < tranValue) {
                            program.setResult(3);
                            break;
                        }
                    }

                    //投票选项流水
                    creatrTransaction(fromAddressVote.toString(), toPubkey, chose.toString(),
                            Constants.PTN, tranValue, contractAddress, 2, 4, bin.toString());//资金流水

                    creatrTransaction(fromAddressVote.toString(), contractAddressVote.toString(), chose.toString(),
                            Constants.PTN, 0L, contractAddress, 2, 7, bin.toString());//投票流水

                    program.setResult(1);
                    break;
                case STOP:
                    if(program.getStack().pop() != null){
                        contractAddress = program.getStack().pop().toString();//合约地址
                    }
                    if(contractAddress != null && !"".equals(contractAddress)){
                        List<String> hostList = nioSocketChannelManager.getChannelHostList();
                        InesvMessage.Message.Builder cancelBuilder = MessageManager.createMessageBuilder(RESPONSE, DeadLine_vote);//TODO
                        System.out.println("合约地址："+contractAddress);
                        cancelBuilder.setContractAddresss(contractAddress);
                        cancelBuilder.addAllNodeAddressList(hostList);
                        System.out.println("=================cancelBuilder:"+cancelBuilder);
                        nioSocketChannelManager.write(cancelBuilder.build());
                    }
                    program.setResult(2);
                    break;

                case TOKEN:
                    Object contractAddressToken = program.getStack().pop();//合约地址
                    Object token = program.getStack().pop();//代币名称
                    Object number = program.getStack().pop();//发行数量
                    Object bit = program.getStack().pop();//最小单位
                    Object fromAddressToken = program.getStack().pop();//代币发行人公钥
                    Object binToken = program.getStack().pop();//bin

                    //判断是否有人使用代币名
                    Integer result = transactionRepository.findTransByTokenNameAndType(token.toString(), 1);
                    if (result > 0) {
                        program.setResult(2);
                        break;
                    }
                    Long bitNum = 1L;
                    for (int i = 0; i < Integer.valueOf(bit.toString()); i++) {
                        bitNum = bitNum * 10L;
                    }
                    long transValueLong = Long.valueOf(number.toString()) * bitNum;
                    creatrTransaction(fromAddressToken.toString(), fromAddressToken.toString(), "", token.toString(), transValueLong, contractAddressToken.toString(), 3, 1, binToken.toString());
                    program.setResult(1);
                    break;
                case ADD:
                    Object b = program.getStack().pop();
                    Object a = program.getStack().pop();
                    if (b instanceof BigDecimal && a instanceof BigDecimal) {
                        program.getStack().push(((BigDecimal) a).add((BigDecimal) b).setScale(8, BigDecimal.ROUND_HALF_UP));
                    } else {
                        program.getStack().push(a.toString() + b.toString());
                    }
                    break;
                case SUB:
                    b = program.getStack().pop();
                    a = program.getStack().pop();
                    if (b instanceof BigDecimal && a instanceof BigDecimal) {
                        program.getStack().push(((BigDecimal) a).subtract((BigDecimal) b).setScale(8, BigDecimal.ROUND_HALF_UP).doubleValue());
                    } else {
                        program.setResult(error);
                    }
                    break;
                case MUL:
                    b = program.getStack().pop();
                    a = program.getStack().pop();
                    if (b instanceof BigDecimal && a instanceof BigDecimal) {
                        program.getStack().push(((BigDecimal) a).multiply((BigDecimal) b).setScale(8, BigDecimal.ROUND_HALF_UP));
                    } else {
                        program.setResult(error);
                    }
                    break;
                case DIV:
                    b = program.getStack().pop();
                    a = program.getStack().pop();
                    if (b instanceof BigDecimal && a instanceof BigDecimal) {
                        program.getStack().push(((BigDecimal) a).divide((BigDecimal) ((BigDecimal) b), 8, BigDecimal.ROUND_HALF_UP).setScale(8, BigDecimal.ROUND_HALF_UP));
                    } else {
                        program.setResult(error);
                    }
                    break;
                case RETURN:
                    b = program.getStack().pop();
                    program.setResult(b);
                    break;
                case INC:
                    a = program.getStack().pop();
                    if (a instanceof BigDecimal) {
                        a = ((BigDecimal) a).add(BigDecimal.ONE);
                        program.getStack().push(a);
                    }
                    break;
                case DEC:
                    a = program.getStack().pop();
                    if (a instanceof BigDecimal) {
                        a = ((BigDecimal) a).subtract(BigDecimal.ONE);
                        program.getStack().push(a);
                    }
                    break;
                case LT:
                    b = program.getStack().pop();
                    a = program.getStack().pop();
                    if (a instanceof BigDecimal && b instanceof BigDecimal) {
                        program.getStack().push(((BigDecimal) a).compareTo((BigDecimal) b) == -1 ? 1 : 0);
                    } else {
                        program.setResult(error);
                    }
                    break;
                case GT:
                    b = program.getStack().pop();
                    a = program.getStack().pop();
                    if (a instanceof BigDecimal && b instanceof BigDecimal) {
                        program.getStack().push(((BigDecimal) a).compareTo((BigDecimal) b) == 1 ? 1 : 0);
                    } else {
                        program.setResult(error);
                    }
                    break;
                case EQ:
                    b = program.getStack().pop();
                    a = program.getStack().pop();
                    if (a instanceof BigDecimal && b instanceof BigDecimal) {
                        program.getStack().push(((BigDecimal) a).compareTo((BigDecimal) b) == 0 ? 1 : 0);
                    } else {
                        program.setResult(a.toString().equals(b.toString()) ? 1 : 0);
                    }
                    break;
                case NOT:
                    b = program.getStack().pop();
                    a = program.getStack().pop();
                    if (a instanceof BigDecimal && b instanceof BigDecimal) {
                        program.getStack().push(((BigDecimal) a).compareTo((BigDecimal) b) != 0 ? 1 : 0);
                    } else {
                        program.setResult(!a.toString().equals(b.toString()) ? 1 : 0);
                    }
                    break;
                case ISZERO:
                    b = program.getStack().pop();
                    if (b instanceof BigDecimal) {
                        program.getStack().push(((BigDecimal) b).compareTo(BigDecimal.ZERO) == 0 ? 1 : 0);
                    } else {
                        program.setResult(error);
                    }
                    break;
                case MOD:
                    b = program.getStack().pop();
                    a = program.getStack().pop();
                    if (a instanceof BigDecimal && b instanceof BigDecimal) {
                        program.getStack().push(((BigDecimal) a).divideAndRemainder((BigDecimal) b)[1].setScale(8, BigDecimal.ROUND_HALF_UP));
                    } else {
                        program.setResult(error);
                    }
                    break;
                case AND:
                    b = program.getStack().pop();
                    a = program.getStack().pop();
                    if (a instanceof BigDecimal && b instanceof BigDecimal) {
                        program.getStack().push((((BigDecimal) a).compareTo(BigDecimal.ZERO) != 0) && (((BigDecimal) b).compareTo(BigDecimal.ZERO) != 0) ? 1 : 0);
                    } else {
                        program.setResult(error);
                    }
                    break;
                case OR:
                    b = program.getStack().pop();
                    a = program.getStack().pop();
                    if (a instanceof BigDecimal && b instanceof BigDecimal) {
                        program.getStack().push((((BigDecimal) a).compareTo(BigDecimal.ZERO) == 1) || (((BigDecimal) b).compareTo(BigDecimal.ZERO) == 1) ? 1 : 0);
                    } else {
                        program.setResult(error);
                    }
                    break;
            }
        }
    }


    /**
     * 生成流水 并广播出去
     */
    public void creatrTransaction(String transFrom, String transTo, String remark, String tokenName, Long transValueLong, String contractAddress, int type, Integer tranType, String bin) {
        String address = ECKey.pubkeyToAddress(transFrom);
        String prikeyFrom = "";

        Transaction transaction = transactionRepository.findByContract(contractAddress, 3);//根据合同地址查出这条合约
        UnconfirmedTran unconfirmedTran = null;

        if (!tokenName.toLowerCase().equals(Constants.PTN) && tranType != 1) {
            Token token = tokenRepository.findByName(tokenName);
            tokenName = token.getName();
        }

        if (type == 1) {
            if (transaction.getTransFrom().equals(transTo) && transaction.getTransTo().equals(transFrom)) {//取消的流水
                unconfirmedTran = new UnconfirmedTran(transFrom, transTo, String.valueOf(transaction.getBlockHeight()), tokenName, transValueLong, 0, DateUtil.getWebTime(), 6, contractAddress + "," + tokenName, contractAddress);
            } else {
                unconfirmedTran = new UnconfirmedTran(transFrom, transTo, String.valueOf(transaction.getBlockHeight()), tokenName, transValueLong, 0, DateUtil.getWebTime(), 5, contractAddress + "," + tokenName, contractAddress);
            }
        } else if (type == 2 && tranType == 7) {
            unconfirmedTran = new UnconfirmedTran(transFrom, transTo, remark, tokenName, transValueLong, 0, DateUtil.getWebTime(), tranType, contractAddress, bin, 2, 0, "");
        } else if (type == 2 && tranType == 4) {
            unconfirmedTran = new UnconfirmedTran(transFrom, transTo, remark, tokenName, transValueLong, 0, DateUtil.getWebTime(), tranType, contractAddress, bin, 2, 0, "");
        } else if (type == 3 && tranType == 1) {
            unconfirmedTran = new UnconfirmedTran(transFrom, transTo, remark, tokenName, transValueLong, 0, DateUtil.getWebTime(), tranType, contractAddress, bin, 3, 0, "");
        }

        Map<String, String> localAccount = initializationManager.getAccountListByAddress(address);
        if (!"".equals(localAccount.get(Constants.PRIKEY))) {
            prikeyFrom = localAccount.get(Constants.PRIKEY);
        } else {
            prikeyFrom = "";
        }
        byte[] transSignature = JSON.toJSONString(ECKey.fromPrivate(Hex.decode(prikeyFrom)).sign(SHAEncrypt.sha3(SerializationUtils.serialize(unconfirmedTran.toString())))).getBytes();
        unconfirmedTran.setTransSignature(transSignature);

        InesvMessage.Message.Builder builder = MessageManager.createMessageBuilder(RESPONSE, NEW_TRANSACTION);
        builder.setUnconfirmedTran(MessageManager.createUnconfirmedTranMessage(unconfirmedTran));
        List<String> hostList = nioSocketChannelManager.getChannelHostList();
        builder.addAllNodeAddressList(hostList);
        nioSocketChannelManager.write(builder.build());

    }


}
