package net.minecraft.src;

import com.pclewis.mcpatcher.mod.CTMUtils;
import com.pclewis.mcpatcher.mod.RenderPass;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.lwjgl.opengl.GL11;

//Spout Start
import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.Block;
import net.minecraft.src.Chunk;
import net.minecraft.src.Entity;
import net.minecraft.src.ICamera;
import net.minecraft.src.MathHelper;
import net.minecraft.src.RenderBlocks;
import net.minecraft.src.RenderItem;
import net.minecraft.src.Tessellator;
import net.minecraft.src.TileEntity;
import net.minecraft.src.TileEntityRenderer;
import net.minecraft.src.World;
import org.newdawn.slick.opengl.Texture;
import org.spoutcraft.client.SpoutClient;
import org.spoutcraft.client.io.CustomTextureManager;
import org.spoutcraft.client.item.SpoutItem;
import org.spoutcraft.api.Spoutcraft;
import org.spoutcraft.api.block.design.GenericBlockDesign;
import org.spoutcraft.api.material.CustomBlock;
import org.spoutcraft.api.material.MaterialData;

import net.minecraft.client.Minecraft;
//Spout End

public class WorldRenderer {

	/** Reference to the World object. */
	public World worldObj;
	private int glRenderList = -1;
	private static Tessellator tessellator = Tessellator.instance;
	public static int chunksUpdated = 0;
	public int posX;
	public int posY;
	public int posZ;

	/** Pos X minus */
	public int posXMinus;

	/** Pos Y minus */
	public int posYMinus;

	/** Pos Z minus */
	public int posZMinus;

	/** Pos X clipped */
	public int posXClip;

	/** Pos Y clipped */
	public int posYClip;

	/** Pos Z clipped */
	public int posZClip;
	public boolean isInFrustum = false;

	/** Should this renderer skip this render pass */
	public boolean[] skipRenderPass = new boolean[4]; //Spout was 4

	/** Pos X plus */
	public int posXPlus;

	/** Pos Y plus */
	public int posYPlus;

	/** Pos Z plus */
	public int posZPlus;

	/** Boolean for whether this renderer needs to be updated or not */
	public boolean needsUpdate;

	/** Axis aligned bounding box */
	public AxisAlignedBB rendererBoundingBox;

	/** Chunk index */
	public int chunkIndex;

	/** Is this renderer visible according to the occlusion query */
	public boolean isVisible = true;

	/** Is this renderer waiting on the result of the occlusion query */
	public boolean isWaitingOnOcclusionQuery;

	/** OpenGL occlusion query */
	public int glOcclusionQuery;

	/** Is the chunk lit */
	public boolean isChunkLit;
	private boolean isInitialized = false;

	/** All the tile entities that have special rendering code for this chunk */
	public List tileEntityRenderers = new ArrayList();
	private List tileEntities;

	/** Bytes sent to the GPU */
	private int bytesDrawn;

	public WorldRenderer(World par1World, List par2List, int par3, int par4, int par5, int par6) {
		this.worldObj = par1World;
		this.tileEntities = par2List;
		this.glRenderList = par6;
		this.posX = -999;
		this.setPosition(par3, par4, par5);
		this.needsUpdate = false;
	}

	/**
	 * Sets a new position for the renderer and setting it up so it can be reloaded with the new data for that position
	 */
	public void setPosition(int par1, int par2, int par3) {
		if (par1 != this.posX || par2 != this.posY || par3 != this.posZ) {
			this.setDontDraw();
			this.posX = par1;
			this.posY = par2;
			this.posZ = par3;
			this.posXPlus = par1 + 8;
			this.posYPlus = par2 + 8;
			this.posZPlus = par3 + 8;
			this.posXClip = par1 & 1023;
			this.posYClip = par2;
			this.posZClip = par3 & 1023;
			this.posXMinus = par1 - this.posXClip;
			this.posYMinus = par2 - this.posYClip;
			this.posZMinus = par3 - this.posZClip;
			float var4 = 6.0F;
			this.rendererBoundingBox = AxisAlignedBB.getBoundingBox((double)((float)par1 - var4), (double)((float)par2 - var4), (double)((float)par3 - var4), (double)((float)(par1 + 16) + var4), (double)((float)(par2 + 16) + var4), (double)((float)(par3 + 16) + var4));
			GL11.glNewList(this.glRenderList + 4, GL11.GL_COMPILE);
			RenderItem.renderAABB(AxisAlignedBB.getAABBPool().addOrModifyAABBInPool((double)((float)this.posXClip - var4), (double)((float)this.posYClip - var4), (double)((float)this.posZClip - var4), (double)((float)(this.posXClip + 16) + var4), (double)((float)(this.posYClip + 16) + var4), (double)((float)(this.posZClip + 16) + var4)));
			GL11.glEndList();
			this.markDirty();
		}
	}

