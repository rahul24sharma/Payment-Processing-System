package com.payment.merchant.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoleAccessService {

    public void requireAny(MerchantRole... allowedRoles) {
        if (allowedRoles == null || allowedRoles.length == 0) {
            return;
        }
        MerchantRole current = currentRole();
        Set<MerchantRole> allowed = Arrays.stream(allowedRoles).collect(Collectors.toSet());
        if (!allowed.contains(current)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient role permissions");
        }
    }

    public MerchantRole currentRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return MerchantRole.ADMIN;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String value = authority.getAuthority();
            if (value != null && value.startsWith("ROLE_")) {
                return MerchantRole.from(value.substring("ROLE_".length()));
            }
        }
        return MerchantRole.ADMIN;
    }
}
