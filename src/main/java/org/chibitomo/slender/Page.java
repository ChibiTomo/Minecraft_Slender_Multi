package org.chibitomo.slender;

import org.bukkit.Location;

public class Page {
	private int id;
	private String message;
	private Boolean taken = false;
	private Location loc;

	public Page(int id, Location loc, String message) {
		this.id = id;
		this.message = message;
		this.loc = loc;
	}

	public int getId() {
		return id;
	}

	public String getMessage() {
		return message;
	}

	public Boolean isTaken() {
		return taken;
	}

	public void setToken(Boolean taken) {
		this.taken = taken;
	}

	public boolean isLocatedAt(Location location) {
		return loc.getWorld() == location.getWorld()
				&& loc.getBlockX() == location.getBlockX()
				&& loc.getBlockY() == location.getBlockY()
				&& loc.getBlockZ() == location.getBlockZ();
	}

	public Location getLoc() {
		return loc;
	}
}
