package com.untamedears.PrisonPearl.managers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;

import com.untamedears.PrisonPearl.PrisonPearlPlugin;
import com.untamedears.PrisonPearl.SaveLoad;
import com.wimbli.WorldBorder.BorderData;
import com.wimbli.WorldBorder.Config;

public class WorldBorderManager implements SaveLoad{
	private final PrisonPearlPlugin plugin;
	private boolean isWorldBorder;
	private boolean dirty;
	private List<Location> WhiteListedLocations;
	
	public WorldBorderManager(PrisonPearlPlugin plugin) {
		this.plugin = plugin;
		this.isWorldBorder=plugin.isWorldBorder();
		WhiteListedLocations = new ArrayList<Location>();
	}
	
    public boolean isMaxFeed(Location loc) {
    	if(!plugin.getPPConfig().getFreeOutsideWorldBorder() || !isWorldBorder || isOnWhiteList(loc)) {
    		return false;
    	}
    	World world = loc.getWorld();
    	BorderData border = Config.Border(world.getName());
    	return !border.insideBorder(loc);
    }
    
    public boolean isOnWhiteList(Location loc) {
    	return WhiteListedLocations.contains(loc);
    }
    
    public boolean isDirty() {
		return dirty;
	}
    
	@Override
	public void save(File file) throws IOException {
		if (plugin.getMysqlStorage() != null) {
			return;
		}
		FileOutputStream  fos = new FileOutputStream (file);
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
		bw.append("whitelistedlocations File");
		bw.append("\n");
		for(Location loc : WhiteListedLocations) {
			bw.append(loc.getX()+","+loc.getY()+","+loc.getZ());
			bw.append("\n");
		}
		bw.flush();
		bw.close();	
		dirty = false;
	}
	@Override
	public void load(File file) throws IOException {
		if (plugin.getMysqlStorage() != null) {
			mysqlLoad();
			return;
		}
		FileInputStream fis = new FileInputStream(file);
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		plugin.getLogger().log(Level.INFO, file.getName());
		if (file.exists()) {
			if(br.readLine() == null) {
				plugin.getLogger().log(Level.INFO, file.getName()+ " file is empty");;
				br.close();
				return;
			}
			String line;
			while ((line = br.readLine()) != null) {
				plugin.getLogger().log(Level.INFO, ("Reading curLine: " + line));
				String parts[] = line.split(",");
				if(parts.length==3) {
					if (addWhitelistedLocation(parts[0], parts[1], parts[2])) {
							plugin.getLogger().log(Level.INFO, ("Location added"));
						} else {
							plugin.getLogger().log(Level.WARNING, "Current line is not in the right format");
						}
				} else {
					plugin.getLogger().log(Level.WARNING, "Current line is not in the right format");
				}
			}
			br.close();
			fis.close();
		}
		dirty = false;
	}
	
    private Double stringToDouble(String str) throws NumberFormatException {
    	return Double.parseDouble(str);
    }
    
    public boolean addWhitelistedLocation(Location loc) {
    	if (WhiteListedLocations.contains(loc)) {
    		return false;
    	} else {
    		WhiteListedLocations.add(loc);
    		plugin.getMysqlStorage().addWorldBorder(loc);
    		plugin.getLogger().log(Level.INFO, ("A new location was added to the whitelist " + loc));
    		dirty = true;
    		return true;
    	}
    }
    public boolean removeWhitelistedLocation(Location loc) {
    	if(WhiteListedLocations.contains(loc)) {
    		WhiteListedLocations.remove(loc);
    		plugin.getMysqlStorage().removeWorldBorder(loc);
    		plugin.getLogger().log(Level.INFO, ("A location was removed from the whitelist " + loc));
    		dirty=true;
    		return true;
    	}
    	return false;
    }
    public boolean addWhitelistedLocation(String x, String y, String z) {
    	try{
    		Double xd = stringToDouble(x);
			Double yd = stringToDouble(y);
			Double zd = stringToDouble(z);
			dirty=true;
			return addWhitelistedLocation(new Location(plugin.getFreeWorld(), xd, yd, zd));
    	}catch(NumberFormatException e) {
    		return false;
    	}

    }
    
    public boolean removeWhitelistedLocation(String x, String y, String z) {
    	try {
    		Double xd = stringToDouble(x);
    		Double yd = stringToDouble(y);
			Double zd = stringToDouble(z);
    		if(removeWhitelistedLocation(new Location(plugin.getFreeWorld(), xd , yd, zd))){
    			return false;
    		}
        	return true;
    	}catch(NumberFormatException e) {
    		return false;
    	}

    }
    
    private void mysqlLoad() {
    	List<Location> locs = plugin.getMysqlStorage().getAllWorldBorder();
    	for (Location loc: locs)
    		addWhitelistedLocation(loc);
    }
	
}
