package nukkitcoders.mobplugin.entities.spawners;

import cn.nukkit.IPlayer;
import cn.nukkit.block.Block;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.utils.Config;
import java.util.Arrays;
import nukkitcoders.mobplugin.AutoSpawnTask;
import nukkitcoders.mobplugin.entities.animal.jumping.Rabbit;
import nukkitcoders.mobplugin.entities.autospawn.AbstractEntitySpawner;
import nukkitcoders.mobplugin.entities.autospawn.SpawnResult;

/**
 * Each entity get it's own spawner class.
 *
 * @author <a href="mailto:kniffman@googlemail.com">Michael Gertz</a>
 */
public class RabbitSpawner extends AbstractEntitySpawner {

    /**
     * @param spawnTask
     */
    public RabbitSpawner(AutoSpawnTask spawnTask, Config pluginConfig) {
        super(spawnTask, pluginConfig);
    }

    public SpawnResult spawn(IPlayer iPlayer, Position pos, Level level) {
        SpawnResult result = SpawnResult.OK;

        int blockId = level.getBlockIdAt((int) pos.x, (int) pos.y, (int) pos.z);
        int blockLightLevel = level.getBlockLightAt((int) pos.x, (int) pos.y, (int) pos.z);
        int biomeId = level.getBiomeId((int) pos.x, (int) pos.z);

        int[] biomes = {2, 130, 5, 32, 160, 30, 12, 140, 17, 19, 33, 161, 31, 133, 158, 13};        
        //Desert, Desert Lake, Taiga, GTTaiga, GSTaiga, Snowy Tagia, S Tundra, Ice Spikes, Desert H, Taiga H, GT Tagia H, GS Taiga H, S Taiga H, Taiga M, S Taiga M, S Tundra M
        
        if (Block.transparent[blockId]) { // only spawns on opaque blocks
            result = SpawnResult.WRONG_BLOCK;
        // normally, they spawn in deserts, flower forests, taiga, mega taiga, cold taiga, ice plains, ice mountains, ice spikes, and the "hills" and "M" variants
        // but as this are nearly all biomes, we leave that as is
        } else if (pos.y > 127 || pos.y < 1 || level.getBlockIdAt((int) pos.x, (int) pos.y, (int) pos.z) == Block.AIR) { // cannot spawn on AIR block
            result = SpawnResult.POSITION_MISMATCH;
//        } else if (blockLightLevel < 9) { // uncommented because lightlevel doesn't work now
//            result = SpawnResult.WRONG_LIGHTLEVEL;
        } else if (!Arrays.asList(biomes).contains(biomeId)) {
            result = SpawnResult.WRONG_BLOCK;
        } else {
            this.spawnTask.createEntity(getEntityName(), pos.add(0, 1.75, 0));
        }

        return result;
    }

    /* (@Override)
     * @see cn.nukkit.entity.ai.IEntitySpawner#getEntityNetworkId()
     */
    @Override
    public int getEntityNetworkId() {
        return Rabbit.NETWORK_ID;
    }

    /* (@Override)
     * @see cn.nukkit.entity.ai.IEntitySpawner#getEntityName()
     */
    @Override
    public String getEntityName() {
        return "Rabbit";
    }

    /* (@Override)
     * @see nukkitcoders.mobplugin.entities.autospawn.AbstractEntitySpawner#getLogprefix()
     */
    @Override
    protected String getLogprefix() {
        return this.getClass().getSimpleName();
    }

}
