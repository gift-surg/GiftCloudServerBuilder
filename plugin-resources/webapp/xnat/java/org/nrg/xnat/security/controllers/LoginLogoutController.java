/*
 * org.nrg.xnat.security.controllers.LoginLogoutController
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xnat.security.controllers;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/auth")

public class LoginLogoutController extends BaseController{
	@RequestMapping("login")
	public String SpringLogin() {
		return "SpringLogin";
	}
}
