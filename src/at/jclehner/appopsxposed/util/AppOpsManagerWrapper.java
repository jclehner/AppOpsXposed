package at.jclehner.appopsxposed.util;

import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.content.Context;

public class AppOpsManagerWrapper extends ObjectWrapper
{
	public static AppOpsManagerWrapper from(Context context) {
		return new AppOpsManagerWrapper(context);
	}

	@TargetApi(19)
	private AppOpsManagerWrapper(Context context) {
		super(context.getSystemService(Context.APP_OPS_SERVICE));
	}

	public List<PackageOpsWrapper> getOpsForPackage(int uid, String packageName, int[] ops)
	{
		final List<?> rawPackageOps = call("getOpsForPackage", new Class<?>[] { int.class, String.class, int[].class },
				uid, packageName, ops);
		final List<PackageOpsWrapper> packageOps = new ArrayList<PackageOpsWrapper>();

		if(rawPackageOps != null)
		{
			for(Object o : rawPackageOps)
				packageOps.add(new PackageOpsWrapper(o));
		}

		return packageOps;
	}

	public String opToName(int op) {
		return callStatic("opToName", new Class<?>[] { int.class }, op);
	}

	public String opToPermission(int op) {
		return callStatic("opToPermission", new Class<?>[] { int.class }, op);
	}

	public static class PackageOpsWrapper extends ObjectWrapper
	{
		private PackageOpsWrapper(Object obj) {
			super(obj);
		}

		public String getPackageName() {
			return call("getPackageName");
		}

		public int getUid() {
			return (Integer) call("getUid");
		}

		public List<OpEntryWrapper> getOps()
		{
			final List<OpEntryWrapper> ops = new ArrayList<OpEntryWrapper>();

			for(Object o : (List<?>) call("getOps"))
				ops.add(new OpEntryWrapper(o));

			return ops;
		}
	}

	public static class OpEntryWrapper extends ObjectWrapper
	{
		private OpEntryWrapper(Object obj) {
			super(obj);
		}

		public int getOp() {
			return (Integer) call("getOp");
		}

		public int getMode() {
			return (Integer) call("getMode");
		}

		public long getTime() {
			return (Long) call("getTime");
		}

		public long getRejectTime() {
			return (Long) call("getRejectTime");
		}

		public boolean isRunning() {
			return (Boolean) call("isRunning");
		}

		public int getDuration() {
			return (Integer) call("getDuration");
		}
	}


}
