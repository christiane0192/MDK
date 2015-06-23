package mapReduce;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.tools.mapreduce.DatastoreMutationPool;
import com.google.appengine.tools.mapreduce.KeyValue;
import com.google.appengine.tools.mapreduce.Reducer;
import com.google.appengine.tools.mapreduce.ReducerInput;

import entities.Charclass;
import entities.Mission;
import entities.Player;

public class EagerSchemaReducerDenormalize extends
		Reducer<Player, Mission, Void> {

	private static final long serialVersionUID = 1L;
	private transient DatastoreMutationPool pool;

	public void beginSlice() {
		pool = DatastoreMutationPool.create();
	}

	public void endSlice() {
		pool.flush();
	}

	@Override
	public void reduce(Player arg0, ReducerInput<Mission> arg1) {

		while (arg1.hasNext()) {
			// Entity entity = arg1.next();
			// if (entity.getProperty("charclass") == null
			// || entity.getProperty("charclass").equals("")) {
			// entity.setProperty("charclass", arg0);
			// pool.put(entity);
			// // emit("Hallo");
			// }

		}
	}

}
