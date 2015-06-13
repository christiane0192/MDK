package tests;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static org.junit.Assert.*;
import entities.Charclass;
import entities.Player;
import geek.HighscoreCalculator;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.dev.HighRepJobPolicy;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;

public class HighscoreEventualConsistencyTest {

	DatastoreService datastore;

	// private List<Entity> expectedResult;
	//
	// Entity player;
	private List<Player> expectedResult;

	Player player;

	public static final class Policy implements HighRepJobPolicy {
		static boolean shouldApply = false;

		public static void applyAll() {
			shouldApply = true;
		}

		public static void applyNone() {
			shouldApply = false;
		}

		@Override
		public boolean shouldApplyNewJob(Key entityGroup) {
			return shouldApply;
		}

		@Override
		public boolean shouldRollForwardExistingJob(Key entityGroup) {
			return shouldApply;
		}
	}

	public final LocalServiceTestHelper helper = new LocalServiceTestHelper(
			new LocalDatastoreServiceTestConfig()
					.setAlternateHighRepJobPolicyClass(Policy.class));

	@Before
	public void setUp() {
		datastore = DatastoreServiceFactory.getDatastoreService();
		helper.setUp();
		Policy.applyAll();
		putTestPlayers();
		player = getTestCharacter();
		expectedResult = getExpectedHighscoreList(player);
	}

	@After
	public void tearDown() {
		helper.tearDown();
	}

	@Test
	public void testHighscoreWithWeakConsistency() {

		Policy.applyNone();

		HighscoreCalculator calculator = new HighscoreCalculator();
		// List<Entity> actualHighscore = calculator.handleNewHighscore(player);
		List<Player> actualHighscore = calculator.handleNewHighscore(player);

		// for (Entity result : expectedResult) {
		// System.out.println(result.getProperty("playername") + "  "
		// + result.getProperty("score"));
		// }

		// System.out.println();
		// System.out.println();

		// for (Entity result : actualHighscore) {
		for (Player result : actualHighscore) {
			System.out.println(result.getName() + "  " + result.getScore());
		}

		assertEquals(expectedResult, actualHighscore);

	}

	@Test
	public void testHighscoreWithStrongConsistency() {

		Policy.applyAll();

		HighscoreCalculator calculator = new HighscoreCalculator();
		// List<Entity> actualHighscore = calculator.handleNewHighscore(player);
		List<Player> actualHighscore = calculator.handleNewHighscore(player);

		// for (Entity result : expectedResult) {
		for (Player result : expectedResult) {
			// System.out.println(result.getProperty("playername") + "  "
			// + result.getProperty("score"));
			System.out.println(result.getName() + "  " + result.getScore());
		}

		System.out.println();
		System.out.println();

		// for (Entity result : actualHighscore) {
		// System.out.println(result.getProperty("playername") + "  "
		// + result.getProperty("score"));
		// }
		for (Player result : actualHighscore) {
			// System.out.println(result.getProperty("playername") + "  "
			// + result.getProperty("score"));
			System.out.println(result.getName() + "  " + result.getScore());
		}

		assertEquals(expectedResult, actualHighscore);

	}

	private List<Player> getExpectedHighscoreList(Player player) {
		// private List<Entity> getExpectedHighscoreList(Entity player) {
		// Query q = HighscoreCalculator.getHighscorePlayerQuery();
		// PreparedQuery pq = datastore.prepare(q);
		// List<Entity> results = pq.asList(FetchOptions.Builder.withLimit(10));

		List<Player> results = ofy().load().type(Player.class).order("score")
				.limit(10).list();

		results.add(2, player);
		results.remove(10);

		return results;

	}

