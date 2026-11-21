package ro.upt.laundry.notification.store;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Copy of machine state built from devices.events.
 * Written only by DeviceEventsListener's consumer thread; read by HTTP threads.
 * Every update is a single atomic map operation that replaces an immutable record.
 */
@Component
public class MachineStore {

    private final Map<Long, MachineView> machines = new ConcurrentHashMap<>();
    private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();

    public boolean isProcessed(String eventId) {
        return processedEventIds.contains(eventId);
    }

    public void markProcessed(String eventId) {
        processedEventIds.add(eventId);
    }

    public void recordFault(Long machineId, Instant at) {
        machines.compute(machineId, (id, m) -> (m == null ? MachineView.unknown(id) : m).withFault(at));
    }

    public void setStatus(Long machineId, String status) {
        machines.compute(machineId, (id, m) -> (m == null ? MachineView.unknown(id) : m).withStatus(status));
    }

    public Collection<MachineView> all() {
        return List.copyOf(machines.values());
    }
}
