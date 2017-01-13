package io.smartcat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;

import com.mongodb.MongoClient;

@Configuration
public class MongoConfig {
	
	@Value("${spring.data.mongodb.host}")
	private String mongohost;
	
	@Value("${spring.data.mongodb.port}")
	private int mongoport;
	
	@Value("${spring.data.mongodb.database}")
	private String db;
	
	@Bean
	public MongoOperations mongoOps() {
		System.out.println("mongoport: " + mongoport);
		MongoOperations mongoOps = new MongoTemplate(new SimpleMongoDbFactory(new MongoClient(mongohost, mongoport), db));
		return mongoOps;
	}

}
