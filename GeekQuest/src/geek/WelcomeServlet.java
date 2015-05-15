package geek;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Entity;
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

@SuppressWarnings("serial")
public class WelcomeServlet extends HttpServlet {

	private DatastoreService datastore;
	private UserService userService;
	private User user;
	private final String INITIAL_HEALTH = "10";
	private BlobstoreService blobstoreService = BlobstoreServiceFactory
			.getBlobstoreService();
	private HighscoreCalculator calculator;

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

		initialize();
		List<Entity> highscorePlayer = calculator.computeHighscore();

		if (hasToSavePlayer(req)) {
			Entity player = getPlayer(req, resp);
			saveEntity(player);
			if (hasPlayerAllProperties(player)) {
				setRequestParameter(req, player);
				setHighscore(req, highscorePlayer);
			} else {
				resp.sendRedirect("/geekquest");
			}
		} else {
			Entity player = getPlayerFromDatabase();
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
		String forwardedFile = "/pages/welcome.jsp";
		if (userWantsToLogout(req)) {
			resp.sendRedirect(createLogoutURL());
		} else if (userSetsNewScore(req)) {
			Entity player = setNewHighscoreForPlayer(req);
			List<Entity> resultHighscore = calculator
					.handleNewHighscore(player);
			setViewData(req, resultHighscore);
		} else if (userWantsToEditCharacter(req)) {
			req.setAttribute("edit", req.getParameter("edit"));
			forwardedFile = "/geekquest";
		} else {
			List<Entity> highscorePlayer = calculator.computeHighscore();
			setViewData(req, highscorePlayer);
		}
		dispatchRequest(req, resp, forwardedFile);
	}

	public void initialize() {
		datastore = DatastoreServiceFactory.getDatastoreService();
		userService = UserServiceFactory.getUserService();
		user = userService.getCurrentUser();
		calculator = new HighscoreCalculator();
	}

	public boolean userWantsToEditCharacter(HttpServletRequest req) {
		return req.getParameter("edit") != null;
	}

	public Entity setNewHighscoreForPlayer(HttpServletRequest req) {
		Entity player = getPlayerFromDatabase();
		if (req.getParameter("newscore") != null) {
			player.setProperty("score",
					Long.valueOf(req.getParameter("newscore")));
		}
		return player;
	}

	public void setViewData(HttpServletRequest req, List<Entity> highscorePlayer) {
		Entity player = getPlayerFromDatabase();
		if (hasPlayerAllProperties(player)) {
			setRequestParameter(req, player);
			setHighscore(req, highscorePlayer);
		}
	}

	public void setHighscore(HttpServletRequest req,
			List<Entity> highscorePlayer) {
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

	public boolean userSetsNewScore(HttpServletRequest req) {
		return req.getParameter("newscore") != null;
	}

	public void setRequestParameter(HttpServletRequest req, Entity player) {

		req.setAttribute("playername", player.getProperty("playername"));
		req.setAttribute("character", player.getProperty("character"));
		req.setAttribute("health", player.getProperty("health"));
		req.setAttribute("missions", getPlayerMissionsFromEmbedded(player));
		// req.setAttribute("fileurl",
		// getUpoadedFileURL((String)player.getProperty("fileblobkey")));
		req.setAttribute("fileurl", player.getProperty("fileurl"));
		req.setAttribute("user", user.getEmail());
	}

	public boolean hasPlayerAllProperties(Entity player) {
		return player.getProperty("playername") != null
				&& !player.getProperty("playername").equals("")
				&& player.getProperty("character") != null
				&& player.getProperty("health") != null
				&& !player.getProperty("health").equals("")
				&& player.getProperty("email") != null
				&& !player.getProperty("email").equals("");
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

	public Entity getPlayer(HttpServletRequest req, HttpServletResponse resp) {
		Key key = getKey("Player", user.getEmail());

		Entity player = new Entity(key);
		player.setProperty("playername", req.getParameter("playername"));
		player.setProperty("character", req.getParameter("character"));
		player.setProperty("health", getHealth());
		player.setProperty("email", user.getEmail());
		// player.setProperty("fileblobkey", getBlobKey(req, resp));
		player.setProperty("fileurl", getUpoadedFileURL(req, resp));
		// setzen der missions
		ArrayList<Entity> selectedMissions = getSelectedMissions(req);
		ArrayList<EmbeddedEntity> selectedEmbeddedMissions = new ArrayList<EmbeddedEntity>();
		for (Entity mission : selectedMissions) {
			EmbeddedEntity embeddedMission = new EmbeddedEntity();
			Key infoKey;

			infoKey = mission.getKey();
			embeddedMission.setKey(infoKey);
			embeddedMission.setPropertiesFrom(mission);

			selectedEmbeddedMissions.add(embeddedMission);
		}

		player.setProperty("missions", selectedEmbeddedMissions);
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

	public ArrayList<Entity> getSelectedMissions(HttpServletRequest req) {
		ArrayList<Entity> selectedMissions = new ArrayList<Entity>();
		List<Entity> allMissions = getAllMissions();
		for (Entity mission : allMissions) {
			String selected = req.getParameter("checkbox"
					+ mission.getProperty("description"));
			if (selected != null && selected.equals("on")) {
				selectedMissions.add(mission);
			}
		}
		return selectedMissions;
	}

	public List<Entity> getAllMissions() {
		Query q = new Query("Mission");
		PreparedQuery pq = datastore.prepare(q);
		List<Entity> result = pq.asList(FetchOptions.Builder.withLimit(5));
		return result;
	}

	public String getHealth() {
		if (hasCharacter()) {
			Entity player = getPlayerFromDatabase();
			return (String) player.getProperty("health");
		} else {
			return INITIAL_HEALTH;
		}
	}

	public boolean hasCharacter() {
		Entity entity = getPlayerFromDatabase();
		return entity != null;
	}

	public Entity getPlayerFromDatabase() {
		Query q = new Query("Player");
		Filter userFilter = new FilterPredicate("email", FilterOperator.EQUAL,
				user.getEmail());
		q.setFilter(userFilter);
		PreparedQuery pq = datastore.prepare(q);
		Entity entity = pq.asSingleEntity();
		return entity;
	}

	// falls User seinen Account loeschen will
	public void deleteUser() {
		Key key = getKey("Player", user.getEmail());
		datastore.delete(key);
	}

	public Key getKey(String kind, String name) {
		return KeyFactory.createKey(kind, name);
	}

	public void saveEntity(Entity entity) {
		datastore.put(entity);
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
