package io.smartcat.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.DbCallback;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.stereotype.Service;

import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoException;

import io.smartcat.repository.MeasurementRepository;

@Service
public class ReportService {
	
	@Autowired
	private MeasurementRepository measurementRepository;
	
	@Autowired
	private MongoOperations mongoOps;
	
	public List<AvgHeartBeatRateDTO> calcAvgHeartBeatRate(long startDate, long endDate) {
		List<AvgHeartBeatRateDTO> result = mongoOps.execute(new DbCallback<List<AvgHeartBeatRateDTO>>() {

			@Override
			public List<AvgHeartBeatRateDTO> doInDB(DB db) throws MongoException, DataAccessException {
				DBCollection collection = db.getCollection("measurements");

				//  db.measurements.aggregate( [
				//	{ $match : { created : {$gte : 10000, $lte : 15050 }, sensor : "Heart Beat Monitor"}}, 
				//	{ $group : {_id : {owner : "$owner", sensor : "$sensor"}, avg_hb : {$avg : "$measuredValue"}}}, 
				//	{ $project : {_id : 0, owner : "$_id.owner", avg_hb : "$avg_hb"}}])
				DBObject match = new BasicDBObject("$match", new BasicDBObject("created", new BasicDBObject("$gte", startDate).append("$lt", endDate)).append("sensor", "Heart Beat Monitor"));
				DBObject group = new BasicDBObject("$group", new BasicDBObject("_id", new BasicDBObject("owner", "$owner").append("sensor", "$sensor")).append("avg_hb", new BasicDBObject("$avg", "$measuredValue")));
				DBObject project = new BasicDBObject("$project", new BasicDBObject("_id", 0).append("owner", "$_id.owner").append("avg_hb", "$avg_hb"));
				List<DBObject> pipeline = new ArrayList<>();
				pipeline.add(match);
				pipeline.add(group);
				pipeline.add(project);

				AggregationOutput output = collection.aggregate(pipeline);
				List<AvgHeartBeatRateDTO> result = new ArrayList<>();
				for (DBObject dbObject : output.results()) {
					AvgHeartBeatRateDTO avgHeartBeatRateDTO = new AvgHeartBeatRateDTO();
					System.out.println(dbObject.toString());
					String owner = (String) dbObject.get("owner");
					double avgHb = (double) dbObject.get("avg_hb");
					avgHeartBeatRateDTO.setAvgHeartBeatRate(avgHb);
					avgHeartBeatRateDTO.setUsername(owner);
					result.add(avgHeartBeatRateDTO);
				}
				return result;
			}
		});
		
		return result;
	}

}
