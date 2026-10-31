package com.example.profilessystem.server;

import com.example.profilessystem.entity.User;
import com.example.profilessystem.mapper.UserMapper;
import org.springframework.stereotype.Service;

@Service
public class LearningAccessService {
    private final UserMapper userMapper;

    public LearningAccessService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public int level(String username) {
        if (username == null || username.isBlank()) return 0;
        User user = userMapper.findByName(username);
        return user == null ? 0 : Math.max(0, Math.min(2, user.getLevel()));
    }
}
