package com.supportportal.listener;

import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

import com.supportportal.domain.User;
import com.supportportal.service.LoginAttemptService;

@Component
public class AuthenticationSuccessListener {

	private LoginAttemptService loginAttemptService;

	@Autowired
	public AuthenticationSuccessListener(LoginAttemptService loginAttemptService) {
		this.loginAttemptService = loginAttemptService;
	}
	
	@EventListener
	public void onAuthenticationSuccess(AuthenticationSuccessEvent event){
		Object principle = event.getAuthentication().getPrincipal();
		if (principle instanceof User) {
			User user = (User) event.getAuthentication().getPrincipal();
			loginAttemptService.evictUserFromLoginAttemptCache(user.getUsername());
		}
	}
}
 