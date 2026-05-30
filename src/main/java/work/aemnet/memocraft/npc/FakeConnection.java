package work.aemnet.memocraft.npc;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

/**
 * A no-op packet sink so headless NPCs (no real client) don't NPE when
 * ServerPlayer internals reach for connection.send(...).
 *
 * <p>Three correctness concerns are handled here, all surfaced by inspecting
 * the Paper 26.1.2 mojang-mapped {@code ServerCommonPacketListenerImpl} and
 * {@code Connection} bytecode (via {@code javap}):
 *
 * <ol>
 *   <li><b>All {@code send(...)} overloads are overridden.</b> The parent
 *       declares {@code send(Packet)} and
 *       {@code send(Packet, ChannelFutureListener)}; the 1-arg variant
 *       delegates to the 2-arg one, which is what actually touches the
 *       channel. We override both.</li>
 *   <li><b>An {@link EmbeddedChannel} is attached to the {@link Connection}
 *       via its real Netty {@code channelActive} hook.</b> Adding the
 *       {@code Connection} (a {@code SimpleChannelInboundHandler}) to an
 *       {@code EmbeddedChannel} pipeline fires {@code channelActive}, which
 *       populates {@code Connection.channel} and {@code Connection.address}.
 *       That keeps {@code isConnected()}, {@code getRemoteAddress()}, and any
 *       direct {@code connection.send(...)} call from NPE'ing. Direct calls
 *       to the underlying channel become no-op writes into the embedded
 *       channel's outbound queue (which we never drain).</li>
 *   <li><b>Both {@code disconnect(...)} overloads are overridden.</b> Paper
 *       26.1.2 has {@code disconnect(Component)} and
 *       {@code disconnect(DisconnectionDetails)}; the {@code Component}
 *       variant wraps and delegates to the {@code DisconnectionDetails}
 *       variant internally, but other NMS code paths may call either, so
 *       both are stubbed. There is no {@code disconnectAsync} on
 *       {@code ServerCommonPacketListenerImpl} in this version.</li>
 * </ol>
 *
 * <ul>
 *   <li>{@code ServerCommonPacketListenerImpl.send(Packet)}</li>
 *   <li>{@code ServerCommonPacketListenerImpl.send(Packet, ChannelFutureListener)}</li>
 *   <li>{@code ServerCommonPacketListenerImpl.disconnect(Component)}</li>
 *   <li>{@code ServerCommonPacketListenerImpl.disconnect(DisconnectionDetails)}</li>
 *   <li>{@code Connection.send(Packet)}</li>
 *   <li>{@code Connection.send(Packet, ChannelFutureListener)}</li>
 *   <li>{@code Connection.send(Packet, ChannelFutureListener, boolean)}</li>
 *   <li>{@code Connection.channelActive(ChannelHandlerContext)} (populates channel + address)</li>
 *   <li>{@code Connection.disconnect(Component)}</li>
 *   <li>{@code Connection.disconnect(DisconnectionDetails)}</li>
 * </ul>
 */
public final class FakeConnection extends ServerGamePacketListenerImpl {
    public FakeConnection(MinecraftServer server, ServerPlayer npc) {
        super(server, makeEmbeddedConnection(), npc,
                CommonListenerCookie.createInitial(npc.getGameProfile(), false));
    }

    /**
     * Builds a {@link Connection} that is "wired up" to a throwaway
     * {@link EmbeddedChannel}, so probe calls ({@code isConnected()},
     * {@code getRemoteAddress()}) and any leaked direct
     * {@code connection.send(...)} calls don't NPE.
     */
    private static Connection makeEmbeddedConnection() {
        Connection connection = new Connection(PacketFlow.CLIENTBOUND);
        // Adding the Connection handler to an EmbeddedChannel pipeline
        // synchronously fires channelActive(ctx), which Connection uses to
        // populate its `channel` and `address` fields. After this point the
        // Connection is in a sane "appears-connected" state. We never read
        // from this channel; any outbound writes that slip past our send()
        // overrides land harmlessly in the channel's outbound message queue.
        EmbeddedChannel channel = new EmbeddedChannel(connection);
        // Ensure the handler is actually registered. EmbeddedChannel's
        // constructor adds + fires channelRegistered/channelActive, but if a
        // future Netty change deferred that we'd notice via a null channel.
        if (!channel.isActive()) {
            throw new IllegalStateException(
                    "FakeConnection: EmbeddedChannel did not become active; "
                            + "Connection.channel will be null and NPCs will NPE.");
        }
        return connection;
    }

    // ---- send(...) overrides ------------------------------------------------
    // Both overloads from ServerCommonPacketListenerImpl. The 1-arg form
    // normally delegates to the 2-arg form (with a null listener); we override
    // both so callers that go straight to the 2-arg variant are still caught.

    @Override
    public void send(Packet<?> packet) {
        // Drop. The NPC has no real client.
    }

    @Override
    public void send(Packet<?> packet, ChannelFutureListener listener) {
        // Drop. Don't fire the listener either: it would normally only run
        // after a real network write completes, and downstream code that
        // cares about the result wouldn't reach this NPC anyway.
    }

    // ---- disconnect(...) overrides -----------------------------------------
    // Both overloads from ServerCommonPacketListenerImpl. Paper 26.1.2 has no
    // `disconnectAsync` on this class hierarchy.

    @Override
    public void disconnect(Component reason) {
        // Drop. The NPC is never "disconnected."
    }

    @Override
    public void disconnect(DisconnectionDetails details) {
        // Drop. Same rationale; this is the variant the Component-arg form
        // delegates to internally, and it's what newer NMS call sites prefer.
    }
}
