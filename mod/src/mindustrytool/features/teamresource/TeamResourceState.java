package mindustrytool.features.teamresource;

import arc.func.Cons;
import arc.graphics.Color;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Interval;
import arc.util.Log;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.core.UI;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.UnitType;
import mindustry.world.blocks.power.PowerGraph;
import mindustry.world.modules.ItemModule;
import solim.signal.Signal;
import mindustry.gen.Groups;

import java.util.Comparator;

/**
 * Encapsulates game logic and state for tracking team resources, flow rates, units, and power grids.
 * Provides reactive signals for Solim UI bindings.
 */
public class TeamResourceState {
    private final ObjectSet<Item> usedItems = new ObjectSet<>();
    private final ObjectSet<UnitType> usedUnits = new ObjectSet<>();
    private final Interval timer = new Interval(2);

    private @Nullable TeamResourceFeature feature;
    private @Nullable Team selectedTeam;
    private @Nullable ItemModule coreItems;
    private @Nullable ItemModule lastSnapshot;
    private @Nullable ItemModule rateDisplay;

    private boolean viewingStats = false;
    private boolean holdingForStats = false;
    private final Seq<PowerGraph> teamGraphs = new Seq<>();

    private final Seq<Team> tempTeams = new Seq<>();
    private final ObjectSet<PowerGraph> tempPowerGraphs = new ObjectSet<>();
    private final Seq<PowerGraph> tempGraphSeq = new Seq<>();
    private final Cons<Building> powerGraphCollector = b -> {
        if (b.team == selectedTeam && b.power != null && b.power.graph != null) {
            PowerGraph graph = b.power.graph;
            if (graph.getLastPowerProduced() > 0 || graph.getLastCapacity() > 0 || graph.getLastPowerNeeded() > 0) {
                tempPowerGraphs.add(graph);
            }
        }
    };
    private static final Comparator<PowerGraph> GRAPH_COMPARATOR = (a, b) -> {
        int cap = Float.compare(b.getLastCapacity(), a.getLastCapacity());
        return cap != 0 ? cap : Integer.compare(a.hashCode(), b.hashCode());
    };

    public final Signal<Team> selectedTeamSignal = Signal.of(Team.sharded);
    public final Signal<Long> tickSignal = Signal.of(0L);
    public final Signal<Seq<Team>> validTeamsSignal = Signal.of(new Seq<>());
    public final Signal<Seq<Item>> usedItemsSignal = Signal.of(new Seq<>());
    public final Signal<Seq<UnitType>> usedUnitsSignal = Signal.of(new Seq<>());

    public TeamResourceState() {
        this(null);
    }

    public TeamResourceState(@Nullable TeamResourceFeature feature) {
        this.feature = feature;
        if (Vars.player != null && Vars.player.team() != null) {
            this.selectedTeam = Vars.player.team();
            this.selectedTeamSignal.set(this.selectedTeam);
        }
        updateValidTeams();
    }

    public void setFeature(@Nullable TeamResourceFeature feature) {
        this.feature = feature;
    }

