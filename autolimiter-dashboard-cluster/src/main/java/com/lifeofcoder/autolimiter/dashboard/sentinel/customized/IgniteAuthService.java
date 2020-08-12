package com.lifeofcoder.autolimiter.dashboard.sentinel.customized;

import com.lifeofcoder.autolimiter.dashboard.ignite.dao.AuthDao;
import com.lifeofcoder.autolimiter.dashboard.model.IgniteAuth;
import com.lifeofcoder.autolimiter.dashboard.sentinel.dashboard.auth.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

/**
 * Ignite的集群认证
 *
 * @author xbc
 * @date 2020/7/29
 */
@Service
@Primary
public class IgniteAuthService implements AuthService<HttpServletRequest> {
    private final static Logger LOGGER = LoggerFactory.getLogger(IgniteAuthService.class);
    private final static String COOKIE_ID = "al_id";

    @Autowired
    private AuthDao authDao;

    @Override
    public AuthUser getAuthUser(HttpServletRequest request) {

        String id = getId(request);
        if (null == id) {
            LOGGER.error("Failed to get id from cookie.");
            return null;
        }

        IgniteAuth user = authDao.getUser(id);
        if (null == user) {
            LOGGER.error("Failed to get user infor from ignite for " + request.getSession().getId());
        }
        return user;
    }

    private String getId(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        //如果没有cookie，则就会为null
        if (null == cookies) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (COOKIE_ID.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    public AuthUser addAuthUser(String userName, HttpServletRequest request, HttpServletResponse response) {
        String id = generateId();
        IgniteAuth igniteAuth = new IgniteAuth(id, userName);
        authDao.addUser(igniteAuth);

        Cookie cookie = buildCookie(id);
        response.addCookie(cookie);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Succeeded to add auth user[name:" + igniteAuth.getNickName() + ", id:" + igniteAuth.key() + "].");
        }

        return igniteAuth;
    }

    private Cookie buildCookie(String id) {
        Cookie cookie = new Cookie(COOKIE_ID, id);
        cookie.setMaxAge(60 * 10); //10分钟
        cookie.setPath("/");
        //        cookie.setSecure(true);
        //        cookie.setHttpOnly(true);
        return cookie;
    }

    private String generateId() {
        return UUID.randomUUID().toString();
    }

}