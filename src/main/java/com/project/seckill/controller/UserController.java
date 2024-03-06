package com.project.seckill.controller;


import com.project.seckill.pojo.User;
import com.project.seckill.vo.RespBean;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author ty
 * @since 2024-03-03
 */
@Controller
@RequestMapping("/user")
public class UserController {
    @RequestMapping("/info")
    @ResponseBody
    public RespBean info(User user)
    {
        return RespBean.success(user);
    }

}
