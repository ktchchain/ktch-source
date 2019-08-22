package com.photon.photonchain.interfaces.controller;

import com.alibaba.fastjson.JSON;
import com.photon.photonchain.extend.compiler.Pcompiler;
import com.photon.photonchain.interfaces.utils.DateUtil;
import com.photon.photonchain.interfaces.utils.Res;
import com.photon.photonchain.interfaces.utils.ValidateUtil;
import com.photon.photonchain.network.ehcacheManager.*;
import com.photon.photonchain.network.proto.InesvMessage;
import com.photon.photonchain.network.proto.MessageManager;
import com.photon.photonchain.network.utils.TokenUtil;
import com.photon.photonchain.storage.constants.Constants;
import com.photon.photonchain.storage.encryption.ECKey;
import com.photon.photonchain.storage.encryption.SHAEncrypt;
import com.photon.photonchain.storage.entity.Token;
import com.photon.photonchain.storage.entity.UnconfirmedTran;
import com.photon.photonchain.storage.repository.TokenRepository;
import com.photon.photonchain.storage.repository.TransactionRepository;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.photon.photonchain.network.proto.EventTypeEnum.EventType.NEW_TRANSACTION;
import static com.photon.photonchain.network.proto.MessageTypeEnum.MessageType.RESPONSE;

/**
 * @Author:Lin
 * @Description:
 * @Date:11:45 2018/1/31
 * @Modified by:
 */
@Controller
@RequestMapping("TokenController")
public class TokenController {

    private static Logger logger = LoggerFactory.getLogger(TokenController.class);

    @Autowired
    private NioSocketChannelManager nioSocketChannelManager;

    @Autowired
    private InitializationManager initializationManager;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private AssetsManager assetsManager;
    @Autowired
    private SyncBlockManager syncBlockManager;
    @Autowired
    private SyncUnconfirmedTranManager syncUnconfirmedTranManager;
    @Autowired
    private SyncTokenManager syncTokenManager;
    @Autowired
    private SyncParticipantManager syncParticipantManager;

    @Autowired
    private UnconfirmedTranManager unconfirmedTranManager;

    @Autowired
    private TransactionRepository transactionRepository;