	private void setupGLTranslation() {
		GL11.glTranslatef((float)this.posXClip, (float)this.posYClip, (float)this.posZClip);
	}

	/**
	 * Will update this chunk renderer
	 */
	
	//TODO: Is it possible to use this Cache system?
	/*
	public void updateRenderer() {
		CTMUtils.start();

		if (!this.needsUpdate) {
			CTMUtils.finish();
		} else {
			this.needsUpdate = false;
			int var1 = this.posX;
			int var2 = this.posY;
			int var3 = this.posZ;
			int var4 = this.posX + 16;
			int var5 = this.posY + 16;
			int var6 = this.posZ + 16;

			for (int var7 = 0; var7 < 4; ++var7) {
				this.skipRenderPass[var7] = true;
			}

			Chunk.isLit = false;
			HashSet var21 = new HashSet();
			var21.addAll(this.tileEntityRenderers);
			this.tileEntityRenderers.clear();
			byte var8 = 1;
			ChunkCache var9 = new ChunkCache(this.worldObj, var1 - var8, var2 - var8, var3 - var8, var4 + var8, var5 + var8, var6 + var8);

			if (!var9.extendedLevelsInChunkCache()) {
				++chunksUpdated;
				RenderBlocks var10 = new RenderBlocks(var9);
				this.bytesDrawn = 0;

				for (int var11 = 0; var11 < 4; ++var11) {
					boolean var12 = false;
					boolean var13 = false;
					boolean var14 = false;
					RenderPass.start(var11);

					for (int var15 = var2; var15 < var5; ++var15) {
						for (int var16 = var3; var16 < var6; ++var16) {
							for (int var17 = var1; var17 < var4; ++var17) {
								int var18 = var9.getBlockId(var17, var15, var16);

								if (var18 > 0) {
									if (!var14) {
										var14 = true;
										GL11.glNewList(this.glRenderList + var11, GL11.GL_COMPILE);
										GL11.glPushMatrix();
										this.setupGLTranslation();
										float var19 = 1.000001F;
										GL11.glTranslatef(-8.0F, -8.0F, -8.0F);
										GL11.glScalef(var19, var19, var19);
										GL11.glTranslatef(8.0F, 8.0F, 8.0F);
										tessellator.startDrawingQuads();
										tessellator.setTranslation((double)(-this.posX), (double)(-this.posY), (double)(-this.posZ));
									}

									Block var23 = Block.blocksList[var18];

									if (var23 != null) {
										if (var11 == 0 && var23.hasTileEntity()) {
											TileEntity var20 = var9.getBlockTileEntity(var17, var15, var16);

											if (TileEntityRenderer.instance.hasSpecialRenderer(var20)) {
												this.tileEntityRenderers.add(var20);
											}
										}

										int var24 = RenderPass.getBlockRenderPass(var23);

										if (var24 != var11) {
											var12 = true;
										} else if (var24 == var11) {
											var13 |= var10.renderBlockByRenderType(var23, var17, var15, var16);
										}
									}
								}
							}
						}
					}

					if (var14) {
						this.bytesDrawn += tessellator.draw();
						GL11.glPopMatrix();
						GL11.glEndList();
						tessellator.setTranslation(0.0D, 0.0D, 0.0D);
					} else {
						var13 = false;
					}

					if (var13) {
						this.skipRenderPass[var11] = false;
					}
				}
			}

			HashSet var22 = new HashSet();
			var22.addAll(this.tileEntityRenderers);
			var22.removeAll(var21);
			this.tileEntities.addAll(var22);
			var21.removeAll(this.tileEntityRenderers);
			this.tileEntities.removeAll(var21);
			this.isChunkLit = Chunk.isLit;
			this.isInitialized = true;
			CTMUtils.finish();
		}		
	}	 
   */
	
