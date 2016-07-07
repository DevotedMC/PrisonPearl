package vg.civcraft.mc.prisonpearl;

import java.util.List;

import vg.civcraft.mc.civmodcore.annotations.CivConfig;
import vg.civcraft.mc.civmodcore.annotations.CivConfigType;

public class PrisonPearlConfig {

	@CivConfig(name = "prison.reset_bed", def = "false", type = CivConfigType.Bool)
	public static boolean isPrisonResetBed() {
		return PrisonPearlPlugin.getInstance().GetConfig().get("prison.reset_bed").getBool();
	}
	
	@CivConfig(name = "prison_grant_perms", def = "", type = CivConfigType.String_List)
	public static List<String> getPrisonGrantPerms() {
		return PrisonPearlPlugin.getInstance().GetConfig().get("prison_grant_perms").getStringList();
	}

	@CivConfig(name = "prison_deny_perms", def = "", type = CivConfigType.String_List)
	public static List<String> getPrisonDenyPerms() {
		return PrisonPearlPlugin.getInstance().GetConfig().get("prison_deny_perms").getStringList();
	}
	
	@CivConfig(name = "prison.world", def = "world_the_end", type = CivConfigType.String)
	public static String getImprisonWorldName() {
		return PrisonPearlPlugin.getInstance().GetConfig().get("prison.world").getString();
	}
	
	@CivConfig(name = "prison.server", def = "bukkit", type = CivConfigType.String)
	public static String getImprisonServerName() {
		return PrisonPearlPlugin.getInstance().GetConfig().get("prison.server").getString();
	}
	
	@CivConfig(name = "database.mysql.username", def = "bukkit", type = CivConfigType.String)
	public static String getUsername() {
		return PrisonPearlPlugin.getInstance().GetConfig().get("database.mysql.username").getString();
	}
	
	@CivConfig(name = "database.mysql.password", def = "", type = CivConfigType.String)
	public static String getPassword() {
		return PrisonPearlPlugin.getInstance().GetConfig().get("database.mysql.password").getString();
	}
	
	@CivConfig(name = "database.mysql.dbname", def = "bukkit", type = CivConfigType.String)
	public static String getDBName() {
		return PrisonPearlPlugin.getInstance().GetConfig().get("database.mysql.dbname").getString();
	}
	
	@CivConfig(name = "database.mysql.host", def = "localhost", type = CivConfigType.String)
	public static String getHost() {
		return PrisonPearlPlugin.getInstance().GetConfig().get("database.mysql.host").getString();
	}
	
	@CivConfig(name = "database.mysql.port", def = "3306", type = CivConfigType.Int)
	public static int getPort() {
		return PrisonPearlPlugin.getInstance().GetConfig().get("database.mysql.port").getInt();
	}
	
	@CivConfig(name = "database.type", def = "0", type = CivConfigType.Int)
	public static int getDatabaseType() {
		return PrisonPearlPlugin.getInstance().GetConfig().get("database.type").getInt();
	}
	
	@CivConfig(name = "free_tppearl", def = "true", type = CivConfigType.Bool)
	public static boolean shouldTpPearlOnFree() {
		return PrisonPearlPlugin.getInstance().GetConfig().get("free_tppearl").getBool();
	}
	
	@CivConfig(name = "prison.clearinventoryonsummon", def = "true", type = CivConfigType.Bool)
	public static boolean shouldPpsummonClearInventory() {
		return PrisonPearlPlugin.getInstance().GetConfig().get("prison.clearinventoryonsummon").getBool();
	}
	
	@CivConfig(name = "prison.ppreturn_kill", def = "true", type = CivConfigType.Bool)
	public static boolean getShouldPPReturnKill() {
		return PrisonPearlPlugin.getInstance().GetConfig().get("prison.ppreturn_kill").getBool();
	}
	
	@CivConfig(name = "prison.ppsummonpearls", def = "true", type = CivConfigType.Bool)
	public static boolean shouldPpsummonLeavePearls() {
		return PrisonPearlPlugin.getInstance().GetConfig().get("prison.ppsummonpearls").getBool();
	}
	
	@CivConfig(name = "prison.motd", def = "You have been imprisoned!", type = CivConfigType.String_List)
	public static List<String> getPrisonMotd() {
		return PrisonPearlPlugin.getInstance().GetConfig().get("prison.motd").getStringList();
	}
	
	@CivConfig(name = "ignore_feed.seconds", def = "0" , type = CivConfigType.Long)
	public static long getIgnoreFeedSecond() {
		return PrisonPearlPlugin.getInstance().GetConfig().get("ignore_feed.seconds").getLong();
	}
	
	@CivConfig(name = "ignore_feed.hours", def = "0" , type = CivConfigType.Long)
	public static long getIngoreFeedHours() {
		return PrisonPearlPlugin.getInstance().GetConfig().get("ignore_feed.hours").getLong();
	}
	
	@CivConfig(name = "ignore_feed.days", def = "0" , type = CivConfigType.Long)
	public static long getIngoreFeedDays() {
		return PrisonPearlPlugin.getInstance().GetConfig().get("ignore_feed.days").getLong();
	}
	
