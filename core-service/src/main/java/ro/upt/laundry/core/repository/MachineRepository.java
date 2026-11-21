package ro.upt.laundry.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.upt.laundry.core.model.Machine;

public interface MachineRepository extends JpaRepository<Machine, Long> {
}