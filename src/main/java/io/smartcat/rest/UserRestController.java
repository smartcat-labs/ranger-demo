package io.smartcat.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.smartcat.service.UserService;

@RestController
@RequestMapping("api/users")
public class UserRestController {
	
	@Autowired
	private UserService userService;
	
    @RequestMapping(value = "/{username}", method = RequestMethod.POST)
    public ResponseEntity createUser(@PathVariable final String username) {
    	
    	userService.create(username);
        return ResponseEntity.accepted().build();

    }

}
