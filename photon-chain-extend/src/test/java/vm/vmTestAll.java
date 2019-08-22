package vm;

import com.photon.photonchain.extend.compiler.Pcompiler;
import com.photon.photonchain.extend.vm.PVM;
import com.photon.photonchain.extend.vm.Program;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: lqh
 * @description:
 * @program: photon-chain
 * @create: 2018-04-18 17:04
 **/
public class vmTestAll {
    @Test
    public void test() throws Exception {
        String contract = "pcompiler version 1.0.0;\n" +
                "contract demo{\n" +
                "set num a;\n" +
                "set num b;\n" +
                "set str operator;\n" +
                "function calculation(num a,num b,str operator){\n" +
                "if(operator==\"+\"){return a+b;}\n" +
                "if(operator==\"-\"){return a-b;}\n" +
                "if(operator==\"*\"){a*b;}\n" +
                "if(operator==\"/\"){a/b;}\n" +
                "}\n" +
                "function add(num a,num b){\n" +
                "a*b;\n" +
                "}\n"+
                "}";

        String bin = Pcompiler.compilerContract(contract);
        Program program = new Program(bin);

        program.analysisAndPushHeap();

        Map<String,Object> map = new HashMap<>();
        map.put("a","33");
        map.put("b","22");
        map.put("function","calculation");//方法名
        map.put("operator","-");//操作
        program.analysisAndGetOpcode(map);


        //PVM pvm = new PVM();
        //pvm.step(program);

        //System.out.println("result:"+program.getResult());

    }
}
