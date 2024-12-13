package africa.flot.application.ports;


import africa.flot.application.dto.query.PackageDTO;
import africa.flot.domain.model.Package;
import africa.flot.domain.model.valueobject.DetailedScore;
import io.smallrye.mutiny.Uni;

import java.util.List;

public interface PackageService {
    Uni<List<PackageDTO>> getPackagesForScore(DetailedScore score);
}
