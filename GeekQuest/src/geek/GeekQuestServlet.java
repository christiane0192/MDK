package geek;

import static com.googlecode.objectify.ObjectifyService.ofy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.*;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.*;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.apphosting.api.DatastorePb.Transaction;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Ref;

import entities.Charclass;
import entities.Mission;
import entities.Player;

@SuppressWarnings("serial")
public class GeekQuestServlet extends HttpServlet {

	// private DatastoreService datastore;
	private UserService userService;
	private User user;

	// register all entity classes for your application at application startup,
	// before Objectify is used
	static {
		ObjectifyService.register(Mission.class);
		ObjectifyService.register(Player.class);
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

		initializeData();

		// generate test data in database with low level API
		// putHighscorePlayerToDatabase();
		// generateMissions();

		// generate test data in database with Object Mapper
		// putPlayerToDatabaseWithObjectify();
		// putMissionsToDatabaseWithObjectify();

		if (user == null) {
			resp.sendRedirect(userService.createLoginURL(req.getRequestURI()));
		} else {
			if (hasCharacter(user)) {
				if (userWantsToEditData(req)) {
					// set player data in view
					handleEditCharacter(req, resp);
				} else {
					resp.sendRedirect("/welcome");
				}
			} else {
				handleUserHasNoCharacter(req, resp);
			}
		}
	}

	public void initializeData() {
		// datastore = DatastoreServiceFactory.getDatastoreService();
		userService = UserServiceFactory.getUserService();
		user = userService.getCurrentUser();
	}

	public void handleEditCharacter(HttpServletRequest req,
			HttpServletResponse resp) {
		setPlayerAttributesInView(req);
		dispatchRequest(req, resp, "/pages/characterdesign.jsp");
	}

	public void setPlayerAttributesInView(HttpServletRequest req) {
		// Entity player = getPlayerFromDatabase();
		Player player = getPlayerFromDatabase();
		if (req.getAttribute("hobbitname") != null) {
			req.setAttribute("pname", req.getAttribute("hobbitname"));
		} else {
			// req.setAttribute("pname", player.getProperty("playername"));
			req.setAttribute("pname", player.getName());
		}
		// List<Entity> missions = getAllMissions();
		List<Mission> missions = getAllMissions();
		// List<Entity> playerMissions = getPlayerMissionsFromEmbedded(player);
		List<Mission> playerMissions = getPlayerMissions(player);
		for (int i = 0; i < missions.size(); i++) {
			// Entity mission = missions.get(i);
			Mission mission = missions.get(i);
			// for (Entity playerMission : playerMissions) {
			for (Mission playerMission : playerMissions) {
				// if (playerMission.getKey().equals(mission.getKey()))
				if (playerMission.getDescription().equals(
						mission.getDescription())) {
					mission.setIsset(true);
				}
			}
			// if (mission.getProperty("isset") == null) {
			// mission.setProperty("isset", false);
			// }
		}
		req.setAttribute("missions", missions);
		String[] charclasses = Charclass.names();
		String[] newCharclasses = new String[Charclass.names().length];
		int index = 1;
		if (req.getAttribute("hobbitname") != null) {
			newCharclasses[0] = "HOBBIT";
		} else {
			// newCharclasses[0] = (String) player.getProperty("character");
			newCharclasses[0] = (String) player.getCharclass().toString();
		}

		for (String charclass : charclasses) {
			if (!charclass.equals(newCharclasses[0])) {
				newCharclasses[index] = charclass;
				index++;
			}
		}
		req.setAttribute("characters", newCharclasses);
		req.setAttribute("user", user);
	}

	// public ArrayList<Entity> getPlayerMissionsFromEmbedded(Entity player) {
	// ArrayList<Entity> missions = new ArrayList<Entity>();
	// ArrayList<EmbeddedEntity> embeddedMissions = (ArrayList<EmbeddedEntity>)
	// player
	// .getProperty("missions");
	// if (embeddedMissions != null) {
	// for (EmbeddedEntity embeddedMission : embeddedMissions) {
	// Key infoKey = embeddedMission.getKey();
	// Entity mission = new Entity(infoKey);
	//
	// mission.setPropertiesFrom(embeddedMission);
	// missions.add(mission);
	// }
	// }
	// return missions;
	// }

