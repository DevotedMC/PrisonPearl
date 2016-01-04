package com.untamedears.PrisonPearl;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.untamedears.PrisonPearl.managers.BroadcastManager;
import com.untamedears.PrisonPearl.managers.DamageLogManager;
import com.untamedears.PrisonPearl.managers.PrisonPearlManager;
import com.untamedears.PrisonPearl.managers.SummonManager;
import com.untamedears.PrisonPearl.managers.WorldBorderManager;

import vg.civcraft.mc.mercury.MercuryAPI;
import vg.civcraft.mc.namelayer.NameAPI;

class PrisonPearlCommands implements CommandExecutor {
    private final PrisonPearlPlugin plugin;
    private final PrisonPearlStorage pearls;
    private final DamageLogManager damageman;
    private final PrisonPearlManager pearlman;
    private final SummonManager summonman;
    private final BroadcastManager broadcastman;
    private final WorldBorderManager wbManager;
    private boolean isNameLayer;
    private boolean isMercury;

    public PrisonPearlCommands(PrisonPearlPlugin plugin, DamageLogManager damageman, PrisonPearlStorage pearls, PrisonPearlManager pearlman, SummonManager summonman, BroadcastManager broadcastman, WorldBorderManager wbManager) {
        this.plugin = plugin;
        this.pearls = pearls;
        this.damageman = damageman;
        this.pearlman = pearlman;
        this.summonman = summonman;
        this.broadcastman = broadcastman;
        this.wbManager = wbManager;
        isNameLayer = Bukkit.getPluginManager().isPluginEnabled("NameLayer");
        isMercury = plugin.isMercuryLoaded();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (label.equalsIgnoreCase("pplocate") || label.equalsIgnoreCase("ppl")) {
            if(sender.hasPermission("prisonpearl.normal.pplocate")) {// sees if the players has the permission.
                return locateCmd(sender, args, false);
            } else {
            	// if players doesn't have permission, broadcasts message saying what they are missing.
            	sender.sendMessage("You Do not have Permissions prisonpearl.normal.pplocate");
            }
        } else if (label.equalsIgnoreCase("pplocateany")) {
            if (sender.hasPermission("prisonpearl.pplocateany")) {// sees if the players has the permission.
                return locateCmd(sender, args, true);
            } else {
            	// if players doesn't have permission, broadcasts message saying what they are missing.
            	sender.sendMessage("You Do not have Permissions prisonpearl.pplocateany");
            }
        } else if (label.equalsIgnoreCase("ppfree") || label.equalsIgnoreCase("ppf")) {
            if(sender.hasPermission("prisonpearl.normal.ppfree")) {// sees if the players has the permission.
                return freeCmd(sender, args, false);
            } else {
            	// if players doesn't have permission, broadcasts message saying what they are missing.
            	sender.sendMessage("You Do not have Permissions prisonpearl.normal.ppfree");
            } 
        } else if (label.equalsIgnoreCase("ppfreeany")) {
            if(sender.hasPermission("prisonpearl.ppfreeany")) {// sees if the players has the permission.
                return freeCmd(sender, args, true);
            } else {
            	// if players doesn't have permission, broadcasts message saying what they are missing.
            	sender.sendMessage("You Do not have Permissions prisonpearl.ppfreeany");
            } 
        } else if (label.equalsIgnoreCase("ppsummon") || label.equalsIgnoreCase("pps")) {
            if(sender.hasPermission("prisonpearl.normal.ppsummon")) {// sees if the players has the permission.
                return summonCmd(sender, args);
            } else { 
            	// if players doesn't have permission, broadcasts message saying what they are missing.
            	sender.sendMessage("You Do not have Permissions prisonpearl.normal.ppsummon");
            }
        } else if (label.equalsIgnoreCase("ppreturn") || label.equalsIgnoreCase("ppr")) {
            if(sender.hasPermission("prisonpearl.normal.ppreturn")) {// sees if the players has the permission.
                return returnCmd(sender, args);
            } else { 
            	// if players doesn't have permission, broadcasts message saying what they are missing.
            	sender.sendMessage("You Do not have Permissions prisonpearl.normal.ppreturn");
            }
        } else if (label.equalsIgnoreCase("ppkill") || label.equalsIgnoreCase("ppk")) {
            if(sender.hasPermission("prisonpearl.normal.ppkill")) {// sees if the players has the permission.
                return killCmd(sender, args);
            } else {
            	// if players doesn't have permission, broadcasts message saying what they are missing.
            	sender.sendMessage("You Do not have Permissions prisonpearl.normal.ppkill");
            }
        } else if (label.equalsIgnoreCase("ppsave")) {
            if(sender.hasPermission("prisonpearl.ppsave")) {// sees if the players has the permission.
                return saveCmd(sender, args);
            } else { 
            	// if players doesn't have permission, broadcasts message saying what they are missing.
            	sender.sendMessage("You Do not have Permissions prisonpearl.ppsave");
            }

        } else if (label.equalsIgnoreCase("ppimprisonany")) {
            if(sender.hasPermission("prisonpearl.ppimprisonany")) {// sees if the players has the permission.
                return imprisonCmd(sender, args);
            } else { 
            	// if players doesn't have permission, broadcasts message saying what they are missing.
            	sender.sendMessage("You Do not have Permissions prisonpearl.ppimprisonany");
            }
        } else if (label.equalsIgnoreCase("ppbroadcast")) {
            if(sender.hasPermission("prisonpearl.normal.ppbroadcast")) {// sees if the players has the permission.
                return broadcastCmd(sender, args);
            } else { 
            	// if players doesn't have permission, broadcasts message saying what they are missing.
            	sender.sendMessage("You Do not have Permissions prisonpearl.normal.ppbroadcast");
            }
        } else if (label.equalsIgnoreCase("ppconfirm")) {
            if(sender.hasPermission("prisonpearl.normal.ppconfirm")) {// sees if the players has the permission.
                return confirmCmd(sender, args);
            } else { 
            	// if players doesn't have permission, broadcasts message saying what they are missing.
            	sender.sendMessage("You Do not have Permissions prisonpearl.normal.ppconfirm");
            }
        } else if (label.equalsIgnoreCase("ppsilence")) {
            if(sender.hasPermission("prisonpearl.normal.ppsilence")) {// sees if the players has the permission.
                return silenceCmd(sender, args);
            } else { 
            	// if players doesn't have permission, broadcasts message saying what they are missing.
            	sender.sendMessage("You Do not have Permissions prisonpearl.normal.ppsilence");
            }
        } else if (label.equalsIgnoreCase("pploadalts")) {
            if(sender.hasPermission("prisonpearl.pploadalts")) {// sees if the players has the permission.
                return reloadAlts(sender);
            } else {
            	// if players doesn't have permission, broadcasts message saying what they are missing.
            	sender.sendMessage("You Do not have Permissions prisonpearl.pploadalts");
            }
        } else if (label.equalsIgnoreCase("ppcheckall")) {
        	if(sender instanceof ConsoleCommandSender){         
                return checkAll(sender);
            } else { 
            	// if players doesn't have permission, broadcasts message saying what they are missing.
            	sender.sendMessage("You Do not have Permissions prisonpearl.ppcheckall");
            }

        } else if (label.equalsIgnoreCase("ppcheck")) {
        	if(sender instanceof ConsoleCommandSender){         
                return check(sender, args);
            } else { 
            	// if players doesn't have permission, broadcasts message saying what they are missing.
            	sender.sendMessage("You Do not have Permissions prisonpearl.ppcheck");
            }
        } else if (label.equalsIgnoreCase("kill")) {
            if(sender.hasPermission("prisonpearl.kill")) {// sees if the players has the permission.
                return kill();
            } else { 
            	// if players doesn't have permission, broadcasts message saying what they are missing.
            	sender.sendMessage("You Do not have Permissions prisonpearl.kill");
            }
        } else if (label.equalsIgnoreCase("ppsetdist")) {
            if(sender.hasPermission("prisonpearl.normal.ppsetdist")) {// sees if the players has the permission.
                return setDistCmd(sender, args);
            } else { 
            	// if players doesn't have permission, broadcasts message saying what they are missing.
            	sender.sendMessage("You Do not have Permissions prisonpearl.normal.ppsetdist");
            }
        } else if (label.equalsIgnoreCase("ppsetdamage")) {
            if(sender.hasPermission("prisonpearl.normal.ppsetdamage")) {// sees if the players has the permission.
                return setDamageCmd(sender, args);
            } else { 
            	// if players doesn't have permission, broadcasts message saying what they are missing.
            	sender.sendMessage("You Do not have Permissions prisonpearl.normal.ppsetdamage");
            }
        } else if (label.equalsIgnoreCase("pptogglespeech")) {
            if(sender.hasPermission("prisonpearl.normal.pptogglespeech")) {// sees if the players has the permission.
                return toggleSpeechCmd(sender, args);
            } else { 
            	// if players doesn't have permission, broadcasts message saying what they are missing.
            	sender.sendMessage("You Do not have Permissions prisonpearl.normal.pptogglespeech");
            }
        } else if (label.equalsIgnoreCase("pptoggledamage")) {
            if(sender.hasPermission("prisonpearl.normal.pptoggledamage")) {// sees if the players has the permission.
                return toggleDamageCmd(sender, args);
            } else { 
            	// if players doesn't have permission, broadcasts message saying what they are missing.
            	sender.sendMessage("You Do not have Permissions prisonpearl.normal.pptoggledamage");
            }
        } else if (label.equalsIgnoreCase("pptoggleblocks")) {
            if(sender.hasPermission("prisonpearl.normal.pptoggleblocks")) {// sees if the players has the permission.
                return toggleBlocksCmd(sender, args);
            } else { 
            	// if players doesn't have permission, broadcasts message saying what they are missing.
            	sender.sendMessage("You Do not have Permissions prisonpearl.normal.pptoggleblocks");
            }
        } else if (label.equalsIgnoreCase("ppsetmotd")) {
            if(sender.hasPermission("prisonpearl.normal.ppsetmotd")) {// sees if the players has the permission.
                return setMotdCmd(sender, args);
            } else { 
            	// if players doesn't have permission, broadcasts message saying what they are missing.
            	sender.sendMessage("You Do not have Permissions prisonpearl.normal.ppsetmotd");
            }
        } else if (label.equalsIgnoreCase("ppfeed")) {
        	pearlman.prisonCommandEvent("ppfeed");
            return feedCmd(sender, args, false);
        } else if (label.equalsIgnoreCase("pprestore")) {
            return restoreCmd(sender, args, false);
        }else if (label.equalsIgnoreCase("ppgetalts")) {
            if(sender.hasPermission("prisonpearl.getalts")) {// sees if the players has the permission.
                return getAltsByName(sender, args);
            } else { 
            	// if players doesn't have permission, broadcasts message saying what they are missing.
            	sender.sendMessage("You Do not have Permissions prisonpearl.getalts");
            }
        } else if (label.equalsIgnoreCase("ppsetalts")) {
            if(sender.hasPermission("prisonpearl.setalts")) {// sees if the players has the permission.
                return setAlts(sender, args);
            } else { 
            	// if players doesn't have permission, broadcasts message saying what they are missing.
            	sender.sendMessage("You Do not have Permissions prisonpearl.setalts");
            }
        } else if (label.equalsIgnoreCase("ppban")) {
        	return banAlt(sender, args);
        } else if (label.equalsIgnoreCase("ppunban")) {
        	return unBanAlt(sender, args);
        } else if (label.equalsIgnoreCase("ppwb")) {
        	if(sender.hasPermission("prisonpearl.wb")) {// sees if the players has the permission.
                return WhiteListedLocations(sender, args);
            } else { 
            	// if players doesn't have permission, broadcasts message saying what they are missing.
            	sender.sendMessage("You Do not have Permissions prisonpearl.wb");
            }
        }
        return false;
    }

