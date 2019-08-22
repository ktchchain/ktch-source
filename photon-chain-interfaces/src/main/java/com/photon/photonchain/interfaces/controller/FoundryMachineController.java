package com.photon.photonchain.interfaces.controller;


import com.photon.photonchain.interfaces.utils.Res;
import com.photon.photonchain.network.core.FoundryMachine;
import com.photon.photonchain.network.ehcacheManager.*;
import com.photon.photonchain.network.proto.InesvMessage;
import com.photon.photonchain.network.proto.MessageManager;
import com.photon.photonchain.storage.constants.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static com.photon.photonchain.network.proto.EventTypeEnum.EventType.ADD_PARTICIPANT;
import static com.photon.photonchain.network.proto.EventTypeEnum.EventType.DEL_PARTICIPANT;
import static com.photon.photonchain.network.proto.MessageTypeEnum.MessageType.RESPONSE;

/**
 * @Author:Lin
 * @Description:
 * @Date:16:52 2018/1/11
 * @Modified by:
 */
@RestController
@RequestMapping("FoundryMachineController")
public class FoundryMachineController {

    final static Logger logger = LoggerFactory.getLogger(FoundryMachineController.class);

    @Autowired
    private FoundryMachine foundryMachine;

    @Autowired
    private FoundryMachineManager foundryMachineManager;

    @Autowired
    private SyncBlockManager syncBlockManager;

    @Autowired
    private InitializationManager initializationManager;


    @Autowired
    private NioSocketChannelManager nioSocketChannelManager;

    @Autowired
    private AssetsManager assetsManager;

    @Autowired
    private SyncUnconfirmedTranManager syncUnconfirmedTranManager;

    @Autowired
    private SyncTokenManager syncTokenManager;

    @Autowired
    private SyncParticipantManager syncParticipantManager;

    @GetMapping("noteFoundryMachineState")
    @ResponseBody
    public Res noteFoundryMachineState() {
        Res res = new Res();
        boolean isExistFoundryList = false;
        Map<String, String> localAccount = initializationManager.getAccountList();
        for (Map.Entry<String, String> entry : localAccount.entrySet()) {
            String address = entry.getKey();
            Map<String, String> accountInfo = initializationManager.getAccountListByAddress(address);
            String pubkey = accountInfo.get(Constants.PUBKEY);
            if (syncParticipantManager.getParticipantList().containsKey(pubkey)) {
                foundryMachineManager.setFoundryMachine(pubkey, true);
                initializationManager.setFoundryMachineState(true);
                isExistFoundryList = true;
                break;
            }
        }

        if (isExistFoundryList && initializationManager.getFoundryMachineState() && nioSocketChannelManager.getActiveNioSocketChannelCount() >= Constants.FORGABLE_NODES) {
            res.setCode(Res.CODE_120);
        } else {
            res.setCode(Res.CODE_121);
        }
        return res;
    }

    @GetMapping("startFoundryMachine")
    @ResponseBody
    public Res startFoundryMachine(String passWord, String address) {
        Res res = new Res();
        boolean isExistFoundryList = false;
        Map<String, String> localAccount = initializationManager.getAccountList();
        for (Map.Entry<String, String> entry : localAccount.entrySet()) {
            Map<String, String> accountInfos = initializationManager.getAccountListByAddress(entry.getKey());
            String pubkey = accountInfos.get(Constants.PUBKEY);
            if (syncParticipantManager.getParticipantList().containsKey(pubkey)) {
                isExistFoundryList = true;
                break;
            }
        }
        if (isExistFoundryList && initializationManager.getFoundryMachineState()) {
            res.setCode(Res.CODE_130);
            return res;
        }
        if (syncBlockManager.isSyncBlock() || syncUnconfirmedTranManager.isSyncTransaction() || syncTokenManager.isSyncToken() || syncParticipantManager.isSyncParticipant()) {
            res.setCode(Res.CODE_115);
            return res;
        }
        Map<String, String> accountInfo = initializationManager.getAccountListByAddress(address);
        String pwd = accountInfo.get(Constants.PWD);
        String priKey = accountInfo.get(Constants.PRIKEY);
        String pubKey = accountInfo.get(Constants.PUBKEY);
        if (pubKey.equals("")) {
            res.setCode(Res.CODE_102);
            return res;
        }
        if (!pwd.equals(passWord)) {
            res.code = Res.CODE_301;
            return res;
        }
        Map<String, Long> assets = assetsManager.getAccountAssets(pubKey, Constants.PTN);
        long balance = assets.get(Constants.BALANCE);

        if (balance <= 0) {
            res.code = Res.CODE_106;
            return res;
        }
        if (foundryMachineManager.foundryMachineIsStart(pubKey) && syncParticipantManager.getParticipantList().containsKey(pubKey)) {
            res.code = Res.CODE_116;
            return res;
        }
        foundryMachineManager.setFoundryMachine(pubKey, true);
        InesvMessage.Message.Builder builder = MessageManager.createMessageBuilder(RESPONSE, ADD_PARTICIPANT);
        builder.setParticipant(pubKey);
        List<String> hostList = nioSocketChannelManager.getChannelHostList();
        builder.addAllNodeAddressList(hostList);
        nioSocketChannelManager.write(builder.build());
        foundryMachine.init(pubKey, priKey);
        initializationManager.setFoundryMachineState(true);
        res.code = Res.CODE_116;
        logger.info("启动锻造--：" + nioSocketChannelManager.getChannelHostList());
        return res;
    }

