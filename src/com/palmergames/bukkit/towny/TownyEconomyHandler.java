package com.palmergames.bukkit.towny;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.event.TownyPreTransactionEvent;
import com.palmergames.bukkit.towny.event.TownyTransactionEvent;
import com.palmergames.bukkit.towny.object.economy.adapter.ReserveEconomyAdapter;
import com.palmergames.bukkit.towny.object.Government;
import com.palmergames.bukkit.towny.object.Transaction;
import com.palmergames.bukkit.towny.object.TransactionType;
import com.palmergames.bukkit.towny.object.economy.adapter.EconomyAdapter;
import com.palmergames.bukkit.towny.object.economy.adapter.VaultEconomyAdapter;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.Colors;

import net.milkbowl.vault.economy.Economy;
import net.tnemc.core.Reserve;

import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.UUID;

/**
 * Economy handler to interface with Register or Vault directly.
 * 
 * @author ElgarL
 * @author Suneet Tipirneni (Siris)
 */
public class TownyEconomyHandler {

	private static Towny plugin = null;
	private static EconomyAdapter economy = null;
	private static EcoType Type = EcoType.NONE;
	private static String version = "";
	
	public enum EcoType {
		NONE, VAULT, RESERVE
	}
	
	public static String getServerAccount() {
		return TownySettings.getString(ConfigNodes.ECO_CLOSED_ECONOMY_SERVER_ACCOUNT);
	}
	
	public static UUID getUUIDServerAccount() {
		return UUID.fromString(TownySettings.getString(ConfigNodes.ECO_CLOSED_ECONOMY_SERVER_ACCOUNT));
	}

	public static void initialize(Towny plugin) {
		TownyEconomyHandler.plugin = plugin;
	}

	/**
	 * @return the economy type we have detected.
	 */
	public static EcoType getType() {
		return Type;
	}

	/**
	 * Are we using any economy system?
	 * 
	 * @return true if we found one.
	 */
	public static boolean isActive() {
		return (Type != EcoType.NONE && TownySettings.isUsingEconomy());
	}

	/**
	 * @return The current economy providers version string
	 */
	public static String getVersion() {
		return version;
	}

	/**
	 * Internal function to set the version string.
	 * 
	 * @param version The version of this eco.
	 */
	private static void setVersion(String version) {
		TownyEconomyHandler.version = version;
	}

	/**
	 * Find and configure a suitable economy provider
	 * 
	 * @return true if successful.
	 */
	public static boolean setupEconomy() {

		Plugin economyProvider;

		/*
		 * Attempt to find Vault for Economy handling
		 */
		try {
			RegisteredServiceProvider<Economy> vaultEcoProvider = plugin.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
			if (vaultEcoProvider != null) {
				/*
				 * Flag as using Vault hooks
				 */
				economy = new VaultEconomyAdapter(vaultEcoProvider.getProvider());
				setVersion(String.format("%s %s", vaultEcoProvider.getProvider().getName(), "via Vault" ));
				Type = EcoType.VAULT;
				return true;
			}
		} catch (NoClassDefFoundError ignored) {
		}

		/*
		 * Attempt to find Reserve for Economy handling
		 */
		economyProvider = plugin.getServer().getPluginManager().getPlugin("Reserve");
		if(economyProvider != null && ((Reserve)economyProvider).economyProvided()) {
			/*
			 * Flat as using Reserve Hooks.
			 */
			economy = new ReserveEconomyAdapter(((Reserve) economyProvider).economy());
			setVersion(String.format("%s %s", ((Reserve) economyProvider).economy().name(), "via Reserve" ));
			Type = EcoType.RESERVE;
			return true;
		}

		/*
		 * No compatible Economy system found.
		 */
		return false;
	}

	/**
	 * Check if account exists
	 * 
	 * @param uniqueId the UUID of the account to check
	 * @return true if the account exists
	 */
	public static boolean hasEconomyAccount(UUID uniqueId) {

		return economy.hasAccount(uniqueId);
	}

	/**
	 * Check if account exists
	 * 
	 * @param gov Government owning this account.
	 * @return true if the account exists
	 */
	public static boolean hasEconomyAccount(Government gov) {

		return economy.hasAccount(gov);
	}
	
