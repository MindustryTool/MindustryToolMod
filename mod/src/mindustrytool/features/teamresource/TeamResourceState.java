package mindustrytool.features.teamresource;

import arc.func.Cons;
import arc.graphics.Color;
import arc.struct.ObjectIntMap;
import arc.struct.ObjectMap;
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
import solim.reactive.Signal;
import mindustry.gen.Groups;

import java.util.Comparator;

public class TeamResourceState {
    private final ObjectSet<Item> usedItems = new ObjectSet<>();
    private final ObjectSet<UnitType> usedUnits = new ObjectSet<>();
    private final Interval timer = new Interval(3);
    private final ObjectMap<Item, HistoryBuffer> itemHistories = new ObjectMap<>();
    private final ObjectMap<PowerGraph, HistoryBuffer> graphHistories = new ObjectMap<>();
    private final HistoryBuffer totalPowerHistory = new HistoryBuffer();

    private @Nullable TeamResourceFeature feature;
    private @Nullable Team selectedTeam;
    private @Nullable ItemModule coreItems;
    private final ObjectIntMap<Item> lastSnapshot = new ObjectIntMap<>();
    private final ObjectIntMap<Item> rateDisplay = new ObjectIntMap<>();

    private boolean viewingStats = false;
    private boolean holdingForStats = false;
    private final Seq<PowerGraph> teamGraphs = new Seq<>();
    private final ObjectIntMap<UnitType> lastUnitCounts = new ObjectIntMap<>();
    private int lastBuildingCount = -1;
    private float lastTotalBalance = 0f;
    private float lastTotalStored = 0f;
    private float lastTotalCapacity = 0f;

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
        lastSnapshot.clear();
        rateDisplay.clear();
        lastUnitCounts.clear();
        lastBuildingCount = -1;
        lastTotalBalance = 0f;
        lastTotalStored = 0f;
        lastTotalCapacity = 0f;
        clearHistory();
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

        if (selectedTeam == null || (Vars.state != null && !selectedTeam.active())) {
            if (Vars.player != null && Vars.player.team() != null) {
                selectedTeam = Vars.player.team();
                selectedTeamSignal.set(selectedTeam);
                changed = true;
            }
        }

        if (timer.get(1, 60f)) {
            updateValidTeams();
        }

        coreItems = (selectedTeam != null && Vars.state != null && selectedTeam.core() != null)
                ? selectedTeam.core().items
                : null;

        if (timer.get(0, 30f)) {
            if (updateCoreItems()) {
                changed = true;
            }

            if (updateUnitCounts()) {
                changed = true;
            }

            if (updateRates()) {
                changed = true;
            }

            boolean showPower = feature == null || Boolean.TRUE.equals(feature.showPowerConfig.get());
            if (showPower) {
                if (updatePowerStats()) {
                    changed = true;
                }
            }
        }

        if (timer.get(2, 60f)) {
            sampleHistory();
        }

        if (changed) {
            tickSignal.set(tickSignal.get() + 1);
        }

