package com.photon.photonchain.interfaces.controller;

import com.alibaba.fastjson.JSON;
import com.photon.photonchain.extend.vm.Heap;
import com.photon.photonchain.extend.vm.Program;
import com.photon.photonchain.interfaces.utils.*;
import com.photon.photonchain.network.core.GenesisBlock;
import com.photon.photonchain.network.core.ResetData;
import com.photon.photonchain.network.ehcacheManager.*;
import com.photon.photonchain.network.peer.PeerClient;
import com.photon.photonchain.network.proto.InesvMessage;
import com.photon.photonchain.network.proto.MessageManager;
import com.photon.photonchain.storage.constants.Constants;
import com.photon.photonchain.storage.encryption.ECKey;
import com.photon.photonchain.storage.encryption.SHAEncrypt;
import com.photon.photonchain.storage.entity.*;
import com.photon.photonchain.storage.repository.*;
import com.photon.photonchain.storage.utils.ContractUtil;
import jodd.http.HttpRequest;
import jodd.http.HttpResponse;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.*;

import static com.photon.photonchain.network.proto.EventTypeEnum.EventType.NEW_TRANSACTION;
import static com.photon.photonchain.network.proto.MessageTypeEnum.MessageType.RESPONSE;

/**
 * @author: lqh
 * @description: ss
 * @program: photon-chain-new
 * @create: 2018-01-19 15:40
 **/
@Controller
@RequestMapping("WalletController")
public class WalletController {

    private static Logger logger = LoggerFactory.getLogger(WalletController.class);

    @Autowired
    private BlockRepository blockRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    InitializationManager initializationManager;

    @Autowired
    SyncBlockManager syncBlockManager;

    @Autowired
    NioSocketChannelManager nioSocketChannelManager;

    @Autowired
    SyncUnconfirmedTranManager syncUnconfirmedTranManager;

    @Autowired
    TokenRepository tokenRepository;

    @Autowired
    private PeerClient peerClient;

    @Autowired
    private ResetData resetData;

    @Autowired
    private UnconfirmedTranRepository unconfirmedTranRepository;

    @Autowired
    private AssetsManager assetsManager;
    private Res res;

    @Autowired
    private SyncTokenManager syncTokenManager;

    @Autowired
    private AddressAndPubkeyRepository addressAndPubkeyRepository;
    @Autowired
    private SyncParticipantManager syncParticipantManager;

    /**
     * 获取钱包总资产
     *
     * @return
     */
    @GetMapping("getTotalBalance")
    @ResponseBody
    public Res getTotalBalance(@RequestParam String tokenName) {
        Res res = new Res();
        Map<String, Object> dataMap = new HashMap<>();
        long totalEffectiveIncome = 0;
        long totalExpenditure = 0l;
        long totalIncome = 0;
        long balance = 0;
        long totalBalance = 0;
        List accountMapList = new ArrayList();
        try {
            Map<String, String> localAccount = initializationManager.getAccountList();
            for (Map.Entry<String, String> entry : localAccount.entrySet()) {
                String address = entry.getKey();
                Map<String, String> accountInfo = initializationManager.getAccountListByAddress(address);
                String pubkey = accountInfo.get(Constants.PUBKEY);
                //TODO 计算资产
                Map<String, Long> assetsMap = assetsManager.getAccountAssets(pubkey, tokenName);
                totalIncome = assetsMap.get(Constants.TOTAL_INCOME);
                totalExpenditure = assetsMap.get(Constants.TOTAL_EXPENDITURE);
                totalEffectiveIncome = assetsMap.get(Constants.TOTAL_EFFECTIVE_INCOME);
                balance = assetsMap.get(Constants.BALANCE);
                totalBalance += balance;
                Map accountMap = new LinkedHashMap();
                accountMap.put("address", address);
                accountMap.put(Constants.PUBKEY, pubkey);
                accountMap.put("totalExpenditure", initializationManager.unitConvert(totalExpenditure, tokenName, Constants.MAX_UNIT).toPlainString());
                accountMap.put("totalIncome", initializationManager.unitConvert(totalIncome, tokenName, Constants.MAX_UNIT).toPlainString());
                accountMap.put("totalEffectiveIncome", initializationManager.unitConvert(totalEffectiveIncome, tokenName, Constants.MAX_UNIT).toPlainString());
                accountMap.put("balance", initializationManager.unitConvert(balance, tokenName, Constants.MAX_UNIT).toPlainString());
                accountMapList.add(accountMap);
            }
            BigDecimal total = initializationManager.unitConvert(totalBalance, tokenName, Constants.MAX_UNIT);
            total = total.compareTo(new BigDecimal(0)) > 0 ? total : new BigDecimal(0);
            dataMap.put("totalBalance", total.toPlainString());
            dataMap.put("accounts", accountMapList);
            res.setCode(Res.CODE_100);
            res.setData(dataMap);
        } catch (Exception e) {
            res.setCode(Res.CODE_101);
            res.setData("");
            e.printStackTrace();
        }
        return res;
    }

    /**
     * 获取账户列表
     *
     * @return
     */
    @GetMapping("getAllAccount")
    @ResponseBody
    public Res getAllAccount(String tokenName) {
        Res res = new Res();
        List<Map> accountMapList = new ArrayList<>();
        Map<String, Object> dataMap = new HashMap<>();
        try {
            long t1 = System.currentTimeMillis();
            Map<String, String> localAccount = initializationManager.getAccountList();
            long t2 = System.currentTimeMillis();
            if (localAccount.isEmpty()) {
                res.setCode(Res.CODE_101);
                return res;
            }
            long t3 = System.currentTimeMillis();
            for (Map.Entry<String, String> entry : localAccount.entrySet()) {
                String address = entry.getKey();
                Map<String, String> accountInfo = initializationManager.getAccountListByAddress(address);
                String pubkey = accountInfo.get(Constants.PUBKEY);
                Long totalExpenditure = 0l;
                Long totalIncome = 0l;
                Long totalEffectiveIncome = 0l;
                Long balance = 0l;
                Map<String, Object> accountMap = new HashMap<>();
                //TODO 计算资产
                Map<String, Long> assetsMap = assetsManager.getAccountAssets(pubkey, tokenName);
                totalIncome = assetsMap.get(Constants.TOTAL_INCOME);
                totalExpenditure = assetsMap.get(Constants.TOTAL_EXPENDITURE);
                totalEffectiveIncome = assetsMap.get(Constants.TOTAL_EFFECTIVE_INCOME);
                balance = assetsMap.get(Constants.BALANCE);
                accountMap.put("totalExpenditure", initializationManager.unitConvert(totalExpenditure, tokenName, Constants.MAX_UNIT).toPlainString());
                accountMap.put("totalIncome", initializationManager.unitConvert(totalIncome, tokenName, Constants.MAX_UNIT).toPlainString());
                accountMap.put("totalEffectiveIncome", initializationManager.unitConvert(totalEffectiveIncome, tokenName, Constants.MAX_UNIT).toPlainString());
                accountMap.put("balance", initializationManager.unitConvert(balance, tokenName, Constants.MAX_UNIT).toPlainString());
                accountMap.put("address", address);
                accountMap.put(Constants.PUBKEY, pubkey);
                accountMapList.add(accountMap);
            }
            long t4 = System.currentTimeMillis();
            logger.info("getAccountList-time[{}],for-time[{},localAccount.size:{}]", (t2 - t1), (t4 - t3), localAccount.size());
            dataMap.put("accounts", accountMapList);
            res.setCode(Res.CODE_100);
            res.setData(dataMap);
        } catch (Exception e) {
            res.setCode(Res.CODE_101);
            res.setData("");
            e.printStackTrace();
        }
        return res;
    }

