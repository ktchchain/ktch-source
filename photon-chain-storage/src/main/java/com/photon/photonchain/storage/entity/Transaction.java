package com.photon.photonchain.storage.entity;

import org.spongycastle.util.encoders.Hex;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Arrays;

/**
 * @Author:lqh
 * @Description:
 * @Date:14:00 2018/01/09
 * @Modified by:
 */
@Entity
@Table(name = "Transaction", indexes = {@Index(name = "index_blockHeight", columnList = "blockHeight"), @Index(name = "idx_transFrom", columnList = "transFrom"), @Index(name = "idx_transTo", columnList = "transTo"), @Index(name = "idx_tokenName", columnList = "tokenName"),
        @Index(name = "idx_contractType", columnList = "contractType"), @Index(name = "idx_transType", columnList = "transType")})
public class Transaction implements Comparable<Transaction>, Serializable {
    @Id
    private byte[] transSignature;
    @Column(columnDefinition = "TEXT")
    private TransactionHead transactionHead;

    private long blockHeight;

    private long lockTime;

    private String transFrom;

    private String transTo;

    private String remark;

    private String tokenName;

    private int transType;//0交易流水 1 生成代币 2挖矿流水 3合约流水 4 合约资金流水 5.兑换流水 6.取消挂单 7.投票选项

    @Column(columnDefinition = "TEXT")
    private String contractBin;

    private String contractAddress;

    private int contractType;//0 非合约 1挂单合约 2.投票合约 3.代币合约 4Java合约

    private int contractState;//0未完结 1完结 2取消

    private String attrOne;

    private String attrTwo;

    private String attrThree;

    private String exchengeToken;//兑换币种

    private long transValue;//统计资产用

    private long fee;//统计资产用

    public String getExchengeToken() {
        return exchengeToken;
    }

    public void setExchengeToken(String exchengeToken) {
        this.exchengeToken = exchengeToken;
    }

    public Transaction() {
    }

    public Transaction(byte[] transSignature, TransactionHead transactionHead, long blockHeight, long lockTime, String transFrom, String transTo, String remark, String tokenName, int transType, long transValue, long fee) {
        this.transSignature = transSignature;
        this.transactionHead = transactionHead;
        this.blockHeight = blockHeight;
        this.lockTime = lockTime;
        this.transFrom = transFrom;
        this.transTo = transTo;
        this.remark = remark;
        this.tokenName = tokenName;
        this.transType = transType;
        this.contractAddress = "";
        this.contractBin = "";
        this.contractType = 0;
        this.contractState = -1;
        this.transValue = transValue;
        this.fee = fee;
    }

    public Transaction(byte[] transSignature, TransactionHead transactionHead, long blockHeight
            , long lockTime, String transFrom, String transTo, String remark, String tokenName, int transType
            , String contractAddress, String contractBin, int contractType, int contractState, String exchengeToken, long transValue, long fee) {
        this.transSignature = transSignature;
        this.transactionHead = transactionHead;
        this.blockHeight = blockHeight;
        this.lockTime = lockTime;
        this.transFrom = transFrom;
        this.transTo = transTo;
        this.remark = remark;
        this.tokenName = tokenName;
        this.transType = transType;
        this.contractAddress = contractAddress;
        this.contractBin = contractBin;
        this.contractType = contractType;
        this.contractState = contractState;
        this.exchengeToken = exchengeToken;
        this.transValue = transValue;
        this.fee = fee;
    }

    public byte[] getTransSignature() {
        return transSignature;
    }

    public void setTransSignature(byte[] transSignature) {
        this.transSignature = transSignature;
    }

    public TransactionHead getTransactionHead() {
        return transactionHead;
    }

    public void setTransactionHead(TransactionHead transactionHead) {
        this.transactionHead = transactionHead;
    }

    public long getBlockHeight() {
        return blockHeight;
    }

    public void setBlockHeight(long blockHeight) {
        this.blockHeight = blockHeight;
    }

    public long getLockTime() {
        return lockTime;
    }

    public void setLockTime(long lockTime) {
        this.lockTime = lockTime;
    }

    public String getTransFrom() {
        return transFrom;
    }

    public void setTransFrom(String transFrom) {
        this.transFrom = transFrom;
    }

    public String getTransTo() {
        return transTo;
    }

