package africa.flot.application.dto.command;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateOfficeCommande {

    private String dateFormat;
    private String externalId;
    private String name;
    private String locale;
    private Date openingDate;
    private Integer parentId;



}
