package entities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.AlsoLoad;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Load;

@Entity
@Index
public class Player implements Serializable {

	@Id
	private String email;
	// @AlsoLoad("name")
	private String nickname;
	private Charclass charclass;
	private long health;
	private String photo;
	private long score;
	private long gold;
	@Load
	private List<Ref<Player>> mercenaries = new ArrayList<Ref<Player>>();
	@Load
	private List<Ref<Mission>> missions = new ArrayList<Ref<Mission>>();

	public Player(String name, Charclass charclass, long health, String email,
			String photo) {
		super();
		this.nickname = name;
		this.charclass = charclass;
		this.health = health;
		this.email = email;
		this.photo = photo;
	}

	public Player() {
		super();
	}

	public void heal(int points) {
		this.health += points;
	}

	public void hurt(int points) {
		this.health -= points;
	}

	public long getScore() {
		return score;
	}

	public void setScore(long score) {
		this.score = score;
	}

	public String getName() {
		return nickname;
	}

	public void setName(String name) {
		this.nickname = name;
	}

	public Charclass getCharclass() {
		return charclass;
	}

	public void setCharclass(Charclass charclass) {
		this.charclass = charclass;
	}

	public long getHealth() {
		return health;
	}

	public void setHealth(long health) {
		this.health = health;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhoto() {
		return photo;
	}

	public void setPhoto(String photo) {
		this.photo = photo;
	}

	public List<Ref<Mission>> getMissions() {
		return missions;
	}

	public void setMissions(List<Ref<Mission>> missions) {
		this.missions = missions;
	}

	public long getGold() {
		return gold;
	}

	public void setGold(long gold) {
		this.gold = gold;
	}

	public List<Ref<Player>> getMercenaries() {
		return mercenaries;
	}

	public void setMercenaries(List<Ref<Player>> mercenaries) {
		this.mercenaries = mercenaries;
	}

}
