package vn.sesgroup.hddt.controller;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import vn.sesgroup.hddt.dto.CurrentUserProfile;
import vn.sesgroup.hddt.utils.Commons;

public class AbstractController {
	public Commons commons = new Commons();
	
	public CurrentUserProfile getCurrentlyAuthenticatedPrincipal() {
		CurrentUserProfile cu = null; 
		if(null != SecurityContextHolder.getContext().getAuthentication() 
				&& SecurityContextHolder.getContext().getAuthentication().isAuthenticated()
				&& !(SecurityContextHolder.getContext().getAuthentication() instanceof AnonymousAuthenticationToken)) {
			if(SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof CurrentUserProfile)
				cu = (CurrentUserProfile) SecurityContextHolder.getContext().getAuthentication().getPrincipal();	
		}
		return cu;
	}

	public void tryToCloseStream(Closeable out) {
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e1) {
        }
    }
}
