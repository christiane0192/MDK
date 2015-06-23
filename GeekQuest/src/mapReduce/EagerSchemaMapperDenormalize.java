package mapReduce;

import static com.googlecode.objectify.ObjectifyService.ofy;

import java.util.Collection;
import java.util.List;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.tools.mapreduce.DatastoreMutationPool;
import com.google.appengine.tools.mapreduce.MapOnlyMapper;
import com.google.appengine.tools.mapreduce.Mapper;
import com.googlecode.objectify.Ref;

import entities.Charclass;
import entities.Mission;
import entities.Player;

public class EagerSchemaMapperDenormalize extends
		Mapper<Entity, Player, Mission> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private transient DatastoreMutationPool pool;

	private DatastoreService datastore;

	// private transient DatastoreService datastore =
	// DatastoreServiceFactory.getDatastoreService();

	public void beginSlice() {
		pool = DatastoreMutationPool.create();
	}

	public void endSlice() {
		pool.flush();
	}

	@Override
	public void map(Entity arg0) {
		datastore = DatastoreServiceFactory.getDatastoreService();
		Player player = ofy().load().type(Player.class)
				.id((String) arg0.getKey().getName()).now();
		List<Ref<Mission>> refs = player.getMissions();
		Collection<Mission> missions = ofy().load().refs(refs).values();
		for (Mission mission : missions) {
			Key key = KeyFactory.createKey("Mission", mission.getDescription());
			try {
				Entity entity = datastore.get(key);
				player.setMissions(null);
				// String s = arg0.getProperty(propertyName)
				// emit(player.getCharclass().toString(), entity);
				emit(player, mission);
			} catch (EntityNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}
}
