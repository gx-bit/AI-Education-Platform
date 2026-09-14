package com.edu.agent.config;
import com.edu.common.security.context.UserContext;
import com.edu.common.security.model.LoginUser;
import jakarta.servlet.*; import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
@Component
public class UserContextFilter extends OncePerRequestFilter {
  @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
    String id=req.getHeader("X-User-Id");
    if(id!=null&&!id.isBlank()) try { UserContext.setCurrentUser(LoginUser.builder().userId(Long.valueOf(id)).username(req.getHeader("X-User-Name")).role(req.getHeader("X-User-Role")).build()); } catch(NumberFormatException ignored) {}
    try { chain.doFilter(req,res); } finally { UserContext.clear(); }
  }
}