    /**
     * 获取账户资产
     * address ： 钱包地址
     * tokenName ：代币
     *
     * @return
     */
    @GetMapping("getAccountBalance")
    @ResponseBody
    public Res getAccountBalance(@RequestParam String address, @RequestParam String tokenName) {


        /*HttpRequest request = HttpRequest.get("http://47.92.213.211:7876/WalletController/getAccountBalance?address="+address+"&tokenName=ktch");
        HttpResponse response = request.send();
        String balances = JSON.parseObject(response.bodyText()).getJSONObject("data").getString("balance");*/



        Res res = new Res();
        Map<String, Object> resultMap = new HashMap<>();
        Map<String, Object> accountMap = new HashMap<>();
        List<Map<String, Object>> transactionList = new ArrayList<>();
        long totalIncome = 0;
        long totalExpenditure = 0;
        long totalEffectiveIncome = 0;
        long balance = 0;
        String pubKey = "";

        //HttpRequest request = HttpRequest.get("http://47.92.213.211:7876/WalletController/getAccountBalance?address="+address+"&tokenName=ktch");
        //HttpResponse response = request.send();
        //String balances = JSON.parseObject(response.bodyText()).getJSONObject("data").getString("balance");

        Map<String, String> account = initializationManager.getAccountListByAddress(address);
        if (account.get(Constants.PUBKEY).equals("")) {
            res.setCode(Res.CODE_102);
            res.setData(resultMap);
            return res;
        }
        pubKey = account.get(Constants.PUBKEY);
        //TODO 计算资产
        Map<String, Long> assetsMap = assetsManager.getAccountAssets(pubKey, tokenName);
        totalIncome = assetsMap.get(Constants.TOTAL_INCOME);
        totalExpenditure = assetsMap.get(Constants.TOTAL_EXPENDITURE);
        totalEffectiveIncome = assetsMap.get(Constants.TOTAL_EFFECTIVE_INCOME);
        balance = assetsMap.get(Constants.BALANCE);

        accountMap.put("totalExpenditure", initializationManager.unitConvert(totalExpenditure, tokenName, Constants.MAX_UNIT).toPlainString());
        accountMap.put("totalIncome", initializationManager.unitConvert(totalIncome, tokenName, Constants.MAX_UNIT).toPlainString());
        accountMap.put("totalEffectiveIncome", initializationManager.unitConvert(totalEffectiveIncome, tokenName, Constants.MAX_UNIT).toPlainString());
        accountMap.put("balance", initializationManager.unitConvert(balance, tokenName, Constants.MAX_UNIT).toPlainString());
        accountMap.put("address", address);
        accountMap.put("pubKey", account.get(Constants.PUBKEY));
        resultMap.put("accountMap", accountMap);

        /*accountMap.put("totalExpenditure", 0);
        accountMap.put("totalIncome", 0);
        accountMap.put("totalEffectiveIncome", 0);
        accountMap.put("balance", balances);
        accountMap.put("address", address);
        accountMap.put("pubKey", "");
        resultMap.put("accountMap", accountMap);*/
        res.setCode(Res.CODE_100);
        res.setData(resultMap);
        return res;

    }

    //TODO：弃用

    /**
     * 获取账户信息(地址+交易列表)
     *
     * @param address   ： 钱包地址
     * @param tokenName ： 币种
     * @return
     */
    @GetMapping("getAccountInfo")
    @ResponseBody
    public Res getAccountInfo(@RequestParam String address, @RequestParam String tokenName, PageObject pageObject) {
        Res res = new Res();
        Map<String, Object> resultMap = new HashMap<>();
        Map<String, Object> accountMap = new HashMap<>();
        List<Map<String, Object>> transactionList = new ArrayList<>();
        Map<String, String> localAccount = initializationManager.getAccountListByAddress(address);
        String pubKey = localAccount.get(Constants.PUBKEY);
        if (pubKey.equals("")) {
            res.setCode(Res.CODE_102);
            return res;
        }

        //TODO 计算资产
        long t1 = System.currentTimeMillis();
        Map<String, Long> assetsMap = assetsManager.getAccountAssets(pubKey, tokenName);
        long totalIncome = assetsMap.get(Constants.TOTAL_INCOME);
        long totalExpenditure = assetsMap.get(Constants.TOTAL_EXPENDITURE);
        long totalEffectiveIncome = assetsMap.get(Constants.TOTAL_EFFECTIVE_INCOME);
        long balance = assetsMap.get(Constants.BALANCE);
        BigDecimal totalExpenditureBig = initializationManager.unitConvert(totalExpenditure, tokenName, Constants.MAX_UNIT);
        BigDecimal totalIncomeBig = initializationManager.unitConvert(totalIncome, tokenName, Constants.MAX_UNIT);
        BigDecimal totalEffectiveIncomeBig = initializationManager.unitConvert(totalEffectiveIncome, tokenName, Constants.MAX_UNIT);
        BigDecimal balanceBig = initializationManager.unitConvert(balance, tokenName, Constants.MAX_UNIT);
        long t2 = System.currentTimeMillis();

        accountMap.put("totalExpenditure", totalExpenditureBig.toPlainString());
        accountMap.put("totalIncome", totalIncomeBig.toPlainString());
        accountMap.put("totalEffectiveIncome", totalEffectiveIncomeBig.toPlainString());
        accountMap.put("balance", balanceBig.toPlainString());
        accountMap.put("address", address);
        accountMap.put("pubKey", pubKey);
        resultMap.put("accountMap", accountMap);
        resultMap.put("transactionList", transactionList);
        resultMap.put("count", 1);
        resultMap.put("pageNumber", 1);
        res.setCode(Res.CODE_100);
        res.setData(resultMap);
        logger.info("getAccountInfo-time:{},totalIncome:{},totalExpenditure:{},totalEffectiveIncome:{},balance:{}", (t2 - t1), totalIncomeBig, totalExpenditureBig, totalEffectiveIncomeBig, balanceBig);
        return res;
    }

    /**
     * 获取区块信息
     *
     * @return
     */
    @GetMapping("getBlockInfo")
    @ResponseBody
    public Res getBlockInfo() {
        Res r = new Res();
        Map<String, Object> resultMap = new HashMap<>();
        BigDecimal avgFee = blockRepository.getFeeAvg();
        Transaction lastTransaction = initializationManager.getLastTransaction();
        TransactionHead lastTransactionHead = lastTransaction.getTransactionHead();
        long block_count = blockRepository.count();
        long trans_count = transactionRepository.count();
        Block lastBlock = initializationManager.getLastBlock();
        BlockHead lastBlockHead = lastBlock.getBlockHead();
        long block_timeDiff_s = (lastBlockHead.getTimeStamp() - GenesisBlock.GENESIS_TIME) / 1000; //s
        if (block_count == 0) {
            block_count = 1;
        }
        long block_time = block_timeDiff_s / block_count;
        resultMap.put("avgAmount", trans_count % block_count > 0 ? trans_count / block_count + 1 : trans_count / block_count); //区块平均交易数目
        resultMap.put("avgFee", initializationManager.unitConvert(avgFee, Constants.PTN, Constants.MAX_UNIT).toPlainString());
        resultMap.put("hTransCount", trans_count);
        resultMap.put("blockTime", block_time);
        r.setCode(Res.CODE_100);
        r.setData(resultMap);
        return r;
    }


