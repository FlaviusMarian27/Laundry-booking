package ro.upt.laundry.notification.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.upt.laundry.notification.store.MachineStore;
import ro.upt.laundry.notification.store.MachineView;

import java.util.Collection;

@RestController
public class MachineCopyController {

    private final MachineStore store;

    public MachineCopyController(MachineStore store) {
        this.store = store;
    }

    @GetMapping("/machines")
    public Collection<MachineView> machines() {
        return store.all();
    }
}
