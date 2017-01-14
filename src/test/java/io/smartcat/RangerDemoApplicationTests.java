package io.smartcat;


import java.time.LocalDateTime;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.test.context.junit4.SpringRunner;

import io.smartcat.data.loader.BuildRunner;
import io.smartcat.data.loader.RandomBuilder;
import io.smartcat.domain.Measurement;
import io.smartcat.domain.User;
import io.smartcat.repository.UserRepository;
import io.smartcat.service.MeasurementService;
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
	
	// FIND THE MAXIMUM HEART BEAT MONITOR measured value for the user
	//
	// for each user we need a bunch of Heart Beat Monitor data with one value that is the max
	// for each user we need a bunch of sensor data with values that are about max for heart beat
	// for each user we need different max values in order to achieve that the maximum is only the max for the current user, not for all users
	// 
	private void createMaxHeartBeatData() {
		String hbm = "Heart Beat Monitor";
		RandomBuilder<Measurement> measurmentsHBMForAlex = new RandomBuilder<>(Measurement.class);
		measurmentsHBMForAlex
			.randomFrom("sensor", hbm) 
			.randomFrom("owner", "alex")
			.randomFromRange("created", 15000L, 15050L)
			.randomFromRange("measuredValue", 30L, 121L)
			.toBeBuilt(50);
		
		RandomBuilder<Measurement> otherSensorsForAllUsers = new RandomBuilder<>(Measurement.class); // noise
		otherSensorsForAllUsers
			.randomFrom("sensor", "a", "b", "c") 
			.randomFrom("owner", "alex", "bob", "charlie", "david")
			.randomFromRange("created", 15000L, 15050L)
			.randomFromRange("measuredValue", 0L, 3000L)
			.toBeBuilt(50000);
		
		RandomBuilder<Measurement> measurmentsHBMForBob = new RandomBuilder<>(Measurement.class);
		measurmentsHBMForBob
			.randomFrom("sensor", hbm) 
			.randomFrom("owner", "alex")
			.randomFromRange("created", 15000L, 15050L)
			.randomFromRange("measuredValue", 30L, 151L)
			.toBeBuilt(50);
		
		RandomBuilder<Measurement> measurmentsHBMForCharlie = new RandomBuilder<>(Measurement.class);
		measurmentsHBMForCharlie
			.randomFrom("sensor", hbm) 
			.randomFrom("owner", "alex")
			.randomFromRange("created", 15000L, 15050L)
			.randomFromRange("measuredValue", 30L, 200L)
			.toBeBuilt(50);
	}
	
	
	
	private void createMeasurementsManually() {
		// TODO
	}

}