    public void setTransTo(String transTo) {
        this.transTo = transTo;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getTokenName() {
        return tokenName;
    }

    public void setTokenName(String tokenName) {
        this.tokenName = tokenName;
    }

    public int getTransType() {
        return transType;
    }

    public void setTransType(int transType) {
        this.transType = transType;
    }

    public String getContractBin() {
        return contractBin;
    }

    public void setContractBin(String contractBin) {
        this.contractBin = contractBin;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public void setContractAddress(String contractAddress) {
        this.contractAddress = contractAddress;
    }

    public int getContractType() {
        return contractType;
    }

    public void setContractType(int contractType) {
        this.contractType = contractType;
    }

    public int getContractState() {
        return contractState;
    }

    public void setContractState(int contractState) {
        this.contractState = contractState;
    }

    public String getAttrOne() {
        return attrOne;
    }

    public void setAttrOne(String attrOne) {
        this.attrOne = attrOne;
    }

    public String getAttrTwo() {
        return attrTwo;
    }

    public void setAttrTwo(String attrTwo) {
        this.attrTwo = attrTwo;
    }

    public String getAttrThree() {
        return attrThree;
    }

    public void setAttrThree(String attrThree) {
        this.attrThree = attrThree;
    }

    public long getTransValue() {
        return transValue;
    }

    public long getFee() {
        return fee;
    }

    public void setTransValue(long transValue) {
        this.transValue = transValue;
    }

    public void setFee(long fee) {
        this.fee = fee;
    }

    @Override
    public boolean equals(Object o) {
        if ( this == o ) return true;
        if ( o == null || getClass ( ) != o.getClass ( ) ) return false;

        Transaction that = (Transaction) o;

        if ( blockHeight != that.blockHeight ) return false;
        if ( lockTime != that.lockTime ) return false;
        if ( transType != that.transType ) return false;
        if ( !Arrays.equals ( transSignature, that.transSignature ) ) return false;
        if ( transactionHead != null ? !transactionHead.equals ( that.transactionHead ) : that.transactionHead != null )
            return false;
        if ( transFrom != null ? !transFrom.equals ( that.transFrom ) : that.transFrom != null ) return false;
        if ( transTo != null ? !transTo.equals ( that.transTo ) : that.transTo != null ) return false;
        if ( remark != null ? !remark.equals ( that.remark ) : that.remark != null ) return false;
        return tokenName != null ? tokenName.equals ( that.tokenName ) : that.tokenName == null;
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode ( transSignature );
        result = 31 * result + (transactionHead != null ? transactionHead.hashCode ( ) : 0);
        result = 31 * result + (int) (blockHeight ^ (blockHeight >>> 32));
        result = 31 * result + (int) (lockTime ^ (lockTime >>> 32));
        result = 31 * result + (transFrom != null ? transFrom.hashCode ( ) : 0);
        result = 31 * result + (transTo != null ? transTo.hashCode ( ) : 0);
        result = 31 * result + (remark != null ? remark.hashCode ( ) : 0);
        result = 31 * result + (tokenName != null ? tokenName.hashCode ( ) : 0);
        result = 31 * result + transType;
        return result;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "transSignature=" + Hex.toHexString ( transSignature ) +
                ", transactionHead=" + transactionHead +
                ", blockHeight=" + blockHeight +
                ", lockTime=" + lockTime +
                ", transFrom='" + transFrom + '\'' +
                ", transTo='" + transTo + '\'' +
                ", remark='" + remark + '\'' +
                ", tokenName='" + tokenName + '\'' +
                ", transType=" + transType +
                ", contractAddress=" + contractAddress +
                ", contractBin=" + contractBin +
                ", contractType=" + contractType +
                '}';
    }

    public String toSignature() {
        return "UnconfirmedTran{" +
                "transFrom='" + transFrom + '\'' +
                ", transTo='" + transTo + '\'' +
                ", remark='" + remark + '\'' +
                ", tokenName='" + tokenName + '\'' +
                ", transValue=" + transactionHead.getTransValue ( ) +
                ", fee=" + transactionHead.getFee ( ) +
                ", timeStamp=" + transactionHead.getTimeStamp ( ) +
                ", transType=" + transType +
                ", contractAddress=" + contractAddress +
                ", contractBin=" + contractBin +
                ", contractType=" + contractType +
                '}';
    }

    public UnconfirmedTran getUnconfirmedTran() {
        return new UnconfirmedTran ( transFrom, transTo, remark, tokenName, transactionHead.getTransValue ( ), transactionHead.getFee ( ), transactionHead.getTimeStamp ( ), transType, contractAddress, contractBin, contractType, contractState, exchengeToken );
    }

    @Override
    public int compareTo(Transaction o) {
        return (int) (o.getTransactionHead ( ).getTimeStamp ( ) - this.getTransactionHead ( ).getTimeStamp ( ));
    }
}
