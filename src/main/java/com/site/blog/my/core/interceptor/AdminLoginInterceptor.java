package com.site.blog.my.core.interceptor;

import com.alibaba.fastjson.JSON;
import com.site.blog.my.core.util.Result;
import com.site.blog.my.core.util.ResultGenerator;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 后台系统身份验证拦截器
 * 支持 REST API 返回 401 JSON
 */
@Component
public class AdminLoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object o) throws Exception {
        String requestServletPath = request.getServletPath();

        if (requestServletPath.startsWith("/admin") && null == request.getSession().getAttribute("loginUser")) {
            // Vue 前端页面请求（HTML/JS/CSS 等）放行，由前端路由守卫处理登录跳转
            boolean isStaticAsset = requestServletPath.startsWith("/admin/assets/") 
                || requestServletPath.equals("/admin/") 
                || requestServletPath.equals("/admin")
                || requestServletPath.endsWith(".html")
                || requestServletPath.endsWith(".js")
                || requestServletPath.endsWith(".css")
                || requestServletPath.endsWith(".map")
                || requestServletPath.endsWith(".ico")
                || requestServletPath.endsWith(".png")
                || requestServletPath.endsWith(".jpg")
                || requestServletPath.endsWith(".svg")
                || requestServletPath.endsWith(".woff")
                || requestServletPath.endsWith(".woff2")
                || requestServletPath.endsWith(".ttf")
                || requestServletPath.endsWith(".eot");
            if (isStaticAsset) {
                return true;
            }
            // 所有 API 请求返回 401 JSON
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            Result result = ResultGenerator.genErrorResult(401, "未登录或登录已过期");
            response.getWriter().write(JSON.toJSONString(result));
            return false;
        } else {
            request.getSession().removeAttribute("errorMsg");
            return true;
        }
    }

    @Override
    public void postHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, ModelAndView modelAndView) throws Exception {
    }

    @Override
    public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception e) throws Exception {

    }
}
