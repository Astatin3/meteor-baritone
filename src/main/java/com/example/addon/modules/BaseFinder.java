package com.example.addon.modules;


import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalXZ;
import baritone.process.CustomGoalProcess;
import com.example.addon.AddonTemplate;
import com.example.addon.LavacastGenerator;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.WorldChunk;
import baritone.api.BaritoneAPI;

import java.util.*;

public class BaseFinder extends Module {

    public BaseFinder() {
        super(AddonTemplate.CATEGORY, "EXAMPLE thuig", "eeeeeeeeeee");
    }

//    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgUnnaturalBlocks = this.settings.createGroup("Unnatural Blocks");
    private final SettingGroup sgSyntheticBlocks = this.settings.createGroup("Synthetic Blocks");
    private final SettingGroup sgDoubleChest = this.settings.createGroup("Double chest detector");







    private final Setting<List<Block>> Unnaturalblocks = sgUnnaturalBlocks.add(new BlockListSetting.Builder()
        .name("Unnatural Blocks")
        .description("Blocks to search for.")
        .onChanged(blocks1 -> {
            if (isActive() && Utils.canUpdate()) onActivate();
        })
        .defaultValue(DEFAULT_NATURAL_BLOCKS)
    .build()
    );

    private final Setting<Integer> UnnaturalMinHeight = sgUnnaturalBlocks.add(new IntSetting.Builder()
        .name("Min height")
        .description("Minimum height to detect blocks")
        .defaultValue(64)
        .range(-64, 319)
        .sliderRange(-64, 319)
        .build()
    );

    private final Setting<Integer> UnnaturalBlockThreshold = sgUnnaturalBlocks.add(new IntSetting.Builder()
        .name("Block Threshold")
        .description("Minimum amount of unnatural blocks")
        .defaultValue(64)
        .range(0, 500)
        .sliderRange(0, 500)
        .build()
    );

    private final Setting<SettingColor> UnnaturalBlocksColor = sgUnnaturalBlocks.add(new ColorSetting.Builder()
        .name("Unnatural Blocks Color")
        .description("The color of the marker.")
        .defaultValue(Color.GREEN)
        .build()
    );











    private final Setting<List<Block>> Syntheticblocks = sgSyntheticBlocks.add(new BlockListSetting.Builder()
        .name("Unnatural Blocks")
        .description("Blocks to search for.")
        .onChanged(blocks1 -> {
            if (isActive() && Utils.canUpdate()) onActivate();
        })
        .defaultValue(DEFAULT_SYNTHETIC_BLOCKS)
    .build()
    );

    private final Setting<Integer> SyntheticBlockThreshold = sgSyntheticBlocks.add(new IntSetting.Builder()
        .name("Block Threshold")
        .description("Minimum amount of unnatural blocks")
        .defaultValue(3)
        .range(0, 20)
        .sliderRange(0, 20)
        .build()
    );

    private final Setting<SettingColor> SyntheticBlocksColor = sgSyntheticBlocks.add(new ColorSetting.Builder()
        .name("Synthetic Blocks Color")
        .description("The color of the marker.")
        .defaultValue(Color.MAGENTA)
        .build()
    );





    private final Setting<SettingColor> DoubleChestColor = sgDoubleChest.add(new ColorSetting.Builder()
        .name("Synthetic Blocks Color")
        .description("The color of the marker.")
        .defaultValue(Color.RED)
        .build()
    );







    private static final int CHUNK_CENTER_OFFSET = 8;

    private class ChunkPosition {
        int start_x;
        int start_y;
        int start_z;
        int end_x;
        int end_y;
        int end_z;

        public Vec3d getCenterPos() {
            return new Vec3d(start_x + CHUNK_CENTER_OFFSET, end_y, start_z + CHUNK_CENTER_OFFSET);
        }
    }

    List<ChunkPosition> AlreadyScannedChunks = new ArrayList<>();

