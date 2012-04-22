package com.untamedears.PrisonPearl;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.StorageMinecart;
import org.bukkit.inventory.InventoryHolder;

public class PrisonPearl {
	private short id;
	private String imprisonedname;
	private InventoryHolder holder;
	
	public PrisonPearl(short id, String imprisonedname, InventoryHolder holder) {
		this.id = id;
		this.imprisonedname = imprisonedname;
		this.holder = holder;
	}
	
	public short getID() {
		return id;
	}
	
	public boolean isValid() {
		return id != -1;
	}
	
	public String getImprisonedName() {
		return imprisonedname;
	}
	
	public Player getImprisonedPlayer() {
		return Bukkit.getPlayerExact(imprisonedname);
	}
	
	public InventoryHolder getHolder() {
		return holder;
	}
	
	public Entity getHolderEntity() {
		if (holder instanceof Entity)
			return (Entity)holder;
		else
			return null;
	}
	
	public BlockState getHolderBlockState() {
		if (holder instanceof BlockState) {
			return (BlockState)holder;
		} else if (holder instanceof DoubleChest) {
			return (BlockState)((DoubleChest)holder).getLeftSide();
		} else {
			return null;
		}
	}
	
	public Location getHolderLocation() {
		if (holder instanceof Entity) {
			return ((Entity)holder).getLocation();
		} else if (holder instanceof BlockState) {
			return ((BlockState)holder).getLocation();
		} else if (holder instanceof DoubleChest) {
			return ((DoubleChest)holder).getLocation();
		} else {
			return null; // TODO log these 
		}
	}
	
	public String getHolderName() {
		Entity entity;
		BlockState state;
		if ((entity = getHolderEntity()) != null) {
			if (entity instanceof Player) {
				return ((Player)entity).getDisplayName();
			} else if (entity instanceof StorageMinecart) {
				return "a storage minecart";
			} else {
				return "an unknown entity"; // TODO log these
			}
		} else if ((state = getHolderBlockState()) != null) {
			switch (state.getType()) {
			case CHEST:
				return "a chest";
			case FURNACE:
				return "a furnace";
			case BREWING_STAND:
				return "a brewing stand";
			default:
				return "an unknown block"; // TODO log these
			}
		} else {
			return null; // TODO log these (really shouldn't happen)
		}
	}
	
	public void setHolder(InventoryHolder holder) {
		this.holder = holder;
		
		pearlEvent(this, PrisonPearlEvent.Type.HELD);
	}
	
	private void pearlEvent(PrisonPearl pp, PrisonPearlEvent.Type type) {
		Bukkit.getPluginManager().callEvent(new PrisonPearlEvent(pp, type));
	}
}
