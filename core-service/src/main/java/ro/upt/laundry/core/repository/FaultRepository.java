package ro.upt.laundry.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.upt.laundry.core.model.Fault;
import java.util.List;

public interface FaultRepository extends JpaRepository<Fault, Long> {
    List<Fault> findByMachineIdOrderByReportedAtDesc(Long machineId);
}