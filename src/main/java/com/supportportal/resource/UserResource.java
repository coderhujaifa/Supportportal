package com.supportportal.resource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.supportportal.domain.User;
import com.supportportal.exception.domain.EmailExistException;
import com.supportportal.exception.domain.ExceptionHandling;
import com.supportportal.exception.domain.UsernameExistException;

@RestController
@RequestMapping(value = "/user")
public class UserResource extends ExceptionHandling {

	@GetMapping("/home")
	public String showUser() throws UsernameExistException {
	    throw new UsernameExistException("The user was not found");
	}

}
