package work.aemnet.memocraft.npc;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

public class MemoNPC extends ServerPlayer implements work.aemnet.memocraft.npc.IMemoNPC {
    private final int memoId;
    private final Vec3 origin;
    private final work.aemnet.memocraft.npc.TetheredWanderGoal wanderGoal;
    private final work.aemnet.memocraft.npc.MemoDisplayController displays;
    private final work.aemnet.memocraft.npc.MemoProfileManager profile;

    public MemoNPC(MinecraftServer server, ServerLevel level, GameProfile profile,
                   int memoId, Vec3 origin, String memoBody, String displayName) {
        super(server, level, profile, work.aemnet.memocraft.npc.MemoProfileManager.clientInfoWithAllSkinLayers());
        this.memoId = memoId;
        this.origin = origin;
        this.wanderGoal = new work.aemnet.memocraft.npc.TetheredWanderGoal(this, origin);
        this.displays = new work.aemnet.memocraft.npc.MemoDisplayController(this, displayName, memoBody);
        this.profile = new work.aemnet.memocraft.npc.MemoProfileManager(this);
    }

    public void spawn(Location bukkitLocation) {
        ServerLevel serverLevel = (ServerLevel) level();
        this.connection = new work.aemnet.memocraft.npc.FakeConnection(serverLevel.getServer(), this);
        setPos(bukkitLocation.getX(), bukkitLocation.getY(), bukkitLocation.getZ());
        setYRot(bukkitLocation.getYaw());
        setXRot(0f);
        setInvulnerable(true);
        serverLevel.addFreshEntity(this);
        profile.broadcastInitializing();
        displays.spawn();
    }

    @Override
    public String getCurrentSkinTexture() {
        return profile.getCurrentSkinTexture();
    }

    @Override
    public void refreshSkin(String texture, String signature) {
        profile.refreshSkin(texture, signature);
    }

    public void despawn() {
        displays.despawn();
        if (!isRemoved()) {
            this.remove(RemovalReason.DISCARDED);
        }
    }

    @Override
    public void tick() {
        super.doTick();
        wanderGoal.tick();
        if ((tickCount + (memoId & 0x7)) % 10 == 0) displays.tick();
        setHealth(getMaxHealth());
    }

    public void showMemoTo(Player viewer) {
        displays.showMemo(viewer);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean hurtServer(@NonNull ServerLevel level, @NonNull DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(@NonNull ServerLevel level, @NonNull DamageSource source) {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    public int getMemoId() { return memoId; }

    public Vec3 getOrigin() { return origin; }

    public String getMemoBody() { return displays.getMemoBody(); }

    public String getMemoDisplayName() { return displays.getName(); }

    public void setAka(String newDisplayName) {
        displays.setName(newDisplayName);
    }
}
