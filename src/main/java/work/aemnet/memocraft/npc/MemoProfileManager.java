package work.aemnet.memocraft.npc;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

public final class MemoProfileManager {
    private final work.aemnet.memocraft.npc.MemoNPC carrier;

    public MemoProfileManager(work.aemnet.memocraft.npc.MemoNPC carrier) {
        this.carrier = carrier;
    }

    public static UUID uuidForMemo(int memoId) {
        return UUIDUtil.createOfflinePlayerUUID("MemoNpc:" + memoId);
    }

    public static GameProfile profileFor(int memoId, String name, String texture, String signature) {
        Multimap<String, Property> backing = LinkedListMultimap.create();
        if (texture != null && signature != null) {
            backing.put("textures", new Property("textures", texture, signature));
        }
        return new GameProfile(uuidForMemo(memoId), name, new PropertyMap(backing));
    }

    public static ClientInformation clientInfoWithAllSkinLayers() {
        ClientInformation d = ClientInformation.createDefault();
        return new ClientInformation(
                d.language(), d.viewDistance(), d.chatVisibility(), d.chatColors(),
                0x7F,  // all PlayerModelPart bits enabled
                d.mainHand(), d.textFilteringEnabled(), d.allowsListing(), d.particleStatus());
    }

    public String getCurrentSkinTexture() {
        return carrier.getGameProfile().properties().get("textures").stream()
                .findFirst().map(Property::value).orElse(null);
    }

    public void refreshSkin(String texture, String signature) {
        carrier.gameProfile = profileFor(
                carrier.getMemoId(),
                carrier.getGameProfile().name(),
                texture,
                signature);

        ClientboundPlayerInfoRemovePacket removeInfo =
                new ClientboundPlayerInfoRemovePacket(List.of(carrier.getUUID()));
        ClientboundPlayerInfoUpdatePacket addInfo =
                ClientboundPlayerInfoUpdatePacket.createSinglePlayerInitializing(carrier, false);
        ClientboundRemoveEntitiesPacket removeEntity =
                new ClientboundRemoveEntitiesPacket(carrier.getId());
        ClientboundAddEntityPacket addEntity =
                new ClientboundAddEntityPacket(carrier, 0, carrier.blockPosition());

        for (ServerPlayer realPlayer : server().getPlayerList().getPlayers()) {
            if (realPlayer instanceof work.aemnet.memocraft.npc.IMemoNPC) continue;
            realPlayer.connection.send(removeInfo);
            realPlayer.connection.send(removeEntity);
            realPlayer.connection.send(addInfo);
            realPlayer.connection.send(addEntity);
        }
    }

    public void broadcastInitializing() {
        ClientboundPlayerInfoUpdatePacket packet =
                ClientboundPlayerInfoUpdatePacket.createSinglePlayerInitializing(carrier, false);
        for (ServerPlayer realPlayer : server().getPlayerList().getPlayers()) {
            if (realPlayer instanceof work.aemnet.memocraft.npc.IMemoNPC) continue;
            realPlayer.connection.send(packet);
        }
    }

    private MinecraftServer server() {
        return ((ServerLevel) carrier.level()).getServer();
    }
}
