package rs.ltt.android.entity;

import com.google.common.base.Objects;

public class Subject {

    private final String subject;

    public Subject(final String subject) {
        this.subject = subject;
    }

    public String getSubject() {
        return subject == null ? null : subject.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Subject subject1 = (Subject) o;
        return Objects.equal(subject, subject1.subject);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(subject);
    }
}
