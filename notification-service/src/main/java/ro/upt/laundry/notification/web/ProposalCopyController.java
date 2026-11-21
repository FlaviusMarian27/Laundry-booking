package ro.upt.laundry.notification.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.upt.laundry.notification.store.ProposalStore;
import ro.upt.laundry.notification.store.ProposalView;

import java.util.Collection;

@RestController
public class ProposalCopyController {

    private final ProposalStore store;

    public ProposalCopyController(ProposalStore store) {
        this.store = store;
    }

    @GetMapping("/proposals")
    public Collection<ProposalView> proposals() {
        return store.all();
    }
}