	private boolean WhiteListedLocations(CommandSender sender, String[] args) {
		if(args.length==0){
			return false;
		}
		if(args[0].equals("add")) {
			if(args.length==1&&sender instanceof Player) {
				Player player = (Player) sender;
				Location loc = new Location(player.getWorld(), player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ());
				if(wbManager.addWhitelistedLocation(loc)) {
					sender.sendMessage(ChatColor.GREEN + "Location was added.");
				} else {
					sender.sendMessage(ChatColor.GREEN + "This location is already on the whitelist.");
				}
				return true;
			} else if(args.length == 4) {
				if(!wbManager.addWhitelistedLocation(args[1], args[2], args[3])) {
					return false;
				}
				sender.sendMessage(ChatColor.GREEN + "Location was added.");
				return true;
			}
		}else if(args[0].equals("remove")) {
			if(args.length==1&&sender instanceof Player) {
				Player player = (Player) sender;
				Location loc = new Location(player.getWorld(), player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ());
				if(wbManager.removeWhitelistedLocation(loc)) {
					sender.sendMessage(ChatColor.GREEN + "Location was removed.");
				} else {
					sender.sendMessage(ChatColor.RED + "Location wasnt found.");
				}
				return true;
			} else if(args.length == 4) {
				if(!wbManager.removeWhitelistedLocation(args[1], args[2], args[3])) {
					sender.sendMessage(ChatColor.RED + "Location wasnt found.");
					return false;
				}
				sender.sendMessage(ChatColor.GREEN + "Location was removed.");
				return true;
			}
		}
		return false;
	}