    @GetMapping("stopNode")
    @ResponseBody
    public Res stopNode() {
        Res res = new Res();
        String removePubkey = "";
        Map<String, String> localAccount = initializationManager.getAccountList();
        for (Map.Entry<String, String> entry : localAccount.entrySet()) {
            String address = entry.getKey();
            Map<String, String> accountInfo = initializationManager.getAccountListByAddress(address);
            String pubkey = accountInfo.get(Constants.PUBKEY);
            if (syncParticipantManager.getParticipantList().containsKey(pubkey)) {
                removePubkey = pubkey;
                break;
            }
        }
        foundryMachineManager.setFoundryMachine(removePubkey, false);
        initializationManager.setFoundryMachineState(false);
        if (!removePubkey.equals("")) {
            InesvMessage.Message.Builder builder = MessageManager.createMessageBuilder(RESPONSE, DEL_PARTICIPANT);
            builder.setParticipant(removePubkey);
            List<String> hostList = nioSocketChannelManager.getChannelHostList();
            builder.addAllNodeAddressList(hostList);
            nioSocketChannelManager.write(builder.build());
        }
        res.code = Res.CODE_118;
        return res;
    }


    @GetMapping("stopFoundryMachine")
    @ResponseBody

    public Res stopFoundryMachine(String passWord, String address) {
        Res res = new Res();
        if (syncBlockManager.isSyncBlock() || syncUnconfirmedTranManager.isSyncTransaction() || syncTokenManager.isSyncToken() || syncParticipantManager.isSyncParticipant()) {
            res.setCode(Res.CODE_117);
            return res;
        }
        Map<String, String> accountInfo = initializationManager.getAccountListByAddress(address);
        String pwd = accountInfo.get(Constants.PWD);
        String priKey = accountInfo.get(Constants.PRIKEY);
        String pubKey = accountInfo.get(Constants.PUBKEY);
        if (pubKey.equals("")) {
            res.setCode(Res.CODE_102);
            return res;
        }
        if (!pwd.equals(passWord)) {
            res.code = Res.CODE_301;
            return res;
        }
        foundryMachineManager.setFoundryMachine(pubKey, false);
        InesvMessage.Message.Builder builder = MessageManager.createMessageBuilder(RESPONSE, DEL_PARTICIPANT);
        builder.setParticipant(pubKey);
        List<String> hostList = nioSocketChannelManager.getChannelHostList();
        builder.addAllNodeAddressList(hostList);
        nioSocketChannelManager.write(builder.build());
        initializationManager.setFoundryMachineState(false);
        res.code = Res.CODE_118;
        return res;
    }

    @GetMapping("foundryMachineState")
    @ResponseBody
    public Res FoundryMachineState(String address) {
        Res res = new Res();
        Map<String, String> accountInfo = initializationManager.getAccountListByAddress(address);
        String pubKey = accountInfo.get(Constants.PUBKEY);
        if (pubKey.equals("")) {
            res.setCode(Res.CODE_102);
            return res;
        }
        if (foundryMachineManager.foundryMachineIsStart(pubKey) && syncParticipantManager.getParticipantList().containsKey(pubKey) && nioSocketChannelManager.getActiveNioSocketChannelCount() >= Constants.FORGABLE_NODES) {
            res.code = Res.CODE_120;
        } else {
            res.code = Res.CODE_121;
        }
        return res;
    }
}
