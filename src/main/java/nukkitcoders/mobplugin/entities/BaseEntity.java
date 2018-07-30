package nukkitcoders.mobplugin.entities;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockNetherPortal;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityCreature;
import cn.nukkit.entity.EntityRideable;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.entity.EntityDamageEvent.DamageCause;
import cn.nukkit.event.entity.EntityMotionEvent;
import cn.nukkit.event.entity.EntityPortalEnterEvent;
import cn.nukkit.event.entity.EntityPortalEnterEvent.PortalType;
import cn.nukkit.level.EnumLevel;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.AxisAlignedBB;
import cn.nukkit.math.NukkitMath;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.AddEntityPacket;
import cn.nukkit.potion.Effect;
import cn.nukkit.scheduler.Task;
import co.aikar.timings.Timings;
import co.aikar.timings.TimingsHistory;
import nukkitcoders.mobplugin.MobPlugin;
import nukkitcoders.mobplugin.entities.monster.Monster;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class BaseEntity extends EntityCreature {

    protected int stayTime = 0;

    protected int moveTime = 0;

    public double moveMultifier = 1.0d;

    protected Vector3 target = null;

    protected Entity followTarget = null;

    public boolean inWater = false;

    public boolean inLava = false;

    public boolean onClimbable = false;

    protected boolean fireProof = false;

    private boolean movement = true;

    private boolean friendly = false;

    private boolean wallcheck = true;

    protected List<Block> blocksAround = new ArrayList<>();

    protected List<Block> collisionBlocks = new ArrayList<>();
    
    private boolean despawnEntities;
    
    private int despawnTicks;

    ///Jump
    private int maxJumpHeight = 1; // default: 1 block jump height - this should be 2 for horses e.g.
    protected boolean isJumping;
    public float jumpMovementFactor = 0.02F;

    public BaseEntity(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
        
        this.despawnEntities = MobPlugin.getInstance().getConfig().getBoolean("entities.despawn-entities", true);
        this.despawnTicks = MobPlugin.getInstance().getConfig().getInt("entities.despawn-ticks", 12000);
    }

    public abstract Vector3 updateMove(int tickDiff);

    public abstract int getKillExperience();

    public boolean isFriendly() {
        return this.friendly;
    }

    public boolean isMovement() {
        return this.movement;
    }

    public boolean isKnockback() {
        return this.attackTime > 0;
    }

    public boolean isWallCheck() {
        return this.wallcheck;
    }

    public void setFriendly(boolean bool) {
        this.friendly = bool;
    }

    public void setMovement(boolean value) {
        this.movement = value;
    }

    public void setWallCheck(boolean value) {
        this.wallcheck = value;
    }

    public double getSpeed() {
        return 1;
    }

    public int getMaxJumpHeight() {
        return this.maxJumpHeight;
    }

    public int getAge() {
        return this.age;
    }

    public Vector3 getTarget(){
        return this.target;
    }

    public void setTarget(Vector3 vec){
        this.target = vec;
    }

    public Entity getFollowTarget() {
        return this.followTarget != null ? this.followTarget : (this.target instanceof Entity ? (Entity) this.target : null);
    }

    public void setFollowTarget(Entity target) {
        this.followTarget = target;

        this.moveTime = 0;
        this.stayTime = 0;
        this.target = null;
    }

    @Override
    protected void initEntity() {
        super.initEntity();

        if (this.namedTag.contains("Movement")) {
            this.setMovement(this.namedTag.getBoolean("Movement"));
        }

        if (this.namedTag.contains("WallCheck")) {
            this.setWallCheck(this.namedTag.getBoolean("WallCheck"));
        }

        if (this.namedTag.contains("Age")) {
            this.age = this.namedTag.getShort("Age");
        }

        //this.setDataProperty(new ByteEntityData(DATA_FLAG_NO_AI, (byte) 1));
    }

    @Override
    public String getName(){
        return this.getClass().getSimpleName();
    }

    public void saveNBT() {
        super.saveNBT();
        this.namedTag.putBoolean("Movement", this.isMovement());
        this.namedTag.putBoolean("WallCheck", this.isWallCheck());
        this.namedTag.putShort("Age", this.age);
    }

    @Override
    public void spawnTo(Player player) {
        if (!this.hasSpawned.containsKey(player.getLoaderId()) && player.usedChunks.containsKey(Level.chunkHash(this.chunk.getX(), this.chunk.getZ()))) {
            AddEntityPacket pk = new AddEntityPacket();
            pk.entityRuntimeId = this.getId();
            pk.entityUniqueId = this.getId();
            pk.type = this.getNetworkId();
            pk.x = (float) this.x;
            pk.y = (float) this.y;
            pk.z = (float) this.z;
            pk.speedX = pk.speedY = pk.speedZ = 0;
            pk.yaw = (float) this.yaw;
            pk.pitch = (float) this.pitch;
            pk.metadata = this.dataProperties;

            player.dataPacket(pk);

            this.hasSpawned.put(player.getLoaderId(), player);
        }
    }

    @Override
    protected void updateMovement() {
        if (MobPlugin.MOB_AI_ENABLED) {
            if (this.lastX != this.x || this.lastY != this.y || this.lastZ != this.z || this.lastYaw != this.yaw || this.lastPitch != this.pitch) {
                this.lastX = this.x;
                this.lastY = this.y;
                this.lastZ = this.z;
                this.lastYaw = this.yaw;
                this.lastPitch = this.pitch;

                this.addMovement(this.x, this.y, this.z, this.yaw, this.pitch, this.yaw);
            }
        }
    }

    public boolean targetOption(EntityCreature creature, double distance) {
        if (this instanceof Monster) {
            if (creature instanceof Player) {
                Player player = (Player) creature;
                return (!player.closed) && player.spawned && player.isAlive() && player.isSurvival() && distance <= 80;
            }
            return creature.isAlive() && (!creature.closed) && distance <= 81;
        }
        return false;
    }

    /*@Override
    public List<Block> getBlocksAround() {
        if (this.blocksAround == null) {
            int minX = NukkitMath.floorDouble(this.boundingBox.getMinX());
            int minY = NukkitMath.floorDouble(this.boundingBox.getMinY());
            int minZ = NukkitMath.floorDouble(this.boundingBox.getMinZ());
            int maxX = NukkitMath.ceilDouble(this.boundingBox.getMaxX());
            int maxY = NukkitMath.ceilDouble(this.boundingBox.getMaxY());
            int maxZ = NukkitMath.ceilDouble(this.boundingBox.getMaxZ());

            this.blocksAround = new ArrayList<>();

            for (int z = minZ; z <= maxZ; ++z) {
                for (int x = minX; x <= maxX; ++x) {
                    for (int y = minY; y <= maxY; ++y) {
                        Block block = this.level.getBlock(this.temporalVector.setComponents(x, y, z));
                        this.blocksAround.add(block);
                    }
                }
            }
        }

        return this.blocksAround;
    }*/

    @Override
    protected void checkBlockCollision() {
        Vector3 vector = new Vector3(0.0D, 0.0D, 0.0D);
        Iterator<Block> d = this.getBlocksAround().iterator();

        inWater = false;
        inLava = false;
        onClimbable = false;

        while (d.hasNext()) {
            Block block = d.next();

            if (block.hasEntityCollision()) {
                block.onEntityCollide(this);
                block.addVelocityToEntity(this, vector);
            }

            if (block.getId() == Block.WATER || block.getId() == Block.STILL_WATER) {
                inWater = true;
            } else if (block.getId() == Block.LAVA || block.getId() == Block.STILL_LAVA) {
                inLava = true;
            } else if (block.getId() == Block.LADDER || block.getId() == Block.VINE) {
                onClimbable = true;
            }
        }

        if (vector.lengthSquared() > 0.0D) {
            vector = vector.normalize();
            double d1 = 0.014D;
            this.motionX += vector.x * d1;
            this.motionY += vector.y * d1;
            this.motionZ += vector.z * d1;
        }
    }

    @Override
    public boolean entityBaseTick(int tickDiff) {
        Timings.entityBaseTickTimer.startTiming();

        if (!this.isPlayer) {
            this.blocksAround = null;
            this.collisionBlocks = null;
        }
        this.justCreated = false;

        if (!this.isAlive()) {
            this.removeAllEffects();
            this.despawnFromAll();
            if (!this.isPlayer) {
                this.close();
            }
            Timings.entityBaseTickTimer.stopTiming();
            return false;
        }
        if (riding != null && !riding.isAlive() && riding instanceof EntityRideable) {
            ((EntityRideable) riding).mountEntity(this);
        }
        

        if (!this.effects.isEmpty()) {
            for (Effect effect : this.effects.values()) {
                if (effect.canTick()) {
                    effect.applyEffect(this);
                }
                effect.setDuration(effect.getDuration() - tickDiff);

                if (effect.getDuration() <= 0) {
                    this.removeEffect(effect.getId());
                }
            }
        }

        boolean hasUpdate = false;

        this.checkBlockCollision();

        if (this.y <= -16 && this.isAlive()) {
            this.attack(new EntityDamageEvent(this, DamageCause.VOID, 10));
            hasUpdate = true;
        }

        if (this.fireTicks > 0) {
            if (this.fireProof || this.inWater) {
                this.fireTicks = 0;
            } else {
                if (!this.hasEffect(Effect.FIRE_RESISTANCE) && ((this.fireTicks % 20) == 0 || tickDiff > 20)) {
                    this.attack(new EntityDamageEvent(this, DamageCause.FIRE_TICK, 1));
                }
                this.fireTicks -= tickDiff;
            }
            
            if (this.fireTicks <= 0) {
                this.extinguish();
            } else {
                this.setDataFlag(DATA_FLAGS, DATA_FLAG_ONFIRE, true);
                hasUpdate = true;
            }
        }

        if (this.noDamageTicks > 0) {
            this.noDamageTicks -= tickDiff;
            if (this.noDamageTicks < 0) {
                this.noDamageTicks = 0;
            }
        }

        if (this.inPortalTicks == 80) {
            EntityPortalEnterEvent ev = new EntityPortalEnterEvent(this, PortalType.NETHER);
            getServer().getPluginManager().callEvent(ev);

            Position newPos = EnumLevel.moveToNether(this);
            if (newPos != null) {
                for (int x = -1; x < 2; x++) {
                    for (int z = -1; z < 2; z++) {
                        int chunkX = (newPos.getFloorX() >> 4) + x,
                                chunkZ = (newPos.getFloorZ() >> 4) + z;
                        FullChunk chunk = newPos.level.getChunk(chunkX, chunkZ, false);
                        if (chunk == null || !(chunk.isGenerated() || chunk.isPopulated())) {
                            newPos.level.generateChunk(chunkX, chunkZ, true);
                        }
                    }
                }
                this.teleport(newPos.add(1.5, 1, 0.5));
                server.getScheduler().scheduleDelayedTask(new Task() {
                    @Override
                    public void onRun(int currentTick) {
                        //dirty hack to make sure chunks are loaded and generated before spawning player
                        teleport(newPos.add(1.5, 1, 0.5));
                        BlockNetherPortal.spawnPortal(newPos);
                    }
                }, 20);
            }
        }

        this.age += tickDiff;
        this.ticksLived += tickDiff;
        TimingsHistory.activatedEntityTicks++;

        Timings.entityBaseTickTimer.stopTiming();
        return hasUpdate;
    }

    @Override
    public boolean isInsideOfSolid() {
        Block block = this.level.getBlock(this.temporalVector.setComponents(NukkitMath.floorDouble(this.x), NukkitMath.floorDouble(this.y + this.getHeight() - 0.18f), NukkitMath.floorDouble(this.z)));
        AxisAlignedBB bb = block.getBoundingBox();
        return bb != null && block.isSolid() && !block.isTransparent() && bb.intersectsWith(this.getBoundingBox());
    }

    @Override
    public boolean attack(EntityDamageEvent source) {
        if (this.isKnockback() && source instanceof EntityDamageByEntityEvent && ((EntityDamageByEntityEvent) source).getDamager() instanceof Player) {
            return false;
        }

        super.attack(source);

        this.target = null;
        //this.attackTime = 7;
        return true;
    }

    public List<Block> getCollisionBlocks() {
        return collisionBlocks;
    }

    public int getMaxFallHeight() {
        if (!(this.target instanceof Entity)) {
            return 3;
        } else {
            int i = (int) (this.getHealth() - this.getMaxHealth() * 0.33F);
            i = i - (3 - this.getServer().getDifficulty()) * 4;

            if (i < 0) {
                i = 0;
            }

            return i + 3;
        }
    }

    @Override
    public boolean setMotion(Vector3 motion) {
        if (MobPlugin.MOB_AI_ENABLED) {
            if (!this.justCreated) {
                EntityMotionEvent ev = new EntityMotionEvent(this, motion);
                this.server.getPluginManager().callEvent(ev);
                if (ev.isCancelled()) {
                    return false;
                }
            }

            this.motionX = motion.x;
            this.motionY = motion.y;
            this.motionZ = motion.z;
        }
        return true;
    }

    @Override
    public boolean move(double dx, double dy, double dz) {
        if (MobPlugin.MOB_AI_ENABLED) {

            Timings.entityMoveTimer.startTiming();

            double movX = dx * moveMultifier;
            double movY = dy;
            double movZ = dz * moveMultifier;

            AxisAlignedBB[] list = this.level.getCollisionCubes(this, this.level.getTickRate() > 1 ? this.boundingBox.getOffsetBoundingBox(dx, dy, dz) : this.boundingBox.addCoord(dx, dy, dz));
            if (this.isWallCheck()) {
                for (AxisAlignedBB bb : list) {
                    dx = bb.calculateXOffset(this.boundingBox, dx);
                }
                this.boundingBox.offset(dx, 0, 0);

                for (AxisAlignedBB bb : list) {
                    dz = bb.calculateZOffset(this.boundingBox, dz);
                }
                this.boundingBox.offset(0, 0, dz);
            }
            for (AxisAlignedBB bb : list) {
                dy = bb.calculateYOffset(this.boundingBox, dy);
            }
            this.boundingBox.offset(0, dy, 0);

            this.setComponents(this.x + dx, this.y + dy, this.z + dz);
            this.checkChunks();

            this.checkGroundState(movX, movY, movZ, dx, dy, dz);
            this.updateFallState(this.onGround);

            Timings.entityMoveTimer.stopTiming();
        }
        return true;
    }

}
