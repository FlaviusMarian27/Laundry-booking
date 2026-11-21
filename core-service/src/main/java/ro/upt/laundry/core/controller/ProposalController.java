package ro.upt.laundry.core.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ro.upt.laundry.core.dto.CreateProposalRequest;
import ro.upt.laundry.core.dto.DecideProposalRequest;
import ro.upt.laundry.core.dto.ProposalResponse;
import ro.upt.laundry.core.model.ProposalStatus;
import ro.upt.laundry.core.service.ProposalService;

import java.util.List;

@RestController
@RequestMapping("/proposals")
public class ProposalController {

    private final ProposalService proposalService;

    public ProposalController(ProposalService proposalService) {
        this.proposalService = proposalService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProposalResponse create(@Valid @RequestBody CreateProposalRequest request) {
        return proposalService.create(request);
    }

    @GetMapping
    public List<ProposalResponse> list(@RequestParam(required = false) ProposalStatus status) {
        return proposalService.list(status);
    }

    @GetMapping("/{id}")
    public ProposalResponse get(@PathVariable Long id) {
        return proposalService.get(id);
    }

    @PostMapping("/{id}/approve")
    public ProposalResponse approve(@PathVariable Long id, @Valid @RequestBody DecideProposalRequest request) {
        return proposalService.approve(id, request.decidedBy());
    }

    @PostMapping("/{id}/reject")
    public ProposalResponse reject(@PathVariable Long id, @Valid @RequestBody DecideProposalRequest request) {
        return proposalService.reject(id, request.decidedBy());
    }
}