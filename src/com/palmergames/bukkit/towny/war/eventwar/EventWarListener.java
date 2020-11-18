package com.palmergames.bukkit.towny.war.eventwar;

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
		war.getWarParticipants().addOnlineWarrior(event.getPlayer());
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onPlayerLogout(PlayerQuitEvent event) {
		War war = TownyUniverse.getInstance().getWarEvent(event.getPlayer());
		if (war == null)
			return;
		war.getWarParticipants().removeOnlineWarrior(event.getPlayer());		
	}
	
	@EventHandler
	private void onPlayerKillsPlayer(PlayerKilledPlayerEvent event) {
		Resident killerRes = event.getKillerRes();
		Resident victimRes = event.getVictimRes();
		War killerWar = TownyUniverse.getInstance().getWarEvent(event.getKiller());
		War victimWar = TownyUniverse.getInstance().getWarEvent(event.getVictim());

		if (killerWar == null || victimWar == null)
			return; // One of the players is not in a war.
		if (killerWar != victimWar)
			return; // The wars are not the same war.
		if (CombatUtil.isAlly(killerRes.getName(), victimRes.getName()))
			return; // They are allies and this was a friendly fire kill.

		// TODO: Potentially redo removal code to only accept residents and calculate Towns and Nations from them/Not throw NotRegisteredExceptions.
		Town killerTown = null;
		try {
			killerTown = killerRes.getTown();
		} catch (NotRegisteredException ignored) {}
		
		int victimLives = killerWar.getWarParticipants().getLives(victimRes); // Use a variable for this because it will be lost once takeLife(victimRes) is called.
		/*
		 * Take a life off of the victim no matter what type of war it is.
		 */
		victimWar.getWarParticipants().takeLife(victimRes);
		
		/*
		 * Handle king/mayor deaths, which can potentially remove them from the war if they have no more lives.
		 */
		switch (killerWar.getWarType()) {
		
			case RIOT:
				break;
			case NATIONWAR:
			case WORLDWAR:
				/*
				 * Look to see if the king's death would remove the nation from the war.
				 */
				if (victimLives == 0 && TownySettings.isRemovingOnMonarchDeath()) {
					try {
						if (victimRes.hasNation() && victimRes.isKing()) {
							TownyMessaging.sendGlobalMessage(Translation.of("MSG_WAR_KING_KILLED", victimRes.getTown().getNation().getName()));
							killerWar.getWarZoneManager().remove(killerTown, victimRes.getTown().getNation()); // Remove the king's nation from the war.
						}
					} catch (NotRegisteredException ignored) {}
				}
				break;
			case CIVILWAR:
			case TOWNWAR:
				/*
				 * Look to see if the mayor's death would remove the town from the war.
				 */
				if (victimLives == 0 && TownySettings.isRemovingOnMonarchDeath()) {
					try {
						if (victimRes.hasTown() && victimRes.isMayor()) {
							TownyMessaging.sendGlobalMessage(Translation.of("MSG_WAR_MAYOR_KILLED", victimRes.getTown().getName()));
							killerWar.getWarZoneManager().remove(killerTown, victimRes.getTown()); // Remove the mayor's town from the war.
						}
					} catch (NotRegisteredException ignored) {}
				}
				break;
		}	

		/*
		 * Give the killer some points.
		 */
		if (TownySettings.getWarPointsForKill() > 0){
			killerWar.getScoreManager().residentScoredKillPoints(victimRes, killerRes, event.getLocation());
		}
	}
}
