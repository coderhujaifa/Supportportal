package com.supportportal.service.impl;

import com.supportportal.domain.UserPrincipal;
import java.util.Date;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.supportportal.domain.User;
import com.supportportal.repository.UserRepository;
import com.supportportal.service.UserService;

import jakarta.transaction.Transactional;


@Service
@Transactional
@Qualifier("UserDetailsService")
public class UserServiceImpl implements UserService,UserDetailsService {

	private Logger LOGGER = org.slf4j.LoggerFactory.getLogger(getClass());
	private UserRepository userRepository;
	
	@Autowired
	public UserServiceImpl(UserRepository userRepository) {
		this.userRepository =userRepository;
	}
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = userRepository.findUserByUsername(username);
		if (user == null) {
			LOGGER.error("user not found by username");
			throw new UsernameNotFoundException("User not found by username:" + username );
		} else {
			user.setLastLoginDateDisplay(user.getLastLoginDate());
			user.setLastLoginDate(new Date());
			userRepository.save(user);
			UserPrincipal userPrincipal = new UserPrincipal(user);
			LOGGER.info("Returning found user by username:" + username);
			return userPrincipal;
		}		
	}
}
