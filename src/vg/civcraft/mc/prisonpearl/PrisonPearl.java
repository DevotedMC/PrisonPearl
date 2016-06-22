package vg.civcraft.mc.prisonpearl;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import vg.civcraft.mc.namelayer.NameAPI;
import vg.civcraft.mc.prisonpearl.managers.PrisonPearlManager;
import vg.civcraft.mc.prisonpearl.misc.FakeLocation;

public class PrisonPearl {
	public static final int HOLDER_COUNT = 5;

	public class Holder {
		public Holder(Player p) {
			player = p;
			item = null;
			blocklocation = null;
			supposedPearlLocation = null;
		}

		public Holder(Item i) {
			player = null;
			item = i;
			blocklocation = null;
			supposedPearlLocation = null;
		}

		public Holder(Location bl) {
			player = null;
			item = null;
			blocklocation = bl;
			supposedPearlLocation = null;
		}

		public Holder(FakeLocation loc) {
			player = null;
			item = null;
			blocklocation = null;
			supposedPearlLocation = loc;
		}

		public final Player player;
		public final Item item;
		public final Location blocklocation;
		public final FakeLocation supposedPearlLocation;
	}

	// Mostly used as a Deque, but clear() is used from LinkedList
	private LinkedList<Holder> holders = new LinkedList<Holder>();
	private final String imprisonedName;
	private final UUID imprisonedId;
	private final UUID killedUUID;
	private final long imprisonTime;
	private String motd = "";
	private boolean pearlOnCursor = false;
	private long lastMoved = 0;
	private final int unique;

	public PrisonPearl(String imprisonedName, UUID imprisonedId,
			Player holderplayer, int unique, UUID killer, long imprisonTime) {
		this.imprisonedName = imprisonedName;
		this.imprisonedId = imprisonedId;
		this.holders.addFirst(new Holder(holderplayer));
		this.unique = unique;
		this.killedUUID = killer;
		this.imprisonTime = imprisonTime;
	}

	public PrisonPearl(String imprisonedName, UUID imprisonedId,
			Location blocklocation, int unique, UUID killer, long imprisonTime) {
		this.imprisonedName = imprisonedName;
		this.imprisonedId = imprisonedId;
		this.holders.addFirst(new Holder(blocklocation));
		this.unique = unique;
		this.killedUUID = killer;
		this.imprisonTime = imprisonTime;
	}

	public PrisonPearl(String imprisonedName, UUID imprisonedId,
			FakeLocation blocklocation, int unique, UUID killer, long imprisonTime) {
		this.imprisonedName = imprisonedName;
		this.imprisonedId = imprisonedId;
		this.holders.addFirst(new Holder(blocklocation));
		this.unique = unique;
		this.killedUUID = killer;
		this.imprisonTime = imprisonTime;
	}

	public static PrisonPearl makeFromLocation(String imprisonedName,
			UUID imprisonedId, Location loc, int unique, UUID killer, long imprisonTime) {
		if (imprisonedId == null || loc == null)
			return null;
		BlockState bs = loc.getBlock().getState();
		if (bs instanceof InventoryHolder)
			return new PrisonPearl(imprisonedName, imprisonedId, loc, unique, killer, imprisonTime);
		else
			return null;
	}

	public String getImprisonedName() {
		return imprisonedName;
	}

	public UUID getImprisonedId() {
		return imprisonedId;
	}

	private String getPlayerName() {
		return Bukkit.getOfflinePlayer(imprisonedId).getName();
	}

	public Player getImprisonedPlayer() {
		return Bukkit.getPlayer(imprisonedId);
	}

	public OfflinePlayer getImprisonedOfflinePlayer() {
		return Bukkit.getOfflinePlayer(imprisonedId);
	}

	public Player getHolderPlayer() {
		return getHolderPlayer(this.holders.peekFirst());
	}

	public Player getHolderPlayer(final Holder holder) {
		if (holder != null) {
			return holder.player;
		}
		return null;
	}

	public BlockState getHolderBlockState() {
		return getHolderBlockState(this.holders.peekFirst());
	}

	public BlockState getHolderBlockState(final Holder holder) {
		if (holder != null && holder.blocklocation != null
				&& !(holder.blocklocation instanceof FakeLocation)) {
			return holder.blocklocation.getBlock().getState();
		}
		return null;
	}