	private boolean unBanAlt(CommandSender sender, String[] args) {
		String name = args[0];
		UUID uuid = null;
		if (isNameLayer)
			uuid = NameAPI.getUUID(name);
		else
			uuid = Bukkit.getOfflinePlayer(name).getUniqueId();
		UUID[] alts = plugin.getAltsList().getAltsArray(uuid);
		int count = 1;
		for (UUID alt: alts){
			plugin.getBanManager().pardon(alt);
			count++;
		}
		sender.sendMessage(ChatColor.GREEN + "" + count +" accounts were unbanned or left alone.");
		return true;
	}

	private boolean banAlt(CommandSender sender, String[] args) {
		String name = args[0];
		UUID uuid = null;
		if (isNameLayer)
			uuid = NameAPI.getUUID(name);
		else
			uuid = Bukkit.getOfflinePlayer(name).getUniqueId();
		UUID[] alts = plugin.getAltsList().getAltsArray(uuid);
		int count = 1;
		for (UUID alt: alts){
			plugin.getBanManager().ban(alt);
			count++;
		}
		sender.sendMessage(ChatColor.GREEN + "" + count +" accounts were banned or left alone.");
		return true;
	}

	private boolean setAlts(CommandSender sender, String[] args) {
    	if (args.length < 1)
    	{
    		return false;
    	}
    	String[] confirmedAlts = new String[args.length-1];
    	System.arraycopy(args, 1, confirmedAlts, 0, confirmedAlts.length);
    	try {
			plugin.setAlts(UUID.fromString(args[0]), toUUIDList(confirmedAlts));
			return true;
		} catch (IOException e) {
			sender.sendMessage("IOException accured when trying to write to excluded_alts.txt");
			e.printStackTrace();
			return false;
		}
	}

