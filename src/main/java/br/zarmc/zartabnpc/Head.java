package br.zarmc.zartabnpc;

import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.ArmorStand;

import java.util.ArrayList;
import java.util.List;

final class Head {
    final String id;
    String world = "world";
    double x, y, z;
    float yaw = 0f;
    double scale = 2.0;
    double speed = 90.0;
    String texture;
    final List<String> commands = new ArrayList<>();

    ItemDisplay display;
    Interaction interaction;
    ArmorStand bedrockStand;

    Head(String id) {
        this.id = id;
    }
}
