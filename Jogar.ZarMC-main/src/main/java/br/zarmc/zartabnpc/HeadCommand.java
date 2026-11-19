package br.zarmc.zartabnpc;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class HeadCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBS = List.of(
            "create", "delete", "list", "addcmd", "delcmd", "cmds",
            "scale", "speed", "move", "up", "tp", "reload", "help");
    private static final List<String> CMD_PREFIXES = List.of(
            "[connect] ", "[player] ", "[console] ", "[message] ", "[sound] ");

    private final ZarTabNPC plugin;
    private final HeadManager hm;

    public HeadCommand(ZarTabNPC plugin, HeadManager hm) {
        this.plugin = plugin;
        this.hm = hm;
    }

    private static void msg(CommandSender s, String text) {
        s.sendMessage(Util.color("&8[&bZarMC&8] &7" + text));
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        if (!s.hasPermission("zartabnpc.admin")) {
            msg(s, "&cVoce nao tem permissao.");
            return true;
        }
        if (a.length == 0) {
            help(s);
            return true;
        }
        String sub = a[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "create": create(s, a); break;
            case "delete": delete(s, a); break;
            case "list": list(s); break;
            case "addcmd": addcmd(s, a); break;
            case "delcmd": delcmd(s, a); break;
            case "cmds": cmds(s, a); break;
            case "scale": scale(s, a); break;
            case "speed": speed(s, a); break;
            case "move": move(s, a); break;
            case "up": up(s, a); break;
            case "tp": tp(s, a); break;
            case "reload":
                plugin.reloadAll();
                msg(s, "&aConfig, cabecas e TAB recarregados.");
                break;
            default: help(s);
        }
        return true;
    }

    private void help(CommandSender s) {
        msg(s, "&bComandos das cabecas:");
        s.sendMessage(Util.color("&f/zhead create <id> [textura|nick] &7- cria (sem textura usa a cabeca da mao)"));
        s.sendMessage(Util.color("&f/zhead addcmd <id> <comando> &7- adiciona comando no clique"));
        s.sendMessage(Util.color("&f/zhead delcmd <id> <numero> &7- remove comando"));
        s.sendMessage(Util.color("&f/zhead cmds <id> &7- lista os comandos"));
        s.sendMessage(Util.color("&f/zhead scale <id> <tamanho> &7- tamanho"));
        s.sendMessage(Util.color("&f/zhead speed <id> <graus/seg> &7- velocidade (negativo gira ao contrario)"));
        s.sendMessage(Util.color("&f/zhead move <id> &7- move para onde voce esta"));
        s.sendMessage(Util.color("&f/zhead up <id> <blocos> &7- sobe/desce (aceita negativo)"));
        s.sendMessage(Util.color("&f/zhead tp <id> &7- teleporta ate a cabeca"));
        s.sendMessage(Util.color("&f/zhead delete <id> | list | reload"));
        s.sendMessage(Util.color("&7Comandos aceitam: &f[connect] servidor&7, &f[player] cmd&7, &f[console] cmd&7, &f[message] texto&7, &f[sound] nome"));
    }

    /** Pega a cabeca citada em a[1]; avisa e devolve null se nao achar. */
    private Head need(CommandSender s, String[] a, String usage) {
        if (a.length < 2) {
            msg(s, "&eUso: " + usage);
            return null;
        }
        Head h = hm.get(a[1]);
        if (h == null) {
            msg(s, "&cNao existe cabeca com o id &f" + a[1] + "&c. Use /zhead list");
        }
        return h;
    }

    private static void place(Head h, Player p) {
        Location l = p.getEyeLocation();
        h.world = l.getWorld().getName();
        h.x = l.getX();
        h.y = l.getY();
        h.z = l.getZ();
    }

    private void create(CommandSender s, String[] a) {
        if (!(s instanceof Player p)) {
            msg(s, "&cSo da pra usar dentro do jogo.");
            return;
        }
        if (a.length < 2) {
            msg(s, "&eUso: /zhead create <id> [textura|nick]");
            return;
        }
        String id = a[1].toLowerCase(Locale.ROOT);
        if (!id.matches("[a-z0-9_-]+")) {
            msg(s, "&cO id so pode ter letras, numeros, - e _");
            return;
        }
        if (hm.get(id) != null) {
            msg(s, "&cJa existe uma cabeca com esse id.");
            return;
        }
        String tex;
        if (a.length >= 3) {
            tex = Util.normalizeTexture(a[2]);
        } else {
            tex = HeadManager.textureOf(p.getInventory().getItemInMainHand());
            if (tex == null) {
                msg(s, "&cSegure uma cabeca na mao (ex: /hdb) ou passe a textura/nick: /zhead create <id> <textura|nick>");
                return;
            }
        }
        Head h = new Head(id);
        h.texture = tex;
        h.scale = plugin.getConfig().getDouble("heads.default-scale", 2.0);
        h.speed = plugin.getConfig().getDouble("heads.default-speed", 90.0);
        place(h, p);
        hm.add(h);
        msg(s, "&aCabeca &f" + id + " &acriada. Agora use &f/zhead addcmd " + id + " [connect] NomeDoServidor");
    }

    private void delete(CommandSender s, String[] a) {
        Head h = need(s, a, "/zhead delete <id>");
        if (h == null) {
            return;
        }
        hm.remove(h);
        msg(s, "&aCabeca removida.");
    }

    private void list(CommandSender s) {
        if (hm.all().isEmpty()) {
            msg(s, "&eNenhuma cabeca criada ainda.");
            return;
        }
        for (Head h : hm.all().values()) {
            s.sendMessage(Util.color("&f" + h.id + " &7- mundo &f" + h.world + " &7- "
                    + h.commands.size() + " comando(s) - tamanho " + h.scale));
        }
    }

    private void addcmd(CommandSender s, String[] a) {
        Head h = need(s, a, "/zhead addcmd <id> <comando>");
        if (h == null) {
            return;
        }
        if (a.length < 3) {
            msg(s, "&eUso: /zhead addcmd <id> <comando>");
            return;
        }
        String c = String.join(" ", Arrays.copyOfRange(a, 2, a.length));
        h.commands.add(c);
        hm.save();
        msg(s, "&aComando adicionado: &f" + c);
    }

    private void delcmd(CommandSender s, String[] a) {
        Head h = need(s, a, "/zhead delcmd <id> <numero>");
        if (h == null) {
            return;
        }
        if (a.length < 3) {
            msg(s, "&eUso: /zhead delcmd <id> <numero>  (veja os numeros em /zhead cmds <id>)");
            return;
        }
        try {
            int n = Integer.parseInt(a[2]);
            if (n < 1 || n > h.commands.size()) {
                msg(s, "&cNumero invalido.");
                return;
            }
            String removed = h.commands.remove(n - 1);
            hm.save();
            msg(s, "&aRemovido: &f" + removed);
        } catch (NumberFormatException e) {
            msg(s, "&cDigite um numero.");
        }
    }

    private void cmds(CommandSender s, String[] a) {
        Head h = need(s, a, "/zhead cmds <id>");
        if (h == null) {
            return;
        }
        if (h.commands.isEmpty()) {
            msg(s, "&eEssa cabeca ainda nao tem comandos.");
            return;
        }
        for (int i = 0; i < h.commands.size(); i++) {
            s.sendMessage(Util.color("&7" + (i + 1) + ". &f" + h.commands.get(i)));
        }
    }

    private void scale(CommandSender s, String[] a) {
        Head h = need(s, a, "/zhead scale <id> <tamanho>");
        if (h == null) {
            return;
        }
        if (a.length < 3) {
            msg(s, "&eUso: /zhead scale <id> <tamanho>");
            return;
        }
        try {
            h.scale = Math.max(0.1, Double.parseDouble(a[2].replace(',', '.')));
            hm.refresh(h);
            msg(s, "&aTamanho: &f" + h.scale);
        } catch (NumberFormatException e) {
            msg(s, "&cDigite um numero (ex: 2.5).");
        }
    }

    private void speed(CommandSender s, String[] a) {
        Head h = need(s, a, "/zhead speed <id> <graus/seg>");
        if (h == null) {
            return;
        }
        if (a.length < 3) {
            msg(s, "&eUso: /zhead speed <id> <graus/seg>");
            return;
        }
        try {
            h.speed = Double.parseDouble(a[2].replace(',', '.'));
            hm.save();
            msg(s, "&aVelocidade: &f" + h.speed + " graus/seg");
        } catch (NumberFormatException e) {
            msg(s, "&cDigite um numero (ex: 90).");
        }
    }

    private void move(CommandSender s, String[] a) {
        if (!(s instanceof Player p)) {
            msg(s, "&cSo da pra usar dentro do jogo.");
            return;
        }
        Head h = need(s, a, "/zhead move <id>");
        if (h == null) {
            return;
        }
        place(h, p);
        hm.refresh(h);
        msg(s, "&aCabeca movida para a sua posicao.");
    }

    private void up(CommandSender s, String[] a) {
        Head h = need(s, a, "/zhead up <id> <blocos>");
        if (h == null) {
            return;
        }
        if (a.length < 3) {
            msg(s, "&eUso: /zhead up <id> <blocos>");
            return;
        }
        try {
            h.y += Double.parseDouble(a[2].replace(',', '.'));
            hm.refresh(h);
            msg(s, "&aAltura ajustada.");
        } catch (NumberFormatException e) {
            msg(s, "&cDigite um numero (ex: 1.5 ou -0.5).");
        }
    }

    private void tp(CommandSender s, String[] a) {
        if (!(s instanceof Player p)) {
            msg(s, "&cSo da pra usar dentro do jogo.");
            return;
        }
        Head h = need(s, a, "/zhead tp <id>");
        if (h == null) {
            return;
        }
        World w = Bukkit.getWorld(h.world);
        if (w == null) {
            msg(s, "&cO mundo &f" + h.world + " &cnao esta carregado.");
            return;
        }
        p.teleport(new Location(w, h.x, h.y - 1.5, h.z));
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String alias, String[] a) {
        List<String> out = new ArrayList<>();
        if (!s.hasPermission("zartabnpc.admin")) {
            return out;
        }
        String last = a[a.length - 1].toLowerCase(Locale.ROOT);
        if (a.length == 1) {
            for (String x : SUBS) {
                if (x.startsWith(last)) {
                    out.add(x);
                }
            }
        } else if (a.length == 2 && !a[0].equalsIgnoreCase("create")
                && !a[0].equalsIgnoreCase("list") && !a[0].equalsIgnoreCase("reload")) {
            for (String id : hm.all().keySet()) {
                if (id.startsWith(last)) {
                    out.add(id);
                }
            }
        } else if (a.length == 3 && a[0].equalsIgnoreCase("addcmd")) {
            for (String x : CMD_PREFIXES) {
                if (x.startsWith(last)) {
                    out.add(x);
                }
            }
        }
        return out;
    }
}