    List<Vec3d> UnnaturalPositions = new ArrayList<>();
    List<ChunkPosition> UnnaturalChunkPositions = new ArrayList<>();
    List<Vec3d> SyntheticPositions = new ArrayList<>();

    List<ChunkPosition> SyntheticChunkPositions = new ArrayList<>();
    List<Vec3d> PlaceBlocks = new ArrayList<>();

    @Override
    public void onActivate() {
        UnnaturalPositions.clear();
        UnnaturalChunkPositions.clear();
        SyntheticPositions.clear();
        SyntheticChunkPositions.clear();
        AlreadyScannedChunks.clear();
        PlaceBlocks.clear();
        AlreadyChosenChunks.clear();

        info(BaritoneAPI.getProvider().getPrimaryBaritone().toString());
    }

    @EventHandler
    private void onChunkData(ChunkDataEvent event) {
        WorldChunk chunk = event.chunk();

        for(int i = 0; i < AlreadyScannedChunks.size(); i++) {
            if (chunk.getPos().getStartX() == AlreadyScannedChunks.get(i).start_x &&
            chunk.getPos().getStartZ() == AlreadyScannedChunks.get(i).start_z) {
                return;
            }
        }

        List<Block> UnnaturalBlocks = Unnaturalblocks.get();
        List<Block> SyntheticBlocks = Syntheticblocks.get();

        List<Vec3d> chests = new ArrayList<>();
        List<Integer> SpawnerYLevels = new ArrayList<>();

        BlockPos.Mutable blockPos = new BlockPos.Mutable();

        int UnnaturalBlockCount = 0;
        int SyntheticBlockCount = 0;
//        List<Vec3d> positions = new ArrayList<>();
        int minSearchY = UnnaturalMinHeight.get();

        int maxY = -5000;
        int minY = 5000;

        for(int x = chunk.getPos().getStartX(); x <= chunk.getPos().getEndX(); ++x) {
            for(int z = chunk.getPos().getStartZ(); z <= chunk.getPos().getEndZ(); ++z) {
                int height = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE).get(x - chunk.getPos().getStartX(), z - chunk.getPos().getStartZ());

                if(height > maxY){
                    maxY = height;
                }

//                    for(int y = MeteorClient.mc.world.getBottomY(); y < minHeight; ++y) {
                for(int y = Math.max(MeteorClient.mc.world.getBottomY(), minSearchY); y < height; ++y) {
                    blockPos.set(x, y, z);
                    BlockState bs = chunk.getBlockState(blockPos);
                    Vec3d curpos = new Vec3d(x, y, z);

                    if(!UnnaturalBlocks.contains(bs.getBlock())) {
                        UnnaturalPositions.add(curpos);
                        UnnaturalBlockCount++;
//                        info(bs.getBlock().getName());
                        if(y < minY){
                            minY = y;
                        }
                    }

                    if(SyntheticBlocks.contains(bs.getBlock())) {
                        SyntheticPositions.add(curpos);
                        SyntheticBlockCount++;
//                        info(bs.getBlock().getName());
                        if(y < minY){
                            minY = y;
                        }
                    }

                    if(bs.getBlock().equals(Blocks.CHEST))
                        chests.add(curpos);

                    if(bs.getBlock().equals(Blocks.SPAWNER))
                        SpawnerYLevels.add(y);
                }
            }
        }

        ChunkPosition c = new ChunkPosition();
        c.start_x = chunk.getPos().getStartX();
        c.start_y = minY;
        c.start_z = chunk.getPos().getStartZ();
        c.end_x = chunk.getPos().getEndX() + 1;
        c.end_y = maxY;
        c.end_z = chunk.getPos().getEndZ() + 1;

        if(UnnaturalBlockCount >= UnnaturalBlockThreshold.get()) {
            UnnaturalChunkPositions.add(c);
            info("Unnatural chunk found (" + c.start_x + ", " + c.start_z + ")");
        }
        if(SyntheticBlockCount >= SyntheticBlockThreshold.get()) {
            SyntheticChunkPositions.add(c);
            info("Base chunk found using synthetic blocks: (" + c.start_x + ", " + c.start_z + ")");
        }