	public void updateRenderer() {
		// Spout Start
		CTMUtils.start();

		if (!this.needsUpdate) {
			CTMUtils.finish();
			System.out.println("SpoutDebug: Chunk Not Updated");
		} else {
			++chunksUpdated;
			int x = this.posX;
			int y = this.posY;
			int z = this.posZ;
			int sizeXOffset = this.posX + 16;
			int sizeYOffset = this.posY + 16;
			int sizeZOffset = this.posZ + 16;

			for(int renderPass = 0; renderPass < skipRenderPass.length; ++renderPass) {
				this.skipRenderPass[renderPass] = true;
			}

			Chunk.isLit = false;
			HashSet tileRenderers = new HashSet();
			tileRenderers.addAll(this.tileEntityRenderers);
			this.tileEntityRenderers.clear();
			//ChunkCache chunkCache = new ChunkCache(this.worldObj, x - 1, y - 1, z - 1, sizeXOffset + 1, sizeYOffset + 1, sizeZOffset + 1);
			RenderBlocks blockRenderer = new RenderBlocks(worldObj);
			List<String> hitTextures = new ArrayList<String>();
			List<String> hitTexturesPlugins = new ArrayList<String>();
			int currentTexture = 0;
			Minecraft game = SpoutClient.getHandle();
			hitTextures.add("/terrain.png");
			hitTexturesPlugins.add("");
			int defaultTexture = game.renderEngine.getTexture("/terrain.png");
			game.renderEngine.bindTexture(defaultTexture);

			short[] customBlockIds = worldObj.world.getChunkAt(posX, posY, posZ).getCustomBlockIds();
			byte[] customBlockData = worldObj.world.getChunkAt(posX, posY, posZ).getCustomBlockData();

			blockRenderer.customIds = customBlockIds;

			int limit = skipRenderPass.length;
			
			for (int renderPass = 0; renderPass < limit; ++renderPass) { // Spout - 3 passes for shaders, 2 without

				boolean skipRenderPass = false;
				boolean rendered = false;
				boolean drawBlock = false;

				if (!drawBlock) {
					drawBlock = true;
					GL11.glNewList(this.glRenderList + renderPass, GL11.GL_COMPILE);
					GL11.glPushMatrix();
					this.setupGLTranslation();
					GL11.glTranslatef(-8F, -8F, -8F);
					GL11.glScalef(1F, 1F, 1F);
					GL11.glTranslatef(8F, 8F, 8F);
					tessellator.startDrawingQuads();
					tessellator.setTranslation((double)(-this.posX), (double)(-this.posY), (double)(-this.posZ));
				}

				game.renderEngine.bindTexture(defaultTexture);
				for (currentTexture = 0; currentTexture < hitTextures.size(); currentTexture++) {	
					int texture = defaultTexture;
					//First pass (currentTexture == 0) is for the default terrain.png texture. Subsquent passes are for custom textures
					//This design is to avoid unnessecary glBindTexture calls.
					if(currentTexture > 0) {
						Texture customTexture = CustomTextureManager.getTextureFromUrl(hitTexturesPlugins.get(currentTexture), hitTextures.get(currentTexture));
						if (customTexture == null) {
							customTexture = CustomTextureManager.getTextureFromJar("/res/block/spout.png");
						}
						if (customTexture != null) {
							texture = customTexture.getTextureID();
							game.renderEngine.bindTexture(texture);
							if(texture <= 0) {
								texture = defaultTexture;
							}
						}
					}

					if(tessellator.texture != texture){
						tessellator.draw();
						tessellator.texture = texture;
						tessellator.startDrawingQuads();
					}

					float[] oldBounds = new float[6];

					//The x,y,z order is important, don't change!
					for (int dx = x; dx < sizeXOffset; ++dx) {
						for (int dz = z; dz < sizeZOffset; ++dz) {
							for (int dy = y; dy < sizeYOffset; ++dy) {
								int id = worldObj.getBlockId(dx, dy, dz);
								if (id > 0) {
									String customTexture = null;
									String customTextureAddon = null;
									GenericBlockDesign design = null;

									CustomBlock mat = null;
									if (customBlockIds != null) {
										int key = ((dx & 0xF) << 12) | ((dz & 0xF) << 8) | (dy & 0xFF);
										if (customBlockIds[key] != 0) {
											mat = MaterialData.getCustomBlock(customBlockIds[key]);
											if (mat != null) {
												design = (GenericBlockDesign) mat.getBlockDesign(customBlockData == null ? 0 : customBlockData[key]);
											}
										}
									}

									if (design != null) {
										customTexture = design.getTexureURL();
										customTextureAddon = design.getTextureAddon();
									}


									if(customTexture != null){
										boolean found = false;

										//Search for the custom texture in our list
										for(int i = 0; i < hitTextures.size(); i++){
											if(hitTextures.get(i).equals(customTexture) && hitTexturesPlugins.get(i).equals(customTextureAddon)) {
												found = true;
												break;
											}
										}
										//If it is not already accounted for, add it so we do an additional pass for it.
										if(!found) {
											hitTextures.add(customTexture);
											hitTexturesPlugins.add(customTextureAddon);
										}

										//Do not render if we are using a different texture than the current one
										if(!hitTextures.get(currentTexture).equals(customTexture) || !hitTexturesPlugins.get(currentTexture).equals(customTextureAddon)) {
											continue;
										}
									}
									//Do not render if we are not using the terrain.png and can't find a valid texture for this custom block
									else if (currentTexture != 0) {
										continue;
									}

									Block block = Block.blocksList[id];
									if (renderPass == 0 && block.hasTileEntity()) {
										TileEntity var20 = worldObj.getBlockTileEntity(dx, dy, dz);
										if (TileEntityRenderer.instance.hasSpecialRenderer(var20)) {
											this.tileEntityRenderers.add(var20);
										}
									}

									//Determine which pass this block needs to be rendered on
									int blockRenderPass = block.getRenderBlockPass();
									if (design != null) {
										blockRenderPass = design.getRenderPass();
									}

									if (blockRenderPass != renderPass) {
										skipRenderPass = true;
									}
									else {										
										if (design != null) {
											oldBounds[0] = (float) block.minX;
											oldBounds[1] = (float) block.minY;
											oldBounds[2] = (float) block.minZ;
											oldBounds[3] = (float) block.maxX;
											oldBounds[4] = (float) block.maxY;
											oldBounds[5] = (float) block.maxZ;
											block.setBlockBounds(design.getLowXBound(), design.getLowYBound(), design.getLowZBound(), design.getHighXBound(), design.getHighYBound(), design.getHighZBound());
											rendered |= design.renderBlock(mat, dx, dy, dz);
											block.setBlockBounds(oldBounds[0], oldBounds[1], oldBounds[2], oldBounds[3], oldBounds[4], oldBounds[5]);
										}
										else {
											rendered |= blockRenderer.renderBlockByRenderType(block, dx, dy, dz);
										}
									}
								}
							}
						}
					}
				}

				if (drawBlock) {
					tessellator.draw();
					tessellator.texture = 0;
					GL11.glPopMatrix();
					GL11.glEndList();
					game.renderEngine.bindTexture(defaultTexture);
					tessellator.setTranslation(0.0D, 0.0D, 0.0D);
				} else {
					rendered = false;
				}

				if (rendered) {
					this.skipRenderPass[renderPass] = false;
				}

				if (!skipRenderPass) {
					break;
				}
			}

			HashSet var24 = new HashSet();
			var24.addAll(this.tileEntityRenderers);
			var24.removeAll(tileRenderers);
			this.tileEntities.addAll(var24);
			tileRenderers.removeAll(this.tileEntityRenderers);			
			// Spout End
			this.tileEntities.removeAll(tileRenderers);
			this.isChunkLit = Chunk.isLit;
			this.isInitialized = true;
			blockRenderer.customIds = null;
			CTMUtils.finish();
		}
	}



