package work.aemnet.memocraft.data;

public record Memo(int id, int playerId, long timestamp, String message,
                   double x, double y, double z, float yaw, float pitch) {
}
