package rs.ltt.android.entity;

public class Subject {

    private final String subject;

    public Subject(final String subject) {
        this.subject = subject;
    }

    public String getSubject() {
        return subject == null ? null : subject.trim();
    }
}
