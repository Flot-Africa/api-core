package africa.flot.infrastructure.config;

import io.quarkus.runtime.annotations.RegisterForReflection;
import africa.flot.application.dto.response.*;

@RegisterForReflection(targets = {
        AuthResponseDTO.class,
        RepaymentTemplateDTO.class,
        RepaymentResponseDTO.class
})
public class ReflectionConfiguration {
    // Configuration vide, l'annotation fait tout le travail
}