	public Item getHolderItem() {
		return getHolderItem(this.holders.peekFirst());
	}

	public Item getHolderItem(final Holder holder) {
		if (holder != null) {
			return holder.item;
		}
		return null;
	}

	public String getHolderName() {
		return getHolderName(this.holders.peekFirst());
	}

	public String getHolderName(final Holder holder) {
		if (holder.player != null) {
			return holder.player.getName();
		} else if (holder.item != null) {
			return "nobody";
		} else if (holder.blocklocation != null) {
			switch (getHolderBlockState(holder).getType()) {
			case CHEST:
			case TRAPPED_CHEST:
				return "a chest";
			case FURNACE:
				return "a furnace";
			case BREWING_STAND:
				return "a brewing stand";
			case DISPENSER:
				return "a dispenser";
			case ITEM_FRAME:
				return "a wall frame";
			case DROPPER:
				return "a dropper";
			case HOPPER:
				return "a hopper";
			case ENDER_CHEST:
				return "a chest";
			default:
				// PrisonPearlPlugin.info("PrisonPearl " +
				// imprisonedId.toString() + " is inside an unknown block " +
				// getHolderBlockState(holder).getType().toString());
				return "an unknown block";
			}
		} else if (holder.supposedPearlLocation != null) {
			FakeLocation fake = (FakeLocation) holder.supposedPearlLocation;
			String player = fake.getPlayer();
			String server = fake.getServerName();
			if (player != null)
				return player + " on server " + server;
			return "another server " + server;
		} else {
			// PrisonPearlPlugin.info("PrisonPearl " + imprisonedId.toString() +
			// " has no player, item, nor location");
			return "unknown";
		}
	}

	public Location getLocation() {
		return getLocation(this.holders.peekFirst());
	}

	public Location getLocation(final Holder holder) {
		if (holder.player != null) {
			return holder.player.getLocation().add(0, -.5, 0);
		} else if (holder.item != null) {
			return holder.item.getLocation();
		} else if (holder.blocklocation != null) {
			return holder.blocklocation;
		} else if (holder.supposedPearlLocation != null){
			return holder.supposedPearlLocation;
		}
		else {
			throw new RuntimeException("PrisonPearl " + imprisonedId.toString() + " has no player, item, nor location");
		}
	}

	public String describeLocation() {
		final Holder holder = this.holders.peekFirst();
		final Location loc = getLocation(holder);
		final Vector vec = loc.toVector();
		final String str;
		if (loc instanceof FakeLocation) {
			FakeLocation fakeLoc = (FakeLocation) loc;
			str = fakeLoc.getWorldName() + " " + fakeLoc.getBlockX() + " " + fakeLoc.getBlockY() + " " + 
					fakeLoc.getBlockZ();
		}
		else 
			str = loc.getWorld().getName() + " " + vec.getBlockX() + " " + vec.getBlockY() + " " + vec.getBlockZ();
		return "held by " + getHolderName(holder) + " at " + str;
	}

	private enum HolderVerReason{
		DEFAULT,
		ENTITY_NOT_IN_CHUNK,
		PLAYER_NOT_ONLINE,
		BLOCK_STACK_NULL,
		NOT_BLOCK_INVENTORY,
		NO_ITEM_PLAYER_OR_LOCATION, //True after here
		OTHER_SERVER,
		ON_GROUND,
		IN_HAND,
		IN_CHEST,
		IN_VIEWER_HAND,
		TIME
	}

