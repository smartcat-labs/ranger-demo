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
	public void getNewestMeasurementsForUser_shouldReturnOnlyMeasurementData_forPassedUsername() {
	}
	
	@Test
	public void getNewestMeasurementsForUser_shouldReturnDataInDescendingOrder() {
	}
	
	@Test
	public void getNewestMeasurementsForUser_shouldReturn50NewestMeasurments_forPassedUsername() {
	}
	
	private void createTestData() {
		// user: "batman", created : [1500,1600) 						// expected data 
		// user: ["superman", "robin", "goblin"], created [1000, 2000)	// other users' data
		// user: "batman", created : [1000, 1500)						// old data for "batman"
	}


}
