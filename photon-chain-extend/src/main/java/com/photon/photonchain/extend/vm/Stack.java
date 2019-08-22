package com.photon.photonchain.extend.vm;

/**
 * @Author:Lin
 * @Description:
 * @Date:16:40 2018/4/11
 * @Modified by:
 */
public class Stack extends java.util.Stack {
    @Override
    public synchronized Object pop() {
        return super.pop();
    }

    @Override
    public Object push(Object o) {
        return super.push(o);
    }
}
