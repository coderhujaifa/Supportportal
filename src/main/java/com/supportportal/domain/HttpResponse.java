package com.supportportal.domain;

import org.springframework.http.HttpStatus;

import lombok.Data;

@Data 
public class HttpResponse {

	private int httpStatusCode; // 200, 201, 400, 500
	private HttpStatus httpStatus;
	private String reason; 
	private String message;
}

