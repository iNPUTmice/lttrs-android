package rs.ltt.android.entity;

import com.google.common.base.Objects;

public abstract class From {

    public static Draft draft() {
        return new Draft();
    }

    public static Named named(final EmailAddress emailAddress) {
        return new Named(emailAddress, false);
    }

    public static Named named(final EmailAddress emailAddress, final boolean seen) {
        return new Named(emailAddress, seen);
    }


    public static class Named extends From {
        private final EmailAddress emailAddress;
        private final boolean seen;

        private Named(final EmailAddress emailAddress, final boolean seen) {
            this.emailAddress = emailAddress;
            this.seen = seen;
        }

        public boolean isSeen() {
            return seen;
        }

        public String getName() {
            return this.emailAddress.getName();
        }

        public String getEmail() {
            return this.emailAddress.getEmail();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Named named = (Named) o;
            return seen == named.seen &&
                    Objects.equal(emailAddress, named.emailAddress);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(emailAddress, seen);
        }
    }

    public static class Draft extends From {
        private Draft() {

        }

    }

}