	/**
	 * Returns the distance of this chunk renderer to the entity without performing the final normalizing square root, for
	 * performance reasons.
	 */
	public float distanceToEntitySquared(Entity par1Entity) {
		float var2 = (float)(par1Entity.posX - (double)this.posXPlus);
		float var3 = (float)(par1Entity.posY - (double)this.posYPlus);
		float var4 = (float)(par1Entity.posZ - (double)this.posZPlus);
		return var2 * var2 + var3 * var3 + var4 * var4;
	}

	/**
	 * When called this renderer won't draw anymore until its gets initialized again
	 */
	public void setDontDraw() {
		for (int var1 = 0; var1 < skipRenderPass.length; ++var1) { //Spout
			this.skipRenderPass[var1] = true;
		}

		this.isInFrustum = false;
		this.isInitialized = false;
	}

	public void stopRendering() {
		this.setDontDraw();
		this.worldObj = null;
	}

	/**
	 * Takes in the pass the call list is being requested for. Args: renderPass
	 */
	public int getGLCallListForPass(int par1) {
		return !this.isInFrustum ? -1 : (!this.skipRenderPass[par1] ? this.glRenderList + par1 : -1);
	}

	public void updateInFrustum(ICamera par1ICamera) {
		this.isInFrustum = par1ICamera.isBoundingBoxInFrustum(this.rendererBoundingBox);
	}

	/**
	 * Renders the occlusion query GL List
	 */
	public void callOcclusionQueryList() {
		GL11.glCallList(this.glRenderList + 4);
	}

	/**
	 * Checks if all render passes are to be skipped. Returns false if the renderer is not initialized
	 */
	public boolean skipAllRenderPasses() {
		return !this.isInitialized ? false : RenderPass.skipAllRenderPasses(this.skipRenderPass);
	}

	/**
	 * Marks the current renderer data as dirty and needing to be updated.
	 */
	public void markDirty() {
		this.needsUpdate = true;
	}
}
