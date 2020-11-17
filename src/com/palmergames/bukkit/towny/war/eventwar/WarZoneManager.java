package com.palmergames.bukkit.towny.war.eventwar;

import java.util.Hashtable;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.war.eventwar.events.PlotAttackedEvent;
import com.palmergames.bukkit.util.Colors;

public class WarZoneManager {
	private War war;
	private static Hashtable<WorldCoord, Integer> warZone = new Hashtable<>();
	
	public WarZoneManager(War war) {

		this.war = war;
	}
	
	public Hashtable<WorldCoord, Integer> getWarZone() {

		return warZone;
	}
	
	public void addWarZone(WorldCoord coord, int health) {
		
		warZone.put(coord, health);
	}
	
	public boolean isWarZone(WorldCoord worldCoord) {

		return warZone.containsKey(worldCoord);
	}

	/*
	 * WarZone Updating 
	 */

	/**
	 * Update a plot given the WarZoneData on the TownBlock
	 * @param townBlock - {@link TownBlock}
	 * @param wzd - {@link WarZoneData}
	 * @throws NotRegisteredException - Generic
	 */
	public void updateWarZone (TownBlock townBlock, WarZoneData wzd) throws NotRegisteredException {
		if (!wzd.hasAttackers()) 
			healPlot(townBlock, wzd);
		else
			attackPlot(townBlock, wzd);
	}

	/**
	 * Heals a plot. Only occurs when the plot has no attackers.
	 * @param townBlock - The {@link TownBlock} to be healed.
	 * @param wzd - {@link WarZoneData}
	 * @throws NotRegisteredException - Generic
	 */
	private void healPlot(TownBlock townBlock, WarZoneData wzd) throws NotRegisteredException {
		WorldCoord worldCoord = townBlock.getWorldCoord();
		int healthChange = wzd.getHealthChange();
		int oldHP = warZone.get(worldCoord);
		int hp = getHealth(townBlock, healthChange);
		if (oldHP == hp)
			return;
		warZone.put(worldCoord, hp);
		String healString =  Colors.Gray + "[Heal](" + townBlock.getCoord().toString() + ") HP: " + hp + " (" + Colors.LightGreen + "+" + healthChange + Colors.Gray + ")";
		TownyMessaging.sendPrefixedTownMessage(townBlock.getTown(), healString);
		for (Player p : wzd.getDefenders()) {
			if (com.palmergames.bukkit.towny.TownyUniverse.getInstance().getDataSource().getResident(p.getName()).getTown() != townBlock.getTown())
				TownyMessaging.sendMessage(p, healString);
		}
		WarUtil.launchFireworkAtPlot (townBlock, wzd.getRandomDefender(), Type.BALL, Color.LIME);

		//Call PlotAttackedEvent to update scoreboard users
		PlotAttackedEvent event = new PlotAttackedEvent(townBlock, wzd.getAllPlayers(), hp, war);
		Bukkit.getServer().getPluginManager().callEvent(event);
	}
	
	/**
	 * Correctly returns the health of a {@link TownBlock} given the change in the health.
	 * 
	 * @param townBlock - The TownBlock to get health of
	 * @param healthChange - Modifier to the health of the TownBlock ({@link Integer})
	 * @return the health of the TownBlock
	 */
	private int getHealth(TownBlock townBlock, int healthChange) {
		WorldCoord worldCoord = townBlock.getWorldCoord();
		int hp = warZone.get(worldCoord) + healthChange;
		boolean isHomeBlock = townBlock.isHomeBlock();
		if (isHomeBlock && hp > TownySettings.getWarzoneHomeBlockHealth())
			return TownySettings.getWarzoneHomeBlockHealth();
		else if (!isHomeBlock && hp > TownySettings.getWarzoneTownBlockHealth())
			return TownySettings.getWarzoneTownBlockHealth();
		return hp;
	}
	
