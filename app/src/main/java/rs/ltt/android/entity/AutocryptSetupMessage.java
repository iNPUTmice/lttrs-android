package rs.ltt.android.entity;

import com.google.common.base.Objects;
import rs.ltt.autocrypt.client.header.PassphraseHint;

public class AutocryptSetupMessage {

    private final AccountWithCredentials account;
    private final String message;
    private final PassphraseHint passphraseHint;

    private AutocryptSetupMessage(
            AccountWithCredentials account, String message, PassphraseHint passphraseHint) {
        this.account = account;
        this.message = message;
        this.passphraseHint = passphraseHint;
    }

    public static AutocryptSetupMessage of(
            final AccountWithCredentials account, final String message) {
        final PassphraseHint passphraseHint = PassphraseHint.of(message);
        return new AutocryptSetupMessage(account, message, passphraseHint);
    }

    public AccountWithCredentials getAccount() {
        return account;
    }

    public String getMessage() {
        return message;
    }

    public PassphraseHint getPassphraseHint() {
        return passphraseHint;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AutocryptSetupMessage that = (AutocryptSetupMessage) o;
        return Objects.equal(account, that.account)
                && Objects.equal(message, that.message)
                && Objects.equal(passphraseHint, that.passphraseHint);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(account, message, passphraseHint);
    }
}
