package net.citizensnpcs.util;

import net.citizensnpcs.api.npc.NPC;

/**
 * projects.gg fork (not upstream): an NPC whose look is drawn by another plugin, for example a packet-based
 * model engine that hides the NPC's own body per viewer and shows its model instead.
 *
 * <p>
 * Set it with {@code npc.data().set(ExternalAppearance.KEY, height)} (any {@link Number}: how tall the look is,
 * in blocks above the NPC's feet; a {@link Boolean} {@code true} marks the NPC without changing any height) and
 * remove it to go back to normal. It is runtime metadata on the NPC, not on its entity, so it stays across
 * despawn and respawn. Unset (the default) changes nothing. While it is set, Citizens:
 * <ul>
 * <li>places hologram lines (including the name line) above {@code height} instead of above the entity's
 * bounding box, so the text sits over the drawn model rather than inside it;</li>
 * <li>does not play the arm swing it sends to a player who starts tracking a player-type NPC
 * ({@code npc.reset-yaw-on-spawn}): the swing only exists to turn the vanilla body, and a model engine that
 * reads swings as attacks would play an attack every time the NPC comes into view.</li>
 * </ul>
 */
public final class ExternalAppearance {
    private ExternalAppearance() {
    }

    /** Whether the NPC's look is provided elsewhere. */
    public static boolean has(NPC npc) {
        return npc.data().has(KEY);
    }

    /**
     * The height hologram lines are placed above: the provided look's height when one is set, otherwise the
     * entity's bounding box height.
     */
    public static double hologramBaseHeight(NPC npc) {
        Object value = npc.data().get(KEY);
        if (value instanceof Number)
            return ((Number) value).doubleValue();
        return NMS.getBoundingBoxHeight(npc.getEntity());
    }

    public static final String KEY = "external-appearance";
}
