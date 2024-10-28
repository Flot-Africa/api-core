package africa.flot.application.dto.command;


import java.util.UUID;

public class PackageSubscriptionCommand {
    private UUID packageId;

    public UUID getPackageId() {
        return packageId;
    }

    public void setPackageId(UUID packageId) {
        this.packageId = packageId;
    }
}
