package com.hl.hlaicodemother.service.impl;

import com.hl.hlaicodemother.mapper.UserMapper;
import com.hl.hlaicodemother.model.entity.User;
import com.hl.hlaicodemother.service.UserService;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import org.springframework.stereotype.Service;

/**
 * 用户 服务层实现。
 *
 * @author <a href="https://github.com/HLLLG">程序员HL</a>
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>  implements UserService {

}
