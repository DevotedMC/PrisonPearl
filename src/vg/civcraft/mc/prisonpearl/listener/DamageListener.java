package vg.civcraft.mc.prisonpearl.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TippedArrow;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

import vg.civcraft.mc.prisonpearl.PrisonPearlPlugin;
import vg.civcraft.mc.prisonpearl.managers.DamageLogManager;

public class DamageListener implements Listener {
	
	private DamageLogManager manager;
	
	public DamageListener() {
		Bukkit.getPluginManager().registerEvents(this, PrisonPearlPlugin.getInstance());
		manager = PrisonPearlPlugin.getDamageLogManager();
	}
	
	// Reset damage logs on dead players
	@EventHandler(priority = EventPriority.MONITOR)
	public void onEntityDeath(EntityDeathEvent event) {
		if (!(event.getEntity() instanceof Player))
			return;

		manager.removeDamage(((Player) event.getEntity()).getName());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event) {
		manager.removeDamage(event.getPlayer().getName());
	}

	// Create damage logs
	@EventHandler(priority = EventPriority.MONITOR)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		if (!(event.getEntity() instanceof Player))
			return;
		Player player = (Player) event.getEntity();

		Player damager = null;
		if (event.getDamager() instanceof Player) {
			damager = (Player) event.getDamager();
		} else if (event.getDamager() instanceof Wolf) {
			Wolf wolf = (Wolf) event.getDamager();
			if (wolf.getOwner() instanceof Player) {
				damager = (Player) wolf.getOwner();
			}
		} else if (event.getDamager() instanceof Arrow) {
			Arrow arrow = (Arrow) event.getDamager();
			if (!(arrow.getShooter() instanceof Player)) {
				return;
			}
			damager = (Player) arrow.getShooter();
		/*TODO: Seriously review this and see if we're missing any events w/ 1.9 - 1.10
		 * } else if (event.getDamager() instanceof Projectile) {
			Projectile general = (Projectile) event.getDamager();
			if (!(general.getShooter() instanceof Player)) {
				return;
			}
			damager = (Player) general.getShooter();*/
		}

		if (damager == null || damager == player)
			return;

		manager.recordDamage(player, damager, event.getDamage());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPotionSplashEvent(PotionSplashEvent event) {
		ProjectileSource ps = event.getPotion().getShooter();
		LivingEntity shooter;
		if (!(ps instanceof LivingEntity))
			return;
		shooter = (LivingEntity) ps;
		if (!(shooter instanceof Player))
			return;
		Player damager = (Player) shooter;

		// So, the idea here is because we can't really determine how much
		// damage a potion actually caused
		// somebody (like poison, weakness, or the API doesn't even seem to tell
		// you the difference between harm I and harm II),
		// we just award 6 damage points to the thrower as long as the potion is
		// sufficiently bad.
		int damage = 6;

		boolean badpotion = false;
		for (PotionEffect effect : event.getPotion().getEffects()) {
			// apparently these aren't really enums, because == doesn't work
			if (effect.getType().equals(PotionEffectType.HARM) || effect.getType().equals(PotionEffectType.POISON)
					|| effect.getType().equals(PotionEffectType.WEAKNESS)) {
				badpotion = true;
				break;
			}
		}

		if (!badpotion) // don't award damage for helpful or do-nothing potions,
						// to prevent pearl stealing
			return;

		for (Entity entity : event.getAffectedEntities()) {
			if (!(entity instanceof Player))
				continue;

			manager.recordDamage((Player) entity, damager, damage);
		}
	}
}