    public void updateValidTeams() {
        tempTeams.clear();
        if (Vars.state != null && Team.all != null) {
            for (Team team : Team.all) {
                if (team != null && team.active()) {
                    try {
                        if (team.data() != null && team.data().hasCore()) {
                            tempTeams.add(team);
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        Seq<Team> current = validTeamsSignal.get();
        if (current == null || current.size != tempTeams.size || !current.equals(tempTeams)) {
            validTeamsSignal.set(new Seq<>(tempTeams));
        }
    }

    public void reset() {
        usedItems.clear();
        usedUnits.clear();
        usedItemsSignal.set(new Seq<>());
        usedUnitsSignal.set(new Seq<>());
        if (lastSnapshot != null) {
            lastSnapshot.clear();
        }
        if (rateDisplay != null) {
            rateDisplay.clear();
        }
        if (Vars.player != null && Vars.player.team() != null) {
            selectedTeam = Vars.player.team();
            selectedTeamSignal.set(selectedTeam);
        } else {
            selectedTeam = Team.sharded;
            selectedTeamSignal.set(Team.sharded);
        }
        teamGraphs.clear();
        updateValidTeams();
        tickSignal.set(tickSignal.get() + 1);
    }

    public void onWorldLoad() {
        reset();
    }

    public boolean update() {
        boolean changed = false;

        if (selectedTeam == null || !selectedTeam.active()) {
            if (Vars.player != null && Vars.player.team() != null) {
                selectedTeam = Vars.player.team();
                selectedTeamSignal.set(selectedTeam);
                changed = true;
            }
        }

        if (timer.get(1, 60f)) {
            updateValidTeams();
        }

        coreItems = (selectedTeam != null && Vars.state != null && selectedTeam.core() != null) ? selectedTeam.core().items : null;
        if (coreItems != null && Vars.content != null) {
            boolean itemsChanged = false;
            for (Item item : Vars.content.items()) {
                if (coreItems.get(item) > 0 && usedItems.add(item)) {
                    itemsChanged = true;
                    changed = true;
                }
            }
            if (itemsChanged) {
                Seq<Item> list = new Seq<>();
                for (Item item : Vars.content.items()) {
                    if (usedItems.contains(item)) {
                        list.add(item);
                    }
                }
                usedItemsSignal.set(list);
            }
        }

        if (selectedTeam != null && Vars.content != null && Vars.state != null) {
            boolean unitsChanged = false;
            for (UnitType unit : Vars.content.units()) {
                try {
                    if (selectedTeam.data() != null && selectedTeam.data().countType(unit) > 0 && usedUnits.add(unit)) {
                        unitsChanged = true;
                        changed = true;
                    }
                } catch (Throwable ignored) {
                }
            }
            if (unitsChanged) {
                Seq<UnitType> list = new Seq<>();
                for (UnitType unit : Vars.content.units()) {
                    if (usedUnits.contains(unit)) {
                        list.add(unit);
                    }
                }
                usedUnitsSignal.set(list);
            }
        }

        if (timer.get(0, 30f)) {
            updateRates();

            boolean showPower = feature == null || Boolean.TRUE.equals(feature.showPowerConfig.get());
            if (showPower) {
                if (updatePowerStats()) {
                    changed = true;
                }
            }
            changed = true;
        }

        if (changed) {
            tickSignal.set(tickSignal.get() + 1);
        }

        return changed;
    }

    public @Nullable Team getSelectedTeam() {
        return selectedTeam;
    }

    public void setSelectedTeam(Team team) {
        this.selectedTeam = team;
        this.selectedTeamSignal.set(team);
        this.usedUnits.clear();
        this.usedUnitsSignal.set(new Seq<>());
        if (lastSnapshot != null) {
            lastSnapshot.clear();
        }
        if (rateDisplay != null) {
            rateDisplay.clear();
        }
        this.teamGraphs.clear();
        tickSignal.set(tickSignal.get() + 1);
    }

    public @Nullable ItemModule getCoreItems() {
        return coreItems;
    }

    public ObjectSet<Item> getUsedItems() {
        return usedItems;
    }

    public ObjectSet<UnitType> getUsedUnits() {
        return usedUnits;
    }

    public Seq<PowerGraph> getTeamGraphs() {
        return teamGraphs;
    }

    public boolean isViewingStats() {
        return viewingStats;
    }

    public void setViewingStats(boolean viewingStats) {
        this.viewingStats = viewingStats;
        tickSignal.set(tickSignal.get() + 1);
    }

    public boolean isHoldingForStats() {
        return holdingForStats;
    }

    public void setHoldingForStats(boolean holdingForStats) {
        this.holdingForStats = holdingForStats;
    }

    public void clearSnapshot() {
        if (lastSnapshot != null) {
            lastSnapshot.clear();
        }
    }

    public int getItemAmount(Item item) {
        return coreItems != null ? coreItems.get(item) : 0;
    }

    public int getItemRate(Item item) {
        return rateDisplay != null ? rateDisplay.get(item) : 0;
    }

    public String getFormattedAmount(Item item) {
        if (coreItems == null) {
            return "0";
        }
        return UI.formatAmount(coreItems.get(item));
    }

    public String getFormattedRate(Item item) {
        if (rateDisplay == null) {
            return "0/s";
        }
        int rate = rateDisplay.get(item);
        if (rate == 0) {
            return "0/s";
        }
        String prefix = rate > 0 ? "+" : "";
        return prefix + rate + "/s";
    }

    public Color getRateColor(Item item) {
        if (rateDisplay == null) {
            return Color.gray;
        }
        int rate = rateDisplay.get(item);
        if (rate > 0) {
            return Pal.heal;
        }
        if (rate < 0) {
            return Pal.remove;
        }
        return Color.gray;
    }

    public String getUnitCountText(@Nullable UnitType type) {
        if (type == null || Vars.state == null) {
            return "";
        }
        Team current = selectedTeam != null ? selectedTeam : (Vars.player != null ? Vars.player.team() : null);
        if (current == null) {
            return "";
        }
        try {
            int count = current.data() != null ? current.data().countType(type) : 0;
            return count > 0 ? UI.formatAmount(count) : "";
        } catch (Throwable t) {
            return "";
        }
    }

    public float getTotalPowerBalance() {
        float total = 0f;
        for (PowerGraph graph : teamGraphs) {
            total += graph.getPowerBalance();
        }
        return total;
    }

    public String getFormattedPowerBalance() {
        float totalBalance = getTotalPowerBalance();
        return (totalBalance >= 0 ? "+" : "") + UI.formatAmount((long) (totalBalance * 60));
    }

    public Color getPowerBalanceColor() {
        return getTotalPowerBalance() >= 0 ? Pal.heal : Pal.remove;
    }

    public float getTotalStoredPower() {
        float total = 0f;
        for (PowerGraph graph : teamGraphs) {
            total += graph.getLastPowerStored();
        }
        return total;
    }

    public float getTotalPowerCapacity() {
        float total = 0f;
        for (PowerGraph graph : teamGraphs) {
            total += graph.getLastCapacity();
        }
        return total;
    }

    public String getFormattedStoredPower() {
        return UI.formatAmount((long) getTotalStoredPower()) + " / " + UI.formatAmount((long) getTotalPowerCapacity());
    }

    public CharSequence formatItem(Item item) {
        if (coreItems == null) {
            return "0";
        }

        try {
            int amount = coreItems.get(item);
            int rate = rateDisplay != null ? rateDisplay.get(item) : 0;

            boolean alwaysShow = feature == null || Boolean.TRUE.equals(feature.alwaysShowFlowRateConfig.get());
            if (alwaysShow) {
                return formatAmountWithRate(amount, rate);
            }

            if (viewingStats) {
                return formatRate(rate);
            }

            String color = "[white]";
            if (rate < 0) {
                color = "[scarlet]";
            } else if (rate > 0) {
                color = "[lime]";
            }

            return color + UI.formatAmount(amount);
        } catch (Exception e) {
            Log.err("Failed to format item", e);
            return "0";
        }
    }

    private void updateRates() {
        if (coreItems == null || Vars.content == null) {
            return;
        }
        if (lastSnapshot == null) {
            lastSnapshot = new ItemModule();
        }
        if (rateDisplay == null) {
            rateDisplay = new ItemModule();
        }

        if (lastSnapshot.any()) {
            int count = coreItems.length();
            for (int id = 0; id < count; id++) {
                int amount = coreItems.get(id);
                Item item = Vars.content.item(id);
                if (item != null) {
                    rateDisplay.set(item, (amount - lastSnapshot.get(id)) * 2);
                }
            }
        }
        lastSnapshot.set(coreItems);
    }

    private boolean updatePowerStats() {
        if (selectedTeam == null) {
            return false;
        }

        tempPowerGraphs.clear();
        if (Groups.build != null) {
            Groups.build.each(powerGraphCollector);
        }

        tempGraphSeq.clear();
        for (PowerGraph graph : tempPowerGraphs) {
            tempGraphSeq.add(graph);
        }
        tempGraphSeq.sort(GRAPH_COMPARATOR);

        if (!tempGraphSeq.equals(teamGraphs)) {
            teamGraphs.set(new Seq<>(tempGraphSeq));
            return true;
        }
        return false;
    }

    private CharSequence formatAmountWithRate(int amount, int rate) {
        String rateText = formatRate(rate);
        return UI.formatAmount(amount) + " " + rateText;
    }

    private String formatRate(int rate) {
        if (rate == 0) {
            return "[gray]0/s[]";
        }
        String color = rate > 0 ? "[lime]+" : "[scarlet]";
        return color + rate + "/s[]";
    }
}
