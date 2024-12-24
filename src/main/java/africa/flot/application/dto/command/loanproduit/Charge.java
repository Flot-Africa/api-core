package africa.flot.application.dto.command.loanproduit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class Charge {
    @NotNull
    private Long id;
    @NotBlank
    private String name;
    private Boolean active;
    private Boolean penalty;
}