        for(int i = 0; i < chests.size(); i++){
            Vec3d chest = chests.get(i);

            if(SpawnerYLevels.contains((int) chest.y))
                continue;

            blockPos.set(chest.x+1, chest.y, chest.z);
            Block bs1 = chunk.getBlockState(blockPos).getBlock();
            blockPos.set(chest.x-1, chest.y, chest.z);
            Block bs2 = chunk.getBlockState(blockPos).getBlock();
            blockPos.set(chest.x, chest.y, chest.z+1);
            Block bs3 = chunk.getBlockState(blockPos).getBlock();
            blockPos.set(chest.x, chest.y, chest.z-1);
            Block bs4 = chunk.getBlockState(blockPos).getBlock();

            if((bs1.equals(Blocks.CHEST) ||
            bs2.equals(Blocks.CHEST) ||
            bs3.equals(Blocks.CHEST) ||
            bs4.equals(Blocks.CHEST)) &&
            !SyntheticChunkPositions.contains(c)) {
                SyntheticChunkPositions.add(c);
                info("Base chunk found using double chests: (" + c.start_x + ", " + c.start_z + ")");
            }
        }


        AlreadyScannedChunks.add(c);

//        false_positions.addAll(positions);
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        for(Vec3d pos : UnnaturalPositions) {
            Box tmpbox = new Box(
                pos.x, pos.y, pos.z, pos.x+1, pos.y+1, pos.z+1
            );
            event.renderer.box(tmpbox, UnnaturalBlocksColor.get(), UnnaturalBlocksColor.get(), ShapeMode.Lines, 0);
        }
        for(ChunkPosition chunkpos : UnnaturalChunkPositions) {
            Box tmpbox = new Box(
                chunkpos.start_x,
                chunkpos.start_y,
                chunkpos.start_z,
                chunkpos.end_x,
                chunkpos.end_y,
                chunkpos.end_z
            );
            event.renderer.box(tmpbox, UnnaturalBlocksColor.get(), UnnaturalBlocksColor.get(), ShapeMode.Lines, 0);
        }





        for(Vec3d pos : SyntheticPositions) {
            Box tmpbox = new Box(
                pos.x, pos.y, pos.z, pos.x+1, pos.y+1, pos.z+1
            );
            event.renderer.box(tmpbox, SyntheticBlocksColor.get(), SyntheticBlocksColor.get(), ShapeMode.Lines, 0);
        }
        for(ChunkPosition chunkpos : SyntheticChunkPositions) {
            Box tmpbox = new Box(
                chunkpos.start_x,
                chunkpos.start_y,
                chunkpos.start_z,
                chunkpos.end_x,
                chunkpos.end_y + 1,
                chunkpos.end_z
            );
            event.renderer.box(tmpbox, SyntheticBlocksColor.get(), SyntheticBlocksColor.get(), ShapeMode.Lines, 0);
        }

