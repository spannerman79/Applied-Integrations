package AppliedIntegrations.Entities.Server;

import AppliedIntegrations.API.IInventoryHost;
import AppliedIntegrations.Blocks.MEServer.BlockServerRib;
import AppliedIntegrations.Entities.AIMultiBlockTile;
import AppliedIntegrations.Entities.IAIMultiBlock;
import AppliedIntegrations.Utils.AILog;
import appeng.api.AEApi;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.storage.ICellContainer;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.StorageChannel;
import net.minecraft.block.Block;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.EnumSet;
import java.util.List;

import static net.minecraftforge.common.util.ForgeDirection.*;

public class TileServerRib extends AIMultiBlockTile implements IAIMultiBlock {

    public boolean isBlockNotified;

    @Override
    public void updateEntity() {
        super.updateEntity();
        if(hasMaster()){
            if(getMaster().MainNetwork == null) {
                IGrid grid = getNetwork();
                for (IGridNode node : grid.getNodes()) {
                    if (!(node.getMachine() instanceof AIMultiBlockTile)) {
                        getMaster().MainNetwork = grid;
                    }
                }
            }
        }
    }

    @Override
    public EnumSet<GridFlags> getFlags() {
        return EnumSet.of(GridFlags.DENSE_CAPACITY);
    }

    public void changeAlt(Boolean alt){
        Block rib = worldObj.getBlock(xCoord,yCoord,zCoord);
        if(rib != null && rib.getClass() == BlockServerRib.class) {
            ((BlockServerRib)rib).isAlt = alt;
            worldObj.markBlockForUpdate(xCoord,yCoord,zCoord);
            worldObj.notifyBlockChange(xCoord,yCoord,zCoord,rib);
        }
    }


}
