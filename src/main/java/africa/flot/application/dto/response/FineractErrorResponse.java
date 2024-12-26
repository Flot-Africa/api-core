// FineractErrorResponse.java

// YApi QuickType插件生成，具体参考文档:https://plugins.jetbrains.com/plugin/18847-yapi-quicktype/documentation

package africa.flot.application.dto.response;
import java.util.List;
import lombok.Data;

@Data
public class FineractErrorResponse {
    private String defaultUserMessage;
    private String developerMessage;
    private String userMessageGlobalisationCode;
    private List<Error> errors;
    private String httpStatusCode;
}

// Error.java

// YApi QuickType插件生成，具体参考文档:https://plugins.jetbrains.com/plugin/18847-yapi-quicktype/documentation

@Data
class Error {
    private String defaultUserMessage;
    private List<Arg> args;
    private String developerMessage;
    private String userMessageGlobalisationCode;
    private String parameterName;
}

// Arg.java

// YApi QuickType插件生成，具体参考文档:https://plugins.jetbrains.com/plugin/18847-yapi-quicktype/documentation

@Data
class Arg {
}
