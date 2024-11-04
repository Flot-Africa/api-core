package africa.flot.infrastructure.service;

import africa.flot.application.dto.query.PackageDTO;
import africa.flot.application.ports.PackageService;
import africa.flot.domain.model.Package;
import africa.flot.domain.model.valueobject.DetailedScore;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.hibernate.reactive.mutiny.Mutiny;

import java.util.List;

@ApplicationScoped
public class PackageServiceImpl implements PackageService {

    @Override
    @WithTransaction
    public Uni<List<PackageDTO>> getPackagesForScore(DetailedScore score) {
        int totalScore = score.getTotalScore();

        int highScoreThreshold = 800;
        int mediumScoreThreshold = 600;
        int lowScoreThreshold = 400;

        return Package.<Package>find("select p from Package p where p.weeklyPayment <= ?1",
                        totalScore >= highScoreThreshold ? 500
                                : totalScore >= mediumScoreThreshold ? 300
                                : totalScore >= lowScoreThreshold ? 100
                                : 50)
                .list()
                .map(packages -> packages.stream()
                        .map(Package::toDTO) // Conversion de chaque Package en PackageDTO
                        .toList());
    }


}



