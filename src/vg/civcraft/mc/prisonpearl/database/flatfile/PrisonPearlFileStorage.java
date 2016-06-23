package vg.civcraft.mc.prisonpearl.database.flatfile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import vg.civcraft.mc.namelayer.NameAPI;
import vg.civcraft.mc.prisonpearl.PrisonPearl;
import vg.civcraft.mc.prisonpearl.PrisonPearlPlugin;
import vg.civcraft.mc.prisonpearl.database.interfaces.IPrisonPearlStorage;

public class PrisonPearlFileStorage implements IPrisonPearlStorage{

	private boolean isNameLayer;
	
	private final Map<UUID, PrisonPearl> pearls_byimprisoned;
	private long lastFeed = 0;
	private File oldStorageFile;
	private File storageFile;
	
	public PrisonPearlFileStorage() {
		isNameLayer = PrisonPearlPlugin.isNameLayerEnabled();
		pearls_byimprisoned = new HashMap<UUID, PrisonPearl>();
		
		oldStorageFile = new File(PrisonPearlPlugin.getInstance().getDataFolder(), "prisonpearlsUUID.txt");
		storageFile = new File(PrisonPearlPlugin.getInstance().getDataFolder(), "prisonpearlsUUID.yml");
	}
	
	@Override
	public void addPearl(PrisonPearl pp) {
		// Add them to the hashmap, they get saved when saved method is called.
		pearls_byimprisoned.put(pp.getImprisonedId(), pp);
	}

	@Override
	public void removePearl(PrisonPearl pp, String reason) {
		pearls_byimprisoned.remove(pp.getImprisonedId());
		PrisonPearlPlugin.log(reason);
	}

	@Override
	public Collection<PrisonPearl> getAllPearls() {
		return new ArrayList<PrisonPearl>(pearls_byimprisoned.values());
	}
	
	@Override
	public void load() {
		if (oldStorageFile.exists()) {
			PrisonPearlPlugin.getInstance().info("Found old storage file, loading pearls from there instead of the main file");
			loadOld();
			oldStorageFile.delete();
			return;
		}
		if (!storageFile.exists()) {
			PrisonPearlPlugin.getInstance().warning("Found no storage file, no pearls were loaded");
			return;
		}
		YamlConfiguration config = YamlConfiguration
				.loadConfiguration(storageFile);
		ConfigurationSection pearls = config.getConfigurationSection("pearls");
		int pearlCount = 0;
		if (pearls != null) {
			for(String key : pearls.getKeys(false)) {
				ConfigurationSection current = pearls.getConfigurationSection(key);
				UUID imprisonedUUID = UUID.fromString(current.getString("imprisonedUUID"));
				ConfigurationSection location = current.getConfigurationSection("location");
				int x = location.getInt("x");
				int y = location.getInt("y");
				int z = location.getInt("z");
				UUID world = UUID.fromString(location.getString("world"));
				int uniqueCount = current.getInt("unique");
				String motd = current.getString("motd");
				String killerName = current.getString("killerUUID");
				UUID killerUUID;
				if (killerName != null) {
					killerUUID = UUID.fromString(killerName);
				}
				else {
					killerUUID = null;
				}
				long imprisonTime = current.getLong("imprisonTime");
				String imprisonedName;
				if (isNameLayer) {
					imprisonedName = NameAPI.getCurrentName(imprisonedUUID);
				}
				else {
					imprisonedName = Bukkit.getOfflinePlayer(imprisonedUUID).getName();
				}
				World w = Bukkit.getWorld(world);
				if (w == null) {
					PrisonPearlPlugin.getInstance().warning("World in which " + imprisonedUUID + " was pearled no longer exists, failed to load the pearl");
					continue;
				}
				Location loc = new Location(w, x, y, z);
				PrisonPearl pp = PrisonPearl.makeFromLocation(imprisonedName, imprisonedUUID, loc, uniqueCount, killerUUID, imprisonTime);
				if (pp != null) {
					pp.setMotd(motd);
					addPearl(pp);
				} else {
					// Pearl was not in a proper container! Freeeeeddooooommm
					PrisonPearlPlugin.getInstance().warning("Whoever has imprisoned " + imprisonedUUID + " failed to put them in a proper containre. Freeing them");
					continue;
				}
				pearlCount++;
			}
			PrisonPearlPlugin.getInstance().info("Loaded " + pearlCount + " pearls from file");
		}
		else {
			PrisonPearlPlugin.getInstance().severe("Could not find pearl section in flatfile");
		}
		lastFeed = config.getLong("lastFeed");
	}

