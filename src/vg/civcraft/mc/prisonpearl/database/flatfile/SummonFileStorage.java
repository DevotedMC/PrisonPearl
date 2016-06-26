package vg.civcraft.mc.prisonpearl.database.flatfile;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import vg.civcraft.mc.prisonpearl.PrisonPearlPlugin;
import vg.civcraft.mc.prisonpearl.Summon;
import vg.civcraft.mc.prisonpearl.database.interfaces.ISummonStorage;

public class SummonFileStorage implements ISummonStorage{

	private File file;
	
	private Map<UUID, Summon> summons;
	
	public SummonFileStorage() {
		summons = new HashMap<UUID, Summon>();
		file = new File(PrisonPearlPlugin.getInstance().getDataFolder(), "summonsUUID.txt");
	}
	
	@Override
	public void addSummon(Summon summon) {
		summons.put(summon.getUUID(), summon);
	}

	@Override
	public void removeSummon(Summon summon) {
		removeSummon(summon.getUUID());
	}

	@Override
	public void removeSummon(UUID uuid) {
		summons.remove(uuid);
	}

	@Override
	public Summon getSummon(UUID uuid) {
		return summons.get(uuid);
	}

	@Override
	public boolean isSummoned(UUID uuid) {
		return summons.containsKey(uuid);
	}

	@Override
	public void updateSummon(Summon summon) {
		// No need to use this.
	}

	@Override
	public Map<UUID, Summon> getAllSummons() {
		return summons;
	}

	@Override
	public void save() {
		try {
			FileOutputStream fos = new FileOutputStream(file);
			BufferedWriter br = new BufferedWriter(new OutputStreamWriter(fos));
			
			for (Entry<UUID, Summon> entry : summons.entrySet()) {
				Summon summon = entry.getValue();
				Location loc = summon.getReturnLocation();
				br.append(summon.getUUID().toString()).append(" ").append(loc.getWorld().getName()).append(" ").append(String.valueOf(loc.getBlockX())).append(" ").append(String.valueOf(loc.getBlockY())).append(" ").append(String.valueOf(loc.getBlockZ())).append(" ").append(String.valueOf(summon.getMaxDistance())).append(" ").append(String.valueOf(summon.getAmountDamage())).append(" ").append(String.valueOf(summon.getCanSpeak())).append(" ").append(String.valueOf(summon.getCanDamage())).append(" ").append(String.valueOf(summon.getCanBreak())).append("\n");
			}
			
			br.flush();
			fos.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void load() {
		if (!file.exists()) {
			PrisonPearlPlugin.getInstance().warning("Found no summoned players file, none were loaded");
			return;
		}
		try {
			FileInputStream fis = new FileInputStream(file);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			
			String line;
			while ((line = br.readLine()) != null) {
				if (line.trim().equals("")) {
					continue;
				}
				String[] parts = line.split(" ");
				String idString = parts[0];
				Location loc = new Location(Bukkit.getWorld(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4]));
				int dist = parts.length >= 6 ? Integer.parseInt(parts[5]) : PrisonPearlPlugin.getInstance().GetConfig().get("summon_damage_radius").getInt();
			    int damage = parts.length >= 7 ? Integer.parseInt(parts[6]) : PrisonPearlPlugin.getInstance().GetConfig().get("summon_damage_amt").getInt();
			    boolean canSpeak = parts.length >= 8 ? Boolean.parseBoolean(parts[7]) : true;
			    boolean canDamage = parts.length >= 9 ? Boolean.parseBoolean(parts[8]) : true;
			    boolean canBreak = parts.length == 10 ? Boolean.parseBoolean(parts[9]) : true;

			    UUID id = UUID.fromString(idString);
				
			    // QUICK FIX: TODO re-evaluate entanglements of Managers and Data Files.
			    // current load order has a ton of unhandled edge cases.
				if (!PrisonPearlPlugin.getDBHandler().getStorageHandler().getPrisonPearlStorage().isImprisoned(id))
					continue;
				
				Summon summon = new Summon(id, loc, PrisonPearlPlugin.getDBHandler().getStorageHandler().getPrisonPearlStorage().getByImprisoned(id));
				summon.setMaxDistance(dist);
				summon.setAmountDamage(damage);
				summon.setCanSpeak(canSpeak);
				summon.setCanDamage(canDamage);
				summon.setCanBreak(canBreak);
				summons.put(id, summon);
			}
			
			fis.close();
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
