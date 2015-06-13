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
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;

public class LocalHighRepDatastoreTest {

	// maximum eventual consistency
	private final LocalServiceTestHelper helper = new LocalServiceTestHelper(
			new LocalDatastoreServiceTestConfig()
					.setDefaultHighRepJobPolicyUnappliedJobPercentage(100));

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
		String emailPlayer = "email100@test.de";
		Key keyPlayer = KeyFactory.createKey(kindPlayer, emailPlayer);
		Entity player = new Entity(keyPlayer);
		player.setProperty("score", 1000);
		ds.put(player);

		// assertEquals(0,
		// ds.prepare(HighscoreCalculator.getHighscorePlayerQuery())
		// .countEntities(FetchOptions.Builder.withLimit(10)));
		assertEquals(0, ofy().load().type(Player.class).order("score")
				.limit(10).count());

		// assertEquals(1, ds.prepare(new Query(kindPlayer, keyPlayer))
		// .countEntities(FetchOptions.Builder.withLimit(10)));

		assertEquals(1, ofy().load().type(Player.class).order("score")
				.limit(10).count());

	}

}
