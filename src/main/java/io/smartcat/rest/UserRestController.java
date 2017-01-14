package io.smartcat.rest;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.smartcat.data.loader.BuildRunner;
import io.smartcat.data.loader.RandomBuilder;
import io.smartcat.domain.Measurement;
import io.smartcat.domain.User;
import io.smartcat.service.UserService;

@RestController
@RequestMapping("api/users")
public class UserRestController {
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private MongoOperations mongoOps;
	
    @RequestMapping(value = "/{username}", method = RequestMethod.POST)
    public ResponseEntity createUser(@PathVariable final String username) {
    	
		createUsersWithRanger();
		createMeasurementsWithRanger();
    	
    	userService.create(username);
        return ResponseEntity.accepted().build();

    }
    
	private void createMeasurementsWithRanger() {
		
		RandomBuilder<Measurement> measurementBuilder = new RandomBuilder<>(Measurement.class);
		measurementBuilder
			.randomFrom("sensor", "Heart Beat Monitor", "Blood Preassure Monitor")
			.randomFrom("owner", "batman", "superman", "robin", "jedi", "yoda")
			.randomFromRange("created", 1000L, 15000L)
			.toBeBuilt(5000);
		
		RandomBuilder<Measurement> last50Measurements = new RandomBuilder<>(Measurement.class);
		last50Measurements
			.randomFrom("sensor", "Heart Beat Monitor", "Blood Preassure Monitor")
			.exclusiveRandomFrom("owner", "batman")
			.exclusiveRandomFromRange("created", 14000L, 15000L)
			.toBeBuilt(50);
		
		BuildRunner<Measurement> runner = new BuildRunner<>();
		runner.addBuilder(measurementBuilder);
		runner.addBuilder(last50Measurements);
		
		List<Measurement> measurements = runner.build();
		
		System.out.println("number: " + measurements.size());
		
		mongoOps.insertAll(measurements);
		
	}

	private void createUsersWithRanger() {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime year1970 = LocalDateTime.of(1970, 1, 1, 0, 0);
		LocalDateTime year1980 = LocalDateTime.of(1980, 1, 1, 0, 0);
		
		RandomBuilder<User> userBuilder = new RandomBuilder<>(User.class);
		userBuilder
			.randomFrom("username", "batman", "superman", "robin", "jedi", "yoda")
			.randomFromRange("birthdate", year1970, year1980)
			.toBeBuilt(10);
		
		RandomBuilder<User> ourUserBuilder = new RandomBuilder<>(User.class);
		userBuilder
			.exclusiveRandomFrom("username", "batman")
			.randomFromRange("birthdate", year1970, year1980)
			.toBeBuilt(1);
		
		BuildRunner<User> runner = new BuildRunner<>();
		runner.addBuilder(userBuilder);
		runner.addBuilder(ourUserBuilder);
		
		List<User> users = runner.build();
		
		mongoOps.insertAll(users);
		
		
	}

}
