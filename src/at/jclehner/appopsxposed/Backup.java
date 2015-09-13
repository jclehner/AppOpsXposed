package at.jclehner.appopsxposed;


import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import at.jclehner.appopsxposed.util.AppOpsManagerWrapper;
import at.jclehner.appopsxposed.util.ObjectWrapper;
import at.jclehner.appopsxposed.util.Util;

/**
 * Handles importing/exporting of changed ops. The XML format
 * was inspired by /data/system/appops.xml, but has been adapted
 * to be more portable. Sample:
 *
 * <aox v="1">
 *   <pkg n="com.example.dialer">
 *      <op n="call_phone" m="ignored" />
 *      <op n="read_contacts" m="errored" />
 *   </pkg>
 * </aox>
 *
 * The identifiers used for ops and modes are such that they can be
 * accessed via reflection from AppOpsManagerWrapper (e.g.
 * "call_phone" -> "OP_" + "call_phone".toUpperCase())
 */
public class Backup
{
	public static boolean restore(Context context)
	{
		final AppOpsManagerWrapper appOps = AppOpsManagerWrapper.from(context);
		FileInputStream stream = null;

		try
		{
			final XmlPullParser xml = Xml.newPullParser();
			stream = new FileInputStream(getAndCreateFile(context));
			xml.setInput(stream, "utf-8");

			// Skip START_DOCUMENT
			xml.next();

			expectStartTag(xml, "aox");

			int version = Integer.parseInt(expectAttribute(xml, "v"));
			if(version != 1)
				throw new IllegalStateException("Expected version 1, got " + version);

			while(xml.next() != XmlPullParser.END_DOCUMENT)
			{
				if(isEndTag(xml, "aox"))
					break;

				expectStartTag(xml, "pkg");
				final String packageName = expectAttribute(xml, "n");
				final int uid = getPackageUid(context, packageName);

				// If uid == -1, the package is not installed. Skip the
				// restore phase, but keep parsing so as to validate the
				// file and not mess up the parser's state.

				boolean wasSkipMsgDisplayed = false;

				while(xml.next() != XmlPullParser.END_DOCUMENT)
				{
					if(isEndTag(xml, "pkg"))
						break;

					expectStartTag(xml, "op");

					final String opName = expectAttribute(xml, "n");
					String modeName = expectAttribute(xml, "m");

					final int op = AppOpsManagerWrapper.opFromName(opName.toUpperCase());
					final int mode = translateMode(packageName, modeName);
					modeName = AppOpsManagerWrapper.modeToName(mode).toLowerCase();

					if(AppOpsManagerWrapper.isValidOp(op) && mode != -1)
					{
						if(uid != -1)
						{
							appOps.setMode(op, uid, packageName, mode);
							Log.i("AOX:Backup", packageName + ": " + opName + " = " + modeName);
						}
						else if(!wasSkipMsgDisplayed)
						{
							Log.i("AOX:Backup", packageName + ": not installed; skipping");
							wasSkipMsgDisplayed = true;
						}
					}
					else
						Log.i("AOX:Backup", packageName + ": not restoring op " + opName);

					xml.next();
					expectEndTag(xml, "op");
				}

				expectEndTag(xml, "pkg");
			}

			return true;
		}
		catch(IOException|XmlPullParserException|ParseException e)
		{
			Log.w("AOX:Backup", e);
			return false;
		}
		finally
		{
			Util.closeQuietly(stream);
		}
	}

	public static boolean create(Context context)
	{
		FileOutputStream stream = null;

		try
		{
			final XmlSerializer xml = Xml.newSerializer();
			xml.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
			stream = new FileOutputStream(getAndCreateFile(context));

			xml.setOutput(stream, "utf-8");
			xml.startDocument(null, true);
			xml.startTag(null, "aox");
			xml.attribute(null, "v", "1");

			final AppOpsManagerWrapper appOps = AppOpsManagerWrapper.from(context);
			final List<AppOpsManagerWrapper.PackageOpsWrapper> pows = appOps.getPackagesForOps(null);

			if(pows != null)
			{
				for(AppOpsManagerWrapper.PackageOpsWrapper pow : pows)
				{
					boolean pkgWritten = false;

					for(AppOpsManagerWrapper.OpEntryWrapper oew : pow.getOps())
					{
						final int mode = oew.getMode();
						final int op = oew.getOp();

						if(mode != AppOpsManagerWrapper.opToDefaultMode(op))
						{
							if(!pkgWritten)
							{
								xml.startTag(null, "pkg");
								xml.attribute(null, "n", pow.getPackageName());
								pkgWritten = true;
							}

							xml.startTag(null, "op");
							xml.attribute(null, "n", AppOpsManagerWrapper.opToName(op).toLowerCase());
							xml.attribute(null, "m", AppOpsManagerWrapper.modeToName(mode).toLowerCase());
							xml.endTag(null, "op");
						}
					}

					if(pkgWritten)
						xml.endTag(null, "pkg");
				}
			}

			xml.endTag(null, "aox");
			xml.endDocument();

			return true;
		}
		catch(IOException e)
		{
			Log.w("AOX:Backup", e);
			return false;
		}
		finally
		{
			Util.closeQuietly(stream);
		}
	}


