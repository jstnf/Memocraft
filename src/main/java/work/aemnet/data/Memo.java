package work.aemnet.data;

public class Memo {
    private long timestamp;
    private String message;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;

    public Memo(long timestamp, String message, double x, double y, double z, float yaw, float pitch) {
        this.timestamp = timestamp;
        this.message = message;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }
}
