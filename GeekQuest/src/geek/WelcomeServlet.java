package geek;

import static com.googlecode.objectify.ObjectifyService.ofy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheFactory;
import javax.cache.CacheManager;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import mapReduce.EagerSchemaMapperController;
import mapReduce.EagerSchemaMapperDenormalizeController;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.memcache.jsr107cache.GCacheFactory;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.api.datastore.Transaction;
import com.googlecode.objectify.Ref;

import entities.Charclass;
import entities.Mission;
import entities.Player;

@SuppressWarnings("serial")
public class WelcomeServlet extends HttpServlet {

	// private DatastoreService datastore;
	private UserService userService;
	private User user;
	private final Long INITIAL_HEALTH = 10l;
	// private final String INITIAL_HEALTH = "10";
	private BlobstoreService blobstoreService = BlobstoreServiceFactory
			.getBlobstoreService();
	private HighscoreCalculator calculator;

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

		initialize();
		// List<Entity> highscorePlayer = calculator.computeHighscore();
		List<Player> highscorePlayer = calculator.computeHighscore();
		String value = req.getParameter("link");
		if (value != null
				&& value.equals("Need help finding a good hobbit name?")) {
			resp.sendRedirect("/namehelper");
		}
		if (hasToSavePlayer(req)) {
			// int retries = 3;
			// while (true) {
			// Transaction txn = datastore.beginTransaction();
			// try {
			// Entity player = getPlayer(req, resp);
			Player player = getPlayer(req, resp);
			saveEntity(player);
			// txn.commit();
			if (hasPlayerAllProperties(player)) {
				setRequestParameter(req, player);
				setHighscore(req, highscorePlayer);
			} else {
				resp.sendRedirect("/geekquest");
			}
			// break;
			// } catch (ConcurrentModificationException e) {
			// if (retries == 0) {
			// throw e;
			// }
			// // Allow retry to occur
			// --retries;
			// } finally {
			// if (txn.isActive()) {
			// txn.rollback();
			// }
			// }
			// }
		} else {
			// Entity player = getPlayerFromDatabase();
			Player player = getPlayerFromDatabase();
			if (hasPlayerAllProperties(player)) {
				setRequestParameter(req, player);
				setHighscore(req, highscorePlayer);
			}
		}
		dispatchRequest(req, resp, "/pages/welcome.jsp");
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

