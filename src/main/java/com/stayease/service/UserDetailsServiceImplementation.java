package com.stayease.service;

import com.stayease.model.User;
import com.stayease.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImplementation implements UserDetailsService
{
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(@NonNull String userEmail) throws UsernameNotFoundException
    {
        User user = userRepository.findByUserEmail(userEmail).orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUserEmail())
                .password(user.getUserPassword())
                .authorities(new SimpleGrantedAuthority("ROLE_" + user.getUserRole().name()))
                .disabled(user.getUserStatus() != User.UserStatus.ACTIVE)
                .build();
    }
}
