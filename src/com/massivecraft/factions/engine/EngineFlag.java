package com.massivecraft.factions.engine;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Wither;
import org.bukkit.entity.Zombie;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingBreakEvent.RemoveCause;

import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.entity.MFlag;
import com.massivecraft.massivecore.Engine;
import com.massivecraft.massivecore.collections.MassiveSet;
import com.massivecraft.massivecore.ps.PS;

public class EngineFlag extends Engine
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static EngineFlag i = new EngineFlag();
	public static EngineFlag get() { return i; }

	// -------------------------------------------- //
	// CONSTANTS
	// -------------------------------------------- //

	public static final Set<SpawnReason> NATURAL_SPAWN_REASONS = new MassiveSet<SpawnReason>(
		SpawnReason.NATURAL,
		SpawnReason.JOCKEY,
		SpawnReason.CHUNK_GEN,
		SpawnReason.OCELOT_BABY,
		SpawnReason.NETHER_PORTAL,
		SpawnReason.MOUNT
	);

	// -------------------------------------------- //
	// FLAG: MONSTERS & ANIMALS
	// -------------------------------------------- //

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void blockMonstersAndAnimals(CreatureSpawnEvent event)
	{
		// If this is a natural spawn ..
		if ( ! NATURAL_SPAWN_REASONS.contains(event.getSpawnReason())) return;

		// ... get the spawn location ...
		Location location = event.getLocation();
		if (location == null) return;
		PS ps = PS.valueOf(location);

		// ... get the faction there ...
		Faction faction = BoardColl.get().getFactionAt(ps);
		if (faction == null) return;

		// ... get the entity type ...
		EntityType type = event.getEntityType();

		// ... and if this type can't spawn in the faction ...
		if (canSpawn(faction, type)) return;

		// ... then cancel.
		event.setCancelled(true);
	}

	public static boolean canSpawn(Faction faction, EntityType type)
	{
		if (MConf.get().entityTypesMonsters.contains(type))
		{
			// Monster
			return faction.getFlag(MFlag.getFlagMonsters());
		}
		else if (MConf.get().entityTypesAnimals.contains(type))
		{
			// Animal
			return faction.getFlag(MFlag.getFlagAnimals());
		}
		else
		{
			// Other
			return true;
		}
	}

	// -------------------------------------------- //
	// FLAG: EXPLOSIONS
	// -------------------------------------------- //

	protected Set<DamageCause> DAMAGE_CAUSE_EXPLOSIONS = EnumSet.of(DamageCause.BLOCK_EXPLOSION, DamageCause.ENTITY_EXPLOSION);

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void blockExplosion(HangingBreakEvent event)
	{
		// If a hanging entity was broken by an explosion ...
		if (event.getCause() != RemoveCause.EXPLOSION) return;
		Entity entity = event.getEntity();

		// ... and the faction there has explosions disabled ...
		Faction faction = BoardColl.get().getFactionAt(PS.valueOf(entity.getLocation()));
		if (faction.isExplosionsAllowed()) return;

		// ... then cancel.
		event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void blockExplosion(EntityDamageEvent event)
	{
		// If an explosion damages ...
		if ( ! DAMAGE_CAUSE_EXPLOSIONS.contains(event.getCause())) return;

		// ... an entity that is modified on damage ...
		if ( ! MConf.get().entityTypesEditOnDamage.contains(event.getEntityType())) return;

		// ... and the faction has explosions disabled ...
		if (BoardColl.get().getFactionAt(PS.valueOf(event.getEntity())).isExplosionsAllowed()) return;

		// ... then cancel!
		event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void blockExplosion(EntityExplodeEvent event)
	{
		// Prepare some variables:
		// Current faction
		Faction faction = null;
		// Current allowed
		Boolean allowed = true;
		// Caching to speed things up.
		Map<Faction, Boolean> faction2allowed = new HashMap<Faction, Boolean>();

		// If an explosion occurs at a location ...
		Location location = event.getLocation();

		// Check the entity. Are explosions disabled there?
		faction = BoardColl.get().getFactionAt(PS.valueOf(location));
		allowed = faction.isExplosionsAllowed();
		if (allowed == false)
		{
			event.setCancelled(true);
			return;
		}
		faction2allowed.put(faction, allowed);

		// Individually check the flag state for each block
		Iterator<Block> iter = event.blockList().iterator();
		while (iter.hasNext())
		{
			Block block = iter.next();
			faction = BoardColl.get().getFactionAt(PS.valueOf(block));
			allowed = faction2allowed.get(faction);
			if (allowed == null)
			{
				allowed = faction.isExplosionsAllowed();
				faction2allowed.put(faction, allowed);
			}

			if (allowed == false) iter.remove();
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void blockExplosion(EntityChangeBlockEvent event)
	{
		// If a wither is changing a block ...
		Entity entity = event.getEntity();
		if (!(entity instanceof Wither)) return;

		// ... and the faction there has explosions disabled ...
		PS ps = PS.valueOf(event.getBlock());
		Faction faction = BoardColl.get().getFactionAt(ps);

		if (faction.isExplosionsAllowed()) return;

		// ... stop the block alteration.
		event.setCancelled(true);
	}

	// -------------------------------------------- //
	// FLAG: ENDERGRIEF
	// -------------------------------------------- //

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void blockEndergrief(EntityChangeBlockEvent event)
	{
		// If an enderman is changing a block ...
		Entity entity = event.getEntity();
		if (!(entity instanceof Enderman)) return;

		// ... and the faction there has endergrief disabled ...
		PS ps = PS.valueOf(event.getBlock());
		Faction faction = BoardColl.get().getFactionAt(ps);
		if (faction.getFlag(MFlag.getFlagEndergrief())) return;

		// ... stop the block alteration.
		event.setCancelled(true);
	}

	// -------------------------------------------- //
	// FLAG: ZOMBIEGRIEF
	// -------------------------------------------- //

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void denyZombieGrief(EntityBreakDoorEvent event)
	{
		// If a zombie is breaking a door ...
		Entity entity = event.getEntity();
		if (!(entity instanceof Zombie)) return;

		// ... and the faction there has zombiegrief disabled ...
		PS ps = PS.valueOf(event.getBlock());
		Faction faction = BoardColl.get().getFactionAt(ps);
		if (faction.getFlag(MFlag.getFlagZombiegrief())) return;

		// ... stop the door breakage.
		event.setCancelled(true);
	}

	// -------------------------------------------- //
	// FLAG: FIRE SPREAD
	// -------------------------------------------- //

	public void blockFireSpread(Block block, Cancellable cancellable)
	{
		// If the faction at the block has firespread disabled ...
		PS ps = PS.valueOf(block);
		Faction faction = BoardColl.get().getFactionAt(ps);

		if (faction.getFlag(MFlag.getFlagFirespread())) return;

		// then cancel the event.
		cancellable.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void blockFireSpread(BlockIgniteEvent event)
	{
		// If fire is spreading ...
		if (event.getCause() != IgniteCause.SPREAD && event.getCause() != IgniteCause.LAVA) return;

		// ... consider blocking it.
		blockFireSpread(event.getBlock(), event);
	}

	// TODO: Is use of this event deprecated?
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void blockFireSpread(BlockSpreadEvent event)
	{
		// If fire is spreading ...
		if (event.getNewState().getType() != Material.FIRE) return;

		// ... consider blocking it.
		blockFireSpread(event.getBlock(), event);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void blockFireSpread(BlockBurnEvent event)
	{
		// If a block is burning ...

		// ... consider blocking it.
		blockFireSpread(event.getBlock(), event);
	}

}
