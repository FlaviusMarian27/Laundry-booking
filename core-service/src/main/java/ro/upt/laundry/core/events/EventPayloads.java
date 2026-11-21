package ro.upt.laundry.core.events;

import ro.upt.laundry.core.model.ProposalKind;
import ro.upt.laundry.core.model.ProposalSeverity;
import ro.upt.laundry.core.model.ProposalStatus;

import java.time.Instant;

/** Payloads of the events from docs/events.md. Field names must match the contract. */
public final class EventPayloads {

    private EventPayloads() {
    }

    // bookings.events
    public record BookingCreated(Long bookingId, Long userId, Long machineId, Instant startTime, Instant endTime) {
    }

    public record BookingCancelled(Long bookingId, Long userId, Long machineId, String reason) {
    }

    public record BookingStarted(Long bookingId, Long userId, Long machineId, Instant startedAt) {
    }

    public record BookingNoShow(Long bookingId, Long userId, Long machineId, Instant startTime) {
    }

    // devices.events
    public record FaultReported(Long faultId, Long machineId, Long reportedBy, String description, Instant reportedAt) {
    }

    public record MaintenanceOpened(Long machineId, Long proposalId, Instant openedAt) {
    }

    public record MaintenanceClosed(Long machineId, Instant closedAt) {
    }

    // feedback.events
    public record FeedbackSubmitted(Long feedbackId, Long userId, String message, Instant submittedAt) {
    }

    // proposals.events
    public record ProposalCreated(Long proposalId, ProposalKind kind, Long targetId, String reason,
                                  ProposalSeverity severity, ProposalStatus status) {
    }

    public record ProposalDecided(Long proposalId, Long decidedBy, Instant decidedAt) {
    }
}