	private void putTestPlayers() {
		String kindPlayer = "Player";

		String emailPlayer1 = "email1@test.de";
		Key keyPlayer1 = KeyFactory.createKey(kindPlayer, emailPlayer1);
		Entity player1 = new Entity(keyPlayer1);
		player1.setProperty("playername", "Hans");
		player1.setProperty("character", "Mage");
		player1.setProperty("health", 25);
		player1.setProperty("email", emailPlayer1);
		player1.setProperty("score", 199);
		datastore.put(player1);

		String emailPlayer2 = "email2@test.de";
		Key keyPlayer2 = KeyFactory.createKey(kindPlayer, emailPlayer2);
		Entity player2 = new Entity(keyPlayer2);
		player2.setProperty("playername", "Maria");
		player2.setProperty("character", "Mage");
		player2.setProperty("health", 25);
		player2.setProperty("email", emailPlayer2);
		player2.setProperty("score", 200);
		datastore.put(player2);

		String emailPlayer3 = "email3@test.de";
		Key keyPlayer3 = KeyFactory.createKey(kindPlayer, emailPlayer3);
		Entity player3 = new Entity(keyPlayer3);
		player3.setProperty("playername", "Vivien");
		player3.setProperty("character", "Mage");
		player3.setProperty("health", 25);
		player3.setProperty("email", emailPlayer3);
		player3.setProperty("score", 251);
		datastore.put(player3);

		String emailPlayer4 = "email4@test.de";
		Key keyPlayer4 = KeyFactory.createKey(kindPlayer, emailPlayer4);
		Entity player4 = new Entity(keyPlayer4);
		player4.setProperty("playername", "Johann");
		player4.setProperty("character", "Mage");
		player4.setProperty("health", 25);
		player4.setProperty("email", emailPlayer4);
		player4.setProperty("score", 270);
		datastore.put(player4);

		String emailPlayer5 = "email5@test.de";
		Key keyPlayer5 = KeyFactory.createKey(kindPlayer, emailPlayer5);
		Entity player5 = new Entity(keyPlayer5);
		player5.setProperty("playername", "Fiona");
		player5.setProperty("character", "Warrior");
		player5.setProperty("health", 25);
		player5.setProperty("email", emailPlayer5);
		player5.setProperty("score", 245);
		datastore.put(player5);

		String emailPlayer6 = "email6@test.de";
		Key keyPlayer6 = KeyFactory.createKey(kindPlayer, emailPlayer6);
		Entity player6 = new Entity(keyPlayer6);
		player6.setProperty("playername", "Alex");
		player6.setProperty("character", "Hobbit");
		player6.setProperty("health", 25);
		player6.setProperty("email", emailPlayer6);
		player6.setProperty("score", 300);
		datastore.put(player6);

		String emailPlayer7 = "email7@test.de";
		Key keyPlayer7 = KeyFactory.createKey(kindPlayer, emailPlayer7);
		Entity player7 = new Entity(keyPlayer7);
		player7.setProperty("playername", "Janina");
		player7.setProperty("character", "Hobbit");
		player7.setProperty("health", 25);
		player7.setProperty("email", emailPlayer7);
		player7.setProperty("score", 123);
		datastore.put(player7);

		String emailPlayer8 = "email8@test.de";
		Key keyPlayer8 = KeyFactory.createKey(kindPlayer, emailPlayer8);
		Entity player8 = new Entity(keyPlayer8);
		player8.setProperty("playername", "Max");
		player8.setProperty("character", "Mage");
		player8.setProperty("health", 25);
		player8.setProperty("email", emailPlayer8);
		player8.setProperty("score", 654);
		datastore.put(player8);

		String emailPlayer9 = "email9@test.de";
		Key keyPlayer9 = KeyFactory.createKey(kindPlayer, emailPlayer9);
		Entity player9 = new Entity(keyPlayer9);
		player9.setProperty("playername", "Janina");
		player9.setProperty("character", "Hobbit");
		player9.setProperty("health", 25);
		player9.setProperty("email", emailPlayer9);
		player9.setProperty("score", 344);
		datastore.put(player9);

		String emailPlayer10 = "email10@test.de";
		Key keyPlayer10 = KeyFactory.createKey(kindPlayer, emailPlayer10);
		Entity player10 = new Entity(keyPlayer10);
		player10.setProperty("playername", "Julian");
		player10.setProperty("character", "Warior");
		player10.setProperty("health", 25);
		player10.setProperty("email", emailPlayer10);
		player10.setProperty("score", 734);
		datastore.put(player10);

	}

	private Player getTestCharacter() {
		// private Entity getTestCharacter() {
		// String kindPlayer = "Player";
		//
		// String emailPlayer = "JUnit@test.de";
		// Key keyPlayer = KeyFactory.createKey(kindPlayer, emailPlayer);
		// Entity player = new Entity(keyPlayer);
		// player.setProperty("playername", "Frodo");
		// player.setProperty("character", "Hobbit");
		// player.setProperty("health", 25);
		// player.setProperty("email", emailPlayer);
		// player.setProperty("score", 527l);

		Player player = new Player();
		player.setName("Frodo");
		player.setCharclass(Charclass.HOBBIT);
		player.setHealth(25);
		player.setEmail("JUnit@test.de");
		player.setScore(527l);

		return player;

	}
}
