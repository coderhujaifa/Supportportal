package com.supportportal.service.impl;

import com.supportportal.domain.User;
import com.supportportal.domain.UserPrincipal;
import com.supportportal.enumeration.Role;
import com.supportportal.exception.domain.EmailExistException;
import com.supportportal.exception.domain.UserNotFoundException;
import com.supportportal.exception.domain.UsernameExistException;
import com.supportportal.repository.UserRepository;
import com.supportportal.service.LoginAttemptService;
import com.supportportal.service.UserService;

import jakarta.transaction.Transactional;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.supportportal.enumeration.Role.ROLE_USER;

@Service
@Transactional
public class UserServiceImpl implements UserService, UserDetailsService {

    private static final String USERNAME_ALREADY_EXISTS = "Username already exists";
	private static final String Email_ALREADY_EXISTS = "Email already exists";
	private static final String NO_USER_FOUNT_BY_USERNAME = "/user/image/profile/temp";
	private static final String DEFAULT_USER_IMAGE_PATH = "/user/image/profile/temp";
	private final Logger LOGGER = LoggerFactory.getLogger(getClass());
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private  LoginAttemptService loginAttemptService;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,LoginAttemptService loginAttemptService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findUserByUsername(username);
        if (user == null) {
            LOGGER.error(NO_USER_FOUNT_BY_USERNAME + username);
            throw new UsernameNotFoundException(NO_USER_FOUNT_BY_USERNAME + username);
        }
        validateLoginAttempt(user);
        user.setLastLoginDateDisplay(user.getLastLoginDate());
        user.setLastLoginDate(new Date());
        userRepository.save(user);
        return new UserPrincipal(user);
    }

    private void validateLoginAttempt(User user)  {
    	if(user.isNotLocked()) {
    		if(loginAttemptService.hasExceededMaxAttempts(user.getUsername())){
    			user.setNotLocked(false);
    		}else {
    			user.setNotLocked(true);
    		}
    	} else {
    		loginAttemptService.evictUserFromLoginAttemptCache(user.getUsername());
    	}
		
	}

	@Override
    public User register(String firstName, String lastName, String username, String email) throws UserNotFoundException, UsernameExistException, EmailExistException {

        validateNewUsernameAndEmail("", username, email);

        String password = generatePassword();
        String encodedPassword = passwordEncoder.encode(password);

        User user = new User();
        user.setUserId(generateUserId());
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUsername(username);
        user.setEmail(email);
        user.setJoinDate(new Date());
        user.setPassword(encodedPassword);
        user.setActive(true);
        user.setNotLocked(true);
        user.setRole(ROLE_USER.name());
        user.setAuthorities(ROLE_USER.getAuthorities());
        user.setProfileImageUrl(getTemporaryProfileImageUrl());

        userRepository.save(user);
        LOGGER.info("New user password: {}", password);
        return user;
    }

    
    @Override
    public List<User> getUsers() {
        return userRepository.findAll();
    }

    @Override
    public User findUserByUsername(String username) {
        return userRepository.findUserByUsername(username);
    }

    @Override
    public User findUserByEmail(String email) {
        return userRepository.findUserByEmail(email);
    }

    private String getTemporaryProfileImageUrl() {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(DEFAULT_USER_IMAGE_PATH)
                .toUriString();
    }

    private String generatePassword() {
        return RandomStringUtils.randomAlphanumeric(10);
    }

    private String generateUserId() {
        return RandomStringUtils.randomNumeric(10);
    }

    private User validateNewUsernameAndEmail(String currentUsername, String newUsername, String newEmail)
            throws UserNotFoundException, UsernameExistException, EmailExistException {
        if (StringUtils.isNotBlank(currentUsername)) {
            User currentUser = findUserByUsername(currentUsername);
            if (currentUser == null) {
                throw new UserNotFoundException("No user found by username: " + currentUsername);
            }

            User userByUsername = findUserByUsername(newUsername);
            if (userByUsername != null && !currentUser.getId().equals(userByUsername.getId())) {
                throw new UsernameExistException(USERNAME_ALREADY_EXISTS);
            }

            User userByEmail = findUserByEmail(newEmail);
            if (userByEmail != null && !currentUser.getId().equals(userByEmail.getId())) {
                throw new EmailExistException(Email_ALREADY_EXISTS);
            }

            return currentUser;
        } else {
            if (findUserByUsername(newUsername) != null) {
                throw new UsernameExistException(USERNAME_ALREADY_EXISTS);
            }

            if (findUserByEmail(newEmail) != null) {
                throw new EmailExistException(Email_ALREADY_EXISTS);
            }

            return null;
        }
    }
}