    private UUID[] toUUIDList(String[] stringids)
    {
		UUID uuids[] = new UUID[stringids.length];
		for(int i = 0; i<stringids.length; i++)
		{
			uuids[i] = UUID.fromString(stringids[i]);
		}
		return uuids;
    }
	private boolean getAltsByName(CommandSender sender, String[] args) {
    	if (args.length != 1)
    	{
    		return false;
    	}
    	OfflinePlayer player = Bukkit.getOfflinePlayer(args[0]);
    	UUID[] alts = plugin.getAltsList().getAltsArray(player.getUniqueId());
    	if (alts.length == 0)
    	{
    		sender.sendMessage("No information about " + args[0]);
    		return false;
    	}
    	else
    	{
    		String message = "";
    		for (int x = 0; x < alts.length; x++)
    		{
    			message = message + alts[x] + ", ";
    		}
    		sender.sendMessage(message);
    		return true;
    	}
	}

	private boolean restoreCmd(CommandSender sender, String args[], boolean any){
        if ((sender instanceof Player)) {
            sender.sendMessage("Must use [[restore at the console");
            return true;
        }
        if (!args[0].isEmpty()){
            sender.sendMessage("Restoring from " + args[0]);
            sender.sendMessage(pearls.restorePearls(pearlman, args[0]));
        }else{
            sender.sendMessage("Restoring from most recent record...");
            sender.sendMessage(pearls.restorePearls(pearlman, null));
        }
        return true;
    }

    private boolean feedCmd(CommandSender sender, String args[], boolean any) {
        if ((sender instanceof Player)) {
            sender.sendMessage("Must use ppfeed at the console");
            return true;
        }
        sender.sendMessage("Feeding all pearls: " + pearls.getPearlCount());
        sender.sendMessage(pearls.feedPearls(pearlman));
        return true;
    }

    private PrisonPearl setCmd(CommandSender sender, String[] args) {
        PrisonPearl pp;
        if (!(sender instanceof Player)) {
            sender.sendMessage("ppset cannot be used at the console");
            return null;
        }
        String[] anArray = {};
        Player player = (Player)sender;
        pp = getCommandPearl(player, anArray, 1);
        if (pp == null){
            return null;
        }
        if (args.length > 1)
            return null;
        if (pp.getImprisonedPlayer() != null && pp.getImprisonedPlayer().isDead()) {
            sender.sendMessage(pp.getImprisonedName() + " is dead. Bring him back to try again.");
            return null;
        } else if (pp.getImprisonedPlayer() == player) {
            sender.sendMessage("You cannot alter your own pearl!");
            return null;
        } else if (!(summonman.isSummoned(pp))) {
            sender.sendMessage(pp.getImprisonedName() + " is not summoned.");
            return null;
        }
        return pp;
    }

