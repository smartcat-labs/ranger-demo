package io.smartcat;


import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.test.context.junit4.SpringRunner;

import io.smartcat.data.loader.BuildRunner;
import io.smartcat.data.loader.RandomBuilder;
import io.smartcat.domain.Measurement;
import io.smartcat.domain.User;
import io.smartcat.repository.UserRepository;
import io.smartcat.service.AvgHeartBeatRateDTO;
import io.smartcat.service.MeasurementService;
import io.smartcat.service.ReportService;
import io.smartcat.service.UserService;

@RunWith(SpringRunner.class)
@SpringBootTest
public class RangerDemoApplicationTests {
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private MongoOperations mongoOps;
	
	@Autowired
	private MeasurementService measurementService;
	
	@Autowired
	private ReportService reportService;

	@Test
	public void contextLoads() {
		userService.create("testingson");
		
		List<User> users = userRepository.findByUsername("testingson");
		Assert.assertEquals(1, users.size());
		Assert.assertEquals("testingson", users.get(0).getUsername());
	}
	
	@Test
	public void test_insert() {
		User u = new User();
		u.setUsername("milan");
		
		mongoOps.insert(u);
		
		BasicQuery query = new BasicQuery("{username: 'milan' }");
		User foundUser = mongoOps.findOne(query, User.class);
		
		Assert.assertEquals("milan", foundUser.getUsername());
	}
	
	@Test
	public void should_return_only_measurements_for_selected_user() {
		createMeasurementsWithRanger();
		List<Measurement> result = measurementService.getNewestMeasurementsForUser("batman", 50);
		Assert.assertEquals(50, result.size());
		result.forEach(measurement -> Assert.assertEquals("batman", measurement.getOwner()));
	}
	
	@Test
	public void should_return_data_in_descending_order() {
		createMeasurementsWithRanger();
		List<Measurement> result = measurementService.getNewestMeasurementsForUser("batman", 50);
		
		Assert.assertEquals(50, result.size()); // need this in order to ensure that non empty list
		
		long previousTimestamp = result.get(0).getCreated();
		for (Measurement m : result) {
			Assert.assertTrue(m.getCreated() <= previousTimestamp);
			previousTimestamp = m.getCreated();
		}
	}
	
	@Test
	public void should_return_the_newest_measurements() {
		createMeasurementsWithRanger();
		List<Measurement> result = measurementService.getNewestMeasurementsForUser("batman", 50);
		// we "marked" the newest data with "Third Party Sensor", in order to ensure that they are the newest
		// I could have also checked that timestamp is between 15000 and 15050, it's the same thing.
		result.forEach(measurement -> Assert.assertEquals("Third Party Sensor", measurement.getSensor()));
	}