    /**
     * 获取区块列表信息
     *
     * @return
     */
    @GetMapping("getBlockListInfo")
    @ResponseBody
    public Res getBlockListInfo(PageObject page) {
        Res r = new Res();
        Map<String, Object> resultMap = new HashMap<>();
        List<Map<String, Object>> blockMapList = new ArrayList<>();
        Integer pageNumber = page.getPageNumber();
        Integer pageSize = page.getPageSize();
        Long count = blockRepository.count();
        Block lastBlock = initializationManager.getLastBlock();
        long start = lastBlock.getBlockHeight() - pageNumber * pageSize;
        long end = lastBlock.getBlockHeight() - (pageNumber - 1) * pageSize;
        List<Block> blockList = blockRepository.findOneInterval(start, end);
        for (Block block : blockList) {
            double totalFee = 0;
            for (Transaction transaction : block.getBlockTransactions()) {
                if (transaction.getTransType() == 2) {
                    totalFee += transaction.getTransactionHead().getTransValue();
                }
            }
            BlockHead blockHead = block.getBlockHead();
            Map<String, Object> blockMap = new HashMap<>();
            blockMap.put("blockHeight", block.getBlockHeight());
            blockMap.put("date", DateUtil.stampToDate(blockHead.getTimeStamp()));
            blockMap.put("totalAmount", 0);
            blockMap.put("totalFee", initializationManager.unitConvert(totalFee, Constants.PTN, Constants.MAX_UNIT).toPlainString());
            blockMap.put("transactionNumber", block.getBlockTransactions().size());
            blockMap.put("foundryPublicKey", ECKey.pubkeyToAddress(Hex.toHexString(block.getFoundryPublicKey())));
            blockMap.put("blockSize", block.getBlockSize());
            //增加blockHash
            blockMap.put("blockHash", block.getBlockHash());
            blockMapList.add(blockMap);
        }
        Collections.reverse(blockMapList);
        resultMap.put("pageNumber", pageNumber);
        resultMap.put("count", count);
        resultMap.put("blockMapList", blockMapList);
        r.setCode(Res.CODE_100);
        r.setData(resultMap);
        return r;
    }

    @GetMapping("getBlockListInfoBySize")
    @ResponseBody
    public Res getBlockListInfoBySize(PageObject page, @RequestParam Integer pageSize)
    {
        Res r = new Res();
        Map resultMap = new HashMap();
        List blockMapList = new ArrayList();
        Integer pageNumber = page.getPageNumber();
        Long count = Long.valueOf(this.blockRepository.count());
        Block lastBlock = this.initializationManager.getLastBlock();
        long start = lastBlock.getBlockHeight() - pageNumber.intValue() * pageSize.intValue();
        long end = lastBlock.getBlockHeight() - (pageNumber.intValue() - 1) * pageSize.intValue();
        List<Block> blockList = this.blockRepository.findOneInterval(start, end);
        for (Block block : blockList) {
            double totalFee = 0.0D;
            for (Transaction transaction : block.getBlockTransactions()) {
                if (transaction.getTransType() == 2) {
                    totalFee += transaction.getTransactionHead().getTransValue();
                }
            }
            BlockHead blockHead = block.getBlockHead();
            Map blockMap = new HashMap();
            blockMap.put("blockHeight", Long.valueOf(block.getBlockHeight()));
            blockMap.put("date", DateUtil.stampToDate(Long.valueOf(blockHead.getTimeStamp())));
            blockMap.put("totalAmount", Integer.valueOf(0));
            blockMap.put("totalFee", this.initializationManager.unitConvert(Double.valueOf(totalFee), "KTCH", "MAX_UNIT").toPlainString());
            blockMap.put("transactionNumber", Integer.valueOf(block.getBlockTransactions().size()));
            blockMap.put("foundryPublicKey", ECKey.pubkeyToAddress(Hex.toHexString(block.getFoundryPublicKey())));
            blockMap.put("blockSize", Long.valueOf(block.getBlockSize()));

            blockMap.put("blockHash", block.getBlockHash());
            blockMapList.add(blockMap);
        }
        Collections.reverse(blockMapList);
        resultMap.put("pageNumber", pageNumber);
        resultMap.put("count", count);
        resultMap.put("blockMapList", blockMapList);
        r.setCode(100);
        r.setData(resultMap);
        return r;
    }

    @GetMapping("getBlockTransListInfo")
    @ResponseBody
    public Res getBlockTransListInfo(@RequestParam String tokenName, PageObject page, @RequestParam Integer pageSize)
    {
        Res res = new Res();
        Map resultMap = new HashMap();
        List transactionList = new ArrayList();
        long confirm = 0L;
        long blockHeight = 0L;
        long count = 0L;
        Integer pageNumber = page.getPageNumber();
        List<Transaction> transactions = null;
        transactions = this.transactionRepository.findTransaction(tokenName, new PageRequest(pageNumber.intValue() - 1, pageSize.intValue(), Sort.Direction.DESC, new String[] { "blockHeight" }));
        count = this.transactionRepository.findTransactionCount(tokenName);
        for (Transaction transaction : transactions) {
            TransactionHead transactionHead = transaction.getTransactionHead();
            blockHeight = transaction.getBlockHeight();
            Block lastBlock = this.initializationManager.getLastBlock();
            if (lastBlock != null) {
                confirm = lastBlock.getBlockHeight() - blockHeight;
            }
            Map transactionMap = new HashMap();
            transactionMap.put("date", DateUtil.stampToDate(Long.valueOf(transactionHead.getTimeStamp())));
            transactionMap.put("amount", this.initializationManager.unitConvert(Long.valueOf(transactionHead.getTransValue()), tokenName, "MAX_UNIT").toPlainString());
            transactionMap.put("fee", this.initializationManager.unitConvert(Long.valueOf(transactionHead.getFee()), "KTCH", "MAX_UNIT").toPlainString());
            transactionMap.put("blockHeight", Long.valueOf(blockHeight));
            transactionMap.put("confirm", Long.valueOf((confirm < 0L) || (blockHeight < 0L) ? 0L : confirm));
            transactionMap.put("from", ECKey.pubkeyToAddress(transactionHead.getTransFrom()));
            transactionMap.put("to", ECKey.pubkeyToAddress(transactionHead.getTransTo()));
            transactionMap.put("type", Integer.valueOf(transaction.getTransType()));

            transactionMap.put("hash", Hex.toHexString(transaction.getTransSignature()));
            transactionList.add(transactionMap);
        }
        resultMap.put("transactionList", transactionList);
        resultMap.put("count", Long.valueOf(count));
        resultMap.put("pageNumber", pageNumber);
        res.setCode(100);
        res.setData(resultMap);
        return res;
    }


    /**
     * 创建钱包账户
     *
     * @param passWord
     * @return
     * @throws IOException
     */
    @PostMapping("createAccount")
    @ResponseBody
    public Res createAccount(@RequestParam String passWord) throws IOException {
        Res res = new Res();
        Map<String, Object> resultMap = new HashMap<>();
        if (!ValidateUtil.rexCheckPassword(passWord)) {
            res.code = Res.CODE_103;
            return res;
        }
        ECKey ecKey = new ECKey(new SecureRandom());
        String privateKey = Hex.toHexString(ecKey.getPrivKeyBytes());
        String publicKey = Hex.toHexString(ecKey.getPubKey());
        String address = Constants.ADDRESS_PREFIX + Hex.toHexString(ecKey.getAddress());
        String accountPath = System.getProperty("user.home") + File.separator + "account";
        String addressPath = accountPath + File.separator + address;
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(Constants.PWD_FLAG);
        stringBuffer.append(passWord);
        stringBuffer.append(Constants.PUBKEY_FLAG);
        stringBuffer.append(publicKey);
        stringBuffer.append(Constants.PRIKEY_FLAG);
        stringBuffer.append(privateKey);
        File folder = new File(accountPath);
        if (!folder.exists()) {
            folder.mkdir();
        }
        File file = new File(addressPath);
        if (!file.exists()) {
            file.createNewFile();
        }
        FileUtil.writeFileContent(addressPath, DeEnCode.encode(stringBuffer.toString()));
        resultMap.put(Constants.PRIKEY, privateKey);
        resultMap.put(Constants.ADDRESS, address);
        resultMap.put(Constants.PUBKEY, publicKey);
        res.code = Res.CODE_100;
        res.data = resultMap;
        //更新缓存
        initializationManager.addAccountList(address, DeEnCode.encode(stringBuffer.toString()));
        //addressAndPubkey
        try {
            addressAndPubkeyRepository.save(new AddressAndPubKey(address, publicKey));
        } catch (Exception e) {
            logger.error("【addressAndPubkey-msg[{}]】", e.getMessage());
        }
        return res;
    }


