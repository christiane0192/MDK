package geek;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheFactory;
import javax.cache.CacheManager;
import javax.servlet.http.HttpServletRequest;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.users.UserServiceFactory;

public class HighscoreCalculator {

	private DatastoreService datastore;
	private Cache cache;
	private static String ADDITIONAL_PLAYERS = "additional";
	private static String DELETED_PLAYERS = "deleted";

	public void initialize() {
		datastore = DatastoreServiceFactory.getDatastoreService();

		try {
			Map properties = new HashMap<>();
			CacheFactory cacheFactory = CacheManager.getInstance()
					.getCacheFactory();
			cache = cacheFactory.createCache(properties);
		} catch (CacheException e) {
			// ...
		}
	}

	public List<Entity> computeHighscore() {
		initialize();
		List<Entity> highscorePlayer = getHighscorePlayer();

		ArrayList<Entity> additionalHighscorePlayers = (ArrayList<Entity>) cache
				.get(ADDITIONAL_PLAYERS);
		ArrayList<Entity> removedHighscorePlayers = (ArrayList<Entity>) cache
				.get(DELETED_PLAYERS);

		List<Entity> removeFromRemoved = new ArrayList<Entity>();
		if (removedHighscorePlayers != null
				&& removedHighscorePlayers.size() != 0) {
			for (Entity player : removedHighscorePlayers) {
				if (highscoreContainsIdenticalPlayer(highscorePlayer, player)) {
					highscorePlayer.remove(player);
				} else {
					removeFromRemoved.add(player);
				}
			}
			for (Entity entity : removeFromRemoved) {
				removedHighscorePlayers.remove(entity);
			}
			cache.put(DELETED_PLAYERS, removedHighscorePlayers);
		}

		List<Entity> remove = new ArrayList<Entity>();
		if (additionalHighscorePlayers != null
				&& additionalHighscorePlayers.size() != 0) {
			for (Entity player : additionalHighscorePlayers) {
				if (!highscoreContainsIdenticalPlayer(highscorePlayer, player)) {
					highscorePlayer.add(player);
				} else {
					remove.add(player);
				}
			}
			for (Entity entity : remove) {
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
		if (highscorePlayer.size() < 10) {
			if (highscorePlayer.size() == 0) {
				index = 0;
			} else {
				index = highscorePlayer.size() - 1;
			}
			result = highscorePlayer.subList(0, index);
		} else {
			result = highscorePlayer.subList(0, 10);
		}
		return result;
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

	public boolean isIdenticalPlayer(Entity playerOne, Entity playerTwo) {
		if (playerOne.getKey().equals(playerTwo.getKey())) {
			return true;
		}

		return false;
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

	public boolean isAlreadyInHighscore(List<Entity> highscore, Entity player) {

		for (Entity highscorePlayer : highscore) {
			if (isIdenticalPlayer(highscorePlayer, player)) {
				return true;
			}
		}

		return false;

	}

	public boolean hasHigherHighscoreThanBefore(List<Entity> highscore,
			Entity player) {
		for (Entity highscorePlayer : highscore) {
			if (isIdenticalPlayer(highscorePlayer, player)) {
				if (hasHigherScore(highscorePlayer, player)) {
					return true;
				}
			}
		}

		return false;
	}

	public boolean hasHigherScore(Entity playerHighscore,
			Entity playerChallenger) {

		if (playerChallenger.getProperty("score") != null
				&& ((Long) playerChallenger.getProperty("score"))
						.compareTo((Long) playerHighscore.getProperty("score")) > 0) {
			return true;
		}
		return false;
	}

	public boolean hasChangesInHighscore(List<Entity> highscore, Entity player) {

		// testcase 1: Player ist bereits im Highscore
		if (isAlreadyInHighscore(highscore, player)) {
			if (hasHigherHighscoreThanBefore(highscore, player)) {
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

	public boolean playerGetsInHighscore(List<Entity> highscore, Entity player) {

		for (Entity highscorePlayer : highscore) {
			if (hasHigherScore(highscorePlayer, player)) {
				return true;
			}
		}

		return false;

	}

	public void saveEntity(Entity entity) {
		datastore.put(entity);
	}

	public boolean containsAdditionalMemcachePlayer(Entity player) {
		ArrayList<Entity> additionalHighscorePlayers = (ArrayList<Entity>) cache
				.get(ADDITIONAL_PLAYERS);
		if (additionalHighscorePlayers == null
				|| additionalHighscorePlayers.size() == 0) {
			return false;
		}
		for (Entity memcachePlayer : additionalHighscorePlayers) {
			if (memcachePlayer.getKey().equals(player.getKey())) {
				return true;
			}
		}
		return false;
	}

	public boolean containsRemovedMemcachePlayer(Entity player) {
		ArrayList<Entity> removedHighscorePlayers = (ArrayList<Entity>) cache
				.get(DELETED_PLAYERS);
		if (removedHighscorePlayers == null
				|| removedHighscorePlayers.size() == 0) {
			return false;
		}
		for (Entity memcachePlayer : removedHighscorePlayers) {
			if (memcachePlayer.getKey().equals(player.getKey())) {
				return true;
			}
		}
		return false;
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

	public List<Entity> handleNewHighscore(Entity player) {
		initialize();
		List<Entity> highscorePlayer = getHighscorePlayer();
		if (hasChangesInHighscore(highscorePlayer, player)) {
			saveEntity(player);
			if (isAlreadyInHighscore(highscorePlayer, player)) {
				// Player ist bereits im Highscore; seine Platzierung wird
				// besser
				if (containsAdditionalMemcachePlayer(player)) {
					removePlayerFromAdditionalMemcache(player);
				}
				if (containsRemovedMemcachePlayer(player)) {
					removePlayerFromDeleteMemcache(player);
				}
				addPlayerToAdditionalMemcache(player);
				addPlayerToDeleteMemcache(player);
			} else {
				// Player war noch nicht im Highscore, erreicht jedoch jetzt
				// eine Platzierung
				if (containsAdditionalMemcachePlayer(player)) {
					removePlayerFromAdditionalMemcache(player);
				}
				addPlayerToAdditionalMemcache(player);
			}
		}
		List<Entity> resultHighscore = computeHighscore();
		return resultHighscore;
	}

}