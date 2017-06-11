package io.smartcat;


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
import io.smartcat.repository.MeasurementRepository;
import io.smartcat.service.AvgHeartBeatRateDTO;
import io.smartcat.service.MeasurementService;
import io.smartcat.service.ReportService;

@RunWith(SpringRunner.class)
@SpringBootTest
public class RangerDemoApplicationTests {
	
	@Autowired
	private MongoOperations mongoOps;
	
	@Autowired
	private MeasurementService measurementService;
	
	@Autowired
	private MeasurementRepository measurementRepository;
	
	@Autowired
	private ReportService reportService;
	
	@Test
	public void should_return_newest_50_measurements_for_selected_user_and_sensor() {
		createTestMeasurements();
		List<Measurement> result = measurementService.getNewestMeasurementsForUserAndSensor("charlie", "thermometer", 50);
		
		// validate that 50 results are returned
		Assert.assertEquals(50, result.size());
		
		// validate that user is "charlie"
		result.forEach(measurement -> Assert.assertEquals("charlie", measurement.getOwner()));
		
		// validate that sensor is "thermometer"
		result.forEach(measurement -> Assert.assertEquals("thermometer", measurement.getSensor()));
		
		// validate that created is between 1000 and 1100
		result.forEach(measurement -> 
			Assert.assertTrue(measurement.getCreated() >= 1000 && measurement.getCreated() < 1100));
		
		// validate that data is returned in descending order by "created" timestamp
		long previousTimestamp = result.get(0).getCreated();
		for (Measurement m : result) {
			Assert.assertTrue(m.getCreated() <= previousTimestamp);
			previousTimestamp = m.getCreated();
		}
	}
	
	private void createTestMeasurements() {
		System.out.println("Starting data generation...");
		long start = System.currentTimeMillis();
		
		ObjectGenerator<Measurement> newestMeasurementsForCharlieAndThermometer = new ObjectGenerator.Builder<Measurement>(Measurement.class)
				.withValues("owner", "charlie")
				.withRanges("created", 1000L, 1100L)
				.withValues("sensor", "thermometer")
				.withValues("version", (short) 1)
				.toBeGenerated(50).build();
		
		ObjectGenerator<Measurement> overlappingMeasurementTimestampOtherOwners = new ObjectGenerator.Builder<Measurement>(Measurement.class)
				.withValues("owner", "alice", "bob", "david", "emma", "flint") // note: no "charlie"
				.withRanges("created", 1000L, 1100L)
				.withValues("sensor",  "heart-rate-monitor", "accelerometer", "hygrometer", "thermometer")
				.toBeGenerated(1000).build();
		
		ObjectGenerator<Measurement> newerDataForCharlieAndNonThermometer = new ObjectGenerator.Builder<Measurement>(Measurement.class)
				.withValues("owner", "charlie")
				.withRanges("created", 1100L, 1200L)
				.withValues("sensor", "heart-rate-monitor", "accelerometer", "hygrometer")
				.toBeGenerated(1000).build();
		
		ObjectGenerator<Measurement> oldDataForCharlie = new ObjectGenerator.Builder<Measurement>(Measurement.class)
				.withValues("owner", "charlie")
				.withRanges("created", 500L, 1000L)
				.withValues("sensor", "heart-rate-monitor", "accelerometer", "hygrometer", "thermometer")
				.toBeGenerated(1000).build();
		
        AggregatedObjectGenerator<Measurement> aggregatedObjectGenerator = new AggregatedObjectGenerator.Builder<Measurement>()
                .withObjectGenerator(newestMeasurementsForCharlieAndThermometer)
                .withObjectGenerator(overlappingMeasurementTimestampOtherOwners)
                .withObjectGenerator(oldDataForCharlie)
                .withObjectGenerator(newerDataForCharlieAndNonThermometer)
                .build();
        
        List<Measurement> measurements = aggregatedObjectGenerator.generateAll();
		long end = System.currentTimeMillis();
		System.out.println("Data generated in: " + (end - start)/1000 + " seconds");
		System.out.println("Saving data in db...");
		mongoOps.insertAll(measurements);
		long endSave = System.currentTimeMillis();
		System.out.println("Data saved in db in: " + (endSave - end)/1000 + " seconds");
		
	}
	
