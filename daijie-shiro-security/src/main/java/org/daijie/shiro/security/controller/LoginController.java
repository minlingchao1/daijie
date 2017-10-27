package org.daijie.shiro.security.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.daijie.core.controller.ApiController;
import org.daijie.core.controller.enums.ResultCode;
import org.daijie.core.factory.specific.ModelResultInitialFactory.Result;
import org.daijie.core.httpResult.ApiResult;
import org.daijie.core.httpResult.ModelResult;
import org.daijie.core.util.encrypt.RSAUtil;
import org.daijie.core.util.http.CookieUtil;
import org.daijie.core.util.http.HttpConversationUtil;
import org.daijie.mybatis.model.User;
import org.daijie.shiro.authc.AuthorizationToken;
import org.daijie.shiro.authc.ShiroConstants;
import org.daijie.shiro.authc.UserToken;
import org.daijie.shiro.security.cloud.UserCloud;
import org.daijie.shiro.session.ShiroRedisSession.Redis;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(description="用户登录")
public class LoginController extends ApiController<UserCloud, Exception> {

	@ApiOperation(notes = "登录", value = "登录")
	@RequestMapping(value = "/login", method = RequestMethod.POST)
	public ModelResult<Object> login(@RequestParam String username, @RequestParam String password) throws Exception{
		//公钥传给客户端
		String publicKey = (String) Redis.getAttribute(ShiroConstants.RSA_PUBLIC_KEY + Redis.getSession().getId());
		//客户端调用登录接口时进行公钥加密后传参
		password = RSAUtil.encryptByPubKey(password);
		
		UserToken userToken = new UserToken();
		User user = service.getUser(username);
		String salt = user.getSalt();
		RSAUtil.set(publicKey, null);
		userToken.setAuthc(user);
		AuthorizationToken token = new AuthorizationToken(username, 
				user.getPassword(), 
				password, salt, "user", userToken);
		token.setRememberMe(true);  
	    Subject subject = SecurityUtils.getSubject();
	    subject.login(token);
	    subject.isAuthenticated();
		return Result.build("登录成功", ApiResult.SUCCESS, ResultCode.CODE_200);
	}
	
	@ApiOperation(notes = "退出", value = "退出")
	@RequestMapping(value = "/logout", method = RequestMethod.POST)
	public ModelResult<Object> logout(){
		Session session = Redis.getSession();
		Redis.deleteSession();
		CookieUtil.set(HttpConversationUtil.TOKEN_NAME, session.getId().toString(), 0);
		return Result.build("退出成功", ApiResult.SUCCESS, ResultCode.CODE_200);
	}
	
	@ApiOperation(notes = "获取登录用户", value = "获取登录用户")
	@RequestMapping(value = "/getLoginUser", method = RequestMethod.POST)
	public ModelResult<User> getUser(){
		return Result.build((User)Redis.getAttribute("user"), "退出成功", ApiResult.SUCCESS, ResultCode.CODE_200);
	}
}
