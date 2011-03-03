package us.exitlights.fairspawn;

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
	private boolean consumeItem = false;

	public SpawnLocManager(FairSpawn fairSpawn) {
		
		this.playerMap = new HashMap<String, TimeLoc>();
		this.fairSpawn = fairSpawn;
		
	}

	public boolean loadSpawnFile() {
		
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
		
		Player player = event.getPlayer();
		
		// Make sure we've got the right item in hand before we say anything
		if (!player.getItemInHand().getType().equals(Material.COMPASS))
			return;
		
		// If we have an entry for the player already, check to see if we're allowed to
		// set a new spawn yet
		if (playerMap.get(player.getName()) != null){
			long lastTime = playerMap.get(player.getName()).getTime();
			if (!sufficientTimePassed(lastTime)){
				player.sendMessage("You can't set your spawn for " + timeLeft(lastTime) + " more minutes");
				return;
			}
		}
		
		// Go ahead and set the spawn location
		Location spawnLoc = player.getLocation();
		  
		// Update the player's spawn location
		setSpawn(player, spawnLoc); 
		  
		// Tell the player they've set their spawn, and how long until they can set it again
		player.sendMessage("Spawn location set, " + fairSpawn.SPAWN_SET_TIME + " minutes until next placement");
		
		// Consume the compass, if we're supposed to do so
		if (consumeItem){
			if (player.getInventory().getItemInHand().getAmount() > 1)
				player.getInventory().getItemInHand().setAmount(player.getInventory().getItemInHand().getAmount()-1);
			else
				player.getInventory().removeItem(player.getInventory().getItemInHand());
		}

	}
	
	private long timeLeft(long lastTime){
		
		long currentTime = System.currentTimeMillis();
		long timeDiff = currentTime - lastTime;
		timeDiff = timeDiff / 60000;
		
		return fairSpawn.SPAWN_SET_TIME - timeDiff;
		
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
		
		// Special cases: the two other options (cooldown length, item consumption)
		if (name.equals("cooldown")){
			fairSpawn.SPAWN_SET_TIME = Integer.valueOf(lineScanner.next());
			return true;
		} else if (name.equals("consume")){
			String val = lineScanner.next();
			consumeItem = (val.equals("true")) ? true : false;
			return true;
		}
		
		// Otherwise, process it as a typical line
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
		outString += "\n";
		
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
		TimeLoc tl = playerMap.get(e.getPlayer().getName());
		if (tl == null)
			return;
		Location loc = tl.getLocation();
		
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
		
		// Write the beginning values (cooldown, consume)
		writeHeader();
		
		// Go through the map, rewriting every line
		for (String name : playerMap.keySet() )
			writeLine(name, playerMap.get(name).getLocation(), playerMap.get(name).getTime());
		
	}
	
	private void writeHeader(){
		
		String outString = "consume ";
		outString += (consumeItem) ? "true\n" : "false\n";
		outString += "cooldown ";
		outString += fairSpawn.SPAWN_SET_TIME + "\n";
		
		try {
			fileOutput.write(outString.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
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
