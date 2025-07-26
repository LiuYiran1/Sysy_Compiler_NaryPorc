package com.compiler.pass;

import com.compiler.ll.Module;
import com.compiler.ll.Values.GlobalValues.Function;

public interface Pass {

    void run(Module module);
}
