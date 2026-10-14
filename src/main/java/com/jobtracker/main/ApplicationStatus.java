package com.jobtracker.main;


import java.util.EnumSet;

public enum ApplicationStatus {

    SAVED, APPLIED, INTERVIEW, OFFER, REJECTED;

    public EnumSet<ApplicationStatus> allowedTransitionSet() {
        return switch (this) {
            case SAVED -> EnumSet.of(ApplicationStatus.APPLIED, ApplicationStatus.REJECTED);
            case APPLIED -> EnumSet.of(ApplicationStatus.INTERVIEW, ApplicationStatus.REJECTED);
            case INTERVIEW ->
                    EnumSet.of(ApplicationStatus.INTERVIEW, ApplicationStatus.OFFER, ApplicationStatus.REJECTED);
            case OFFER -> EnumSet.of(ApplicationStatus.REJECTED);
            case REJECTED -> EnumSet.noneOf(ApplicationStatus.class);
        };
    }
}