	public HolderVerReason verifyHolder(Holder holder, StringBuilder feedback) {
		// Return true if the pearl exists in a valid location
		if (System.currentTimeMillis() - this.lastMoved < 2000) {
			// The pearl was recently moved. Due to a race condition, this exists to
			//  prevent players from spamming /ppl to get free when a pearl is moved.
			return HolderVerReason.TIME;
		}
		if (holder.item != null) {
			Chunk chunk = holder.item.getLocation().getChunk();
			for (Entity entity : chunk.getEntities()) {
				if (entity == holder.item)
					feedback.append(String.format("Found on ground at (%d,%d,%d)",
							entity.getLocation().getBlockX(),
							entity.getLocation().getBlockY(),
							entity.getLocation().getBlockZ()));
					return HolderVerReason.ON_GROUND;
			}
			feedback.append("On ground not in chunk");
			return HolderVerReason.ENTITY_NOT_IN_CHUNK;
		} else {
			Inventory inv;
			if (holder.player != null) {
				if (!holder.player.isOnline()) {
					feedback.append(String.format("Jailor %s not online",
						holder.player.getName()));
					return HolderVerReason.PLAYER_NOT_ONLINE;
				}
				if (pearlOnCursor) {
					feedback.append(String.format("In hand of %s at (%d,%d,%d)",
							holder.player.getName(),
							holder.player.getLocation().getBlockX(),
							holder.player.getLocation().getBlockY(),
							holder.player.getLocation().getBlockZ()));
					return HolderVerReason.IN_HAND;
				}
				ItemStack cursoritem = holder.player.getItemOnCursor();
				if (cursoritem.getType() == Material.ENDER_PEARL && isItemStackPrisonPearl(cursoritem))
					return HolderVerReason.IN_HAND;
				inv = holder.player.getInventory();
				feedback.append(String.format("Not in %s's inventory", holder.player.getName()));
			} else if (holder.blocklocation != null) {
				BlockState bs = getHolderBlockState(holder);
				if (bs == null) {
					feedback.append("BlockState is null");
					return HolderVerReason.BLOCK_STACK_NULL;
				}
				Location bsLoc = bs.getLocation();
				if (!(bs instanceof InventoryHolder)) {
					feedback.append(String.format(
						"%s not inventory at (%d,%d,%d)", bs.getType().toString(),
						bsLoc.getBlockX(), bsLoc.getBlockY(), bsLoc.getBlockZ()));
					return HolderVerReason.NOT_BLOCK_INVENTORY;
				}
				inv = ((InventoryHolder)bs).getInventory();
				for (HumanEntity viewer : inv.getViewers()) {
					ItemStack cursoritem = viewer.getItemOnCursor();
					if (cursoritem.getType() == Material.ENDER_PEARL && isItemStackPrisonPearl(cursoritem))
						feedback.append(String.format("In hand of %s viewing chest at (%d,%d,%d)",
								viewer.getName(),
								holder.blocklocation.getBlockX(),
								holder.blocklocation.getBlockY(),
								holder.blocklocation.getBlockZ()));
						return HolderVerReason.IN_VIEWER_HAND;
				}
				feedback.append(String.format(
					"In %s at (%d,%d,%d)", bs.getType().toString(),
					bsLoc.getBlockX(), bsLoc.getBlockY(), bsLoc.getBlockZ()));
			} else if (holder.supposedPearlLocation != null){
				feedback.append("Pearl is not located on current server. Assuming it is legit until told otherwise.");
				return HolderVerReason.OTHER_SERVER;
			}
			else {
				feedback.append("Has no player, item, nor location");
				return HolderVerReason.NO_ITEM_PLAYER_OR_LOCATION;
			}
			for (ItemStack item : inv.all(Material.ENDER_PEARL).values()) {
				if (isItemStackPrisonPearl(item)) {
					if (holder.blocklocation != null)
						feedback.append(String.format("In inventory at (%d,%d,%d)",
							holder.blocklocation.getBlockX(),
							holder.blocklocation.getBlockY(),
							holder.blocklocation.getBlockZ()));
					else if (holder.player != null)
						feedback.append(String.format("In inventory at (%d,%d,%d)",
								holder.player.getLocation().getBlockX(),
								holder.player.getLocation().getBlockY(),
								holder.player.getLocation().getBlockZ()));
					return HolderVerReason.IN_CHEST;
				}
			}
			return HolderVerReason.DEFAULT;
		}
	}

    public boolean verifyLocation() {
        StringBuilder sb = new StringBuilder();

		StringBuilder verifier_log = new StringBuilder();
		StringBuilder failure_reason_log = new StringBuilder();

        for (final Holder holder : this.holders) {
			HolderVerReason reason = verifyHolder(holder, verifier_log);
            if (reason.ordinal() > 5) {

				sb.append(String.format("PP (%s) passed verification for reason %s: %s",
										getPlayerName(), reason.toString(), verifier_log.toString()));
				PrisonPearlPlugin.log(sb.toString());
				
                return true;
            } else {
				failure_reason_log.append(reason.toString()).append(", ");
			}
            verifier_log.append(", ");
        }
		sb.append(String.format("PP (%s) failed verification for reason %s: %s",
				getPlayerName(), failure_reason_log.toString(), verifier_log.toString()));
        PrisonPearlPlugin.log(sb.toString());
        return false;
    }

