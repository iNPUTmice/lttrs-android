package rs.ltt.android.entity;

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
    }

    public static class Draft extends From {
        private Draft() {

        }

    }

}
