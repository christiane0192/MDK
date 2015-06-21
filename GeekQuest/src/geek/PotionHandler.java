package geek;

import static com.googlecode.objectify.ObjectifyService.ofy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.appengine.api.datastore.Transaction;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.VoidWork;

import entities.Mission;
import entities.Player;
import entities.Potion;

public class PotionHandler {

	public static void drinkPotion(final Player player, final Potion potion) {
		ofy().transact(new VoidWork() {
			public void vrun() {
				if (player.getHealth() != 0 && !potion.isEmpty()) {
					if (player.getHealth() + potion.getHealthpoints() > 10) {
						potion.drink();
						int add = (int) (10 - player.getHealth());
						player.heal(add);
					} else {
						player.heal(potion.drink());
					}
				}
			}

		});
	}

	public static void sellPotion(final Player playerSeller,
			final Player buyer, final Potion potion, final int price) {
		ofy().transact(new VoidWork() {
			public void vrun() {
				if (buyer.getGold() >= price) {
					Key<Player> playerSellerKey = Key.create(Player.class,
							playerSeller.getEmail());
					Key<Potion> originalPotionKey = Key.create(playerSellerKey,
							Potion.class, potion.getId());
					potion.setId(2);
					Key<Player> buyerKey = Key.create(Player.class,
							buyer.getEmail());
					potion.setPlayer(buyerKey);
					ofy().save().entity(potion).now();
					ofy().delete().key(originalPotionKey).now();
					buyer.setGold(buyer.getGold() - price);
					playerSeller.setGold(playerSeller.getGold() + price);
				}
			}

		});
	}

	public static void payMercenaries(final Player player, final int pay) {
		ofy().transact(new VoidWork() {
			public void vrun() {
				Collection<Player> mercenaries = ofy().load()
						.refs(player.getMercenaries()).values();
				if (player.getGold() > pay * mercenaries.size()) {
					for (Player mercenary : mercenaries) {
						mercenary.setGold(mercenary.getGold() + pay);
					}
				}
				player.setGold(player.getGold() - pay * mercenaries.size());
			}

		});
	}
}
