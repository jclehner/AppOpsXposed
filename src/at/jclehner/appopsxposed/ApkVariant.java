package at.jclehner.appopsxposed;

import java.util.ArrayList;
import java.util.List;

import android.os.Build;
import at.jclehner.appopsxposed.variants.Samsung;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public abstract class ApkVariant implements IXposedHookLoadPackage
{
	protected final String ANY = "";

	private static final ApkVariant[] VARIANTS = {
		new Samsung()
	};

	public static List<ApkVariant> getAllMatching(LoadPackageParam lpparam)
	{
		List<ApkVariant> variants = new ArrayList<ApkVariant>();

		for(ApkVariant variant : VARIANTS)
		{
			if(variant.isMatching(lpparam))
				variants.add(variant);
		}

		return variants;
	}

	public String versionName() {
		return ANY;
	}

	public String manufacturer() {
		return ANY;
	}

	public String release() {
		return ANY;
	}

	public String buildId() {
		return ANY;
	}

	public int minApiLevel() {
		return 0;
	}

	public boolean blocksStockVariant() {
		return false;
	}

	private boolean isMatching(LoadPackageParam lpparam)
	{
		if(manufacturer() != ANY && !Build.MANUFACTURER.equalsIgnoreCase(manufacturer()))
			return false;

		if(release() != ANY && !Build.VERSION.RELEASE.equals(release()))
			return false;

		if(buildId() != ANY && !Build.ID.equals(buildId()))
			return false;

		if(Build.VERSION.SDK_INT < minApiLevel())
			return false;

		return true;
	}
}
