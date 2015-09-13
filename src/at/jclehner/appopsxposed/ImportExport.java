package at.jclehner.appopsxposed;


import android.content.Context;
import android.util.Xml;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

import at.jclehner.appopsxposed.util.AppOpsManagerWrapper;

public class ImportExport
{
	public static void export(Context context) throws IOException
	{
		final XmlSerializer out = Xml.newSerializer();
		out.setOutput(System.err, "utf-8");
		out.startDocument(null, true);
		out.startTag(null, "aox");
		out.attribute(null, "v", "1");

		final AppOpsManagerWrapper appOps = AppOpsManagerWrapper.from(context);
		for(AppOpsManagerWrapper.PackageOpsWrapper pow : appOps.getPackagesForOps(null))
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
						out.startTag(null, "pkg");
						out.attribute(null, "n", pow.getPackageName());
						pkgWritten = true;
					}

					out.startTag(null, "op");
					out.attribute(null, "n", AppOpsManagerWrapper.opToName(op).toLowerCase());
					out.attribute(null, "m", AppOpsManagerWrapper.modeToName(mode).toLowerCase());
					out.endTag(null, "op");
				}
			}

			if(pkgWritten)
				out.endTag(null, "pkg");
		}

		out.endTag(null, "aox");
		out.endDocument();
	}
}