	@Test
	public void avg_heart_beat_should_return_correct_results() {
		createAverageHeartBeatData();
		List<AvgHeartBeatRateDTO> result = reportService.calcAvgHeartBeatRate(100, 110);
		Assert.assertEquals(2, result.size());
		for (AvgHeartBeatRateDTO dto : result) {
			if (dto.getUsername().equals("alex")) {
				Assert.assertEquals(61, dto.getAvgHeartBeatRate(), 0.0001);
			} else {
				Assert.assertEquals("bob", dto.getUsername());
				Assert.assertEquals(71, dto.getAvgHeartBeatRate(), 0.0001);
			}
		}
	}

	// 1. make sure average is calculated correctly - the tricky part, small number of measurements for one user only?
	// 	avg for alex = 61; avg for bob = 71
	// 2. create other sensors in correct time window
	// 3. create hbm data in wrong time window (before and after)
	// 4. 
	private void createAverageHeartBeatData() {
//		String hbm = "Heart Beat Monitor";
//		RandomBuilder<Measurement> measurmentsBefore = new RandomBuilder<>(Measurement.class);
//		measurmentsBefore
//			.randomFrom("sensor", hbm, "a sensor", "b sensor", "c sensor") 
//			.randomFrom("owner", "alex", "bob", "charlie", "david")
//			.randomFromRange("created", 90L, 100L)
//			.randomFromRange("measuredValue", 30L, 121L)
//			.toBeBuilt(500);
//		
//		RandomBuilder<Measurement> measurmentsAfter = new RandomBuilder<>(Measurement.class);
//		measurmentsAfter
//			.randomFrom("sensor", hbm, "a sensor", "b sensor", "c sensor") 
//			.randomFrom("owner", "alex", "bob", "charlie", "david")
//			.randomFromRange("created", 110L, 120L)
//			.randomFromRange("measuredValue", 30L, 121L)
//			.toBeBuilt(500);
//		
//		RandomBuilder<Measurement> otherSensorsInCorrectTimeRange = new RandomBuilder<>(Measurement.class);
//		otherSensorsInCorrectTimeRange
//			.randomFrom("sensor", "a sensor", "b sensor", "c sensor") 
//			.randomFrom("owner", "alex", "bob", "charlie", "david")
//			.randomFromRange("created", 100L, 110L)
//			.randomFromRange("measuredValue", 30L, 121L)
//			.toBeBuilt(500);
//		
//		RandomBuilder<Measurement> dataOfInterestForAlex1 = new RandomBuilder<>(Measurement.class);
//		dataOfInterestForAlex1
//			.randomFrom("sensor", hbm) 
//			.randomFrom("owner", "alex")
//			.randomFromRange("created", 100L, 110L)
//			.randomFromRange("measuredValue", 60L, 61L)
//			.toBeBuilt(1);
//		
//		RandomBuilder<Measurement> dataOfInterestForAlex2 = new RandomBuilder<>(Measurement.class);
//		dataOfInterestForAlex2
//			.randomFrom("sensor", hbm) 
//			.randomFrom("owner", "alex")
//			.randomFromRange("created", 100L, 110L)
//			.randomFromRange("measuredValue", 62L, 63L)
//			.toBeBuilt(1);
//		
//		RandomBuilder<Measurement> dataOfInterestForBob = new RandomBuilder<>(Measurement.class);
//		dataOfInterestForBob
//			.randomFrom("sensor", hbm) 
//			.randomFrom("owner", "bob")
//			.randomFromRange("created", 100L, 110L)
//			.randomFromRange("measuredValue", 70L, 71L)
//			.toBeBuilt(1);
//		
//		RandomBuilder<Measurement> dataOfInterestForBob2 = new RandomBuilder<>(Measurement.class);
//		dataOfInterestForBob2
//			.randomFrom("sensor", hbm) 
//			.randomFrom("owner", "bob")
//			.randomFromRange("created", 100L, 110L)
//			.randomFromRange("measuredValue", 72L, 73L)
//			.toBeBuilt(1);
//		
//		BuildRunner<Measurement> runner = new BuildRunner<>();
//		runner.addBuilder(measurmentsBefore);
//		runner.addBuilder(measurmentsAfter);
//		runner.addBuilder(otherSensorsInCorrectTimeRange);
//		runner.addBuilder(dataOfInterestForAlex1);
//		
//		runner.addBuilder(dataOfInterestForAlex2);
//		runner.addBuilder(dataOfInterestForBob);
//		runner.addBuilder(dataOfInterestForBob2);
//		
//		List<Measurement> measurements = runner.build();
//		mongoOps.insertAll(measurements);
	}
	
