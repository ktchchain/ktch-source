package compiler;

import com.photon.photonchain.extend.compiler.Pcompiler;
import org.junit.Test;

/**
 * @Author:Lin
 * @Description:
 * @Date:10:14 2018/4/13
 * @Modified by:
 */
public class compilerTest {
    @Test
    public void calculation() {
        try {
            String contract = "pcompiler   version  1.0.0;"+
                    "contract demo{" +
                    "set num num1=2;" +
                    "set num num2=1000;" +
                    "function calculaset(num a,num b,str operator)" +
                    "{if(12==123)" +
                    "{return a+b;}" +
//                    "else{\n" +
//"                    System.out.println(contentMap.get(\"content\" + function_i));}" +
                    "if(operator==\"a\")" +
                    "{return a-b;}" +
                    "if(operator==\"*\")" +
                    "{return a*b;}" +
                    " a = a+1;" +
                    "if(operator==\"/\")" +
                    "{return a/b;}}" +
                    "function calculation2(num c,num  a,str  b)" +
                    "{ ;return a+b;}" +
                    " set coin a=2 ptn;" +
                    " set coin b=1000 lin;" +
                    "event guadan(b,a);" +
                    "}"
                    ;
//            String s = "^pcompiler[ ]+?version[ ]+?1.0.0[ ]+?;[ ]+?contract[ ]+?demo[ ]+?\\{[ ]+[\\\\s\\\\S]+?";
            System.out.println(contract);
            String s = "^pcompiler[ ]+?version[ ]+?[1\\.0\\.0][ ]*?;[ ]*?contract[ ]+?[\\s\\S]+?";
            String s1 = "AB[\\s\\S]+?BA";
            String str = "AB 11 11BA11 1BA";
            System.out.println(contract.matches(s));
//            Map<String, String> contractMap = RegexCompile.getLine(contract);
//            for (String s : contractMap.keySet()) {
//                if (StringUtils.startsWith(contractMap.get(s).trim(), "function ")) {
//                    RegexCompile.functionRegex(contractMap.get(s).trim());
//                }
//            }
//            StringBuilder contractSb = new StringBuilder(contract);
//            String index = StringUtils.substringBefore(contract, "event");
//            if (contract.charAt(index.length() - 1) == ';' || contract.charAt(index.length() - 1) == '}') {
//                contractSb.insert(index.length(), "set address add=\"sdfsadfadsf\";");
//            }
//            contract = RegexCompile.variableComplement(contractSb.toString());
//            RegexCompile.checkRegex(contract);
//            System.out.println(Pcompiler.compilerContract(contract));
//            contractSb = new StringBuilder(contract);
//            contractSb=new StringBuilder(JSON.parseObject(Hex.decode("7b22636f6e74656e7438223a226576656e742067756164616e28622c6129222c22636f6e74656e7437223a2261646472657373206164643d73646673616466616473662b222c22636f6e74656e7436223a2273657420636f696e20623d31303030206c696e222c22636f6e74656e7435223a2273657420636f696e20613d312070746e222c22636f6e74656e7434223a227365742061646472657373206d79416464726573733d707861326136383061366165643461383433313330303530303966636134326433663233356363313165222c22636f6e74656e743130223a2266756e6374696f6e2067756164616e28616464726573732066726f6d416464726573732c6164647265737320636f6e747261637441646472657373297b7472616e732866726f6d416464726573732c6e756c6c2c61293b7472616e7328636f6e7472616374416464726573732c66726f6d416464726573732c62293b7d222c22636f6e74656e7433223a2266756e6374696f6e2063616c63756c6174696f6e286e756d20612c6e756d20622c737472206f70657261746f72297b6966286f70657261746f723d3d2b297b72657475726e20612b623b2c6b6a6c6a6b6c3b7d6966286f70657261746f723d3d2d297b72657475726e20612d623b7d6966286f70657261746f723d3d2a297b72657475726e20612a623b7d6966286f70657261746f723d3d2f297b72657475726e20612f623b7d7d222c22636f6e74656e7432223a22736574206e756d20623d2d312e31222c22636f6e7472616374223a2264656d6f222c22636f6e74656e7431223a22736574202020206e756d20613d2d3130222c2276657273696f6e223a2270636f6d70696c65722076657273696f6e20312e302e30222c22636f6e74656e7439223a2266756e6374696f6e20696e6974286164647265737320636f6e747261637441646472657373297b7472616e73286e756c6c2c636f6e7472616374416464726573732c62293b7d227d"), Map.class));
//            Map<String, Object> contractMap = JSON.parseObject(Hex.decode(Pcompiler.compilerContract(contractSb.toString())), Map.class);//将字节码转回map
//            String parameter = null;
//            BigDecimal fee = new BigDecimal(0);
//            List<String> list = new ArrayList<>();
//            for(String s :contractMap.keySet()){
//                if(StringUtils.startsWith(contractMap.get(s).toString().trim(),"set")){
//                    String[] variable = ArrayUtils.removeAllOccurences(contractMap.get(s).toString().split(" "), "");
//                    if(variable[1].equals("coin")){
//                        list.add(contractMap.get(s).toString());
//                    }
//                }
//                if(StringUtils.startsWith(contractMap.get(s).toString().trim(),"event")){
//                    if(StringUtils.substringBetween(contractMap.get(s).toString(),"event","(").trim().equals("guadan")){
//                        parameter = StringUtils.substringBetween(contractMap.get(s).toString(),"(",")");
//                    }
//                }
//            }
//            String[] eventParams = parameter.split(",");//事件里的参数
//            for(String s:list){
//                String[] variable = ArrayUtils.removeAllOccurences(s.split(" "), "");
////                String[] params = variable[2].split("=");//获取coin 参数
//                //获取手续费
//                if(variable[variable.length - 1].equals(Constants.PTN) && (variable[2].equals(eventParams[0]) || variable[2].equals(eventParams[1]))){
//                    fee = new BigDecimal(variable[4]).multiply(new BigDecimal(Constants.MININUMUNIT)).divide(new BigDecimal(1000));
//                }
//            }
//            System.out.println(eventParams[0]+":"+fee);
            /*List<String> list = Pcompiler.getMember(contractSb.toString(), "(set coin[^;]*)");
            String indexAfter = StringUtils.substringBetween(contractSb.toString(), "event", ";");
            String parameter = StringUtils.substringBetween(indexAfter, "(", ")");
            String[] eventParams = parameter.split(",");//事件里的参数
            BigDecimal sell = new BigDecimal(0);
            String tokenName = null;
            BigDecimal transValue = null;
            BigDecimal fee = null;
            for (String s : list) {
                String[] variable = ArrayUtils.removeAllOccurences(s.split(" "), "");
                if (variable[1].equals("coin")) {
                    //获取手续费
                    if (variable[variable.length - 1].equals(Constants.PTN) && (variable[2].equals(eventParams[0]) || variable[2].equals(eventParams[1]))) {
                        fee = new BigDecimal(variable[4]).multiply(new BigDecimal(Constants.MININUMUNIT)).divide(new BigDecimal(1000));
                    }
                    if (variable[2].equals(eventParams[0])) {//获取第一个参数得到 sell 类型
//                        Assets assets = assetsRepository.findByPubKeyAndTokenName(pubKey, variable[variable.length - 1]);
//                        if (assets == null) {
//                            res.setCode(Res.CODE_102);return res;
//                        } else {
//                            long effectiveIncome = assets.getTotalEffectiveIncome();
//                            long expenditure = assets.getTotalExpenditure();
//                            long balance = effectiveIncome - expenditure;
//                            if (balance < new BigDecimal(new Long(variable[5])).multiply(new BigDecimal(Constants.MININUMUNIT)).longValue()) {
//                                res.setCode(Res.CODE_106);return res;
//                            }
//                        }
                        //代币名
                        tokenName = variable[variable.length - 1];
                        //交易金额
                        transValue = new BigDecimal(variable[4]).multiply(new BigDecimal(Constants.MININUMUNIT));
                    }
                }
            }*/
//            System.out.println(tokenName + ":" + transValue + ":" + fee);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void guadan() {
        String contract = "pcompiler version 1.0.0;\n" +
                "contract demo{" +
                "address myAddress=\"pxa2a680a6aed4a84313005009fca42d3f235cc11e\";\n" +
                "coin selling=1 ptn;\n" +
                "coin purchase=1000 lin;\n" +
                "event guadan(coin purchase,coin selling);\n" +
                "}";
        System.out.println(Pcompiler.compilerContract(contract));
        /*"contract guadan{\n" +
                "address myAddress=\"pxa2a680a6aed4a84313005009fca42d3f235cc11e\";\n"+
                "coin selling=1 ptn;\n"+
                "coin purchase=1000 lin;\n"+
                "}";*/

        /*function init(address heyuedizhi){trans(myAddress,heyuedizhi,selling);}

        function guadan(address add,address heyuedizhi){trans(add,myAddress,purchase);trans(heyuedizhi,add,selling);}*/
    }
}