	@CivConfig(name = "ignore_feed.feed_delay", def = "72000000" , type = CivConfigType.Long)
	public static long getIgnoreFeedDelay() {
		return PrisonPearlPlugin.getInstance().GetConfig().get("ignore_feed.feed_delay").getLong();
	}
	
	@CivConfig(name = "upkeep.resource", def = "263", type = CivConfigType.Int)
	public static int getResourceUpkeepMaterial() {
		return PrisonPearlPlugin.getInstance().GetConfig().get("upkeep.resource").getInt();
	}
	
	@CivConfig(name = "upkeep.quantity", def = "4" , type = CivConfigType.Int)
	public static int getResourceUpkeepAmount() {
		return PrisonPearlPlugin.getInstance().GetConfig().get("upkeep.quantity").getInt();
	}
	
	@CivConfig(name = "autofree_worldborder", def = "true", type = CivConfigType.Bool)
	public static boolean getShouldAutoFreeWorldBorder() {
		return PrisonPearlPlugin.getInstance().GetConfig().get("autofree_worldborder").getBool();
	}
	
	@CivConfig(name = "prison_musthotbar", def = "true", type = CivConfigType.Bool)
	public static boolean getMustPrisonPearlHotBar() {
		return PrisonPearlPlugin.getInstance().GetConfig().get("prison_musthotbar").getBool();
	}
	
	@CivConfig(name = "prison_stealing", def = "true", type = CivConfigType.Bool)
	public static boolean getAllowPrisonStealing() {
		return PrisonPearlPlugin.getInstance().GetConfig().get("prison_stealing").getBool();
	}
	
	@CivConfig(name = "damagelog_min", def = "3" , type = CivConfigType.Int)
	public static int getDamageLogMin() {
		return PrisonPearlPlugin.getInstance().GetConfig().get("damagelog_min").getInt();
	}
	
	@CivConfig(name = "damagelog_ticks", def = "600" , type = CivConfigType.Int)
	public static int getDamagelogTicks() {
		return PrisonPearlPlugin.getInstance().GetConfig().get("damagelog_ticks").getInt();
	}
	
	@CivConfig(name = "summon_damage_radius", def = "20" , type = CivConfigType.Int)
	public static int getSummonDamageRadius() {
		return PrisonPearlPlugin.getInstance().GetConfig().get("summon_damage_radius").getInt();
	}
	
	@CivConfig(name = "summon_damage_ticks", def = "20" , type = CivConfigType.Int)
	public static int getSummonDamageTicks() {
		return PrisonPearlPlugin.getInstance().GetConfig().get("summon_damage_ticks").getInt();
	}
	
	@CivConfig(name = "summon_damage_amt", def = "2" , type = CivConfigType.Int)
	public static int getSummonDamageAmount() {
		return PrisonPearlPlugin.getInstance().GetConfig().get("summon_damage_amt").getInt();
	}
	
	@CivConfig(name = "kickMessage", def = "You have too many imprisoned alts! "
			+ "If you think this is an error, please message the mods on /r/civcraft", type = CivConfigType.String)
	public static String getKickMessage() {
		return PrisonPearlPlugin.getInstance().GetConfig().get("kickMessage").getString();
	}
	
	@CivConfig(name = "alts.enabled", def = "true", type = CivConfigType.Bool)
	public static boolean getShouldEnableAltsManager(){
		return PrisonPearlPlugin.getInstance().GetConfig().get("alts.enabled").getBool();
	}
	
	@CivConfig(name = "alts.max_imprisoned", def = "2", type = CivConfigType.Int)
	public static int getMaxAltsAllowed() {
		return PrisonPearlPlugin.getInstance().GetConfig().get("alts.max_imprisoned").getInt();
	}
	
	@CivConfig(name = "deny_pearls_worlds", def = "", type = CivConfigType.String_List)
	public static List<String> getPearlDenyTransferWorlds() {
		return PrisonPearlPlugin.getInstance().GetConfig().get("deny_pearls_worlds").getStringList();
	}
	
	@CivConfig(name = "prison.unloadTimerTicks", def = "1200", type = CivConfigType.Int)
	public static int getPrisonUnloadTimerTicks() {
		return PrisonPearlPlugin.getInstance().GetConfig().get("prison.unloadTimerTicks").getInt();
	}
	
	@CivConfig(name = "mercury.pearlupdateticks", def = "100", type = CivConfigType.Int)
	public static int getMercuryUpdateMessageTicks() {
		return PrisonPearlPlugin.getInstance().GetConfig().get("mercury.pearlupdateticks").getInt();
	}
	
	@CivConfig(name = "ppsaveticks", def = "1200", type = CivConfigType.Int)
	public static int getPPSaveTicks() {
		return PrisonPearlPlugin.getInstance().GetConfig().get("ppsaveticks").getInt();
	}

	@CivConfig(name = "debug", def = "false", type = CivConfigType.Bool)
	public static boolean isDebug() {
		return PrisonPearlPlugin.getInstance().GetConfig().get("debug").getBool();
	}
}
