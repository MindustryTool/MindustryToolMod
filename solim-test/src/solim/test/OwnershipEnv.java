package solim.test;

/**
 * A test base that runs its body with no ambient owner, so a suite whose subject is ownership can
 * assert the exact owner stack. Ordinary suites get a throwaway owner from {@link SolimEnv}.
 */
public class OwnershipEnv extends SolimEnv {

    @Override
    protected boolean opensOwnershipWindow() {
        return false;
    }
}