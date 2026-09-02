package com.site.blog.my.core.controller.common;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class CommonController {

    @GetMapping("/common/kaptcha")
    public void defaultKaptcha(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws Exception {
        httpServletResponse.setHeader("Cache-Control", "no-store");
        httpServletResponse.setHeader("Pragma", "no-cache");
        httpServletResponse.setDateHeader("Expires", 0);
        httpServletResponse.setContentType("image/png");

        // 线条干扰型验证码：不涉及 copyArea 扭曲，规避 Java 8 libawt AnyIntIsomorphicCopy 自拷贝崩溃（SIGILL）
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(150, 40, 4, 30);

        // 验证码存入session
        httpServletRequest.getSession().setAttribute("verifyCode", lineCaptcha);

        // 输出图片流
        lineCaptcha.write(httpServletResponse.getOutputStream());
    }
}

