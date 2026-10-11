package mindustrytool.features.autoplay.tasks;

import arc.util.Nullable;
import arc.util.Time;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.gen.Unit;
import mindustry.type.Item;

public class TransportAI extends BaseAutoplayAI {

    public static final float TRANSFER_COOLDOWN = 25f;
    public static final float TRANSFER_TIMEOUT = 60f;

    public enum Action {
        PICKUP,
        DELIVER,
        DEPOSIT_CORE,
        CLEAR_HELD
    }

    public Action action = Action.PICKUP;
    public @Nullable Building targetBuilding;
    public @Nullable Item targetItem;
    public int targetAmount = 0;

    private float transferCooldownTimer = 0f;
    private float transferPendingTimer = 0f;
    private boolean transferPending = false;
    private int prevAmount = 0;

    public void setTaskTarget(Action action, @Nullable Building building, @Nullable Item item, int amount) {
        this.action = action;
        this.targetBuilding = building;
        this.targetItem = item;
        this.targetAmount = amount;
    }

    public boolean canAct() {
        return !transferPending && transferCooldownTimer <= 0f;
    }

    private void markActed(Unit unit) {
        transferPending = true;
        transferPendingTimer = TRANSFER_TIMEOUT;
        prevAmount = unit.stack.amount;
        transferCooldownTimer = TRANSFER_COOLDOWN;
    }

    private void updatePending(Unit unit) {
        if (transferCooldownTimer > 0f) {
            transferCooldownTimer -= Time.delta;
        }
        if (!transferPending) {
            return;
        }

        if (action == Action.PICKUP) {
            if (unit.stack.amount > prevAmount) {
                transferPending = false;
                transferPendingTimer = 0f;
                return;
            }
        } else {
            if (unit.stack.amount < prevAmount || unit.stack.amount == 0) {
                transferPending = false;
                transferPendingTimer = 0f;
                return;
            }
        }

        transferPendingTimer -= Time.delta;
        if (transferPendingTimer <= 0f) {
            transferPending = false;
        }
    }

    @Override
    public void updateMovement() {
        if (unit == null || !unit.isValid()) {
            return;
        }

        updatePending(unit);

        if (action == Action.CLEAR_HELD) {
            if (canAct()) {
                unit.clearItem();
                markActed(unit);
            }
            return;
        }

        if (targetBuilding == null || !targetBuilding.isValid()) {
            return;
        }

        float range = Math.min(unit.type.range, Vars.itemTransferRange);
        moveTo(targetBuilding, range * 0.7f, 20f);

        if (!unit.within(targetBuilding, range)) {
            return;
        }

        if (!canAct()) {
            return;
        }

        if (action == Action.PICKUP) {
            if (targetItem != null && targetAmount > 0) {
                Call.requestItem(Vars.player, targetBuilding, targetItem, targetAmount);
                markActed(unit);
            }
        } else if (action == Action.DELIVER || action == Action.DEPOSIT_CORE) {
            if (unit.stack.amount > 0) {
                Call.transferInventory(Vars.player, targetBuilding);
                markActed(unit);
            }
        }
    }
}