    private boolean setDistCmd(CommandSender sender, String args[]) {
        PrisonPearl pp = setCmd(sender, args);
        if (pp == null) {
            return false;
        }
        int dist;
        try {
            dist = Integer.parseInt(args[0]);
        } catch (Exception ex) {
        	if (ex instanceof ArrayIndexOutOfBoundsException){
        		sender.sendMessage("You must specify a distance.");
        		return true;
        	}
            sender.sendMessage("Invalid distance " + args[0]);
            return false;
        }
        summonman.getSummon(pp.getImprisonedId()).setAllowedDistance(dist);
        sender.sendMessage(pp.getImprisonedName() + "'s allowed distance set to " + args[0]);
        return true;
    }

    private boolean setDamageCmd(CommandSender sender, String args[]) {
        PrisonPearl pp = setCmd(sender, args);
        if (pp == null) {
            return false;
        }
        int dmg;
        try {
            dmg = Integer.parseInt(args[0]);
        } catch (Exception ex) {
            sender.sendMessage("Invalid damage " + args[0]);
            return false;
        }
        summonman.getSummon(pp.getImprisonedId()).setDamageAmount(dmg);
        sender.sendMessage(pp.getImprisonedName() + "'s damage amount set to " + args[0]);
        return true;
    }

    private boolean toggleSpeechCmd(CommandSender sender, String args[]) {
        PrisonPearl pp = setCmd(sender, args);
        if (pp == null) {
            return false;
        }
        boolean speak = summonman.getSummon(pp.getImprisonedId()).isCanSpeak();
        summonman.getSummon(pp.getImprisonedId()).setCanSpeak(!speak);
        sender.sendMessage(pp.getImprisonedName() + " ability to speak set to " + !speak);
        return true;
    }

    private boolean toggleDamageCmd(CommandSender sender, String args[]) {
        PrisonPearl pp = setCmd(sender, args);
        if (pp == null) {
            return false;
        }
        boolean damage = summonman.getSummon(pp.getImprisonedId()).isCanDealDamage();
        summonman.getSummon(pp.getImprisonedId()).setCanDealDamage(!damage);
        sender.sendMessage(pp.getImprisonedName() + " ability to deal damage set to " + !damage);
        return true;
    }

    private boolean toggleBlocksCmd(CommandSender sender, String args[]) {
        PrisonPearl pp = setCmd(sender, args);
        if (pp == null) {
            return false;
        }
        boolean block = summonman.getSummon(pp.getImprisonedId()).isCanBreakBlocks();
        summonman.getSummon(pp.getImprisonedId()).setCanBreakBlocks(!block);
        sender.sendMessage(pp.getImprisonedName() + " ability to break blocks set to " + !block);
        return true;
    }

    private boolean setMotdCmd(CommandSender sender, String args[]) {
        PrisonPearl pp;
        if (!(sender instanceof Player)) {
            sender.sendMessage("ppset cannot be used at the console");
            return true;
        }
        String[] anArray = {};
        Player player = (Player)sender;
        pp = getCommandPearl(player, anArray, 1);
        if (pp == null) {
            return false;
        }
        String s = "";
        for (String arg : args) {
            s = s.concat(arg + " ");
        }
        pp.setMotd(s);
        sender.sendMessage(pp.getImprisonedName() + "'s Message of the Day set to " + s);
        return true;
    }   

    private boolean locateCmd(CommandSender sender, String args[], boolean any) {
        String name_is;
        String name_possesive;
        PrisonPearl pp;
        if (!any) {
            if (args.length != 0)
                return false;
            if (!(sender instanceof Player)) {
                sender.sendMessage("Must use pplocateany at the console");
                return true;
            }
            name_is = "You are";
            name_possesive = "Your";
            pp = pearls.getByImprisoned((Player)sender);
        } else {
            if (args.length != 1)
                return false;
            name_is = args[0] + " is";
            name_possesive = args[0] + "'s";
            UUID uuid = null;
            if (isNameLayer)
            	uuid = NameAPI.getUUID(args[0]);
            else
            	uuid = Bukkit.getOfflinePlayer(args[0]).getUniqueId();
            pp = pearls.getByImprisoned(uuid);
        }
        if (pp != null) {
            if (!pp.verifyLocation()) {
                System.err.println("PrisonPearl for " + pp.getImprisonedName() + " didn't validate, so is now set free");
                String reason = pp.getImprisonedName() + " is being freed. Reason: Locate command returned a invalid pearl.";
                pearlman.freePearl(pp, reason);
            } else {
                sender.sendMessage(ChatColor.GREEN + name_possesive + " prison pearl is " + pp.describeLocation());
                if (sender instanceof Player && !any)
                    broadcastman.broadcast((Player)sender, ChatColor.GREEN + pp.getImprisonedName() + ": " + pp.describeLocation());
            }
        } else {
            sender.sendMessage(name_is + " not imprisoned");
        }
        return true;
    }

