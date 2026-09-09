/* Commander Table. Copyright 2026. GPL-3.0-or-later. */
package table;
import forge.gui.interfaces.IGuiBase;
import java.lang.reflect.Proxy;
import javax.swing.SwingUtilities;
import java.util.List;
public final class HeadlessPlatform {
 public static IGuiBase create(String assets) {
  return (IGuiBase) Proxy.newProxyInstance(IGuiBase.class.getClassLoader(), new Class[]{IGuiBase.class}, (proxy,m,args)-> {
   String n=m.getName();
   switch(n){
    case "toString":return "CommanderTableHeadless";
    case "hashCode":return System.identityHashCode(proxy);
    case "equals":return proxy==args[0];
    case "getAssetsDir":return assets.endsWith("/")?assets:assets+"/";
    case "getCurrentVersion":return "2.0.15-SNAPSHOT / Commander Table";
    case "isRunningOnDesktop":return true;
    case "isGuiThread":return SwingUtilities.isEventDispatchThread();
    case "getScreenScale":return 1f;
    case "getAvatarCount": case "getSleevesCount":return 1;
    case "encodeSymbols":return args[0];
    case "invokeInEdtNow":case "invokeInEdtLater":SwingUtilities.invokeLater((Runnable)args[0]);return null;
    case "invokeInEdtAndWait":if(SwingUtilities.isEventDispatchThread())((Runnable)args[0]).run();else SwingUtilities.invokeAndWait((Runnable)args[0]);return null;
    case "runBackgroundTask":new Thread((Runnable)args[1],"forge-background").start();return null;
    case "showBugReportDialog":case "showImageDialog":System.err.println("Forge: "+java.util.Arrays.toString(args));return null;
    case "showOptionDialog":return -1;
    case "showInputDialog":return args[3]==null?"":args[3];
    case "getChoices":case "order":return List.of();
    case "getNewGuiGame":for(var room:TableServer.ROOMS.values())if(room.aiOnly&&(room.status.equals("starting")||room.status.equals("playing"))&&room.seats[0].gui!=null)return room.seats[0].gui;throw new IllegalStateException("No spectator table is starting.");
   }
   if(m.getReturnType()==boolean.class)return false;
   if(m.getReturnType()==int.class)return 0;
   if(m.getReturnType()==float.class)return 1f;
   return null;
  });
 }
}
