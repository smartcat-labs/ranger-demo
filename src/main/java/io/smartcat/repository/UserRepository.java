package io.smartcat.repository;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.repository.CrudRepository;

import io.smartcat.domain.User;

public interface UserRepository extends CrudRepository<User, Serializable>{
	
	List<User> findByUsername(String username);

}