		initialize();
		if (shouldStartMapReduceJob(req)) {
			EagerSchemaMapperDenormalizeController.startMapJob();
		}
		if (shouldStartMapJob(req)) {
			EagerSchemaMapperController.startMapJob();
		}
		String forwardedFile = "/pages/welcome.jsp";
		if (userWantsToLogout(req)) {
			resp.sendRedirect(createLogoutURL());
		} else if (userSetsNewScore(req)) {
			// int retries = 3;
			// while (true) {
			// Transaction txn = datastore.beginTransaction();
			// try {
			// Entity player = setNewHighscoreForPlayer(req);
			Player player = setNewHighscoreForPlayer(req);
			// List<Entity> resultHighscore = calculator
			// .handleNewHighscore(player);
			List<Player> resultHighscore = calculator
					.handleNewHighscore(player);
			// txn.commit();
			setViewData(req, resultHighscore);
			// break;
			// } catch (ConcurrentModificationException e) {
			// if (retries == 0) {
			// throw e;
			// }
			// // Allow retry to occur
			// --retries;
			// } finally {
			// if (txn.isActive()) {
			// txn.rollback();
			// }
			// }
			// }
		} else if (userWantsToEditCharacter(req)) {
			req.setAttribute("edit", req.getParameter("edit"));
			forwardedFile = "/geekquest";
		} else {
			// List<Entity> highscorePlayer = calculator.computeHighscore();
			List<Player> highscorePlayer = calculator.computeHighscore();
			setViewData(req, highscorePlayer);
		}
		dispatchRequest(req, resp, forwardedFile);
	}

	public void initialize() {
		// datastore = DatastoreServiceFactory.getDatastoreService();
		userService = UserServiceFactory.getUserService();
		user = userService.getCurrentUser();
		calculator = new HighscoreCalculator();
	}

	public boolean userWantsToEditCharacter(HttpServletRequest req) {
		return req.getParameter("edit") != null;
	}

	public Player setNewHighscoreForPlayer(HttpServletRequest req) {
		// public Entity setNewHighscoreForPlayer(HttpServletRequest req) {
		// Entity player = getPlayerFromDatabase();
		// if (req.getParameter("newscore") != null) {
		// player.setProperty("score",
		// Long.valueOf(req.getParameter("newscore")));
		// }
		Player player = getPlayerFromDatabase();

		if (req.getParameter("newscore") != null) {

			if (player.getScore() < Long.valueOf(req.getParameter("newscore"))) {
				player.setScore(Long.valueOf(req.getParameter("newscore")));
				saveEntity(player);
			}
		}

		return player;
	}

	public boolean hasHigherScore(Player playerHighscore,
			Player playerChallenger) {

		if ((new Long(playerChallenger.getScore())).compareTo(new Long(
				playerHighscore.getScore())) > 0) {
			return true;
		}
		return false;
	}

	public void setViewData(HttpServletRequest req, List<Player> highscorePlayer) {
		// public void setViewData(HttpServletRequest req, List<Entity>
		// highscorePlayer) {
		// Entity player = getPlayerFromDatabase();
		// if (hasPlayerAllProperties(player)) {
		// setRequestParameter(req, player);
		// setHighscore(req, highscorePlayer);
		// }
		Player player = getPlayerFromDatabase();
		if (hasPlayerAllProperties(player)) {
			setRequestParameter(req, player);
			setHighscore(req, highscorePlayer);
		}
	}

	// public void setHighscore(HttpServletRequest req,
	// List<Entity> highscorePlayer) {
	public void setHighscore(HttpServletRequest req,
			List<Player> highscorePlayer) {
		req.setAttribute("highscores", highscorePlayer);
	}

	public String getBlobKey(HttpServletRequest req, HttpServletResponse resp) {
		Map<String, List<BlobKey>> blobs = blobstoreService.getUploads(req);
		List<BlobKey> blobKeys = blobs.get("file");
		if (blobKeys == null || blobKeys.isEmpty()) {
			return null;
		} else {
			return blobKeys.get(0).getKeyString();
		}
	}

	public String createLogoutURL() {
		return userService.createLogoutURL(userService
				.createLoginURL("/geekquest"));
	}

	public boolean userWantsToLogout(HttpServletRequest req) {
		return req.getParameter("logout") != null;
	}

	public boolean shouldStartMapJob(HttpServletRequest req) {
		return req.getParameter("map") != null;
	}

	public boolean shouldStartMapReduceJob(HttpServletRequest req) {
		return req.getParameter("mapreduce") != null;
	}

	public boolean userSetsNewScore(HttpServletRequest req) {
		return req.getParameter("newscore") != null;
	}

	public void setRequestParameter(HttpServletRequest req, Player player) {
		// public void setRequestParameter(HttpServletRequest req, Entity
		// player) {

		// req.setAttribute("playername", player.getProperty("playername"));
		// req.setAttribute("character", player.getProperty("character"));
		// req.setAttribute("health", player.getProperty("health"));
		// req.setAttribute("missions", getPlayerMissionsFromEmbedded(player));
		// req.setAttribute("fileurl", player.getProperty("fileurl"));
		// req.setAttribute("user", user.getEmail());

		req.setAttribute("playername", player.getName());
		req.setAttribute("character", player.getCharclass().toString());
		req.setAttribute("health", player.getHealth());
		req.setAttribute("missions", getPlayerMissions(player));
		req.setAttribute("fileurl", player.getPhoto());
		req.setAttribute("user", user.getEmail());
	}

	public List<Mission> getPlayerMissions(Player player) {
		List<Ref<Mission>> refs = player.getMissions();
		Collection<Mission> missions = ofy().load().refs(refs).values();
		return new ArrayList<Mission>(missions);
	}

	public boolean hasPlayerAllProperties(Player player) {
		// public boolean hasPlayerAllProperties(Entity player) {
		// return player.getProperty("playername") != null
		// && !player.getProperty("playername").equals("")
		// && player.getProperty("character") != null
		// && player.getProperty("health") != null
		// && !player.getProperty("health").equals("")
		// && player.getProperty("email") != null
		// && !player.getProperty("email").equals("");
		return player.getName() != null && player.getCharclass() != null
				&& player.getEmail() != null;
	}

	// public boolean hasFileUpoaded(HttpServletRequest req, HttpServletResponse
	// resp){
	// return getBlobKey(req, resp)!=null;
	// }
	public boolean hasFileUpoaded(Entity player) {
		return player.getProperty("fileblobkey") != null;
	}

	public ArrayList<Entity> getPlayerMissionsFromEmbedded(Entity player) {
		ArrayList<Entity> missions = new ArrayList<Entity>();
		ArrayList<EmbeddedEntity> embeddedMissions = (ArrayList<EmbeddedEntity>) player
				.getProperty("missions");
		if (embeddedMissions != null) {
			for (EmbeddedEntity embeddedMission : embeddedMissions) {
				Key infoKey = embeddedMission.getKey();
				Entity mission = new Entity(infoKey);

				mission.setPropertiesFrom(embeddedMission);
				missions.add(mission);
			}
		}
		return missions;
	}

	public boolean hasToSavePlayer(HttpServletRequest req) {
		return req.getParameter("playername") != null
				&& !req.getParameter("playername").equals("");
	}

	public Player getPlayer(HttpServletRequest req, HttpServletResponse resp) {
		// public Entity getPlayer(HttpServletRequest req, HttpServletResponse
		// resp) {
		// Key key = getKey("Player", user.getEmail());
		//
		// Entity player = new Entity(key);
		// player.setProperty("playername", req.getParameter("playername"));
		// player.setProperty("character", req.getParameter("character"));
		// player.setProperty("health", getHealth());
		// player.setProperty("email", user.getEmail());
		// Long score = getScore();
		// if (score != null) {
		// player.setProperty("score", score);
		// }
		// // player.setProperty("fileblobkey", getBlobKey(req, resp));
		// player.setProperty("fileurl", getUpoadedFileURL(req, resp));
		// // setzen der missions
		// ArrayList<Entity> selectedMissions = getSelectedMissions(req);
		// ArrayList<EmbeddedEntity> selectedEmbeddedMissions = new
		// ArrayList<EmbeddedEntity>();
		// for (Entity mission : selectedMissions) {
		// EmbeddedEntity embeddedMission = new EmbeddedEntity();
		// Key infoKey;
		//
		// infoKey = mission.getKey();
		// embeddedMission.setKey(infoKey);
		// embeddedMission.setPropertiesFrom(mission);
		//
		// selectedEmbeddedMissions.add(embeddedMission);
		// }
		//
		// player.setProperty("missions", selectedEmbeddedMissions);
		// return player;

		Player player = new Player();
		player.setName(req.getParameter("playername"));
		Charclass charclass = null;
		for (Charclass chclass : Charclass.values()) {
			if (req.getParameter("character").equals(chclass.toString())) {
				charclass = chclass;
			}
		}
		player.setCharclass(charclass);
		player.setHealth(getHealth());
		player.setEmail(user.getEmail());
		Long score = getScore();
		if (score != null) {
			player.setScore(score);
		}
		player.setPhoto(getUpoadedFileURL(req, resp));
		// setzen der missions
		ArrayList<Ref<Mission>> selectedMissions = getSelectedMissions(req);
		player.setMissions(selectedMissions);
		return player;
	}

	public String getUpoadedFileURL(HttpServletRequest req,
			HttpServletResponse resp) {
		String key = getBlobKey(req, resp);
		if (key != null) {
			String url = "http://localhost:8888/serve?blob-key=" + key;
			return url;
		}
		return "";
	}

	public ArrayList<Ref<Mission>> getSelectedMissions(HttpServletRequest req) {
		// public ArrayList<Entity> getSelectedMissions(HttpServletRequest req)
		// {
		// ArrayList<Entity> selectedMissions = new ArrayList<Entity>();
		// List<Entity> allMissions = getAllMissions();
		// for (Entity mission : allMissions) {
		// String selected = req.getParameter("checkbox"
		// + mission.getProperty("description"));
		// if (selected != null && selected.equals("on")) {
		// selectedMissions.add(mission);
		// }
		// }
		// return selectedMissions;

		List<Mission> allMissions = getAllMissions();
		ArrayList<Ref<Mission>> selectedMissions = new ArrayList<Ref<Mission>>();
		for (Mission mission : allMissions) {
			String selected = req.getParameter("checkbox"
					+ mission.getDescription());
			if (selected != null && selected.equals("on")) {
				selectedMissions.add(Ref.create(mission));
			}
		}

		return selectedMissions;

	}

	public List<Mission> getAllMissions() {
		// public List<Entity> getAllMissions() {
		// Query q = new Query("Mission");
		// PreparedQuery pq = datastore.prepare(q);
		// List<Entity> result = pq.asList(FetchOptions.Builder.withLimit(5));
		List<Mission> result = ofy().load().type(Mission.class).list();
		return result;
	}

	public Long getHealth() {
		// public String getHealth() {
		if (hasCharacter()) {
			Player player = getPlayerFromDatabase();
			// Entity player = getPlayerFromDatabase();
			// return (String) player.getProperty("health");
			return player.getHealth();
		} else {
			return INITIAL_HEALTH;
		}
	}

	public Long getScore() {
		if (hasCharacter()) {
			// Entity player = getPlayerFromDatabase();
			Player player = getPlayerFromDatabase();
			// return (Long) player.getProperty("score");
			return player.getScore();
		} else {
			return null;
		}
	}

	public boolean hasCharacter() {
		// Entity entity = getPlayerFromDatabase();
		Player entity = getPlayerFromDatabase();
		return entity != null;
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

		Player player = ofy().load().type(Player.class).id(user.getEmail())
				.now();
		return player;
	}

	// falls User seinen Account loeschen will
	public void deleteUser() {
		// Key key = getKey("Player", user.getEmail());
		// datastore.delete(key);
		Player player = getPlayerFromDatabase();
		ofy().delete().entity(player).now();
	}

	public Key getKey(String kind, String name) {
		return KeyFactory.createKey(kind, name);
	}

	public void saveEntity(Player entity) {
		// public void saveEntity(Entity entity) {
		// datastore.put(entity);
		ofy().save().entity(entity).now();
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

}
