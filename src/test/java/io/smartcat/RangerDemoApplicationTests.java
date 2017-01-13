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

import io.smartcat.domain.User;
import io.smartcat.repository.UserRepository;
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

}
