package com.untamedears.PrisonPearl.managers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import com.untamedears.PrisonPearl.PrisonPearlPlugin;

public class FBanManager extends BanManager {
	final public static String BAN_MSG = "Banned for having too many imprisoned alts!";
	final public static String JOURNAL_BASE_NAME = "ban_journalUUID";

	private PrisonPearlPlugin plugin_ = null;
	private Set<UUID> bannedUUIDs_ = new TreeSet<UUID>();
	private File banJournal_ = null;
	private File banJournalBakOne_ = null;
	private File banJournalBakTwo_ = null;
	private String banMessage_ = BAN_MSG;
	private File baseDirectory_ = null;
	private String journalBaseName_ = JOURNAL_BASE_NAME;

	public FBanManager(final PrisonPearlPlugin plugin) {
		plugin_ = plugin;
		setDirectory(plugin.getDataFolder());
	}

	@Override
	public boolean initialize() {
		Bukkit.getPluginManager().registerEvents(this, plugin_);
		loadBanJournal();
		rotateBanJournal();
		writeBanJournal();
		return true;
	}

	@Override
	public void setBanMessage(String msg) {
		banMessage_ = msg;
	}
	
	@Override
	public String getBanMessage() {
		return banMessage_;
	}

	public void setJournalBaseName(String name) {
		journalBaseName_ = name;
		initFiles();
	}

	public void setDirectory(File dir) {
		baseDirectory_ = dir;
		initFiles();
	}

	private void initFiles() {
		banJournal_ = new File(baseDirectory_, journalBaseName_ + ".dat");
		banJournalBakOne_ = new File(baseDirectory_, journalBaseName_ + ".1.bak");
		banJournalBakTwo_ = new File(baseDirectory_, journalBaseName_ + ".2.bak");
	}

	@Override
	public boolean isBanned(UUID uuidName) {
		return bannedUUIDs_.contains(uuidName);
	}

	@Override
    public Set<UUID> listBannedUUIDS() {
		return bannedUUIDs_;
    }

	@Override
	public void ban(UUID UUIDName) {
		setBanState(UUIDName, true);
	}

	@Override
	public void pardon(UUID UUIDName) {
		setBanState(UUIDName, false);
	}

	@Override
	public void setBanState(UUID UUIDName, boolean isBanned) {
		final boolean currentlyBanned = bannedUUIDs_.contains(UUIDName);
		if (isBanned != currentlyBanned) {
			recordToBanJournal(UUIDName, isBanned);
			if (isBanned) {
				bannedUUIDs_.add(UUIDName);
			} else {
				bannedUUIDs_.remove(UUIDName);
			}
		}
	}

	private void writeBanJournal() {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(banJournal_, true);
			for (UUID uuidNames : bannedUUIDs_) {
				recordToBanJournal(fos, uuidNames, true);
			}
		} catch (IOException e) {
			PrisonPearlPlugin.info("Failed to append ban journal: " + e.toString());
			e.printStackTrace();
		} finally {
			try {
				if (fos != null) {
					fos.close();
					fos = null;
				}
			} catch (IOException ie) {
				PrisonPearlPlugin.info("Failed to close ban journal: " + ie.toString());
			}
		}
	}

	private void recordToBanJournal(UUID name, boolean isBanned) {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(banJournal_, true);
			recordToBanJournal(fos, name, isBanned);
		} catch (IOException e) {
			PrisonPearlPlugin.info("Failed to append ban journal: " + e.toString());
			e.printStackTrace();
		} finally {
			try {
				if (fos != null) {
					fos.close();
					fos = null;
				}
			} catch (IOException ie) {
				PrisonPearlPlugin.info("Failed to close ban journal: " + ie.toString());
			}
		}
	}

	private void recordToBanJournal(FileOutputStream fos, UUID name, boolean isBanned) {
		final String ban_record = String.format(
			"%s|%s\n", name.toString(), isBanned ? "ban" : "free");
		try {
			fos.write(ban_record.getBytes(Charset.forName("UTF-8")));
		} catch (IOException e) {
			PrisonPearlPlugin.info("Failed to append ban journal: " + ban_record);
			e.printStackTrace();
		}
	}

	private void loadBanJournal() {
		// Only keep bans on load
		if (bannedUUIDs_ == null || bannedUUIDs_.size() > 0) {
			bannedUUIDs_ = new TreeSet<UUID>();
		}
		for (Map.Entry<UUID, Boolean> entry : readBanJournal(false).entrySet()) {
			final Boolean isBanned = entry.getValue();
			if (isBanned == null || !isBanned) {
				continue;
			}
			final UUID uuidName = entry.getKey();
			bannedUUIDs_.add(uuidName);
		}
		PrisonPearlPlugin.log("PrisonPearl BanManager Ban Count: " + String.valueOf(bannedUUIDs_.size()));
	}

	// come back here
	public Map<UUID, Boolean> readBanJournal(boolean openPreviousJournal) {
		// The journal is append only so the last line for a player wins. This
		//  just reads all players and updates the Map each line, resulting in
		//  the same.
		Map<UUID, Boolean> banState = new TreeMap<UUID, Boolean>();
		File activeJournal = openPreviousJournal ? banJournalBakOne_ : banJournal_;
		if (!activeJournal.exists()) {
			try {
				activeJournal.createNewFile();
			} catch (IOException e) {
				PrisonPearlPlugin.info("Unable to create ban journal: " + activeJournal.getPath());
				e.printStackTrace();
			}
			return banState;
		}
		// Load the journal
		BufferedReader br = null;
		try {
			br = new BufferedReader(
				new InputStreamReader(
					new FileInputStream(activeJournal),
					Charset.forName("UTF-8")));
			while (true) {
				String line = br.readLine();
				if (line == null) {
					break;
				}
				if (line.length() <= 0) {
					continue;
				}
				String[] parts = line.split("\\|");
				if (parts.length < 2) {
					PrisonPearlPlugin.info("Malformed line: " + line);
					continue;
				}
				final UUID playerName = UUID.fromString(parts[0]);
				final boolean isBanned = "ban".equalsIgnoreCase(parts[1]);
				banState.put(playerName, isBanned);
			}
		} catch (IOException e) {
			PrisonPearlPlugin.info("Failed to read ban journal");
			e.printStackTrace();
		} finally {
			try {
				if (br != null) {
					br.close();
					br = null;
				}
			} catch (IOException ie) {
				PrisonPearlPlugin.info("Failed to close ban journal: " + ie.toString());
			}
		}
		return banState;
	}

	private void rotateBanJournal() {
		try {
			banJournalBakTwo_.delete();
			banJournalBakOne_.renameTo(banJournalBakTwo_);
			banJournal_.renameTo(banJournalBakOne_);
			banJournal_.createNewFile();
		} catch (Exception e) {
			PrisonPearlPlugin.info("Failed to rotate ban journal");
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean hasBannedPlayers(){
		return bannedUUIDs_.size() > 0;
	}
}
