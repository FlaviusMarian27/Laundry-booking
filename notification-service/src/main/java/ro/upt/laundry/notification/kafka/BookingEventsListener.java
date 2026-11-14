package ro.upt.laundry.notification.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class BookingEventsListener {

    private static final Logger log = LoggerFactory.getLogger(BookingEventsListener.class);

    @KafkaListener(topics = "bookings.events")
    public void onMessage(ConsumerRecord<String, String> record) {
        log.info("Received from {} [partition={}, offset={}] key={} value={}",
                record.topic(), record.partition(), record.offset(),
                record.key(), record.value());
    }
}