    private boolean freeCmd(CommandSender sender, String args[], boolean any) {
        PrisonPearl pp;
        if (!any) {
            if (args.length > 1)
                return false;
            if (!(sender instanceof Player)) {
                sender.sendMessage("Must use freeany at console");
                return true;
            }
            Player player = (Player)sender;
            int slot = getCommandPearlSlot(player, args, 0);
            if (slot == -1)
                return true;
            pp = pearls.getByItemStack(player.getInventory().getItem(slot));
            player.getInventory().setItem(slot, null);
        } else {
            if (args.length != 1)
                return false;
            UUID uuid = null;
            if (isNameLayer)
            	uuid = NameAPI.getUUID(args[0]);
            else
            	uuid = Bukkit.getOfflinePlayer(args[0]).getUniqueId();
            pp = pearls.getByImprisoned(uuid);
            if (pp == null) {
                sender.sendMessage(args[0] + " is not imprisoned");
                return true;
            }
        }
        if (pearlman.freePearl(pp, pp.getImprisonedName() + " is being freed. Reason: Freed via command by " + sender.getName())) {
            if (pp.getImprisonedPlayer() != sender) // when freeing yourself, you're already going to get a message
                sender.sendMessage("You've freed " + pp.getImprisonedName());
        } else {
            sender.sendMessage("You failed to free " + pp.getImprisonedName());
        }
        return true;
    }

    private boolean imprisonCmd(CommandSender sender, String args[]) {
        if (args.length != 1)
            return false;
        if (!(sender instanceof Player)) {
            sender.sendMessage("imprison cannot be used at the console");
            return true;
        }
        String playerName = args[0];
        UUID uuid = null;
        if (isNameLayer)
        	uuid = NameAPI.getUUID(args[0]);
        else
        	uuid = Bukkit.getOfflinePlayer(args[0]).getUniqueId();
        if (uuid == null){
        	sender.sendMessage("No player found with the name: "+args[0]);
        	return true;
        }
        if (pearlman.imprisonPlayer(uuid, (Player)sender)) {
            sender.sendMessage("You imprisoned " + args[0]);
            Player imprisoned = Bukkit.getPlayer(uuid);
            if (imprisoned != null) {
            	imprisoned.setHealth(0.0);
            }
        } else {
            sender.sendMessage("You failed to imprison " + args[0]);
        }
        return true;
    }

    private boolean summonCmd(CommandSender sender, String args[]) {
        if (args.length > 1)
            return false;
        if (!(sender instanceof Player)) {
            sender.sendMessage("Command cannot be used at console");
            return true;
        }
        Player player = (Player)sender;
        PrisonPearl pp;
        if (args.length == 1) {
            try {
                pp = getCommandPearl(player, args, 0);
            } catch (NumberFormatException e) {
                pp = getCommandPearl(player, args, 1);
            }
        } else {
            pp = getCommandPearl(player, args, 0);
        }
        if (pp == null)
            return true;
        //check if the pearled player is combat tagged
        if (plugin.isCombatTagged(pp.getImprisonedName())) {
            sender.sendMessage(ChatColor.RED+"[PrisonPearl]"+ChatColor.WHITE+" You cannot summon a CombatTagged player.");
            return true;
        }
        if (pp.getImprisonedPlayer() == null || pp.getImprisonedPlayer().isDead()) {
        	if (plugin.isMercuryLoaded() && plugin.isBetterShardsEnabled()) {
        		if (!MercuryAPI.getAllPlayers().contains(pp.getImprisonedName())) {
        			sender.sendMessage(pp.getImprisonedName() + " cannot be summoned");
                	return true;
        		}
        	} else {
        		sender.sendMessage(pp.getImprisonedName() + " cannot be summoned");
            	return true;
        	}
        } else if (pp.getImprisonedPlayer() == player) {
            sender.sendMessage("You cannot summon yourself!");
            return true;
        } else if (summonman.isSummoned(pp)) {
            sender.sendMessage(pp.getImprisonedName() + " is already summoned");
            return true;
        }
        if (summonman.summonPearl(pp))
            sender.sendMessage("Attempting to summon " + pp.getImprisonedName());
        else
            sender.sendMessage("You failed to summon " + pp.getImprisonedName());
        return true;
    }