        return changed;
    }

    private boolean updateCoreItems() {
        if (coreItems == null || Vars.content == null) {
            return false;
        }
        boolean itemsChanged = false;
        Seq<Item> allItems = Vars.content.items();
        for (int i = 0; i < allItems.size; i++) {
            Item item = allItems.get(i);
            int count = coreItems.get(item);
            if (count > 0 && usedItems.add(item)) {
                itemsChanged = true;
                getItemHistory(item);
            }
        }
        if (itemsChanged) {
            Seq<Item> list = new Seq<>();
            for (int i = 0; i < allItems.size; i++) {
                Item item = allItems.get(i);
                if (usedItems.contains(item)) {
                    list.add(item);
                }
            }
            usedItemsSignal.set(list);
            return true;
        }
        return false;
    }

    private boolean updateUnitCounts() {
        if (selectedTeam == null || Vars.content == null || Vars.state == null) {
            return false;
        }
        boolean changed = false;
        boolean hideZeroUnits = feature != null && Boolean.TRUE.equals(feature.hideZeroUnitsConfig.get());
        boolean unitsChanged = false;
        Seq<UnitType> allUnits = Vars.content.units();
        for (int i = 0; i < allUnits.size; i++) {
            UnitType unit = allUnits.get(i);
            try {
                int count = selectedTeam.data() != null ? selectedTeam.data().countType(unit) : 0;
                int prev = lastUnitCounts.get(unit, -1);
                if (count != prev) {
                    lastUnitCounts.put(unit, count);
                    changed = true;
                }
                if (count > 0) {
                    if (usedUnits.add(unit)) {
                        unitsChanged = true;
                        changed = true;
                    }
                } else if (hideZeroUnits) {
                    if (usedUnits.remove(unit)) {
                        unitsChanged = true;
                        changed = true;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        if (unitsChanged) {
            Seq<UnitType> list = new Seq<>();
            for (int i = 0; i < allUnits.size; i++) {
                UnitType unit = allUnits.get(i);
                if (usedUnits.contains(unit)) {
                    list.add(unit);
                }
            }
            usedUnitsSignal.set(list);
        }
        return changed;
    }

    public @Nullable Team getSelectedTeam() {
        return selectedTeam;
    }

    public void setSelectedTeam(Team team) {
        this.selectedTeam = team;
        this.selectedTeamSignal.set(team);
        this.usedItems.clear();
        this.usedItemsSignal.set(new Seq<>());
        this.usedUnits.clear();
        this.usedUnitsSignal.set(new Seq<>());
        lastSnapshot.clear();
        rateDisplay.clear();
        this.teamGraphs.clear();
        clearHistory();
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
        lastSnapshot.clear();
    }

    public int getItemAmount(Item item) {
        if (coreItems == null && selectedTeam != null && Vars.state != null && selectedTeam.core() != null) {
            coreItems = selectedTeam.core().items;
        }
        return coreItems != null ? coreItems.get(item) : 0;
    }

    public int getItemRate(Item item) {
        return rateDisplay.get(item, 0);
    }

    public static String formatCompactAmount(long amount) {
        if (amount == Long.MIN_VALUE) {
            return "-9.2E";
        }
        boolean negative = amount < 0;
        long abs = negative ? -amount : amount;
        if (abs < 0) {
            abs = Long.MAX_VALUE;
        }

        String result;
        if (abs < 1000) {
            result = String.valueOf(abs);
        } else if (abs < 10000) {
            long fractional = (abs % 1000) / 100;
            result = (abs / 1000) + "." + fractional + "k";
        } else if (abs < 1000000) {
            result = (abs / 1000) + "k";
        } else if (abs < 10000000) {
            long fractional = (abs % 1000000) / 100000;
            result = (abs / 1000000) + "." + fractional + "M";
        } else if (abs < 1000000000) {
            result = (abs / 1000000) + "M";
        } else {
            result = (abs / 1000000000) + "B";
        }
        return negative ? "-" + result : result;
    }

    public String getFormattedAmount(Item item) {
        if (coreItems == null) {
            return "0";
        }
        return formatCompactAmount(coreItems.get(item));
    }

    public String getFormattedRate(Item item) {
        int rate = rateDisplay.get(item, 0);
        if (rate == 0) {
            return "0/s";
        }
        return (rate > 0 ? "+" : "") + formatCompactAmount(rate);
    }

    public String getFullFormattedRate(Item item) {
        int rate = rateDisplay.get(item, 0);
        if (rate == 0) {
            return "0/s";
        }
        return (rate > 0 ? "+" : "") + rate + "/s";
    }

    public Color getRateColor(Item item) {
        int rate = rateDisplay.get(item, 0);
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
            return count > 0 ? formatCompactAmount(count) : "";
        } catch (Throwable t) {
            return "";
        }
    }

    public HistoryBuffer getItemHistory(Item item) {
        HistoryBuffer history = itemHistories.get(item);
        if (history == null) {
            history = new HistoryBuffer();
            if (coreItems == null && selectedTeam != null && Vars.state != null && selectedTeam.core() != null) {
                coreItems = selectedTeam.core().items;
            }
            if (coreItems != null) {
                history.add(coreItems.get(item));
            }
            itemHistories.put(item, history);
        }
        return history;
    }

    public @Nullable HistoryBuffer getPowerGraphHistory(PowerGraph graph) {
        return graphHistories.get(graph);
    }

    public HistoryBuffer getTotalPowerHistory() {
        return totalPowerHistory;
    }

    public void clearHistory() {
        for (HistoryBuffer buffer : itemHistories.values()) {
            buffer.clear();
        }
        itemHistories.clear();
        for (HistoryBuffer buffer : graphHistories.values()) {
            buffer.clear();
        }
        graphHistories.clear();
        totalPowerHistory.clear();
    }

    private void sampleHistory() {
        if (Vars.state == null || selectedTeam == null) {
            return;
        }
        Seq<Item> items = usedItemsSignal.get();
        if (items != null) {
            for (int i = 0; i < items.size; i++) {
                Item item = items.get(i);
                HistoryBuffer history = getItemHistory(item);
                history.add(coreItems != null ? coreItems.get(item) : 0f);
            }
        }

        totalPowerHistory.add(getTotalStoredPower());

        for (int i = 0; i < teamGraphs.size; i++) {
            PowerGraph graph = teamGraphs.get(i);
            HistoryBuffer history = graphHistories.get(graph);
            if (history == null) {
                history = new HistoryBuffer();
                graphHistories.put(graph, history);
            }
            history.add(graph.getLastPowerStored());
        }
    }

    public float getTotalPowerBalance() {
        float total = 0f;
        for (int i = 0; i < teamGraphs.size; i++) {
            total += teamGraphs.get(i).getPowerBalance();
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
        for (int i = 0; i < teamGraphs.size; i++) {
            total += teamGraphs.get(i).getLastPowerStored();
        }
        return total;
    }

    public float getTotalPowerCapacity() {
        float total = 0f;
        for (int i = 0; i < teamGraphs.size; i++) {
            total += teamGraphs.get(i).getLastCapacity();
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
            int rate = rateDisplay.get(item, 0);

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

    private boolean updateRates() {
        if (coreItems == null || Vars.content == null) {
            if (rateDisplay.size > 0) {
                rateDisplay.clear();
                return true;
            }
            return false;
        }

        boolean ratesOrAmountsChanged = false;
        Seq<Item> allItems = Vars.content.items();
        if (lastSnapshot.size > 0) {
            for (int i = 0; i < allItems.size; i++) {
                Item item = allItems.get(i);
                if (item != null) {
                    int amount = coreItems.get(item);
                    int previous = lastSnapshot.get(item, amount);
                    if (amount != previous) {
                        ratesOrAmountsChanged = true;
                    }
                    int newRate = (amount - previous) * 2;
                    int oldRate = rateDisplay.get(item, 0);
                    if (newRate != oldRate) {
                        ratesOrAmountsChanged = true;
                        rateDisplay.put(item, newRate);
                    }
                }
            }
        } else {
            ratesOrAmountsChanged = true;
        }
        lastSnapshot.clear();
        for (int i = 0; i < allItems.size; i++) {
            Item item = allItems.get(i);
            if (item != null) {
                lastSnapshot.put(item, coreItems.get(item));
            }
        }
        return ratesOrAmountsChanged;
    }

    private boolean updatePowerStats() {
        if (selectedTeam == null) {
            return false;
        }

        boolean graphsChanged = false;
        int currentBuildCount = Groups.build != null ? Groups.build.size() : 0;
        if (currentBuildCount != lastBuildingCount) {
            lastBuildingCount = currentBuildCount;
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
                graphsChanged = true;
            }
        }

        float balance = getTotalPowerBalance();
        float stored = getTotalStoredPower();
        float cap = getTotalPowerCapacity();

        boolean statsChanged = graphsChanged
                || Math.abs(balance - lastTotalBalance) > 0.01f
                || (long) stored != (long) lastTotalStored
                || (long) cap != (long) lastTotalCapacity;

        if (statsChanged) {
            lastTotalBalance = balance;
            lastTotalStored = stored;
            lastTotalCapacity = cap;
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
