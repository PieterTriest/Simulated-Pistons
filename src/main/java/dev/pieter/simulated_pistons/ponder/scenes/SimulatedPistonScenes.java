package dev.pieter.simulated_pistons.ponder.scenes;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import dev.pieter.simulated_pistons.content.SimulatedPistonBlock;
import dev.pieter.simulated_pistons.content.SimulatedPistonBlockEntity;
import dev.pieter.simulated_pistons.content.SimulatedPistonLinkBlock;
import dev.pieter.simulated_pistons.content.SimulatedPistonLinkBlockEntity;
import dev.pieter.simulated_pistons.index.SPBlocks;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.OverlayInstructions;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.api.scene.SelectionUtil;
import net.createmod.ponder.api.scene.VectorUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class SimulatedPistonScenes {
    private static void setPistonCogKineticSpeed(final CreateSceneBuilder scene, final SceneBuildingUtil util, final BlockPos pistonPos, final float rpm) {
        scene.world().modifyBlock(pistonPos, state -> state.setValue(SimulatedPistonBlock.ASSEMBLED, true), false);
        scene.world().modifyBlockEntityNBT(util.select().position(pistonPos), SimulatedPistonBlockEntity.class, nbt -> {
            nbt.getCompound("PistonCog").putFloat("Speed", rpm);
        });
    }

    public static void intro(final SceneBuilder builder, final SceneBuildingUtil util) {
        final CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        final CreateSceneBuilder.WorldInstructions world = scene.world();
        final OverlayInstructions overlay = scene.overlay();
        final SelectionUtil select = util.select();
        final VectorUtil vector = util.vector();

        scene.title("simulated_piston_ponder_scene_1", "Moving Structures using the Simulated Piston");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.setSceneOffsetY(-0.5f);

        final BlockPos piston = new BlockPos(2, 1, 2);
        final Selection pistonSelection = select.position(piston);
        final Selection sideInputCog = select.position(5, 0, 3);
        final Selection platformLargeCog = select.position(4, 1, 3);
        final Selection platformSmallCog = select.position(3, 1, 2);
        final Selection transmissionCogs = platformLargeCog.add(platformSmallCog);
        final Selection attachmentPlatform = select.fromTo(0, 2, 2, 4, 3, 2);
        final Selection attachmentRedstoneDecor = select.fromTo(0, 4, 2, 4, 4, 2);

        scene.idle(10);
        world.showSection(sideInputCog, Direction.UP);
        scene.idle(10);
        world.showSection(transmissionCogs, Direction.DOWN);
        scene.idle(10);
        world.showSection(pistonSelection, Direction.DOWN);
        scene.idle(20);

        overlay.showText(80)
                .text("Simulated Pistons attach to the block in front of their head")
                .pointAt(vector.topOf(piston))
                .colored(PonderPalette.GREEN)
                .placeNearTarget();

        final AABB headFace = AABB.unitCubeFromLowerCorner(Vec3.atLowerCornerOf(piston.above()));
        overlay.chaseBoundingBoxOutline(PonderPalette.GREEN, headFace, headFace, 90);
        scene.idle(70);

        final Selection centerColumn = select.fromTo(2, 2, 2, 2, 3, 2);
        final ElementLink<WorldSectionElement> contraption = world.showIndependentSection(select.position(2, 2, 2), Direction.DOWN);
        scene.idle(10);
        world.showSectionAndMerge(select.position(2, 3, 2), Direction.DOWN, contraption);
        scene.idle(10);
        scene.effects().superGlue(piston.above(), Direction.DOWN, true);
        world.showSectionAndMerge(attachmentPlatform.substract(centerColumn), Direction.DOWN, contraption);
        scene.idle(10);
        world.showSectionAndMerge(attachmentRedstoneDecor, Direction.DOWN, contraption);

        scene.idle(15);
        overlay.showControls(vector.centerOf(4, 2, 2), Pointing.RIGHT, 40)
                .withItem(honeyGlueStack())
                .rightClick();

        final AABB glueBox = new AABB(util.grid().at(4, 2, 2));
        overlay.chaseBoundingBoxOutline(PonderPalette.OUTPUT, glueBox, glueBox, 1);
        overlay.chaseBoundingBoxOutline(PonderPalette.OUTPUT, glueBox, glueBox.expandTowards(-4, 1, 0), 80);

        scene.idle(15);
        overlay.showText(70)
                .text("Use Super Glue or Honey Glue to select the blocks moved by the piston")
                .pointAt(vector.centerOf(0, 2, 2))
                .colored(PonderPalette.OUTPUT)
                .attachKeyFrame()
                .placeNearTarget();

        scene.idle(80);
        world.setKineticSpeed(sideInputCog, -16);
        world.setKineticSpeed(platformLargeCog, 16);
        world.setKineticSpeed(platformSmallCog, -32);
        world.setKineticSpeed(pistonSelection, 32);
        setPistonCogKineticSpeed(scene, util, piston, 32);
        final BlockPos hiddenPistonLink = new BlockPos(5, 0, 4);
        world.setBlock(
                hiddenPistonLink,
                SPBlocks.SIMULATED_PISTON_LINK.get().defaultBlockState().setValue(SimulatedPistonLinkBlock.FACING, Direction.UP),
                false
        );
        world.modifyBlockEntityNBT(select.position(hiddenPistonLink), SimulatedPistonLinkBlockEntity.class, nbt -> {
            nbt.putInt("ChainLength", 1);
            nbt.putFloat("ParentExtension", 1);
        });
        final ElementLink<WorldSectionElement> pistonLink = world.showIndependentSectionImmediately(select.position(hiddenPistonLink));
        world.moveSection(pistonLink, Vec3.atLowerCornerOf(piston.subtract(hiddenPistonLink)), 0);

        overlay.showText(80)
                .text("When powered, it assembles the selected blocks and pushes them forward")
                .pointAt(vector.topOf(piston))
                .attachKeyFrame()
                .placeNearTarget();

        world.moveSection(contraption, new Vec3(0, 1, 0), 80);
        world.moveSection(pistonLink, new Vec3(0, 1, 0), 80);
        scene.idle(90);

        world.setKineticSpeed(sideInputCog.add(transmissionCogs).add(pistonSelection), 0);
        setPistonCogKineticSpeed(scene, util, piston, 0);
        scene.markAsFinished();
    }

    private static ItemStack honeyGlueStack() {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("simulated", "honey_glue")));
    }
}
