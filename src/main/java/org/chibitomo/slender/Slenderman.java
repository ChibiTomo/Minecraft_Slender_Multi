package org.chibitomo.slender;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

public class Slenderman {
	private Player slenderman;
	private int maxTpDistance;
	private Boolean isVisible = true;
	private Slender plugin;
	private boolean isAlreadySeen = false;
	private String oldName;

	public Slenderman(Slender plugin) {
		this.plugin = plugin;
	}

	public boolean isSlenderman(Player player) {
		return player != null && player.equals(slenderman);
	}

	public void setMaxTpDistance(int n) {
		maxTpDistance = n;
	}

	public int getMaxTpDistance() {
		return maxTpDistance;
	}

	public void setSlenderman(Player player) {
		if (slenderman != null) {
			setVisibility(true);
			plugin.setPlayerName(slenderman, oldName);
		}
		if (!isSlenderman(player)) {
			slenderman = player;
			if (slenderman != null) {
				// TODO: Give object in hand.
				slenderman.sendMessage("You are the Slenderman");
				// CraftPlayer p = ((CraftPlayer) slenderman);
				// EntityPlayer ep = p.getHandle();
				// oldName = ep.getName();
				plugin.setPlayerName(slenderman, ChatColor.RED + "Slenderman"
						+ ChatColor.WHITE);
			}
		}
	}

	public Player getPlayer() {
		return slenderman;
	}

	public void setVisibility(Boolean visible) {
		isVisible = visible;
		plugin.setPlayerVisibility(slenderman, visible);
	}

	public void tp(Location loc) {
		float yaw = slenderman.getLocation().getYaw();
		float pitch = slenderman.getLocation().getPitch();
		loc.setYaw(yaw);
		loc.setPitch(pitch);

		BlockFace face = getClosestFace(yaw);

		if (pitch < 0) { // Look up
			loc.setY(loc.getY() - 1);
		} else if (pitch > 0) { // Look down
			loc.setY(loc.getY() + 1);
		} else if (face.compareTo(BlockFace.EAST) == 0) {
			loc.setX(loc.getX() - 1);
		} else if (face.compareTo(BlockFace.WEST) == 0) {
			loc.setX(loc.getX() + 1);
		} else if (face.compareTo(BlockFace.NORTH) == 0) {
			loc.setZ(loc.getZ() - 1);
		} else if (face.compareTo(BlockFace.SOUTH) == 0) {
			loc.setZ(loc.getZ() + 1);
		}

		Boolean visible = isVisible();
		setVisibility(false);
		slenderman.teleport(loc);
		setVisibility(visible);
	}

	private BlockFace getClosestFace(float direction) {
		direction = ((-direction + 90) * -1) - 90;
		direction %= 360;
		if (direction < 0) {
			direction += 360;
		}

		direction = Math.round(direction / 45);

		switch ((int) direction) {

		case 0:
			return BlockFace.SOUTH;
		case 1:
			return BlockFace.SOUTH_WEST;
		case 2:
			return BlockFace.WEST;
		case 3:
			return BlockFace.NORTH_WEST;
		case 4:
			return BlockFace.NORTH;
		case 5:
			return BlockFace.NORTH_EAST;
		case 6:
			return BlockFace.EAST;
		case 7:
			return BlockFace.SOUTH_EAST;
		default:
			return BlockFace.SOUTH;
		}
	}

	public void toogleVisibility() {
		setVisibility(!isVisible());
		String message = "You are now invisible";
		if (isVisible()) {
			message = "You are now visible";
		}
		slenderman.sendMessage(message);
	}

	public boolean isVisible() {
		return isVisible;
	}

	public void kill(Player player) {
		player.sendMessage("Slenderman cought you...");
	}

	public boolean isSeen() {
		int viewDist = plugin.getViewDist();
		List<Player> players = plugin.getNearbyPlayers(slenderman, viewDist);
		boolean slendermanIsSeen = false;

		for (Player p : players) {
			slendermanIsSeen |= plugin.canSeeSlenderman(p) && !p.isDead();
		}
		return slendermanIsSeen;
	}

	public boolean isAlreadySeen() {
		return isAlreadySeen;
	}

	public void setAlreadySeen(boolean isSeen) {
		if (!isAlreadySeen && isSeen) {
			slenderman.getPlayer().sendMessage("Someone sees you...");
		}
		isAlreadySeen = isSeen;
	}
}
