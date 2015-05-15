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
	private Cache cache;
	private static String ADDITIONAL_PLAYERS = "additional";
	private static String DELETED_PLAYERS = "deleted";

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

		initialize();
		List<Entity> highscorePlayer = computeHighscore();

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
			handleNewHighscore(req);
		}else if(userWantsToEditCharacter(req)){
			req.setAttribute("edit", req.getParameter("edit"));
			forwardedFile = "/geekquest";
		} else {
			List<Entity> highscorePlayer = computeHighscore();
			setViewData(req, highscorePlayer);
		}
		dispatchRequest(req, resp, forwardedFile);
	}

	public void initialize() {
		datastore = DatastoreServiceFactory.getDatastoreService();
		userService = UserServiceFactory.getUserService();
		user = userService.getCurrentUser();

		try {
			Map properties = new HashMap<>();
			CacheFactory cacheFactory = CacheManager.getInstance()
					.getCacheFactory();
			cache = cacheFactory.createCache(properties);
		} catch (CacheException e) {
			// ...
		}
	}
	
	public boolean userWantsToEditCharacter(HttpServletRequest req){
		return req.getParameter("edit")!=null;
	}

	public void handleNewHighscore(HttpServletRequest req) {
		Entity player = setNewHighscoreForPlayer(req);
		List<Entity> highscorePlayer = getHighscorePlayer();
		if (hasChangesInHighscore(highscorePlayer, player)) {
			saveEntity(player);
			if(isAlreadyInHighscore(highscorePlayer, player)){
				// Player ist bereits im Highscore; seine Platzierung wird
				// besser
				if(containsAdditionalMemcachePlayer(player)){
					removePlayerFromAdditionalMemcache(player);
				}
				if(containsRemovedMemcachePlayer(player)){
					removePlayerFromDeleteMemcache(player);
				}
				addPlayerToAdditionalMemcache(player);
				addPlayerToDeleteMemcache(player);
			}else{
				// Player war noch nicht im Highscore, erreicht jedoch jetzt
				// eine Platzierung
				if(containsAdditionalMemcachePlayer(player)){
					removePlayerFromAdditionalMemcache(player);
				}
				addPlayerToAdditionalMemcache(player);
			}
		}
		List<Entity> resultHighscore = computeHighscore();
		setViewData(req, resultHighscore);
	}
	
	public boolean containsAdditionalMemcachePlayer(Entity player){
		ArrayList<Entity> additionalHighscorePlayers = (ArrayList<Entity>) cache
				.get(ADDITIONAL_PLAYERS);
		if(additionalHighscorePlayers==null || additionalHighscorePlayers.size()==0){
			return false;
		}
		for(Entity memcachePlayer: additionalHighscorePlayers){
			if(memcachePlayer.getKey().equals(player.getKey())){
				return true;
			}
		}
		return false;
	}
	
	public boolean containsRemovedMemcachePlayer(Entity player){
		ArrayList<Entity> removedHighscorePlayers = (ArrayList<Entity>) cache
				.get(DELETED_PLAYERS);
		if(removedHighscorePlayers==null || removedHighscorePlayers.size()==0){
			return false;
		}
		for(Entity memcachePlayer: removedHighscorePlayers){
			if(memcachePlayer.getKey().equals(player.getKey())){
				return true;
			}
		}
		return false;
	}

	public List<Entity> computeHighscore() {
		List<Entity> highscorePlayer = getHighscorePlayer();

		ArrayList<Entity> additionalHighscorePlayers = (ArrayList<Entity>) cache
				.get(ADDITIONAL_PLAYERS);
		ArrayList<Entity> removedHighscorePlayers = (ArrayList<Entity>) cache
				.get(DELETED_PLAYERS);

		List<Entity> removeFromRemoved = new ArrayList<Entity>();
		if (removedHighscorePlayers != null && removedHighscorePlayers.size()!=0) {
			for (Entity player : removedHighscorePlayers) {
				if (highscoreContainsIdenticalPlayer(highscorePlayer, player)) {
					highscorePlayer.remove(player);
				} else {
					removeFromRemoved.add(player);
				}
			}
			for(Entity entity : removeFromRemoved){
				removedHighscorePlayers.remove(entity);
			}
			cache.put(DELETED_PLAYERS, removedHighscorePlayers);
		}

		List<Entity> remove = new ArrayList<Entity>();
		if (additionalHighscorePlayers != null && additionalHighscorePlayers.size()!=0) {
			for (Entity player : additionalHighscorePlayers) {
				if (!highscoreContainsIdenticalPlayer(highscorePlayer, player)) {
					highscorePlayer.add(player);
				} else {
					remove.add(player);
				}
			}
			for(Entity entity: remove){
				additionalHighscorePlayers.remove(entity);
			}
			cache.put(ADDITIONAL_PLAYERS, additionalHighscorePlayers);
		}

		Comparator<Entity> playerComparator = new Comparator<Entity>() {

			@Override
			public int compare(Entity o1, Entity o2) {
				Long score1 = (Long) o1.getProperty("score");
				Long score2 = (Long) o2.getProperty("score");
				return score2.compareTo(score1);
			}

		};
		Collections.sort(highscorePlayer, playerComparator);
		List<Entity> result;
		int index;
		if(highscorePlayer.size()<10){
			if(highscorePlayer.size()==0){
				index=0;
			}
			else{
				index= highscorePlayer.size()-1;
			}
			result = highscorePlayer.subList(0,  index);
		}else{
			result= highscorePlayer.subList(0, 10);
		}
		return result;
	}

	public void addPlayerToAdditionalMemcache(Entity player) {
		ArrayList<Entity> additionalHighscorePlayers = (ArrayList<Entity>) cache
				.get(ADDITIONAL_PLAYERS);
		if (additionalHighscorePlayers == null) {
			additionalHighscorePlayers = new ArrayList<>();
		}
		additionalHighscorePlayers.add(player);
		cache.put(ADDITIONAL_PLAYERS, additionalHighscorePlayers);
	}
	
	public void removePlayerFromAdditionalMemcache(Entity player) {
		ArrayList<Entity> additionalHighscorePlayers = (ArrayList<Entity>) cache
				.get(ADDITIONAL_PLAYERS);
		if (additionalHighscorePlayers == null) {
			return;
		}
		additionalHighscorePlayers.remove(player);
		cache.put(ADDITIONAL_PLAYERS, additionalHighscorePlayers);
	}

	public void addPlayerToDeleteMemcache(Entity player) {
		ArrayList<Entity> removedHighscorePlayers = (ArrayList<Entity>) cache
				.get(DELETED_PLAYERS);
		if (removedHighscorePlayers == null) {
			removedHighscorePlayers = new ArrayList<>();
		}
		removedHighscorePlayers.add(player);
		cache.put(DELETED_PLAYERS, removedHighscorePlayers);
	}
	public void removePlayerFromDeleteMemcache(Entity player) {
		ArrayList<Entity> removedHighscorePlayers = (ArrayList<Entity>) cache
				.get(DELETED_PLAYERS);
		if (removedHighscorePlayers == null) {
			return;
		}
		removedHighscorePlayers.remove(player);
		cache.put(DELETED_PLAYERS, removedHighscorePlayers);
	}

	public boolean highscoreContainsIdenticalPlayer(List<Entity> highscore,
			Entity player) {
		for (Entity highscorePlayer : highscore) {
			if (isIdenticalPlayer(highscorePlayer, player)) {
				return true;
			}
		}
		return false;
	}

	public void changeHighscoreAddPlayer(List<Entity> highscore, Entity player) {
		highscore.add(player);
		Comparator<Entity> playerComparator = new Comparator<Entity>() {

			@Override
			public int compare(Entity o1, Entity o2) {
				Integer scoreO1 = Integer
						.valueOf((int) o1.getProperty("score"));
				Integer scoreO2 = Integer
						.valueOf((int) o2.getProperty("score"));
				return scoreO1.compareTo(scoreO2);
			}

		};
		Collections.sort(highscore, playerComparator);
		highscore.remove(10);
	}

	public Entity setNewHighscoreForPlayer(HttpServletRequest req) {
		Entity player = getPlayerFromDatabase();
		if (req.getParameter("newscore") != null) {
			player.setProperty("score", Long.valueOf(req.getParameter("newscore")));
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

	public boolean hasChangesInHighscore(List<Entity> highscore, Entity player) {

		// testcase 1: Player ist bereits im Highscore
		if (isAlreadyInHighscore(highscore, player)) {
			if(hasHigherHighscoreThanBefore(highscore,player)){
				return true;
			}
			return false;
		}
		// testcase 2: Player war noch nicht im Highscore und erreicht jetzt
		// eine Platzierung im Highscore
		if (playerGetsInHighscore(highscore, player)) {
			return true;
		}

		return false;
	}
	
	public boolean hasHigherHighscoreThanBefore(List<Entity> highscore, Entity player){
		for (Entity highscorePlayer : highscore) {
			if (isIdenticalPlayer(highscorePlayer, player)) {
				if(hasHigherScore(highscorePlayer, player)){
					return true;
				}
			}
		}

		return false;
	}

	public boolean playerGetsInHighscore(List<Entity> highscore, Entity player) {

		for (Entity highscorePlayer : highscore) {
			if (hasHigherScore(highscorePlayer, player)) {
				return true;
			}
		}

		return false;

	}

	public boolean isAlreadyInHighscore(List<Entity> highscore, Entity player) {

		for (Entity highscorePlayer : highscore) {
			if (isIdenticalPlayer(highscorePlayer, player)) {
				return true;
			}
		}

		return false;

	}

	public boolean hasHigherScore(Entity playerHighscore,
			Entity playerChallenger) {

		if (playerChallenger.getProperty("score") != null
				&& ((Long)playerChallenger.getProperty("score")).compareTo((Long)playerHighscore
						.getProperty("score")) >0) {
			return true;
		}
		return false;
	}

	public boolean isIdenticalPlayer(Entity playerOne, Entity playerTwo) {
		if (playerOne.getKey().equals(playerTwo.getKey())) {
			return true;
		}

		return false;
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

	public List<Entity> getHighscorePlayer() {
		Query q = getHighscorePlayerQuery();
		PreparedQuery pq = datastore.prepare(q);
		List<Entity> result = pq.asList(FetchOptions.Builder.withLimit(10)
				.offset(0));
		return result;
	}

	public static Query getHighscorePlayerQuery() {
		Query q = new Query("Player");
		q.addSort("score", SortDirection.DESCENDING);
		return q;
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