	/**
	 * Attempt to delete the economy account.
	 * 
	 * @param accountName name of the account to delete
	 */
	@Deprecated
	public static void removeAccount(String accountName) {
		economy.deleteAccount(accountName);
	}
	
	public static void removeAccount(UUID uuid) {
		economy.deleteAccount(uuid);
	}
	
	public static void removeAccount(Government gov) {
		economy.deleteAccount(gov);
	}

	/**
	 * Returns the accounts current balance
	 * 
	 * @param accountName name of the economy account
	 * @param world name of world to check in (for TNE Reserve)   
	 * @return double containing the total in the account
	 */
	@Deprecated
	public static double getBalance(String accountName, World world) {
		checkNewAccount(accountName);
		return economy.getBalance(accountName, world);
	}

	public static double getBalance(UUID uuid, World world) {
		checkNewAccount(uuid);
		return economy.getBalance(uuid, world);
	}
	
	public static double getBalance(Government gov, World world) {
		checkNewAccount(gov);
		return economy.getBalance(gov, world);
	}
	
	/**
	 * Returns true if the account has enough money
	 * 
	 * @param accountName name of an economy account
	 * @param amount minimum amount to check against (Double)
	 * @param world name of the world to check in (for TNE Reserve)   
	 * @return true if there is enough in the account
	 */
	@Deprecated
	public static boolean hasEnough(String accountName, double amount, World world) {
		return getBalance(accountName, world) >= amount;
	}
	
	public static boolean hasEnough(UUID uuid, double amount, World world) {
		return getBalance(uuid, world) >= amount;
	}
	
	public static boolean hasEnough(Government gov, double amount, World world) {
		return getBalance(gov, world) >= amount;
	}
	
	private static boolean runPreChecks(Transaction transaction, UUID uuid) {
		TownyPreTransactionEvent preEvent = new TownyPreTransactionEvent(transaction);
		BukkitTools.getPluginManager().callEvent(preEvent);

		if (preEvent.isCancelled()) {
			TownyMessaging.sendErrorMsg(transaction.getUUID(), preEvent.getCancelMessage());
			return false;
		}

		checkNewAccount(uuid);
		return true;
	}

	public static boolean subtract(UUID uuid, double amount, World world) {
		Transaction transaction = new Transaction(TransactionType.SUBTRACT, uuid, amount);
		TownyTransactionEvent event = new TownyTransactionEvent(transaction);
		
		if (!runPreChecks(transaction, uuid)) {
			return false;
		}
		
		if (economy.subtract(uuid, amount, world)) {
			BukkitTools.getPluginManager().callEvent(event);
			return true;
		}
		
		return false;
	}

	/**
	 * Add funds to an account.
	 * 
	 * @param uuid UUID to add funds to
	 * @param amount amount of currency to add
	 * @param world name of world (for TNE Reserve)
	 * @return true if successful
	 */
	public static boolean add(UUID uuid, double amount, World world) {

		Transaction transaction = new Transaction(TransactionType.ADD, uuid, amount);
		TownyTransactionEvent event = new TownyTransactionEvent(transaction);

		if (!runPreChecks(transaction, uuid)) {
			return false;
		}

		if (economy.add(uuid, amount, world)) {
			BukkitTools.getPluginManager().callEvent(event);
			return true;
		}

		return false;
	}
	
	public static boolean subtract(Government gov, double amount, World world) {
		Transaction transaction = new Transaction(TransactionType.SUBTRACT, gov.getUUID(), amount);
		TownyTransactionEvent event = new TownyTransactionEvent(transaction);
		
		if (!runPreChecks(transaction, gov.getUUID())) {
			return false;
		}
		
		if (economy.subtract(gov, amount, world)) {
			BukkitTools.getPluginManager().callEvent(event);
			return true;
		}
		
		return false;
	}

