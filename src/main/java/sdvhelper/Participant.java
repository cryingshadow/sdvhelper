package sdvhelper;

import java.util.stream.*;

public record Participant(int id, String familyName, String givenNames, Gender gender, String school, String subject) {

    public String toCSV() {
        return Stream.of(
            String.valueOf(this.id()),
            this.familyName(),
            this.givenNames(),
            this.gender().symbol,
            this.school(),
            this.subject()
        ).collect(Collectors.joining(";"));
    }

}
