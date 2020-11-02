package com.palmergames.bukkit.towny.war.eventwar;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.player.PlayerKilledPlayerEvent;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.utils.CombatUtil;

public class EventWarListener implements Listener {

	public EventWarListener() {
		
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerLogin(PlayerLoginEvent event) {
		War war = TownyUniverse.getInstance().getWarEvent(event.getPlayer());
		if (war == null)
			return;
		war.addOnlineWarrior(event.getPlayer());
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onPlayerLogout(PlayerQuitEvent event) {
		War war = TownyUniverse.getInstance().getWarEvent(event.getPlayer());
		if (war == null)
			return;
		war.removeOnlineWarrior(event.getPlayer());		
	}
	
	@EventHandler
	private void onPlayerKillsPlayer(PlayerKilledPlayerEvent event) {
		Player killer = event.getKiller();
		Player victim = event.getVictim();
		Resident killerRes = event.getKillerRes();
		Resident victimRes = event.getVictimRes();
		War killerWar = TownyUniverse.getInstance().getWarEvent(killer);
		War victimWar = TownyUniverse.getInstance().getWarEvent(victim);

		if (killerWar == null || victimWar == null)
			return; // One of the players is not in a war.
		if (killerWar != victimWar)
			return; // The wars are not the same war.
		if (CombatUtil.isAlly(killer.getName(), victim.getName()))
			return; // They are allies and this was a friendly fire kill.

		victimWar.takeLife(victimRes);
		
		
		Town killerTown = null;
		try {
			killerTown = killerRes.getTown();
		} catch (NotRegisteredException ignored) {}
		
		switch (killerWar.getWarType()) {
		case RIOT:
			break;
		case NATIONWAR:
		case WORLDWAR:
			if (killerWar.getLives(victimRes) == 0 && killerTown != null && TownySettings.isRemovingOnMonarchDeath()) {
				try {
					if (victimRes.hasNation() && victimRes.isKing()) {
						TownyMessaging.sendGlobalMessage(Translation.of("MSG_WAR_KING_KILLED", victimRes.getTown().getNation().getName()));
						killerWar.remove(killerTown, victimRes.getTown().getNation()); // Remove the king's nation from the war.
					}
				} catch (NotRegisteredException ignored) {}
			}
			break;
			
		case CIVILWAR:
		case TOWNWAR:
			if (killerWar.getLives(victimRes) == 0 && killerTown != null && TownySettings.isRemovingOnMonarchDeath()) {
				try {
					if (victimRes.hasTown() && victimRes.isMayor()) {
						TownyMessaging.sendGlobalMessage(Translation.of("MSG_WAR_MAYOR_KILLED", victimRes.getTown().getName()));
						killerWar.remove(killerTown, victimRes.getTown()); // Remove the mayor's town from the war.
					}
				} catch (NotRegisteredException ignored) {}
			}
			break;
		
		}	

		if (TownySettings.getWarPointsForKill() > 0){
			killerWar.residentScoredKillPoints(victimRes, killerRes, event.getLocation());
		}
	}
}
