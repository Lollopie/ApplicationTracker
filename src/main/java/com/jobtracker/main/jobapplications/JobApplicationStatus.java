package com.jobtracker.main.jobapplications;


import java.util.EnumSet;

public enum JobApplicationStatus {

    SAVED, APPLIED, INTERVIEW, OFFER, REJECTED;

    public EnumSet<JobApplicationStatus> allowedTransitionSet() {
        return switch (this) {
            case SAVED -> EnumSet.of(JobApplicationStatus.APPLIED, JobApplicationStatus.REJECTED);
            case APPLIED -> EnumSet.of(JobApplicationStatus.INTERVIEW, JobApplicationStatus.REJECTED);
            case INTERVIEW ->
                    EnumSet.of(JobApplicationStatus.INTERVIEW, JobApplicationStatus.OFFER, JobApplicationStatus.REJECTED);
            case OFFER -> EnumSet.of(JobApplicationStatus.REJECTED);
            case REJECTED -> EnumSet.noneOf(JobApplicationStatus.class);
        };
    }
}