    @PostMapping("validataAddressIsTrans")
    @ResponseBody
    public Res validataAddressIsTrans(@RequestParam String address, @RequestParam String tokenName) {
        Res res = new Res();
        Map<String, Object> resultMap = new HashMap<>();
        String pubkey = "";
        address = address.trim().replace(" ", "");
        AddressAndPubKey addressAndPubKey = addressAndPubkeyRepository.findByAddress(address);
        if (addressAndPubKey == null) {
            res.code = Res.CODE_201;
        } else {
            res.code = Res.CODE_202;
        }
        return res;
    }


    /**
     * 转币
     *
     * @param transFrom     ： 发送方
     * @param transTo       ：接收方
     * @param transValue    ： 金额
     * @param fee           ： 手续费
     * @param passWord      ： 密码
     * @param remark        ： 备注
     * @param transToPubkey ：接受方公钥
     * @return
     */
    @PostMapping("transferAccounts")
    @ResponseBody
    public Res transferAccounts(@RequestParam String transFrom, @RequestParam String transTo, @RequestParam String transValue, @RequestParam String fee, @RequestParam String passWord,
                                @RequestParam(defaultValue = "") String remark, @RequestParam(defaultValue = "") String transToPubkey, @RequestParam String tokenName) {
        Map intoMaps = new HashMap();
        intoMaps.put("transFrom", transFrom);
        intoMaps.put("transTo", transTo);
        intoMaps.put("transValue", transValue);
        intoMaps.put("fee", fee);
        intoMaps.put("passWord", passWord);
        intoMaps.put("remark", remark);
        intoMaps.put("transToPubkey", transToPubkey);
        intoMaps.put("tokenName", tokenName);
        logger.info("==========================into【{}】】", "transferAccounts", JSON.toJSONString(intoMaps));
        Res res = new Res();
        long webTime = DateUtil.getWebTime();
        if (webTime == Constants.GENESIS_TIME) {
            res.code = Res.CODE_500;
            return res;
        }
        Map<String, Object> resultMap = new HashMap<>();
        long effectiveIncome = 0;
        long income = 0;
        long expenditure = 0;
        long tokenDicimal = 6;
        transFrom = transFrom.trim().replace(" ", "");
        transTo = transTo.trim().replace(" ", "");
        transValue = transValue.trim().replace(" ", "");
        tokenName = tokenName.trim().replace(" ", "");
        Token token = tokenRepository.findByName(tokenName.toLowerCase());
        if (token != null) {
            tokenDicimal = token.getDecimals();
        }
        if (transValue.contains(".")) {
            String decimal = transValue.substring(transValue.indexOf(".") + 1);
            if (decimal.length() > tokenDicimal) {
                res.code = Res.CODE_128;
                return res;
            }
        }
        fee = fee.trim().replace(" ", "");
        transToPubkey = transToPubkey.trim().replace(" ", "");
        long transValueLong = initializationManager.unitConvert(transValue, tokenName, Constants.MINI_UNIT).longValue();
        BigDecimal feeDig = new BigDecimal(fee).multiply(new BigDecimal(Constants.MININUMUNIT));
        long feeLong = feeDig.longValue();
        if (feeLong <= 0) {
            res.code = Res.CODE_155;
            return res;
        }
        if (transValueLong <= 0) {
            res.code = Res.CODE_126;
            return res;
        }
        if (syncBlockManager.isSyncBlock() || syncUnconfirmedTranManager.isSyncTransaction() || syncTokenManager.isSyncToken() || syncParticipantManager.isSyncParticipant()) {
            res.code = Res.CODE_104;
            return res;
        }
        Map<String, String> localAccount = initializationManager.getAccountListByAddress(transFrom);
        String pwdFrom = localAccount.get(Constants.PWD);
        String pubkeyFrom = localAccount.get(Constants.PUBKEY);
        String prikeyFrom = localAccount.get(Constants.PRIKEY);

        if (pubkeyFrom.equals("")) {
            res.code = Res.CODE_105;
            return res;
        }

        AddressAndPubKey addressAndPubKey = addressAndPubkeyRepository.findByAddress(transTo);
        String pubkeyTo = "";
        if ((addressAndPubKey == null) && (transToPubkey == null || transToPubkey.equals(""))) {
            res.code = Res.CODE_201;
            return res;
        }
        if ((addressAndPubKey == null) && transToPubkey != null && !transToPubkey.equals("")) {
            pubkeyTo = transToPubkey;
        }
        if (addressAndPubKey != null) {
            pubkeyTo = addressAndPubKey.getPubKey();
        }

        if (!passWord.equals(pwdFrom)) {
            res.code = Res.CODE_301;
            return res;
        }

        if (!ECKey.pubkeyToAddress(transToPubkey).equals(transTo) && transToPubkey != null && !transToPubkey.equals("")) {
            res.code = Res.CODE_401;
            return res;
        }

        //TODO 计算资产
        Map<String, Long> assetsFrom = assetsManager.getAccountAssets(pubkeyFrom, tokenName);
        long balance = assetsFrom.get(Constants.BALANCE);

        if (tokenName.equalsIgnoreCase(Constants.PTN)) {
            if (balance < (transValueLong + feeLong)) {
                res.code = Res.CODE_106;
                return res;
            }
        } else {
            //token
            if (balance < transValueLong) {
                res.code = Res.CODE_106;
                return res;
            }
            //ptn
            //TODO 计算资产
            Map<String, Long> assetsFromPtn = assetsManager.getAccountAssets(pubkeyFrom, Constants.PTN);
            balance = assetsFromPtn.get(Constants.BALANCE);
            if (balance < feeLong) {
                res.code = Res.CODE_129;
                return res;
            }
        }
        UnconfirmedTran unconfirmedTran = new UnconfirmedTran(pubkeyFrom.toLowerCase(), pubkeyTo.toLowerCase(), remark, tokenName, transValueLong, feeLong, webTime, 0);
        byte[] transSignature = JSON.toJSONString(ECKey.fromPrivate(Hex.decode(prikeyFrom)).sign(SHAEncrypt.sha3(SerializationUtils.serialize(unconfirmedTran.toString())))).getBytes();
        unconfirmedTran.setTransSignature(transSignature);
        InesvMessage.Message.Builder builder = MessageManager.createMessageBuilder(RESPONSE, NEW_TRANSACTION);
        builder.setUnconfirmedTran(MessageManager.createUnconfirmedTranMessage(unconfirmedTran));
        List<String> hostList = nioSocketChannelManager.getChannelHostList();
        builder.addAllNodeAddressList(hostList);
        nioSocketChannelManager.write(builder.build());
        resultMap.put("transHash", Hex.toHexString(transSignature));
        res.code = Res.CODE_200;
        res.data = resultMap;
        //log
        Map intoMap = new HashMap();
        intoMap.put("transFrom", transFrom);
        intoMap.put("transTo", transTo);
        intoMap.put("transValue", transValue);
        intoMap.put("fee", fee);
        intoMap.put("passWord", passWord);
        intoMap.put("remark", remark);
        intoMap.put("transToPubkey", transToPubkey);
        intoMap.put("tokenName", tokenName);
        logger.info("interInface【{}】,into【{}】,out【{}】", "transferAccounts", JSON.toJSONString(intoMap), JSON.toJSONString(res));
        return res;
    }



