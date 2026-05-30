package work.aemnet.memocraft.npc;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public final class TetheredWanderGoal {
    private static final float TETHER_RADIUS = 6.0f;
    private static final double WALK_SPEED = 0.10;
    private static final double ARRIVAL_EPSILON = 0.3;
    private static final int MIN_PAUSE = 40;
    private static final int MAX_PAUSE = 120;
    private static final int STUCK_THRESHOLD = 20;

    private final Entity carrier;
    private final Vec3 origin;
    private final Random rand = new Random();

    private Vec3 target = null;
    private int pauseTicks = 0;
    private int stuckTicks = 0;
    private Vec3 lastPos = null;

    public TetheredWanderGoal(Entity carrier, Vec3 origin) {
        this.carrier = carrier;
        this.origin = origin;
    }

    public void tick() {
        Vec3 currentPos = carrier.position();
        if (pauseTicks > 0) {
            pauseTicks--;
            return;
        }
        if (target == null || horizontalDistanceSq(currentPos, target) <= ARRIVAL_EPSILON * ARRIVAL_EPSILON) {
            target = pickNewTarget();
            pauseTicks = MIN_PAUSE + rand.nextInt(MAX_PAUSE - MIN_PAUSE + 1);
            return;
        }

        double dx = target.x - currentPos.x;
        double dz = target.z - currentPos.z;
        double mag = Math.sqrt(dx * dx + dz * dz);
        if (mag < 1e-6) { target = null; return; }
        double stepX = (dx / mag) * WALK_SPEED;
        double stepZ = (dz / mag) * WALK_SPEED;
        double nextX = currentPos.x + stepX;
        double nextZ = currentPos.z + stepZ;
        double nextY = currentPos.y;

        Vec3 nextPos = new Vec3(nextX, nextY, nextZ);
        BlockState blockHere = carrier.level().getBlockState(BlockPos.containing(nextX, nextY, nextZ));
        if (!blockHere.isAir() && !blockHere.getCollisionShape(carrier.level(), BlockPos.containing(nextX, nextY, nextZ)).isEmpty()) {
            BlockState above = carrier.level().getBlockState(BlockPos.containing(nextX, nextY + 1, nextZ));
            if (above.isAir()) {
                nextY += 1; // step up
                nextPos = new Vec3(nextX, nextY, nextZ);
            } else {
                target = null; return;
            }
        }

        // Snap to ground: at most 1 block down.
        BlockState below = carrier.level().getBlockState(BlockPos.containing(nextX, nextY - 1, nextZ));
        if (below.isAir()) {
            BlockState below2 = carrier.level().getBlockState(BlockPos.containing(nextX, nextY - 2, nextZ));
            if (below2.isAir()) { target = null; return; } // would fall too far
            nextY -= 1;
            nextPos = new Vec3(nextX, nextY, nextZ);
        }

        carrier.setPos(nextPos.x, nextPos.y, nextPos.z);
        carrier.setYRot((float) Math.toDegrees(Math.atan2(-stepX, stepZ)));

        if (lastPos != null && currentPos.distanceToSqr(lastPos) < 0.05 * 0.05) {
            stuckTicks++;
        } else {
            stuckTicks = 0;
        }
        if (stuckTicks > STUCK_THRESHOLD) {
            target = null;
            stuckTicks = 0;
        }
        lastPos = currentPos;
    }

    private Vec3 pickNewTarget() {
        // If off-tether, head home.
        Vec3 currentPos = carrier.position();
        double dx = currentPos.x - origin.x;
        double dz = currentPos.z - origin.z;
        if (dx * dx + dz * dz > (TETHER_RADIUS * 1.5) * (TETHER_RADIUS * 1.5)) {
            return origin;
        }
        for (int attempt = 0; attempt < 5; attempt++) {
            double r = TETHER_RADIUS * Math.sqrt(rand.nextDouble());
            double theta = rand.nextDouble() * Math.PI * 2;
            double tx = origin.x + r * Math.cos(theta);
            double tz = origin.z + r * Math.sin(theta);
            int surfaceY = carrier.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (int) Math.floor(tx), (int) Math.floor(tz));
            if (Math.abs(surfaceY - origin.y) <= 2) {
                return new Vec3(tx, surfaceY, tz);
            }
        }
        pauseTicks = MAX_PAUSE;
        return currentPos; // stay put
    }

    private static double horizontalDistanceSq(Vec3 a, Vec3 b) {
        double dx = a.x - b.x, dz = a.z - b.z;
        return dx * dx + dz * dz;
    }
}
