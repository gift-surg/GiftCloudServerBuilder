package org.nrg.xnat.security.controllers;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping("/auth")

public class LoginLogoutController extends BaseController{
	@RequestMapping("login")
	public String SpringLogin() {
		return "SpringLogin";
	}
}