	public List<Mission> getPlayerMissions(Player player) {
		List<Ref<Mission>> refs = player.getMissions();
		Collection<Mission> missions = ofy().load().refs(refs).values();
		return new ArrayList<Mission>(missions);
	}

	public Player getPlayerFromDatabase() {
		// public Entity getPlayerFromDatabase() {
		// Key key = KeyFactory.createKey("Player", user.getEmail());
		// Entity entity;
		// try {
		// entity = datastore.get(key);
		// return entity;
		// } catch (EntityNotFoundException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// return null;

		// Player player = ofy().load().key(arg0)
		// List<Player> players = ofy().load().type(Player.class)
		// .filter("email", user.getEmail()).list();
		Player player = ofy().load().type(Player.class).id(user.getEmail())
				.now();
		return player;
	}

	public boolean hasCharacter(User user) {
		// Query q = new Query("Player");
		// Filter userFilter = new FilterPredicate("email",
		// FilterOperator.EQUAL,
		// user.getEmail());
		// q.setFilter(userFilter);
		// PreparedQuery pq = datastore.prepare(q);
		// Entity entity = pq.asSingleEntity();

		Player player = new Player();
		player.setEmail(user.getEmail());
		Key<Player> playerKey = Key.create(player);
		List<Player> players = ofy().load().type(Player.class)
				.filterKey("=", playerKey).list();
		return players != null && !players.isEmpty();
	}

	public boolean userWantsToEditData(HttpServletRequest req) {
		return req.getParameter("edit") != null
				|| req.getAttribute("edit") != null;
	}

	public void handleUserHasNoCharacter(HttpServletRequest req,
			HttpServletResponse resp) {

		if (!areMissionsSetInGUI(req)) {
			// List<Entity> missions = getAllMissions();
			List<Mission> missions = getAllMissions();
			// for (Entity mission : missions) {
			for (Mission mission : missions) {
				// mission.setProperty("isset", false);
				mission.setIsset(false);
			}
			req.setAttribute("missions", missions);
			req.setAttribute("characters", Charclass.names());
			req.setAttribute("user", user);
			dispatchRequest(req, resp, "/pages/characterdesign.jsp");
		}
	}

	public boolean areMissionsSetInGUI(HttpServletRequest req) {
		return req.getAttribute("missions") != null
				&& !req.getAttribute("missions").equals("");
	}

