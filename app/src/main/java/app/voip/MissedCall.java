package app.voip;

public class MissedCall {
    private static int sNotificationIdSource = 56789;
    private int mNotificationId;
    private int mMissedCallsCount;

    public MissedCall() {
        mNotificationId = sNotificationIdSource++;
        mMissedCallsCount = 1;
    }

    public void incrementCount() {
        mMissedCallsCount++;
    }

    public int getNotificationId() {
        return mNotificationId;
    }

    public int getMissedCallsCount() {
        return mMissedCallsCount;
    }
}
