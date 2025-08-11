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
import org.springframework.http.MediaType;
import org.springframework.http.MediaType;
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
import com.supportportal.domain.HttpResponse; 
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import javax.mail.MessagingException;


@RestController
@RequestMapping("/user")
public class UserResource extends ExceptionHandling {

    
	private static final String USER_DELETE_SUCCESSFULLY = "User deleted successfully";
	private static final String EMAIL_SENT = "An email with a new password was sent to: ";
	private static final HttpStatus NO_CONTENT = HttpStatus.NO_CONTENT;
	private static final String USER_FOLDER = System.getProperty("user.home") + "/supportportal/user/";
	private static final String FORWARD_SLASH = "/";
	private static final String TEMP_PROFILE_IMAGE_BASE_URL = "https://robohash.org/";
	private static final String IMAGE_JPEG_VALUE = "image/jpeg";

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
    public ResponseEntity<User> addNewUser(
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam("userName") String userName,
            @RequestParam("email") String email,
            @RequestParam("role") String role,
            @RequestParam("isActive") String isActive,
            @RequestParam("isNonLocked") String isNonLocked,
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage
    ) throws UserNotFoundException, UsernameExistException, EmailExistException, IOException {
        User newUser = userService.addNewUse(firstName,lastName,userName,email,role,Boolean.parseBoolean(isNonLocked),Boolean.parseBoolean(isActive),profileImage);
        return new ResponseEntity<>(newUser, HttpStatus.OK);
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
	    public ResponseEntity<HttpResponse> resetPassword(@PathVariable("email") String email)
	            throws MessagingException, EmailExistException, EmailNotFoundException {
	        userService.resetPassword(email);
	        return response(HttpStatus.OK, EMAIL_SENT + email);
	    }

	      
	    @DeleteMapping("/delete/{id}")
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
	
	    @GetMapping(path = "/image/{username}/{fileName}", produces = {MediaType.IMAGE_JPEG_VALUE})
	    public byte[] getProfileImage(@PathVariable("username") String username, @PathVariable("fileName")String fileName)throws IOException  {
	    	return Files.readAllBytes(Paths.get(USER_FOLDER + username + FORWARD_SLASH + fileName));
	    }
	    
	    @GetMapping(path = "/image/profile/{username}", produces = {MediaType.IMAGE_JPEG_VALUE})
	    public byte[] getTempProfileImage(@PathVariable("username") String username) throws IOException {
	        URL url = new URL(TEMP_PROFILE_IMAGE_BASE_URL + URLEncoder.encode(username, StandardCharsets.UTF_8));
	        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
	        try (InputStream inputStream = url.openStream()) {
	            int bytesRead;
	            byte[] chunk = new byte[1024];
	            while ((bytesRead = inputStream.read(chunk)) > 0) {
	                byteArrayOutputStream.write(chunk, 0, bytesRead);
	            }
	        }
	        return byteArrayOutputStream.toByteArray();
	    }
   
    private ResponseEntity<HttpResponse> response(HttpStatus httpStatus, String message) {
        return new ResponseEntity<>(new HttpResponse(httpStatus.value(),httpStatus,
         httpStatus.getReasonPhrase().toUpperCase(),message.toUpperCase()),httpStatus);
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
