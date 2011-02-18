package com.bukkit.exitlights.fairspawn;

import java.io.File;
import org.bukkit.Server;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;

public class FairSpawn extends JavaPlugin{
	
	public FSPlayerListener playerListener;
	public SpawnLocManager spawnLocManager;
	
	public static final int SPAWN_SET_TIME = 30;
   
	public FairSpawn(PluginLoader pluginLoader, Server instance, PluginDescriptionFile desc, File folder, File plugin, ClassLoader cLoader) {
    
		super(pluginLoader, instance, desc, folder, plugin, cLoader);

	}

	public void onEnable() {
		
        // Instantiate member variables
		this.playerListener = new FSPlayerListener(this);
		this.spawnLocManager = new SpawnLocManager(this);
		
        PluginDescriptionFile pdfFile = this.getDescription();
		PluginManager pm = getServer().getPluginManager();
		
        // Load the spawn file
        if (!this.spawnLocManager.loadSpawnLocs()){
        	System.out.println(pdfFile.getName() + ": failed to load spawn location file, " + 
        			pdfFile.getName() + " will be disabled");
        	return;
        }
		
        // Do the rest of the stuff
        pm.registerEvent(Event.Type.PLAYER_ITEM, this.playerListener, Event.Priority.Normal, this);
        pm.registerEvent(Event.Type.PLAYER_RESPAWN, this.playerListener, Event.Priority.Normal, this);
        pm.registerEvent(Event.Type.PLAYER_JOIN, this.playerListener, Event.Priority.Normal, this);
        pm.registerEvent(Event.Type.PLAYER_TELEPORT, this.playerListener, Event.Priority.Normal, this);
        System.out.println( pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!" );
        
	}
	
	public void onDisable() {
		
		spawnLocManager.shutdown();
		System.out.println( this.getDescription().getName() + " shutting down");
		
	}
	
}