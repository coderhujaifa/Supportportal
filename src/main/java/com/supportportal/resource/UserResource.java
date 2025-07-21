package com.supportportal.resource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.supportportal.domain.User;

@RestController
@RequestMapping(value = "/user")
public class UserResource {

	@GetMapping("/home")
	public String showUser() {
		return "application works";
	}
}
