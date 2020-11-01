package com.palmergames.bukkit.towny.war.eventwar;

public enum WarType {
	RIOT("Riot", true, true, true, true, 1, 100.0),
	TOWNWAR("Town vs Town War", false, false, false, false, 5, 100.0),
	CIVILWAR("National Civil War", false, false, false, false, 5, 100.0),
	NATIONWAR("Nation vs Nation War", false, false, false, false, 10, 1000.0),
	WORLDWAR("World War", true, false, false, false, -1, 10000.0);
	
	String name;
	boolean hasTownBlockHP;
	boolean hasMonarchDeath;
	boolean hasTownBlocksSwitchTowns;
	boolean hasTownConquering;
	int lives;
	double baseSpoils;
	
	/**
	 * 
	 * @param name - Base name used.
	 * @param hasTownBlockHP - Whether townblocks have HP and are fought over for points.
	 * @param hasMonarchDeath - Whether killing the mayor or king will remove the town/nation from the war.
	 * @param lives - How many lives each player gets before being removed from the war.
	 * @param baseSpoils - How much money is added to the war.
	 */
	WarType(String name, boolean hasTownBlockHP, boolean hasMonarchDeath, boolean hasTownBlocksSwitchTowns, boolean hasTownConquering, int lives, double baseSpoils) {
		this.name = name;
		this.hasTownBlockHP = hasTownBlockHP;
		this.hasMonarchDeath = hasMonarchDeath;
		this.hasTownBlocksSwitchTowns = hasTownBlocksSwitchTowns;
		this.hasTownConquering = hasTownConquering;
		this.lives = lives;
		this.baseSpoils = baseSpoils;
	}
	
	public String getName() {
		return name;
	}
}
