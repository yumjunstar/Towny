package com.palmergames.bukkit.towny.object.economy;

import java.util.UUID;

import com.google.common.base.Charsets;
import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownySettings;

/**
 * For internal use only.
 */
public class TownyServerAccount extends Account {
	public TownyServerAccount() {
		super(UUID.nameUUIDFromBytes((TownySettings.getString(ConfigNodes.ECO_CLOSED_ECONOMY_SERVER_ACCOUNT).getBytes(Charsets.UTF_8))));
	}

	@Override
	protected boolean addMoney(double amount) {
		return TownyEconomyHandler.add(uuid, amount, world);
	}

	@Override
	protected boolean subtractMoney(double amount) {
		return TownyEconomyHandler.subtract(uuid, amount, world);
	}

	@Override
	public String getName() {
		return TownySettings.getString(ConfigNodes.ECO_CLOSED_ECONOMY_SERVER_ACCOUNT);
	}
}
