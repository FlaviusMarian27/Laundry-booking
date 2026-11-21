package ro.upt.laundry.notification.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Sends notifications. For now they are only logged; a real channel (email, push, WebSocket)
 * can be added here without changing the listeners.
 */
@Component
public class Notifier {

    private static final Logger log = LoggerFactory.getLogger(Notifier.class);

    public void toUser(Long userId, String message) {
        log.info("NOTIFICATION -> user {}: {}", userId, message);
    }

    public void toAdmin(String message) {
        log.info("NOTIFICATION -> admin: {}", message);
    }

    public void toAllStudents(String message) {
        log.info("NOTIFICATION -> all students: {}", message);
    }
}
