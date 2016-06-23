package vg.civcraft.mc.prisonpearl;

import java.util.logging.Level;

import org.bukkit.Bukkit;

import vg.civcraft.mc.civmodcore.ACivMod;
import vg.civcraft.mc.prisonpearl.command.PrisonPearlCommandHandler;
import vg.civcraft.mc.prisonpearl.database.DataBaseHandler;
import vg.civcraft.mc.prisonpearl.listener.AltsListListener;
import vg.civcraft.mc.prisonpearl.listener.BanListener;
import vg.civcraft.mc.prisonpearl.listener.BetterShardsListener;
import vg.civcraft.mc.prisonpearl.listener.CombatTagListener;
import vg.civcraft.mc.prisonpearl.listener.DamageListener;
import vg.civcraft.mc.prisonpearl.listener.MercuryListener;
import vg.civcraft.mc.prisonpearl.listener.PlayerListener;
import vg.civcraft.mc.prisonpearl.listener.PrisonPortaledPlayerListener;
import vg.civcraft.mc.prisonpearl.listener.SummonListener;
import vg.civcraft.mc.prisonpearl.managers.AltsListManager;
import vg.civcraft.mc.prisonpearl.managers.BanManager;
import vg.civcraft.mc.prisonpearl.managers.BroadcastManager;
import vg.civcraft.mc.prisonpearl.managers.CombatTagManager;
import vg.civcraft.mc.prisonpearl.managers.DamageLogManager;
import vg.civcraft.mc.prisonpearl.managers.MercuryManager;
import vg.civcraft.mc.prisonpearl.managers.NameLayerManager;
import vg.civcraft.mc.prisonpearl.managers.PrisonPearlManager;
import vg.civcraft.mc.prisonpearl.managers.PrisonPortaledPlayerManager;
import vg.civcraft.mc.prisonpearl.managers.SummonManager;
import vg.civcraft.mc.prisonpearl.managers.WorldBorderManager;

public class PrisonPearlPlugin extends ACivMod {

	private static PrisonPearlPlugin plugin;
	private static DataBaseHandler dbHandle; // This class handles both the save/load and getting storage types.
	
	private static AltsListManager altsManager;
	private static BanManager banManager;
	private static BroadcastManager broadManager;
	private static CombatTagManager combatManager;
	private static DamageLogManager damageManager;
	private static MercuryManager mercuryManager;
	private static NameLayerManager namelayerManager;
	private static WorldBorderManager worldborderManager;
	private static PrisonPearlManager pearlManager;
	private static PrisonPortaledPlayerManager portaledManager;
	private static SummonManager summonManager;
	
	public void onEnable() {
		super.onEnable();
		plugin = this;
		
		// It would be best to load the storage first so all managers have it available;
		dbHandle = new DataBaseHandler();
		dbHandle.getSaveLoadHandler().load();
		handleManagers();
		handleListeners();
		// Register commands.
		PrisonPearlCommandHandler handle = new PrisonPearlCommandHandler();
		setCommandHandler(handle);
		handle.registerCommands();
		new PrisonPearlUtil();
		
		pearlManager.feedPearls();
	}
	
	public void onDisable() {
		super.onDisable();
		dbHandle.getSaveLoadHandler().save();
	}
	
	private void handleManagers() {
		altsManager = new AltsListManager();
		banManager = BanManager.initialize();
		broadManager = new BroadcastManager();
		combatManager = new CombatTagManager(plugin.getServer(), plugin.getLogger());
		damageManager = new DamageLogManager();
		mercuryManager = new MercuryManager();
		namelayerManager = new NameLayerManager();
		worldborderManager = new WorldBorderManager();
		pearlManager = new PrisonPearlManager();
		portaledManager = new PrisonPortaledPlayerManager();
		summonManager = new SummonManager();
	}
	
	private void handleListeners() {
		new AltsListListener();
		new BanListener();
		new DamageListener();
		if (isMercuryEnabled())
			new MercuryListener();
		new PlayerListener();
		new PrisonPortaledPlayerListener();
		if (isCombatTagPlusEnabled())
			new CombatTagListener();
		if (isBetterShardsEnabled())
			new BetterShardsListener();
		new SummonListener();
	}
	
	@Override
	protected String getPluginName() {
		return "PrisonPearl";
	}
	
	public static DataBaseHandler getDBHandler() {
		return dbHandle;
	}
	
	public static BanManager getBanManager() {
		return banManager;
	}
	
	public static AltsListManager getAltsListManager() {
		return altsManager;
	}
	
	public static BroadcastManager getBroadcastManager() {
		return broadManager;
	}
	
	public static CombatTagManager getCombatTagManager() {
		return combatManager;
	}
	
	public static DamageLogManager getDamageLogManager() {
		return damageManager;
	}
	
	public static MercuryManager getMercuryManager() {
		return mercuryManager;
	}
	
	public static NameLayerManager getNameLayerManager() {
		return namelayerManager;
	}
	
	public static PrisonPearlManager getPrisonPearlManager() {
		return pearlManager;
	}
	
	public static PrisonPortaledPlayerManager getPrisonPortaledPlayerManager() {
		return portaledManager;
	}
	
	public static SummonManager getSummonManager() {
		return summonManager;
	}
	
	public static WorldBorderManager getWorldBorderManager() {
		return worldborderManager;
	}
	
	public static PrisonPearlPlugin getInstance() {
		return plugin;
	}
	
	public static void log(String message) {
		plugin.getLogger().log(Level.INFO, message);
	}
	
	public static boolean isNameLayerEnabled() {
		return Bukkit.getPluginManager().isPluginEnabled("NameLayer");
	}
	
	public static boolean isCBanManagementEnabled() {
		return Bukkit.getPluginManager().isPluginEnabled("CBanManagement");
	}
	
	public static boolean isMercuryEnabled() {
		return Bukkit.getPluginManager().isPluginEnabled("Mercury");
	}
	
	public static boolean isBetterShardsEnabled() {
		return Bukkit.getPluginManager().isPluginEnabled("BetterShards");
	}
	
	public static boolean isWorldBorderEnabled() {
		return Bukkit.getPluginManager().isPluginEnabled("WorldBorder");
	}

	public static boolean isCombatTagPlusEnabled() {
		return Bukkit.getPluginManager().isPluginEnabled("CombatTagPlus");
	}
}
