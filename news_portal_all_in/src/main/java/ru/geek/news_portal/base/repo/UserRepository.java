package ru.geek.news_portal.base.repo;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.geek.news_portal.base.entities.User;

import java.nio.channels.FileChannel;
import java.util.Optional;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {

  User findOneByUsername(String username);

  boolean existsByUsername(String username);
  Optional<User> findUserByUsername(String username);
}