    @PostMapping("addToken")
    @ResponseBody
    public Res addToken(@RequestParam String address, @RequestParam String symbol, @RequestParam String name, @RequestParam String decimals, @RequestParam String tokenAmount,
                        @RequestParam String remark, @RequestParam String passWord) {
        Res res = new Res();
        String contractStr = "pcompiler version 1.0.0 ;\n" +
                "contract t{\n" +
                "set  integer   tokenBit = "+decimals+";\n" +
                "set integer tokenNum = "+tokenAmount+";\n" +
                "set str tokenName = \""+name+"\";\n" +
                "event token( tokenBit ,tokenNum ,tokenName);}";
        //获取用户信息
        Map<String, String> localAccount = initializationManager.getAccountListByAddress(address);
        String pubKey = localAccount.get(Constants.PUBKEY);
        String priKey = localAccount.get(Constants.PRIKEY);
        String savePwd = localAccount.get(Constants.PWD);

        InesvMessage.Message.Builder builder = MessageManager.createMessageBuilder(RESPONSE, NEW_TRANSACTION);
        InesvMessage.Message.Builder builderFee = MessageManager.createMessageBuilder(RESPONSE, NEW_TRANSACTION);
        StringBuilder contractSb = new StringBuilder(contractStr);
        //为第一行添加address start
        int temp1 = contractStr.indexOf("{") + 1;
        contractSb = contractSb.insert(temp1, "set address add=" + pubKey + ";");
        contractSb = new StringBuilder(Pcompiler.complementSpace(contractSb.toString()));//为参数=补空格
        //为第一行添加address end
        //创建合约地址 start
        ECKey ecKey = new ECKey(new SecureRandom());
        String contractPrivateKey = Hex.toHexString(ecKey.getPrivKeyBytes());
        String contractPublicKey = Hex.toHexString(ecKey.getPubKey());
        String contractAddress = Constants.ADDRESS_PREFIX + Hex.toHexString(ecKey.getAddress());
        String bin = Pcompiler.compilerContract(contractSb.toString());
        //创建合约地址 end
        ECKey ecKeyFee = new ECKey(new SecureRandom());//生成手续费存放账户
        long webTime = DateUtil.getWebTime();
        if (webTime == Constants.GENESIS_TIME) {
            res.setCode(Res.CODE_500);
            return res;
        }
        if (syncBlockManager.isSyncBlock() || syncUnconfirmedTranManager.isSyncTransaction() || syncTokenManager.isSyncToken() || syncParticipantManager.isSyncParticipant()) {
            res.setCode(Res.CODE_115);
            return res;
        }
        if (name.length() < 3 || name.length() > 20 || !ValidateUtil.checkLetter(name)) {
            res.setCode(Res.CODE_127);
            return res;
        }
        if (tokenRepository.findByName(name.toLowerCase()) != null || name.equalsIgnoreCase(Constants.PTN)) {
            res.setCode(Res.CODE_123);
            return res;
        }
        if (!ValidateUtil.checkPositiveInteger(tokenAmount) || tokenAmount.length() > 11) {
            res.setCode(Res.CODE_124);
            return res;
        }
        int decimalsInt = 0;
        try {
            decimalsInt = Integer.parseInt(decimals);
        } catch (Exception e) {
            res.setCode(Res.CODE_125);
            return res;
        }
        long tokenAmountLong = Long.parseLong(tokenAmount);
        long tokenAmountLongMiniUnit = 1;
        for (int i = 0; i < decimalsInt; i++) {
            tokenAmountLongMiniUnit *= 10;
        }
        //todo
        if (!savePwd.equals(passWord)) {
            res.setCode(Res.CODE_301);
            return res;
        }
        if (pubKey.equals("")) {
            res.setCode(Res.CODE_102);
            return res;
        }
        if (tokenAmountLong < 1000 || tokenAmountLong >= 100000000000l) {
            res.setCode(Res.CODE_124);
            return res;
        }
        if (decimalsInt < 6 || decimalsInt > 8) {
            res.setCode(Res.CODE_125);
            return res;
        }

        //校验tokenname是否存在 start
        boolean isExistUnconfirmedTrans = unconfirmedTranManager.isExistKeyEqualsIgnoreCase(name);
        Integer confirmTransaction = transactionRepository.findTransByTokenName(name);
        if (isExistUnconfirmedTrans || confirmTransaction > 0) {
            res.setCode(Res.CODE_123);
            return res;
        }
        //校验tokenname是否存在 end

        if (tokenAmountLong >= 10000000000l && decimalsInt > 7) {
            res.setCode(Res.CODE_143);
            return res;
        }
        Token token = new Token(symbol, name, "", decimalsInt);
        long fee = Double.valueOf(TokenUtil.TokensRate(name) * Constants.MININUMUNIT).longValue();
        if (fee == 0) fee = 1;

        Map<String, Long> assets = assetsManager.getAccountAssets(pubKey, Constants.PTN);
        long balance = assets.get(Constants.BALANCE);

        if (balance < fee) {
            res.setCode(Res.CODE_106);
            return res;
        }
        //TODO 暂存 未确定流水
        UnconfirmedTran unconfirmedTran = new UnconfirmedTran(pubKey, contractPublicKey, "", name
                , 0, 0, webTime, 3, contractAddress, bin, 3, 0, null);
        byte[] transSignature = JSON.toJSONString(ECKey.fromPrivate(Hex.decode(priKey)).sign(SHAEncrypt.sha3(SerializationUtils.serialize(unconfirmedTran.toString())))).getBytes();
        unconfirmedTran.setTransSignature(transSignature);
        logger.warn("代币流水：" + unconfirmedTran.toString());
        builder.setUnconfirmedTran(MessageManager.createUnconfirmedTranMessage(unconfirmedTran));
        //手续费流水
        UnconfirmedTran unconfirmedTranFee = new UnconfirmedTran(pubKey, Hex.toHexString(ecKeyFee.getPubKey()), "", Constants.PTN
                , fee, 0, webTime, 4, contractAddress, bin, 3, 0, null);
        byte[] transSignatureFee = JSON.toJSONString(ECKey.fromPrivate(Hex.decode(priKey)).sign(SHAEncrypt.sha3(SerializationUtils.serialize(unconfirmedTranFee.toString())))).getBytes();
        unconfirmedTranFee.setTransSignature(transSignatureFee);
        logger.warn("代币手续费流水：" + unconfirmedTranFee.toString());
        builderFee.setUnconfirmedTran(MessageManager.createUnconfirmedTranMessage(unconfirmedTranFee));

        List<String> hostList = nioSocketChannelManager.getChannelHostList();//获取所有节点
        builder.addAllNodeAddressList(hostList);
        builderFee.addAllNodeAddressList(hostList);

        nioSocketChannelManager.write(builderFee.build());
        nioSocketChannelManager.write(builder.build());

        res.setCode(Res.CODE_119);
        return res;
//        long webTime = DateUtil.getWebTime();
//        if (webTime == Constants.GENESIS_TIME) {
//            res.setCode(Res.CODE_500);
//            return res;
//        }
//        if (syncBlockManager.isSyncBlock() || syncUnconfirmedTranManager.isSyncTransaction() || syncTokenManager.isSyncToken() || syncParticipantManager.isSyncParticipant()) {
//            res.setCode(Res.CODE_115);
//            return res;
//        }
//        if (name.length() < 3 || name.length() > 20 || !ValidateUtil.checkLetter(name)) {
//            res.setCode(Res.CODE_127);
//            return res;
//        }
//        if (tokenRepository.findByName(name.toLowerCase()) != null || name.equalsIgnoreCase(Constants.PTN)) {
//            res.setCode(Res.CODE_123);
//            return res;
//        }
//        if (!ValidateUtil.checkPositiveInteger(tokenAmount) || tokenAmount.length() > 11) {
//            res.setCode(Res.CODE_124);
//            return res;
//        }
//        int decimalsInt = 0;
//        try {
//            decimalsInt = Integer.parseInt(decimals);
//        } catch (Exception e) {
//            res.setCode(Res.CODE_125);
//            return res;
//        }
//        long tokenAmountLong = Long.parseLong(tokenAmount);
//        long tokenAmountLongMiniUnit = 1;
//        for (int i = 0; i < decimalsInt; i++) {
//            tokenAmountLongMiniUnit *= 10;
//        }
//        Map<String, String> localAccount = initializationManager.getAccountListByAddress(address);
//        String pubKey = localAccount.get(Constants.PUBKEY);
//        String priKey = localAccount.get(Constants.PRIKEY);
//        String savePwd = localAccount.get(Constants.PWD);
//        if (!savePwd.equals(passWord)) {
//            res.setCode(Res.CODE_301);
//            return res;
//        }
//        if (pubKey.equals("")) {
//            res.setCode(Res.CODE_102);
//            return res;
//        }
//        if (tokenAmountLong < 1000 || tokenAmountLong >= 100000000000l) {
//            res.setCode(Res.CODE_124);
//            return res;
//        }
//        if (decimalsInt < 6 || decimalsInt > 8) {
//            res.setCode(Res.CODE_125);
//            return res;
//        }
//        if (tokenAmountLong >= 10000000000l && decimalsInt > 7) {
//            res.setCode(Res.CODE_143);
//            return res;
//        }
//        Token token = new Token(symbol, name, "", decimalsInt);
//        long fee = Double.valueOf(TokenUtil.TokensRate(name) * Constants.MININUMUNIT).longValue();
//        if (fee == 0) fee = 1;
//
//        Map<String, Long> assets = assetsManager.getAccountAssets(pubKey, Constants.PTN);
//        long balance = assets.get(Constants.BALANCE);
//
//        if (balance < fee) {
//            res.setCode(Res.CODE_106);
//            return res;
//        }
//        UnconfirmedTran unconfirmedTran = new UnconfirmedTran(pubKey, pubKey, remark, name, tokenAmountLongMiniUnit * tokenAmountLong, fee, webTime, 1);
//        byte[] transSignature = JSON.toJSONString(ECKey.fromPrivate(Hex.decode(priKey)).sign(SHAEncrypt.sha3(SerializationUtils.serialize(unconfirmedTran.toString())))).getBytes();
//        unconfirmedTran.setTransSignature(transSignature);
//        InesvMessage.Message.Builder builder = MessageManager.createMessageBuilder(RESPONSE, NEW_TOKEN);
//        builder.setUnconfirmedTran(MessageManager.createUnconfirmedTranMessage(unconfirmedTran));
//        builder.setToken(MessageManager.createTokenMessage(token));
//        List<String> hostList = nioSocketChannelManager.getChannelHostList();
//        builder.addAllNodeAddressList(hostList);
//        nioSocketChannelManager.write(builder.build());
    }


    @GetMapping("getAddTokenFee")
    @ResponseBody
    public Res getAddTokenFee(@RequestParam String name, @RequestParam String tokenAmount, @RequestParam int decimal) {
        Res res = new Res();
        Map resultMap = new HashMap();
        long tokenAmountLongMiniUnit = 1;
        for (int i = 0; i < decimal; i++) {
            tokenAmountLongMiniUnit *= 10;
        }
        long tokenAmountLong = Long.parseLong(tokenAmount);
        long fee = Double.valueOf(TokenUtil.TokensRate(name) * Constants.MININUMUNIT).longValue();
        if (fee == 0) fee = 1;
        resultMap.put("fee", new BigDecimal(fee).divide(new BigDecimal(Constants.MININUMUNIT)).setScale(6, BigDecimal.ROUND_HALF_UP).toPlainString());
        res.setCode(Res.CODE_100);
        res.setData(resultMap);
        return res;
    }


    @GetMapping("getAllToken")
    @ResponseBody
    public Iterable<Token> getTokenList() {
        return tokenRepository.findAll();
    }

}
