/*
 * AppOpsXposed - AppOps for Android 4.3+
 * Copyright (C) 2013 Joseph C. Lehner
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of  MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package at.jclehner.appopsxposed.hacks;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import at.jclehner.appopsxposed.AppOpsXposed;
import at.jclehner.appopsxposed.Hack;
import at.jclehner.appopsxposed.R;
import at.jclehner.appopsxposed.util.AppOpsManagerWrapper;
import at.jclehner.appopsxposed.util.Constants;
import at.jclehner.appopsxposed.util.OpsLabelHelper;
import at.jclehner.appopsxposed.util.Res;
import at.jclehner.appopsxposed.util.Util;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

/*
 * A very crude hack to get something similar to OP_BOOT_COMPLETED,
 * available on recent CyanogenMod builds and (interestingly) some
 * Sony ROMs.
 *
 * Truly adding a OP_BOOT_COMPLETED is not at all easy, as it would require
 * the modification of 'public static final' primitive fields (e.g.
 * AppOpsXposed._NUM_OP), which may or may not work - most often it doesn't.
 * Instead, the operations OP_VIBRATE and OP_POST_NOTIFICATION were merged
 * into one single operation controlling both. More precisely, the numeric
 * value of OP_POST_NOTIFICATION becomes our OP_BOOT_COMPLETED, while
 * OP_VIBRATE remains unchanged, but behaves as if it were
 * OP_POST_NOTIFICATION.
 *
 * These two operations were chosen because:
 *     1) They are at least somewhat related (notifications often including
 *        vibration)
 *     2) Neither are important for privacy-conscious users
 *     3) OP_POST_NOTIFICATION was chosen as OP_BOOST_COMPLETED since the
 *        former can be found under the 'Device' section in AppOps, rather
 *        than the 'Media' option for OP_VIBRATE (in case creation of the
 *        'Bootup' section fails for some reason.
 *
 */

@TargetApi(19)
public class BootCompletedHack extends Hack
{
	public static final BootCompletedHack INSTANCE = new BootCompletedHack();

	private static final int OP_VIBRATE =
			XposedHelpers.getStaticIntField(AppOpsManager.class, "OP_VIBRATE");

	private static final int OP_POST_NOTIFICATION =
			XposedHelpers.getStaticIntField(AppOpsManager.class, "OP_POST_NOTIFICATION");


	private static final int MERGE_TARGET = OP_VIBRATE;
	private static final int MERGE_SOURCE = OP_POST_NOTIFICATION;

	public static final int OP_BOOT_COMPLETED = MERGE_SOURCE;

	@Override
	public void handleLoadSettingsPackage(LoadPackageParam lpparam) throws Throwable
	{
		// Must be called before injectLabelAndSummary; otherwise, AppOpsState's
		// static intializer may have been run, and we can't hook the OpsTemplate
		// constructors anymore!
		addBootupTemplate(lpparam);
		injectLabelAndSummary(lpparam);
		Util.sIsBootCompletedHackWorking = true;
	}

	@Override
	protected void handleLoadAnyPackage(LoadPackageParam lpparam) throws Throwable
	{
		patchAppOpsManager(lpparam.classLoader);

		if(AppOpsXposed.MODULE_PACKAGE.equals(lpparam.packageName))
		{
			XposedHelpers.findAndHookMethod(Util.class.getName(), lpparam.classLoader,
					"isBootCompletedHackWorking", XC_MethodReplacement.returnConstant(true));
		}
	}

	@Override
	protected void handleLoadFrameworkPackage(LoadPackageParam lpparam) throws Throwable {
		patchFrameworkPart(lpparam.classLoader);
	}

	@Override
	protected String onGetKeySuffix() {
		return "boot_completed";
	}

	private void patchFrameworkPart(ClassLoader classLoader)
	{
		try
		{
			patchNotificationManagerService(classLoader);
			patchActivityManagerService(classLoader);
		}
		catch(Throwable t)
		{
			XposedBridge.log(t);
		}
	}

