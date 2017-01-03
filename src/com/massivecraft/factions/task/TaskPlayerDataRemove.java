package com.massivecraft.factions.task;

import com.massivecraft.factions.Factions;
import com.massivecraft.factions.Rel;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.entity.MPlayerColl;
import com.massivecraft.massivecore.MassiveCore;
import com.massivecraft.massivecore.ModuloRepeatTask;
import com.massivecraft.massivecore.event.EventMassiveCorePlayerLeave;
import com.massivecraft.massivecore.util.MUtil;
import com.massivecraft.massivecore.util.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;

public class TaskPlayerDataRemove extends ModuloRepeatTask
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static TaskPlayerDataRemove i = new TaskPlayerDataRemove();
	public static TaskPlayerDataRemove get() { return i; }
	
	// -------------------------------------------- //
	// OVERRIDE: MODULO REPEAT TASK
	// -------------------------------------------- //
	
	@Override
	public long getDelayMillis()
	{
		return (long) (MConf.get().taskPlayerDataRemoveMinutes * TimeUnit.MILLIS_PER_MINUTE);
	}
	
	@Override
	public void setDelayMillis(long delayMillis)
	{
		MConf.get().taskPlayerDataRemoveMinutes = delayMillis / (double) TimeUnit.MILLIS_PER_MINUTE;
	}
	
	@Override
	public void invoke(long now)
	{
		if ( ! MassiveCore.isTaskServer()) return;
		MPlayerColl.get().considerRemovePlayerMillis();
	}

	// -------------------------------------------- //
	// UPDATE LAST ACTIVITY
	// -------------------------------------------- //

	public static void updateLastActivity(CommandSender sender)
	{
		if (sender == null) throw new RuntimeException("sender");
		if (MUtil.isntSender(sender)) return;

		MPlayer mplayer = MPlayer.get(sender);
		mplayer.setLastActivityMillis();
	}

	public static void updateLastActivitySoon(final CommandSender sender)
	{
		if (sender == null) throw new RuntimeException("sender");
		Bukkit.getScheduler().scheduleSyncDelayedTask(Factions.get(), new Runnable()
		{
			@Override
			public void run()
			{
				updateLastActivity(sender);
			}
		});
	}

	// Can't be cancelled
	@EventHandler(priority = EventPriority.LOWEST)
	public void updateLastActivity(PlayerJoinEvent event)
	{
		// During the join event itself we want to be able to reach the old data.
		// That is also the way the underlying fallback Mixin system does it and we do it that way for the sake of symmetry.
		// For that reason we wait till the next tick with updating the value.
		updateLastActivitySoon(event.getPlayer());
	}

	// Can't be cancelled
	@EventHandler(priority = EventPriority.LOWEST)
	public void updateLastActivity(EventMassiveCorePlayerLeave event)
	{
		// Here we do however update immediately.
		// The player data should be fully updated before leaving the server.
		updateLastActivity(event.getPlayer());
	}

	// -------------------------------------------- //
	// REMOVE PLAYER DATA WHEN BANNED
	// -------------------------------------------- //

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerKick(PlayerKickEvent event)
	{
		// If a player was kicked from the server ...
		Player player = event.getPlayer();

		// ... and if the if player was banned (not just kicked) ...
		//if (!event.getReason().equals("Banned by admin.")) return;
		if (!player.isBanned()) return;

		// ... and we remove player data when banned ...
		if (!MConf.get().removePlayerWhenBanned) return;

		// ... get rid of their stored info.
		MPlayer mplayer = MPlayerColl.get().get(player, false);
		if (mplayer == null) return;

		if (mplayer.getRole() == Rel.LEADER)
		{
			mplayer.getFaction().promoteNewLeader();
		}

		mplayer.leave();
		mplayer.detach();
	}

}
