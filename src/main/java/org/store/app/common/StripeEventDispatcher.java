package org.store.app.common;

import com.stripe.model.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.store.app.service.StripeEventHandlerService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeEventDispatcher {

    private final List<StripeEventHandlerService> handlers;

    public void dispatch(Event event) {
        for (StripeEventHandlerService handler : handlers) {
            if (handler.canHandle(event.getType())) {
                handler.handle(event);
                return;
            }
        }
        log.info("No handler found for event type: {}", event.getType());
    }
}
