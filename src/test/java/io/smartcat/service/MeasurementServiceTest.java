package io.smartcat.service;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.test.context.junit4.SpringRunner;

import io.smartcat.domain.Measurement;
import io.smartcat.ranger.AggregatedObjectGenerator;
import io.smartcat.ranger.ObjectGenerator;

@RunWith(SpringRunner.class)
@SpringBootTest
public class MeasurementServiceTest {
	
	@Autowired
	private MeasurementService measurementService;
	
	@Autowired
	private MongoOperations mongoOps;
	
	// test for demo purposes only
	@Test
	public void findByOwnerAndSensor_shouldReturnCorrectData_forPassedOwnerAndSensor() {
		createTestData();
		List<Measurement> result = measurementService.getMeasurementsByUserAndSensor("flint", "EM-sensor");
		
		Assert.assertEquals(50, result.size());
		
		result.forEach(measurement -> Assert.assertEquals("flint", measurement.getOwner()));
		result.forEach(measurement -> Assert.assertEquals("EM-sensor", measurement.getSensor()));
	}
	
	private void createTestData() {		
		System.out.println("Starting data generation...");
		long start = System.currentTimeMillis();
		
		ObjectGenerator<Measurement> otherUsersData = new ObjectGenerator.Builder<Measurement>(Measurement.class)
				.withValues("owner", "alice", "bob", "charlie", "david", "emma")
				.withRanges("created", 1000L, 2000L)
				.withRanges("measuredValue", 0L, 100L)
				.withValues("sensor", "hear-rate-monitor", "accelerometer", "hygrometer", "thermometer")
				.toBeGenerated(400_000).build();
		
		ObjectGenerator<Measurement> flintUserData = new ObjectGenerator.Builder<Measurement>(Measurement.class)
				.withValues("owner", "flint")
				.withRanges("created", 1500L, 1600L)
				.withRanges("measuredValue", 0L, 100L)
				.withValues("sensor", "EM-sensor")
				.toBeGenerated(50).build();
		
		ObjectGenerator<Measurement> flintUserOtherData = new ObjectGenerator.Builder<Measurement>(Measurement.class)
				.withValues("owner", "flint")
				.withRanges("created", 1000L, 1500L)
				.withRanges("measuredValue", 0L, 100L)
				.withValues("sensor", "hear-rate-monitor", "accelerometer", "hygrometer", "thermometer")
				.toBeGenerated(99_950).build();
		
        AggregatedObjectGenerator<Measurement> aggregatedObjectGenerator = new AggregatedObjectGenerator.Builder<Measurement>()
                .withObjectGenerator(otherUsersData)
                .withObjectGenerator(flintUserOtherData)
                .withObjectGenerator(flintUserData).build();
        
        List<Measurement> measurements = aggregatedObjectGenerator.generateAll();
		long end = System.currentTimeMillis();
		System.out.println("Data generated in: " + (end - start)/1000 + " seconds");
		System.out.println("Saving data in db...");
		mongoOps.insertAll(measurements);
		long endSave = System.currentTimeMillis();
		System.out.println("Data saved in db in: " + (endSave - end)/1000 + " seconds");
		
	}

}
