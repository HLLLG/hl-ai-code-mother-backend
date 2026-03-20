package com.hl.hlaicodemother.aop;

import com.hl.hlaicodemother.annotation.AuthCheck;
import com.hl.hlaicodemother.exception.BusinessException;
import com.hl.hlaicodemother.exception.ErrorCode;
import com.hl.hlaicodemother.exception.ThrowUtils;
import com.hl.hlaicodemother.model.entity.User;
import com.hl.hlaicodemother.model.enums.UserRoleEnum;
import com.hl.hlaicodemother.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    @Around("@annotation(authCheck)")
    public Object doIntercept(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        // 必要的权限
        String mustRole = authCheck.mustRole();
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
        // 获取当前登录用户的权限信息
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        // 不需要权限，放行
        if (mustRoleEnum == null) {
            joinPoint.proceed();
        }
        // 若用户权限为空，则无权限
        ThrowUtils.throwIf(userRoleEnum == null, ErrorCode.NOT_AUTH_ERROR);
        // 若需要管理员权限，但用户权限不是管理员，则无权限
        if (mustRoleEnum.equals(UserRoleEnum.ADMIN) && !userRoleEnum.equals(UserRoleEnum.ADMIN)) {
            throw new BusinessException(ErrorCode.NOT_AUTH_ERROR);
        }
        return joinPoint.proceed();
    }
}
