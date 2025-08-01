package com.supportportal.service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

@Service
public class LoginAttemptService {

    private static final int MAXIMUM_NUMBER_OF_ATTEMPTS = 5;
    private static final int ATTEMPT_INCREMENT = 1;
    private LoadingCache<String, Integer> loginAttemptCache;

    public LoginAttemptService() {
        super();
        loginAttemptCache = CacheBuilder.newBuilder()
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .maximumSize(100)
                .build(new CacheLoader<String, Integer>() {
                    public Integer load(String key) {
                        return 0;
                    }
                });
    }

    public void evictUserFromLoginAttemptCache(String username) {
        loginAttemptCache.invalidate(username);
    }

    public void addUserToLoginAttemptCache(String username) {
    	int attemps = 0;
    	try {

    		attemps =  ATTEMPT_INCREMENT + loginAttemptCache.get(username);
    	}catch (ExecutionException e) {
    		e.printStackTrace();//
    	}
    	loginAttemptCache.put(username, attemps);
    }

    public boolean hasExceededMaxAttempts(String username) {
    	try {
        return loginAttemptCache.get(username) >= MAXIMUM_NUMBER_OF_ATTEMPTS;
    	} catch (ExecutionException e) {
    		e.printStackTrace();
    	}
    	return false;
    }
}
