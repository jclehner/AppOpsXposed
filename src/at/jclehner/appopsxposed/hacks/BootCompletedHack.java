package at.jclehner.appopsxposed.hacks;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import at.jclehner.appopsxposed.Hack;
import at.jclehner.appopsxposed.Util;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

@TargetApi(19)
public class BootCompletedHack extends Hack
{
	public static final BootCompletedHack INSTANCE = new BootCompletedHack(); 
	
	private static final int OP_VIBRATE = 
			XposedHelpers.getStaticIntField(AppOpsManager.class, "OP_VIBRATE");
	
	private static final int OP_POST_NOTIFICATION =
			XposedHelpers.getStaticIntField(AppOpsManager.class, "OP_POST_NOTIFICATION");
	

	private static final int OP_BOOT_COMPLETED = OP_POST_NOTIFICATION;
	
	@Override
	public void handleLoadAnyPackage(LoadPackageParam lpparam) throws Throwable {
		patchFrameworkPart(lpparam.classLoader);
	}
	
	@Override
	public void handleLoadSettingsPackage(LoadPackageParam lpparam) throws Throwable
	{		
		final Class<?> appOpsStateClazz = lpparam.classLoader.loadClass(
				"com.android.settings.applications.AppOpsState");
		
		XposedBridge.hookAllConstructors(appOpsStateClazz, new XC_MethodHook() {
			
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable 
					{						
						final CharSequence summary = Util.getOpStringFromPermission((Context) param.args[0],
								OP_BOOT_COMPLETED);
												
						Object array = XposedHelpers.getObjectField(param.thisObject, "mOpSummaries");
						Array.set(array, OP_VIBRATE, Array.get(array, OP_VIBRATE) + " / " 
								 + Array.get(array, OP_BOOT_COMPLETED));
						Array.set(array, OP_BOOT_COMPLETED, summary);
												
						array = XposedHelpers.getObjectField(param.thisObject, "mOpLabels");
						Array.set(array, OP_VIBRATE, Array.get(array, OP_VIBRATE) + " / " 
								 + Array.get(array, OP_BOOT_COMPLETED));	
						Array.set(array, OP_BOOT_COMPLETED, Util.capitalizeFirst(summary));
					}
		});
	}
	
	private void patchFrameworkPart(ClassLoader classLoader)
	{
		try
		{			
			if(OP_BOOT_COMPLETED == OP_VIBRATE)
				patchVibratorService(classLoader);
			else //if(OP_BOOT_COMPLETED == OP_POST_NOTIFICATION)
				patchNotificationManagerService(classLoader);
						
			patchActivityManagerService(classLoader);
			patchAppOpsManager(classLoader);
		}
		catch(Throwable t)
		{
			XposedBridge.log(t);
		}
	}
	
	@TargetApi(19) 
	private void patchAppOpsManager(ClassLoader classLoader)
	{		
		final XC_MethodHook hook = new XC_MethodHook() {
			
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable 
			{
				final int op = (Integer) param.args[0];
				if(op == OP_BOOT_COMPLETED)
				{
					param.setResult(XposedHelpers.getStaticObjectField(AppOpsManagerReturnValues.class, 
							param.method.getName()));
				}
			}
		};
		
		for(Field f : AppOpsManagerReturnValues.class.getDeclaredFields())
		{
			XposedHelpers.findAndHookMethod(AppOpsManager.class, f.getName(),
					int.class, hook);		
		}
	}
	
	@TargetApi(19)
	private void patchActivityManagerService(ClassLoader classLoader) throws Throwable
	{		
		final Class<?> amServiceClazz = classLoader.loadClass(
				"com.android.server.am.ActivityManagerService");
		
		final Class<?> procRecordClazz = classLoader.loadClass(
				"com.android.server.am.ProcessRecord");
		
		final Class<?> iIntentRecvrClazz = classLoader.loadClass(
				"android.content.IIntentReceiver");
				
		XposedHelpers.findAndHookMethod(amServiceClazz, "broadcastIntentLocked", 
				procRecordClazz, String.class, Intent.class, String.class,
				iIntentRecvrClazz, int.class, String.class, Bundle.class,
				String.class, int.class, boolean.class, boolean.class, int.class,
				int.class, int.class, new XC_MethodHook() {
			
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable 
					{
						if(Manifest.permission.RECEIVE_BOOT_COMPLETED.equals(param.args[8]))
						{
							param.args[9] = OP_BOOT_COMPLETED;
							log("broadcastIntentLocked called; setting op to OP_BOOT_COMPLETED");
							log("  processRecord=" + param.args[0]);
						}
					}
		});
	}
	
