package entities;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Parent;

@Entity
public class Potion {

	@Id
	private long id;
	private Color color;
	private long healthpoints;
	@Parent
	private Key<Player> player;

	public int drink() {
		int addedHealth = (int) healthpoints;
		healthpoints = 0;
		return addedHealth;
	}

	public boolean isEmpty() {
		return healthpoints == 0;
	}

	public int getHealthpoints() {
		return (int) healthpoints;
	}

	public void setHealthpoints(int healthpoints) {
		this.healthpoints = healthpoints;
	}

	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public Key<Player> getPlayer() {
		return player;
	}

	public void setPlayer(Key<Player> player) {
		this.player = player;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

}
