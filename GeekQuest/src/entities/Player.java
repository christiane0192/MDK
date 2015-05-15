package entities;

public class Player {

	private String name;
	private Charclass charclass;
	private long health;
	private String email;
	private String photo;

	public Player(String name, Charclass charclass, long health, String email, String photo){
		this.name=name;
		this.charclass=charclass;
		this.health= health;
		this.email=email;
		this.photo=photo;
	}

	public void heal(int points){
		this.health+=points;
	}

	public void hurt(int points){
		this.health-=points;
	}
}
