package africa.flot.infrastructure.config;


import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Produces;

@ApplicationScoped
public class PostgresConfig {

    @Inject
    @DataSource("write")
    AgroalDataSource defaultDataSource;

    @Inject
    @DataSource("read")
    AgroalDataSource readDataSource;

    @Produces
    @DefaultBean
    @Named("write")
    public AgroalDataSource defaultDataSource() {
        return defaultDataSource;
    }

    @Produces
    @Named("read")
    public AgroalDataSource readDataSource() {
        return readDataSource;
    }
}