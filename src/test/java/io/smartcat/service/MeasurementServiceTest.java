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

	@Test
	public void getNewestMeasurementsForUser_shouldReturnOnlyUserMeasurementData_forUsername() {
		createTestMeasurementData();
		List<Measurement> result = measurementService.getNewestMeasurementsForUser("batman", 50);
		Assert.assertEquals(50, result.size());
		result.forEach(m -> Assert.assertEquals("batman", m.getOwner()));
	}
	
	@Test
	public void getNewestMeasurementsForUser_shouldReturnDataInDescendingOrder() {
		createTestMeasurementData();
		List<Measurement> result = measurementService.getNewestMeasurementsForUser("batman", 50);
		Assert.assertEquals(50, result.size());
		
		long previousTimestamp = result.get(0).getCreated();
		for (Measurement measurement : result) {
			System.out.println("timestamp is: " + previousTimestamp);
			Assert.assertTrue(measurement.getCreated() <= previousTimestamp);
			
			previousTimestamp = measurement.getCreated();
		}
		
	}
	
	@Test
	public void getNewestMeasurementsForUser_shouldReturn50NewestMeasurments_forBatman() {
		createTestMeasurementData();
		List<Measurement> result = measurementService.getNewestMeasurementsForUser("batman", 50);
		Assert.assertEquals(50, result.size());
		result.forEach(measurment -> Assert.assertEquals("marker sensor", measurment.getSensor()));
	}
	
	private void createTestMeasurementData() {
		RandomBuilder<Measurement> dataBuilderForOtherUsers = new RandomBuilder<>(Measurement.class);
		dataBuilderForOtherUsers.randomFrom("owner", "robin", "yoda", "jedi", "skywalker")
			.randomFromRange("created", 1000L, 2000L) // TODO: fill other properties as well
			.toBeBuilt(500);
		
		RandomBuilder<Measurement> batmanDataBuilder = new RandomBuilder<>(Measurement.class);
		batmanDataBuilder.randomFrom("owner", "batman")
			.randomFromRange("created", 1500L, 1550L)
			.randomFrom("sensor", "marker sensor")
			.toBeBuilt(50);
		
		RandomBuilder<Measurement> oldDataForBatman = new RandomBuilder<>(Measurement.class);
		oldDataForBatman.randomFrom("owner", "batman")
			.randomFromRange("created", 1000L, 1500L)
			.toBeBuilt(500);
		
		BuildRunner<Measurement> runner = new BuildRunner<>();
		runner.addBuilder(dataBuilderForOtherUsers);
		runner.addBuilder(batmanDataBuilder);
		runner.addBuilder(oldDataForBatman);
		
		List<Measurement> testMeasurements = runner.build();
		
		Collections.shuffle(testMeasurements);
		
		mongoOps.insertAll(testMeasurements);
		
	}

}
