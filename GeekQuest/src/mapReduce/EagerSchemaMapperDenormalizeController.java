package mapReduce;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.tools.mapreduce.MapJob;
import com.google.appengine.tools.mapreduce.MapReduceJob;
import com.google.appengine.tools.mapreduce.MapReduceSettings;
import com.google.appengine.tools.mapreduce.MapReduceSpecification;
import com.google.appengine.tools.mapreduce.MapSettings;
import com.google.appengine.tools.mapreduce.MapSpecification;
import com.google.appengine.tools.mapreduce.Marshallers;
import com.google.appengine.tools.mapreduce.inputs.DatastoreInput;
import com.google.appengine.tools.mapreduce.outputs.DatastoreOutput;
import com.google.appengine.tools.mapreduce.outputs.NoOutput;

import entities.Mission;
import entities.Player;

public class EagerSchemaMapperDenormalizeController {

	public static void startMapJob() {

		// MapSettings settings = new MapSettings.Builder().setWorkerQueueName(
		// "mapreduce-workers").build();
		MapReduceSettings settings = new MapReduceSettings.Builder().build();

		Query query = new Query("Player");

		MapReduceSpecification mapSpec = new MapReduceSpecification.Builder(
				new DatastoreInput(query, 2),
				new EagerSchemaMapperDenormalize(),
				new EagerSchemaReducerDenormalize(), new NoOutput())
				// .setKeyMarshaller(Marshallers.getStringMarshaller())
				.setKeyMarshaller(
						Marshallers.getGenericJsonMarshaller(Player.class))
				.setValueMarshaller(
						Marshallers.getGenericJsonMarshaller(Mission.class))
				.build();

		// MapReduceSpecification mapSpec = new
		// MapReduceSpecification.Builder<>(
		// new DatastoreInput(query, 2),
		// new EagerSchemaMapperDenormalize(),
		// new EagerSchemaReducerDenormalize(), new DatastoreOutput())
		// .setKeyMarshaller(Marshallers.getStringMarshaller())
		// .setValueMarshaller(
		// Marshallers.getGenericJsonMarshaller(Entity.class))
		// .setJobName("MapReduceTest Denormalize").setNumReducers(1)
		// .build();

		String id = MapReduceJob.start(mapSpec, settings);
		System.out.println("MapReduce Id: " + id);
	}
}
