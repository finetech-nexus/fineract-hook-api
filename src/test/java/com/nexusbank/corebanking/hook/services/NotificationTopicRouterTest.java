package com.nexusbank.corebanking.hook.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationTopicRouterTest {

    private final NotificationTopicRouter router = new NotificationTopicRouter(
            "notification.transaction",
            "notification.payment-confirmation",
            "notification.account",
            "notification.card");

    @Test
    void routesMoneyMovementToTransactionTopic() {
        NotificationDestination destination = router.route("SAVINGSACCOUNT", "DEPOSIT");
        assertEquals("TRANSACTION", destination.notificationType());
        assertEquals("notification.transaction", destination.topic());
    }

    @Test
    void routesConfirmationToItsOwnTopic() {
        NotificationDestination destination = router.route("ONLINEPAYMENT", "CONFIRM");
        assertEquals("PAYMENT_CONFIRMATION", destination.notificationType());
        assertEquals("notification.payment-confirmation", destination.topic());
    }

    @Test
    void routesClientLifecycleToAccountTopic() {
        NotificationDestination destination = router.route("CLIENT", "ACTIVATE");
        assertEquals("ACCOUNT", destination.notificationType());
        assertEquals("notification.account", destination.topic());
    }

    @Test
    void holdReservationIsNotACustomerNotification() {
        assertTrue(NotificationTopicRouter.isHoldReservation("PREPARE"));
        assertTrue(NotificationTopicRouter.isHoldReservation("HOLDAMOUNT"));
        assertFalse(NotificationTopicRouter.isHoldReservation("CREATE"));
        assertFalse(NotificationTopicRouter.isHoldReservation("WITHDRAWAL"));
    }

    @Test
    void routesCardEventsToCardTopic() {
        NotificationDestination destination = router.route("CARD", "BLOCK");
        assertEquals("CARD", destination.notificationType());
        assertEquals("notification.card", destination.topic());
    }
}
