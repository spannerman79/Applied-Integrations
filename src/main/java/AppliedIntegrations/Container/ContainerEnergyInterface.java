package AppliedIntegrations.Container;

import AppliedIntegrations.API.IEnergyInterface;
import AppliedIntegrations.API.LiquidAIEnergy;
import AppliedIntegrations.Parts.AIPart;
import AppliedIntegrations.Entities.TileEnergyInterface;
import AppliedIntegrations.Gui.GuiEnergyInterface;
import AppliedIntegrations.Gui.Widgets.WidgetEnergySlot;

import AppliedIntegrations.Parts.EnergyInterface.PartEnergyInterface;
import AppliedIntegrations.Utils.AIGridNodeInventory;
import appeng.container.slot.SlotRestrictedInput;
import com.google.common.collect.Maps;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ContainerFurnace;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static AppliedIntegrations.API.LiquidAIEnergy.*;

public class ContainerEnergyInterface extends ContainerWithNetworkTool {
    /**
     * The number of upgrade slots we have
     */
    private static int NUMBER_OF_UPGRADE_SLOTS = 1;

    /**
     * The x position of the upgrade slots
     */
    private static int UPGRADE_X_POS = 186;

    /**
     * The Y position for the upgrade slots
     */
    private static int UPGRADE_Y_POS = 8;
    public String realContainer;

    public boolean firstUpdate = true;

    private List<WidgetEnergySlot> energySlotList = new ArrayList<WidgetEnergySlot>();
    public final IEnergyInterface EnergyInterface;
    public PartEnergyInterface part;
    private TileEnergyInterface tile;
    public int LinkedRFStorage;
    public EntityPlayer player;

    private ContainerFurnace cont;
    private GuiEnergyInterface linkedGUI;

    public final Map<LiquidAIEnergy, Integer> LinkedStorageMap = Maps.newHashMap();
    public final Map<ForgeDirection,Map<LiquidAIEnergy, Integer>> LinkedTileStorageMap = Maps.newHashMap();
    public ContainerEnergyInterface(final EntityPlayer player, final IEnergyInterface energyInterface) {
        super(energyInterface, player);
        this.EnergyInterface = energyInterface;
        this.player = player;
        super.bindPlayerInventory(player.inventory,149,207);

        // check if interface part or tile?
        if (energyInterface instanceof PartEnergyInterface) {
            LinkedStorageMap.put(RF,LinkedRFStorage);
            for(ForgeDirection side : ForgeDirection.VALID_DIRECTIONS){
                LinkedTileStorageMap.put(side, LinkedStorageMap);
            }

            this.linkedGUI = new GuiEnergyInterface(this,this.EnergyInterface,player);
            PartEnergyInterface part = (PartEnergyInterface) this.EnergyInterface;
            // register listener
            part.addListener(this);

            this.part = (PartEnergyInterface)EnergyInterface;// add slots
            this.addUpgradeSlots(part.getUpgradeInventory(), this.NUMBER_OF_UPGRADE_SLOTS,
                    this.UPGRADE_X_POS, this.UPGRADE_Y_POS);
        } else if (energyInterface instanceof TileEnergyInterface) {
            TileEnergyInterface tile = (TileEnergyInterface) this.EnergyInterface;
            this.tile = (TileEnergyInterface)this.EnergyInterface;
            tile.addListener(this);
            // add slots
            this.addUpgradeSlots(tile.getUpgradeInventory(), this.NUMBER_OF_UPGRADE_SLOTS,
                    this.UPGRADE_X_POS + 1, this.UPGRADE_Y_POS);
        }
    }
    private void addCraftingSlots(AIGridNodeInventory upgradeInventory) {
        for(int i=0;i<8;i++){
            this.addSlotToContainer(new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.ENCODED_PATTERN,upgradeInventory,i,8+(i*18),(4*18)+149,
                    player.inventory));
        }
    }
    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }

    @Override
    public void onContainerClosed( @Nonnull final EntityPlayer player ) {
        super.onContainerClosed(player);
        if(part != null) {
            ((PartEnergyInterface) this.EnergyInterface).removeListener(this);
            this.realContainer = null;
            this.part.setRealContainer(realContainer);
        }
            //FlowMode = 0;
            //this.part.flowMode = Gui;
    }


    @Override
    public boolean onFilterReceive(AIPart part) {
        return part.getLocation().x == this.part.getX() && part.getLocation().y == this.part.getY()
                && part.getLocation().z == this.part.getZ() && part.getSide() == this.part.getSide();
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        if(part!=null) {
            for (int i = 0; i < this.crafters.size(); ++i) {
                ICrafting icrafting = (ICrafting) this.crafters.get(i);
                icrafting.sendProgressBarUpdate(this, 0, this.part.getEnergyStorage(RF).getEnergyStored());
                icrafting.sendProgressBarUpdate(this, 1, this.part.getEnergyStorage(J).getEnergyStored());
                icrafting.sendProgressBarUpdate(this, 2, this.part.getEnergyStorage(EU).getEnergyStored());
            }
           // this.LinkedRFStorage = this.part.getEnergyStorage(RF).getEnergyStored();
          //  this.LinkedJoStorage = this.part.getEnergyStorage(J).getEnergyStored();
           // this.LinkedEUStorage = this.part.getEnergyStorage(EU).getEnergyStored();
        }
    }
    @Override
    public ItemStack transferStackInSlot(final EntityPlayer player, final int slotNumber )
    {
        // Get the slot
        Slot slot = this.getSlotOrNull( slotNumber );

        // Do we have a valid slot with an item?
        if( ( slot != null ) && ( slot.getHasStack() ) )
        {
            // Pass to super
            return super.transferStackInSlot( player, slotNumber );
        }

        return null;
    }

    public boolean onStorageReceive(PartEnergyInterface sender) {
        if(sender.getX() == this.part.getX() && sender.getY() == this.part.getY() && this.part.getZ() == sender.getZ() && this.part.getSide() == sender.getSide()){
            return true;
        }
        return false;
    }

}


