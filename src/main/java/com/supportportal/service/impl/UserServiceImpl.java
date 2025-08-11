package com.supportportal.service.impl;

import com.supportportal.constant.FileConstant;
import com.supportportal.domain.User;
import com.supportportal.domain.UserPrincipal;
import com.supportportal.enumeration.Role;
import com.supportportal.exception.domain.EmailExistException;
import com.supportportal.exception.domain.EmailNotFoundException;
import com.supportportal.exception.domain.UserNotFoundException;
import com.supportportal.exception.domain.UsernameExistException;
import com.supportportal.repository.UserRepository;
import com.supportportal.service.EmailService;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.mail.MessagingException;
import static com.supportportal.constant.FileConstant.*;
import static com.supportportal.constant.UserImplConstant.*;
import static com.supportportal.enumeration.Role.ROLE_USER;
import static com.supportportal.enumeration.Role.ROLE_USER;

@Service
@Transactional
public class UserServiceImpl implements UserService, UserDetailsService {

	private static final String USERNAME_ALREADY_EXISTS = "Username already exists";
	private static final String Email_ALREADY_EXISTS = "Email already exists";
	private static final String NO_USER_FOUNT_BY_USERNAME = "/user/image/profile/temp";
	private static final String DEFAULT_USER_IMAGE_PATH = "/user/image/profile/temp";
	private static final String EMPTY = "";
	private final Logger LOGGER = LoggerFactory.getLogger(getClass());
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private LoginAttemptService loginAttemptService;
	private EmailService emailService;

	public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
			LoginAttemptService loginAttemptService, EmailService emailService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.loginAttemptService = loginAttemptService;
		this.emailService = emailService;
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

	private void validateLoginAttempt(User user) {
		if (user.isNotLocked()) {
			if (loginAttemptService.hasExceededMaxAttempts(user.getUsername())) {
				user.setNotLocked(false);
			} else {
				user.setNotLocked(true);
			}
		} else {
			loginAttemptService.evictUserFromLoginAttemptCache(user.getUsername());
		}

	}

	@Override
	public User register(String firstName, String lastName, String username, String email)
			throws UserNotFoundException, UsernameExistException, EmailExistException {

		validateNewUsernameAndEmail("", username, email);

		User user = new User();
		user.setUserId(generateUserId());
		String password = generatePassword();
		user.setFirstName(firstName);
		user.setLastName(lastName);
		user.setUsername(username);
		user.setEmail(email);
		user.setJoinDate(new Date());
		user.setPassword(encodedPassword(password));
		user.setActive(true);
		user.setNotLocked(true);
		user.setRole(ROLE_USER.name());
		user.setAuthorities(ROLE_USER.getAuthorities());
		user.setProfileImageUrl(getTemporaryProfileImageUrl(username));
		userRepository.save(user);
		LOGGER.info("New user password: {}", password);
		try {
			emailService.sendNewPasswordEmail(firstName, password, email);
		} catch (MessagingException e) {
			LOGGER.error("Failed to send new password email: {}", e.getMessage());
			e.printStackTrace();
		}

		return user;
	}

	@Override
	public User addNewUse(String firstName, String lastName, String username, String email, String role,
			boolean isNoneLocked, boolean isActive, MultipartFile profileImage)
			throws UserNotFoundException, UsernameExistException, EmailExistException {

		validateNewUsernameAndEmail(EMPTY, username, email);

		User user = new User();
		String password = generatePassword();
		user.setUserId(generateUserId());
		user.setFirstName(firstName);
		user.setLastName(lastName);
		user.setJoinDate(new Date());
		user.setUsername(username);
		user.setEmail(email);
		user.setPassword(encodedPassword(password));
		user.setActive(isActive);
		user.setNotLocked(isNoneLocked);
		user.setRole(getRoleEnumName(role).name());
		user.setAuthorities(getRoleEnumName(role).getAuthorities());

		userRepository.save(user);

		saveProfileImage(user, profileImage);
		return user;
	}

	@Override
	public User UpdateUser(String currentUsername, String newFirstName, String newLastName, String newUsername,
			String newEmail, String role, boolean isNoneLocked, boolean isActive, MultipartFile profileImage)
			throws UserNotFoundException, UsernameExistException, EmailExistException {
		User currentUser = validateNewUsernameAndEmail(currentUsername, newUsername, newEmail);
		currentUser.setFirstName(newFirstName);
		currentUser.setLastName(newLastName);
		currentUser.setJoinDate(new Date());
		currentUser.setUsername(newUsername);
		currentUser.setEmail(newEmail);
		currentUser.setActive(isActive);
		currentUser.setNotLocked(isNoneLocked);
		currentUser.setRole(getRoleEnumName(role).name());
		currentUser.setAuthorities(getRoleEnumName(role).getAuthorities());
		userRepository.save(currentUser);
		saveProfileImage(currentUser, profileImage);
		return currentUser;
	}

	@Override
	public void resetPassword(String email) throws MessagingException, EmailNotFoundException {
		User user = userRepository.findUserByEmail(email);
		if (user == null) {
			throw new EmailNotFoundException(NO_USER_FOUND_BY_EMAIL + email);
		}
		String password = generatePassword();
		user.setPassword(encodedPassword(password));
		userRepository.save(user);

		// Ab sirf MessagingException handle karna padega
		emailService.sendNewPasswordEmail(user.getFirstName(), password, user.getEmail());
	}

	@Override
	public User updateProfileImage(String username, MultipartFile profileImage)
			throws UserNotFoundException, UsernameExistException, EmailExistException {
		User user = validateNewUsernameAndEmail(username, null, null);
		saveProfileImage(user, profileImage);
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

	@Override
	public void deleteUser(long id) {
		userRepository.deleteById(id);

	}

	private void saveProfileImage(User user, MultipartFile profileImage) {
		if (profileImage != null) {
			try {
				Path userFolder = Paths.get(FileConstant.USER_FOLDER + user.getUsername()).toAbsolutePath().normalize();
				if (!Files.exists(userFolder)) {
					Files.createDirectories(userFolder);
					LOGGER.info(FileConstant.DIRECTORY_CREATED + userFolder);
				}

				Path imagePath = userFolder.resolve(user.getUsername() + FileConstant.DOT + FileConstant.JPG_EXTENSION);
				Files.deleteIfExists(imagePath);

				Files.copy(profileImage.getInputStream(), imagePath, StandardCopyOption.REPLACE_EXISTING);

				user.setProfileImageUrl(getTemporaryProfileImageUrl(user.getUsername()));
				userRepository.save(user);

				LOGGER.info(FileConstant.FILE_SAVED_IN_FILE_SYSTEM + profileImage.getOriginalFilename());
			} catch (IOException e) {
				LOGGER.error("Error saving profile image: {}", e.getMessage());
			}
		}
	}

	private String setProfileImageUrl(String username) {
		return ServletUriComponentsBuilder.fromCurrentContextPath()
				.path(USER_IMAGE_PATH + username + FORWARD_SLASH + username + DOT + JPG_EXTENSION).toString();
	}

	private Role getRoleEnumName(String role) {
		return Role.valueOf(role.toUpperCase());
	}

	private String getTemporaryProfileImageUrl(String username) {
		return ServletUriComponentsBuilder.fromCurrentContextPath()
				.path(FileConstant.DEFAULT_USER_IMAGE_PATH + username + FileConstant.DOT + FileConstant.JPG_EXTENSION)
				.toUriString();
	}

	private String encodedPassword(String password) {
		return passwordEncoder.encode(password);
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
