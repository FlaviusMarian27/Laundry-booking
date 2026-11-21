package ro.upt.laundry.core.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.upt.laundry.core.model.Proposal;
import ro.upt.laundry.core.model.ProposalStatus;

import java.util.List;
import java.util.Optional;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {

    List<Proposal> findAllByOrderByCreatedAtDesc();

    List<Proposal> findByStatusOrderByCreatedAtDesc(ProposalStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Proposal p where p.id = :id")
    Optional<Proposal> findForUpdate(@Param("id") Long id);
}