package rs.ltt.android.entity;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.common.base.MoreObjects;

public class QueryInfo implements Parcelable {
    public static final Creator<QueryInfo> CREATOR =
            new Creator<QueryInfo>() {
                @Override
                public QueryInfo createFromParcel(Parcel in) {
                    return new QueryInfo(in);
                }

                @Override
                public QueryInfo[] newArray(int size) {
                    return new QueryInfo[size];
                }
            };
    public final long accountId;
    public final Type type;
    public final String value;

    public QueryInfo(final long accountId, final Type type, final String value) {
        this.accountId = accountId;
        this.type = type;
        this.value = value;
    }

    protected QueryInfo(final Parcel in) {
        this.accountId = in.readLong();
        this.type = Type.valueOf(in.readString());
        this.value = in.readString();
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeLong(accountId);
        dest.writeString(type.toString());
        dest.writeString(value);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("accountId", accountId)
                .add("type", type)
                .add("value", value)
                .toString();
    }

    public enum Type {
        MAIN,
        MAILBOX,
        KEYWORD,
        SEARCH
    }
}
