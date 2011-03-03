package us.exitlights.fairspawn;

import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerItemEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class FSPlayerListener extends PlayerListener{
	
	public static FairSpawn plugin;
	public boolean diedFlag;
	 
	public FSPlayerListener(FairSpawn instance) {
	        plugin = instance;
	        diedFlag = false;
	}
	  
	public void onPlayerItem(PlayerItemEvent event){
		
		plugin.spawnLocManager.handleClick(event);
		  
	  }
	  
	  public void onPlayerRespawn(PlayerRespawnEvent e){
		  
		  plugin.spawnLocManager.handleRespawn(e);
		
	  }
	  
	  public void onPlayerJoin(PlayerEvent e){
		  
		  plugin.spawnLocManager.handleJoin(e);
		  
	  }
	  
	  public void onPlayerTeleport(PlayerMoveEvent e){
		  
		  plugin.spawnLocManager.handleJoin(e);
		  
	  }

}