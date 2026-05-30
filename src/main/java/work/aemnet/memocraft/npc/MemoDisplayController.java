package work.aemnet.memocraft.npc;

import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public final class MemoDisplayController {

    private static final double NAME_REVEAL_RADIUS = 5.0;
    private static final double NAME_HYSTERESIS_RADIUS = 6.5;
    private static final int MEMO_VISIBLE_DURATION = 100;

    private static final float NAME_TAG_Y_OFFSET = 0.3f;
    private static final float MEMO_BODY_Y_OFFSET = 0.6f;

    private final work.aemnet.memocraft.npc.MemoNPC carrier;
    private final String memoBody;
    private String displayName;

    private TextDisplay nameTagDisplay;
    private TextDisplay memoBodyDisplay;
    private boolean nameVisible = false;
    private int memoVisibleTicks = 0;

    public MemoDisplayController(work.aemnet.memocraft.npc.MemoNPC carrier, String displayName, String memoBody) {
        this.carrier = carrier;
        this.displayName = displayName;
        this.memoBody = memoBody;
    }

    public void spawn() {
        var bukkitNpc = carrier.getBukkitEntity();
        var bukkitWorld = bukkitNpc.getWorld();
        var spawnLoc = bukkitNpc.getLocation();

        nameTagDisplay = (TextDisplay) bukkitWorld.spawnEntity(spawnLoc, EntityType.TEXT_DISPLAY);
        configureDisplay(nameTagDisplay, displayName, NAME_TAG_Y_OFFSET);
        nameTagDisplay.setVisibleByDefault(false);
        bukkitNpc.addPassenger(nameTagDisplay);

        memoBodyDisplay = (TextDisplay) bukkitWorld.spawnEntity(spawnLoc, EntityType.TEXT_DISPLAY);
        configureDisplay(memoBodyDisplay, memoBody, MEMO_BODY_Y_OFFSET);
        memoBodyDisplay.setVisibleByDefault(false);
        bukkitNpc.addPassenger(memoBodyDisplay);
    }

    public void tick() {
        var bukkitNpc = carrier.getBukkitEntity();
        var loc = bukkitNpc.getLocation();
        double nearestDistSq = Double.MAX_VALUE;
        for (var p : bukkitNpc.getWorld().getPlayers()) {
            if (p.getGameMode() == GameMode.SPECTATOR) continue;
            if (((CraftPlayer) p).getHandle() instanceof work.aemnet.memocraft.npc.IMemoNPC) continue;
            double d = p.getLocation().distanceSquared(loc);
            if (d < nearestDistSq) nearestDistSq = d;
        }
        double nearestDist = Math.sqrt(nearestDistSq);

        if (!nameVisible && nearestDist <= NAME_REVEAL_RADIUS) {
            nameTagDisplay.setVisibleByDefault(true);
            nameVisible = true;
        } else if (nameVisible && nearestDist > NAME_HYSTERESIS_RADIUS) {
            nameTagDisplay.setVisibleByDefault(false);
            nameVisible = false;
        }

        if (memoVisibleTicks > 0) {
            memoVisibleTicks -= 10;
            if (memoVisibleTicks <= 0) {
                memoBodyDisplay.setVisibleByDefault(false);
            }
        }
    }

    public void showMemo(Player viewer) {
        memoVisibleTicks = MEMO_VISIBLE_DURATION;
        memoBodyDisplay.setVisibleByDefault(true);
    }

    public void setName(String newDisplayName) {
        this.displayName = newDisplayName;
        if (nameTagDisplay != null) {
            nameTagDisplay.text(Component.text(newDisplayName));
        }
    }

    public String getName() {
        return displayName;
    }

    public String getMemoBody() {
        return memoBody;
    }

    public void despawn() {
        if (nameTagDisplay != null) nameTagDisplay.remove();
        if (memoBodyDisplay != null) memoBodyDisplay.remove();
    }

    private static void configureDisplay(TextDisplay d, String text, float yOffset) {
        d.text(Component.text(text));
        d.setBillboard(Display.Billboard.CENTER);
        d.setBackgroundColor(Color.fromARGB(0x40, 0, 0, 0));
        d.setSeeThrough(false);
        d.setShadowed(true);
        d.setPersistent(false);
        d.setTransformation(new Transformation(
                new Vector3f(0f, yOffset, 0f),
                new AxisAngle4f(0f, 0f, 0f, 1f),
                new Vector3f(1f, 1f, 1f),
                new AxisAngle4f(0f, 0f, 0f, 1f)));
    }
}