	public void dispatchRequest(HttpServletRequest req,
			HttpServletResponse resp, String forwardedFile) {
		RequestDispatcher rd = getServletContext().getRequestDispatcher(
				forwardedFile);
		try {
			rd.forward(req, resp);
		} catch (ServletException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// public List<Entity> generateMissions() {
	// List<Entity> missions = new ArrayList<Entity>();
	//
	// String kindDestroyRing = "Mission";
	// String nameDestroyRing = "Destroying Ring";
	// Key keyDestroyRing = KeyFactory.createKey(kindDestroyRing,
	// nameDestroyRing);
	// Entity missionDestroyRing = new Entity(keyDestroyRing);
	// missionDestroyRing.setProperty("description", "Destroying Ring");
	// missionDestroyRing.setProperty("isAccomplished", false);
	// missions.add(missionDestroyRing);
	// datastore.put(missionDestroyRing);
	//
	// String kindVisitRivendell = "Mission";
	// String nameVisitRivendell = "Visit Rivendell";
	// Key keyVisitRivendell = KeyFactory.createKey(kindVisitRivendell,
	// nameVisitRivendell);
	// Entity missionVisitRivendell = new Entity(keyVisitRivendell);
	// missionVisitRivendell.setProperty("description", "Visit Rivendell");
	// missionVisitRivendell.setProperty("isAccomplished", false);
	// missions.add(missionVisitRivendell);
	// datastore.put(missionVisitRivendell);
	//
	// return missions;
	//
	// }

	public void putMissionsToDatabaseWithObjectify() {
		Mission mission1 = new Mission();
		mission1.setDescription("Destroying Ring");
		mission1.setAccomplished(false);

		Mission mission2 = new Mission();
		mission2.setDescription("Visit Rivendell");
		mission2.setAccomplished(false);

		// save Entity synchronous

		ofy().save().entity(mission1).now();
		ofy().save().entity(mission2).now();
	}

	public void putPlayerToDatabaseWithObjectify() {

		List<Player> players = new ArrayList<Player>();

		Player player1 = new Player();
		player1.setName("Hans");
		player1.setCharclass(Charclass.MAGE);
		player1.setHealth(25);
		player1.setEmail("email1@test.de");
		player1.setScore(199);
		players.add(player1);

		Player player2 = new Player();
		player2.setName("Maria");
		player2.setCharclass(Charclass.MAGE);
		player2.setHealth(25);
		player2.setEmail("email2@test.de");
		player2.setScore(200);
		players.add(player2);

		Player player3 = new Player();
		player3.setName("Vivien");
		player3.setCharclass(Charclass.MAGE);
		player3.setHealth(25);
		player3.setEmail("email3@test.de");
		player3.setScore(251);
		players.add(player3);

		Player player4 = new Player();
		player4.setName("Johann");
		player4.setCharclass(Charclass.MAGE);
		player4.setHealth(25);
		player4.setEmail("email4@test.de");
		player4.setScore(270);
		players.add(player4);

		Player player5 = new Player();
		player5.setName("Fiona");
		player5.setCharclass(Charclass.DWARF);
		player5.setHealth(25);
		player5.setEmail("email5@test.de");
		player5.setScore(245);
		players.add(player5);

		Player player6 = new Player();
		player6.setName("Alex");
		player6.setCharclass(Charclass.HOBBIT);
		player6.setHealth(25);
		player6.setEmail("email6@test.de");
		player6.setScore(300);
		players.add(player6);

		Player player7 = new Player();
		player7.setName("Janina");
		player7.setCharclass(Charclass.HOBBIT);
		player7.setHealth(25);
		player7.setEmail("email7@test.de");
		player7.setScore(123);
		players.add(player7);

		Player player8 = new Player();
		player8.setName("Max");
		player8.setCharclass(Charclass.ELF);
		player8.setHealth(25);
		player8.setEmail("email8@test.de");
		player8.setScore(654);
		players.add(player8);

		Player player9 = new Player();
		player9.setName("Janine");
		player9.setCharclass(Charclass.HOBBIT);
		player9.setHealth(25);
		player9.setEmail("email9@test.de");
		player9.setScore(344);
		players.add(player9);

		Player player10 = new Player();
		player10.setName("Julian");
		player10.setCharclass(Charclass.ELF);
		player10.setHealth(25);
		player10.setEmail("email9@test.de");
		player10.setScore(734);
		players.add(player10);

		// save Entity synchronous
		for (Player player : players) {
			ofy().save().entity(player).now();
		}
	}

	// public void putHighscorePlayerToDatabase() {
	// String kindPlayer = "Player";
	//
	// String emailPlayer1 = "email1@test.de";
	// Key keyPlayer1 = KeyFactory.createKey(kindPlayer, emailPlayer1);
	// Entity player1 = new Entity(kindPlayer);
	// player1.setProperty("playername", "Hans");
	// player1.setProperty("character", "Mage");
	// player1.setProperty("health", 25);
	// player1.setProperty("email", emailPlayer1);
	// player1.setProperty("score", 199);
	// datastore.put(player1);
	//
	// String emailPlayer2 = "email2@test.de";
	// Key keyPlayer2 = KeyFactory.createKey(kindPlayer, emailPlayer2);
	// Entity player2 = new Entity(kindPlayer);
	// player2.setProperty("playername", "Maria");
	// player2.setProperty("character", "Mage");
	// player2.setProperty("health", 25);
	// player2.setProperty("email", emailPlayer2);
	// player2.setProperty("score", 200);
	// datastore.put(player2);
	//
	// String emailPlayer3 = "email3@test.de";
	// Key keyPlayer3 = KeyFactory.createKey(kindPlayer, emailPlayer3);
	// Entity player3 = new Entity(kindPlayer);
	// player3.setProperty("playername", "Vivien");
	// player3.setProperty("character", "Mage");
	// player3.setProperty("health", 25);
	// player3.setProperty("email", emailPlayer3);
	// player3.setProperty("score", 251);
	// datastore.put(player3);
	//
	// String emailPlayer4 = "email4@test.de";
	// Key keyPlayer4 = KeyFactory.createKey(kindPlayer, emailPlayer4);
	// Entity player4 = new Entity(kindPlayer);
	// player4.setProperty("playername", "Johann");
	// player4.setProperty("character", "Mage");
	// player4.setProperty("health", 25);
	// player4.setProperty("email", emailPlayer4);
	// player4.setProperty("score", 270);
	// datastore.put(player4);
	//
	// String emailPlayer5 = "email5@test.de";
	// Key keyPlayer5 = KeyFactory.createKey(kindPlayer, emailPlayer5);
	// Entity player5 = new Entity(kindPlayer);
	// player5.setProperty("playername", "Fiona");
	// player5.setProperty("character", "Warrior");
	// player5.setProperty("health", 25);
	// player5.setProperty("email", emailPlayer5);
	// player5.setProperty("score", 245);
	// datastore.put(player5);
	//
	// String emailPlayer6 = "email6@test.de";
	// Key keyPlayer6 = KeyFactory.createKey(kindPlayer, emailPlayer6);
	// Entity player6 = new Entity(kindPlayer);
	// player6.setProperty("playername", "Alex");
	// player6.setProperty("character", "Hobbit");
	// player6.setProperty("health", 25);
	// player6.setProperty("email", emailPlayer6);
	// player6.setProperty("score", 300);
	// datastore.put(player6);
	//
	// String emailPlayer7 = "email7@test.de";
	// Key keyPlayer7 = KeyFactory.createKey(kindPlayer, emailPlayer7);
	// Entity player7 = new Entity(kindPlayer);
	// player7.setProperty("playername", "Janina");
	// player7.setProperty("character", "Hobbit");
	// player7.setProperty("health", 25);
	// player7.setProperty("email", emailPlayer7);
	// player7.setProperty("score", 123);
	// datastore.put(player7);
	//
	// String emailPlayer8 = "email8@test.de";
	// Key keyPlayer8 = KeyFactory.createKey(kindPlayer, emailPlayer8);
	// Entity player8 = new Entity(kindPlayer);
	// player8.setProperty("playername", "Max");
	// player8.setProperty("character", "Mage");
	// player8.setProperty("health", 25);
	// player8.setProperty("email", emailPlayer8);
	// player8.setProperty("score", 654);
	// datastore.put(player8);
	//
	// String emailPlayer9 = "email9@test.de";
	// Key keyPlayer9 = KeyFactory.createKey(kindPlayer, emailPlayer9);
	// Entity player9 = new Entity(kindPlayer);
	// player9.setProperty("playername", "Janina");
	// player9.setProperty("character", "Hobbit");
	// player9.setProperty("health", 25);
	// player9.setProperty("email", emailPlayer9);
	// player9.setProperty("score", 344);
	// datastore.put(player9);
	//
	// String emailPlayer10 = "email10@test.de";
	// Key keyPlayer10 = KeyFactory.createKey(kindPlayer, emailPlayer10);
	// Entity player10 = new Entity(kindPlayer);
	// player10.setProperty("playername", "Julian");
	// player10.setProperty("character", "Warior");
	// player10.setProperty("health", 25);
	// player10.setProperty("email", emailPlayer10);
	// player10.setProperty("score", 734);
	// datastore.put(player10);
	//
	// }

	public List<Mission> getAllMissions() {
		// public List<Entity> getAllMissions() {
		// Query q = new Query("Mission");
		// PreparedQuery pq = datastore.prepare(q);
		// List<Entity> result = pq.asList(FetchOptions.Builder.withLimit(5));
		List<Mission> result = ofy().load().type(Mission.class).list();
		return result;
	}
}
