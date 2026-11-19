package br.zarmc.zartabnpc;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class ZarTabNPC extends JavaPlugin {
    private HeadManager heads;
    private TabManager tab;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        heads = new HeadManager(this);
        tab = new TabManager(this);
        getServer().getPluginManager().registerEvents(heads, this);
        getServer().getPluginManager().registerEvents(tab, this);

        heads.load();
        heads.start();
        tab.start();

        PluginCommand cmd = getCommand("zhead");
        if (cmd != null) {
            HeadCommand hc = new HeadCommand(this, heads);
            cmd.setExecutor(hc);
            cmd.setTabCompleter(hc);
        }
        getLogger().info("ZarTabNPC 2.0 ligado. Cabecas: " + heads.all().size());
    }

    @Override
    public void onDisable() {
        if (tab != null) {
            tab.stop();
        }
        if (heads != null) {
            heads.shutdown();
        }
    }

    public void reloadAll() {
        reloadConfig();
        heads.shutdown();
        heads.load();
        heads.start();
        tab.start();
    }
}
