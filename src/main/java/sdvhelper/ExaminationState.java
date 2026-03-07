package sdvhelper;

public enum ExaminationState {

    GROUP("G"),
    INTERVIEW1("I"),
    INTERVIEW2("II"),
    NORMAL("N");

    public final String id;

    private ExaminationState(final String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return this.id;
    }

}
