package com.photon.photonchain.interfaces.utils;

/**
 * @Author:lqh
 * @Description:
 * @Date:10:31 2018/1/11
 * @Modified by:
 */
public class Res {

    public int code;

    public Object data;

    public final static int CODE_100 = 100; //获取数据成功
    public final static int CODE_101 = 101; //获取数据失败
    public final static int CODE_102 = 102; //该账户不存在
    public final static int CODE_103 = 103; // 密码只能为6-20位的字母、数字、字符组合(!@#$%^&*()-=_+,.<>/-)
    public final static int CODE_104 = 104; //正在同步区块，无法转账
    public final static int CODE_105 = 105; //发送方账户不存在
    public final static int CODE_106 = 106; //可用余额不足
    public final static int CODE_107 = 107; //导出成功
    public final static int CODE_108 = 108; //助记词错误
    public final static int CODE_109 = 109; //已经存在该钱包，无法导入;
    public final static int CODE_110 = 110; //导入成功
    public final static int CODE_111 = 111; //导入失败
    public final static int CODE_112 = 112; //加载账号成功
    public final static int CODE_113 = 113; //加载账号失败
    public final static int CODE_114 = 114; //交易hash不存在
    public final static int CODE_115 = 115; //正在同步区块，无法启动
    public final static int CODE_116 = 116;  //铸造机启动成功
    public final static int CODE_117 = 117;  //铸造机停止失败
    public final static int CODE_118 = 118;  //铸造机停止成功
    public final static int CODE_119 = 119;  //发行代币成功
    public final static int CODE_120 = 120;  //挖矿中
    public final static int CODE_121 = 121;  //未挖矿
    public final static int CODE_122 = 122;  //操作成功
    public final static int CODE_123 = 123;  //已经存在代币
    public final static int CODE_124 = 124;  //代币发行量只能为1000-100000000000的整数
    public final static int CODE_125 = 125;  //代币精度只能为6-8
    public final static int CODE_126 = 126;  //交易金额必须大于0
    public final static int CODE_127 = 127;  //代币名称只能是3-20个字母
    public final static int CODE_128 = 128;  //交易金额精度超出范围
    public final static int CODE_129 = 129;  //发送方ptn余额不足
    public final static int CODE_130 = 130;  //当前节点已有账户在挖矿，无法再开启挖矿
    public final static int CODE_131 = 131;  //代币不存在
    public final static int CODE_132 = 132;  //该合约已被兑换
    public final static int CODE_133 = 133;  //合同地址为空
    public final static int CODE_134 = 134;  //钱包地址为空
    public final static int CODE_135 = 135;  //请输入密码
    public final static int CODE_136 = 136;  //未知的流水
    public final static int CODE_137 = 137;  //未知的本地地址公钥
    public final static int CODE_138 = 138;  //撤销成功
    public final static int CODE_139 = 139;  //交易成功
    public final static int CODE_140 = 140;  //未知的合同
    public final static int CODE_141 = 141;  //该合同已失效
    public final static int CODE_142 = 142;  //该合同已被兑换，无法撤销
    public final static int CODE_143 = 143;  //当发行量大于或等于10000000000,单位不得超过7
    public final static int CODE_144 = 144;  //请选择选项
    public final static int CODE_145 = 145;  //投票成功
    public final static int CODE_146 = 146;  //合约已过期,感谢您的热心参与
    public final static int CODE_147 = 147;  //您已参与过了此次投票
    public final static int CODE_148 = 148;  //该选项已投票次数已超过了合约代码设置值，无法投票
    public final static int CODE_149 = 149;  //您已发行过此代币,请等待确认
    public final static int CODE_150 = 150;  //代币发行成功
    public final static int CODE_151 = 151;  //方法执行成功
    public final static int CODE_152 = 152;  //没有此方法!或传递参数不对应,请检查
    public final static int CODE_153 = 153;  //代码异常,不能正常执行
    public final static int CODE_154 = 154;  //传递参数有误
    public final static int CODE_155 = 155;  //矿工费必须大于0


    public final static int CODE_200 = 200; //转账申请已提交
    public final static int CODE_201 = 201; //接收账户未产生交易，请提供钱包公钥
    public final static int CODE_202 = 202; //验证通过
    public final static int CODE_203 = 203; //验证失败

    public final static int CODE_301 = 301; //密码错误
    public final static int CODE_401 = 401; //钱包地址与公钥不一致
    public final static int CODE_500 = 500; //系统错误，请稍后再试

    public final static int CODE_600 = 600; //事件不匹配
    public final static int CODE_601 = 601; //合约创建失败



    public Res() {
        this.code = 0;
        this.data = "";
    }

    public Res(int code, Object data) {
        super();
        this.code = code;
        this.data = data;
    }

    @Override
    public String toString() {
        return "Res [code=" + code + ", data=" + data + "]";
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

}
