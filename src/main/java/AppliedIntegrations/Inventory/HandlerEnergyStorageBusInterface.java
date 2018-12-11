package AppliedIntegrations.Inventory;

import AppliedIntegrations.Entities.TileEnergyInterface;
import AppliedIntegrations.Parts.EnergyInterface.PartEnergyInterface;
import AppliedIntegrations.Parts.EnergyStorageBus.PartEnergyStorage;
import AppliedIntegrations.Utils.AILog;
import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.IncludeExclude;
import appeng.api.implementations.tiles.ITileStorageMonitorable;
import appeng.api.networking.IGridNode;
import appeng.api.networking.events.MENetworkCellArrayUpdate;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.MachineSource;
import appeng.api.networking.security.PlayerSource;
import appeng.api.networking.storage.IBaseMonitor;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.storage.*;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IItemList;
import appeng.util.Platform;
import net.minecraft.tileentity.TileEntity;

/**
 * @Author Azazell
 */
class HandlerEnergyStorageBusInterface
        extends HandlerEnergyStorageBusBase
        implements IMEMonitorHandlerReceiver<IAEFluidStack>
{
    /**
     * Interface the storage bus is facing.
     */
    private ITileStorageMonitorable MEInterface = null;

    /**
     * Handler to the interfaces ME grid.
     */
    private MEInventoryHandler<IAEFluidStack> handler;

    /**
     * Hashcode of the interface.
     */
    private int handlerHash;

    /**
     * Prevent infinite loops when sub-grid's sub-grid is hosts grid.
     * yeah.
     */
    private boolean canPostUpdate = true;

    /**
     * Creates the interface handler.
     *
     * @param part
     */
    public HandlerEnergyStorageBusInterface( final PartEnergyStorage part )
    {
        super( part );
    }

    /**
     * Checks if the sub-grid can accept this Energy.
     */
    @Override
    public boolean canAccept( final IAEFluidStack fluidStack )
    {
        // Is the fluid an Energy Energy?
        if( this.isFluidEnergy( fluidStack ) )
        {
            // Pass to handler
            if( this.handler != null )
            {
                return this.handler.canAccept( fluidStack );
            }
        }

        return false;
    }

    /**
     * Attempts to extract the Energy from the sub-grid.
     */
    @Override
    public IAEFluidStack extractItems( final IAEFluidStack request, final Actionable mode, final BaseActionSource source )
    {
        // Is the fluid an Energy Energy?
        if( this.isFluidEnergy( request ) )
        {
            if( this.handler != null )
            {
                // Extract the Energy
                IAEFluidStack extractedEnergy = this.handler.extractItems( request, mode, source );

                return extractedEnergy;
            }
        }

        return null;
    }

    /**
     * Gets the Energies from the sub-grid.
     */
    @Override
    public IItemList<IAEFluidStack> getAvailableItems( final IItemList<IAEFluidStack> out )
    {
        if( this.handler != null )
        {
            // Get the subgrids fluids
            IItemList<IAEFluidStack> subGridFluids = this.handler.getAvailableItems( AEApi.instance().storage().createFluidList() );

            for( IAEFluidStack fluid : subGridFluids )
            {
                // Is the fluid as Energy Energy?
                if( this.isFluidEnergy( fluid ) )
                {
                    // Add to the output list
                    out.add( fluid );
                }
            }

        }

        return out;
    }

    /**
     * Attempts to inject the Energy into the sub-network.
     */
    @Override
    public IAEFluidStack injectItems( final IAEFluidStack input, final Actionable mode, final BaseActionSource source )
    {
        // Is the fluid an Energy Energy?
        if( this.isFluidEnergy( input ) )
        {
            if( this.handler != null )
            {
                // Inject the Energy
                IAEFluidStack remainingEnergy = this.handler.injectItems( input, mode, source );

                return remainingEnergy;
            }
        }
        return input;
    }

    /**
     * Is the handler still valid for receiving changes?
     */
    @Override
    public boolean isValid( final Object verificationToken )
    {
        return this.handler == verificationToken;
    }

    /**
     * Sub-grid list changed, we should update.
     */
    @Override
    public void onListUpdate()
    {
        if( this.canPostUpdate )
        {
            this.canPostUpdate = false;
            try
            {
                this.partStorageBus.getGridBlock().getGrid().postEvent( new MENetworkCellArrayUpdate() );
            }
            catch( Exception e )
            {

            }
        }
    }

    @Override
    public boolean onNeighborChange()
    {
        ITileStorageMonitorable facingInterface = null;

        // Get the tile we are facing
        TileEntity tileEntity = this.getFacingTile();

        // Is the tile a part host?
        if( tileEntity instanceof IPartHost )
        {
            // Get the facing part
            IPart facingPart = this.getFacingPartFromPartHost( (IPartHost)tileEntity );

            if( facingPart instanceof PartEnergyInterface)
            {
                facingInterface = (ITileStorageMonitorable)facingPart;
                AILog.info("Interface detected");
            }
        }
        // Is it an interface?
        else if( tileEntity instanceof TileEnergyInterface)
        {
            AILog.info("Interface detected");
            facingInterface = (ITileStorageMonitorable)tileEntity;
        }

        // Is the storage bus facing an interface?
        if( facingInterface != null )
        {
            // Get the tile hashcode
            int newHandlerHash = Platform.generateTileHash( tileEntity );

            // Do the hashes match?
            if( ( this.handlerHash == newHandlerHash ) && ( this.handlerHash != 0 ) )
            {
                return false;
            }

            // Post cell update to cell network.
            try
            {
                this.partStorageBus.getGridBlock().getGrid().postEvent( new MENetworkCellArrayUpdate() );
            }
            catch( Exception e )
            {

            }

            // Set the tile hashcode
            this.handlerHash = newHandlerHash;

            // Set the interface
            this.MEInterface = facingInterface;

            // Clear the old handler.
            this.handler = null;

            // Get the monitor
            IStorageMonitorable monitor = this.MEInterface.getMonitorable( this.partStorageBus.getSide().getOpposite(), this.machineSource );

            // Ensure a monitor was retrieved
            if( monitor != null )
            {
                // Get the fluid inventory
                IMEInventory inv = monitor.getFluidInventory();

                // Ensure the fluid inventory was retrieved
                if( inv != null )
                {
                    // Create the handler
                    this.handler = new MEInventoryHandler<IAEFluidStack>( inv, StorageChannel.FLUIDS );

                    // Set the handler properties
                    this.handler.setBaseAccess( this.getAccess() );
                    this.handler.setWhitelist( IncludeExclude.WHITELIST );
                    this.handler.setPriority( this.getPriority() );

                    // Add the handler as a listener
                    if( inv instanceof IMEMonitor )
                    {
                        ( (IMEMonitor)inv ).addListener( this, this.handler );
                    }
                }
            }

            return true;
        }

        // Not facing interface
        this.handlerHash = 0;
        this.MEInterface = null;

        // Was the handler attached to an interface?
        if( this.handler != null )
        {
            this.handler = null;
            return true;
        }

        return false;
    }

    /**
     * A change occurred in the sub-grid, inform the host grid.
     */
    @Override
    public void postChange( final IBaseMonitor<IAEFluidStack> monitor, final Iterable<IAEFluidStack> change, final BaseActionSource actionSource )
    {
        try
        {
            IActionHost actionHost = null;

            // Get the action source
            if( actionSource instanceof PlayerSource )
            {
                // From the player source
                actionHost = ( (PlayerSource)actionSource ).via;
            }
            else if( actionSource instanceof MachineSource )
            {
                // From the machine source
                actionHost = ( (MachineSource)actionSource ).via;
            }

            // Ensure there is an action host
            if( actionHost != null )
            {
                // Post update if change did not come from host grid, prevents double posting.
                if( actionHost.getActionableNode().getGrid() != this.partStorageBus.getActionableNode().getGrid() )
                {
                    // Update the host grid
                    this.postAlterationToHostGrid( change );
                }
            }
        }
        catch( Exception e )
        {

        }
    }

    @Override
    public void tickingRequest( final IGridNode node, final int TicksSinceLastCall )
    {
        this.canPostUpdate = true;
    }

    @Override
    public boolean validForPass( final int pass )
    {
        if( this.handler != null )
        {
            return this.handler.validForPass( pass );
        }

        return false;
    }

}