    private boolean returnCmd(CommandSender sender, String args[]) {
        if (args.length > 1)
            return false;
        if (!(sender instanceof Player)) {
            sender.sendMessage("Command cannot be used at console");
            return true;
        }
        Player player = (Player)sender;
        PrisonPearl pp = getCommandPearl(player, args, 0);
        if (pp == null)
            return true;
        //check if the pearled player is combat tagged
        if (plugin.isCombatTagged(pp.getImprisonedName())) {
            sender.sendMessage(ChatColor.RED+"[PrisonPearl]"+ChatColor.WHITE+" You cannot return a CombatTagged player.");
            return true;
        }
        if (pp.getImprisonedName().equals(player.getName())) {
            sender.sendMessage("You cannot return yourself!");
            return true;
        } else if (!summonman.isSummoned(pp)) {
            sender.sendMessage(pp.getImprisonedName() + " has not been summoned!");
            return true;
        } else if (damageman.hasDamageLog(player)) {
            sender.sendMessage(pp.getImprisonedName() + " is in combat and cannot be returned!");
            return true;
        }
        if (summonman.returnPearl(pp))
            sender.sendMessage("You've returned " + pp.getImprisonedName());
        else
            sender.sendMessage("You failed to return " + pp.getImprisonedName());
        return true;
    }

    private boolean killCmd(CommandSender sender, String args[]) {
        if (args.length > 1)
            return false;
        if (!(sender instanceof Player)) {
            sender.sendMessage("Command cannot be used at console");
            return true;
        }
        Player player = (Player)sender;
        PrisonPearl pp = getCommandPearl(player, args, 0);
        if (pp == null)
            return true;
        if (!summonman.isSummoned(pp)) {
            sender.sendMessage(pp.getImprisonedName() + " has not been summoned!");
            return true;
        }
        if (summonman.killPearl(pp))
            sender.sendMessage("You've killed " + pp.getImprisonedName());
        else
            sender.sendMessage("You failed to kill " + pp.getImprisonedName());
        return true;
    }

    private boolean saveCmd(CommandSender sender, String args[]) {
        if (args.length > 0)
            return false;
        try {
            plugin.saveAll(true);
            sender.sendMessage("PrisonPearl data saved!");
            return true;
        } catch (RuntimeException e) {
            if (!(sender instanceof ConsoleCommandSender))
                sender.sendMessage("PrisonPearl failed to save data! Check server logs!");
            throw e;
        }
    }

    private boolean broadcastCmd(CommandSender sender, String args[]) {
        if (args.length != 1)
            return false;
        if (!(sender instanceof Player)) {
            sender.sendMessage("Command cannot be used at console");
            return true;
        }
        Player player = (Player)sender;
        UUID uuid = null;
        if (isNameLayer)
        	uuid = NameAPI.getUUID(args[0]);
        else
        	uuid = Bukkit.getOfflinePlayer(args[0]).getUniqueId();
        Player receiver = Bukkit.getPlayer(uuid);
        if (receiver == null) {
            sender.sendMessage("No such player " + args[0]);
            return true;
        } else if (receiver == player) {
            sender.sendMessage("You cannot broadcast to yourself!");
            return true;
        } else if (!pearls.isImprisoned(player)) {
            sender.sendMessage("You are not imprisoned!");
            return true;
        }
        if (broadcastman.addBroadcast(player, receiver)) {
            sender.sendMessage("You will broadcast pplocate information to " + receiver.getDisplayName());
            receiver.sendMessage("Type /ppconfirm to receive pplocate broadcasts from " + player.getDisplayName());
        } else {
            sender.sendMessage("You are already broadcasting to " + receiver.getDisplayName());
        }
        return true;
    }

    private boolean confirmCmd(CommandSender sender, String args[]) {
        if (args.length > 1)
            return false;
        if (!(sender instanceof Player)) {
            sender.sendMessage("Command cannot be used at console");
            return true;
        }
        Player player = (Player)sender;
        Player broadcaster;
        if (args.length == 1) {
        	UUID uuid = null;
            if (isNameLayer)
            	uuid = NameAPI.getUUID(args[0]);
            else
            	uuid = Bukkit.getOfflinePlayer(args[0]).getUniqueId();
            broadcaster = Bukkit.getPlayer(uuid);
            if (broadcaster == null) {
                sender.sendMessage("No such player " + args[0]);
                return true;
            }
        } else {
            broadcaster = broadcastman.getQuickConfirmPlayer(player);
            if (broadcaster == null) {
                sender.sendMessage("Nobody has requested to broadcast to you");
                return true;
            }
        }
        if (broadcastman.confirmBroadcast(broadcaster, player)) {
            player.sendMessage("You will now receive broadcasts from " + broadcaster.getDisplayName());
        } else {
            player.sendMessage(broadcaster.getDisplayName() + " does not wish to broadcast to you");
        }
        return true;
    }

