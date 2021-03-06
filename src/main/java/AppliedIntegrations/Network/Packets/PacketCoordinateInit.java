package AppliedIntegrations.Network.Packets;

import AppliedIntegrations.Gui.PartGui;
import AppliedIntegrations.Network.AIPacket;
import AppliedIntegrations.Utils.AILog;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * @Author Azazell
 *
 * @Usage Send this packet, whenever you want to mark gui as "Gui of THIS machine", ex:
 * you want to send data to PartEnergyStorage gui, then you need to mark gui as gui of that part, to mark gui just send this packet.
 */
public class PacketCoordinateInit extends AIPacket<PacketCoordinateInit> {

    public PacketCoordinateInit(){}

    public PacketCoordinateInit(int x, int y, int z, World w){

    }

    public PacketCoordinateInit(int x, int y, int z, World w, ForgeDirection dir){
        Gui g = Minecraft.getMinecraft().currentScreen;
         if (g instanceof PartGui) {
                PartGui partGui = (PartGui) g;

                partGui.setX(x);
                partGui.setY(y);
                partGui.setZ(z);

                partGui.setWorld(w);
                partGui.setSide(dir);
         }
    }

    @Override
    public IMessage HandleMessage(MessageContext ctx) {
        return null;
    }
}
