package com.supportportal.service;

import com.supportportal.domain.User;
import com.supportportal.exception.domain.EmailExistException;
import com.supportportal.exception.domain.EmailNotFoundException;
import com.supportportal.exception.domain.UserNotFoundException;
import com.supportportal.exception.domain.UsernameExistException;

import java.util.List;
import javax.mail.MessagingException;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
	User register(String firstName, String lastName, String username, String email)
			throws UserNotFoundException, UsernameExistException, EmailExistException;

	List<User> getUsers();

	User findUserByUsername(String username);

	User findUserByEmail(String email);

	void deleteUser(long id);

	User addNewUse(String firstName, String lastName, String username, String email, String role,boolean isNonLocked, boolean isActive, MultipartFile profileImage)throws UserNotFoundException, UsernameExistException, EmailExistException;

	User UpdateUser(String currentUsername, String newFirstName, String newLastName, String newUsername,String newEmail, String role, boolean isNonLocked, boolean isActive, MultipartFile profileImage)throws UserNotFoundException, UsernameExistException, EmailExistException;

	void resetPassword(String email) throws MessagingException, EmailNotFoundException;


	User updateProfileImage(String username, MultipartFile profileImage)throws UserNotFoundException, UsernameExistException, EmailExistException;
}