	/**
	 * There are attackers on the plot, update the health.
	 * @param townBlock - The {@link TownBlock} being attacked
	 * @param wzd - {@link WarZoneData}
	 * @throws NotRegisteredException - Generic
	 */
	private void attackPlot(TownBlock townBlock, WarZoneData wzd) throws NotRegisteredException {

		Player attackerPlayer = wzd.getRandomAttacker();
		Resident attackerResident = com.palmergames.bukkit.towny.TownyUniverse.getInstance().getDataSource().getResident(attackerPlayer.getName());
		Town attacker = attackerResident.getTown();

		//Health, messaging, fireworks..
		WorldCoord worldCoord = townBlock.getWorldCoord();
		int healthChange = wzd.getHealthChange();
		int hp = getHealth(townBlock, healthChange);
		Color fwc = healthChange < 0 ? Color.RED : (healthChange > 0 ? Color.LIME : Color.GRAY);
		if (hp > 0) {
			warZone.put(worldCoord, hp);
			String healthChangeStringDef, healthChangeStringAtk;
			if (healthChange > 0) { 
				healthChangeStringDef = "(" + Colors.LightGreen + "+" + healthChange + Colors.Gray + ")";
				healthChangeStringAtk = "(" + Colors.Red + "+" + healthChange + Colors.Gray + ")";
			}
			else if (healthChange < 0) {
				healthChangeStringDef = "(" + Colors.Red + healthChange + Colors.Gray + ")";
				healthChangeStringAtk = "(" + Colors.LightGreen + healthChange + Colors.Gray + ")";
			}
			else {
				healthChangeStringDef = "(+0)";
				healthChangeStringAtk = "(+0)";
			}
			if (!townBlock.isHomeBlock()){
				TownyMessaging.sendPrefixedTownMessage(townBlock.getTown(), Colors.Gray + Translation.of("msg_war_town_under_attack") + " (" + townBlock.getCoord().toString() + ") HP: " + hp + " " + healthChangeStringDef);
				if ((hp >= 10 && hp % 10 == 0) || hp <= 5){
					WarUtil.launchFireworkAtPlot (townBlock, attackerPlayer, Type.BALL_LARGE, fwc);
					for (Town town: townBlock.getTown().getNation().getTowns())
						if (town != townBlock.getTown())
							TownyMessaging.sendPrefixedTownMessage(town, Colors.Gray + Translation.of("msg_war_nation_under_attack") + " [" + townBlock.getTown().getName() + "](" + townBlock.getCoord().toString() + ") HP: " + hp + " " + healthChangeStringDef);
					for (Nation nation: townBlock.getTown().getNation().getAllies())
						if (nation != townBlock.getTown().getNation())
							TownyMessaging.sendPrefixedNationMessage(nation , Colors.Gray + Translation.of("msg_war_nations_ally_under_attack", townBlock.getTown().getName()) + " [" + townBlock.getTown().getName() + "](" + townBlock.getCoord().toString() + ") HP: " + hp + " " + healthChangeStringDef);
				}
				else
					WarUtil.launchFireworkAtPlot (townBlock, attackerPlayer, Type.BALL, fwc);
				for (Town attackingTown : wzd.getAttackerTowns())
					TownyMessaging.sendPrefixedTownMessage(attackingTown, Colors.Gray + "[" + townBlock.getTown().getName() + "](" + townBlock.getCoord().toString() + ") HP: " + hp + " " + healthChangeStringAtk);
			} else {
				TownyMessaging.sendPrefixedTownMessage(townBlock.getTown(), Colors.Gray + Translation.of("msg_war_homeblock_under_attack")+" (" + townBlock.getCoord().toString() + ") HP: " + hp + " " + healthChangeStringDef);
				if ((hp >= 10 && hp % 10 == 0) || hp <= 5){
					WarUtil.launchFireworkAtPlot (townBlock, attackerPlayer, Type.BALL_LARGE, fwc);
					for (Town town: townBlock.getTown().getNation().getTowns())
						if (town != townBlock.getTown())
							TownyMessaging.sendPrefixedTownMessage(town, Colors.Gray + Translation.of("msg_war_nation_member_homeblock_under_attack", townBlock.getTown().getName()) + " [" + townBlock.getTown().getName() + "](" + townBlock.getCoord().toString() + ") HP: " + hp + " " + healthChangeStringDef);
					for (Nation nation: townBlock.getTown().getNation().getAllies())
						if (nation != townBlock.getTown().getNation())
							TownyMessaging.sendPrefixedNationMessage(nation , Colors.Gray + Translation.of("msg_war_nation_ally_homeblock_under_attack", townBlock.getTown().getName()) + " [" + townBlock.getTown().getName() + "](" + townBlock.getCoord().toString() + ") HP: " + hp + " " + healthChangeStringDef);
				}
				else
					WarUtil.launchFireworkAtPlot (townBlock, attackerPlayer, Type.BALL, fwc);
				for (Town attackingTown : wzd.getAttackerTowns())
					TownyMessaging.sendPrefixedTownMessage(attackingTown, Colors.Gray + "[" + townBlock.getTown().getName() + "](" + townBlock.getCoord().toString() + ") HP: " + hp + " " + healthChangeStringAtk);
			}
		} else {
			WarUtil.launchFireworkAtPlot (townBlock, attackerPlayer, Type.CREEPER, fwc);
			war.getWarParticipants().remove(attacker, townBlock);
		}

		//Call PlotAttackedEvent to update scoreboard users
		PlotAttackedEvent event = new PlotAttackedEvent(townBlock, wzd.getAllPlayers(), hp, war);
		Bukkit.getServer().getPluginManager().callEvent(event);
	}
}
