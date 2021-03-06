package AppliedIntegrations.Utils;

import AppliedIntegrations.Network.Packets.PacketClientFilter;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;

import static AppliedIntegrations.AppliedIntegrations.getLogicalSide;


public class AILog
{
    public static final Logger log = LogManager.getLogger( "Applied Integrations" );

    public static void debug( final String format, final Object ... data )
    {
        log.debug( String.format( format, data ) );
    }

    public static void error( final Throwable e, final String format, final Object ... data )
    {
        log.error( String.format( format, data ), e );
    }
    public static void info( final String message, Object... params)
    {
        log.info(message, params );
    }

    public static void chatLog(final String message) {
        Minecraft.getMinecraft().thePlayer.addChatComponentMessage(new ChatComponentText(message));
    }
    public static void chatLog(final String message, EntityPlayer player){
        player.addChatComponentMessage(new ChatComponentText(message));
    }

    public static void debugThread(boolean useChatLog){
        if(useChatLog){
            chatLog(Thread.currentThread().getName());
            chatLog(getLogicalSide().name());
        }else{
            info(Thread.currentThread().getName());
            info(getLogicalSide().name());
        }
    }
    public static void debugObject(Object obj, boolean useChatLog) {
        for(Field f : obj.getClass().getFields()){
            try {
                if(useChatLog) {
                    chatLog(f.get(obj).toString());
                }else {
                    info(f.get(obj).toString());
                }
            }catch (IllegalAccessException except){

            }
        }
    }

    public static void debugObjects(Object... objects) {
        for(Object obj : objects){
            try {
                info(obj.toString());
            }catch (NullPointerException nullPtr){
                info("null");
            }
        }
    }
}
