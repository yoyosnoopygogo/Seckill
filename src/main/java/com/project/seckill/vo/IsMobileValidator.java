package com.project.seckill.vo;

import com.project.seckill.utils.ValidatorUtil;
import com.project.seckill.validator.IsMobile;
import org.thymeleaf.util.StringUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class IsMobileValidator implements ConstraintValidator<IsMobile,String> {
    private boolean required=false;
    @Override
    public void initialize(IsMobile constraintAnnoation)
    {
        required= constraintAnnoation.required();
    }
    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        if(required)
        {
            return ValidatorUtil.isMobile(s);
        }
        else {
            if(StringUtils.isEmpty(s))
            {
                return true;
            }
            else
            {
                return ValidatorUtil.isMobile(s);
            }
        }
    }
}

