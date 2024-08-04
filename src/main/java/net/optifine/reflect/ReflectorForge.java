package net.optifine.reflect;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.EntityLiving;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapData;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReflectorForge {

    public static void FMLClientHandler_trackBrokenTexture(ResourceLocation loc, String message) {
    }

    public static void FMLClientHandler_trackMissingTexture(ResourceLocation loc) {
    }

    public static void putLaunchBlackboard(String key, Object value) {
    }

    public static boolean renderFirstPersonHand(RenderGlobal renderGlobal, float partialTicks, int pass) {
        return false;
    }

    public static InputStream getOptiFineResourceStream(String path) {
        return null;
    }

    public static boolean blockHasTileEntity(IBlockState state) {
        Block block = state.getBlock();
        return block.hasTileEntity();
    }

    public static boolean isItemDamaged(ItemStack stack) {
        return stack.isItemDamaged();
    }

    public static boolean armorHasOverlay(ItemArmor itemArmor, ItemStack itemStack) {
        int i = itemArmor.getColor(itemStack);
        return i != -1;
    }

    public static MapData getMapData(ItemMap itemMap, ItemStack stack, World world) {
        return itemMap.getMapData(stack, world);
    }

    public static String[] getForgeModIds() {
        return new String[0];
    }

    public static boolean doSpecialSpawn(EntityLiving entityliving, World world, float x, int y, float z) {
        return false;
    }
}
