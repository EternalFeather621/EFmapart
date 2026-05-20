package com.eternalfeather.efmapart.economy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class EconomyManager {

    private final JavaPlugin plugin;
    private Economy economy;

    public EconomyManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("没有找到 Vault，经济扣费功能无法使用。");
            return false;
        }

        RegisteredServiceProvider<Economy> provider =
                Bukkit.getServicesManager().getRegistration(Economy.class);

        if (provider == null) {
            plugin.getLogger().warning("Vault 没有找到可用的经济插件，例如 EssentialsX。");
            return false;
        }

        economy = provider.getProvider();

        plugin.getLogger().info("已成功连接经济插件：" + economy.getName());
        return true;
    }

    public boolean isAvailable() {
        return economy != null;
    }

    public double getBalance(Player player) {
        if (economy == null) {
            return 0.0;
        }

        return economy.getBalance(player);
    }

    public boolean has(Player player, double amount) {
        if (economy == null) {
            return false;
        }

        return economy.has(player, amount);
    }

    public boolean withdraw(Player player, double amount) {
        if (economy == null) {
            return false;
        }

        EconomyResponse response = economy.withdrawPlayer(player, amount);

        return response.transactionSuccess();
    }
}