	// this creates test data for whole bunch of test cases:
	// 1. batman's and only batman's data are returned (there are other users as well, test checks that all the data returned is batman's)
	// 2. data is returned in descending order -> 
	// 3. data returned is the newest data for the user ( there are other data as well, but we "marked" the newest data in a way, in this case we set that the newest data is from "Third Party Sensor"
	// Q: is there better way to manually create test data? How did you solve this problem.
	private void createMeasurementsWithRanger() {
		RandomBuilder<Measurement> newestMeasurementsForBatman = new RandomBuilder<>(Measurement.class);
		newestMeasurementsForBatman
			.randomFrom("sensor", "Third Party Sensor") // mark the newest results with "Third Party Sensor"
			.randomFrom("owner", "batman")
			.randomFromRange("created", 15000L, 15050L) // this is latest timestamp
			.toBeBuilt(50);
		
		RandomBuilder<Measurement> overlappingMeasurementTimestampOtherOwners = new RandomBuilder<>(Measurement.class);
		overlappingMeasurementTimestampOtherOwners
			.randomFrom("sensor", "Heart Beat Monitor", "Blood Preassure Monitor")
			.randomFrom("owner", "superman", "robin", "jedi", "yoda") // note: no "batman"
			.randomFromRange("created", 14000L, 16000L)
			.toBeBuilt(1000);
		
		RandomBuilder<Measurement> olderDataForBatman = new RandomBuilder<>(Measurement.class);
		olderDataForBatman
			.randomFrom("sensor", "Heart Beat Monitor", "Blood Preassure Monitor")
			.randomFrom("owner", "batman")
			.randomFromRange("created", 1000L, 15000L)
			.toBeBuilt(1000);
		
		BuildRunner<Measurement> runner = new BuildRunner<>();
		runner.addBuilder(olderDataForBatman);
		runner.addBuilder(overlappingMeasurementTimestampOtherOwners);
		runner.addBuilder(newestMeasurementsForBatman);
		
		List<Measurement> measurements = runner.build();
		
		System.out.println("number of created measurements: " + measurements.size());
		
		mongoOps.insertAll(measurements);
		
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
		String hbm = "Heart Beat Monitor";
		RandomBuilder<Measurement> measurmentsBefore = new RandomBuilder<>(Measurement.class);
		measurmentsBefore
			.randomFrom("sensor", hbm, "a sensor", "b sensor", "c sensor") 
			.randomFrom("owner", "alex", "bob", "charlie", "david")
			.randomFromRange("created", 90L, 100L)
			.randomFromRange("measuredValue", 30L, 121L)
			.toBeBuilt(500);
		
		RandomBuilder<Measurement> measurmentsAfter = new RandomBuilder<>(Measurement.class);
		measurmentsAfter
			.randomFrom("sensor", hbm, "a sensor", "b sensor", "c sensor") 
			.randomFrom("owner", "alex", "bob", "charlie", "david")
			.randomFromRange("created", 110L, 120L)
			.randomFromRange("measuredValue", 30L, 121L)
			.toBeBuilt(500);
		
		RandomBuilder<Measurement> otherSensorsInCorrectTimeRange = new RandomBuilder<>(Measurement.class);
		otherSensorsInCorrectTimeRange
			.randomFrom("sensor", "a sensor", "b sensor", "c sensor") 
			.randomFrom("owner", "alex", "bob", "charlie", "david")
			.randomFromRange("created", 100L, 110L)
			.randomFromRange("measuredValue", 30L, 121L)
			.toBeBuilt(500);
		
		RandomBuilder<Measurement> dataOfInterestForAlex1 = new RandomBuilder<>(Measurement.class);
		dataOfInterestForAlex1
			.randomFrom("sensor", hbm) 
			.randomFrom("owner", "alex")
			.randomFromRange("created", 100L, 110L)
			.randomFromRange("measuredValue", 60L, 61L)
			.toBeBuilt(1);
		
		RandomBuilder<Measurement> dataOfInterestForAlex2 = new RandomBuilder<>(Measurement.class);
		dataOfInterestForAlex2
			.randomFrom("sensor", hbm) 
			.randomFrom("owner", "alex")
			.randomFromRange("created", 100L, 110L)
			.randomFromRange("measuredValue", 62L, 63L)
			.toBeBuilt(1);
		
		RandomBuilder<Measurement> dataOfInterestForBob = new RandomBuilder<>(Measurement.class);
		dataOfInterestForBob
			.randomFrom("sensor", hbm) 
			.randomFrom("owner", "bob")
			.randomFromRange("created", 100L, 110L)
			.randomFromRange("measuredValue", 70L, 71L)
			.toBeBuilt(1);
		
		RandomBuilder<Measurement> dataOfInterestForBob2 = new RandomBuilder<>(Measurement.class);
		dataOfInterestForBob2
			.randomFrom("sensor", hbm) 
			.randomFrom("owner", "bob")
			.randomFromRange("created", 100L, 110L)
			.randomFromRange("measuredValue", 72L, 73L)
			.toBeBuilt(1);
		
		BuildRunner<Measurement> runner = new BuildRunner<>();
		runner.addBuilder(measurmentsBefore);
		runner.addBuilder(measurmentsAfter);
		runner.addBuilder(otherSensorsInCorrectTimeRange);
		runner.addBuilder(dataOfInterestForAlex1);
		
		runner.addBuilder(dataOfInterestForAlex2);
		runner.addBuilder(dataOfInterestForBob);
		runner.addBuilder(dataOfInterestForBob2);
		
		List<Measurement> measurements = runner.build();
		mongoOps.insertAll(measurements);
	}
	
	
	
	private void createMeasurementsManually() {
		// TODO
	}

}