	private void patchAppOpsManager(ClassLoader classLoader) throws Throwable
	{
		final Map<String, Object> functions = getAppOpsManagerFunctionsToPatch();

		final XC_MethodHook hook = new XC_MethodHook() {

			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable
			{
				final int op = (Integer) param.args[0];
				if(op == OP_BOOT_COMPLETED)
				{
					param.setResult(functions.get(param.method.getName()));
				}
			}
		};

		for(String function : functions.keySet())
		{
			try
			{
				XposedHelpers.findAndHookMethod(classLoader.loadClass("android.app.AppOpsManager"), function,
						int.class, hook);
			}
			catch(NoSuchMethodError e)
			{
				debug(e);
			}
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

		XposedBridge.hookAllMethods(amServiceClazz, "broadcastIntentLocked", new XC_MethodHook() {

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

	private static final String NOTIFICATION_MANAGER_SERVICE_CLASS =
			Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP ?
			"com.android.server.NotificationManagerService" :
			"com.android.server.notification.NotificationManagerService";

	private void patchNotificationManagerService(ClassLoader classLoader) throws Throwable
	{
		final Class<?> notificationMgrSvcClazz =
				classLoader.loadClass(NOTIFICATION_MANAGER_SERVICE_CLASS);

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
		{
			Class<?> targetClazz = null;
			boolean foundTarget = false;

			// In Lollipop, {are,set}NotificationsEnabledForPackage have been moved to
			// an anonymous inner class in NotificationManagerService.mService.
			// Since we can't get the class directly from that field using Field.getType(),
			// loop through all inner classes and try to hook methods.

			for(int i = 1; i <= 100; ++i)
			{
				try
				{
					targetClazz = loadClass(NOTIFICATION_MANAGER_SERVICE_CLASS + "$" + i);
					hookNotificationMethods(targetClazz);
					foundTarget = true;
					break;
				}
				catch(ClassNotFoundException e)
				{
					break;
				}
				catch(NoSuchMethodError e)
				{
					continue;
				}
				catch(NoSuchMethodException e)
				{
					continue;
				}
			}

			if(!foundTarget)
				debug("No inner class contains notification methods!");
			else if(targetClazz != null)
				debug("Found notification methods in " + targetClazz.getName());
		}
		else
			hookNotificationMethods(notificationMgrSvcClazz);

		final Class<?> appOpsMgrClazz = loadClass("android.app.AppOpsManager");

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
									//log("noteOpNoThrow called in noteNotificationOp");

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


	}

	private void hookNotificationMethods(Class<?> targetClazz) throws Throwable
	{
		// hook AppOpsManager.checkOpNoThrow for the duration of the call to
		// NotificationManagerService.areNotificationsEnabledForPackage

		final Class<?> appOpsMgrClazz = loadClass("android.app.AppOpsManager");

		XposedHelpers.findAndHookMethod(targetClazz, "areNotificationsEnabledForPackage",
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

		XposedHelpers.findAndHookMethod(targetClazz, "setNotificationsEnabledForPackage",
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

	private void injectLabelAndSummary(LoadPackageParam lpparam) throws Throwable
	{
		final Class<?> appOpsStateClazz = lpparam.classLoader.loadClass(
				"com.android.settings.applications.AppOpsState");

		XposedBridge.hookAllConstructors(appOpsStateClazz, new XC_MethodHook() {

					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable
					{
						final CharSequence summary = OpsLabelHelper.getPermissionLabel((Context) param.args[0],
								android.Manifest.permission.RECEIVE_BOOT_COMPLETED);

						Object array = XposedHelpers.getObjectField(param.thisObject, "mOpSummaries");
						Array.set(array, MERGE_TARGET, Array.get(array, MERGE_TARGET) + " / "
								 + Array.get(array, OP_BOOT_COMPLETED));
						Array.set(array, OP_BOOT_COMPLETED, summary);

						array = XposedHelpers.getObjectField(param.thisObject, "mOpLabels");
						Array.set(array, MERGE_TARGET, Array.get(array, MERGE_TARGET) + " / "
								 + Array.get(array, OP_BOOT_COMPLETED));
						Array.set(array, OP_BOOT_COMPLETED, Util.capitalizeFirst(summary));
					}
		});
	}

	private void addBootupTemplate(LoadPackageParam lpparam)
	{
		try
		{
			removeBootCompletedFromTemplates(lpparam);

			final Class<?> pagerAdapterClazz = lpparam.classLoader.loadClass(
					"com.android.settings.applications.AppOpsSummary$MyPagerAdapter");

			final Class<?> opsTemplateClazz = lpparam.classLoader.loadClass(
					"com.android.settings.applications.AppOpsState$OpsTemplate");

			final Class<?> appOpsCategoryClazz = lpparam.classLoader.loadClass(
					"com.android.settings.applications.AppOpsCategory");

			final Object bootupTemplate = XposedHelpers.newInstance(opsTemplateClazz,
					new int[] { OP_BOOT_COMPLETED }, new boolean[] { true });


			final Object bootupCategoryFragment = XposedHelpers.newInstance(appOpsCategoryClazz,
					bootupTemplate);

			final Object[][] adapterReturnValueInfos = {
					{ "getPageTitle", Res.getModString(R.string.app_ops_categories_bootup) },
					{ "getItem", bootupCategoryFragment }
			};

			for(final Object[] adapterReturnValueInfo : adapterReturnValueInfos)
			{
				XposedHelpers.findAndHookMethod(pagerAdapterClazz, (String) adapterReturnValueInfo[0],
						int.class, new XC_MethodHook() {

							@Override
							protected void beforeHookedMethod(MethodHookParam param) throws Throwable
							{
								final int pos = (Integer) param.args[0];
								final int bootupPos = ((Integer)
										XposedHelpers.callMethod(param.thisObject, "getCount")) - 1;

								if(pos == bootupPos)
									param.setResult(adapterReturnValueInfo[1]);
							}
				});
			}

			XposedHelpers.findAndHookMethod(pagerAdapterClazz, "getCount",
					new XC_MethodHook() {
						@Override
						protected void afterHookedMethod(MethodHookParam param) throws Throwable
						{
							param.setResult(1 + (Integer) param.getResult());
						}
			});
		}
		catch(Throwable t)
		{
			log("Failed to add bootup template");
			debug(t);
		}
	}

	private void removeBootCompletedFromTemplates(LoadPackageParam lpparam)
	{
		if(true)
			return;

		try
		{
			final Class<?> opsTemplateClazz = lpparam.classLoader.loadClass(
					"com.android.settings.applications.AppOpsState$OpsTemplate");

			XposedHelpers.findAndHookConstructor(opsTemplateClazz,
					int[].class, boolean[].class, new XC_MethodHook() {

						@Override
						protected void beforeHookedMethod(MethodHookParam param) throws Throwable
						{
							try
							{
								final int oldLength = Array.getLength(param.args[0]);
								if(oldLength == 1)
									return;

								final int[] newOps = new int[oldLength - 1];
								boolean wasItemRemoved = false;
								for(int i = 0, k = 0; i != oldLength; ++i, ++k)
								{
									final int op = Array.getInt(param.args[0], i);
									if(op == OP_BOOT_COMPLETED)
									{
										wasItemRemoved = true;
										--k;
										continue;
									}
										if(k < newOps.length)
										newOps[k] = op;
									else
										break;
								}
								if(wasItemRemoved)
								{
									log("OP_BOOT_COMPLETED removed from " + param.thisObject);
									param.args[0] = newOps;
								}
							}
							catch(Throwable t)
							{
								debug(t);
							}
						}
			});
		}
		catch(Throwable t)
		{
			debug(t);
		}
	}


	/*
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
	*/

	private static final Object[][][] APP_OPS_MANAGER_RETURN_VALUES = {
			{ // API 18
				{ "opToSwitch", OP_BOOT_COMPLETED },
				{ "opToName", "BOOT_COMPLETED" },
				{ "opToPermission", Manifest.permission.RECEIVE_BOOT_COMPLETED }
			},
			{ // API 19
				{ "opToDefaultMode", AppOpsManagerWrapper.MODE_ALLOWED },
				{ "opAllowsReset", true }
			},
			null, // API 20
			{ // API 21
				{ "opToRestriction", null },
				{ "opAllowSystemBypassRestriction", true }
			},
			null // API 22

	};

	private static Map<String, Object> getAppOpsManagerFunctionsToPatch()
	{
		final Map<String, Object> allFunctions = new HashMap<String, Object>();

		for(int api = 18; api <= Build.VERSION.SDK_INT; ++api)
		{
			final int i = api - 18;
			if(i < APP_OPS_MANAGER_RETURN_VALUES.length)
			{
				final Object[][] funcs = APP_OPS_MANAGER_RETURN_VALUES[i];
				if(funcs != null)
				{
					for(Object[] func : funcs)
						allFunctions.put((String) func[0], func[1]);
				}
			}
			else
				break;
		}

		return allFunctions;
	}
}
