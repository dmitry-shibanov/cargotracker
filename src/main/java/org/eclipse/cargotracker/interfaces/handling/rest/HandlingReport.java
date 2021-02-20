package org.eclipse.cargotracker.interfaces.handling.rest;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** Transfer object for handling reports. */
public class HandlingReport {

    @NotBlank(message = "Missing completion time.")
    @Size(min = 16, max = 16, message = "Completion time value must be sixteen characters long.")
    // TODO [DDD] Apply regular expression validation.
    private String completionTime;

    @NotBlank(message = "Missing tracking ID.")
    @Size(min = 4, message = "Tracking ID must be at least four characters.")
    private String trackingId;

    @NotBlank(message = "Missing event type.")
    @Size(
            min = 4,
            max = 7,
            message = "Event type value must be one of: RECEIVE, LOAD, UNLOAD, CUSTOMS, CLAIM")
    // TODO [DDD] Apply regular expression validation.
    private String eventType;

    @NotBlank(message = "UN location code missing.")
    @Size(min = 5, max = 5, message = "UN location code must be five characters long.")
    private String unLocode;

    @Size(
            min = 4,
            max = 5,
            message = "Voyage number value must be between four and five characters long.")
    private String voyageNumber;

    public String getCompletionTime() {
        return completionTime;
    }

    public void setCompletionTime(String value) {
        this.completionTime = value;
    }

    public String getTrackingId() {
        return trackingId;
    }

    public void setTrackingId(String trackingId) {
        this.trackingId = trackingId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String value) {
        this.eventType = value;
    }

    public String getUnLocode() {
        return unLocode;
    }

    public void setUnLocode(String value) {
        this.unLocode = value;
    }

    public String getVoyageNumber() {
        return voyageNumber;
    }

    public void setVoyageNumber(String value) {
        this.voyageNumber = value;
    }
}
