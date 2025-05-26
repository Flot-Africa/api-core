package africa.flot.domain.model.enums;

/**
 * @author katinan.toure 24/05/2025 17:56
 * @project api-core
 */
public enum StatutEvaluation {
    EN_ATTENTE{
        @Override
        public String toString() {
            return "en_attente";
        }
    },
    VALIDE{
        @Override
        public String toString() {
            return "valide";
        }
    },
    ECHEC{
        @Override
        public String toString() {
            return"echec";
        }
    }
}
