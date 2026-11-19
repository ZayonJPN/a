package br.zarmc.zartabnpc;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * TAB proprio: header/footer animados, nome na tab, nametag,
 * ordenacao por grupo e scoreboard (sidebar) - cada jogador tem o seu quadro.
 */
public final class TabManager implements Listener {
    private static final class Fmt {
        String team;
        Component tab;
        Component prefix;
        Component suffix;
    }

    private final ZarTabNPC plugin;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();
    private BukkitTask task;
    private long animTick = 0;

    public TabManager(ZarTabNPC plugin) {
        this.plugin = plugin;
    }

    void start() {
        stop();
        FileConfiguration c = plugin.getConfig();
        if (!c.getBoolean("tab.enabled", true)) {
            return;
        }
        int period = Math.max(1, c.getInt("tab.update-ticks", 10));
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::update, 20L, period);
    }

    void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (boards.containsKey(p.getUniqueId())) {
                p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                p.playerListName(null);
                p.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
            }
        }
        boards.clear();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (task != null) {
            Bukkit.getScheduler().runTaskLater(plugin, this::update, 2L);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        boards.remove(p.getUniqueId());
        for (Scoreboard sb : boards.values()) {
            Team t = sb.getEntryTeam(p.getName());
            if (t != null) {
                t.removeEntry(p.getName());
            }
        }
    }

    // ------------------------------------------------------------ loop

    private void update() {
        FileConfiguration c = plugin.getConfig();
        animTick += Math.max(1, c.getInt("tab.update-ticks", 10));
        boolean collision = c.getBoolean("tab.collision", false);

        Collection<? extends Player> online = Bukkit.getOnlinePlayers();
        Map<UUID, Fmt> fmts = new HashMap<>();
        for (Player p : online) {
            fmts.put(p.getUniqueId(), build(p, c));
        }

        String headerFrame = frame(c.getStringList("tab.header.frames"), c.getInt("tab.header.interval", 20));
        String footerFrame = frame(c.getStringList("tab.footer.frames"), c.getInt("tab.footer.interval", 20));

        for (Player p : online) {
            Scoreboard sb = boards.computeIfAbsent(p.getUniqueId(),
                    k -> Bukkit.getScoreboardManager().getNewScoreboard());
            if (p.getScoreboard() != sb) {
                p.setScoreboard(sb);
            }

            Fmt mine = fmts.get(p.getUniqueId());
            if (!Objects.equals(p.playerListName(), mine.tab)) {
                p.playerListName(mine.tab);
            }

            p.sendPlayerListHeaderAndFooter(
                    Util.color(Util.apply(p, headerFrame)),
                    Util.color(Util.apply(p, footerFrame)));

            for (Player t : online) {
                applyTeam(sb, t, fmts.get(t.getUniqueId()), collision);
            }
            cleanup(sb);
            sidebar(p, sb, c);
        }
    }

    private String frame(List<String> frames, int interval) {
        if (frames.isEmpty()) {
            return "";
        }
        long idx = (animTick / Math.max(1, interval)) % frames.size();
        return frames.get((int) idx);
    }

    // ------------------------------------------------------------ formato

    private Fmt build(Player p, FileConfiguration c) {
        String group = "default";
        if (Util.hasPapi()) {
            String g = Util.apply(p, c.getString("tab.group-placeholder", "%luckperms_primary_group_name%"));
            if (g != null && !g.isEmpty() && !g.contains("%")) {
                group = g.toLowerCase(Locale.ROOT);
            }
        }
        String base = "tab.groups." + group + ".";
        int prio = c.getInt(base + "priority", c.getInt("tab.default-priority", 50));
        String tabFmt = c.getString(base + "tab-format", c.getString("tab.tab-format", "%player%"));
        String pre = c.getString(base + "nametag-prefix", c.getString("tab.nametag-prefix", ""));
        String suf = c.getString(base + "nametag-suffix", c.getString("tab.nametag-suffix", ""));
        boolean sorting = c.getBoolean("tab.sorting", true);

        String name = p.getName();
        if (name.length() > 14) {
            name = name.substring(0, 14);
        }
        int order = sorting ? Math.max(0, Math.min(99, prio)) : 50;

        Fmt f = new Fmt();
        f.team = String.format("%02d", order) + name;
        f.tab = Util.color(Util.apply(p, tabFmt));
        f.prefix = Util.color(Util.apply(p, pre));
        f.suffix = Util.color(Util.apply(p, suf));
        return f;
    }

    private void applyTeam(Scoreboard sb, Player target, Fmt f, boolean collision) {
        Team team = sb.getTeam(f.team);
        if (team == null) {
            team = sb.registerNewTeam(f.team);
            team.setOption(Team.Option.COLLISION_RULE,
                    collision ? Team.OptionStatus.ALWAYS : Team.OptionStatus.NEVER);
        }
        if (!f.prefix.equals(team.prefix())) {
            team.prefix(f.prefix);
        }
        if (!f.suffix.equals(team.suffix())) {
            team.suffix(f.suffix);
        }
        if (!team.hasEntry(target.getName())) {
            team.addEntry(target.getName());
        }
    }

    /** Remove times de jogadores que ficaram vazios (mudou de grupo / saiu). */
    private void cleanup(Scoreboard sb) {
        for (Team t : new ArrayList<>(sb.getTeams())) {
            String n = t.getName();
            if (!n.isEmpty() && Character.isDigit(n.charAt(0)) && t.getEntries().isEmpty()) {
                t.unregister();
            }
        }
    }

    // ------------------------------------------------------------ sidebar

    private static String entry(int i) {
        return "\u00a7" + Integer.toHexString(i) + "\u00a7r";
    }

    private void sidebar(Player p, Scoreboard sb, FileConfiguration c) {
        Objective obj = sb.getObjective("zsb");
        boolean enabled = c.getBoolean("scoreboard.enabled", true)
                && !c.getStringList("scoreboard.disabled-worlds").contains(p.getWorld().getName());
        if (!enabled) {
            if (obj != null) {
                obj.unregister();
            }
            return;
        }

        List<String> lines = c.getStringList("scoreboard.lines");
        if (lines.size() > 15) {
            lines = lines.subList(0, 15);
        }

        if (obj == null) {
            obj = sb.registerNewObjective("zsb", Criteria.DUMMY, Component.empty());
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
            if (c.getBoolean("scoreboard.hide-numbers", true)) {
                obj.numberFormat(NumberFormat.blank());
            }
        }

        String title = frame(c.getStringList("scoreboard.title.frames"), c.getInt("scoreboard.title.interval", 20));
        obj.displayName(Util.color(Util.apply(p, title)));

        for (int i = 0; i < 15; i++) {
            String entry = entry(i);
            if (i < lines.size()) {
                Team team = sb.getTeam("zl" + i);
                if (team == null) {
                    team = sb.registerNewTeam("zl" + i);
                    team.addEntry(entry);
                }
                Component line = Util.color(Util.apply(p, lines.get(i)));
                if (!line.equals(team.prefix())) {
                    team.prefix(line);
                }
                obj.getScore(entry).setScore(lines.size() - i);
            } else {
                sb.resetScores(entry);
            }
        }
    }
}
