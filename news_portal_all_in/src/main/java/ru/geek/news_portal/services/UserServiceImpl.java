/**
 * Бизнес логика User
 * @author
 * fix Dmitriy Ostrovskiy 19.03.2020
 * created on
 */

package ru.geek.news_portal.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.geek.news_portal.base.entities.Role;
import ru.geek.news_portal.base.entities.User;
import ru.geek.news_portal.base.repo.UserRepository;
import ru.geek.news_portal.base.repo.RoleRepository;
import ru.geek.news_portal.utils.SystemUser;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {
  private UserRepository userRepository;
  private RoleRepository roleRepository;
  private BCryptPasswordEncoder passwordEncoder;

  @Autowired
  public void setUserRepository(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Autowired
  public void setRoleRepository(RoleRepository roleRepository) {
    this.roleRepository = roleRepository;
  }

  @Autowired
  public void setPasswordEncoder(BCryptPasswordEncoder passwordEncoder) {
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  @Transactional
  public User findByUsername(String username) {
    return userRepository.findOneByUsername(username);
  }

  @Override
  @Transactional
  public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
    User user = userRepository.findOneByUsername(userName);
    if (user == null) {
      throw new UsernameNotFoundException("Invalid username or password.");
    }
    return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(),
            mapRolesToAuthorities(user.getRoles()));
  }

  private Collection<? extends GrantedAuthority> mapRolesToAuthorities(Collection<Role> roles) {
    return roles.stream().map(role -> new SimpleGrantedAuthority(role.getName())).collect(Collectors.toList());
  }

  @Override
  public boolean isUserExist(String username) {
    return userRepository.existsByUsername(username);
  }

  @Override
  @Transactional
  public User save(SystemUser systemUser) {
    User user = new User();
    if (findByUsername(systemUser.getUsername()) != null) {
      throw new RuntimeException("User with username " + systemUser.getUsername() + " is already exist");
    }
    user.setUsername(systemUser.getUsername());

    if (systemUser.getPassword() != null) {
      user.setPassword(passwordEncoder.encode(systemUser.getPassword()));
    }
    user.setFirstName(systemUser.getFirstName());
    user.setLastName(systemUser.getLastName());
    user.setEmail(systemUser.getEmail());
    user.setRoles(Arrays.asList(roleRepository.findOneByName("ROLE_READER")));
    return userRepository.save(user);
  }
}
