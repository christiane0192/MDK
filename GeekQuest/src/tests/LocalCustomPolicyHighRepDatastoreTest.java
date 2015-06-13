package tests;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static org.junit.Assert.*;
import entities.Player;
import geek.HighscoreCalculator;
import geek.WelcomeServlet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.dev.HighRepJobPolicy;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;

public class LocalCustomPolicyHighRepDatastoreTest {

	private static final class CustomHighRepJobPolicy implements
			HighRepJobPolicy {
		static int newJobCounter = 0;
		static int existingJobCounter = 0;

		// apply ever second new job
		// @Override
		// public boolean shouldApplyNewJob(Key arg0) {
		// // every other new job fails to apply
		// return newJobCounter++ % 2 == 0;
		// }

		// apply ever third new job
		@Override
		public boolean shouldApplyNewJob(Key arg0) {
			// every other new job fails to apply
			return newJobCounter++ % 3 == 0;
		}

		@Override
		public boolean shouldRollForwardExistingJob(Key arg0) {
			// every other existing job fails to apply
			return existingJobCounter++ % 2 == 0;
		}

	}

	private final LocalServiceTestHelper helper = new LocalServiceTestHelper(
			new LocalDatastoreServiceTestConfig()
					.setAlternateHighRepJobPolicyClass(CustomHighRepJobPolicy.class));

	@Before
	public void setUp() {
		helper.setUp();

	}

	@After
	public void tearDown() {
		helper.tearDown();
	}

	@Test
	public void testQuery() throws EntityNotFoundException {

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();

		String kindPlayer = "Player";

		String emailPlayerOne = "email100@test.de";
		Key keyPlayerOne = KeyFactory.createKey(kindPlayer, emailPlayerOne);
		Entity playerOne = new Entity(keyPlayerOne);
		playerOne.setProperty("score", 1000);
		ds.put(playerOne);

		String emailPlayerTwo = "email200@test.de";
		Key keyPlayerTwo = KeyFactory.createKey(kindPlayer, emailPlayerTwo);
		Entity playerTwo = new Entity(keyPlayerTwo);
		playerTwo.setProperty("score", 1500);
		ds.put(playerTwo);

		// for test with 3 Players
		String emailPlayerThree = "email300@test.de";
		Key keyPlayerThree = KeyFactory.createKey(kindPlayer, emailPlayerThree);
		Entity playerThree = new Entity(keyPlayerThree);
		playerThree.setProperty("score", 900);
		ds.put(playerThree);

		// Test Queries for 2 Players, apply every second job
		// test2Players(ds);

		// Test Queries for 3 Players, apply every second job
		// test3Players(ds);

		// Test Queries for 3 Players, apply every third job
		test3PlayersThirdApplied(ds);

	}

	private void test2Players(DatastoreService ds) {
		// first global query only sees the first Entity
		// assertEquals(1,
		// ds.prepare(HighscoreCalculator.getHighscorePlayerQuery())
		// .countEntities(FetchOptions.Builder.withLimit(10)));
		assertEquals(1, ofy().load().type(Player.class).order("score")
				.limit(10).count());
		// second global query sees both Entities because we "groom" (attempt to
		// apply unapplied jobs) after every query
		// assertEquals(2,
		// ds.prepare(HighscoreCalculator.getHighscorePlayerQuery())
		// .countEntities(FetchOptions.Builder.withLimit(10)));
		assertEquals(2, ofy().load().type(Player.class).order("score")
				.limit(10).count());

	}

	private void test3Players(DatastoreService ds) {
		// first global query only sees first an third Entity
		// assertEquals(2,
		// ds.prepare(HighscoreCalculator.getHighscorePlayerQuery())
		// .countEntities(FetchOptions.Builder.withLimit(10)));
		assertEquals(2, ofy().load().type(Player.class).order("score")
				.limit(10).count());
		// second global query sees all three Entities because we "groom"
		// (attempt to
		// apply unapplied jobs) after every query
		// assertEquals(3,
		// ds.prepare(HighscoreCalculator.getHighscorePlayerQuery())
		// .countEntities(FetchOptions.Builder.withLimit(10)));
		assertEquals(3, ofy().load().type(Player.class).order("score")
				.limit(10).count());

	}

	private void test3PlayersThirdApplied(DatastoreService ds) {
		// first global query only sees first an third Entity
		// assertEquals(1,
		// ds.prepare(HighscoreCalculator.getHighscorePlayerQuery())
		// .countEntities(FetchOptions.Builder.withLimit(10)));
		assertEquals(1, ofy().load().type(Player.class).order("score")
				.limit(10).count());
		// second global query sees all three Entities because we "groom"
		// (attempt to
		// apply unapplied jobs) after every query
		// assertEquals(2,
		// ds.prepare(HighscoreCalculator.getHighscorePlayerQuery())
		// .countEntities(FetchOptions.Builder.withLimit(10)));
		assertEquals(2, ofy().load().type(Player.class).order("score")
				.limit(10).count());

	}

}
