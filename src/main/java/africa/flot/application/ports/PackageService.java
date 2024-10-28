package africa.flot.application.ports;


import africa.flot.domain.model.Package;
import africa.flot.domain.model.valueobject.DetailedScore;
import io.smallrye.mutiny.Uni;

import java.util.List;

public interface PackageService {
    Uni<List<Package>> getPackagesForScore(DetailedScore score);
}
