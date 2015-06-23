package mapReduce;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.tools.mapreduce.MapJob;
import com.google.appengine.tools.mapreduce.MapSettings;
import com.google.appengine.tools.mapreduce.MapSpecification;
import com.google.appengine.tools.mapreduce.inputs.DatastoreInput;
import com.google.appengine.tools.mapreduce.outputs.NoOutput;

public class EagerSchemaMapperController {

	public static void startMapJob() {

		// MapSettings settings = new MapSettings.Builder().setWorkerQueueName(
		// "mapreduce-workers").build();
		MapSettings settings = new MapSettings.Builder().build();

		Query query = new Query("Player");

		MapSpecification mapSpec = new MapSpecification.Builder<>(
				new DatastoreInput(query, 2), new EagerSchemaMapper())
				.setJobName("Create MapReduce entities").build();

		String id = MapJob.start(mapSpec, settings);
		System.out.println("MapReduce Id: " + id);
	}
}
