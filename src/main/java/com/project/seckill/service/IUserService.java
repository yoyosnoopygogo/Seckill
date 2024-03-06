package com.project.seckill.service;

import com.project.seckill.pojo.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.project.seckill.vo.LoginVo;
import com.project.seckill.vo.RespBean;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author ty
 * @since 2024-03-03
 */
public interface IUserService extends IService<User> {


    RespBean doLogin(LoginVo loginVo, HttpServletRequest request, HttpServletResponse response);
    User getUserByCookie(String userTicket, HttpServletRequest request, HttpServletResponse response);
    RespBean updatePassword(String userTicket, String password, HttpServletRequest request, HttpServletResponse response);

}
