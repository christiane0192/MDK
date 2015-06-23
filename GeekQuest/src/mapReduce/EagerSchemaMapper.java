package mapReduce;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.tools.mapreduce.DatastoreMutationPool;
import com.google.appengine.tools.mapreduce.MapOnlyMapper;

public class EagerSchemaMapper extends MapOnlyMapper<Entity, Void> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private transient DatastoreMutationPool pool;

	public void beginSlice() {
		pool = DatastoreMutationPool.create();
	}

	public void endSlice() {
		pool.flush();
	}

	@Override
	public void map(Entity arg0) {
		if ((arg0.getProperty("name") != null && arg0.getProperty("name") != "")
				|| (arg0.getProperty("gold") == null || arg0
						.getProperty("gold").equals(""))) {
			if (arg0.getProperty("name") != null
					&& arg0.getProperty("name") != "") {
				arg0.setProperty("nickname", arg0.getProperty("name"));
				arg0.setProperty("name", null);
			}
			if (arg0.getProperty("gold") == null
					|| arg0.getProperty("gold").equals("")) {
				arg0.setProperty("gold", 0);
			}
			pool.put(arg0);
		}
	}
}