    /**
     * 获取交易列表
     *
     * @param address ： 钱包地址
     * @return
     */
    @GetMapping("getTransactionList")
    @ResponseBody
    public Res getTransactionList(@RequestParam String address, @RequestParam String tokenName, PageObject page) {
        Res res = new Res();
        Map<String, Object> resultMap = new HashMap<>();
        List<Map<String, Object>> transactionList = new ArrayList<>();
        long confirm = 0;
        long blockHeight = 0;
        long count = 0;
        Integer pageNumber = page.getPageNumber();
        Integer pageSize = page.getPageSize();
        Iterable<Transaction> transactions = null;
        if (address.equalsIgnoreCase("all")) {
            transactions = transactionRepository.findTransaction(tokenName, new PageRequest(pageNumber - 1, pageSize, Sort.Direction.DESC, "blockHeight"));
            count = transactionRepository.findTransactionCount(tokenName);
        } else {
            Map<String, String> localAccount = initializationManager.getAccountListByAddress(address);
            String pubKey = localAccount.get(Constants.PUBKEY);
            if (pubKey.equals("")) {
                res.code = Res.CODE_102;
                return res;
            }
            transactions = transactionRepository.findAllByAccountAndTokenName(pubKey,tokenName,new PageRequest(pageNumber - 1, pageSize, Sort.Direction.DESC, "blockHeight"));
            count = transactionRepository.findAllByAccountAndTokenNameAndCount(tokenName, pubKey);
        }
        for (Transaction transaction : transactions) {
            TransactionHead transactionHead = transaction.getTransactionHead();
            blockHeight = transaction.getBlockHeight();
            Block lastBlock = initializationManager.getLastBlock();
            if (lastBlock != null) {
                confirm = lastBlock.getBlockHeight() - blockHeight;
            }
            Map<String, Object> transactionMap = new HashMap<>();
            transactionMap.put("date", DateUtil.stampToDate(transactionHead.getTimeStamp()));  // 交易时间
            transactionMap.put("amount", initializationManager.unitConvert(transactionHead.getTransValue(), tokenName, Constants.MAX_UNIT).toPlainString());  // 成交金额
            transactionMap.put("fee", initializationManager.unitConvert(transactionHead.getFee(), Constants.PTN, Constants.MAX_UNIT).toPlainString());  // 交易费用
            transactionMap.put("blockHeight", blockHeight);  // 高度
            transactionMap.put("confirm", confirm < 0 || blockHeight < 0 ? 0 : confirm);  // 确认
            transactionMap.put("from", transactionHead.getTransFrom());
            transactionMap.put("to", transactionHead.getTransTo());
            transactionMap.put("type", Integer.valueOf(transaction.getTransType()));
            transactionMap.put("hash", Hex.toHexString(transaction.getTransSignature()));
            transactionList.add(transactionMap);
        }
        resultMap.put("transactionList", transactionList);
        resultMap.put("count", count);
        resultMap.put("pageNumber", pageNumber);
        res.setCode(Res.CODE_100);
        res.setData(resultMap);
        return res;
    }

