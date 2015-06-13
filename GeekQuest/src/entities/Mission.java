package entities;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

@Entity
@Index
public class Mission {

	@Id
	private String description;
	private boolean isAccomplished;
	private boolean isset;

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isAccomplished() {
		return isAccomplished;
	}

	public void setAccomplished(boolean isAccomplished) {
		this.isAccomplished = isAccomplished;
	}

	public boolean isIsset() {
		return isset;
	}

	public void setIsset(boolean isset) {
		this.isset = isset;
	}

}
