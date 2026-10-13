package solim.test;

import solim.modifier.PendingCellConfig;

public class SolimCoreEnv extends SolimEnv {

    @Override
    protected void resetModuleState() {
        PendingCellConfig.install();
    }
}
