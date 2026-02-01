## Granular Behavior Analysis

Based on your requirements, I will decompose the code in [behavor.txt](file:///e:/Codes/MindustryTool/MindustryToolMod/src/mindustrytool/features/autoplay/tasks/behavor.txt) into the following specialized tasks. Each task will be an independent module in the priority list, allowing for fine-tuned control.

### 1. **New Specialized Tasks**
*   **`FollowAssistTask`**: Exclusively helps other players by following them and contributing to their build plans.
*   **`GlobalAssistTask`**: Focuses on the team's global build queue (`unit.team.data().plans`).
*   **`AttackTask`**: Targets and engages nearby enemies within a configurable range.
*   **`FleeTask`**: Retreats to the core if enemies are nearby and the unit is not engaged in higher-priority tasks.
*   **`SelfHealTask`**: Triggers when the player's unit is at low health, directing it to a safe location or core to recover.
*   **`RepairTask`** (Refined): Focused purely on repairing damaged structures.
*   **`MiningTask`** (Refined): Focused purely on resource extraction.

### 2. **Visualization System**
*   **Target Tracking**: I will implement a `BaseAutoplayAI` that tracks a `targetPos`.
*   **Line Rendering**: [AutoplayFeature.java](file:///e:/Codes/MindustryTool/MindustryToolMod/src/mindustrytool/features/autoplay/AutoplayFeature.java) will be updated to draw a dashed or colored line from the unit to its current AI target, providing clear visual feedback on what the autoplay is doing.

---

## Technical Implementation Plan

### Step 1: Interface & Base AI
1.  **Update `AutoplayTask`**: Add a `Vec2 getTargetPos()` method to the interface.
2.  **Create `BaseAutoplayAI`**: A shared base class for all autoplay AIs that handles common movement logic and stores the current `targetPos`.

### Step 2: Implement Granular Tasks
1.  **Create 5 new task files**: `FollowAssistTask`, `GlobalAssistTask`, `AttackTask`, `FleeTask`, and `SelfHealTask`.
2.  **Update existing files**: Refactor `RepairTask` and `MiningTask` to use the new `BaseAutoplayAI`.

### Step 3: Feature & UI Update
1.  **Update `AutoplayFeature`**:
    *   Register all new tasks in the `init()` method.
    *   Add line rendering logic in the `draw()` method using `Drawf.dashLine` or similar.
2.  **Update Dialog**: Ensure the new tasks appear in the [AutoplaySettingDialog.java](file:///e:/Codes/MindustryTool/MindustryToolMod/src/mindustrytool/features/autoplay/AutoplaySettingDialog.java) for reordering and toggling.

### Step 4: Localization
1.  Add names and status descriptions for all 7 tasks to `bundle.properties`.

Does this more granular approach meet your expectations? I'm ready to start the implementation upon your approval.