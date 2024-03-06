package com.project.seckill.service.impl;

import com.project.seckill.exception.GlobalException;
import com.project.seckill.pojo.User;
import com.project.seckill.mapper.UserMapper;
import com.project.seckill.service.IUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.project.seckill.utils.CookieUtil;
import com.project.seckill.utils.MD5Util;
import com.project.seckill.utils.UUIDUtil;
import com.project.seckill.utils.ValidatorUtil;
import com.project.seckill.vo.LoginVo;
import com.project.seckill.vo.RespBean;
import com.project.seckill.vo.RespBeanEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.StringUtils;
import sun.security.action.GetLongAction;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author ty
 * @since 2024-03-03
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisTemplate redisTemplate;
    @Override
    public RespBean doLogin(LoginVo loginVo, HttpServletRequest request, HttpServletResponse response) {
        String mobile =loginVo.getMobile();
        String password=loginVo.getPassword();

//        if(StringUtils.isEmpty(mobile)||StringUtils.isEmpty(password))
//        {
//            return RespBean.error(RespBeanEnum.LOGIN_ERROR);
//        }
//        if(!ValidatorUtil.isMobile(mobile)){
//            return RespBean.error(RespBeanEnum.MOBILE_ERROR);
//        }
        User user=userMapper.selectById(mobile);
        if(user==null)
        {
            throw new GlobalException(RespBeanEnum.LOGIN_ERROR);
        }
        if(!MD5Util.formPassToDBPass(password,user.getSalt()).equals(user.getPassword()))
        {
            throw new GlobalException(RespBeanEnum.LOGIN_ERROR);
        }

        String ticket= UUIDUtil.uuid();
        redisTemplate.opsForValue().set("user:"+ticket,user);
//        request.getSession().setAttribute(ticket,user);
        CookieUtil.setCookie(request,response,"userTicket",ticket);
        return RespBean.success(ticket);
    }

    @Override
    public User getUserByCookie(String userTicket, HttpServletRequest request, HttpServletResponse response) {
        if(StringUtils.isEmpty(userTicket))
        {
            return null;
        }
        User user=(User)redisTemplate.opsForValue().get("user:"+userTicket);
        if(user!=null)
        {
            CookieUtil.setCookie(request,response,"userTicket",userTicket);
        }
        return user;
    }

    @Override
    public RespBean updatePassword(String userTicket, String password, HttpServletRequest request, HttpServletResponse response) {
        User user = getUserByCookie(userTicket, request, response);
        if(user == null) {
            throw new GlobalException(RespBeanEnum.MOBILE_NOT_EXIST);
        }
        user.setPassword(MD5Util.inputPassToDBPass(password, user.getSalt()));
        int i = userMapper.updateById(user);
        if(i == 1) {
            redisTemplate.delete("user:" + userTicket);
            return RespBean.success();
        } else {
            return RespBean.error(RespBeanEnum.PASSWORD_UPDATE_FAIL);
        }
    }
}
