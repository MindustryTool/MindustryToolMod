package solim.test;

import solim.modifier.PendingCellConfig;

/**
 * Core-layer test environment for solim-core tests.
 *
 * Extends {@link SolimEnv} with the solim-core specific reset the runtime
 * layer cannot perform: reinstalling the default cell configurator
 * ({@link PendingCellConfig#install()}) after a test replaced it via
 * {@code ParentStack.setCellConfigurator(...)}.
 *
 * Lives in solim-core (not solim-test) because it references solim-core
 * types — a test-support type must be owned by the lowest module whose
 * types it needs, otherwise a build-path cycle appears.
 */
public class SolimCoreEnv extends SolimEnv {

	@Override
	protected void resetModuleState() {
		PendingCellConfig.install();
	}
}
