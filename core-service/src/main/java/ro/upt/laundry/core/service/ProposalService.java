package ro.upt.laundry.core.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.upt.laundry.core.dto.CreateProposalRequest;
import ro.upt.laundry.core.dto.ProposalResponse;
import ro.upt.laundry.core.exception.ApiException;
import ro.upt.laundry.core.events.EventPayloads;
import ro.upt.laundry.core.events.EventRecorder;
import ro.upt.laundry.core.events.Topics;
import ro.upt.laundry.core.model.*;
import ro.upt.laundry.core.repository.MachineRepository;
import ro.upt.laundry.core.repository.ProposalRepository;
import ro.upt.laundry.core.repository.UserRepository;

import java.time.Instant;
import java.util.List;

@Service
public class ProposalService {

    private final ProposalRepository proposals;
    private final MachineRepository machines;
    private final UserRepository users;
    private final BookingService bookingService;
    private final EventRecorder events;

    public ProposalService(ProposalRepository proposals, MachineRepository machines,
                           UserRepository users, BookingService bookingService, EventRecorder events) {
        this.proposals = proposals;
        this.machines = machines;
        this.users = users;
        this.bookingService = bookingService;
        this.events = events;
    }

    @Transactional
    public ProposalResponse create(CreateProposalRequest req) {
        ProposalSeverity severity = req.severity();
        if (req.kind() == ProposalKind.MAINTENANCE) {
            if (severity == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "severity is required for MAINTENANCE");
            }
            Machine machine = machines.findById(req.targetId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Machine not found: " + req.targetId()));
            if (machine.getStatus() != MachineStatus.AVAILABLE) {
                throw new ApiException(HttpStatus.CONFLICT, "Machine is already under maintenance");
            }
        } else {
            if (!users.existsById(req.targetId())) {
                throw new ApiException(HttpStatus.NOT_FOUND, "User not found: " + req.targetId());
            }
            if (severity == null) {
                severity = ProposalSeverity.MEDIUM;
            }
        }
        try {
            Proposal saved = proposals.saveAndFlush(new Proposal(
                    req.kind(), req.targetId(), req.reason().strip(), severity, Instant.now()));
            events.record(Topics.PROPOSALS, saved.getId(), "proposal.created",
                    new EventPayloads.ProposalCreated(saved.getId(), saved.getKind(), saved.getTargetId(),
                            saved.getReason(), saved.getSeverity(), saved.getStatus()));
            return ProposalResponse.from(saved);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, "A PENDING proposal already exists for this target");
        }
    }

    @Transactional(readOnly = true)
    public List<ProposalResponse> list(ProposalStatus status) {
        List<Proposal> result = (status == null)
                ? proposals.findAllByOrderByCreatedAtDesc()
                : proposals.findByStatusOrderByCreatedAtDesc(status);
        return result.stream().map(ProposalResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ProposalResponse get(Long id) {
        return ProposalResponse.from(proposals.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Proposal not found: " + id)));
    }

    @Transactional
    public ProposalResponse approve(Long id, Long adminId) {
        requireAdmin(adminId);
        Proposal p = lockPending(id);
        Instant now = Instant.now();

        if (p.getKind() == ProposalKind.MAINTENANCE) {
            Machine machine = machines.findById(p.getTargetId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Machine not found: " + p.getTargetId()));
            if (machine.getStatus() != MachineStatus.AVAILABLE) {
                throw new ApiException(HttpStatus.CONFLICT, "Machine is already under maintenance");
            }
            machine.markMaintenance();
            p.decide(ProposalStatus.APPROVED, adminId, now);
            // Event order (docs/events.md): proposal.approved, maintenance.opened, then booking.cancelled per booking
            recordDecided(p, "proposal.approved");
            events.record(Topics.DEVICES, machine.getId(), "maintenance.opened",
                    new EventPayloads.MaintenanceOpened(machine.getId(), p.getId(), now));
            bookingService.cancelUpcomingForMachine(machine.getId());
        } else {
            p.decide(ProposalStatus.APPROVED, adminId, now);
            recordDecided(p, "proposal.approved");
        }
        return ProposalResponse.from(p);
    }

    @Transactional
    public ProposalResponse reject(Long id, Long adminId) {
        requireAdmin(adminId);
        Proposal p = lockPending(id);
        p.decide(ProposalStatus.REJECTED, adminId, Instant.now());
        recordDecided(p, "proposal.rejected");
        return ProposalResponse.from(p);
    }

    private void recordDecided(Proposal p, String type) {
        events.record(Topics.PROPOSALS, p.getId(), type,
                new EventPayloads.ProposalDecided(p.getId(), p.getDecidedBy(), p.getDecidedAt()));
    }

    private Proposal lockPending(Long id) {
        Proposal p = proposals.findForUpdate(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Proposal not found: " + id));
        if (p.getStatus() != ProposalStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "Proposal already " + p.getStatus());
        }
        return p;
    }

    private void requireAdmin(Long userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found: " + userId));
        if (user.getRole() != UserRole.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only an ADMIN can decide proposals");
        }
    }
}