	@Deprecated
	public void loadOld() {		
		String line;
		try {
			FileInputStream fis = new FileInputStream(oldStorageFile);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			while ((line = br.readLine()) != null) {
				if(line.matches("lastFeed:([0-9]+)")) {
					lastFeed = Long.parseLong(line.split(":")[1]);
					continue;
				}
				String parts[] = line.split(" ");
				if (parts.length <= 1)
					continue;
				UUID imprisoned = UUID.fromString(parts[0]);
				Location loc = new Location(Bukkit.getWorld(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4]));
				String name = "";
				if (isNameLayer)
					name = NameAPI.getCurrentName(imprisoned);
				else
					name = Bukkit.getOfflinePlayer(imprisoned).getName();
				int unique = Integer.parseInt(parts[5]);
				//old storage didnt store killer and imprisonTime
				PrisonPearl pp = PrisonPearl.makeFromLocation(name, imprisoned, loc, unique, null, -1L);
				if (parts.length > 6) {
					String motd = "";
					for (int i = 6; i < parts.length; i++) {
						motd = motd.concat(parts[i] + " ");
					}
					if (pp != null)
						pp.setMotd(motd);
				}
				if (pp == null) {
					System.err.println("PrisonPearl for " + imprisoned + " didn't validate, so is now set free. Chunks and/or prisonpearls.txt are corrupt");
					continue;
				}
				
				addPearl(pp);
			}
			fis.close();
		} catch (NumberFormatException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	@Override
	public void save() {
		if (storageFile.exists()) {
			//ensure we always start with a new clean yaml
			storageFile.delete();
		}
		try {
			storageFile.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		YamlConfiguration config = YamlConfiguration
				.loadConfiguration(storageFile);
		ConfigurationSection pearls = config.createSection("pearls");
		for(PrisonPearl pp : getAllPearls()) {
			ConfigurationSection current = pearls.createSection(pp.getImprisonedId() + pp.getImprisonedName());
			current.set("imprisonedUUID", pp.getImprisonedId().toString());
			Location loc = pp.getLocation();
			current.set("location.x", loc.getBlockX());
			current.set("location.y", loc.getBlockY());
			current.set("location.z", loc.getBlockZ());
			current.set("location.world", loc.getWorld().getUID().toString());
			current.set("unique", pp.getUniqueIdentifier());
			if (pp.getKillerUUID() != null) {
				current.set("killerUUID", pp.getKillerUUID().toString());
			}
			current.set("imprisonTime", pp.getImprisonTime());
			current.set("motd", pp.getMotd());
		}
		config.set("lastFeed", lastFeed);
		try {
		config.save(storageFile);
		} catch (IOException e) {
			PrisonPearlPlugin.getInstance().severe("Failed to save pearled player data to file");
			e.printStackTrace();
		}
	}

	@Override
	public void updatePearl(PrisonPearl pp) {
		// We do not use this for flat files.
	}

	@Override
	public boolean isImprisoned(UUID uuid) {
		return pearls_byimprisoned.containsKey(uuid);
	}

	@Override
	public boolean isImprisoned(Player p) {
		return isImprisoned(p.getUniqueId());
	}

	@Override
	public Integer getImprisonedCount(UUID[] ids) {
		return pearls_byimprisoned.size();
	}

	@Override
	public Set <UUID> getImprisonedIds(UUID[] ids) {
		return pearls_byimprisoned.keySet();
	}

	@Override
	public void updateLastFeed(long lastFeed) {
		this.lastFeed = lastFeed;
	}

	@Override
	public long getLastFeed() {
		return lastFeed;
	}

	@Override
	public PrisonPearl getByImprisoned(Player player) {
		return getByImprisoned(player.getUniqueId());
	}

	@Override
	public PrisonPearl getByImprisoned(UUID uuid) {
		return pearls_byimprisoned.get(uuid);
	}

	@Override
	public PrisonPearl newPearl(String imprisonedName, UUID imprisonedId, Player imprisoner) {
		Random rand = new Random();
		PrisonPearl pp = new PrisonPearl(imprisonedName, imprisonedId, imprisoner, rand.nextInt(1000000000), imprisoner.getUniqueId(), System.currentTimeMillis());
		addPearl(pp);
		pp.setHolder(imprisoner); // This will set the holder to something valid so it can correctly send it out.
		pp.markMove();
		return pp;
	}

	@Override
	public PrisonPearl newPearl(OfflinePlayer imprisoned, Player imprisoner) {
		return newPearl(imprisoned.getName(), imprisoned.getUniqueId(), imprisoner);
	}
}
