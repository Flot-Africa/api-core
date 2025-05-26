package africa.flot.infrastructure.service;

import africa.flot.application.ports.YangoApiService;
import africa.flot.domain.model.YangoDriverData;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.UUID;

@ApplicationScoped
public class YangoApiServiceImpl implements YangoApiService {
    private static final Logger LOG = Logger.getLogger(YangoApiServiceImpl.class);

    @Override
    public Uni<YangoDriverData> getDriverInfo(UUID leadId) {
        LOG.infof("Récupération des données Yango pour le lead %s", leadId);

        // TODO: Implémentation réelle avec appel à l'API Yango
        // Pour le moment, nous retournons des données de test

        YangoDriverData data = new YangoDriverData();
        data.setTotalRides(350);
        data.setExperienceYears(2);
        data.setMonthlyRevenue(280000.0);
        data.setRating(4.92);
        data.setAccidents(0);
        data.setDrivingTestScore(17.5);

        return Uni.createFrom().item(data);
    }
}