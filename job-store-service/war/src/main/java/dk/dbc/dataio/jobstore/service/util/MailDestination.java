package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.NotificationEntity;
import dk.dbc.vipcore.exception.VipCoreException;
import dk.dbc.vipcore.marshallers.Information;
import dk.dbc.vipcore.service.VipCoreServiceConnector;
import jakarta.mail.Session;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static dk.dbc.dataio.commons.types.Constants.CALL_OPEN_AGENCY;
import static dk.dbc.dataio.commons.types.Constants.MISSING_FIELD_VALUE;

public class MailDestination {
    private static final Logger LOGGER = LoggerFactory.getLogger(MailDestination.class);

    private final Session mailSession;
    private final VipCoreServiceConnector vipCoreServiceConnector;
    private String destination;

    public MailDestination(Session mailSession, NotificationEntity notification, VipCoreServiceConnector vipCoreServiceConnector) {
        this.mailSession = mailSession;
        this.vipCoreServiceConnector = vipCoreServiceConnector;
        setDestination(notification);
    }

    public InternetAddress[] getToAddresses() {
        List<InternetAddress> addressList = Arrays.stream(destination.split(";")).distinct().map(s -> {
            try {
                return new InternetAddress(s, true);
            } catch (AddressException e) {
                LOGGER.error("Illegal address:{}", s, e);
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
        InternetAddress[] addresses = new InternetAddress[addressList.size()];
        for (int i = 0; i < addressList.size(); i++) {
            addresses[i] = addressList.get(i);
        }
        return addresses;
    }

    public Session getMailSession() {
        return mailSession;
    }

    @Override
    public String toString() {
        return destination;
    }

    public void useFallbackDestination() {
        destination = System.getenv("MAIL_TO_FALLBACK");
    }

    private void setDestination(NotificationEntity notification) {
        destination = notification.getDestination();
        final JobEntity job = notification.getJob();
        if (job != null) {
            if (MailNotification.isUndefined(destination)) {
                destination = inferDestinationFromJobSpecification(notification, job.getSpecification()).orElse(MISSING_FIELD_VALUE);
            }
            if (CALL_OPEN_AGENCY.equals(destination)) {
                final Information agencyInformation;
                try {
                    agencyInformation = vipCoreServiceConnector.getInformation(Long.toString(job.getSpecification().getSubmitterId()));
                    destination = inferDestinationFromAgencyInformation(notification, agencyInformation)
                            .orElse(MISSING_FIELD_VALUE);
                } catch (VipCoreException e) {
                    LOGGER.error("Failed to get agency information for agency " + job.getSpecification().getSubmitterId(), e);
                }
            }
        }
        if (destination.equals(MISSING_FIELD_VALUE)) {
            useFallbackDestination();
        }
    }

    private Optional<String> inferDestinationFromJobSpecification(NotificationEntity notification, JobSpecification jobSpecification) {
        Optional<String> destination = Optional.empty();
        switch (notification.getType()) {
            case JOB_CREATED:
                destination = toOptional(jobSpecification.getMailForNotificationAboutVerification());
                break;
            case JOB_COMPLETED:
                destination = toOptional(jobSpecification.getMailForNotificationAboutProcessing());
                if (!destination.isPresent()) {
                    destination = toOptional(jobSpecification.getMailForNotificationAboutVerification());
                }
                break;
        }
        return destination;
    }

    private Optional<String> inferDestinationFromAgencyInformation(NotificationEntity notification, Information agencyInformation) {
        Optional<String> destination = Optional.empty();
        switch (notification.getType()) {
            case JOB_CREATED:
                destination = toOptional(agencyInformation.getBranchTransReportEmail());
                if (!destination.isPresent()) {
                    destination = toOptional(agencyInformation.getBranchRejectedRecordsEmail());
                }
                break;
            case JOB_COMPLETED:
                destination = toOptional(agencyInformation.getBranchRejectedRecordsEmail());
                if (!destination.isPresent()) {
                    destination = toOptional(agencyInformation.getBranchTransReportEmail());
                }
                break;
        }
        return destination;
    }

    private Optional<String> toOptional(String value) {
        if (value == null || value.trim().isEmpty() || value.equals(MISSING_FIELD_VALUE)) {
            return Optional.empty();
        }
        return Optional.of(value);
    }
}