    public void setHolder(Player player) {
        this.holders.addFirst(new Holder(player));
        this.pearlOnCursor = false;
        while (this.holders.size() > PrisonPearl.HOLDER_COUNT) {
            this.holders.removeLast();
        }
    }

    public void setCursorHolder(Player player) {
        this.holders.addFirst(new Holder(player));
        this.pearlOnCursor = true;
        while (this.holders.size() > PrisonPearl.HOLDER_COUNT) {
            this.holders.removeLast();
        }
    }

    public <ItemBlock extends BlockState & InventoryHolder> void setHolder(ItemBlock blockstate) {
        this.holders.addFirst(new Holder(blockstate.getLocation()));
        this.pearlOnCursor = false;
        while (this.holders.size() > PrisonPearl.HOLDER_COUNT) {
            this.holders.removeLast();
        }
    }
    
    public void setHolder(Location location) {
        this.holders.addFirst(new Holder(location));
        this.pearlOnCursor = false;
        while (this.holders.size() > PrisonPearl.HOLDER_COUNT) {
            this.holders.removeLast();
        }
    }
    
    public void setHolder(FakeLocation location) {
        this.holders.addFirst(new Holder(location));
        this.pearlOnCursor = false;
        while (this.holders.size() > PrisonPearl.HOLDER_COUNT) {
            this.holders.removeLast();
        }
    }

    public void setHolder(Item item) {
        this.holders.addFirst(new Holder(item));
        this.pearlOnCursor = false;
        while (this.holders.size() > PrisonPearl.HOLDER_COUNT) {
            this.holders.removeLast();
        }
    }

    public String getMotd() {
        return motd;
    }

    public void setMotd(String motd) {
    	if (motd != null && motd.equals(""))
    		motd = null;
        this.motd = motd;
    }

    public void markMove() {
        this.lastMoved = System.currentTimeMillis();
    }
    
    public long getLastMoved() {
    	return this.lastMoved;
    }
    
    // ignoreList is a Set of lower-case player names to not send messages to
    public static Set<String> sendProximityMessage(
            final Location location, final Double distance, final String message, final Set<String> ignoreList) {
        final Double distSquared = distance * distance;
        final Double minX = location.getX() - distance;
        final Double maxX = location.getX() + distance;
        final Double minZ = location.getZ() - distance;
        final Double maxZ = location.getZ() + distance;
        final World world = location.getWorld();
        final Set<String> messagedPlayers = new HashSet<String>();
        for (Player other : world.getPlayers()) {
            final Location otherLoc = other.getLocation();
            final Double otherX =  otherLoc.getX();
            if (otherX < minX || otherX > maxX) {
                continue;
            }
            final Double otherZ =  otherLoc.getZ();
            if (otherZ < minZ || otherZ > maxZ) {
                continue;
            }
            final Double otherDistSq = location.distanceSquared(otherLoc);
            if (otherDistSq > distSquared) {
                continue;
            }
            final String otherNameLc = other.getName().toLowerCase();
            if (ignoreList != null && ignoreList.contains(otherNameLc)) {
                continue;
            }
            other.sendMessage(message);
            messagedPlayers.add(otherNameLc);
        }
        return messagedPlayers;
    }
    
    public int getUniqueIdentifier() {
    	return unique;
    }
    
    public UUID getKillerUUID() {
    	return killedUUID;
    }
    
    public String getKillerName() {
    	if (killedUUID == null) {
    		return "Unknown player";
    	}
    	if (PrisonPearlPlugin.isNameLayerEnabled()) {
    		return NameAPI.getCurrentName(killedUUID);
    	}
    	else {
    		return Bukkit.getOfflinePlayer(killedUUID).getName();
    	}
    }
    
    public long getImprisonTime() {
    	return imprisonTime;
    }

	private boolean isItemStackPrisonPearl(ItemStack item) {
		return PrisonPearlPlugin.getPrisonPearlManager().isItemStackPrisonPearl(this, item);
	}
}
