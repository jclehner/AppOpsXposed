package at.jclehner.appopsxposed;


import android.content.Context;
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
public class ImportExport
{
	public static boolean restore(Context context)
	{
		FileInputStream stream = null;

		try
		{
			final XmlPullParser xml = Xml.newPullParser();
			stream = new FileInputStream(getBackupFile(context));
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
				final String pkg = expectAttribute(xml, "n");

				Log.d("AOX:Backup", pkg);

				while(xml.next() != XmlPullParser.END_DOCUMENT)
				{
					if(isEndTag(xml, "pkg"))
						break;

					expectStartTag(xml, "op");

					final String opName = expectAttribute(xml, "n");
					final String modeName = expectAttribute(xml, "m");

					Log.d("AOX:Backup", "  " + opName + " = " + modeName);

					xml.next();
					expectEndTag(xml, "op");
				}

				expectEndTag(xml, "pkg");
			}

			return true;
		}
		catch(IOException|XmlPullParserException e)
		{
			Log.w("AOX:Backup", e);
			return false;
		}
		finally
		{
			Util.closeQuietly(stream);
		}
	}

	public static boolean export(Context context)
	{
		FileOutputStream stream = null;

		try
		{
			final XmlSerializer xml = Xml.newSerializer();
			xml.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
			stream = new FileOutputStream(getBackupFile(context));

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

	private static File getBackupFile(Context context) throws IOException
	{
		final File f = new File(Environment.getExternalStorageDirectory(), "appopsxposed-backup.xml");
		if(!f.exists())
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

	private static boolean isEndTag(XmlPullParser xml, String tag) throws XmlPullParserException
	{
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
		int type = xml.getEventType();

		// Skip over whitespace
		while((type == XmlPullParser.TEXT) && TextUtils.isEmpty(xml.getName()))
			type = xml.next();

		boolean validEvent = false;

		switch(type)
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

	static class ParseException extends RuntimeException
	{
		ParseException(XmlPullParser xml, String message)
		{
			super("XML:" + xml.getLineNumber() + ":" + xml.getColumnNumber() + ": " + message);
		}
	}
}
