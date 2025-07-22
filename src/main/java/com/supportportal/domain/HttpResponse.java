package com.supportportal.domain;

import java.util.Date;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data 
@AllArgsConstructor
public class HttpResponse {
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM-dd-yyyy hh:mm:ss")
	private Date timeStamp;
	private int httpStatusCode; // 200, 201, 400, 500
	private HttpStatus httpStatus;
	private String reason; 
	private String message;
}

