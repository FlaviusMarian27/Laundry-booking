package ro.upt.laundry.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.upt.laundry.core.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
}