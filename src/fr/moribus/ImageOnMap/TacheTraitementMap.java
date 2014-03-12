package fr.moribus.ImageOnMap;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.map.MapView;
import org.bukkit.scheduler.BukkitRunnable;

public class TacheTraitementMap extends BukkitRunnable
{
	int i;
	Player joueur;
 	ImageRendererThread renduImg;
 	PlayerInventory inv;
 	ItemStack map;
 	ImageOnMap plugin;
 	boolean resized, renamed;
	
 	TacheTraitementMap(Player j, String u, ImageOnMap plug, boolean rs, boolean rn)
 	{
 		i = 0;
 		joueur = j;
 		renduImg = new ImageRendererThread(u, rs);
 		renduImg.start();
 		inv = joueur.getInventory();
 		plugin = plug;
 		resized = rs;
 		renamed = rn;
 	}
 	
	@SuppressWarnings("deprecation")
	@Override
	public void run() 
	{
		if(!renduImg.getStatut())
		{
			//joueur.sendMessage("Nombre d'exécution depuis le lancement du timer : " + i);
			i++;
			if(renduImg.erreur)
			{
				joueur.sendMessage("There was a problem while fetching image. Check your URL.");
				cancel();
			}
			if(i > 42)
			{
				joueur.sendMessage("TIMEOUT: the render took too many time");
				cancel();
			}
		}
		else
		{
			cancel();
			int nbImage = renduImg.getImg().length;
			if (plugin.getConfig().getInt("Limit-map-by-server") != 0 && nbImage + ImgUtility.getNombreDeMaps(plugin) > plugin.getConfig().getInt("Limit-map-by-server"))
			{
				joueur.sendMessage("ERROR: cannot render "+ nbImage +" picture(s): the limit of maps per server would be exceeded.");
				return;
			}
			if(joueur.hasPermission("imageonmap.nolimit"))
			{
				
			}
			else
			{
				if (plugin.getConfig().getInt("Limit-map-by-player") != 0 && nbImage + ImgUtility.getNombreDeMapsParJoueur(plugin, joueur.getName()) > plugin.getConfig().getInt("Limit-map-by-player"))
				{
					joueur.sendMessage(ChatColor.RED +"ERROR: cannot render "+ nbImage +" picture(s): the limit of maps allowed for you (per player) would be exceeded.");
					return;
				}
			}
			MapView carte;
			
			ArrayList<ItemStack> restant = new ArrayList<ItemStack>();
			short[] ids = new short[nbImage];
			for (int i = 0; i < nbImage; i++)
			{
				if(nbImage == 1 && joueur.getItemInHand().getType() == Material.MAP)
					carte = Bukkit.getMap(joueur.getItemInHand().getDurability());
				else
					carte = Bukkit.createMap(joueur.getWorld());
				ImageRendererThread.SupprRendu(carte);
				carte.addRenderer(new Rendu(renduImg.getImg()[i]));
				map = new ItemStack(Material.MAP, 1, carte.getId());
				if(nbImage > 1)
				{
					ids[i] = carte.getId();
					if(renamed == true)
					{
						ItemMeta meta = map.getItemMeta();
						meta.setDisplayName("Map (" +renduImg.getNumeroMap().get(i) +")");
						map.setItemMeta(meta);
					}
					
				}
				
				if(nbImage == 1 && joueur.getItemInHand().getType() == Material.MAP)
					joueur.setItemInHand(map);
				else
					ImgUtility.AddMap(map, inv, restant);
				
				//Svg de la map
				SavedMap svg = new SavedMap(plugin, joueur.getName(), carte.getId(), renduImg.getImg()[i], joueur.getWorld().getName());
				svg.SaveMap();
				joueur.sendMap(carte);
			}
			SavedPoster poster;
			if(nbImage > 1)
			{
				poster = new SavedPoster(plugin, ids, joueur.getName());
				poster.Save();
				joueur.sendMessage("Poster ( Id: "+ poster.getId()+ " ) finished");
			}
			else
				joueur.sendMessage("Render finished");
			if(!restant.isEmpty())
				joueur.sendMessage(restant.size()+ " maps can't be place in your inventory. Please make free space in your inventory and run "+ ChatColor.GOLD+  "/maptool getrest");
			plugin.setRemainingMaps(joueur.getName(), restant);
		}
	}

}