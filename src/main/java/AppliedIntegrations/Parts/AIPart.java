package AppliedIntegrations.Parts;
import AppliedIntegrations.Utils.EffectiveSide;
import AppliedIntegrations.Utils.AILog;
import AppliedIntegrations.Utils.AIGridNodeInventory;
import AppliedIntegrations.Utils.AIUtils;
import AppliedIntegrations.grid.AEPartGridBlock;
import AppliedIntegrations.AppliedIntegrations;
import appeng.api.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import appeng.api.config.SecurityPermissions;
import appeng.api.networking.IGrid;
import appeng.api.networking.events.MENetworkChannelsChanged;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.implementations.IPowerChannelState;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.networking.events.MENetworkPowerStatusChange;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.parts.BusSupport;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartHost;
import appeng.api.parts.IPartRenderHelper;
import appeng.api.parts.PartItemStack;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.util.AECableType;
import appeng.api.util.AEColor;
import appeng.api.util.DimensionalCoord;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import AppliedIntegrations.Render.TextureManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import AppliedIntegrations.AIConfigOPT;
import static AppliedIntegrations.AppliedIntegrations.getNewID;

/**
 * @Author Azazell
 */
public abstract class AIPart
		implements IPart, IGridHost, IActionHost, IPowerChannelState
{
	private final static String NBT_KEY_OWNER = "Owner";

	protected final static int ACTIVE_FACE_BRIGHTNESS = 0xD000D0;


	protected final static int ACTIVE_TERMINAL_LIGHT_LEVEL = 9;

	private final SecurityPermissions[] interactionPermissions;
	public final int uniquePacketID;

	protected IPartHost host;

	protected TileEntity hostTile;
	private ForgeDirection cableSide;


	private boolean isActive;
	private boolean isPowered;

	private int ownerID;
	public final ItemStack associatedItem;
	protected IGridNode node;
	protected AEPartGridBlock gridBlock;

	protected int capacity = AIConfigOPT.interfaceMaxStorage;
	protected int maxTransfer = 500000;

	public int getX(){
		if(this.getHost()!=null)
			return this.getHost().getLocation().x;
		return 0;
	}
	public int getY(){
		if(this.getHost()!=null)
			return this.getHost().getLocation().y;
		return 0;
	}
	public int getZ(){
		if(this.getHost()!=null)
			return this.getHost().getLocation().z;
		return 0;
	}
	public AIPart(final PartEnum associatedPart, final SecurityPermissions ... interactionPermissions )
	{
		// Set the associated item
		this.associatedItem = associatedPart.getStack();

		this.uniquePacketID = getNewID();
		// Set clearance
		if( ( interactionPermissions != null ) && ( interactionPermissions.length > 0 ) )
		{
			this.interactionPermissions = interactionPermissions;
		}
		else
		{
			this.interactionPermissions = null;
		}
			// Create the grid block
		if(EffectiveSide.isServerSide()) {
			this.gridBlock = new AEPartGridBlock(this);
		}else{
			this.gridBlock = null;
		}
	}

	private void updateStatus()
	{
		// Ignored client side
		if( EffectiveSide.isClientSide() )
		{
			return;
		}

		// Do we have a node?
		if( this.node != null )
		{
			// Get the active state
			boolean currentlyActive = this.node.isActive();

			// Has that state changed?
			if( currentlyActive != this.isActive )
			{
				// Set our active state
				this.isActive = currentlyActive;

				// Mark the host for an update
				this.host.markForUpdate();
			}
		}

		// Fire the neighbor changed event
		this.onNeighborChanged();
	}
	protected abstract AIGridNodeInventory getUpgradeInventory();

	protected boolean doesPlayerHavePermission( final EntityPlayer player, final SecurityPermissions permission )
	{
		if( EffectiveSide.isClientSide() )
		{
			return false;
		}

		// Get the security grid
		ISecurityGrid sGrid = this.gridBlock.getSecurityGrid();

		// Did we get the grid?
		if( sGrid == null )
		{
			// No security grid to check against.
			return false;
		}

		// Return the permission
		return sGrid.hasPermission( player, permission );
	}



	protected boolean doesPlayerHavePermission( final int playerID, final SecurityPermissions permission )
	{
		if( EffectiveSide.isClientSide() )
		{
			return false;
		}

		// Get the security grid
		ISecurityGrid sGrid = this.gridBlock.getSecurityGrid();

		// Did we get the grid?
		if( sGrid == null )
		{
			// No security grid to check against.
			return false;
		}

		// Return the permission
		return sGrid.hasPermission( playerID, permission );
	}


	protected TileEntity getFacingTile()
	{
		if( this.hostTile == null )
		{
			return null;
		}

		// Get the world
		World world = this.hostTile.getWorldObj();

		// Get our location
		int x = this.hostTile.xCoord;
		int y = this.hostTile.yCoord;
		int z = this.hostTile.zCoord;

		// Get the tile entity we are facing
		return world.getTileEntity( x + this.cableSide.offsetX, y + this.cableSide.offsetY, z + this.cableSide.offsetZ );
	}



	@Override
	public void addToWorld()
	{
		// Ignored on client side
		if( EffectiveSide.isClientSide() )
		{
			return;
		}
		this.gridBlock = new AEPartGridBlock(this);
		this.node = AEApi.instance().createGridNode( this.gridBlock );

		// Set the player id
		this.node.setPlayerID( this.ownerID );

		// Update state
		if( ( this.hostTile != null ) && ( this.host != null ) && ( this.hostTile.getWorldObj() != null ) )
		{
			try
			{
				this.node.updateState();
			}
			catch( Exception e )
			{
				AILog.error( e, "Machine (%s) was unable to update it's node. The part may not function correctly",
						this.associatedItem.getDisplayName() );
			}
		}

		// Update the part
		this.updateStatus();
		AILog.info("Machine:" + this.associatedItem.getDisplayName() + " Placed by Player with AE2 ID:" + this.getGridNode().getPlayerID());
	}

	@Override
	public abstract int cableConnectionRenderTo();

	@Override
	public boolean canBePlacedOn( final BusSupport type )
	{
		// Can only be placed on normal cable
		return type == BusSupport.CABLE;
	}

	@Override
	public boolean canConnectRedstone()
	{
		return false;
	}

	@Override
	public IGridNode getActionableNode()
	{
		return this.node;
	}

	@Override
	public abstract void getBoxes( IPartCollisionHelper helper );

	@SideOnly(Side.CLIENT)
	@Override
	public abstract IIcon getBreakingTexture();

	@Override
	public AECableType getCableConnectionType( final ForgeDirection dir )
	{
		return AECableType.SMART;
	}



	@Override
	public void getDrops( final List<ItemStack> drops, final boolean wrenched )
	{
	}

	@Override
	public final IGridNode getExternalFacingNode()
	{
		return null;
	}

	public AEPartGridBlock getGridBlock()
	{
		return this.gridBlock;
	}

	@Override
	public IGridNode getGridNode()
	{
		return this.node;
	}

	@Override
	public IGridNode getGridNode( final ForgeDirection direction )
	{
		return getGridNode();
	}

	public final IPartHost getHost()
	{
		return this.host;
	}


	public final TileEntity getHostTile()
	{
		return this.hostTile;
	}

	public abstract double getIdlePowerUsage();

	@Override
	public ItemStack getItemStack( final PartItemStack type )
	{
		// Get the itemstack
		ItemStack itemStack = this.associatedItem.copy();

		// Save NBT data if the part was wrenched
		if( type == PartItemStack.Wrench )
		{
			// Create the item tag
			NBTTagCompound itemNBT = new NBTTagCompound();

			// Write the data
			this.writeToNBT( itemNBT, PartItemStack.Wrench );

			// Set the tag
			if( !itemNBT.hasNoTags() )
			{
				itemStack.setTagCompound( itemNBT );
			}
		}

		return itemStack;
	}




	public final DimensionalCoord getLocation()
	{
		return new DimensionalCoord( this.hostTile.getWorldObj(), this.hostTile.xCoord, this.hostTile.yCoord, this.hostTile.zCoord );
	}
	public Object getClientGuiElement( final EntityPlayer player ) {return null; }
	public Object getServerGuiElement( final EntityPlayer player ) { return null; }


	public ForgeDirection getSide()
	{
		return this.cableSide;
	}


	public String getUnlocalizedName()
	{
		return this.associatedItem.getUnlocalizedName() + ".name";
	}


	@Override
	public boolean isActive()
	{
		// Are we server side?
		if( EffectiveSide.isServerSide() )
		{
			// Do we have a node?
			if( this.node != null )
			{
				// Get it's activity
				this.isActive = this.node.isActive();
			}
			else
			{
				this.isActive = false;
			}
		}

		return this.isActive;
	}

	@Override
	public boolean isLadder( final EntityLivingBase entity )
	{
		return false;
	}

	@Override
	public abstract int getLightLevel();
	public final boolean isPartUseableByPlayer( final EntityPlayer player )
	{
		if( EffectiveSide.isClientSide() )
		{
			return false;
		}

		// Null check host
		if( ( this.hostTile == null ) || ( this.host == null ) )
		{
			return false;
		}

		// Does the host still exist in the world and the player in range of it?
		if( !AIUtils.canPlayerInteractWith( player, this.hostTile ) )
		{
			return false;
		}

		// Is the part still attached?
		if( this.host.getPart( this.cableSide ) != this )
		{
			return false;
		}

		// Are there any permissions to check?
		if( this.interactionPermissions != null )
		{
			// Get the security grid
			ISecurityGrid sGrid = this.gridBlock.getSecurityGrid();
			if( sGrid == null )
			{
				// Security grid was unaccessible.
				return false;
			}

			// Check each permission
			for( SecurityPermissions perm : this.interactionPermissions )
			{
				if( !sGrid.hasPermission( player, perm ) )
				{
					return false;
				}
			}
		}

		return true;
	}

	@Override
	public boolean isPowered()
	{
		try
		{
			// Server side?
			if( EffectiveSide.isServerSide() && ( this.gridBlock != null ) )
			{
				// Get the energy grid
				IEnergyGrid eGrid = this.gridBlock.getEnergyGrid();
				if( eGrid != null )
				{
					this.isPowered = eGrid.isNetworkPowered();
				}
				else
				{
					this.isPowered = false;
				}
			}
		}
		catch( Exception e )
		{
			// Network unavailable, return cached value.
		}

		return this.isPowered;
	}




	public boolean isReceivingRedstonePower()
	{
		if( this.host != null )
		{
			// Get redstone state
			return this.host.hasRedstone( this.cableSide );
		}
		return false;
	}

	@Override
	public boolean isSolid()
	{
		return false;
	}


	public final void markForSave()
	{
		// Ensure there is a host
		if( this.host != null )
		{
			// Mark
			this.host.markForSave();
		}
	}

	public final void markForUpdate()
	{
		if( this.host != null )
		{
			this.host.markForUpdate();
		}
	}
	public List<String> getWailaBodey(NBTTagCompound tag, List<String> oldList) {
		return oldList;
	}

	public NBTTagCompound getWailaTag(NBTTagCompound tag) {
		return tag;
	}
	@Override
	public boolean onActivate( final EntityPlayer player, final Vec3 position )
	{
		// Is the player sneaking?
		if( player.isSneaking() )
		{
			return false;
		}

		// Is this server side?
		if( EffectiveSide.isServerSide() )
		{
			// Launch the gui
			AppliedIntegrations.launchGui( this, player, this.hostTile.getWorldObj(), this.hostTile.xCoord, this.hostTile.yCoord, this.hostTile.zCoord );
		}
		return true;
	}

	@Override
	public void onEntityCollision( final Entity entity )
	{
	}

	@Override
	public void onNeighborChanged()
	{
	}

	@Override
	public final void onPlacement( final EntityPlayer player, final ItemStack held, final ForgeDirection side )
	{
		// Set the owner
		this.ownerID = AEApi.instance().registries().players().getID( player.getGameProfile() );
	}

	@Override
	public boolean onShiftActivate( final EntityPlayer player, final Vec3 position )
	{
		return false;
	}
	@Override
 public void randomDisplayTick(World world, int x, int y, int z, Random r) {}

	@Override
	public void readFromNBT( final NBTTagCompound data )
	{
		// Read the owner
		if( data.hasKey( AIPart.NBT_KEY_OWNER ) )
		{
			this.ownerID = data.getInteger( AIPart.NBT_KEY_OWNER );
		}
	}

	@SideOnly(Side.CLIENT)
	@Override
	public boolean readFromStream( final ByteBuf stream ) throws IOException
	{
		// Cache old values
		boolean oldActive = this.isActive;
		boolean oldPowered = this.isPowered;

		// Read the new values
		this.isActive = stream.readBoolean();
		this.isPowered = stream.readBoolean();

		// Redraw if they don't match.
		return( ( oldActive != this.isActive ) || ( oldPowered != this.isPowered ) );
	}

	@Override
	public void removeFromWorld()
	{
		if( this.node != null )
		{
			this.node.destroy();
		}
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void renderDynamic( final double x, final double y, final double z, final IPartRenderHelper helper, final RenderBlocks renderer )
	{
		// Ignored
	}

	@SideOnly(Side.CLIENT)
	@Override
	public abstract void renderInventory( IPartRenderHelper helper, RenderBlocks renderer );

	@SideOnly(Side.CLIENT)
	public void renderInventoryBusLights( final IPartRenderHelper helper, final RenderBlocks renderer )
	{
		// Set color to white
		helper.setInvColor( 0xFFFFFF );

		IIcon busColorTexture = TextureManager.BUS_COLOR.getTextures()[0];

		IIcon sideTexture = TextureManager.BUS_COLOR.getTextures()[2];

		helper.setTexture( busColorTexture, busColorTexture, sideTexture, sideTexture, busColorTexture, busColorTexture );

		// Rend the box
		helper.renderInventoryBox( renderer );

		// Set the brightness
		Tessellator.instance.setBrightness( 0xD000D0 );

		helper.setInvColor( AEColor.Transparent.blackVariant );

		IIcon lightTexture = TextureManager.BUS_COLOR.getTextures()[1];

		// Render the lights
		helper.renderInventoryFace( lightTexture, ForgeDirection.UP, renderer );
		helper.renderInventoryFace( lightTexture, ForgeDirection.DOWN, renderer );
		helper.renderInventoryFace( lightTexture, ForgeDirection.NORTH, renderer );
		helper.renderInventoryFace( lightTexture, ForgeDirection.EAST, renderer );
		helper.renderInventoryFace( lightTexture, ForgeDirection.SOUTH, renderer );
		helper.renderInventoryFace( lightTexture, ForgeDirection.WEST, renderer );
	}
	@Override
	public int isProvidingStrongPower()
	{
		return 0;
	}

	@Override
	public int isProvidingWeakPower()
	{
		return 0;
	}
	@SideOnly(Side.CLIENT)
	@Override
	public abstract void renderStatic( int x, int y, int z, IPartRenderHelper helper, RenderBlocks renderer );

	@SideOnly(Side.CLIENT)
	public void renderStaticBusLights( final int x, final int y, final int z, final IPartRenderHelper helper, final RenderBlocks renderer )
	{
		IIcon busColorTexture = TextureManager.BUS_COLOR.getTextures()[0];

		IIcon sideTexture = TextureManager.BUS_COLOR.getTextures()[2];

		helper.setTexture( busColorTexture, busColorTexture, sideTexture, sideTexture, busColorTexture, busColorTexture );

		// Render the box
		helper.renderBlock( x, y, z, renderer );

		// Are we active?
		if( this.isActive() )
		{
			// Set the brightness
			Tessellator.instance.setBrightness( 0xD000D0 );

			// Set the color to match the cable
			Tessellator.instance.setColorOpaque_I( this.host.getColor().blackVariant );
		}
		else
		{
			// Set the color to black
			Tessellator.instance.setColorOpaque_I( 0 );
		}

		IIcon lightTexture = TextureManager.BUS_COLOR.getTextures()[1];

		// Render the lights
		helper.renderFace( x, y, z, lightTexture, ForgeDirection.UP, renderer );
		helper.renderFace( x, y, z, lightTexture, ForgeDirection.DOWN, renderer );
		helper.renderFace( x, y, z, lightTexture, ForgeDirection.NORTH, renderer );
		helper.renderFace( x, y, z, lightTexture, ForgeDirection.EAST, renderer );
		helper.renderFace( x, y, z, lightTexture, ForgeDirection.SOUTH, renderer );
		helper.renderFace( x, y, z, lightTexture, ForgeDirection.WEST, renderer );
	}

	@Override
	public boolean requireDynamicRender()
	{
		return false;
	}

	@Override
	public void securityBreak()
	{
		List<ItemStack> drops = new ArrayList<ItemStack>();

		// Get this item
		drops.add( this.getItemStack( PartItemStack.Break ) );

		// Get the drops for this part
		this.getDrops( drops, false );



		// Remove the part
		this.host.removePart( this.cableSide, false );

	}

	@Override
	public final void setPartHostInfo( final ForgeDirection side, final IPartHost host, final TileEntity tile )
	{
		this.cableSide = side;
		this.host = host;
		this.hostTile = tile;

	}

	@MENetworkEventSubscribe
	public final void setPower( final MENetworkPowerStatusChange event )
	{
		this.updateStatus();
	}

	public void setupPartFromItem( final ItemStack itemPart )
	{
		if( itemPart.hasTagCompound() )
		{
			this.readFromNBT( itemPart.getTagCompound() );
		}
	}

	@MENetworkEventSubscribe
	public void updateChannels( final MENetworkChannelsChanged event )
	{
		this.updateStatus();
	}

	@Override
	public void writeToNBT( final NBTTagCompound data )
	{
		// Assume world saving.
		this.writeToNBT( data, PartItemStack.World );
	}

	public void writeToNBT( final NBTTagCompound data, final PartItemStack saveType )
	{
		if( saveType == PartItemStack.World )
		{
			// Set the owner ID
			data.setInteger( AIPart.NBT_KEY_OWNER, this.ownerID );
		}

	}

	@Override
	public void writeToStream( final ByteBuf stream ) throws IOException
	{
		// Write active
		stream.writeBoolean( this.isActive() );

		// Write powered
		stream.writeBoolean( this.isPowered() );
	}

    public IMEMonitor<IAEFluidStack> getEnergyProvidingInventory() {
		IGridNode n = getGridNode();
		if (n == null)
			return null;
		IGrid g = n.getGrid();
		if (g == null)
			return null;
		IStorageGrid storage = g.getCache(IStorageGrid.class);
		if (storage == null)
			return null;
		IMEMonitor<IAEFluidStack> energyStorage = storage.getFluidInventory();
		if (energyStorage == null)
			return null;
		return energyStorage;
    }
}