	public static File getFile(Context context)
	{
		return new File(Environment.getExternalStorageDirectory(), "appopsxposed-backup.xml");
	}

	private static File getAndCreateFile(Context context) throws IOException
	{
		final File f = getFile(context);
		f.createNewFile();
		return f;
	}

	private static String expectAttribute(XmlPullParser xml, String name) throws XmlPullParserException
	{
		final String value = xml.getAttributeValue(null, name);
		if(value == null)
			throw new ParseException(xml, "Expected attribute \"" + name + "\" in <" + xml.getName() + ">");
		return value;
	}

	private static boolean isEndTag(XmlPullParser xml, String tag) throws IOException, XmlPullParserException
	{
		skipWhitespace(xml);
		return xml.getEventType() == XmlPullParser.END_TAG && tag.equals(xml.getName());
	}

	private static void expectStartTag(XmlPullParser xml, String tag) throws IOException, XmlPullParserException
	{
		expectTag(xml, tag, false);
	}

	private static void expectEndTag(XmlPullParser xml, String tag) throws IOException, XmlPullParserException
	{
		expectTag(xml, tag, true);
	}

	private static void expectTag(XmlPullParser xml, String tag, boolean end) throws IOException, XmlPullParserException
	{
		skipWhitespace(xml);

		boolean validEvent = false;

		switch(xml.getEventType())
		{
			case XmlPullParser.START_TAG:
				validEvent = !end;
				break;
			case XmlPullParser.END_TAG:
				validEvent = end;
				break;
			default:
				throw new ParseException(xml, "Expected <" + (end ? "/" : "") + tag + ">, got "
						+ stateToString(xml));
		}

		if(!validEvent)
		{
			throw new ParseException(xml, "Expected <" + (end ? "/" : "") + tag + ">, got "
					+ stateToString(xml));
		}
	}

	private static void skipWhitespace(XmlPullParser xml) throws IOException, XmlPullParserException
	{
		int type = xml.getEventType();

		// Skip over whitespace
		while((type == XmlPullParser.TEXT) && TextUtils.isEmpty(xml.getName()))
			type = xml.next();
	}

	private static String stateToString(XmlPullParser xml) throws XmlPullParserException
	{
		final int type = xml.getEventType();

		switch(type)
		{
			case XmlPullParser.START_TAG:
			case XmlPullParser.END_TAG:
				return "<" + (type == XmlPullParser.END_TAG ? "/" : "") + xml.getName() + ">";

			default:
				return XmlPullParser.TYPES[type];
		}
	}

	private static int translateMode(String pkg, String modeName)
	{
		final String fieldName = "MODE_" + modeName.toUpperCase();
		final int mode = ObjectWrapper.getStatic(AppOpsManagerWrapper.class, fieldName, -1);
		if(mode == -1)
		{
			Log.i("AOX:Backup", pkg + ": unknown mode " + modeName + " -> ignored");
			return AppOpsManagerWrapper.MODE_IGNORED;
		}

		return mode;
	}

	private static int getPackageUid(Context context, String pkg)
	{
		try
		{
			return context.getPackageManager().getPackageInfo(pkg, 0).applicationInfo.uid;
		}
		catch(PackageManager.NameNotFoundException e)
		{
			return -1;
		}
	}

	static class ParseException extends RuntimeException
	{
		ParseException(XmlPullParser xml, String message)
		{
			super("XML:" + xml.getLineNumber() + ":" + xml.getColumnNumber() + ": " + message);
		}
	}
}