	/**
	 * Add funds to an account.
	 * 
	 * @param gov Government to add funds to
	 * @param amount amount of currency to add
	 * @param world name of world (for TNE Reserve)
	 * @return true if successful
	 */
	public static boolean add(Government gov, double amount, World world) {

		Transaction transaction = new Transaction(TransactionType.ADD, gov.getUUID(), amount);
		TownyTransactionEvent event = new TownyTransactionEvent(transaction);

		if (!runPreChecks(transaction, gov.getUUID())) {
			return false;
		}

		if (economy.add(gov, amount, world)) {
			BukkitTools.getPluginManager().callEvent(event);
			return true;
		}

		return false;
	}

	/**
	 * Attempts to remove an amount from an account
	 * 
	 * @param accountName name of the account to modify
	 * @param amount amount of currency to remove from the account
	 * @param world name of the world in which to check in (TNE Reserve)   
	 * @return true if successful
	 */
	@Deprecated
	public static boolean subtract(String accountName, double amount, World world) {

		if (economy.subtract(accountName, amount, world)) {
			return true;
		}
		
		return false;
	}

	/**
	 * Add funds to an account.
	 * 
	 * @param accountName account to add funds to
	 * @param amount amount of currency to add
	 * @param world name of world (for TNE Reserve)
	 * @return true if successful
	 */
	@Deprecated
	public static boolean add(String accountName, double amount, World world) {

		if (economy.add(accountName, amount, world)) {
			return true;
		}

		return false;
	}

	@Deprecated
	public static boolean setBalance(String accountName, double amount, World world) {
		checkNewAccount(accountName);
		return economy.setBalance(accountName, amount, world);
	}

	public static boolean setBalance(UUID uuid, double amount, World world) {
		checkNewAccount(uuid);
		return economy.setBalance(uuid, amount, world);
	}

	public static boolean setBalance(Government gov, double amount, World world) {
		checkNewAccount(gov);
		return economy.setBalance(gov, amount, world);
	}
	
	/**
	 * Format this balance according to the current economy systems settings.
	 * 
	 * @param balance account balance passed by the economy handler
	 * @return string containing the formatted balance
	 */
	public static String getFormattedBalance(double balance) {

		String formattedBalance = economy.getFormattedBalance(balance);
		if (formattedBalance != null) {
			return Colors.translateColorCodes(formattedBalance);
		}

		return Colors.translateColorCodes(String.format("%.2f", balance));

	}

	/**
	 * Adds money to the server account (used for towny closed economy.)
	 * 
	 * @param amount The amount to deposit.
	 * @param world The world of the deposit.
	 * @return A boolean indicating success.
	 */
	public static boolean addToServer(double amount, World world) {
		return add(getUUIDServerAccount(), amount, world);
	}

	/**
	 * Removes money to the server account (used for towny closed economy.)
	 *
	 * @param amount The amount to withdraw.
	 * @param world The world of the withdraw.
	 * @return A boolean indicating success.
	 */
	public static boolean subtractFromServer(double amount, World world) {
		return subtract(getUUIDServerAccount(), amount, world);
	}
	@Deprecated
	private static void checkNewAccount(String accountName) {
		// Check if the account exists, if not create one.
		if (!economy.hasAccount(accountName)) {
			economy.newAccount(accountName);
		}
	}
	
	private static void checkNewAccount(UUID uuid) {
		// Check if the account exists, if not create one.
		if (!economy.hasAccount(uuid)) {
			economy.newAccount(uuid);
		}
	}
	
	private static void checkNewAccount(Government gov) {
		// Check if the account exists, if not create one.
		if (!economy.hasAccount(gov)) {
			economy.newAccount(gov);
		}
	}

	public static void newAccount(Government gov) {
		economy.newAccount(gov);
	}
	
	public static boolean hasAccount(Government gov) {
		return economy.hasAccount(gov);
	}
	
	public static void newAccount(UUID uuid) {
		economy.newAccount(uuid);
	}
	
	public static boolean hasAccount(UUID uuid) {
		return economy.hasAccount(uuid);
	}

	@Deprecated
	public static void newAccount(String accountName) {
		economy.newAccount(accountName);
	}
	@Deprecated
	public static boolean hasAccount(String accountName) {
		return economy.hasAccount(accountName);
	}

}