	@Test
	public void findByOwnerAndSensor_shouldFindMeasurements_ForPassedOwnerAndSensorOnly() {
		createTestDataFor_findByOwnerAndSensor();
		List<Measurement> result = measurementRepository.findByOwnerAndSensor("alex", "HEART_BEAT_MONITOR");
		
		Assert.assertEquals(10, result.size());
		result.forEach(measurement -> Assert.assertEquals("alex", measurement.getOwner()));
		result.forEach(measurement -> Assert.assertEquals("HEART_BEAT_MONITOR", measurement.getSensor()));
		
	}
	
	// owners: [alex, bob, charlie, david]
	// sensors: [HEART_BEAT_MONITOR, ACCELEROMETER, COMPAS, THIRD_PARTY_SENSOR]
	// 
	// 1. measurements for owner = 'alex' and sensor HEART_BEAT_MONITOR // target data
	// 2. measurements for owner = 'alex' and sensors:  [ACCELEROMETER, COMPAS, THIRD_PARTY_SENSOR] // noise
	// 3. measurements for owner = [bob, charlie, david] and sensors [HEART_BEAT_MONITOR, ACCELEROMETER, COMPAS, THIRD_PARTY_SENSOR] // noise
	// 
	private void createTestDataFor_findByOwnerAndSensor() {
//		RandomBuilder<Measurement> heartBeatMonitorDataForAlex = new RandomBuilder<>(Measurement.class);
//		heartBeatMonitorDataForAlex
//			.randomFrom("sensor", "HEART_BEAT_MONITOR") 
//			.randomFrom("owner", "alex")
//			.randomFromRange("created", 100L, 110L)
//			.randomFromRange("measuredValue", 60L, 61L)
//			.toBeBuilt(10);
//		
//		RandomBuilder<Measurement> otherSensorDataForAlex = new RandomBuilder<>(Measurement.class);
//		otherSensorDataForAlex
//			.randomFrom("sensor", "ACCELEROMETER", "COMPAS", "THIRD_PARTY_SENSOR") 
//			.randomFrom("owner", "alex")
//			.randomFromRange("created", 110L, 120L)
//			.randomFromRange("measuredValue", 3L, 100L)
//			.toBeBuilt(100);
//		
//		RandomBuilder<Measurement> sensorDataForOtherUsers = new RandomBuilder<>(Measurement.class);
//		sensorDataForOtherUsers
//			.randomFrom("sensor", "HEART_BEAT_MONITOR", "ACCELEROMETER", "COMPAS", "THIRD_PARTY_SENSOR") 
//			.randomFrom("owner", "bob", "charlie", "david")
//			.randomFromRange("created", 150L, 200L)
//			.randomFromRange("measuredValue", 3L, 100L)
//			.toBeBuilt(100);
//		
//		BuildRunner<Measurement> runner = new BuildRunner<>();
//		runner.addBuilder(heartBeatMonitorDataForAlex);
//		runner.addBuilder(otherSensorDataForAlex);
//		runner.addBuilder(sensorDataForOtherUsers);
//		
//		List<Measurement> measurements = runner.build();
//		System.out.println("number of created measurement: " + measurements.size());
//		
//		mongoOps.insertAll(measurements);
	}

}
