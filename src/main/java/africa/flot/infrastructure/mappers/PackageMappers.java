package africa.flot.infrastructure.mappers;

import africa.flot.application.dto.query.PackageDTO;
import africa.flot.domain.model.Package;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PackageMappers {

    PackageMappers INSTANCE = Mappers.getMapper(PackageMappers.class);
    // Méthode pour mapper l'entité Package en PackageDTO
    PackageDTO toPackageDTO(Package packageEntity);

}
