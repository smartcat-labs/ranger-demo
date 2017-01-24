package io.smartcat.service;

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.test.context.junit4.SpringRunner;

import com.sun.jna.platform.win32.OaIdl.MEMBERID;

import io.smartcat.data.loader.BuildRunner;
import io.smartcat.data.loader.RandomBuilder;
import io.smartcat.domain.Measurement;

@RunWith(SpringRunner.class)
@SpringBootTest
public class MeasurementServiceTest {
	
	@Autowired
	private MeasurementService measurementService;
	
	@Autowired
	private MongoOperations mongoOps;
	
	// test for demo purposes only
	// not orthogonal and stuff
	@Test
	public void findByOwnerAndSensor_shouldReturnCorrectData_forPassedOwnerAndSensor() {
		createTestData();
		List<Measurement> result = measurementService.getMeasurementsByUserAndSensor("batman", "proximity");
		
		Assert.assertEquals(50, result.size());
		
		result.forEach(measurement -> Assert.assertEquals("batman", measurement.getOwner()));
		result.forEach(measurement -> Assert.assertEquals("proximity", measurement.getSensor()));
	}
	
	private void createTestData() {		
		System.out.println("Starting data generation...");
		long start = System.currentTimeMillis();
		
		RandomBuilder<Measurement> otherUsersData = new RandomBuilder<>(Measurement.class);
		otherUsersData.randomFrom("owner", "superman", "robin", "goblin")
		.randomFromRange("created", 1000L, 2000L)
		.randomFromRange("measuredValue", 0L, 100L)
		.randomFrom("sensor", "hbm", "accelerometer", "hygrometer", "proximity")
		.toBeBuilt(400000);
		
		RandomBuilder<Measurement> batmansProximity = new RandomBuilder<>(Measurement.class);
		batmansProximity.randomFrom("owner", "batman")
		.randomFromRange("created", 1500L, 1600L)
		.randomFromRange("measuredValue", 0L, 100L)
		.randomFrom("sensor", "proximity")
		.toBeBuilt(50);
		
		RandomBuilder<Measurement> otherSensorsForBatman = new RandomBuilder<>(Measurement.class);
		otherSensorsForBatman.randomFrom("owner", "batman")
		.randomFromRange("created", 1000L, 1500L)
		.randomFromRange("measuredValue", 0L, 100L)
		.randomFrom("sensor", "hbm", "accelerometer", "hygrometer")
		.toBeBuilt(99950);
		
		BuildRunner<Measurement> runner = new BuildRunner<>();
		runner.addBuilder(otherUsersData);
		runner.addBuilder(batmansProximity);
		runner.addBuilder(otherSensorsForBatman);
		
		List<Measurement> result = runner.build();
		long end = System.currentTimeMillis();
		System.out.println("Data generated in: " + (end - start)/1000 + " seconds");
		System.out.println("Saving data in db...");
		mongoOps.insertAll(result);
		long endSave = System.currentTimeMillis();
		System.out.println("Data saved in db in: " + (endSave - end)/1000 + " seconds");
		
		
	}


}
