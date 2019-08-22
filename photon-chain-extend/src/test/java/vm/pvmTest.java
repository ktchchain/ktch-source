package vm;

import com.photon.photonchain.extend.vm.Heap;
import com.photon.photonchain.extend.vm.PVM;
import com.photon.photonchain.extend.vm.Program;
import com.photon.photonchain.extend.vm.Stack;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: lqh
 * @description: vm
 * @program: photon-chain
 * @create: 2018-04-18 09:33
 **/
public class pvmTest {

    @Test
    public void test() {
        Program program = new Program(" ");
        String opCodes = "or,return";
        String[] opCodesArray = opCodes.split(",");
        List<String> ops = new ArrayList<>();
        Stack stack = new Stack();
        for (int i = 0; i < opCodesArray.length; i++) {
            ops.add(opCodesArray[i]);
        }
        stack.push(new BigDecimal(0));
        stack.push(new BigDecimal(0));
        program.setStack(stack);
        program.setOps(ops);

        PVM pvm = new PVM();
        pvm.step(program);

        System.out.println("result:" + program.getResult());
    }

    @Test
    public void ss() {
        BigDecimal a = new BigDecimal(10);
        BigDecimal b = new BigDecimal(2);
        BigDecimal shan = a.divideAndRemainder(b)[0];
        BigDecimal yu = a.divideAndRemainder(b)[1];
        System.out.println("商：" + shan + ",余数：" + yu);
    }

}