    /**
     * 获取同步区块进度
     *
     * @return
     */
    @GetMapping("getsyncBlockSchedule")
    @ResponseBody
    public Res getsyncBlockSchedule() {
        Res res = new Res();
        Map<String, Object> resultMap = new HashMap<>();
        BigDecimal blockSchedule = new BigDecimal(0);
        long needSyncBlockHeight = 0;
        long curBlockHeight = 0;
        try {
            needSyncBlockHeight = syncBlockManager.needSyncBlockHeight();
            if (needSyncBlockHeight == 0) {
                needSyncBlockHeight = syncBlockManager.getSyncBlockSchedule();
            } else {
                syncBlockManager.setSyncBlockSchedule(needSyncBlockHeight);
            }
            curBlockHeight = initializationManager.getBlockHeight();
            blockSchedule = new BigDecimal((double) curBlockHeight / needSyncBlockHeight);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        logger.info("needSyncBlockHeight【{}】，curBlockHeight【{}】,blockSchedule:【{}】", needSyncBlockHeight, curBlockHeight, blockSchedule);
        blockSchedule.setScale(2, BigDecimal.ROUND_HALF_UP);
        if (blockSchedule.compareTo(new BigDecimal(1)) == 1) {
            blockSchedule = new BigDecimal(1);
        }
        resultMap.put("blockSchedule", blockSchedule.toPlainString());
        res.setCode(Res.CODE_100);
        res.setData(resultMap);
        return res;
    }


    /**
     * 导出
     *
     * @return
     */
    @PostMapping("exportWallet")
    @ResponseBody
    public Res exportWallet(@RequestParam String address, @RequestParam String passWord, @RequestParam String mnemonic) {
        Res res = new Res();
        Map<String, Object> resultMap = new HashMap<>();
        Map<String, Object> jsonMap = new HashMap<>();
        String SHAEncrypTmnemonic = Hex.toHexString(SHAEncrypt.sha3(mnemonic.getBytes()));
        Map<String, String> accountList = initializationManager.getAccountList();
        String account = accountList.get(address);
        if (account == null || "".equals(account)) {
            res.setCode(Res.CODE_102);
            return res;
        }
        String savaPwd = initializationManager.getAccountListByAddress(address).get(Constants.PWD);
        if (!passWord.equals(savaPwd)) {
            res.setCode(Res.CODE_301);
            return res;
        }
        jsonMap.put("mnemonic", SHAEncrypTmnemonic);
        jsonMap.put("account", account);
        resultMap.put("jsonStr", jsonMap);
        res.setCode(Res.CODE_107);
        res.setData(resultMap);
        //log
        Map intoMap = new HashMap();
        intoMap.put("address", address);
        intoMap.put("passWord", passWord);
        intoMap.put("mnemonic", mnemonic);
        logger.info("interInface【{}】,into【{}】,out【{}】", "exportWallet", JSON.toJSONString(intoMap), JSON.toJSONString(res));
        return res;

    }

    /**
     * 导入
     *
     * @return
     */
    @PostMapping("importWallet")
    @ResponseBody
    public Res importWallet(@RequestParam String mnemonicText, @RequestParam String jsonStr) {
        Res res = new Res();
        Map<String, Object> resultMap = new HashMap<>();
        Map<String, Object> jsonMap = new HashMap<>();
        String SHAEncrypTmnemonic = Hex.toHexString(SHAEncrypt.sha3(mnemonicText.getBytes()));
        try {
            jsonMap = JSON.parseObject(jsonStr, Map.class);
            String savaMnemonic = jsonMap.get("mnemonic").toString();
            String accountEncode = jsonMap.get("account").toString();
            if (!savaMnemonic.equals(SHAEncrypTmnemonic)) {
                res.setCode(Res.CODE_108);
                return res;
            }
            String account = DeEnCode.decode(accountEncode);
            String pubkey = account.substring(account.indexOf(Constants.PUBKEY_FLAG) + Constants.PUBKEY_FLAG.length(), account.indexOf(Constants.PRIKEY_FLAG));
            String address = ECKey.pubkeyToAddress(pubkey);
            Map<String, String> localAccount = initializationManager.getAccountListByAddress(address);
            if (!localAccount.get(Constants.PUBKEY).equals("")) {
                resultMap.put(Constants.ADDRESS, address);
                res.setCode(Res.CODE_109);
                res.setData(resultMap);
                return res;
            }

            String accountPath = System.getProperty("user.home") +File.separator  +"account";
            String addressPath = System.getProperty("user.home") + File.separator+"account"+ File.separator+ address;
            File dir = new File(accountPath);
            if (!dir.exists()) {
                dir.mkdir();
            }
            File file = new File(addressPath);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileUtil.writeFileContent(addressPath, accountEncode);
            //update local Account
            initializationManager.addAccountList(address, accountEncode);
            resultMap.put(Constants.ADDRESS, address);
            res.setCode(Res.CODE_110);
            res.setData(resultMap);
            //addressAndPubkey
            try {
                addressAndPubkeyRepository.save(new AddressAndPubKey(address, pubkey));
            } catch (Exception e) {
                logger.error("【addressAndPubkey-msg[{}]】", e.getMessage());
            }
            //log
            Map intoMap = new HashMap();
            intoMap.put("mnemonicText", mnemonicText);
            intoMap.put("jsonStr", jsonStr);
            logger.info("interInface【{}】,into【{}】,out【{}】", "importWallet", JSON.toJSONString(intoMap), JSON.toJSONString(res));
            return res;
        } catch (Exception e) {
            res.setCode(Res.CODE_111);
            e.printStackTrace();
            return res;
        }

    }


    @PostMapping("getTransactionByHash")
    @ResponseBody
    public Res getTransactionByHash(@RequestParam String hash) {
        Res res = new Res();
        Map<String, Object> resultMap = new HashMap<>();
        List<Transaction> transactions = new ArrayList<>();
        List<Map<String, Object>> transactionList = new ArrayList<>();
        long confirm = 0;
        long blockHeight = 0;
        try {
            transactions = transactionRepository.findAllByTransSignature(Hex.decode(hash));
            for (Transaction transaction : transactions) {
                TransactionHead transactionHead = transaction.getTransactionHead();
                blockHeight = transaction.getBlockHeight();
                Block lastBlock = initializationManager.getLastBlock();
                if (lastBlock != null) {
                    confirm = lastBlock.getBlockHeight() - blockHeight;
                }
                Map<String, Object> transactionMap = new HashMap<>();

                if (transaction.getTransType() == 3 && transaction.getContractType() == 1) {
                    Map exchangeInfoMap = this.getExchangeInfoByBin(transaction.getContractBin());
                    transactionMap.put("address", ECKey.pubkeyToAddress(transaction.getTransFrom()));
                    transactionMap.put("contractAddress", transaction.getContractAddress());
                    transactionMap.put("contractState", transaction.getContractState());
                    transactionMap.put("date", DateUtil.stampToDate(transaction.getLockTime()));
                    transactionMap.put("fromCoinName", exchangeInfoMap.get("fromCoinName"));
                    transactionMap.put("fromVal", exchangeInfoMap.get("fromVal"));
                    transactionMap.put("toCoinName", exchangeInfoMap.get("toCoinName"));
                    transactionMap.put("toVal", exchangeInfoMap.get("toVal"));
                    transactionMap.put("transType", transaction.getTransType());
                    transactionMap.put("contractType", transaction.getContractType());
                } else {
                    transactionMap.put("coinType", transaction.getTokenName());
                    transactionMap.put("date", DateUtil.stampToDate(transactionHead.getTimeStamp()));
                    transactionMap.put("from", ECKey.pubkeyToAddress(transactionHead.getTransFrom()));
                    transactionMap.put("to", ECKey.pubkeyToAddress(transactionHead.getTransTo()));
                    transactionMap.put("amount", initializationManager.unitConvert(transactionHead.getTransValue(), transaction.getTokenName(), Constants.MAX_UNIT));
                    transactionMap.put("blockHeight", blockHeight);
                    transactionMap.put("txHash", Hex.toHexString(transaction.getTransSignature()));
                    transactionMap.put("tokenName", transaction.getTokenName());
                    transactionMap.put("value", initializationManager.unitConvert(transaction.getTransactionHead().getTransValue(), transaction.getTokenName(), Constants.MAX_UNIT));
                    transactionMap.put("fee", initializationManager.unitConvert(transaction.getTransactionHead().getFee(), Constants.PTN, Constants.MAX_UNIT));
                    transactionMap.put("date", DateUtil.stampToDate(transaction.getTransactionHead().getTimeStamp()));
                    transactionMap.put("confirm", initializationManager.getLastBlock().getBlockHeight() - transaction.getBlockHeight() < 0 ? 0 : initializationManager.getLastBlock().getBlockHeight() - transaction.getBlockHeight());
                    transactionMap.put("remark", transaction.getRemark());
                    transactionMap.put("transType", transaction.getTransType());
                    transactionMap.put("contractType", transaction.getContractType());
                }

                transactionList.add(transactionMap);
            }
            resultMap.put("transactions", transactionList);
            res.code = Res.CODE_100;
            res.data = resultMap;
        } catch (Exception e) {
            e.printStackTrace();
            res.code = Res.CODE_114;
        }
        return res;
    }

    @GetMapping("getTokenList")
    @ResponseBody
    public Res getTokenList() {
        Res res = new Res();
        String fileContent = "";
        List tokenList = new ArrayList();
        String accountTokenInfoPath = System.getProperty("user.home") + File.separator + "account" + File.separator + "accountTokenInfo";
        File file = new File(accountTokenInfoPath);
        try {
            if (file.exists()) {
                for (int i = 0; i < FileUtil.readFileByLines(accountTokenInfoPath).size(); i++) {
                    fileContent += FileUtil.readFileByLines(accountTokenInfoPath).get(i);
                }
                tokenList = JSON.parseObject(fileContent, List.class);
            }
        } catch (Exception e) {
            tokenList = new ArrayList();
        }
        Collections.reverse(tokenList);
        res.setCode(100);
        res.setData(tokenList);
        return res;
    }

    @GetMapping("getTokenListByAddress")
    @ResponseBody
    public Res getTokenListByAddress(@RequestParam String address) {
        return this.getTokenList();
    }


    @GetMapping("tokenOpenAndClose")
    @ResponseBody
    public Res tokenOpenAndClose(@RequestParam int flag, @RequestParam String tokenName) {
        Res res = new Res();
        String fileContent = "";
        try {
            String accountTokenInfoPath = System.getProperty("user.home") + File.separator + "account" + File.separator + "accountTokenInfo";
            File file = new File(accountTokenInfoPath);
            if (file.exists()) {
                for (int i = 0; i < FileUtil.readFileByLines(accountTokenInfoPath).size(); i++) {
                    fileContent += FileUtil.readFileByLines(accountTokenInfoPath).get(i);
                }
            }
            if (fileContent.equals("")) {
                if (flag == 0) {
                } else {
                    List tokenList = new ArrayList();
                    tokenList.add(tokenName);
                    FileUtil.writeFileContent(accountTokenInfoPath, JSON.toJSONString(tokenList));
                }
            } else {
                List tokenList = JSON.parseObject(fileContent, List.class);
                if (flag == 0) {
                    tokenList.remove(tokenName);
                } else {
                    tokenList.add(tokenName);
                }
                file.delete();
                file.createNewFile();
                fileContent = JSON.toJSONString(tokenList);
                FileUtil.writeFileContent(accountTokenInfoPath, fileContent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        res.setCode(Res.CODE_122);
        return res;
    }


    @GetMapping("tokenOpenAndCloseList")
    @ResponseBody
    public Res tokenOpenAndCloseList() {
        List tokenInfoList = new ArrayList();
        String fileContent = "";
        Set tokenList = new HashSet();
        String accountTokenInfoPath = System.getProperty("user.home") + File.separator + "account" + File.separator + "accountTokenInfo";
        File file = new File(accountTokenInfoPath);
        try {
            if (file.exists()) {
                for (int i = 0; i < FileUtil.readFileByLines(accountTokenInfoPath).size(); i++) {
                    fileContent += FileUtil.readFileByLines(accountTokenInfoPath).get(i);
                }
                try {
                    tokenList = JSON.parseObject(fileContent, Set.class);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Res res = new Res();
        Iterator tokens = tokenRepository.findAll().iterator();
        while (tokens.hasNext()) {
            Token token = (Token) tokens.next();
            Map tokenMap = new HashMap();
            tokenMap.put("tokenName", token.getName());
            tokenMap.put("isOpen", tokenList.contains(token.getName()) ? 1 : 0);
            tokenInfoList.add(tokenMap);
        }
        res.setCode(Res.CODE_122);
        res.setData(tokenInfoList);
        return res;
    }

    @GetMapping("tokenOpenAndCloseListFuzzy")
    @ResponseBody
    public Res tokenOpenAndCloseListFuzzy(@RequestParam("tokenStr") String tokenStr) {
        List tokenInfoList = new ArrayList();
        String fileContent = "";
        List tokenList = new ArrayList();
        String accountTokenInfoPath = System.getProperty("user.home") + File.separator + "account" + File.separator + "accountTokenInfo";
        File file = new File(accountTokenInfoPath);
        try {
            if (file.exists()) {
                for (int i = 0; i < FileUtil.readFileByLines(accountTokenInfoPath).size(); i++) {
                    fileContent += FileUtil.readFileByLines(accountTokenInfoPath).get(i);
                }
                try {
                    tokenList = JSON.parseObject(fileContent, List.class);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Res res = new Res();
        Iterator tokens = tokenRepository.findAll().iterator();
        while (tokens.hasNext()) {
            Map tokenMap = new HashMap();
            Token token = (Token) tokens.next();
            if (token.getName().toLowerCase().contains(tokenStr.toLowerCase())) {
                tokenMap.put("tokenName", token.getName());
                tokenMap.put("isOpen", tokenList.contains(token.getName()) ? 1 : 0);
                tokenInfoList.add(tokenMap);
            }
        }
        res.setCode(Res.CODE_122);
        res.setData(tokenInfoList);
        return res;
    }

    @GetMapping("getBlockInfoByBlockHeight")
    @ResponseBody
    public Block getBlockInfoByBlockHeight(@RequestParam long blockHeight) {
        Block block = blockRepository.findBlockByBlockId(blockHeight);
        if (block == null) {
            return null;
        }
        byte[] blockSHA = SHAEncrypt.SHA256(block.getBlockHead());
        block.setBlockHash(Hex.toHexString(blockSHA));
        return block;
    }

    @GetMapping("getBlockInfoByBlockHeightDealWithData")
    @ResponseBody
    public Res getBlockInfoByBlockHeightDealWithData(@RequestParam long blockHeight) {
        Res res = new Res();
        LinkedHashMap resultMap = new LinkedHashMap();
        Map blockMap = new HashMap();
        List<Map> transactionListMap = new ArrayList<>();
        int type = 0;
        double totalFee = 0;
        Map<String, BigDecimal> tokenTotalAmount = new HashMap<>();
        try {
            Block block = blockRepository.findBlockByBlockId(blockHeight);
            byte[] blockSHA = SHAEncrypt.SHA256(block.getBlockHead());
            for (Transaction transaction : block.getBlockTransactions()) {
                String tokenName = transaction.getTokenName();
                long value = transaction.getTransactionHead().getTransValue();
                long fee = transaction.getTransactionHead().getFee();
                if (transaction.getTransType() == 2) {
                    totalFee += transaction.getTransactionHead().getTransValue();
                }
                Map transactionMap = new HashMap();
                long confirm = initializationManager.getLastBlock().getBlockHeight() - transaction.getBlockHeight() < 0 ? 0 : initializationManager.getLastBlock().getBlockHeight() - transaction.getBlockHeight();
                String date = DateUtil.stampToDate(transaction.getTransactionHead().getTimeStamp());
                switch (transaction.getTransType()) {
                    case 0:
                    case 1:
                    case 2:
                        transactionMap.put("tokenName", transaction.getTokenName());
                        transactionMap.put("from", ECKey.pubkeyToAddress(transaction.getTransFrom()));
                        transactionMap.put("to", ECKey.pubkeyToAddress(transaction.getTransTo()));
                        transactionMap.put("value", initializationManager.unitConvert(transaction.getTransactionHead().getTransValue(), transaction.getTokenName(), Constants.MAX_UNIT).toPlainString());
                        transactionMap.put("fee", initializationManager.unitConvert(transaction.getTransactionHead().getFee(), Constants.PTN, Constants.MAX_UNIT).toPlainString());
                        transactionMap.put("date", date);
                        transactionMap.put("transType", transaction.getTransType());
                        transactionMap.put("contractType", transaction.getContractType());
                        transactionMap.put("confirm", confirm);
                        transactionMap.put("remark", transaction.getRemark());
                        transactionMap.put("txHash", Hex.toHexString(transaction.getTransSignature()));
                        break;
                    case 3:
                        transactionMap.put("transType", transaction.getTransType());
                        transactionMap.put("contractType", transaction.getContractType());
                        transactionMap.put("confirm", confirm);
                        transactionMap.put("date", date);
                        transactionMap.put("from", ECKey.pubkeyToAddress(transaction.getTransFrom()));
                        transactionMap.put("contractAddress", transaction.getContractAddress());
                        break;
                    case 4:
                        transactionMap.put("transType", transaction.getTransType());
                        transactionMap.put("contractType", transaction.getContractType());
                        transactionMap.put("confirm", confirm);
                        transactionMap.put("date", date);
                        transactionMap.put("from", ECKey.pubkeyToAddress(transaction.getTransFrom()));
                        transactionMap.put("contractAddress", transaction.getContractAddress());
                        transactionMap.put("value", initializationManager.unitConvert(transaction.getTransactionHead().getTransValue(), transaction.getTokenName(), Constants.MAX_UNIT).toPlainString());
                        break;
                    case 5:
                        transactionMap.put("transType", transaction.getTransType());
                        transactionMap.put("contractType", transaction.getContractType());
                        transactionMap.put("confirm", confirm);
                        transactionMap.put("date", date);
                        transactionMap.put("from", ECKey.pubkeyToAddress(transaction.getTransFrom()));
                        transactionMap.put("contractAddress", transaction.getContractAddress());
                        break;
                    case 6:
                        transactionMap.put("transType", transaction.getTransType());
                        transactionMap.put("contractType", transaction.getContractType());
                        transactionMap.put("confirm", confirm);
                        transactionMap.put("date", date);
                        transactionMap.put("from", ECKey.pubkeyToAddress(transaction.getTransFrom()));
                        transactionMap.put("contractAddress", transaction.getContractAddress());
                        break;
                    case 7:
                        String topic = "";
                        Map binMap = ContractUtil.analisysVotesContract(transaction.getContractBin());
                        if (binMap != null) {
                            topic = binMap.get("topic").toString();
                        }
                        transactionMap.put("transType", transaction.getTransType());
                        transactionMap.put("contractType", transaction.getContractType());
                        transactionMap.put("confirm", confirm);
                        transactionMap.put("date", date);
                        transactionMap.put("from", ECKey.pubkeyToAddress(transaction.getTransFrom()));
                        transactionMap.put("topic", topic);
                        transactionMap.put("item", transaction.getRemark());
                        transactionMap.put("contractAddress", transaction.getContractAddress());
                        break;
                }
                transactionListMap.add(transactionMap);
            }
            blockMap.put("blockHeight", block.getBlockHeight());
            blockMap.put("blockSize", block.getBlockSize());
            blockMap.put("tokenTotalAmount", tokenTotalAmount);
            blockMap.put("totalFee", initializationManager.unitConvert(totalFee, Constants.PTN, Constants.MAX_UNIT));
            blockMap.put("blockSignature", block.getBlockSignature());
            blockMap.put("foundryPublicKey", ECKey.pubkeyToAddress(Hex.toHexString(block.getFoundryPublicKey())));
            blockMap.put("date", DateUtil.stampToDate(block.getBlockHead().getTimeStamp()));
            blockMap.put("hashPrevBlock", Hex.toHexString(block.getBlockHead().getHashPrevBlock()));
            blockMap.put("hashBlock", Hex.toHexString(blockSHA));
            blockMap.put("blockTransactions", transactionListMap);
            resultMap.put("block", blockMap);
        } catch (Exception e) {
            res.setCode(Res.CODE_101);
            res.setData(resultMap);
            return res;
        }
        res.setCode(Res.CODE_100);
        res.setData(resultMap);
        return res;
    }


    @GetMapping("getCurNodeBlockHeight")
    @ResponseBody
    public long getCurNodeBlockHeight() {
        Block block = initializationManager.getLastBlock();
        return block.getBlockHeight();
    }

    /**
     * 获取账户信息(地址+交易列表)
     *
     * @param ieType:   收支类型：0支出，1收入
     * @param tokenName ： 币种
     * @return
     */
    @GetMapping("getTransactionByPubkey")
    @ResponseBody
    public Res getTransactionByPubkey(@RequestParam Integer ieType, @RequestParam String pubKey, @RequestParam String tokenName, PageObject pageObject) {
        Res res = new Res();
        long blockHeight = 0;
        long confirm = 0;
        long type = 0;
        int pageNumber = pageObject.getPageNumber();
        int pageSize = pageObject.getPageSize();
        Map<String, Object> resultMap = new HashMap<>();
        Map<String, Object> accountMap = new HashMap<>();
        List<Map<String, Object>> transactionList = new ArrayList<>();
        int count = 0;
        long start = 0;
        List<Transaction> transactions = null;
        if (ieType == 0) {//支出
            count = transactionRepository.findTransByTransFromCount(pubKey, tokenName);
            start = count - pageNumber * pageSize;
            start = start < 0 ? 0 : start;
            pageObject.setSumRecord(count);
            transactions = transactionRepository.findTransByTransFrom(pubKey, tokenName, start, pageObject.getPageSize());
        } else if (ieType == 1) {//收入
            count = transactionRepository.findTransByTransToCount(pubKey, tokenName);
            start = count - pageNumber * pageSize;
            start = start < 0 ? 0 : start;
            pageObject.setSumRecord(count);
            transactions = transactionRepository.findTransByTransTo(pubKey, tokenName, start, pageObject.getPageSize());
        }
        Block lastBlock = initializationManager.getLastBlock();
        for (Transaction transaction : transactions) {
            if (transaction.getTransType() == 1 && ieType == 0) {
                continue;
            }
            blockHeight = transaction.getBlockHeight();
            if (lastBlock != null) {
                confirm = lastBlock.getBlockHeight() - blockHeight;
            }
            Map<String, Object> transactionMap = new HashMap<>();
            transactionMap.put("date", DateUtil.stampToDate(transaction.getTransactionHead().getTimeStamp()));  // 交易时间
            transactionMap.put("type", transaction.getTransType());  // 交易类型
            transactionMap.put("coinType", transaction.getTokenName());
            transactionMap.put("amount", initializationManager.unitConvert(transaction.getTransactionHead().getTransValue(), transaction.getTokenName(), Constants.MAX_UNIT).toPlainString());  // 成交金额
            transactionMap.put("fee", initializationManager.unitConvert(transaction.getTransactionHead().getFee(), Constants.PTN, Constants.MAX_UNIT).toPlainString());  // 交易费用
            transactionMap.put("blockHeight", blockHeight);  // 高度
            transactionMap.put("confirm", confirm < 0 || blockHeight < 0 ? 0 : confirm);  // 确认
            transactionMap.put("hash", transaction.getTransSignature());
            transactionMap.put("from", ECKey.pubkeyToAddress(transaction.getTransFrom()));
            transactionMap.put("to", ECKey.pubkeyToAddress(transaction.getTransTo()));
            transactionList.add(transactionMap);
        }
        Collections.reverse(transactionList);
        resultMap.put("transactionList", transactionList);
        resultMap.put("count", count);
        resultMap.put("pageNumber", pageObject.getPageNumber());
        res.setCode(Res.CODE_100);
        res.setData(resultMap);
        return res;
    }

    /*
    * 获取节点状态
    * 0同步中1节点连接2节点断开
    * */
    @GetMapping("getNodeState")
    @ResponseBody
    public Res getNodeState() {
        Res res = new Res();
        Map resultMap = new HashMap();
        if (nioSocketChannelManager.getActiveNioSocketChannelCount() >= Constants.ACTIVE_NODE_COUNT) {
            logger.info("###########---syncBlockManager.isSyncBlock():" + syncBlockManager.isSyncBlock());
            logger.info("###########---syncUnconfirmedTranManager.isSyncTransaction():" + syncUnconfirmedTranManager.isSyncTransaction());
            logger.info("###########--- syncTokenManager.isSyncToken():" + syncTokenManager.isSyncToken());
            if (syncBlockManager.isSyncBlock() || syncUnconfirmedTranManager.isSyncTransaction() || syncTokenManager.isSyncToken() || syncParticipantManager.isSyncParticipant()) {
                resultMap.put("nodeState", 0);
            } else {
                resultMap.put("nodeState", 1);
            }
        } else {
            resultMap.put("nodeState", 2);
        }
        res.setCode(Res.CODE_100);
        res.setData(resultMap);
        return res;
    }

    @GetMapping("syncData")
    @ResponseBody
    public Res syncData(@RequestParam String transFrom, @RequestParam String passWord) {
        Res res = new Res();
        Map<String, String> localAccount = initializationManager.getAccountListByAddress(transFrom);
        String pwdFrom = localAccount.get(Constants.PWD);
        String pubkeyFrom = localAccount.get(Constants.PUBKEY);

        if (pubkeyFrom.equals("")) {
            res.code = Res.CODE_105;
            return res;
        }
        if (!passWord.equals(pwdFrom)) {
            res.code = Res.CODE_301;
            return res;
        } else {
            nioSocketChannelManager.removeInvalidChannel();
            peerClient.init();
            resetData.resetAssets();
            res.setCode(Res.CODE_122);
            return res;
        }
    }


    /**
     * 走势图 获取一个星期的交易量
     */
    @PostMapping("getTheTrend")
    @ResponseBody
    public Res getTheTrend() {
        Res res = new Res();
        Long nowTime = DateUtil.getWebTime();//获取到现在的时间
        Long halfYearhours = 4320 * 3600000l;//半年时间的时间搓
        Long halfYearTime = nowTime - halfYearhours;//半年前的时间搓

        //String path = System.getProperty("user.home") + "\\account\\" + "count.txt";
        String path = "./photon_chain/" + "count.txt";
        File file = new File(path);
        if (!file.exists()) {
            res.setCode(Res.CODE_100);
            return res;
        } else {
            String data = FileUtil.readToString(path);
            if (!data.isEmpty() && !"".equals(data)) {
                data = StringUtils.substringBefore(data.replace("\r\n", ""), "||");
                data = data.replace("[", "").replace("]", "").replace("\"datas\":", "");
                String[] dataArr = data.split(",");//将数据转成数组
                StringBuffer sb = new StringBuffer();
                sb.append("\"datas\":[");
                for (int i = 0; i < dataArr.length; i++) {
                    if (i % 2 == 0) {//取出数据的时间搓
                        if (Long.valueOf(dataArr[i]) > halfYearTime) {//最近半年的数据获得
                            sb.append("[");
                            sb.append(dataArr[i]);
                            sb.append(",");
                            sb.append(dataArr[i + 1]);
                            sb.append("]");
                            sb.append(",");
                        }
                    }
                }
                String returnData = StringUtils.substringBeforeLast(sb.toString(), ",");
                returnData = returnData + "]";

                res.setCode(Res.CODE_100);
                res.setData("{" + returnData + "}");
                return res;
            } else {
                res.setCode(Res.CODE_100);
                return res;
            }
        }
    }


    private Map<Object, Object> getExchangeInfoByBin(@RequestParam String bin) {
        Map<Object, Object> resultMap = new HashMap<Object, Object>();
        Program program = new Program();
        program.setBin(bin);
        program.analysisAndPushHeap();
        Heap heap = program.getHeap();
        Object a = heap.getItem("fromCoin") == null ? "a" : heap.getItem("fromCoin");
        Object b = heap.getItem("toCoin") == null ? "b" : heap.getItem("toCoin");
        Long fromVal = Long.parseLong(heap.getItem(a.toString()).toString());
        String fromCoinName = heap.getItem(a + "Type").toString();
        Long toVal = Long.parseLong(heap.getItem(b.toString()).toString());
        String toCoinName = heap.getItem(b + "Type").toString();
        resultMap.put("fromCoinName", fromCoinName);
        resultMap.put("fromVal", fromVal);
        resultMap.put("toCoinName", toCoinName);
        resultMap.put("toVal", toVal);
        return resultMap;
    }


    public static void main(String[] args) {
        String fee ="-1";
        BigDecimal feeDig = new BigDecimal(fee).multiply(new BigDecimal(Constants.MININUMUNIT));
        long feeLong = feeDig.longValue();

    }

}
