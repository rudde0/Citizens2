package net.citizensnpcs.trait;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.LocationLookup.PerPlayerMetadata;
import net.citizensnpcs.api.event.DespawnReason;
import net.citizensnpcs.api.event.SpawnReason;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.RemoveReason;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitName;
import net.citizensnpcs.npc.EntityController;
import net.citizensnpcs.util.EntityPacketTracker;
import net.citizensnpcs.util.NMS;
import net.citizensnpcs.util.PlayerUpdateTask;

@TraitName("packet")
public class PacketNPC extends Trait {
    private int lastVehicleId = -1;
    private EntityPacketTracker packetTracker;
    private boolean spawned = false;

    public PacketNPC() {
        super("packet");
    }

    public EntityPacketTracker getPacketTracker() {
        return packetTracker;
    }

    @Override
    public void onRemove(RemoveReason reason) {
        if (reason == RemoveReason.REMOVAL) {
            npc.despawn(DespawnReason.PENDING_RESPAWN);
            CitizensAPI.getScheduler().runRegionTask(npc.getStoredLocation(), () -> {
                if (npc.getStoredLocation() != null) {
                    npc.spawn(npc.getStoredLocation(), SpawnReason.RESPAWN);
                }
            });
        }
    }

    @Override
    public void onSpawn() {
        packetTracker = NMS.createPacketTracker(npc.getEntity());
        spawned = true;
    }

    @Override
    public void run() {
        if (!spawned)
            return;
        PerPlayerMetadata<Boolean> ppm = CitizensAPI.getLocationLookup().registerMetadata("packetnpc", null);
        Entity vehicle = npc.getEntity() == null ? null : npc.getEntity().getVehicle();
        int vehicleId = vehicle == null ? -1 : vehicle.getEntityId();
        // the vehicle sends its passenger list from its own tracker, which happens before this entity
        // exists clientside - the client drops the mount, so resend it after linking and on any change
        boolean vehicleChanged = vehicleId != lastVehicleId;
        lastVehicleId = vehicleId;
        for (Player nearby : CitizensAPI.getLocationLookup().getNearbyPlayers(npc)) {
            boolean linked = ppm.has(nearby.getUniqueId(), npc.getUniqueId().toString());
            if (!linked) {
                packetTracker.link(nearby);
                ppm.set(nearby.getUniqueId(), npc.getUniqueId().toString(), true);
                // metadata set before the player linked (display billboard, transformation, ...) isn't part of
                // what the client ends up with, so push the current state again now that it can receive it
                NMS.resendMetadata(nearby, npc.getEntity());
            }
            if (vehicle != null && (!linked || vehicleChanged)) {
                NMS.sendPassengers(nearby, vehicle);
            }
        }
        packetTracker.run();
    }

    public EntityController wrap(EntityController controller) {
        if (!(controller instanceof PacketController))
            return new PacketController(controller);
        return controller;
    }

    private class PacketController implements EntityController {
        private final EntityController base;

        public PacketController(EntityController controller) {
            base = controller;
        }

        @Override
        public void create(Location at, NPC npc) {
            base.create(at, npc);
        }

        @Override
        public void die() {
            base.die();
            if (!spawned)
                return;
            PlayerUpdateTask.deregister(getBukkitEntity());
            PerPlayerMetadata<Boolean> ppm = CitizensAPI.getLocationLookup().registerMetadata("packetnpc", null);
            packetTracker.unlinkAll(player -> ppm.remove(player.getUniqueId(), npc.getUniqueId().toString()));
            spawned = false;
        }

        @Override
        public Entity getBukkitEntity() {
            return base.getBukkitEntity();
        }

        @Override
        public void remove() {
            if (!spawned)
                return;
            PlayerUpdateTask.deregister(getBukkitEntity());
            PerPlayerMetadata<Boolean> ppm = CitizensAPI.getLocationLookup().registerMetadata("packetnpc", null);
            packetTracker.unlinkAll(player -> ppm.remove(player.getUniqueId(), npc.getUniqueId().toString()));
            base.remove();
            spawned = false;
        }

        @Override
        public void spawn(Location at, java.util.function.Consumer<Boolean> callback) {
            NMS.setLocationDirectly(base.getBukkitEntity(), at);
            PlayerUpdateTask.register(getBukkitEntity());
            callback.accept(true);
        }
    }
}