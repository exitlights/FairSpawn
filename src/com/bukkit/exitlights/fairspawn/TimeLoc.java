package com.bukkit.exitlights.fairspawn;

import org.bukkit.Location;

public class TimeLoc {
	
	private Location location;
	private long time;
	
	public TimeLoc(Location location, long time){
		this.location = location;
		this.time = time;
	}
	
	public Location getLocation(){
		return this.location;
	}
	
	public long getTime(){
		return this.time;
	}

}
