package edu.cmu.cs214.scheduling.workflow;

import edu.cmu.cs214.scheduling.domain.Booking;
import edu.cmu.cs214.scheduling.domain.BookingOutcome;
import edu.cmu.cs214.scheduling.domain.BookingRequest;
import edu.cmu.cs214.scheduling.domain.Member;
import edu.cmu.cs214.scheduling.domain.Room;

/** The four workflow operations for one booking type. */
interface BookingHandler {

    String FACILITIES_CONTACT = "facilities@rooms.example.edu";

    BookingOutcome submit(BookingRequest request, Room room);

    boolean cancel(Booking booking, boolean adminOverride, String roomName);

    double priceOf(Booking booking);

    String describe(Booking booking, String roomName);

    static String recipientFor(Member member) {
        return member == null ? FACILITIES_CONTACT : member.getEmail();
    }
}
