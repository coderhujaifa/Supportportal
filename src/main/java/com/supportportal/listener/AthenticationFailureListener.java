package com.supportportal.listener;

import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.stereotype.Component;

import com.supportportal.service.LoginAttemptService;

@Component
public class AthenticationFailureListener {


	private LoginAttemptService loginAttemptService;
	
	@Autowired
	public AthenticationFailureListener(LoginAttemptService loginAttemptService) {
		this.loginAttemptService = loginAttemptService;
	}
	
	@EventListener
	public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) throws ExecutionException {
		Object principle = event.getAuthentication().getPrincipal();
		if (principle instanceof String) {
			String username = (String) event.getAuthentication().getPrincipal();
			loginAttemptService.addUserToLoginAttemptCache(username);
		}
	}
}
