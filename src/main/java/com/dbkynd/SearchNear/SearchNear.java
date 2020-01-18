package com.dbkynd.SearchNear;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import net.md_5.bungee.api.chat.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class SearchNear extends JavaPlugin implements Listener {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args) {
        if (cmd.getName().equalsIgnoreCase("search")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players may use this command!");
                return true;
            }

            Player player = (Player) sender;

            if (!(player.hasPermission("search.use"))) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
                return true;
            }

            if (args.length == 0) {
                player.sendMessage(ChatColor.RED + "Usage: /search <block> [radius]");
                return true;
            }

            Material targetBlock;
            try {
                targetBlock = Material.valueOf(args[0].toUpperCase());
            } catch (Exception e) {
                player.sendMessage(ChatColor.RED + args[0] + " is not a valid block name!");
                return true;
            }

            if (!targetBlock.isBlock()) {
                player.sendMessage(ChatColor.RED + args[0] + " is not a valid block name!");
                return true;
            }

            WorldEditPlugin we = (WorldEditPlugin) Bukkit.getPluginManager().getPlugin("WorldEdit");

            if (we == null) {
                player.sendMessage(ChatColor.RED + "Error communicating with WorldEdit");
                return true;
            }

            BlockVector3 min = null;
            BlockVector3 max = null;

            if (args.length == 1) {
                Region region;
                try {
                    region = we.getSession(player).getSelection(we.getSession(player).getSelectionWorld());
                } catch (IncompleteRegionException | NullPointerException e) {
                    player.sendMessage(ChatColor.RED + "You need to make a WorldEdit selection first or use a radius!");
                    return true;
                }

                min = region.getMinimumPoint();
                max = region.getMaximumPoint();
            } else if (args.length == 2) {
                int radius;
                try {
                    radius = Integer.parseInt(args[1]);
                } catch (Exception e) {
                    player.sendMessage(ChatColor.RED + "The radius must be a number!");
                    return true;
                }

                Location playerLocation = player.getLocation();
                double minY = playerLocation.getY() - radius < 0 ? 0 : playerLocation.getY() - radius;
                double maxY = playerLocation.getY() + radius > 255 ? 255 : playerLocation.getY() + radius;
                min = BlockVector3.at(playerLocation.getX() - radius, minY, playerLocation.getZ() - radius);
                max = BlockVector3.at(playerLocation.getX() + radius, maxY, playerLocation.getZ() + radius);
            }

            assert min != null;
            assert max != null;

            List<Location> matches = new ArrayList<>();

            for (int x = min.getX(); x <= max.getX(); x++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    for (int y = min.getY(); y <= max.getY(); y++) {
                        Block block = player.getWorld().getBlockAt(x, y, z);
                        if (block.getType().toString().equals(args[0].toUpperCase())) {
                            matches.add(new Location(player.getWorld(), x, y, z));
                        }
                    }
                }
            }

            String amount;
            if (matches.size() == 0) {
                amount = ChatColor.RED.toString() + matches.size();
            } else {
                amount = ChatColor.DARK_GREEN.toString() + matches.size();
            }

            player.sendMessage(ChatColor.GREEN + "Found: " + amount + ChatColor.GREEN + " matching blocks!");

            if (matches.size() == 0) return true;

            if (matches.size() <= 10) {
                for (Location match : matches) {
                    TextComponent message = new TextComponent(ChatColor.WHITE + "[" + ChatColor.AQUA + match.getBlockX() + ChatColor.WHITE + ", " + ChatColor.AQUA + match.getBlockY() + ChatColor.WHITE + ", " + ChatColor.AQUA + match.getBlockZ() + ChatColor.WHITE + "]");
                    String command = "/tp " + player.getName() + " " + match.getBlockX() + " " + match.getBlockY() + " " + match.getBlockZ();
                    message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
                    message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to teleport.").create()));
                    player.spigot().sendMessage(message);
                }
            } else {
                player.sendMessage(ChatColor.GREEN + "Results too lengthy for chat. -> Output to plugin folder.");
            }

            if (!getDataFolder().exists()) {
                getDataFolder().mkdir();
            }

            Date date = new Date();
            String fileName = targetBlock.name() + "_" + Math.floorDiv(date.getTime(), 1000) + ".txt";

            File results = new File(getDataFolder(), fileName);

            try {
                results.createNewFile();
                FileWriter fr = new FileWriter(results, true);
                for (Location match : matches) {
                    fr.write("/tp " + match.getBlockX() + " " + match.getBlockY() + " " + match.getBlockZ() + "\n");
                }
                fr.close();
            } catch (IOException e) {
                e.printStackTrace();
                player.sendMessage(ChatColor.RED + "Error saving results to file.");
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {

        if (cmd.getName().equalsIgnoreCase("search")) {
            if (args.length == 1) {

                ArrayList<String> blockNames = new ArrayList<String>();

                if (!(args[0].equals(""))) {
                    for (Material block : Material.values()) {
                        if (block.isBlock()) {
                            if (block.name().toLowerCase().startsWith(args[0].toLowerCase())) {
                                blockNames.add(block.name());
                            }
                        }
                    }
                }
                Collections.sort(blockNames);

                return blockNames;
            }
        }
        return null;
    }
}
