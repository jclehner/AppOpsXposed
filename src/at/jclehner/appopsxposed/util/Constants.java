package at.jclehner.appopsxposed.util;

import at.jclehner.appopsxposed.R;

public final class Constants
{
	public static final String[] APP_OPS_PERMISSIONS = {
		"android.permission.UPDATE_APP_OPS_STATS",
		"android.permission.GET_APP_OPS_STATS"
	};

	public static final String MODULE_PACKAGE = "at.jclehner.appopsxposed";

	public static final int[] ICONS = {
			R.drawable.ic_launcher2,
			R.drawable.ic_appops_cog_grey,
			R.drawable.ic_appops_cog_black,
			R.drawable.ic_appops_cog_teal,
			R.drawable.ic_appops_cog_white,
			R.drawable.ic_appops_cog_circle,
			R.drawable.ic_appops_black,
			R.drawable.ic_appops_shield_teal,
			R.drawable.ic_appops_white,
			R.drawable.ic_appops_shield_circle
	};

	public static final int ICON_LAUNCHER = 0;
	public static final int ICON_COG_GREY = 1;
	public static final int ICON_COG_BLACK = 2;
	public static final int ICON_COG_TEAL = 3;
	public static final int ICON_COG_WHITE = 4;
	public static final int ICON_COG_CIRCLE = 5;
	public static final int ICON_SHIELD_BLACK = 6;
	public static final int ICON_SHIELD_TEAL = 7;
	public static final int ICON_SHIELD_WHITE = 8;
	public static final int ICON_SHIELD_CIRCLE = 0;


	private Constants() {}
}