	private void patchNotificationManagerService(ClassLoader classLoader) throws Throwable
	{		
		final Class<?> notificationMgrSvcClazz = 
				classLoader.loadClass("com.android.server.NotificationManagerService");
		
		final Class<?> appOpsMgrClazz =
				classLoader.loadClass("android.app.AppOpsManager");
		
		// hook AppOpsManager.checkOpNoThrow for the duration of the call to 
		// NotificationManagerService.areNotificationsEnabledForPackage
		
		XposedHelpers.findAndHookMethod(notificationMgrSvcClazz, "areNotificationsEnabledForPackage", 
				String.class, int.class, new XC_MethodHook() {
			
				private Unhook mUnhook;
			
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable 
				{
					mUnhook = XposedHelpers.findAndHookMethod(appOpsMgrClazz, "checkOpNoThrow", 
							int.class, int.class, String.class, new XC_MethodHook() {
								@Override
								protected void beforeHookedMethod(MethodHookParam param)
										throws Throwable 
								{
									log("checkOpNoThrow called in areNotificationsEnabledForPackage");
									
									final int op = (Integer) param.args[0];
									if(op == OP_POST_NOTIFICATION)
										param.args[0] = OP_VIBRATE;
								}
					});
				}
				
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					mUnhook.unhook();
				}			
		});
		
		// hook AppOpsManager.noteOpNoThrow for the duration of the call to 
		// NotificationManagerService.noteNotificationOp
		
		XposedHelpers.findAndHookMethod(notificationMgrSvcClazz, "noteNotificationOp", 
				String.class, int.class, new XC_MethodHook() {
			
				private Unhook mUnhook;
			
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable 
				{
					mUnhook = XposedHelpers.findAndHookMethod(appOpsMgrClazz, "noteOpNoThrow", 
							int.class, int.class, String.class, new XC_MethodHook() {
								@Override
								protected void beforeHookedMethod(MethodHookParam param)
										throws Throwable 
								{
									log("noteOpNoThrow called in noteNotificationOp");
									
									final int op = (Integer) param.args[0];
									if(op == OP_POST_NOTIFICATION)
										param.args[0] = OP_VIBRATE;
								}
					});
				}
				
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					mUnhook.unhook();
				}			
		});
		
		// hook AppOpsManager.setMode for the duration of the call to 
		// NotificationManagerService.setNotificationsEnabledForPackage
		
		XposedHelpers.findAndHookMethod(notificationMgrSvcClazz, "setNotificationsEnabledForPackage", 
				String.class, int.class, boolean.class, new XC_MethodHook() {
			
				private Unhook mUnhook;
			
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable 
				{
					mUnhook = XposedHelpers.findAndHookMethod(appOpsMgrClazz, "setMode", 
							int.class, int.class, String.class, int.class, new XC_MethodHook() {
								@Override
								protected void beforeHookedMethod(MethodHookParam param)
										throws Throwable 
								{
									log("setMode called in setNotificationsEnabledForPackage");
									
									final int op = (Integer) param.args[0];
									if(op == OP_POST_NOTIFICATION)
										param.args[0] = OP_VIBRATE;
								}
					});
				}
				
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					mUnhook.unhook();
				}			
		});
	}

	private void patchVibratorService(ClassLoader classLoader) throws Throwable
	{		
		final Class<?> vibServiceClazz = 
				classLoader.loadClass("com.android.server.VibratorService");
		
		final Class<?> vibrationClazz =
				classLoader.loadClass("com.android.server.VibratorService$Vibration");
		
		final Class<?> appOpsServiceClazz =
				classLoader.loadClass("com.android.server.AppOpsService");
		
		XposedHelpers.findAndHookMethod(vibServiceClazz, "startVibrationLocked", 
				vibrationClazz, new XC_MethodHook() {
			
					private Unhook mUnhook;
			
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable 
					{
						mUnhook = XposedHelpers.findAndHookMethod(appOpsServiceClazz, "startOperation",
								IBinder.class, int.class, int.class, String.class, new XC_MethodHook() {
									@Override
									protected void beforeHookedMethod(MethodHookParam param) throws Throwable 
									{
										log("startOperation called in startVibrationLocked");
										
										final int op = (Integer) param.args[1];
										if(op == OP_VIBRATE)
											param.args[1] = OP_POST_NOTIFICATION;
									}
						});
					}
					
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						mUnhook.unhook();
					}			
		});
		
		XposedHelpers.findAndHookMethod(vibServiceClazz, "reportFinishVibrationLocked", 
				 new XC_MethodHook() {
			
					private Unhook mUnhook;
			
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable 
					{
						mUnhook = XposedHelpers.findAndHookMethod(appOpsServiceClazz, "finishOperation",
								IBinder.class, int.class, int.class, String.class, new XC_MethodHook() {
									@Override
									protected void beforeHookedMethod(MethodHookParam param) throws Throwable 
									{
										log("finishOperation called in reportFinishVibrationLocked");
										
										final int op = (Integer) param.args[1];
										if(op == OP_VIBRATE)
											param.args[1] = OP_POST_NOTIFICATION;
									}
						});
					}
					
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						mUnhook.unhook();
					}			
		});		
	}
	
	private void log(String str)
	{
		XposedBridge.log("AOX:BootCompletedHack: " + str);
	}
	
	static class AppOpsManagerReturnValues
	{
		static final String opToName = "BOOT_COMPLETED";
		static final String opToPermission = Manifest.permission.RECEIVE_BOOT_COMPLETED;
		static final int opToDefaultMode = AppOpsManager.MODE_ALLOWED;
		static final boolean opAllowsReset = false;
	}
	
}