    private boolean silenceCmd(CommandSender sender, String args[]) {
        if (args.length != 1)
            return false;
        if (!(sender instanceof Player)) {
            sender.sendMessage("Command cannot be used at console");
            return true;
        }
        Player player = (Player)sender;
        UUID uuid = null;
        if (isNameLayer)
        	uuid = NameAPI.getUUID(args[0]);
        else
        	uuid = Bukkit.getOfflinePlayer(args[0]).getUniqueId();
        Player broadcaster = Bukkit.getPlayer(uuid);
        if (broadcaster == null) {
            sender.sendMessage("No such player " + args[0]);
            return true;
        }
        if (broadcastman.silenceBroadcast(player, broadcaster)) {
            player.sendMessage("You will no longer receive broadcasts from " + broadcaster.getDisplayName());
        } else {
            player.sendMessage(broadcaster.getDisplayName() + " is not broadcasting to you");
        }
        return true;
    }

    private PrisonPearl getCommandPearl(Player player, String args[], int pos) {
        int slot = getCommandPearlSlot(player, args, pos);
        if (slot != -1)
            return pearls.getByItemStack(player.getInventory().getItem(slot));
        else
            return null;
    }

    private int getCommandPearlSlot(Player player, String args[], int pos) {
        if (args.length <= pos) {
            ItemStack item = player.getItemInHand();
            if (item.getType() != Material.ENDER_PEARL) {
                player.sendMessage("You must hold a pearl or supply the player's name to use this command");
                return -1;
            }
            if (pearls.getByItemStack(item) == null) {
                player.sendMessage("This is an ordinary ender pearl");
                return -1;
            }
            return player.getInventory().getHeldItemSlot();
        } else {

        	UUID uuid = null;
            if (isNameLayer)
            	uuid = NameAPI.getUUID(args[pos]);
            else
            	uuid = Bukkit.getOfflinePlayer(args[pos]).getUniqueId();
            PrisonPearl pp = pearls.getByImprisoned(uuid);
            if (pp != null) {
                Inventory inv = player.getInventory();
                for (Entry<Integer, ? extends ItemStack> entry : inv.all(Material.ENDER_PEARL).entrySet()) {
                    if (pearls.isPrisonPearl(entry.getValue()))
                        return entry.getKey();
                }
            }
            player.sendMessage("You don't possess " + args[0] + "'s prison pearl");
            return -1;
        }
    }

    private boolean reloadAlts(CommandSender sender) {
        if (!(sender instanceof Player)) {
            plugin.loadAlts();
            plugin.checkBanAllAlts();
            return true;
        }
        return false;
    }

    private boolean checkAll(CommandSender sender) {
        if (!(sender instanceof Player)) {
            plugin.checkBanAllAlts();
            return true;
        }
        return false;
    }

    private boolean check(CommandSender sender, String[] args) {
        if (args.length != 1)
            return false;
        if (!(sender instanceof Player)) {
        	UUID uuid = null;
            if (isNameLayer)
            	uuid = NameAPI.getUUID(args[0]);
            else
            	uuid = Bukkit.getOfflinePlayer(args[0]).getUniqueId();
            boolean isBanned = plugin.isTempBanned(uuid);
            if (isBanned) {
                sender.sendMessage(args[0]+" is temp banned for having "+plugin.getImprisonedCount(uuid)+" imprisoned accounts: "+plugin.getImprisonedAltsString(uuid));
            } else {
                sender.sendMessage(args[0]+" is not temp banned");
            }
            return true;
        }
        return false;
    }
    
    private boolean listBans(CommandSender sender, String[] args) {
        String search = null;
        if (args.length >= 1) {
            search = args[0].toLowerCase();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Bans: ");
        boolean first = true;
        for (UUID playerUUID : plugin.getBanManager().listBannedUUIDS()) {
        	UUID uuid = Bukkit.getPlayer(search).getUniqueId();
            if (search == null || playerUUID.toString().contains(uuid.toString())) {
                if (first) {
                    first = false;
                } else {
                    sb.append(", ");
                }
                String player = "";
                if (isNameLayer)
                	player = NameAPI.getCurrentName(playerUUID);
                else
                	player = Bukkit.getOfflinePlayer(playerUUID).getName();
                sb.append(player);
            }
        }
        if (first) {
            sender.sendMessage("No matching bans.");
        } else {
            sender.sendMessage(sb.toString());
        }
        return true;
    }

    private boolean kill() {
        return false;
    }
}