        for(Vec3d pos : PlaceBlocks) {
            Box tmpbox = new Box(
                pos.x, pos.y, pos.z, pos.x+1, pos.y+1, pos.z+1
            );
            event.renderer.box(tmpbox, Color.WHITE, Color.WHITE, ShapeMode.Lines, 0);
        }

    }

    private static final int MillisPerLavaBlock = 3000 / 4;
    private static final int MillisPerWaterBlock = 500 / 4;


    private void useItem(FindItemResult item, boolean placedWater, BlockPos blockPos, boolean interactItem) {
        if (!item.found()) return;

        if (interactItem) {
            Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), 10, true, () -> {
                if (item.isOffhand()) {
                    mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
                } else {
                    InvUtils.swap(item.slot(), true);
                    mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                    InvUtils.swapBack();
                }
            });
        } else {
            BlockUtils.place(blockPos, item, true, 10, true);
        }
    }




    private double XZDistance(Vec3d p1, Vec3d p2){
        return Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.z - p2.z, 2));
    }

    private List<ChunkPosition> AlreadyChosenChunks = new ArrayList<>();
    private Thread runThread;

    public void generate() {
        PlaceBlocks.clear();
        AlreadyChosenChunks.clear();

        runThread = new Thread(() -> {
            try {
            for (int i = 0; i < SyntheticChunkPositions.size(); i++) {

                if (AlreadyChosenChunks.contains(SyntheticChunkPositions.get(i))) continue;
                Vec3d originpos = SyntheticChunkPositions.get(i).getCenterPos();
                List<Vec3d> chunks = new ArrayList<>();
                chunks.add(originpos);
                SearchNearbyChunks(chunks, originpos);

                info("Found region with " + chunks.size() + " chunks");
                info("Generating path");

                int maxY = Integer.MIN_VALUE;
                int minY = Integer.MAX_VALUE;

                int sumX = 0;
                int sumZ = 0;

                List<LavacastGenerator.Position> targetTiles = new ArrayList<>();
                for (int a = 0; a < chunks.size(); a++) {
                    Vec3d chunk = chunks.get(a);

                    maxY = Math.max(maxY, (int) chunk.y);
                    minY = Math.min(minY, (int) chunk.y);

                    LavacastGenerator.Position pos = new LavacastGenerator.Position(
                        (int) ((chunk.x - CHUNK_CENTER_OFFSET) / 16),
                        (int) ((chunk.z - CHUNK_CENTER_OFFSET) / 16)
                    );

                    targetTiles.add(pos);

                    sumX += pos.x;
                    sumZ += pos.z;
                }


                LavacastGenerator.Position origin = new LavacastGenerator.Position(
                    (int) Math.round((double) sumX / targetTiles.size()),
                    (int) Math.round((double) sumZ / targetTiles.size())
                );


                LavacastGenerator.MovementNode path = new LavacastGenerator.MovementNode(origin, 0);
                List<LavacastGenerator.Position> visited = new ArrayList<>();
                visited.add(origin);

                while (!visited.containsAll(targetTiles)) {
                    path.expand(targetTiles, visited);
                }

                //            info("Found path with " + countMoves(optimizedPath) + " Moves");

                int Yoffset = (path.depthBeyond * 16) + maxY + 16;
                int lavacastHeight = Yoffset - minY;

                if (path != null)
                    recurseAddBlocks(path, 0, null, Yoffset);

                int centerX = (path.position.x * 16) + CHUNK_CENTER_OFFSET;
                int centerZ = (path.position.z * 16) + CHUNK_CENTER_OFFSET;

                BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(
                    new GoalBlock(
                        centerX,
                        Yoffset + 1,
                        centerZ
                    )
                );

                while(BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().isActive())
                    Thread.sleep(100);

                recurseBuild(path, 0, null, Yoffset);

//                int level = 3;

                for(int level = 3; level <= 15; level += 2){

                    FindItemResult cobble = InvUtils.find(Items.COBBLESTONE);

                    if (!cobble.found() || cobble.count() < 32) {
                        error("Not enough blocks, halting");
                        Thread.currentThread().interrupt();
                        return;
                    }

                    BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(
                        new GoalBlock(
                            centerX + 1,
                            Yoffset + level,
                            centerZ
                        )
                    );

                    while(BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().isActive())
                        Thread.sleep(100);


                    BlockPos lavacast_support_block_1 = new BlockPos(
                        centerX, Yoffset + level - 3, centerZ
                    );

                    if (mc.world.getBlockState(lavacast_support_block_1).getBlock() == Blocks.AIR) {
                        BlockUtils.place(lavacast_support_block_1, cobble, true, 0, true);
                        Thread.sleep(PLACE_DELAY);
                    }
//
//                    BlockPos lavacast_support_block2 = new BlockPos(
//                        centerX, Yoffset + level - 2, centerZ
//                    );
//
//                    if (mc.world.getBlockState(lavacast_support_block2).getBlock() == Blocks.AIR) {
//                        BlockUtils.place(lavacast_support_block2, cobble, true, 0, true);
//                        Thread.sleep(PLACE_DELAY);
//                    }



                    BlockPos lavacast_block = new BlockPos(
                        centerX, Yoffset + level - 2, centerZ
                    );

                    if (mc.world.getBlockState(lavacast_block).getBlock() != Blocks.AIR) {
                        BlockUtils.breakBlock(lavacast_block, false);
                        Thread.sleep(PLACE_DELAY);
                    }




                    BlockPos scaffold_block_1 = new BlockPos(
                        centerX, Yoffset + level - 1, centerZ
                    );

                    if (mc.world.getBlockState(scaffold_block_1).getBlock() != Blocks.AIR) {
                        BlockUtils.breakBlock(scaffold_block_1
                            , false);
                        Thread.sleep(PLACE_DELAY);
                    }
//
//


                    BlockPos scaffold_block_2 = new BlockPos(
                        centerX + 1, Yoffset + level - 2, centerZ
                    );

                    if (mc.world.getBlockState(scaffold_block_2).getBlock() != Blocks.AIR) {
                        BlockUtils.breakBlock(scaffold_block_2, false);
                        Thread.sleep(PLACE_DELAY);
                    }


                    BlockPos scaffold_block_3 = new BlockPos(
                        centerX + 1, Yoffset + level - 3, centerZ
                    );

                    if (mc.world.getBlockState(scaffold_block_3).getBlock() != Blocks.AIR) {
                        BlockUtils.breakBlock(scaffold_block_3, false);
                        Thread.sleep(PLACE_DELAY);
                    }


                    mc.player.setPosition(
                        centerX + 1.4,
                        Yoffset + level,
                        centerZ + 0.5
                    );


                    useItem(InvUtils.findInHotbar(Items.LAVA_BUCKET), false, lavacast_block, true);
                    Thread.sleep((long) MillisPerLavaBlock * (lavacastHeight + level));
                    useItem(InvUtils.findInHotbar(Items.BUCKET), false, lavacast_block, true);
                    Thread.sleep(MillisPerLavaBlock);
                    useItem(InvUtils.findInHotbar(Items.WATER_BUCKET), false, lavacast_block, true);
                    Thread.sleep(MillisPerWaterBlock  * 4);
                    useItem(InvUtils.findInHotbar(Items.BUCKET), false, lavacast_block, true);
                    Thread.sleep(MillisPerWaterBlock * 2);








//                    level += 2;
                }


//                BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(
//                    new GoalBlock(
//                        (path.position.x * 16) + CHUNK_CENTER_OFFSET,
//                        0,
//                        (path.position.z * 16) + CHUNK_CENTER_OFFSET
//                    )
//                );
            }
            } catch(InterruptedException v){
                System.out.println(v);
            }
        });

        runThread.start();
    }

    @Override
    public void onDeactivate() {
        if(runThread != null)
            runThread.interrupt();
    }

    private static final int PLACE_DELAY = 100;

    private void recurseBuild(LavacastGenerator.MovementNode node, int depth, LavacastGenerator.Position prevPosition, int Yoffset) throws InterruptedException {
        info("Building with depth " + depth);

        if(prevPosition != null) {
            int deltaX = node.position.x - prevPosition.x;
            int deltaZ = node.position.z - prevPosition.z;

            FindItemResult invBlocks = InvUtils.find(Items.COBBLESTONE);

            if (!invBlocks.found() || invBlocks.count() < 32) {
                error("Not enough blocks, halting");
//                toggle();
                Thread.currentThread().interrupt();
                return;
            }



            for (int i = -16; i < 0; i++) {

                int x = (node.position.x * 16) + CHUNK_CENTER_OFFSET + (i * deltaX);
                int y = Yoffset - (depth * 16) - i;
                int z = (node.position.z * 16) + CHUNK_CENTER_OFFSET + (i * deltaZ);

                BlockUtils.place(new BlockPos(new Vec3i(x, y, z)), invBlocks, true, 0, true);

                Thread.sleep(PLACE_DELAY);

                BlockUtils.place(new BlockPos(new Vec3i(x, y - 1, z)), invBlocks, true, 0, true);

                Thread.sleep(PLACE_DELAY);


                BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(
                    new GoalBlock(x, y + 1, z)
                );

                while (BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().isActive())
                    Thread.sleep(100);
            }
        }
//            PlaceBlocks.add(new Vec3d(
//                (node.position.x * 16) + CHUNK_CENTER_OFFSET,
//                Yoffset - (depth * 16),
//                (node.position.z * 16) + CHUNK_CENTER_OFFSET
//            ));
//        }

        if(node.PosX != null)
            recurseBuild(node.PosX, depth+1, node.position, Yoffset);
        if(node.PosZ != null)
            recurseBuild(node.PosZ, depth+1, node.position, Yoffset);
        if(node.NegX != null)
            recurseBuild(node.NegX, depth+1, node.position, Yoffset);
        if(node.NegZ != null)
            recurseBuild(node.NegZ, depth+1, node.position, Yoffset);

        if(node.PosX != null || node.PosZ != null || node.NegX != null || node.NegZ != null) {
            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(
                new GoalBlock(
                    (node.position.x * 16) + CHUNK_CENTER_OFFSET,
                    Yoffset - (depth * 16) + 1,
                    (node.position.z * 16) + CHUNK_CENTER_OFFSET)
            );

            while (BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().isActive())
                Thread.sleep(100);
        }
    }



    private void recurseAddBlocks(LavacastGenerator.MovementNode node, int depth, LavacastGenerator.Position prevPosition, int Yoffset) {

        if(prevPosition != null) {
            int deltaX = node.position.x - prevPosition.x;
            int deltaZ = node.position.z - prevPosition.z;

            for (int i = -16; i < 0; i++) {
                PlaceBlocks.add(new Vec3d(
                    (node.position.x * 16) + CHUNK_CENTER_OFFSET + (i * deltaX),
                    Yoffset - (depth * 16) - i,
                    (node.position.z * 16) + CHUNK_CENTER_OFFSET + (i * deltaZ)
                ));
            }
        }else{
            PlaceBlocks.add(new Vec3d(
                (node.position.x * 16) + CHUNK_CENTER_OFFSET,
                Yoffset - (depth * 16),
                (node.position.z * 16) + CHUNK_CENTER_OFFSET
            ));
        }

        if(node.PosX != null)
            recurseAddBlocks(node.PosX, depth+1, node.position, Yoffset);
        if(node.PosZ != null)
            recurseAddBlocks(node.PosZ, depth+1, node.position, Yoffset);
        if(node.NegX != null)
            recurseAddBlocks(node.NegX, depth+1, node.position, Yoffset);
        if(node.NegZ != null)
            recurseAddBlocks(node.NegZ, depth+1, node.position, Yoffset);

    }


    private void SearchNearbyChunks(List<Vec3d> chunks, Vec3d origin) {
        for(int i = 0; i < UnnaturalChunkPositions.size(); i++){
            ChunkPosition chunkpos = UnnaturalChunkPositions.get(i);
//            chunkpos.getEndPos()
            Vec3d pos = chunkpos.getCenterPos();
            if(chunks.contains(pos)) continue;
            if(AlreadyChosenChunks.contains(chunkpos)) continue;
            double distance = XZDistance(pos, origin);
            if(distance > 32) continue;

            chunks.add(pos);
            AlreadyChosenChunks.add(chunkpos);

            SearchNearbyChunks(chunks, pos);
        }
        for(int i = 0; i < SyntheticChunkPositions.size(); i++){
            ChunkPosition chunkpos = SyntheticChunkPositions.get(i);
//            chunkpos.getEndPos()
            Vec3d pos = chunkpos.getCenterPos();
            if(chunks.contains(pos)) continue;
            if(AlreadyChosenChunks.contains(chunkpos)) continue;
            double distance = XZDistance(pos, origin);
            if(distance > 32) continue;

            chunks.add(pos);
            AlreadyChosenChunks.add(chunkpos);

            SearchNearbyChunks(chunks, pos);
        }
    }











    public static final List<Block> DEFAULT_SYNTHETIC_BLOCKS = List.of(
        Blocks.CRAFTING_TABLE,
        Blocks.FURNACE,
//        Blocks.CHEST,
        Blocks.ENDER_CHEST,
        Blocks.NETHER_PORTAL
    );

    public static final List<Block> DEFAULT_NATURAL_BLOCKS = List.of(
        Blocks.AIR,
        Blocks.CAVE_AIR,
        Blocks.VOID_AIR,

        Blocks.GRASS_BLOCK,
        Blocks.SHORT_GRASS,
        Blocks.TALL_GRASS,
        Blocks.SEAGRASS,
        Blocks.TALL_SEAGRASS,
        Blocks.KELP,
        Blocks.KELP_PLANT,
        Blocks.CLAY,

        Blocks.VINE,
        Blocks.BAMBOO,
        Blocks.FERN,
        Blocks.LARGE_FERN,
        Blocks.LILAC,
        Blocks.LILY_PAD,
        Blocks.LILY_OF_THE_VALLEY,
        Blocks.DANDELION,
        Blocks.POPPY,
        Blocks.BLUE_ORCHID,
        Blocks.ALLIUM,
        Blocks.MUSHROOM_STEM,
        Blocks.RED_MUSHROOM,
        Blocks.BROWN_MUSHROOM,
        Blocks.RED_MUSHROOM_BLOCK,
        Blocks.BROWN_MUSHROOM_BLOCK,
        Blocks.SUGAR_CANE,
        Blocks.PEONY,
        Blocks.ROSE_BUSH,
        Blocks.AZURE_BLUET,
        Blocks.RED_TULIP,
        Blocks.ORANGE_TULIP,
        Blocks.WHITE_TULIP,
        Blocks.CORNFLOWER,
        Blocks.PINK_TULIP,
        Blocks.BEE_NEST,
        Blocks.OXEYE_DAISY,
        Blocks.PUMPKIN,
        Blocks.MELON,
        Blocks.PUMPKIN,
        Blocks.PINK_PETALS,
//        Block

        Blocks.MOSS_BLOCK,
        Blocks.MOSS_CARPET,
        Blocks.AZALEA,
        Blocks.FLOWERING_AZALEA,
        Blocks.FLOWERING_AZALEA_LEAVES,
        Blocks.CAVE_VINES,
        Blocks.CAVE_VINES_PLANT,
        Blocks.SPORE_BLOSSOM,
        Blocks.AZALEA_LEAVES,
        Blocks.ROOTED_DIRT,
        Blocks.SWEET_BERRY_BUSH,

        Blocks.PACKED_ICE,
        Blocks.ICE,
        Blocks.SNOW,
        Blocks.SNOW_BLOCK,
        Blocks.MAGMA_BLOCK,
        Blocks.OBSIDIAN,
        Blocks.COARSE_DIRT,
        Blocks.PODZOL,
        Blocks.POWDER_SNOW,
        Blocks.BLUE_ICE,

        Blocks.POLISHED_TUFF,
        Blocks.CHISELED_TUFF,
        Blocks.WAXED_CUT_COPPER,
        Blocks.WAXED_CUT_COPPER_SLAB,
        Blocks.WAXED_WEATHERED_COPPER_BULB,
        Blocks.WAXED_CHISELED_COPPER,
        Blocks.WAXED_EXPOSED_COPPER_BULB,
        Blocks.WAXED_COPPER_GRATE,
        Blocks.WAXED_OXIDIZED_COPPER_TRAPDOOR,
        Blocks.TUFF_BRICKS,
        Blocks.CHISELED_TUFF_BRICKS,
        Blocks.TRIAL_SPAWNER,
        Blocks.VAULT,
        Blocks.WAXED_COPPER_BLOCK,
        Blocks.WAXED_COPPER_BULB,
        Blocks.WAXED_OXIDIZED_COPPER,
        Blocks.WAXED_OXIDIZED_COPPER_BULB,
        Blocks.WAXED_OXIDIZED_CUT_COPPER,
        Blocks.WAXED_OXIDIZED_CUT_COPPER_STAIRS,

        Blocks.LAVA,
        Blocks.WATER,
        Blocks.BUBBLE_COLUMN,

        Blocks.DIRT,
        Blocks.STONE,
        Blocks.ANDESITE,
        Blocks.DIORITE,
        Blocks.GRANITE,
        Blocks.TUFF,
        Blocks.GRAVEL,
        Blocks.DIRT_PATH,
        Blocks.MYCELIUM,
        Blocks.DEAD_BUSH,

        Blocks.INFESTED_STONE,
        Blocks.INFESTED_DEEPSLATE,

        Blocks.AMETHYST_BLOCK,
        Blocks.AMETHYST_CLUSTER,
        Blocks.BUDDING_AMETHYST,
        Blocks.LARGE_AMETHYST_BUD,
        Blocks.MEDIUM_AMETHYST_BUD,
        Blocks.SMALL_AMETHYST_BUD,

        Blocks.SPAWNER,
        Blocks.COBBLESTONE,
        Blocks.MOSSY_COBBLESTONE,

//        Blocks.OAK_PLANKS,
//        Blocks.CHAIN,
//        Blocks.COBWEB,
//        Blocks.OAK_FENCE,
        Blocks.RAIL,



        Blocks.DEEPSLATE,
        Blocks.DRIPSTONE_BLOCK,
        Blocks.POINTED_DRIPSTONE,
        Blocks.SMALL_DRIPLEAF,
        Blocks.BIG_DRIPLEAF,
        Blocks.BIG_DRIPLEAF_STEM,
        Blocks.SCULK_CATALYST,
        Blocks.SCULK_SENSOR,
        Blocks.SCULK_VEIN,
        Blocks.SCULK,
        Blocks.SMOOTH_BASALT,
        Blocks.CALCITE,
        Blocks.GLOW_LICHEN,

        Blocks.COAL_ORE,
        Blocks.DEEPSLATE_COAL_ORE,
        Blocks.COPPER_ORE,
        Blocks.DEEPSLATE_COPPER_ORE,
        Blocks.DIAMOND_ORE,
        Blocks.DEEPSLATE_DIAMOND_ORE,
        Blocks.EMERALD_ORE,
        Blocks.DEEPSLATE_EMERALD_ORE,
        Blocks.GOLD_ORE,
        Blocks.DEEPSLATE_GOLD_ORE,
        Blocks.IRON_ORE,
        Blocks.DEEPSLATE_IRON_ORE,
        Blocks.LAPIS_ORE,
        Blocks.DEEPSLATE_LAPIS_ORE,
        Blocks.REDSTONE_ORE,
        Blocks.DEEPSLATE_REDSTONE_ORE,

        Blocks.RAW_IRON_BLOCK,
        Blocks.RAW_COPPER_BLOCK,

        Blocks.SAND,
        Blocks.RED_SAND,
        Blocks.SANDSTONE,

        Blocks.ACACIA_LOG,
        Blocks.BIRCH_LOG,
        Blocks.CHERRY_LOG,
        Blocks.DARK_OAK_LOG,
        Blocks.MANGROVE_LOG,
        Blocks.OAK_LOG,
        Blocks.JUNGLE_LOG,
        Blocks.PALE_OAK_LOG,
        Blocks.SPRUCE_LOG,

        Blocks.ACACIA_LEAVES,
        Blocks.BIRCH_LEAVES,
        Blocks.CHERRY_LEAVES,
        Blocks.DARK_OAK_LEAVES,
        Blocks.MANGROVE_LEAVES,
        Blocks.OAK_LEAVES,
        Blocks.JUNGLE_LEAVES,
        Blocks.PALE_OAK_LEAVES,
        Blocks.SPRUCE_LEAVES,

        Blocks.BEDROCK
    );



}
