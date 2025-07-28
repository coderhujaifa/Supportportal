package com.supportportal.resource;


import com.supportportal.domain.User;
import com.supportportal.domain.UserPrincipal;
import com.supportportal.exception.domain.EmailExistException;
import com.supportportal.exception.domain.EmailNotFoundException;
import com.supportportal.exception.domain.ExceptionHandling;
import com.supportportal.exception.domain.UserNotFoundException;
import com.supportportal.exception.domain.UsernameExistException;
import com.supportportal.service.UserService;
import com.supportportal.utility.JWTTokenProvider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static org.springframework.http.HttpStatus.OK;

import static com.supportportal.constant.SecurityConstant.JWT_TOKEN_HEADER;
import static org.springframework.http.HttpStatus.OK;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.List;

import javax.mail.MessagingException;


@RestController
@RequestMapping("/user")
public class UserResource extends ExceptionHandling {

    
	private static final String USER_DELETE_SUCCESSFULLY = null;
	private static final String EMAIL_SENT = null;
	private static final HttpStatus NO_CONTENT = null;
	private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JWTTokenProvider jwtTokenProvider;

    @Autowired
    public UserResource(UserService userService, AuthenticationManager authenticationManager, JWTTokenProvider jwtTokenProvider) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<User> login(@RequestBody User user) {
        authenticate(user.getUsername(), user.getPassword());
        User loginUser = userService.findUserByUsername(user.getUsername());
        UserPrincipal userPrincipal = new UserPrincipal(loginUser);
        HttpHeaders jwtHeaders = getJwtHeader(userPrincipal);
        return new ResponseEntity<>(loginUser, jwtHeaders, OK);
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody User user) throws UsernameExistException, UserNotFoundException, EmailExistException {
        User newUser = userService.register(user.getFirstName(), user.getLastName(), user.getUsername(), user.getEmail());
        return new ResponseEntity<>(newUser, OK);
    }
    
    @PostMapping("/add")
    public ResponseEntity <User> addNewUser(@RequestParam("firstName") String firstName,
        @RequestParam("lastName") String lastName,
        @RequestParam("userName") String userName,
        @RequestParam("email") String email,
        @RequestParam("role") String role,
        @RequestParam("isActive") String isActive,
        @RequestParam("isNonLocked") String isNonLocked,
        @RequestParam(value ="profileImage", required = false) MultipartFile profileImage)throws UserNotFoundException, UsernameExistException, EmailExistException, IOException {
        User newUser = userService.addNewUse(firstName, lastName, userName, email, role,Boolean.parseBoolean(isNonLocked),Boolean.parseBoolean(isActive),profileImage);
        return new ResponseEntity<>(newUser, OK);
    }

    @PostMapping("/update")
    public ResponseEntity <User> update(@RequestParam("currentUsername") String currentUsername,
    	@RequestParam("firstName") String firstName,
    	@RequestParam("lastName") String lastName,
        @RequestParam("userName") String userName,
        @RequestParam("email") String email,
        @RequestParam("role") String role,
        @RequestParam("isActive") String isActive,
        @RequestParam("isNonLocked") String isNonLocked,
        @RequestParam(value ="profileImage", required = false) MultipartFile profileImage)throws UserNotFoundException, UsernameExistException, EmailExistException, IOException {
        User updateUser = userService.UpdateUser(currentUsername, firstName, lastName, userName, email, role,Boolean.parseBoolean(isNonLocked),Boolean.parseBoolean(isActive),profileImage);
        return new ResponseEntity<>(updateUser, OK);
    }

    @GetMapping("/find/{username}")
    public ResponseEntity<User> getUser(@PathVariable("username") String username){
    	User user = userService.findUserByUsername(username);
    	return new ResponseEntity(user, OK);
    }
    
    @GetMapping("/list")
    public ResponseEntity<List<User>> getAllUser(){
    	List <User> user = userService.getUsers();
    	return new ResponseEntity(user, OK);
    }
    
    @GetMapping("/resetPassword/{email}")
    public ResponseEntity<HttpResponse> resetPassword(@PathVariable("email") String email) throws MessagingException,EmailExistException, EmailNotFoundException{
    	userService.resetPassword(email);
    	return response(OK, EMAIL_SENT + email);
    }
      
    @GetMapping("/delete/{id}")
    @PreAuthorize("hasAnyAuthority('user:delete')")
    public ResponseEntity<HttpResponse> deleteUser(@PathVariable("id") long id){
    	userService.deleteUser(id);
    	return response(NO_CONTENT, USER_DELETE_SUCCESSFULLY);
    }
    
    @PostMapping("/updateProfileIamge")
    public ResponseEntity <User> updateProfileIamge(@RequestParam("username") String username,
        @RequestParam(value ="profileImage", required = false) MultipartFile profileImage)throws UserNotFoundException, UsernameExistException, EmailExistException, IOException {
    	User user = userService.updateProfileImage(username, profileImage);
        return new ResponseEntity<>(user, OK);
    }

    private ResponseEntity<HttpResponse> response(HttpStatus ok, String string) {
		return new ResponseEntity<>; 
	}

	private HttpHeaders getJwtHeader(UserPrincipal user) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(JWT_TOKEN_HEADER, jwtTokenProvider.generateJwtToken(user));
        return headers;
    }

    private void authenticate(String username, String password) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
    }
}
