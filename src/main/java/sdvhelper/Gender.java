package sdvhelper;

public enum Gender {

    FEMALE("w"),
    MALE("m"),
    OTHER("d");

    public static Gender forSymbol(final String symbol) {
        for (final Gender gender : Gender.values()) {
            if (gender.symbol.equals(symbol)) {
                return gender;
            }
        }
        return OTHER;
    }

    public final String symbol;

    private Gender(final String symbol) {
        this.symbol = symbol;
    }

}
