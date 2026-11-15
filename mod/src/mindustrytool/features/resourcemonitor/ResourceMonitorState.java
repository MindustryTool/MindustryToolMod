package mindustrytool.features.resourcemonitor;

import arc.func.Cons;
import arc.graphics.Color;
import arc.struct.ObjectIntMap;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Interval;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Time;
import java.util.Comparator;
import mindustry.Vars;
import mindustry.core.UI;
import mindustry.game.Team;
import mindustry.game.Teams.TeamData;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.UnitType;
import mindustry.world.blocks.power.PowerGraph;
import mindustry.world.modules.ItemModule;
import solim.reactive.Signal;

public class ResourceMonitorState {
    private final ObjectSet<Item> usedItems = new ObjectSet<>();
    private final ObjectSet<UnitType> usedUnits = new ObjectSet<>();
    private final Interval timer = new Interval(2);

    private @Nullable ResourceMonitorFeature feature;
    private @Nullable Team selectedTeam = Team.sharded;
    private @Nullable ItemModule coreItems;
    private final ObjectIntMap<Item> lastSnapshot = new ObjectIntMap<>();
    private final ObjectIntMap<Item> rateDisplay = new ObjectIntMap<>();
    private float lastRateUpdateTime = -1f;

    private boolean viewingStats = false;
    private boolean holdingForStats = false;
    private final Seq<PowerGraph> teamGraphs = new Seq<>();
    private final ObjectIntMap<UnitType> lastUnitCounts = new ObjectIntMap<>();
    private final ObjectMap<Item, ItemFormatEntry> itemFormatCache = new ObjectMap<>();
    private final ObjectIntMap<UnitType> unitFormatCountCache = new ObjectIntMap<>();
    private final ObjectMap<UnitType, String> unitFormatTextCache = new ObjectMap<>();
    private long cachedBalanceLong = Long.MIN_VALUE;
    private boolean cachedBalancePositive = false;
    private String cachedBalanceText = "+0";
    private long cachedStoredLong = Long.MIN_VALUE;
    private long cachedCapacityLong = Long.MIN_VALUE;
    private String cachedStoredText = "0 / 0";
    private float lastTotalBalance = 0f;
    private float lastTotalStored = 0f;
    private float lastTotalCapacity = 0f;

    private static final class ItemFormatEntry {
        int amount = Integer.MIN_VALUE;
        int rate = Integer.MIN_VALUE;
        int mode = -1;
        CharSequence text = "0";
    }

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
    public final Signal<Seq<PowerGraph>> teamGraphsSignal = Signal.of(new Seq<>());

    public ResourceMonitorState() {
        this(null);
    }

    public ResourceMonitorState(@Nullable ResourceMonitorFeature feature) {
        this.feature = feature;
        if (Vars.player != null && Vars.player.team() != null) {
            this.selectedTeam = Vars.player.team();
            this.selectedTeamSignal.set(this.selectedTeam);
        }
        updateValidTeams();
    }

    public void setFeature(@Nullable ResourceMonitorFeature feature) {
        this.feature = feature;
    }

