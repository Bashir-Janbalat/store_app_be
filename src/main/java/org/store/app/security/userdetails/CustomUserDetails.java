package org.store.app.security.userdetails;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Data
public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String email;
    private final String name;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final String phone;
    private final String countryCode;
    private final String dialCode;

    public CustomUserDetails(Long id, String email, String name, String password,
                             String phone, String countryCode, String dialCode,
                             Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.password = password;
        this.phone = phone;
        this.countryCode = countryCode;
        this.dialCode = dialCode;
        this.authorities = authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
