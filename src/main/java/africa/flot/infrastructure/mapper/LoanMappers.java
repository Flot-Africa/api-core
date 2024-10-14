package africa.flot.infrastructure.mapper;

import africa.flot.application.dto.query.loan.LoanDetailVM;
import africa.flot.domain.model.Loan;
import io.quarkus.arc.DefaultBean;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;


@DefaultBean
@Mapper(componentModel = "cdi")
public interface LoanMappers {

    LoanMappers INSTANCE = Mappers.getMapper(LoanMappers.class);

    LoanDetailVM loanToLonnDetailVM(Loan loan);

    Loan lonnDetailVMToLoan(LoanDetailVM loanDetailVM);
}
