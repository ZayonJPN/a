package br.zarmc.zartabnpc;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class HeadManager implements Listener {
    private final ZarTabNPC plugin;
    private final Map<String, Head> heads = new LinkedHashMap<>();
    private final Map<UUID, Long> cooldown = new HashMap<>();
    private BukkitTask task;
    private int visibilityTicks = 0;

    public HeadManager(ZarTabNPC plugin) {
        this.plugin = plugin;
    }

    Map<String, Head> all() {
        return heads;
    }

    Head get(String id) {
        return heads.get(id.toLowerCase(Locale.ROOT));
    }

    void add(Head h) {
        heads.put(h.id, h);
        save();
    }

    void remove(Head h) {
        despawn(h);
        heads.remove(h.id);
        save();
    }

    /** Remove as entidades; o tick() recria na hora com os novos dados. */
    void refresh(Head h) {
        despawn(h);
        save();
    }

    // ------------------------------------------------------------ arquivo

    void load() {
        heads.clear();
        File file = new File(plugin.getDataFolder(), "heads.yml");
        if (!file.exists()) {
            return;
        }
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = y.getConfigurationSection("heads");
        if (sec == null) {
            return;
        }
        for (String id : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(id);
            if (s == null) {
                continue;
            }
            Head h = new Head(id.toLowerCase(Locale.ROOT));
            h.world = s.getString("world", "world");
            h.x = s.getDouble("x");
            h.y = s.getDouble("y");
            h.z = s.getDouble("z");
            h.scale = s.getDouble("scale", 2.0);
            h.speed = s.getDouble("speed", 90.0);
            h.texture = s.getString("texture", "");
            h.commands.addAll(s.getStringList("commands"));
            heads.put(h.id, h);
        }
    }

    void save() {
        File file = new File(plugin.getDataFolder(), "heads.yml");
        YamlConfiguration y = new YamlConfiguration();
        for (Head h : heads.values()) {
            String p = "heads." + h.id + ".";
            y.set(p + "world", h.world);
            y.set(p + "x", h.x);
            y.set(p + "y", h.y);
            y.set(p + "z", h.z);
            y.set(p + "scale", h.scale);
            y.set(p + "speed", h.speed);
            y.set(p + "texture", h.texture);
            y.set(p + "commands", new ArrayList<>(h.commands));
        }
        try {
            plugin.getDataFolder().mkdirs();
            y.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Nao consegui salvar heads.yml: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------ ciclo

    void start() {
        if (task != null) {
            task.cancel();
        }
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 1L);
    }

    void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (Head h : heads.values()) {
            despawn(h);
        }
    }

    private void tick() {
        for (Head h : heads.values()) {
            World w = Bukkit.getWorld(h.world);
            if (w == null) {
                continue;
            }
            int cx = ((int) Math.floor(h.x)) >> 4;
            int cz = ((int) Math.floor(h.z)) >> 4;
            if (!w.isChunkLoaded(cx, cz)) {
                continue;
            }
            boolean broken = h.display == null || !h.display.isValid()
                    || h.interaction == null || !h.interaction.isValid();
            if (broken) {
                despawn(h);
                spawn(h, w);
            }
            h.yaw += (float) (h.speed / 20.0);
            if (h.yaw >= 360f) {
                h.yaw -= 360f;
            }
            h.display.setRotation(h.yaw, 0f);
            if (h.bedrockStand != null && h.bedrockStand.isValid()) {
                h.bedrockStand.setRotation(h.yaw, 0f);
            }
        }
        if (++visibilityTicks >= 20) {
            visibilityTicks = 0;
            for (Player player : Bukkit.getOnlinePlayers()) syncVisibility(player);
        }
    }

    private void spawn(Head h, World w) {
        Location loc = new Location(w, h.x, h.y, h.z);
        ItemStack item = buildHead(h.texture);
        float s = (float) h.scale;

        h.display = w.spawn(loc, ItemDisplay.class, d -> {
            d.setItemStack(item);
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.HEAD);
            d.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f), new AxisAngle4f(),
                    new Vector3f(s, s, s), new AxisAngle4f()));
            d.setBrightness(new Display.Brightness(15, 15));
            d.setPersistent(false);
            d.setInvulnerable(true);
        });

        double mult = plugin.getConfig().getDouble("heads.interaction-size-multiplier", 1.0);
        float size = (float) (h.scale * mult);
        Location il = loc.clone().subtract(0, size / 2.0, 0);
        h.interaction = w.spawn(il, Interaction.class, i -> {
            i.setInteractionWidth(size);
            i.setInteractionHeight(size);
            i.setResponsive(true);
            i.setPersistent(false);
            i.setInvulnerable(true);
        });
        // ArmorStand e entendido pelo cliente Bedrock via Geyser, ao contrario de ItemDisplay.
        if (plugin.getConfig().getBoolean("heads.bedrock-fallback.enabled", true)) {
            h.bedrockStand = w.spawn(loc.clone().subtract(0, 1.45, 0), ArmorStand.class, stand -> {
                stand.setInvisible(true);
                stand.setGravity(false);
                stand.setInvulnerable(true);
                stand.setSilent(true);
                stand.setPersistent(false);
                stand.setBasePlate(false);
                stand.setArms(false);
                stand.setSmall(false);
                stand.getEquipment().setHelmet(item);
            });
        }
        for (Player player : w.getPlayers()) syncVisibility(player);
    }

    private void despawn(Head h) {
        if (h.display != null) {
            h.display.remove();
            h.display = null;
        }
        if (h.interaction != null) {
            h.interaction.remove();
            h.interaction = null;
        }
        if (h.bedrockStand != null) {
            h.bedrockStand.remove();
            h.bedrockStand = null;
        }
    }

    // Floodgate e opcional: usa reflexao para nao exigir dependencia na compilacao.
    private boolean isBedrock(Player player) {
        try {
            Class<?> api = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object instance = api.getMethod("getInstance").invoke(null);
            return Boolean.TRUE.equals(api.getMethod("isFloodgatePlayer", UUID.class)
                    .invoke(instance, player.getUniqueId()));
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    private void syncVisibility(Player player) {
        boolean bedrock = isBedrock(player);
        for (Head h : heads.values()) {
            if (h.display != null && h.display.isValid()) {
                if (bedrock && h.bedrockStand != null) player.hideEntity(plugin, h.display);
                else player.showEntity(plugin, h.display);
            }
            if (h.bedrockStand != null && h.bedrockStand.isValid()) {
                if (bedrock) player.showEntity(plugin, h.bedrockStand);
                else player.hideEntity(plugin, h.bedrockStand);
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline()) syncVisibility(event.getPlayer());
        }, 20L);
    }

    // ------------------------------------------------------------ cabecas

    static ItemStack buildHead(String tex) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (tex != null && tex.length() > 40) {
            PlayerProfile pp = Bukkit.createProfile(
                    UUID.nameUUIDFromBytes(tex.getBytes(StandardCharsets.UTF_8)), "ZarHead");
            pp.setProperty(new ProfileProperty("textures", tex));
            meta.setPlayerProfile(pp);
        } else if (tex != null && !tex.isEmpty()) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(tex));
        }
        item.setItemMeta(meta);
        return item;
    }

    static String textureOf(ItemStack item) {
        if (item == null || item.getType() != Material.PLAYER_HEAD) {
            return null;
        }
        if (!(item.getItemMeta() instanceof SkullMeta meta)) {
            return null;
        }
        PlayerProfile pp = meta.getPlayerProfile();
        if (pp == null) {
            return null;
        }
        for (ProfileProperty prop : pp.getProperties()) {
            if (prop.getName().equals("textures")) {
                return prop.getValue();
            }
        }
        return null;
    }

    // ------------------------------------------------------------ clique

    private Head findByEntity(Entity ent) {
        UUID id = ent.getUniqueId();
        for (Head h : heads.values()) {
            if (h.interaction != null && h.interaction.getUniqueId().equals(id)) {
                return h;
            }
            if (h.display != null && h.display.getUniqueId().equals(id)) return h;
            if (h.bedrockStand != null && h.bedrockStand.getUniqueId().equals(id)) return h;
        }
        return null;
    }

    private boolean handle(Player p, Entity ent) {
        Head h = findByEntity(ent);
        if (h == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        Long last = cooldown.get(p.getUniqueId());
        if (last != null && now - last < 500L) {
            return true;
        }
        cooldown.put(p.getUniqueId(), now);
        for (String c : new ArrayList<>(h.commands)) {
            run(p, c);
        }
        return true;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (handle(e.getPlayer(), e.getRightClicked())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onAttack(PrePlayerAttackEntityEvent e) {
        if (handle(e.getPlayer(), e.getAttacked())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player p && handle(p, e.getEntity())) {
            e.setCancelled(true);
        }
    }

    /**
     * Formatos:
     *  [connect] servidor   -> manda o jogador para outro servidor do Velocity
     *  [player] comando     -> jogador executa
     *  [console] comando    -> console executa
     *  [message] texto      -> manda mensagem
     *  [sound] nome         -> toca som (ex: entity.player.levelup)
     *  sem prefixo          -> jogador executa
     * %player% vira o nome de quem clicou.
     */
    private void run(Player p, String raw) {
        String cmd = Util.apply(p, raw).trim();
        String low = cmd.toLowerCase(Locale.ROOT);
        if (low.startsWith("[console]")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), strip(cmd.substring(9)));
        } else if (low.startsWith("[player]")) {
            p.performCommand(strip(cmd.substring(8)));
        } else if (low.startsWith("[connect]")) {
            connect(p, cmd.substring(9).trim());
        } else if (low.startsWith("[message]")) {
            p.sendMessage(Util.color(cmd.substring(9).trim()));
        } else if (low.startsWith("[sound]")) {
            p.playSound(p.getLocation(), cmd.substring(7).trim(), 1.0f, 1.0f);
        } else {
            p.performCommand(strip(cmd));
        }
    }

    private static String strip(String c) {
        c = c.trim();
        return c.startsWith("/") ? c.substring(1) : c;
    }

    private void connect(Player p, String server) {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(b);
        try {
            out.writeUTF("Connect");
            out.writeUTF(server);
        } catch (IOException ignored) {
            return;
        }
        p.sendPluginMessage(plugin, "BungeeCord", b.toByteArray());
    }
}