    public void updateValidTeams() {
        tempTeams.clear();
        if (Vars.state != null && Vars.state.teams != null) {
            Seq<TeamData> active = Vars.state.teams.getActive();
            if (active != null) {
                for (int i = 0; i < active.size; i++) {
                    TeamData data = active.get(i);
                    if (data != null && data.team != null && data.hasCore()) {
                        tempTeams.add(data.team);
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
        lastRateUpdateTime = -1f;
        lastUnitCounts.clear();
        itemFormatCache.clear();
        unitFormatCountCache.clear();
        unitFormatTextCache.clear();
        cachedBalanceLong = Long.MIN_VALUE;
        cachedStoredLong = Long.MIN_VALUE;
        cachedCapacityLong = Long.MIN_VALUE;
        lastTotalBalance = 0f;
        lastTotalStored = 0f;
        lastTotalCapacity = 0f;
        if (Vars.player != null && Vars.player.team() != null) {
            selectedTeam = Vars.player.team();
            selectedTeamSignal.set(selectedTeam);
        } else {
            selectedTeam = Team.sharded;
            selectedTeamSignal.set(Team.sharded);
        }
        teamGraphs.clear();
        teamGraphsSignal.set(new Seq<>());
        updateValidTeams();
        tickSignal.set(tickSignal.get() + 1);
    }

    public void onWorldLoad() {
        reset();
    }

    public boolean update() {
        boolean changed = false;

        if (selectedTeam == null || (Vars.state != null && Vars.state.teams != null && !selectedTeam.active())) {
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
        if (coreItems != null && Vars.content != null) {
            boolean hideZeroItems = feature != null && Boolean.TRUE.equals(feature.hideZeroItemsConfig.get());
            boolean itemsChanged = false;
            Seq<Item> allItems = Vars.content.items();
            for (int i = 0; i < allItems.size; i++) {
                Item item = allItems.get(i);
                int count = coreItems.get(item);
                if (count > 0) {
                    if (usedItems.add(item)) {
                        itemsChanged = true;
                        changed = true;
                    }
                } else if (hideZeroItems) {
                    if (usedItems.remove(item)) {
                        itemsChanged = true;
                        changed = true;
                    }
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
            }
        }

        if (selectedTeam != null && Vars.content != null && Vars.state != null) {
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
        }

        if (timer.get(0, 30f)) {
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
        if (Vars.state != null && Vars.state.isGame()) {
            this.usedItems.clear();
            this.usedItemsSignal.set(new Seq<>());
        }
        this.usedUnits.clear();
        this.usedUnitsSignal.set(new Seq<>());
        lastSnapshot.clear();
        rateDisplay.clear();
        lastRateUpdateTime = -1f;
        lastUnitCounts.clear();
        itemFormatCache.clear();
        unitFormatCountCache.clear();
        unitFormatTextCache.clear();
        cachedBalanceLong = Long.MIN_VALUE;
        cachedStoredLong = Long.MIN_VALUE;
        cachedCapacityLong = Long.MIN_VALUE;
        this.teamGraphs.clear();
        this.teamGraphsSignal.set(new Seq<>());
        tickSignal.set(tickSignal.get() + 1);
    }

    public @Nullable ItemModule getCoreItems() {
        return coreItems;
    }

    void setCoreItems(@Nullable ItemModule coreItems) {
        this.coreItems = coreItems;
    }

    void setItemRate(Item item, int rate) {
        rateDisplay.put(item, rate);
        itemFormatCache.remove(item);
    }

    boolean stepRates() {
        return updateRates();
    }

    public void refreshZeroFilters() {
        if (Vars.content != null) {
            boolean hideZeroItems = feature != null && Boolean.TRUE.equals(feature.hideZeroItemsConfig.get());
            if (hideZeroItems && coreItems != null) {
                boolean itemsChanged = false;
                Seq<Item> allItems = Vars.content.items();
                for (int i = 0; i < allItems.size; i++) {
                    Item item = allItems.get(i);
                    if (coreItems.get(item) <= 0 && usedItems.remove(item)) {
                        itemsChanged = true;
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
                }
            }
            boolean hideZeroUnits = feature != null && Boolean.TRUE.equals(feature.hideZeroUnitsConfig.get());
            if (hideZeroUnits && selectedTeam != null) {
                boolean unitsChanged = false;
                Seq<UnitType> allUnits = Vars.content.units();
                for (int i = 0; i < allUnits.size; i++) {
                    UnitType unit = allUnits.get(i);
                    int count = lastUnitCounts.get(unit, 0);
                    if (count <= 0 && usedUnits.remove(unit)) {
                        unitsChanged = true;
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
            }
        }
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

    public int getItemAmount(@Nullable Item item) {
        return item != null && coreItems != null ? coreItems.get(item) : 0;
    }

    public int getItemRate(@Nullable Item item) {
        return item != null ? rateDisplay.get(item, 0) : 0;
    }

    public String getFormattedAmount(@Nullable Item item) {
        if (item == null || coreItems == null) {
            return "0";
        }
        return UI.formatAmount(coreItems.get(item));
    }

    public String getFormattedRate(@Nullable Item item) {
        if (item == null || rateDisplay == null) {
            return "0/s";
        }
        int rate = rateDisplay.get(item, 0);
        if (rate == 0) {
            return "0/s";
        }
        String prefix = rate > 0 ? "+" : "";
        return prefix + rate + "/s";
    }

    public Color getRateColor(@Nullable Item item) {
        if (item == null || rateDisplay == null) {
            return Color.gray;
        }
        int rate = rateDisplay.get(item, 0);
        return rate > 0 ? Pal.heal : (rate < 0 ? Pal.remove : Color.gray);
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
            if (count <= 0) {
                return (feature != null && !Boolean.TRUE.equals(feature.hideZeroUnitsConfig.get())) ? "0" : "";
            }
            if (unitFormatCountCache.get(type, Integer.MIN_VALUE) == count) {
                String cached = unitFormatTextCache.get(type);
                if (cached != null) {
                    return cached;
                }
            }
            String formatted = UI.formatAmount(count);
            unitFormatCountCache.put(type, count);
            unitFormatTextCache.put(type, formatted);
            return formatted;
        } catch (Throwable t) {
            return "";
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
        long val = (long) (totalBalance * 60);
        boolean pos = totalBalance >= 0;
        if (cachedBalanceLong != val || cachedBalancePositive != pos) {
            cachedBalanceLong = val;
            cachedBalancePositive = pos;
            cachedBalanceText = (pos ? "+" : "") + UI.formatAmount(val);
        }
        return cachedBalanceText;
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
        long stored = (long) getTotalStoredPower();
        long cap = (long) getTotalPowerCapacity();
        if (cachedStoredLong != stored || cachedCapacityLong != cap) {
            cachedStoredLong = stored;
            cachedCapacityLong = cap;
            cachedStoredText = UI.formatAmount(stored) + " / " + UI.formatAmount(cap);
        }
        return cachedStoredText;
    }

    public CharSequence formatItem(@Nullable Item item) {
        if (item == null) {
            return "0";
        }
        try {
            int amount = coreItems != null ? coreItems.get(item) : 0;
            int rate = rateDisplay.get(item, 0);
            if (coreItems == null && rate == 0) {
                return "0";
            }

            boolean alwaysShow = feature == null || Boolean.TRUE.equals(feature.alwaysShowFlowRateConfig.get());
            int mode = alwaysShow ? 1 : (viewingStats ? 2 : 3);
            ItemFormatEntry entry = itemFormatCache.get(item);
            if (entry != null && entry.amount == amount && entry.rate == rate && entry.mode == mode) {
                return entry.text;
            }
            if (entry == null) {
                entry = new ItemFormatEntry();
                itemFormatCache.put(item, entry);
            }
            entry.amount = amount;
            entry.rate = rate;
            entry.mode = mode;

            if (alwaysShow) {
                entry.text = formatAmountWithRate(amount, rate);
                return entry.text;
            }

            if (viewingStats) {
                entry.text = formatRate(rate);
                return entry.text;
            }

            String color = rate < 0 ? "[scarlet]" : (rate > 0 ? "[lime]" : "[white]");
            entry.text = color + UI.formatAmount(amount);
            return entry.text;
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
        float now = Time.time;
        float elapsedTicks = lastRateUpdateTime >= 0f ? (now - lastRateUpdateTime) : 30f;
        float elapsedSeconds = elapsedTicks > 0.001f ? (elapsedTicks / 60f) : 0.5f;
        Seq<Item> allItems = Vars.content.items();
        if (lastSnapshot.size > 0 && elapsedSeconds <= 5f) {
            for (int i = 0; i < allItems.size; i++) {
                Item item = allItems.get(i);
                if (item != null) {
                    int amount = coreItems.get(item);
                    int previous = lastSnapshot.get(item, amount);
                    if (amount != previous) {
                        ratesOrAmountsChanged = true;
                    }
                    int newRate = Math.round((amount - previous) / elapsedSeconds);
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
        lastRateUpdateTime = now;
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
        tempPowerGraphs.clear();
        if (selectedTeam.data() != null && selectedTeam.data().buildings != null) {
            Seq<Building> teamBuildings = selectedTeam.data().buildings;
            for (int i = 0; i < teamBuildings.size; i++) {
                Building b = teamBuildings.get(i);
                if (b != null) {
                    powerGraphCollector.get(b);
                }
            }
        } else if (Groups.build != null) {
            Groups.build.each(powerGraphCollector);
        }

        tempGraphSeq.clear();
        for (PowerGraph graph : tempPowerGraphs) {
            tempGraphSeq.add(graph);
        }
        tempGraphSeq.sort(GRAPH_COMPARATOR);

        if (!tempGraphSeq.equals(teamGraphs)) {
            teamGraphs.set(new Seq<>(tempGraphSeq));
            teamGraphsSignal.set(new Seq<>(tempGraphSeq));
            graphsChanged = true;
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
        String color = rate < 0 ? "[scarlet]" : (rate > 0 ? "[lime]" : "[white]");
        return color + UI.formatAmount(amount) + " [gray](" + formatRate(rate) + "[gray])[]";
    }

    private String formatRate(int rate) {
        if (rate == 0) {
            return "0/s";
        }
        String prefix = rate > 0 ? "[lime]+" : "[scarlet]";
        return prefix + rate + "[gray]/s[]";
    }
}
