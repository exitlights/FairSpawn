package com.bukkit.exitlights.fairspawn;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class SpawnLocManager {
	
	private HashMap<String, TimeLoc> playerMap;
	private FileInputStream fileInput;
	private FileOutputStream fileOutput;
	private FairSpawn fairSpawn;
	private File file;

	public SpawnLocManager(FairSpawn fairSpawn) {
		
		this.playerMap = new HashMap<String, TimeLoc>();
		this.fairSpawn = fairSpawn;
		
	}

	public boolean loadSpawnLocs() {
		
		// Open the file
		file = new File("spawnLocs");
		if (!file.exists())
			try {
				file.createNewFile();
			} catch (IOException e) {
				return false;
			}
			
		// Open the input stream
		try {
			fileInput = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			return false;
		}
		
		// Open the output stream
		try {
			fileOutput = new FileOutputStream(file, true);
		} catch (FileNotFoundException e) {
			return false;
		}
		
		// Process the file by loading it into the playerMap
		if (!processFile())
			return false;
		
		return true;
		
	}
	
	public void handleClick(PlayerItemEvent event){
		
		// Make sure we've got the right item in hand before we say anything
		if (!event.getPlayer().getItemInHand().getType().equals(Material.COMPASS))
			return;
		
		// If we have an entry for the player already, check to see if we're allowed to
		// set a new spawn yet
		if (playerMap.get(event.getPlayer().getName()) != null){
			long lastTime = playerMap.get(event.getPlayer().getName()).getTime();
			if (!sufficientTimePassed(lastTime)){
				event.getPlayer().sendMessage("You can't set your spawn for " + timeLeft(lastTime) + " more minutes");
				return;
			}
		}
		
		// Go ahead and set the spawn location
		Location spawnLoc = event.getPlayer().getLocation();
		  
		// Update the player's spawn location
		setSpawn(event.getPlayer(), spawnLoc); 
		  
		// Tell the player they've set their spawn, and how long until they can set it again
		event.getPlayer().sendMessage("Spawn location set, " + FairSpawn.SPAWN_SET_TIME + " minutes until next placement");

		}
	
	private long timeLeft(long lastTime){
		
		long currentTime = System.currentTimeMillis();
		long timeDiff = currentTime - lastTime;
		timeDiff = timeDiff / 60000;
		
		return FairSpawn.SPAWN_SET_TIME - timeDiff;
		
	}
	
	private boolean sufficientTimePassed(long lastTime){
		
		long time = timeLeft(lastTime);
		
		if (time > 0)
			return false;

		return true;
		
	}
	
	private boolean processFile(){
		
		Scanner fileScanner = new Scanner(fileInput);
		
		while (fileScanner.hasNextLine()){
			
			String line = fileScanner.nextLine();
			Scanner lineScanner = new Scanner(line);
			
			while (lineScanner.hasNext())
				if (!processLine(lineScanner))
					return false;
				
		}
		
		return true;
		
	}

	private boolean processLine(Scanner lineScanner) {

		String name = lineScanner.next();
		double x = Double.valueOf(lineScanner.next());
		double y = Double.valueOf(lineScanner.next());
		double z = Double.valueOf(lineScanner.next());
		float yaw = Float.valueOf(lineScanner.next());
		float pitch = Float.valueOf(lineScanner.next());
		long time = Long.valueOf(lineScanner.next());
		
		TimeLoc tl = new TimeLoc(new Location(fairSpawn.getServer().getWorlds().get(0), x, y, z, yaw, pitch), 
				time);
		
		playerMap.put(name, tl);
		
		return true;
		
	}
	
	private boolean writeLine(String name, Location location, long time){
		
		String outString = name;
		outString += " " + location.getX();
		outString += " " + location.getY();
		outString += " " + location.getZ();
		outString += " " + location.getYaw();
		outString += " " + location.getPitch();
		outString += " " + time;
		outString += "\t\n\t";
		
		try {
			fileOutput.write(outString.getBytes());
		} catch (IOException e) {
			return false;
		}
		
		return true;
		
	}

	public void setSpawn(Player player, Location spawnLoc) {
		
		long time = System.currentTimeMillis();
		
		// Update the HashMap
		playerMap.put(player.getName(), new TimeLoc(spawnLoc, time));
		
		// Set the compass location
		player.setCompassTarget(spawnLoc);
		
		// Update the file
		writeLine(player.getName(), spawnLoc, time);
		
	}

	public void handleRespawn(PlayerRespawnEvent e) {
		
		// Get the location based on the player name
		Location loc = playerMap.get(e.getPlayer().getName()).getLocation();
		
		// If we didn't get a location, just dump the event
		if (loc == null)
			return;
		
		// Otherwise, set the location and set where the compass points
		e.setRespawnLocation(loc);
		
	}
	
	private void rewriteFile(){
		
		// Close the file...
		try {
			fileOutput.close();
			fileInput.close();
		} catch (IOException e) {
			System.out.println("Failed to close");
		}
		
		// Reopen the file...
		try {
			fileOutput = new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			return;
		}
		
		// Go through the map, rewriting every line
		for (String name : playerMap.keySet() )
			writeLine(name, playerMap.get(name).getLocation(), playerMap.get(name).getTime());
		
	}
	
	public void shutdown(){
		
		// Re-write the whole file
		rewriteFile();
		
		// Close the streams
		try {
			fileOutput.close();
			fileInput.close();
		} catch (IOException e) {
			System.out.println("Failed to close");
		}
		
	}

	public void handleJoin(PlayerEvent e) {

		// See if there's a location for this player
		TimeLoc tl = playerMap.get(e.getPlayer().getName());
		if (tl == null)
			return;
		
		// If there is, set their compass to point in that direction
		Location playerSpawn = tl.getLocation();
		e.getPlayer().setCompassTarget(playerSpawn);
			
	}
	
}
