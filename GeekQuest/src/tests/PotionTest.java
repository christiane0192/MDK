package tests;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.VoidWork;
import com.googlecode.objectify.util.Closeable;

import entities.Charclass;
import entities.Color;
import entities.Player;
import entities.Potion;
import geek.PotionHandler;

public class PotionTest {

	// maximum eventual consistency
	private final LocalServiceTestHelper helper = new LocalServiceTestHelper(
			new LocalDatastoreServiceTestConfig()
					.setDefaultHighRepJobPolicyUnappliedJobPercentage(100));

	Closeable session;

	@Before
	public void setUp() {
		helper.setUp();
		session = ObjectifyService.begin();
		ObjectifyService.register(Player.class);
		ObjectifyService.register(Potion.class);
	}

	@After
	public void tearDown() {
		helper.tearDown();
		session.close();
		session = null;
	}

	@Test
	public void testDeadPlayer() throws EntityNotFoundException {

		String email = "email100@test.de";
		Player player = new Player();
		player.setCharclass(Charclass.DWARF);
		player.setEmail(email);
		player.setHealth(0);
		player.setName("Frodo");
		player.setScore(398);
		ofy().save().entity(player).now();

		Potion potion = new Potion();
		potion.setId(40);
		potion.setColor(Color.PURPLE);
		potion.setHealthpoints(4);
		potion.setPlayer(Key.create(Player.class, player.getEmail()));
		ofy().save().entity(potion).now();

		PotionHandler.drinkPotion(player, potion);

		assertEquals(0, player.getHealth());
		assertEquals(4, potion.getHealthpoints());
		assertEquals(false, potion.isEmpty());
	}

	@Test
	public void testMaximumHealth10() throws EntityNotFoundException {

		String email = "email100@test.de";
		Player player = new Player();
		player.setCharclass(Charclass.DWARF);
		player.setEmail(email);
		player.setHealth(7);
		player.setName("Frodo");
		player.setScore(398);
		ofy().save().entity(player).now();

		Potion potion = new Potion();
		potion.setId(40);
		potion.setColor(Color.PURPLE);
		potion.setHealthpoints(4);
		potion.setPlayer(Key.create(Player.class, player.getEmail()));
		ofy().save().entity(potion).now();

		PotionHandler.drinkPotion(player, potion);

		assertEquals(10, player.getHealth());
		assertEquals(0, potion.getHealthpoints());
		assertEquals(true, potion.isEmpty());
	}

	@Test
	public void testSellPotion() throws EntityNotFoundException {

		String email = "email100@test.de";
		Player player = new Player();
		player.setCharclass(Charclass.DWARF);
		player.setEmail(email);
		player.setHealth(4);
		player.setGold(30);
		player.setName("Frodo");
		player.setScore(398);
		ofy().save().entity(player).now();

		String email2 = "email101@test.de";
		Player player2 = new Player();
		player2.setCharclass(Charclass.ELF);
		player2.setEmail(email2);
		player2.setGold(50);
		player2.setHealth(4);
		player2.setName("Maria");
		player2.setScore(398);
		ofy().save().entity(player2).now();

		Potion potion = new Potion();
		potion.setId(40);
		potion.setColor(Color.PURPLE);
		potion.setHealthpoints(4);
		potion.setPlayer(Key.create(Player.class, player.getEmail()));
		ofy().save().entity(potion).now();

		PotionHandler.sellPotion(player, player2, potion, 10);

		assertEquals(40, player.getGold());
		assertEquals(40, player2.getGold());
		assertEquals(1, ofy().load().ancestor(player).count());
		assertEquals(2, ofy().load().ancestor(player2).count());
	}

	@Test
	public void testPayMercenaries() {

		String email2 = "email101@test.de";
		Player mercenary1 = new Player();
		mercenary1.setCharclass(Charclass.ELF);
		mercenary1.setEmail(email2);
		mercenary1.setGold(50);
		mercenary1.setHealth(4);
		mercenary1.setName("Maria");
		mercenary1.setScore(398);
		ofy().save().entity(mercenary1).now();

		String email3 = "email102@test.de";
		Player mercenary2 = new Player();
		mercenary2.setCharclass(Charclass.HOBBIT);
		mercenary2.setEmail(email3);
		mercenary2.setGold(50);
		mercenary2.setHealth(4);
		mercenary2.setName("Janine");
		mercenary2.setScore(450);
		ofy().save().entity(mercenary2).now();

		List<Ref<Player>> mercenaryList = new ArrayList<Ref<Player>>();
		mercenaryList.add(Ref.create(mercenary1));
		mercenaryList.add(Ref.create(mercenary2));

		String email = "email100@test.de";
		Player player = new Player();
		player.setCharclass(Charclass.DWARF);
		player.setEmail(email);
		player.setHealth(4);
		player.setGold(2000);
		player.setName("Frodo");
		player.setScore(398);
		player.setMercenaries(mercenaryList);
		ofy().save().entity(player).now();

		PotionHandler.payMercenaries(player, 50);
		Player m1 = ofy().load().type(Player.class).id(mercenary1.getEmail())
				.now();
		Player m2 = ofy().load().type(Player.class).id(mercenary2.getEmail())
				.now();

		assertEquals(1900, player.getGold());
		assertEquals(100, m1.getGold());
		assertEquals(100, m2.getGold());

	}
}
