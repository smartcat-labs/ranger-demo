package io.smartcat.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.smartcat.domain.User;
import io.smartcat.repository.UserRepository;

@Service
public class UserService {
	
	@Autowired
	private UserRepository userRepository;
	
	public void create(String username) {
		User user = new User();
		user.setUsername(username);
		userRepository.save(user);
	}

}
